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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.asie.stackup.script.ScriptHandler;
import pl.asie.stackup.script.TokenNumeric;
import pl.asie.stackup.script.TokenProvider;
import pl.asie.stackup.script.TokenResourceLocation;

import java.io.File;
import java.util.Objects;

@Mod(
		modid = "stackup",
		name = "StackUp",
		version = StackUp.VERSION,
		dependencies = "before:refinedstorage"
)
public class StackUp {
	static final String VERSION = "@VERSION@";

	@SidedProxy(modId = "stackup", clientSide = "pl.asie.stackup.ProxyClient", serverSide = "pl.asie.stackup.proxyCommon")
	public static ProxyCommon proxy;
	public static Logger logger;

	public static boolean compatChiselsBits = true;
	static int maxStackSize = 64;
	private static boolean alwaysAbbreviate = false;
	private static int fontScaleLevel = -1;

	private static File stackupScriptLocation;
	private static boolean hadPostInit = false;

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
		String oldString = count;

		StringBuilder fmtCodes = new StringBuilder();
		int fmtCodeCount = 0;
		while (count.codePointAt(0) == 0xA7) {
			fmtCodes.append(count, 0, 2);
			fmtCodeCount++;
			count = count.substring(2);
		}

		String newCount = abbreviateInner(count, forceSmall);
		//noinspection StringEquality
		if (newCount == count) {
			return oldString;
		} else if (fmtCodeCount == 0) {
			return newCount;
		} else {
			fmtCodes.append(newCount);
			return fmtCodes.toString();
		}
	}

	private static String abbreviateInner(String count, boolean forceSmall) {
		boolean smallAbbr = alwaysAbbreviate || forceSmall;
		int maxLen = (smallAbbr ? 3 : 5);

		if (count.length() <= maxLen) {
			return count;
		}

		int countI;
		try {
			countI = Integer.parseInt(count);
		} catch (NumberFormatException e) {
			return count;
		}

		if (smallAbbr) {
			if (countI >= 1000 && countI <= 99999) {
				count = (countI / 1000) + "k";
			} else if (countI >= 100000 && countI <= 999999) {
				count = "." + (countI / 100000) + "m";
			} else if (countI >= 1000000 && countI <= 99999999) {
				count = (countI / 1000000) + "m";
			} else if (countI >= 100000000 && countI <= 999999999) {
				count = "." + (countI / 100000000) + "b";
			} else if (countI >= 1000000000) {
				count = (countI / 1000000000) + "b";
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
				count = (a / 100) + "." + String.format("%02d", (a % 100)) + "B";
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
		if (!StackUpCoremodGlue.coremodUp) {
			throw new RuntimeException("Coremod not present!");
		}

		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		logger = LogManager.getLogger();

		maxStackSize = config.getInt("maxStackSize", "general", 64, 64, 999999999, "The maximum stack size for new stacks.");
		StackUpCoremodGlue.patchRefinedStorage = config.getBoolean("refinedstorage", "modpatches", true, "Should Refined Storage be patched to support large stacks? (GUI extraction only; works fine otherwise).");
		compatChiselsBits = config.getBoolean("chiselsandbits", "modpatches", true, "Should Chisels & Bits bits automatically be adjusted by the mod to match the bit bag's stacking size?");
		fontScaleLevel = config.getInt("fontScaleLevel", "client", -2, -2, 3, "Above how many digits should the font be scaled down? -1 sets \"dynamic logic\" - 2 if maxStackSize > 999, 3 otherwise. -2 scales if maxStackSize > 99. Set 0 to have the text be permanently scaled down.");
		alwaysAbbreviate = config.getBoolean("abbreviateAlways", "client", false, "Prefer abbreviation over scaling the font down.");

		if (config.hasChanged()) {
			config.save();
		}

		stackupScriptLocation = new File(event.getModConfigurationDirectory(), "stackup");
		MinecraftForge.EVENT_BUS.register(this);

		Items.AIR.setMaxStackSize(maxStackSize);

		//noinspection deprecation
		TokenProvider.INSTANCE.addToken("id", () -> new TokenResourceLocation<Item>((i) -> Objects.requireNonNull(i.getRegistryName()).toString()));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRegisterItems(RegistryEvent.Register<Item> event) {
		if (hadPostInit) {
			reload(event.getRegistry());
		}
	}

	private static TObjectIntMap<Item> oldStackValues = new TObjectIntHashMap<>();

	public static void backupStackSize(Item i) {
		if (!oldStackValues.containsKey(i)) {
			oldStackValues.put(i, i.getItemStackLimit());
		}
	}

	protected static void reload(IForgeRegistry<Item> registry) {
		for (Item i : oldStackValues.keySet()) {
			i.setMaxStackSize(oldStackValues.get(i));
		}
		oldStackValues.clear();
		new ScriptHandler().process(registry, stackupScriptLocation);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		reload(ForgeRegistries.ITEMS);
		hadPostInit = true;
	}

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandStackUp());
	}
}
