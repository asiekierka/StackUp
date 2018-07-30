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

package pl.asie.stackup;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class StackUpHelpers {
	public static boolean coremodUp = false;
	public static final Random RANDOM = new Random();

	private StackUpHelpers() {

	}

	public static int getModelCount(ItemStack stack) {
		if (stack.getCount() > 2) {
			return MathHelper.log2DeBruijn(stack.getCount());
		} else {
			return stack.getCount();
		}
	}

	@SuppressWarnings("unused")
	public static float getItemRenderDistanceNeg(EntityItem item) {
		return -getItemRenderDistance(item);
	}

	@SuppressWarnings("unused")
	public static float getItemRenderDistance(EntityItem item) {
		int mc = getModelCount(item.getItem());
		if (mc <= 2) {
			return 0.09375F;
		} else {
			return 0.125F / MathHelper.sqrt(mc - 1);
		}
	}

	@SuppressWarnings("unused")
	public static int drawItemCountWithShadow(FontRenderer fr, String text, float x, float y, int color) {
		x = x - 19 + 2 + fr.getStringWidth(text);

		boolean forceSmall = StackUp.proxy.forceSmallTooltip();
		text = StackUp.abbreviate(text, forceSmall);

		if (forceSmall || text.length() <= StackUp.getFontScaleLevel()) {
			x += 19 - 2 - fr.getStringWidth(text);
			return fr.drawStringWithShadow(text, x, y, color);
		}

		GlStateManager.pushMatrix();
		GlStateManager.scale(0.5f, 0.5f, 1f);
		x *= 2;
		y *= 2;
		x = x + 32 - fr.getStringWidth(text);
		y += 6;

		int scaleFactor = StackUp.proxy.getScaleFactor();
		if ((scaleFactor & 1) == 1) {
			float difference = (scaleFactor - 1) / (float) scaleFactor;

			GlStateManager.translate(x, y, 0);
			GlStateManager.scale(difference, difference, 1f);
			x = fr.getStringWidth(text) / 2f;
			y = 3;
		}

		int i = fr.drawStringWithShadow(text, x, y, color);

		GlStateManager.popMatrix();
		return i;
	}

	@SuppressWarnings("unused")
	public static int getMaxStackSize() {
		return StackUp.maxStackSize;
	}
}
