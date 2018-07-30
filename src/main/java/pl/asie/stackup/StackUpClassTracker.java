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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class StackUpClassTracker {
	private static Map<String, String> superclassMap = new HashMap<>();
	private static Multimap<String, String> interfaceMap = HashMultimap.create();

	public static void addClass(String currC) {
		if (!superclassMap.containsKey(currC)) {
			String filename = FMLDeobfuscatingRemapper.INSTANCE.unmap(currC.replace('.', '/'));
			filename = filename.replace('.', '/') + ".class";
			InputStream stream = StackUpClassTracker.class.getClassLoader().getResourceAsStream(filename);
			if (stream != null) {
				try {
					ClassReader reader = new ClassReader(stream);
					String newC = reader.getSuperName();
					if (newC != null) {
						newC = FMLDeobfuscatingRemapper.INSTANCE.map(newC).replace('/', '.');
						superclassMap.put(currC, newC);
					}
					for (String s : reader.getInterfaces()) {
						String newI = FMLDeobfuscatingRemapper.INSTANCE.map(s).replace('/', '.');
						interfaceMap.put(currC, newI);
					}
				} catch (IOException e) {
					e.printStackTrace();
					superclassMap.put(currC, null);
				}
			} else {
//				System.err.println("Could not find " + filename + "!");
//				superclassMap.put(currC, null);
			}
		}
	}

	public static boolean isImplements(String c, String sc) {
		String currC = c;
		while (currC != null && currC.length() > 0) {
			if (currC.equals(sc)) {
				return true;
			} else {
				addClass(currC);
				for (String s : interfaceMap.get(currC)) {
					if (isImplements(s, sc)) {
						return true;
					}
				}
				currC = superclassMap.get(currC);
			}
		}

		return false;
	}

	public static boolean isExtends(String c, String sc) {
		String currC = c;
		while (currC != null && currC.length() > 0) {
			if (currC.equals(sc)) {
				return true;
			} else {
				addClass(currC);
				currC = superclassMap.get(currC);
			}
		}

		return false;
	}
}
