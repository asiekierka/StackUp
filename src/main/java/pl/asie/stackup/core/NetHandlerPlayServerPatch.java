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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

public final class NetHandlerPlayServerPatch {
	private NetHandlerPlayServerPatch() {

	}

	public static void patchCreativeInventory(ClassNode node) {
		for (MethodNode mn : node.methods) {
			if ("processCreativeInventoryAction".equals(mn.name)
					|| "func_147344_a".equals(mn.name)) {
				ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
				while (it.hasNext()) {
					AbstractInsnNode in = it.next();
					if (in.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						MethodInsnNode min = (MethodInsnNode) in;
						if (min.owner.equals("net/minecraft/item/ItemStack")
								&& (min.name.equals("getCount") || min.name.equals("func_190916_E"))) {
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
	}
}
