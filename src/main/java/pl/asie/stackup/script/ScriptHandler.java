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

package pl.asie.stackup.script;

import com.google.common.collect.Ordering;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import pl.asie.stackup.StackUp;

import java.io.*;
import java.util.*;

public class ScriptHandler {
	protected static void cutWhitespace(PushbackReader r) throws IOException {
		int c;
		//noinspection StatementWithEmptyBody
		while (Character.isWhitespace((c = r.read()))) { }
		r.unread(c);
	}

	protected void processFile(RegistryNamespaced<ResourceLocation, Item> registry, File file) {
		System.out.println("Parsing " + file.getName());

		try (InputStream stream = new FileInputStream(file)) {
			new ScriptContext(registry, stream, TokenProvider.INSTANCE).execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void processDirectory(RegistryNamespaced<ResourceLocation, Item> registry, File file) {
		Set<File> files = new TreeSet<>(Ordering.natural());
		for (File f : Objects.requireNonNull(file.listFiles())) {
			files.add(f);
		}

		for (File f : files) {
			if (f.isDirectory()) {
				processDirectory(registry, f);
			} else {
				processFile(registry, f);
			}
		}
	}

	public void process(RegistryNamespaced<ResourceLocation, Item> registry, File baseDir) {
		if (baseDir == null || !baseDir.exists() || !baseDir.isDirectory()) {
			return;
		}

		processDirectory(registry, baseDir);
	}
}
