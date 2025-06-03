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
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
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
import pl.asie.stackup.config.ConfigUtils;
import pl.asie.stackup.script.*;

import java.io.File;
import java.util.Objects;

@Mod(
		modid = "stackup",
		name = "StackUp",
		version = StackUp.VERSION,
		dependencies = "before:refinedstorage;before:mantle;before:ic2;before:appliedenergistics2;before:actuallyadditions",
		guiFactory = "pl.asie.stackup.config.ConfigGuiFactory"
)
public class StackUp {
	static final String VERSION = "@VERSION@";

	@SidedProxy(modId = "stackup", clientSide = "pl.asie.stackup.ProxyClient", serverSide = "pl.asie.stackup.ProxyCommon")
	public static ProxyCommon proxy;
	public static Logger logger;

	static int maxStackSize = 64;

	private static File stackupScriptLocation;
	private static Configuration config;
	private boolean hadPostInit;

	public static Configuration getConfig() {
		return config;
	}

	private void handleConfigChanged(boolean runtime) {
		if (!runtime) {
			StackUpConfig.scriptingActive = ConfigUtils.getBoolean(config, "general", "enableScripting", true, "Enable StackUp's own rules/scripting format.", true);
			maxStackSize = ConfigUtils.getInt(config, "general", "maxStackSize", 64, 64, 999999999, "The maximum stack size for new stacks.", true);

			StackUpConfig.coremodPatchRefinedStorage = ConfigUtils.getBoolean(config, "modpatches", "refinedstorage", true, "Should Refined Storage be patched to support large stacks? (GUI extraction only; works fine otherwise).", true);
			StackUpConfig.coremodPatchMantle = ConfigUtils.getBoolean(config, "modpatches", "mantle", true, "Should Mantle (Tinkers' Construct, etc.) be patched to support large stacks?", true);
			StackUpConfig.coremodPatchIc2 = ConfigUtils.getBoolean(config, "modpatches", "industrialcraft2", true, "Should IndustrialCraft 2 be patched to support large stacks?", true);
			StackUpConfig.coremodPatchAppliedEnergistics2 = ConfigUtils.getBoolean(config, "modpatches", "appliedenergistics2", true, "Should Applied Energistics 2 be patched to support large stacks?", true);
			StackUpConfig.coremodPatchActuallyAdditions = ConfigUtils.getBoolean(config, "modpatches", "actuallyadditions", true, "Should Actually Additions be patched to support large stacks?", true);
			StackUpConfig.compatChiselsBits = ConfigUtils.getBoolean(config, "modpatches", "chiselsandbits", true, "Should Chisels & Bits bits automatically be adjusted by the mod to match the bit bag's stacking size?", true);
		}

		StackUpConfig.lowestScaleDown = ConfigUtils.getFloat(config, "client", "fontScaleMinimum", 0.6f, 0.0f, 1.0f, "Lower bound of the font scale used by StackUp.", false);
		StackUpConfig.highestScaleDown = ConfigUtils.getFloat(config, "client", "fontScaleMaximum", 0.6f, 0.0f, 1.0f, "Upper bound of the font scale used by StackUp.", false);
		StackUpConfig.scaleTextLinearly = ConfigUtils.getBoolean(config, "client", "fontScaleLinear", false, "Scale text linearly as opposed to by steps. Useful with SmoothFont.", false);

		StackUpConfig.equalScaleDown = Math.abs(StackUpConfig.lowestScaleDown - StackUpConfig.highestScaleDown) <= 0.001f;

		if (config.hasChanged()) {
			config.save();
		}
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if ("stackup".equals(event.getModID())) {
			handleConfigChanged(true);
		}
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		if (!StackUpConfig.coremodActive) {
			throw new RuntimeException("Cannot load StackUp - coremod not present!");
		}

		logger = LogManager.getLogger();

		config = new Configuration(event.getSuggestedConfigurationFile());
		handleConfigChanged(false);

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

	private static final TObjectIntMap<Item> oldStackValues = new TObjectIntHashMap<>();

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
