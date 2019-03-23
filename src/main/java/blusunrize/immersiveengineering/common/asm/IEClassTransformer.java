/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.asm;

import com.google.common.collect.Maps;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author BluSunrize - 20.07.2017
 */
public class IEClassTransformer implements IClassTransformer
{
	private static Map<String, MethodTransformer[]> transformerMap = Maps.newHashMap();

	static
	{
		Consumer<MethodNode> addThreadingCall = (m)->{
			InsnList insert = new InsnList();
			insert.add(new LdcInsnNode("Server thread"));
			insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "blusunrize/immersiveengineering/common/asm/ThreadLogger", "checkForThread",
					"(Ljava/lang/String;)V", false));
			m.instructions.insertBefore(m.instructions.get(0), insert);
		};
		transformerMap.put("net.minecraft.world.WorldServer", new MethodTransformer[]{
			new MethodTransformer("updateBlockTick", "func_175654_a",
					"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V", addThreadingCall),
			new MethodTransformer("scheduleBlockUpdate", "func_180497_b",
					"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V", addThreadingCall),
			new MethodTransformer("getPendingBlockUpdates", "func_175712_a",
					"(Lnet/minecraft/world/gen/structure/StructureBoundingBox;Z)Ljava/util/List;", addThreadingCall),
		});
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if(basicClass!=null&&transformerMap.containsKey(transformedName))
		{
			MethodTransformer[] transformers = transformerMap.get(transformedName);
			ClassReader reader = new ClassReader(basicClass);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);

			for(MethodNode method : node.methods)
				for(MethodTransformer methodTransformer : transformers)
					if((methodTransformer.functionName.equals(method.name)||methodTransformer.srgName.equals(method.name))&&methodTransformer.functionDesc.equals(method.desc))
						methodTransformer.function.accept(method);

			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			return writer.toByteArray();
		}
		return basicClass;
	}

	private static class MethodTransformer
	{
		final String functionName;
		final String srgName;
		final String functionDesc;
		final Consumer<MethodNode> function;

		private MethodTransformer(String funcName, String srgName, String funcDesc, Consumer<MethodNode> function)
		{
			this.functionName = funcName;
			this.srgName = srgName;
			this.functionDesc = funcDesc;
			this.function = function;
		}
	}
}
