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

package pl.asie.fiftyforty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;

public class ProxyClient extends ProxyCommon {
	@Override
	public int getScaleFactor() {
		return new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
	}

	@Override
	public boolean forceSmallTooltip() {
		return getScaleFactor() <= 1 || super.forceSmallTooltip();
	}
}
