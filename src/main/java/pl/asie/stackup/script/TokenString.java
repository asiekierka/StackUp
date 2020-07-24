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

import net.minecraft.item.Item;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.List;
import java.util.function.Function;

public class TokenString<T> extends Token<T> {
	protected final Function<T, List<String>> function;
	private final boolean ignoreCase;
	private ComparisonType type;
	private String s;

	public TokenString(Function<T, List<String>> function, boolean ignoreCase) {
		this.function = function;
		this.ignoreCase = ignoreCase;
	}

	protected boolean isInvalidComparisonType(ComparisonType type) {
		return type != ComparisonType.EQUAL && type != ComparisonType.APPROXIMATELY_EQUAL && type != ComparisonType.NOT_EQUAL;
	}

	@Override
	public void parse(PushbackReader reader) throws IOException, TokenException {
		type = getComparisonType(reader);
		if (isInvalidComparisonType(type)) {
			throw new TokenException("Unsupported comparison type " + type + "!");
		}

		StringBuilder builder = new StringBuilder();
		ScriptHandler.cutWhitespace(reader);
		int c = reader.read();
		if (c != '"') {
			throw new TokenException("Expected string beginning, " + c + " found!");
		}

		while ((c = reader.read()) != '"') {
			builder.appendCodePoint(c);
		}
		ScriptHandler.cutWhitespace(reader);
		s = builder.toString();
	}

	protected String getString() {
		return s;
	}

	protected ComparisonType getComparisonType() {
		return type;
	}

	protected boolean compare(String sReceived, String sSet) {
		switch (type) {
			case EQUAL:
			case NOT_EQUAL:
			default:
				return ignoreCase ? sReceived.equalsIgnoreCase(sSet) : sReceived.equals(sSet);
			case APPROXIMATELY_EQUAL:
				boolean hasStartStar = sSet.startsWith("*");
				boolean hasEndStar = sSet.endsWith("*");
				if (hasStartStar && hasEndStar) {
					if (sSet.length() == 1) {
						return true;
					} else {
						return sReceived.toLowerCase().contains(sSet.substring(1, sSet.length() - 1).toLowerCase());
					}
				} else if (hasStartStar) {
					return sReceived.toLowerCase().endsWith(sSet.substring(1, sSet.length()).toLowerCase());
				} else if (hasEndStar) {
					return sReceived.toLowerCase().startsWith(sSet.substring(0, sSet.length() - 1).toLowerCase());
				} else {
					return sReceived.equalsIgnoreCase(sSet);
				}
		}
	}

	@Override
	public boolean apply(T object) {
		for (String str : function.apply(object)) {
			if (compare(str, s)) {
				// if NOT_EQUAL, compare checks for EQUAL, we return FALSE
				// otherwise, we return TRUE
				return getComparisonType() != ComparisonType.NOT_EQUAL;
			}
		}

		return getComparisonType() == ComparisonType.NOT_EQUAL;
	}
}
