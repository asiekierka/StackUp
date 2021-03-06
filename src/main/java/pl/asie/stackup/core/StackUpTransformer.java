/*
 * Copyright (c) 2018, 2020 Adrian Siekierka
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

package pl.asie.stackup.core;

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
import pl.asie.stackup.StackUpConfig;
import pl.asie.stackup.StackUpCore;

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

		if (StackUpClassTracker.isImplements(transformedName, "net.minecraft.inventory.IInventory")) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("getInventoryStackLimit", "func_70297_j_"));
		}

		if (StackUpClassTracker.isImplements(transformedName, "net.minecraftforge.items.IItemHandler")) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("getSlotLimit"));
		}

		if (StackUpClassTracker.isExtends(transformedName, "net.minecraft.inventory.Slot")) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit(
					"getItemStackLimit", "func_178170_b",
					"getSlotStackLimit", "func_75219_a"
			));
		}

		if (StackUpConfig.coremodPatchRefinedStorage && transformedName.startsWith("com.raoulvdberge.refinedstorage.apiimpl.network.grid.handler.ItemGridHandler")) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("onExtract"));
		} else if (StackUpConfig.coremodPatchMantle && "slimeknights.mantle.tileentity.TileInventory".equals(transformedName)) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("<init>"));
		} else if (StackUpConfig.coremodPatchIc2 && "ic2.core.block.invslot.InvSlot".equals(transformedName)) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("<init>"));
		} else if (StackUpConfig.coremodPatchAppliedEnergistics2 && "appeng.tile.inventory.AppEngInternalInventory".equals(transformedName)) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("<init>"));
		} else if (StackUpConfig.coremodPatchAppliedEnergistics2 && "appeng.tile.inventory.AppEngInternalAEInventory".equals(transformedName)) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("<init>"));
		} else if (StackUpConfig.coremodPatchActuallyAdditions && "de.ellpeck.actuallyadditions.mod.tile.TileEntityInventoryBase".equals(transformedName)) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("getMaxStackSize"));
		} else if ("net.minecraft.client.renderer.entity.RenderEntityItem".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				spliceClasses(node, "pl.asie.stackup.core.RenderEntityItemSplice",
						"getModelCount", "func_177078_a");
				RenderEntityItemPatch.patchDistanceConstant(node);
			});
		} else if ("net.minecraft.inventory.InventoryHelper".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				spliceClasses(node, "pl.asie.stackup.core.InventoryHelperPerformanceSplice",
						"spawnItemStack", "func_180173_a");
			});
		} else if ("net.minecraft.util.ServerRecipeBookHelper".equals(transformedName)) {
			consumer = consumer.andThen(MaxStackConstantPatch.patchMaxLimit("func_194324_a"));
		} else if ("net.minecraft.network.PacketBuffer".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				spliceClasses(node, "pl.asie.stackup.core.PacketBufferWriterSplice",
						"readItemStack", "func_150791_c",
						"writeItemStack", "func_150788_a");
			});
		} else if ("net.minecraft.client.renderer.RenderItem".equals(transformedName)) {
			consumer = consumer.andThen(RenderItemPatch::patchDrawItemCount);
		} else if ("net.minecraft.network.NetHandlerPlayServer".equals(transformedName)) {
			consumer = consumer.andThen(NetHandlerPlayServerPatch::patchCreativeInventory);
		} else if ("net.minecraftforge.common.util.PacketUtil".equals(transformedName)) {
			consumer = consumer.andThen((node) -> {
				spliceClasses(node, "pl.asie.stackup.core.PacketUtilWriterSplice", "writeItemStackFromClientToServer");
			});
		} else if ("net.minecraft.item.ItemStack".equals(transformedName)) {
			consumer = consumer.andThen(ItemStackPatch::patchCountGetSet);
		}

		if (consumer != emptyConsumer) {
			return processNode(basicClass, consumer);
		} else {
			return data;
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
		try (InputStream stream = StackUpCore.class.getClassLoader().getResourceAsStream(className.replace('.', '/') + ".class")) {
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
