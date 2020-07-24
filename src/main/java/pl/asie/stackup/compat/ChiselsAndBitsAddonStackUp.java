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

package pl.asie.stackup.compat;

import mod.chiselsandbits.api.ChiselsAndBitsAddon;
import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.api.IChiselsAndBitsAddon;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import pl.asie.stackup.StackUp;
import pl.asie.stackup.StackUpHelpers;

import java.lang.reflect.Field;

@ChiselsAndBitsAddon
public class ChiselsAndBitsAddonStackUp implements IChiselsAndBitsAddon {
	private static Item cb_block_bit, cb_bit_bag;

	private int getBitStackSize(IChiselAndBitsAPI api) {
		// Idea 1: Query the bit bag, if present.
		if (cb_bit_bag != null) {
			IBitBag bag = api.getBitbag(new ItemStack(cb_bit_bag));
			if (bag != null) {
				return bag.getBitbagStackSize();
			}
		}

		// Idea 2: Reflect into the mod. (Sorry, Algo.)
		try {
			Class coreClass = Class.forName("mod.chiselsandbits.core.ChiselsAndBits");
			if (coreClass != null) {
				Object config = coreClass.getMethod("getConfig").invoke(null);
				Field bagStackSizeField = config.getClass().getField("bagStackSize");
				bagStackSizeField.setAccessible(true);
				int v = (int) bagStackSizeField.get(config);
				return v;
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return -1;
	}

	@Override
	public void onReadyChiselsAndBits(IChiselAndBitsAPI api) {
		if (!StackUp.compatChiselsBits) {
			return;
		}

		cb_block_bit = Item.getByNameOrId("chiselsandbits:block_bit");
		if (cb_block_bit != null) {
			cb_bit_bag = Item.getByNameOrId("chiselsandbits:bit_bag");
			int size = Math.min(StackUpHelpers.getMaxStackSize(), getBitStackSize(api));
			if (size > cb_block_bit.getItemStackLimit(new ItemStack(cb_block_bit))) {
				System.out.println("StackUp: Setting max stack size of C&B bits to " + size);
				cb_block_bit.setMaxStackSize(size);
			}
		}
	}
}
