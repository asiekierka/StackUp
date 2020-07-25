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

import com.google.common.collect.ImmutableList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
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
import pl.asie.stackup.script.*;

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

	@SidedProxy(modId = "stackup", clientSide = "pl.asie.stackup.ProxyClient", serverSide = "pl.asie.stackup.ProxyCommon")
	public static ProxyCommon proxy;
	public static Logger logger;

	static int maxStackSize = 64;

	private static File stackupScriptLocation;
	private static boolean hadPostInit = false;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		if (!StackUpConfig.coremodActive) {
			throw new RuntimeException("Coremod not present!");
		}

		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		logger = LogManager.getLogger();

		StackUpConfig.scriptingActive = config.getBoolean("enableScripting", "general", true, "Enable StackUp's own rules/scripting format.");
		maxStackSize = config.getInt("maxStackSize", "general", 64, 64, 999999999, "The maximum stack size for new stacks.");

		StackUpConfig.coremodPatchRefinedStorage = config.getBoolean("refinedstorage", "modpatches", true, "Should Refined Storage be patched to support large stacks? (GUI extraction only; works fine otherwise).");
		StackUpConfig.coremodPatchMantle = config.getBoolean("mantle", "modpatches", true, "Should Mantle (Tinkers' Construct, etc.) be patched to support large stacks?");
		StackUpConfig.coremodPatchIc2 = config.getBoolean("industrialcraft2", "modpatches", true, "Should IndustrialCraft 2 be patched to support large stacks?");
		StackUpConfig.coremodPatchAppliedEnergistics = config.getBoolean("appliedenergistics", "modpatches", true, "Should Actually Additions be patched to support large stacks?");
		StackUpConfig.coremodPatchActuallyAdditions = config.getBoolean("actuallyadditions", "modpatches", true, "Should Actually Additions be patched to support large stacks?");
		StackUpConfig.compatChiselsBits = config.getBoolean("chiselsandbits", "modpatches", true, "Should Chisels & Bits bits automatically be adjusted by the mod to match the bit bag's stacking size?");

		StackUpConfig.lowestScaleDown = config.getFloat("fontScaleMinimum", "client", 0.5f, 0.0f, 1.0f, "Lower bound of the font scale used by StackUp.");
		StackUpConfig.highestScaleDown = config.getFloat("fontScaleMaximum", "client", 1.0f, 0.0f, 1.0f, "Upper bound of the font scale used by StackUp.");
		StackUpConfig.scaleTextLinearly = config.getBoolean("fontScaleLinear", "client", false, "Scale text linearly as opposed to by steps. Useful with SmoothFont.");

		if (config.hasChanged()) {
			config.save();
		}

		stackupScriptLocation = new File(event.getModConfigurationDirectory(), "stackup");
		if (StackUpConfig.scriptingActive && !stackupScriptLocation.exists()) {
			//noinspection ResultOfMethodCallIgnored
			stackupScriptLocation.mkdir();
		}

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(proxy);

		Items.AIR.setMaxStackSize(maxStackSize);

		TokenProvider.INSTANCE.addToken("isBlock", () -> new TokenBoolean<Item>(
				(i) -> i instanceof ItemBlock || (Block.getBlockFromItem(i) != Blocks.AIR)
		));
		TokenProvider.INSTANCE.addToken("blockClass", () -> new TokenClass<Item>((i) -> {
			if (i instanceof ItemBlock) {
				Block b = Block.getBlockFromItem(i);
				if (b != Blocks.AIR) {
					return ImmutableList.of(b.getClass());
				}
			}

			return ImmutableList.of();
		}, false));
		TokenProvider.INSTANCE.addToken("itemClass", () -> new TokenClass<Item>((i) -> ImmutableList.of(i.getClass()), false));
		TokenProvider.INSTANCE.addToken("id", () -> new TokenResourceLocation<Item>((i) -> ImmutableList.of(Objects.requireNonNull(i.getRegistryName()).toString())));
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
		if (StackUpConfig.scriptingActive) {
			new ScriptHandler().process(registry, stackupScriptLocation);
		}
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
