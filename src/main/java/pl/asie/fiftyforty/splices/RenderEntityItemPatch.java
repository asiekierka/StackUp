/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * This file is part of FiftyForty.
 *
 * FiftyForty is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FiftyForty is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FiftyForty.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.fiftyforty.splices;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class RenderEntityItemPatch extends RenderEntityItem {
	public RenderEntityItemPatch(RenderManager renderManagerIn, RenderItem p_i46167_2_) {
		super(renderManagerIn, p_i46167_2_);
	}

	@Override
	protected int getModelCount(ItemStack stack) {
		if (stack.getCount() > 2) {
			return MathHelper.log2DeBruijn(stack.getCount());
		} else {
			return stack.getCount();
		}
	}
}
