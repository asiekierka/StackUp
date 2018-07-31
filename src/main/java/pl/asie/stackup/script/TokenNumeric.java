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

import java.io.IOException;
import java.io.PushbackReader;
import java.util.function.Function;

public class TokenNumeric<T> extends Token<T> {
	protected final Function<T, Number> function;
	private ComparisonType type;
	private int number;

	public TokenNumeric(Function<T, Number> function) {
		this.function = function;
	}

	protected boolean isInvalidComparisonType(ComparisonType type) {
		return type != ComparisonType.EQUAL && type != ComparisonType.LESS_EQUAL && type != ComparisonType.NOT_EQUAL
				&& type != ComparisonType.LESS_THAN && type != ComparisonType.GREATER_EQUAL && type != ComparisonType.GREATER_THAN;
	}

	public static int parseInteger(PushbackReader reader) throws NumberFormatException, IOException {
		StringBuilder builder = new StringBuilder();
		ScriptHandler.cutWhitespace(reader);
		int c;
		while (Character.isDigit(c = reader.read())) {
			builder.appendCodePoint(c);
		}
		reader.unread(c);
		ScriptHandler.cutWhitespace(reader);

		return Integer.parseInt(builder.toString());
	}

	@Override
	public void parse(PushbackReader reader) throws IOException, TokenException {
		type = getComparisonType(reader);
		if (isInvalidComparisonType(type)) {
			throw new TokenException("Unsupported comparison type " + type + "!");
		}

		try {
			number = parseInteger(reader);
		} catch (NumberFormatException e) {
			throw new TokenException("Invalid number!", e);
		}
	}

	protected int getNumber() {
		return number;
	}

	protected ComparisonType getComparisonType() {
		return type;
	}

	@Override
	public boolean apply(T object) {
		Number n = function.apply(object);
		int iv = n.intValue();
		switch (type) {
			case EQUAL:
			default:
				return iv == number;
			case NOT_EQUAL:
				return iv != number;
			case LESS_THAN:
				return iv < number;
			case GREATER_THAN:
				return iv > number;
			case LESS_EQUAL:
				return iv <= number;
			case GREATER_EQUAL:
				return iv >= number;
		}
	}
}
