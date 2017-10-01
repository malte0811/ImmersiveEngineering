package blusunrize.immersiveengineering.client.render;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityTeslaCoil;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityTeslaCoil.LightningAnimation;
import blusunrize.immersiveengineering.common.util.BinaryTree;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class TileRenderTeslaCoil extends TileEntitySpecialRenderer<TileEntityTeslaCoil>
{
	@Override
	public void render(TileEntityTeslaCoil tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(tile.isDummy()||!tile.getWorld().isBlockLoaded(tile.getPos(), false))
			return;
		Iterator<LightningAnimation> animationIt = TileEntityTeslaCoil.effectMap.get(tile.getPos()).iterator();

		setLightmapDisabled(true);
		GL11.glPushAttrib(GL11.GL_LIGHTING);
		GlStateManager.disableLighting();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		while(animationIt.hasNext())
		{
			LightningAnimation animation = animationIt.next();

			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, z);

			GlStateManager.disableTexture2D();
			GlStateManager.enableBlend();
			GlStateManager.shadeModel(GL11.GL_SMOOTH);
			GlStateManager.enableCull();


			double tx = tile.getPos().getX();
			double ty = tile.getPos().getY();
			double tz = tile.getPos().getZ();
			float curWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
			drawAnimation(animation, tx,ty,tz, new float[]{77/255f,74/255f,152/255f, .75f}, 4f);
			GL11.glLineWidth(curWidth);
			
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
			GlStateManager.shadeModel(GL11.GL_FLAT);

			GlStateManager.popMatrix();
			break;
		}
		GL11.glPopAttrib();
		setLightmapDisabled(false);
	}
	
	public void drawAnimation(LightningAnimation animation, double tileX, double tileY, double tileZ, float[] rgba, float lineWidth)
	{
		Tessellator tes = ClientUtils.tes();
		BufferBuilder worldrenderer = tes.getBuffer();
		GlStateManager.translate(0, 2, 0);
		GlStateManager.rotate(-45, 1, 0, 0);
		if (animation!=null&&animation.structure!=null)
		{
			drawTree(animation.structure, worldrenderer, tes);
		}
	}

	//This is absolutely horrible, but I want to see whether the generation code works
	private void drawTree(BinaryTree<LightningAnimation.StreamerNode> tree, BufferBuilder worldrenderer, Tessellator tes) {
		Shaders.useShader(Shaders.TESLA_STREAMER);
		double widthFactor = .1/(double) LightningAnimation.MAX_DEPTH;
		GlStateManager.pushMatrix();
		GlStateManager.rotate((float)tree.content.angle, (float) tree.content.axis.x,
				(float) tree.content.axis.y,
				(float) tree.content.axis.z);
		GlStateManager.color(1,1,1,1);
		worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		drawSection(tree.content.length, (tree.getDepth()+1)* widthFactor,
				tree.getDepth()* widthFactor, worldrenderer);
		tes.draw();
		GlStateManager.translate(0, tree.content.length, 0);
		if (tree.getLeft()!=null) {
			drawTree(tree.getLeft(), worldrenderer, tes);
		}
		if (tree.getRight()!=null) {
			drawTree(tree.getRight(), worldrenderer, tes);
		}
		GlStateManager.popMatrix();
		Shaders.stopUsingShaders();
	}
	private void drawSection(double length, double diaBottom, double diaTop, BufferBuilder worldrenderer) {
		//drawPart(length, diaBottom/4, diaTop/4, worldrenderer);
		drawPart(length, diaBottom, diaTop, worldrenderer);
	}

	private void drawPart(double length, double diaBottom, double diaTop, BufferBuilder worldrenderer)
	{
		worldrenderer.pos(-diaBottom / 2, 0, 0).color(0, 1, 0, 1f).endVertex();
		worldrenderer.pos(-diaTop / 2, length, 0).color(0, 0, 0, 1f).endVertex();
		worldrenderer.pos(diaTop / 2, length, 0).color(1, 0, 0, 1f).endVertex();
		worldrenderer.pos(diaBottom / 2, 0, 0).color(1, 1, 0, 1f).endVertex();
		worldrenderer.pos(0, 0, -diaBottom / 2).color(0, 1, 0, 1f).endVertex();
		worldrenderer.pos(0, length, -diaTop / 2).color(0, 0, 0, 1f).endVertex();
		worldrenderer.pos(0, length, diaTop / 2).color(1, 0, 0, 1f).endVertex();
		worldrenderer.pos(0, 0, diaBottom / 2).color(1, 1, 0, 1f).endVertex();

		worldrenderer.pos(-diaTop / 2, length, 0).color(0, 0, 0, 1f).endVertex();
		worldrenderer.pos(-diaBottom / 2, 0, 0).color(0, 1, 0, 1f).endVertex();
		worldrenderer.pos(diaBottom / 2, 0, 0).color(1, 1, 0, 1f).endVertex();
		worldrenderer.pos(diaTop / 2, length, 0).color(1, 0, 0, 1f).endVertex();
		worldrenderer.pos(0, length, -diaTop / 2).color(0, 0, 0, 1f).endVertex();
		worldrenderer.pos(0, 0, -diaBottom / 2).color(0, 1, 0, 1f).endVertex();
		worldrenderer.pos(0, 0, diaBottom / 2).color(1, 1, 0, 1f).endVertex();
		worldrenderer.pos(0, length, diaTop / 2).color(1, 0, 0, 1f).endVertex();
	}
}