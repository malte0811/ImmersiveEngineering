package blusunrize.immersiveengineering.common.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class BinaryTree<T>
{
	private int depth = 0;
	private int depthUp = 0;
	private int totalNodes = 0;
	@Nullable
	private BinaryTree<T> left;
	@Nullable
	private BinaryTree<T> right;
	@Nullable
	private BinaryTree<T> up;
	@Nonnull
	public T content;

	public BinaryTree(T content) {
		this.content = content;
	}

	@Nullable
	public BinaryTree<T> getLeft()
	{
		return left;
	}

	@Nullable
	public BinaryTree<T> getRight()
	{
		return right;
	}

	public void setLeft(@Nullable BinaryTree<T> left)
	{
		this.left = left;
		if (left!=null) {
			left.up = this;
			left.updateRecursiveUp();
		}
		updateRecursiveDown();
	}

	public void setLeft(T t) {
		setLeft(new BinaryTree<>(t));
	}

	public void setRight(@Nullable BinaryTree<T> right)
	{
		this.right = right;
		if (right!=null) {
			right.up = this;
			right.updateRecursiveUp();
		}
		updateRecursiveDown();
	}
	public void setRight(T t) {
		setRight(new BinaryTree<>(t));
	}

	private void updateRecursiveDown() {
		int lDepth = left!=null?(left.getDepth()+1):0;
		int rDepth = right!=null?(right.getDepth()+1):0;
		final int oldDepth = depth;
		depth = Math.max(lDepth, rDepth);
		int lNodes = left!=null?left.totalNodes:0;
		int rNodes = right!=null?right.totalNodes:0;
		final int oldTotal = totalNodes;
		totalNodes = lNodes+rNodes;
		if (up!=null&&(depth!=oldDepth||oldTotal!=totalNodes)) {
			up.updateRecursiveDown();
		}
	}

	private void updateRecursiveUp() {
		final int oldUp = depthUp;
		depthUp = up==null?0:(up.depthUp+1);
		if (depthUp!=oldUp) {
			if (left!=null) {
				left.updateRecursiveUp();
			}
			if (right!=null) {
				right.updateRecursiveUp();
			}
		}
	}

	public int getDepth()
	{
		return depth;
	}

	public int getTotalNodes()
	{
		return totalNodes;
	}

	public int getDepthUp()
	{
		return depthUp;
	}

	public void inOrderTraverse(Consumer<BinaryTree<T>> consumer) {
		if (left!=null) {
			left.inOrderTraverse(consumer);
		}
		consumer.accept(this);
		if (right!=null) {
			right.inOrderTraverse(consumer);
		}
	}
}
