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

package pl.asie.stackup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pl.asie.stackup.client.StackUpClientHelpers;
import pl.asie.stackup.client.StackUpTextGenerator;

public class ProxyClient extends ProxyCommon {
	@SubscribeEvent
	public void onTooltip(ItemTooltipEvent event) {
		FontRenderer renderer = event.getItemStack().getItem().getFontRenderer(event.getItemStack());
		if (renderer == null) {
			renderer = Minecraft.getMinecraft().fontRenderer;
			if (renderer == null) {
				return;
			}
		}
		String count = Integer.toString(event.getItemStack().getCount());
		StackUpTextGenerator.AbbreviationResult countA = StackUpTextGenerator.abbreviate(renderer, count, StackUpClientHelpers.SLOT_MAX_WIDTH, true);
		if (countA.isAbbreviated()) {
			event.getToolTip().add("x " + count);
		}
	}

	@Override
	public int getCurrentScaleFactor() {
		return new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
	}
}
