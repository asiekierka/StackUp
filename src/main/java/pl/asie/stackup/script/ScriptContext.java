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

package pl.asie.stackup.script;

import com.google.common.base.Charsets;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.tuple.Pair;
import pl.asie.stackup.StackUp;
import pl.asie.stackup.StackUpHelpers;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScriptContext {
	private final IForgeRegistry<Item> registry;
	private final TokenProvider tokenProvider;
	private final InputStreamReader streamReader;
	private final BufferedReader reader;
	private final TObjectIntMap<Item> stackSizeMap;

	public ScriptContext(IForgeRegistry<Item> registry, InputStream stream, TokenProvider provider) {
		this.registry = registry;
		this.tokenProvider = provider;
		this.reader = new BufferedReader(streamReader = new InputStreamReader(stream, Charsets.UTF_8));
		this.stackSizeMap = new TObjectIntHashMap<>();

		// hacks...
		provider.addToken("size", () -> new TokenNumeric<Item>((item) -> getStackSize(item, false)));
	}

	public void execute() {
		int parsed = 0;

		try {
			Iterator<String> it = reader.lines().iterator();
			while (it.hasNext()) {
				parseLine(it.next());
				parsed++;
			}
			reader.close();
			streamReader.close();
		} catch (IOException | TokenException e) {
			e.printStackTrace();
		}

		StackUp.logger.info("Parsed " + parsed + " lines.");

		applyChanges();
	}

	protected void parseLine(String line) throws IOException, TokenException {
		line = line.trim();
		if (line.length() == 0) {
			return;
		}

		if (line.startsWith("#")) {
			return;
		}

		PushbackReader reader = new PushbackReader(new StringReader(line), 2);

		List<Token> args = new ArrayList<>();
		int newStackSize = 0;
		int operator = '=';

		int i = 256; // eh
		while ((i--) > 0) {
			if (i < 256) {
				ScriptHandler.cutWhitespace(reader);
				int c = reader.read();
				if (c != ',') {
					reader.unread(c);
				} else {
					ScriptHandler.cutWhitespace(reader);
				}
			}

			Pair<String, Token> token = tokenProvider.getToken(reader);
			if (token.getRight() == null) {
				int c1 = reader.read();
				if (c1 == '-') {
					int c2 = reader.read();
					if (c2 == '>') {
						newStackSize = TokenNumeric.parseInteger(reader);
						break;
					} else if (c2 == '=') {
						newStackSize = TokenNumeric.parseInteger(reader);
						operator = c1;
						break;
					} else {
						reader.unread(c2);
					}
				} else if (c1 == '+' || c1 == '*' || c1 == '/') {
					int c2 = reader.read();
					if (c2 == '=') {
						newStackSize = TokenNumeric.parseInteger(reader);
						operator = c1;
						break;
					} else {
						reader.unread(c2);
					}
				} else {
					reader.unread(c1);
				}

				throw new TokenException("Token not found: " + token.getLeft() + "!");
			} else {
				token.getRight().parse(reader);
				args.add(token.getRight());
			}
		}

		if (newStackSize > 0) {
			for (Item item : registry) {
				boolean ok = true;
				for (Token t : args) {
					if (t.isInvert()) {
						if (t.apply(item)) {
							ok = false;
							break;
						}
					} else {
						if (!t.apply(item)) {
							ok = false;
							break;
						}
					}
				}

				if (ok) {
					switch (operator) {
						case '=':
							stackSizeMap.put(item, clamp(newStackSize));
							break;
						case '+':
							stackSizeMap.put(item, clamp(getStackSize(item, true) + newStackSize));
							break;
						case '-':
							stackSizeMap.put(item, clamp(getStackSize(item, true) - newStackSize));
							break;
						case '*':
							stackSizeMap.put(item, clamp(getStackSize(item, true) * newStackSize));
							break;
						case '/':
							stackSizeMap.put(item, clamp(getStackSize(item, true) / newStackSize));
							break;
					}
				}
			}
		}
	}

	protected int clamp(int v) {
		if (v < 1) return 1;
		else if (v > StackUpHelpers.getMaxStackSize()) return StackUpHelpers.getMaxStackSize();
		else return v;
	}

	protected int getStackSize(Item i, boolean withMap) {
		if (withMap && stackSizeMap.containsKey(i)) {
			return stackSizeMap.get(i);
		} else {
			//noinspection deprecation
			return i.getItemStackLimit();
		}
	}

	protected void applyChanges() {
		for (Item i : stackSizeMap.keySet()) {
			if (i == Items.AIR) {
				continue;
			}

			int target = stackSizeMap.get(i);
			StackUp.backupStackSize(i);
			i.setMaxStackSize(target);
			//noinspection deprecation
			int result = i.getItemStackLimit();
			if (target != result) {
				StackUp.logger.warn("Could not change stack size on item " + i.getRegistryName() + "!");
			}
		}

		stackSizeMap.clear();
	}
}
