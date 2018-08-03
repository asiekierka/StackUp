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

package pl.asie.stackup.script.rule;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

public abstract class Rule implements INBTSerializable<NBTTagCompound> {
	private boolean applied = false;

	public final boolean apply() {
		if (applied) {
			throw new RuntimeException("Cannot apply rule twice!");
		} else {
			if (applyInternal()) {
				applied = true;
				return true;
			} else {
				return false;
			}
		}
	}

	public final boolean undo() {
		if (!applied) {
			throw new RuntimeException("Cannot undo an unapplied rule!");
		} else {
			if (undoInternal()) {
				applied = false;
				return true;
			} else {
				return false;
			}
		}
	}

	protected abstract boolean applyInternal();
	protected abstract boolean undoInternal();
}
