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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

public final class RenderItemPatch {
	private RenderItemPatch() {

	}

	public static void patchDrawItemCount(ClassNode node) {
		for (MethodNode mn : node.methods) {
			if ("renderItemOverlayIntoGUI".equals(mn.name)
					|| "func_180453_a".equals(mn.name)) {
				ListIterator<AbstractInsnNode> it = mn.instructions.iterator();
				while (it.hasNext()) {
					AbstractInsnNode in = it.next();
					//     INVOKEVIRTUAL net/minecraft/client/gui/FontRenderer.drawStringWithShadow (Ljava/lang/String;FFI)I
					if (in.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						MethodInsnNode min = (MethodInsnNode) in;
						if (min.owner.equals("net/minecraft/client/gui/FontRenderer")
								&& min.desc.equals("(Ljava/lang/String;FFI)I")) {
							it.set(new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									"pl/asie/stackup/client/StackUpClientHelpers",
									"drawItemCountWithShadow",
									"(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;FFI)I",
									false
							));
							System.out.println("Patched item count render in RenderItem!");
							break;
						}
					}
				}
			}
		}
	}
}
