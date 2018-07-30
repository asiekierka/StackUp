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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

@Mod(
		modid = "fiftyforty",
		name = "5040",
		version = FiftyForty.VERSION,
		dependencies = "before:refinedstorage"
)
public class FiftyForty {
	static final String VERSION = "@VERSION@";

	@SidedProxy(modId = "fiftyforty", clientSide = "pl.asie.fiftyforty.ProxyClient", serverSide = "pl.asie.fiftyforty.proxyCommon")
	public static ProxyCommon proxy;

	static int maxStackSize = 64;
	static boolean patchRefinedStorage = true;
	private static boolean alwaysAbbreviate = false;
	private static int fontScaleLevel = -1;

	public static int getFontScaleLevel() {
		if (fontScaleLevel >= 0) {
			return fontScaleLevel;
		} else if (fontScaleLevel == -1) {
			return (maxStackSize > 999 && !alwaysAbbreviate) ? 2 : 3;
		} else {
			return (maxStackSize > 99 && !alwaysAbbreviate) ? 0 : 3;
		}
	}

	public static String abbreviate(String count) {
		return abbreviate(count, proxy.forceSmallTooltip());
	}

	public static String abbreviate(String count, boolean forceSmall) {
		boolean smallAbbr = alwaysAbbreviate || forceSmall;
		int maxLen = (smallAbbr ? 3 : 5);

		if (count.length() <= maxLen) {
			return count;
		}

		int countI = Integer.parseInt(count);
		if (smallAbbr) {
			if (countI >= 1000 && countI <= 99999) {
				count = (countI / 1000) + "k";
			} else if (countI >= 100000 && countI <= 999999) {
				count = "." + (countI / 100000) + "m";
			} else if (countI >= 1000000 && countI <= 99999999) {
				count = (countI / 1000000) + "m";
			} else if (countI >= 100000000 && countI <= 999999999) {
				count = "." + (countI / 100000000) + "g";
			} else if (countI >= 1000000000) {
				count = (countI / 1000000000) + "g";
			}
		} else {
			if (countI >= 100000 && countI <= 999999) {
				count = (countI / 1000) + "K";
			} else if (countI >= 1000000 && countI <= 9999999) {
				int a = (countI / 10000);
				count = (a / 100) + "." + String.format("%02d", (a % 100)) + "M";
			} else if (countI >= 10000000 && countI <= 99999999) {
				int a = (countI / 100000);
				count = (a / 10) + "." + (a % 10) + "M";
			} else if (countI >= 100000000 && countI <= 999999999) {
				int a = (countI / 1000000);
				count = a + "M";
			} else if (countI >= 1000000000) {
				int a = (countI / 10000000);
				count = (a / 100) + "." + String.format("%02d", (a % 100)) + "M";
			}
		}

		return count;
	}

	@SubscribeEvent
	public void onTooltip(ItemTooltipEvent event) {
		String count = Integer.toString(event.getItemStack().getCount());
		String countA = abbreviate(count);
		//noinspection StringEquality
		if (count != countA) {
			event.getToolTip().add("x " + count);
		}
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(new File(new File("config"), "fiftyforty.cfg"));

		maxStackSize = config.getInt("maxStackSize", "general", 64, 64, 999999999, "The maximum stack size for new stacks.");
		patchRefinedStorage = config.getBoolean("refinedstorage", "modpatches", true, "Should Refined Storage be patched to support large stacks? (GUI extraction only; works fine otherwise).");
		fontScaleLevel = config.getInt("fontScaleLevel", "client", -2, -2, 3, "Above how many digits should the font be scaled down? -1 sets \"dynamic logic\" - 2 if maxStackSize > 999, 3 otherwise. -2 scales if maxStackSize > 99. Set 0 to have the text be permanently scaled down.");
		alwaysAbbreviate = config.getBoolean("abbreviateAlways", "client", false, "Prefer abbreviation over scaling the font down.");

		if (config.hasChanged()) {
			config.save();
		}

		MinecraftForge.EVENT_BUS.register(this);

		Items.AIR.setMaxStackSize(maxStackSize);

		//noinspection ConstantConditions
		if (VERSION.equals("@VERSION@")) {
			System.out.println("--      WARNING!      --");
			System.out.println("5040 DEBUG MODE ENABLED!");
			System.out.println("--      WARNING!      --");
			Items.REDSTONE.setMaxStackSize(65536);
			Items.DIAMOND.setMaxStackSize(1000000);
			Items.QUARTZ.setMaxStackSize(999999999);
		}
	}
}
