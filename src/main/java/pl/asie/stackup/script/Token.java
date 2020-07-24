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

import java.io.IOException;
import java.io.PushbackReader;

public abstract class Token<T> {
	public enum ComparisonType {
		EQUAL,
		APPROXIMATELY_EQUAL,
		REGEX_EQUAL,
		NOT_EQUAL,
		LESS_THAN,
		LESS_EQUAL,
		GREATER_THAN,
		GREATER_EQUAL,
		ASSIGN_ADD,
		ASSIGN_SUB,
		ASSIGN_MUL,
		ASSIGN_DIV
	}

	protected ComparisonType getComparisonType(PushbackReader r) throws IOException, TokenException {
		ScriptHandler.cutWhitespace(r);
		int codePoint = r.read();
		if (codePoint == '+' || codePoint == '-' || codePoint == '*' || codePoint == '/' || codePoint == '%') {
			int oldCode = codePoint;
			codePoint = r.read();
			if (codePoint == '=') {
				if (oldCode == '+') return ComparisonType.ASSIGN_ADD;
				else if (oldCode == '-') return ComparisonType.ASSIGN_SUB;
				else if (oldCode == '*') return ComparisonType.ASSIGN_MUL;
				else if (oldCode == '/') return ComparisonType.ASSIGN_DIV;
				else if (oldCode == '%') return ComparisonType.REGEX_EQUAL;
				else throw new TokenException("Should not get here! " + oldCode);
			} else {
				throw new TokenException("Invalid comparison type!");
			}
		}
		if (codePoint == '!') {
			codePoint = r.read();
			if (codePoint == '=') {
				return ComparisonType.NOT_EQUAL;
			} else {
				throw new TokenException("Invalid comparison type!");
			}
		} else if (codePoint == '~') {
			codePoint = r.read();
			if (codePoint == '=') {
				return ComparisonType.APPROXIMATELY_EQUAL;
			} else {
				throw new TokenException("Invalid comparison type!");
			}
		} else if (codePoint == '=') {
			codePoint = r.read();
			if (codePoint != '=') {
				r.unread(codePoint);
			}

			return ComparisonType.EQUAL;
		} else if (codePoint == '<') {
			codePoint = r.read();
			if (codePoint == '=') {
				return ComparisonType.LESS_EQUAL;
			} else {
				r.unread(codePoint);
				return ComparisonType.LESS_THAN;
			}
		} else if (codePoint == '>') {
			codePoint = r.read();
			if (codePoint == '=') {
				return ComparisonType.GREATER_EQUAL;
			} else {
				r.unread(codePoint);
				return ComparisonType.GREATER_THAN;
			}
		} else {
			throw new TokenException("Invalid comparison type!");
		}
	}

	private boolean invert;

	public boolean isInvert() {
		return invert;
	}

	public void setInvert(boolean invert) {
		this.invert = invert;
	}

	public abstract void parse(PushbackReader reader) throws IOException, TokenException;

	public abstract boolean apply(T object);
}
