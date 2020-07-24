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

package pl.asie.stackup.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import pl.asie.stackup.StackUp;
import pl.asie.stackup.StackUpConfig;
import pl.asie.stackup.StackUpHelpers;

public final class StackUpClientHelpers {
	public static final int SLOT_MAX_WIDTH = 16;

	private StackUpClientHelpers() {

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
		if (text.length() == 0) {
			return 0;
		}

		if (StackUpTextGenerator.getStringLenWithoutFmtCodes(text) <= 2) {
			// normal vanilla size, use normal vanilla drawing
			return fr.drawStringWithShadow(text, x, y, color);
		}

		float xOffset = 19 - 2 - fr.getStringWidth(text);
		float yOffset = 6 + 3;

		x -= xOffset;
		y -= yOffset;
		StackUpTextGenerator.AbbreviationResult result = StackUpTextGenerator.abbreviate(fr, text, SLOT_MAX_WIDTH, false);

		float scaleDiff = result.getScaleFactor();

		x += 16 - (fr.getStringWidth(result.getText()) * scaleDiff);
		y += 16 - (8 * scaleDiff);

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0f);
		GlStateManager.scale(scaleDiff, scaleDiff, 1f);
		GlStateManager.translate(-x, -y, 0.0f);

		int i = fr.drawStringWithShadow(result.getText(), x, y, color);

		GlStateManager.popMatrix();
		return i;
	}
}
