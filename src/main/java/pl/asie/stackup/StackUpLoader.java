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

import net.minecraft.item.Item;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import pl.asie.stackup.script.ScriptContext;
import pl.asie.stackup.script.TokenProvider;

import java.io.IOException;

public class StackUpLoader implements IResourceManagerReloadListener {
	@Override
	public void reload(IResourceManager manager) {
		StackUp.restoreStackSizes();

		boolean setAny = false;
		for (ResourceLocation location : manager.getAllResourceLocations("scripts", (s) -> s.endsWith(".stackup"))) {
			try {
				IResource resource = manager.getResource(location);
				new ScriptContext(Item.REGISTRY, resource.getInputStream(), TokenProvider.INSTANCE).execute();
				setAny = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		StackUp.findMaxStackSize(setAny);
	}
}
