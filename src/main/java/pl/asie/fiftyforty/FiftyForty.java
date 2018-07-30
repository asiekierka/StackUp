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

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

@Mod(
		modid = "fiftyforty",
		name = "5040",
		version = "@VERSION@",
		dependencies = "before:refinedstorage"
)
public class FiftyForty {
	static int maxStackSize = 64;
	static boolean patchRefinedStorage = true;
	private static int fontScaleLevel = -1;

	public static int getFontScaleLevel() {
		if (fontScaleLevel >= 0) {
			return fontScaleLevel;
		} else {
			return maxStackSize > 999 ? 2 : 3;
		}
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(new File(new File("config"), "fiftyforty.cfg"));

		maxStackSize = config.getInt("maxStackSize", "general", 64, 64, 999999999, "The maximum stack size for new stacks.");
		patchRefinedStorage = config.getBoolean("refinedstorage", "modpatches", true, "Should Refined Storage be patched to support large stacks? (GUI extraction only; works fine otherwise).");
		fontScaleLevel = config.getInt("fontScaleLevel", "client", -1, -1, 3, "Above how many digits should the font be scaled down? -1 sets default (2 if maxStackSize > 999, 3 otherwise). Set 0 to have the text be permanently scaled down.");

		if (config.hasChanged()) {
			config.save();
		}

		Items.AIR.setMaxStackSize(maxStackSize);
		Items.REDSTONE.setMaxStackSize(9999);
	}
}
