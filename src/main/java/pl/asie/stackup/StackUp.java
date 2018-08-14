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

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.pattern.BlockTagMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import org.dimdev.rift.listener.MinecraftStartListener;
import pl.asie.stackup.script.*;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class StackUp {
	public static final int MAX_MAX_STACK_SIZE = 1048576;
	public static int maxStackSize = 64;
	public static boolean alwaysAbbreviate = false;
	public static int fontScaleLevel = -2;

	// TODO
	public static ProxyCommon proxy = new ProxyCommon();

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

	private static Field mssField;

	public static void setMaxStackSize(Item i, int target) {
		// thanks mojang
		try {
			if (mssField == null) {
				try {
					mssField = Item.class.getDeclaredField("maxStackSize");
				} catch (NoSuchFieldException e) {
					try {
						//noinspection JavaReflectionMemberAccess
						mssField = Item.class.getDeclaredField("m");
					} catch (NoSuchFieldException ee) {
						throw new RuntimeException(ee);
					}
				}
				mssField.setAccessible(true);
			}
			mssField.set(i, target);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void load() {
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
		TokenProvider.INSTANCE.addToken("id", () -> new TokenResourceLocation<Item>((i) -> ImmutableList.of(Objects.requireNonNull(Item.REGISTRY.getIDForObject(i)).toString())));
		TokenProvider.INSTANCE.addToken("tag", () -> new TokenResourceLocation<Item>((i) -> ItemTags.getCollection().getTags().stream().filter((t) -> i.isTagged(Objects.requireNonNull(ItemTags.getCollection().getTag(t)))).map(ResourceLocation::toString).collect(Collectors.toList())));

		/* Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		maxStackSize = config.getInt("maxStackSize", "general", 64, 64, 999999999, "The maximum stack size for new stacks.");
		StackUpCoremodGlue.patchRefinedStorage = config.getBoolean("refinedstorage", "modpatches", true, "Should Refined Storage be patched to support large stacks? (GUI extraction only; works fine otherwise).");
		compatChiselsBits = config.getBoolean("chiselsandbits", "modpatches", true, "Should Chisels & Bits bits automatically be adjusted by the mod to match the bit bag's stacking size?");
		fontScaleLevel = config.getInt("fontScaleLevel", "client", -2, -2, 3, "Above how many digits should the font be scaled down? -1 sets \"dynamic logic\" - 2 if maxStackSize > 999, 3 otherwise. -2 scales if maxStackSize > 99. Set 0 to have the text be permanently scaled down.");
		alwaysAbbreviate = config.getBoolean("abbreviateAlways", "client", false, "Prefer abbreviation over scaling the font down.");

		if (config.hasChanged()) {
			config.save();
		} */
	}

	private static Object2IntMap<Item> oldStackValues = new Object2IntOpenHashMap<>();

	public static void backupStackSize(Item i) {
		if (!oldStackValues.containsKey(i)) {
			oldStackValues.put(i, i.getItemStackLimit());
		}
	}

	public static void restoreStackSizes() {
		for (Item i : oldStackValues.keySet()) {
			setMaxStackSize(i, oldStackValues.get(i));
		}
		oldStackValues.clear();
	}

	public static void findMaxStackSize(boolean search) {
		int mss = 64;

		if (search) {
			for (Item i : Item.REGISTRY) {
				mss = Math.max(i.getItemStackLimit(), mss);
			}
		}

		maxStackSize = mss;
		setMaxStackSize(Items.AIR, maxStackSize);
	}
}
