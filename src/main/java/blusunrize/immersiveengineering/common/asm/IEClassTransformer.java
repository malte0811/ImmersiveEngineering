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

import java.util.ListIterator;
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
			{
				InsnList insert = new InsnList();
				insert.add(new LdcInsnNode("Server thread"));
				insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "blusunrize/immersiveengineering/common/asm/ThreadLogger", "checkForThread",
						"(Ljava/lang/String;)V", false));
				m.instructions.insertBefore(m.instructions.get(0), insert);
			}
			ListIterator<AbstractInsnNode> it = m.instructions.iterator();
			AbstractInsnNode last = null;
			while(it.hasNext())
			{
				AbstractInsnNode curr = it.next();
				if(curr.getOpcode()==Opcodes.POP)
				{
					if(last instanceof MethodInsnNode)
					{
						MethodInsnNode lastMeth = (MethodInsnNode)last;
						if("java/util/TreeSet".equals(lastMeth.owner)||"java/util/Set".equals(lastMeth.owner))
						{
							if("remove".equals(lastMeth.name)||"add".equals(lastMeth.name))
							{
								it.remove();
								InsnList insert = new InsnList();
								insert.add(new InsnNode(Opcodes.DUP_X1));
								m.instructions.insertBefore(last, insert);
								insert.clear();
								insert.add(new VarInsnNode(Opcodes.ALOAD, 0));
								insert.add(new LdcInsnNode(lastMeth.owner+"::"+lastMeth.name));
								insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "blusunrize/immersiveengineering/common/asm/ThreadLogger", "logIfFalse",
										"(Lnet/minecraft/world/NextTickListEntry;ZLnet/minecraft/world/WorldServer;Ljava/lang/String;)V", false));
								m.instructions.insert(last, insert);
							}
						}
					}
				}
				last = curr;
			}
		};
		transformerMap.put("net.minecraft.world.WorldServer", new MethodTransformer[]{
			new MethodTransformer("updateBlockTick", "func_175654_a",
					"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V", addThreadingCall),
			new MethodTransformer("scheduleBlockUpdate", "func_180497_b",
					"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V", addThreadingCall),
			new MethodTransformer("getPendingBlockUpdates", "func_175712_a",
					"(Lnet/minecraft/world/gen/structure/StructureBoundingBox;Z)Ljava/util/List;", addThreadingCall),
				new MethodTransformer("tickUpdates", "func_72955_a", "(Z)Z", addThreadingCall),
		});
		Consumer<MethodNode> addChangeCall = (m) -> {
			InsnList insert = new InsnList();
			insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "blusunrize/immersiveengineering/common/asm/ThreadLogger", "verifyOrderChangeCall",
					"()V", false));
			m.instructions.insertBefore(m.instructions.get(0), insert);
		};
		transformerMap.put("net.minecraft.world.NextTickListEntry", new MethodTransformer[]{
				new MethodTransformer("setScheduledTime", "func_77176_a",
						"(J)Lnet/minecraft/world/NextTickListEntry;", addChangeCall),
				new MethodTransformer("setPriority", "func_82753_a",
						"(I)V", addChangeCall),
		});
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if(basicClass!=null)//&&transformerMap.containsKey(transformedName))
		{
			MethodTransformer[] transformers = transformerMap.getOrDefault(transformedName, new MethodTransformer[0]);
			ClassReader reader = new ClassReader(basicClass);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);

			for(MethodNode method : node.methods)
			{
				for(MethodTransformer methodTransformer : transformers)
					if((methodTransformer.functionName.equals(method.name)||methodTransformer.srgName.equals(method.name))&&methodTransformer.functionDesc.equals(method.desc))
						methodTransformer.function.accept(method);
				for(AbstractInsnNode insn : method.instructions.toArray())
				{
					if(insn instanceof FieldInsnNode)
					{
						FieldInsnNode fInsn = (FieldInsnNode)insn;
						if("field_77180_e".equals(fInsn.name))
							System.out.println("Access to scheduledTime found: "+name+"::"+method.name+"|"+method.desc+
									", opcode is "+fInsn.getType());
						else if("field_82754_f".equals(fInsn.name))
							System.out.println("Access to priority found: "+name+"::"+method.name+"|"+method.desc+
									", opcode is "+fInsn.getType());
						else if("field_73064_N".equals(fInsn.name))
							System.out.println("Access to hash found: "+name+"::"+method.name+"|"+method.desc+
									", opcode is "+fInsn.getType());
						else if("field_73065_O".equals(fInsn.name))
							System.out.println("Access to tree found: "+name+"::"+method.name+"|"+method.desc+
									", opcode is "+fInsn.getType());
					}
					else if(insn instanceof LdcInsnNode)
					{
						LdcInsnNode ldc = (LdcInsnNode)insn;
						if("field_73064_N".equals(ldc.cst))
							System.out.println("String ref to hash found: "+name+"::"+method.name+"|"+method.desc);
						else if("field_73065_O".equals(ldc.cst))
							System.out.println("String ref to tree found: "+name+"::"+method.name+"|"+method.desc);
					}
				}
			}
			if(transformers.length > 0)
			{
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				node.accept(writer);
				return writer.toByteArray();
			}
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
