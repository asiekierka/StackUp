/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * This file is part of StackUp.
 *
 * StackUp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * StackUp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with StackUp.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.stackup.asm;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;

public class StackUpTransformer implements IClassTransformer {
	public boolean hasClass(String s) {
		try {
			Class.forName(s);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		transformedName = transformedName.replace('/', '.');

		byte[] data = basicClass;
		Consumer<ClassNode> consumer = (n) -> {};
		Consumer<ClassNode> emptyConsumer = consumer;

		if (StackUpClassTracker.isImplements(transformedName, "net.minecraft.inventory.IInventory", "adb")) {
			consumer = consumer.andThen((node) -> {
				patchMaxLimit(node, "getInventoryStackLimit", "f()I");
			});
		}

		/* if (StackUpClassTracker.isImplements(transformedName, "net.minecraftforge.items.IItemHandler")) {
			consumer = consumer.andThen((node) -> {
				patchMaxLimit(node, "getSlotLimit");
			});
		} */

		if (StackUpClassTracker.isExtends(transformedName, "net.minecraft.inventory.Slot", "aqt")) {
			consumer = consumer.andThen((node) -> {
				patchMaxLimit(node,
						"getItemStackLimit", "b(Lata;)I",
						"getSlotStackLimit", "a()I");
			});
		}

		if (StackUpClassTracker.isExtends(transformedName, "net.minecraft.item.crafting.ServerRecipePlacer", "oz")) {
			consumer = consumer.andThen((node) -> {
				patchMaxLimit(node,
						"func_201509_a", "a(ZIZ)I");
			});
		}

		if ("net.minecraft.client.renderer.entity.RenderEntityItem".equals(transformedName) || "cyu".equals(transformedName)) {
			boolean isDeobf = transformedName.contains(".");

			consumer = consumer.andThen((node) -> {
				for (MethodNode mn : node.methods) {
					// LDC -0.09375
					// v
					// ALOAD 1
					// INVOKESTATIC getItemRenderDistance(Lnet/minecraft/entity/item/EntityItem;)F
					if ("doRender".equals(mn.name)
							|| ("a".equals(mn.name) && mn.desc.endsWith("DDDFF)V"))) {
						ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
						while (it.hasNext()) {
							AbstractInsnNode in = it.next();
							if (in.getOpcode() == Opcodes.LDC) {
								LdcInsnNode min = (LdcInsnNode) in;
								if (min.cst instanceof Number
										&& Math.abs(Math.abs(((Number) min.cst).floatValue()) - 0.09375F) < 0.001F) {
									boolean isNegative = ((Number) min.cst).floatValue() < 0;

									it.set(new VarInsnNode(
											Opcodes.ALOAD, 1
									));
									it.add(new MethodInsnNode(
											Opcodes.INVOKESTATIC,
											"pl/asie/stackup/StackUpHelpers",
											isNegative ? "getItemRenderDistanceNeg" : "getItemRenderDistance",
											isDeobf ? "(Lnet/minecraft/entity/item/EntityItem;)F" : "(Lami;)F",
											false
									));
									System.out.println("Patched item render distance constant in RenderEntityItem!");
								}
							}
						}
					}
				}
			});
		} else if ("net.minecraft.inventory.InventoryHelper".equals(transformedName) || "ade".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				spliceClasses(node, "pl.asie.stackup.splices.InventoryHelperPerformancePatch",
						"spawnItemStack", "a");
			});
		} else if ("net.minecraft.network.PacketBuffer".equals(transformedName) || "hy".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				spliceClasses(node, "pl.asie.stackup.splices.PacketBufferWriters",
						"readItemStack", "k",
						"writeItemStack", "a");
			});
		} else if ("net.minecraft.client.renderer.ItemRenderer".equals(transformedName) || "cyw".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				for (MethodNode mn : node.methods) {
					if ("renderItemOverlayIntoGUI".equals(mn.name)
							|| ("a".equals(mn.name) && mn.desc.endsWith(";IILjava/lang/String;)V"))) {
						ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
						while (it.hasNext()) {
							AbstractInsnNode in = it.next();
							//     INVOKEVIRTUAL net/minecraft/client/gui/FontRenderer.drawStringWithShadow (Ljava/lang/String;FFI)I
							if (in.getOpcode() == Opcodes.INVOKEVIRTUAL) {
								MethodInsnNode min = (MethodInsnNode) in;
								if ((min.owner.equals("net/minecraft/client/gui/FontRenderer") || min.owner.equals("cfz"))
										&& min.desc.equals("(Ljava/lang/String;FFI)I")) {
									it.set(new MethodInsnNode(
											Opcodes.INVOKESTATIC,
											"pl/asie/stackup/StackUpHelpers",
											"drawItemCountWithShadow",
											"(L"+min.owner+";Ljava/lang/String;FFI)I",
											false
									));
									System.out.println("Patched item count render in ItemRenderer!");
									break;
								}
							}
						}
					}
				}
			});
		} else if ("net.minecraft.network.NetHandlerPlayServer".equals(transformedName) || "ub".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				for (MethodNode mn : node.methods) {
					if ("processCreativeInventoryAction".equals(mn.name)
							|| "a".equals(mn.name)) {
						ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
						while (it.hasNext()) {
							AbstractInsnNode in = it.next();
							if (in.getOpcode() == Opcodes.INVOKEVIRTUAL) {
								MethodInsnNode min = (MethodInsnNode) in;
								if ((min.owner.equals("net/minecraft/item/ItemStack") || min.owner.equals("ata"))
										&& (min.name.equals("getCount") || min.name.equals("C"))) {
									AbstractInsnNode in2 = it.next();
									if (in2.getOpcode() == Opcodes.BIPUSH) {
										IntInsnNode intInsnNode = (IntInsnNode) in2;
										if (intInsnNode.operand == 64) {
											System.out.println("Patched processCreativeInventoryAction count check!");
											it.set(new MethodInsnNode(
													Opcodes.INVOKESTATIC, "pl/asie/stackup/StackUpHelpers", "getMaxStackSize", "()I", false
											));
										}
									}
								}
							}
						}
					}
				}
			});
		} else if ("net.minecraft.item.ItemStack".equals(transformedName) || "ata".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				for (MethodNode mn : node.methods) {
					if ("<init>".equals(mn.name)) {
						ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
						while (it.hasNext()) {
							AbstractInsnNode in = it.next();
							if (in instanceof LdcInsnNode && "Count".equals(((LdcInsnNode) in).cst)) {
								AbstractInsnNode in2 = it.next();
								if (in2.getOpcode() == Opcodes.INVOKEVIRTUAL) {
									// :thinking:
									boolean patched = false;
									MethodInsnNode min2 = (MethodInsnNode) in2;
									if (min2.name.equals("getByte")) {
										min2.name = "getInteger";
										patched = true;
									} else if (min2.name.equals("f")) {
										min2.name = "h";
										patched = true;
									}

									if (patched) {
										min2.desc = "(Ljava/lang/String;)I";
										System.out.println("Patched ItemStack Count getter!");
									}
								}
							}
						}
					} else if ("writeToNBT".equals(mn.name) || "b(Lgy;)Lgy;".equals(mn.name+mn.desc)) {
						ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
						while (it.hasNext()) {
							AbstractInsnNode in = it.next();
							if (in instanceof LdcInsnNode && "Count".equals(((LdcInsnNode) in).cst)) {
								it.next();
								it.next();
								it.next();
								AbstractInsnNode in2 = it.next();
								if (in2.getOpcode() == Opcodes.INVOKEVIRTUAL) {
									// :thinking:
									boolean patched = false;
									MethodInsnNode min2 = (MethodInsnNode) in2;
									if (min2.name.equals("setByte")) {
										min2.name = "setInteger";
										patched = true;
									} else if ("a(Ljava/lang/String;B)V".equals(min2.name + min2.desc)) {
										min2.name = "b";
										patched = true;
									}

									if (patched) {
										min2.desc = "(Ljava/lang/String;I)V";
										System.out.println("Patched ItemStack Count setter!");

										// Remove I2B cast
										it.previous();
										it.previous();
										it.remove();
									}
								}
							}
						}
					}
				}
			});
		}

		if (consumer != emptyConsumer) {
			return processNode(basicClass, consumer);
		} else {
			return data;
		}
	}

	public static void patchMaxLimit(ClassNode node, String... methods) {
		Set<String> methodSet = Sets.newHashSet(methods);

		for (MethodNode mn : node.methods) {
			if (methodSet.contains(mn.name) || methodSet.contains(mn.name + mn.desc)) {
				int patchesMade = 0;

				ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
				while (it.hasNext()) {
					AbstractInsnNode in = it.next();
					if (in.getOpcode() == Opcodes.BIPUSH) {
						IntInsnNode iin = (IntInsnNode) in;
						if (iin.operand == 64) {
							System.out.println("Patched max stack check in " + node.name + " -> " + mn.name + "!");
							//    INVOKESTATIC pl/asie/stackup/StackUpHelpers.getMaxStackSize ()I
							it.set(new MethodInsnNode(
									Opcodes.INVOKESTATIC, "pl/asie/stackup/StackUpHelpers", "getMaxStackSize", "()I", false
							));
							patchesMade++;
						}
					}
				}

				if (patchesMade > 1) {
					System.out.println("NOTE: Made " + patchesMade + " patches in " + node.name + " -> " + mn.name + "!");
				}
			}
		}
	}

	public static byte[] processNode(byte[] data, Consumer<ClassNode> classNodeConsumer) {
		ClassReader reader = new ClassReader(data);
		ClassNode nodeOrig = new ClassNode();
		reader.accept(nodeOrig, 0);
		classNodeConsumer.accept(nodeOrig);
		ClassWriter writer = new ClassWriter(0);
		nodeOrig.accept(writer);
		return writer.toByteArray();
	}

	public static byte[] spliceClasses(final byte[] data, final String className, final String... methods) {
		ClassReader reader = new ClassReader(data);
		ClassNode nodeOrig = new ClassNode();
		reader.accept(nodeOrig, 0);
		ClassNode nodeNew = spliceClasses(nodeOrig, className, methods);
		ClassWriter writer = new ClassWriter(0);
		nodeNew.accept(writer);
		return writer.toByteArray();
	}

	public static ClassNode spliceClasses(final ClassNode data, final String className, final String... methods) {
		try (InputStream stream = StackUpTransformer.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
			return spliceClasses(data, ByteStreams.toByteArray(stream), className, methods);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static ClassNode spliceClasses(final ClassNode nodeData, final byte[] dataSplice, final String className, final String... methods) {
		// System.out.println("Splicing from " + className + " to " + targetClassName)
		if (dataSplice == null) {
			throw new RuntimeException("Class " + className + " not found! This is a FoamFix bug!");
		}

		final Set<String> methodSet = Sets.newHashSet(methods);
		final List<String> methodList = Lists.newArrayList(methods);

		final ClassReader readerSplice = new ClassReader(dataSplice);
		final String className2 = className.replace('.', '/');
		final String targetClassName2 = nodeData.name;
		final String targetClassName = targetClassName2.replace('/', '.');
		final Remapper remapper = new Remapper() {
			public String map(final String name) {
				return className2.equals(name) ? targetClassName2 : name;
			}
		};

		ClassNode nodeSplice = new ClassNode();
		readerSplice.accept(new ClassRemapper(nodeSplice, remapper), ClassReader.EXPAND_FRAMES);
		for (String s : nodeSplice.interfaces) {
			if (methodSet.contains(s)) {
				nodeData.interfaces.add(s);
				System.out.println("Added INTERFACE: " + s);
			}
		}

		for (int i = 0; i < nodeSplice.methods.size(); i++) {
			if (methodSet.contains(nodeSplice.methods.get(i).name)) {
				MethodNode mn = nodeSplice.methods.get(i);
				boolean added = false;

				for (int j = 0; j < nodeData.methods.size(); j++) {
					if (nodeData.methods.get(j).name.equals(mn.name)
							&& nodeData.methods.get(j).desc.equals(mn.desc)) {
						MethodNode oldMn = nodeData.methods.get(j);
						System.out.println("Spliced in METHOD: " + targetClassName + "." + mn.name);
						nodeData.methods.set(j, mn);
						if (nodeData.superName != null && nodeData.name.equals(nodeSplice.superName)) {
							ListIterator<AbstractInsnNode> nodeListIterator = mn.instructions.iterator();
							while (nodeListIterator.hasNext()) {
								AbstractInsnNode node = nodeListIterator.next();
								if (node instanceof MethodInsnNode
										&& node.getOpcode() == Opcodes.INVOKESPECIAL) {
									MethodInsnNode methodNode = (MethodInsnNode) node;
									if (targetClassName2.equals(methodNode.owner)) {
										methodNode.owner = nodeData.superName;
									}
								}
							}
						}

						oldMn.name = methodList.get((methodList.indexOf(oldMn.name)) & (~1)) + "_stackup_old";
						nodeData.methods.add(oldMn);
						added = true;
						break;
					}
				}

				if (!added) {
					System.out.println("Added METHOD: " + targetClassName + "." + mn.name);
					nodeData.methods.add(mn);
					added = true;
				}
			}
		}

		for (int i = 0; i < nodeSplice.fields.size(); i++) {
			if (methodSet.contains(nodeSplice.fields.get(i).name)) {
				FieldNode mn = nodeSplice.fields.get(i);
				boolean added = false;

				for (int j = 0; j < nodeData.fields.size(); j++) {
					if (nodeData.fields.get(j).name.equals(mn.name)
							&& nodeData.fields.get(j).desc.equals(mn.desc)) {
						System.out.println("Spliced in FIELD: " + targetClassName + "." + mn.name);
						nodeData.fields.set(j, mn);
						added = true;
						break;
					}
				}

				if (!added) {
					System.out.println("Added FIELD: " + targetClassName + "." + mn.name);
					nodeData.fields.add(mn);
					added = true;
				}
			}
		}

		return nodeData;
	}
}
