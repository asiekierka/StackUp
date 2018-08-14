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

import net.minecraft.client.Minecraft;

public class ProxyClient extends ProxyCommon {
	/* @SubscribeEvent
	public void onTooltip(ItemTooltipEvent event) {
		String count = Integer.toString(event.getItemStack().getCount());
		String countA = StackUp.abbreviate(count);
		//noinspection StringEquality
		if (count != countA) {
			event.getToolTip().add("x " + count);
		}
	} */

	@Override
	public int getScaleFactor() {
		/* return new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor(); */
		return 1;
	}

	@Override
	public boolean forceSmallTooltip() {
		return getScaleFactor() <= 1 || super.forceSmallTooltip();
	}
}
