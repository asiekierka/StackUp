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

package pl.asie.stackup.asm;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.dimdev.rift.listener.DispenserBehaviorAdder;
import org.dimdev.riftloader.listener.InitializationListener;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.List;

public class StackUpCore implements InitializationListener {
	@Override
	public void onInitialization() {
		//noinspection unchecked
		((List<String>) Launch.blackboard.get("TweakClasses")).add("pl.asie.stackup.asm.StackUpTweaker");
		Mixins.addConfiguration("mixins.stackup.json");
	}
}