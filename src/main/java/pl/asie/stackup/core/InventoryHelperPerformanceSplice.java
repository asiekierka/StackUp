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

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import pl.asie.stackup.StackUpHelpers;

public class InventoryHelperPerformanceSplice {
	public static void func_180173_a(World world, double x, double y, double z, ItemStack stack) {
		spawnItemStack(world, x, y, z, stack);
	}

	public static void spawnItemStack(World world, double x, double y, double z, ItemStack stack) {
		float xOffset = StackUpHelpers.RANDOM.nextFloat() * 0.8F + 0.1F;
		float yOffset = StackUpHelpers.RANDOM.nextFloat() * 0.8F + 0.1F;
		float zOffset = StackUpHelpers.RANDOM.nextFloat() * 0.8F + 0.1F;

		int minStackSize = (stack.getMaxStackSize() + 7) / 8;
		int maxStackSize = (stack.getMaxStackSize() + 1) / 2;

		if (stack.getCount() <= 8) {
			minStackSize = stack.getCount();
			maxStackSize = stack.getCount();
		}

		while (!stack.isEmpty()) {
			EntityItem entityitem = new EntityItem(world,
					x + xOffset, y + yOffset, z + zOffset,
					stack.splitStack(StackUpHelpers.RANDOM.nextInt(maxStackSize - minStackSize + 1) + minStackSize)
			);
			entityitem.motionX = StackUpHelpers.RANDOM.nextGaussian() * 0.05D;
			entityitem.motionY = StackUpHelpers.RANDOM.nextGaussian() * 0.05D + 0.2D;
			entityitem.motionZ = StackUpHelpers.RANDOM.nextGaussian() * 0.05D;
			world.spawnEntity(entityitem);
		}
	}
}
