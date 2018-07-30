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
		version = "@VERSION@"
)
public class FiftyForty {
	static int maxStackSize = 64;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(new File(new File("config"), "fiftyforty.cfg"));

		maxStackSize = config.getInt("maxStackSize", "general", 64, 64, Integer.MAX_VALUE, "The maximum stack size for new stacks.");

		if (config.hasChanged()) {
			config.save();
		}

		Items.AIR.setMaxStackSize(maxStackSize);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		Items.REDSTONE.setMaxStackSize(256);
	}
}
