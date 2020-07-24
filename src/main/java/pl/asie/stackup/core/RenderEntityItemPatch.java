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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ListIterator;

public final class RenderEntityItemPatch {
	private RenderEntityItemPatch() {

	}

	public static void patchDistanceConstant(ClassNode node) {
		for (MethodNode mn : node.methods) {
			// LDC -0.09375
			// v
			// ALOAD 1
			// INVOKESTATIC getItemRenderDistance(Lnet/minecraft/entity/item/EntityItem;)F
			if ("doRender".equals(mn.name)
					|| "func_76986_a".equals(mn.name)) {
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
									"pl/asie/stackup/client/StackUpClientHelpers",
									isNegative ? "getItemRenderDistanceNeg" : "getItemRenderDistance",
									"(Lnet/minecraft/entity/item/EntityItem;)F",
									false
							));
							System.out.println("Patched item render distance constant in RenderEntityItem!");
						}
					}
				}
			}
		}
	}
}
