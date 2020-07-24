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

package pl.asie.stackup.script.rule;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import pl.asie.stackup.StackUp;
import pl.asie.stackup.StackUpHelpers;

public class RuleChangeStackSize extends Rule {
	private Item item;
	private int newStackSize;
	private int oldValue;

	public RuleChangeStackSize() {

	}

	public RuleChangeStackSize(Item item, int newStackSize) {
		this.item = item;
		this.newStackSize = newStackSize;
	}

	@Override
	protected boolean applyInternal() {
		if (newStackSize < 0 || newStackSize > StackUpHelpers.getMaxStackSize()) {
			return false;
		}

		oldValue = item.getItemStackLimit();
		item.setMaxStackSize(newStackSize);
		if (newStackSize != oldValue && item.getItemStackLimit() == oldValue) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	protected boolean undoInternal() {
		int oldValueCmp = item.getItemStackLimit();
		item.setMaxStackSize(oldValue);
		if (oldValueCmp != oldValue && item.getItemStackLimit() == oldValueCmp) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setString("id", item.getRegistryName().toString());
		compound.setInteger("size", newStackSize);
		return compound;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString("id")));
		newStackSize = nbt.getInteger("size");
	}
}
