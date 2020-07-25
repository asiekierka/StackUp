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

package pl.asie.stackup.config;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public final class ConfigUtils {
	private ConfigUtils() {

	}

	private static void prepareCategory(Configuration config, String category) {
		ConfigCategory configCategory = config.getCategory(category);
		configCategory.setLanguageKey("config.stackup." + category + ".name");
	}

	public static boolean getBoolean(Configuration config, String category, String name, boolean defaultValue, String comment, boolean requiresRestart) {
		prepareCategory(config, category);
		Property prop = config.get(category, name, defaultValue);
		prop.setComment(comment);
		prop.setRequiresMcRestart(requiresRestart);
		prop.setLanguageKey("config.stackup." + category + "." + name + ".name");
		return prop.getBoolean();
	}

	public static String getString(Configuration config, String category, String name, String defaultValue, String comment, boolean requiresRestart) {
		prepareCategory(config, category);
		Property prop = config.get(category, name, defaultValue);
		prop.setComment(comment);
		prop.setRequiresMcRestart(requiresRestart);
		prop.setLanguageKey("config.stackup." + name + ".name");
		return prop.getString();
	}

	public static String[] getStringList(Configuration config, String category, String name, String[] defaultValue, String comment, boolean requiresRestart) {
		prepareCategory(config, category);
		Property prop = config.get(category, name, defaultValue);
		prop.setComment(comment);
		prop.setRequiresMcRestart(requiresRestart);
		prop.setLanguageKey("config.stackup." + name + ".name");
		return prop.getStringList();
	}

	public static int getInt(Configuration config, String category, String name, int defaultValue, int minValue, int maxValue, String comment, boolean requiresRestart) {
		prepareCategory(config, category);
		Property prop = config.get(category, name, defaultValue);
		prop.setMinValue(minValue);
		prop.setMaxValue(maxValue);
		prop.setComment(comment);
		prop.setRequiresMcRestart(requiresRestart);
		prop.setLanguageKey("config.stackup." + name + ".name");
		return prop.getInt();
	}

	public static float getFloat(Configuration config, String category, String name, float defaultValue, float minValue, float maxValue, String comment, boolean requiresRestart) {
		return (float) getDouble(config, category, name, defaultValue, minValue, maxValue, comment, requiresRestart);
	}

	public static double getDouble(Configuration config, String category, String name, double defaultValue, double minValue, double maxValue, String comment, boolean requiresRestart) {
		prepareCategory(config, category);
		Property prop = config.get(category, name, defaultValue);
		prop.setMinValue(minValue);
		prop.setMaxValue(maxValue);
		prop.setComment(comment);
		prop.setRequiresMcRestart(requiresRestart);
		prop.setLanguageKey("config.stackup." + name + ".name");
		return prop.getDouble();
	}
}
