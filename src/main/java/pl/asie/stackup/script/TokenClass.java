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

import java.util.List;
import java.util.function.Function;

public class TokenClass<T> extends TokenString<T> {
	private final Function<T, List<Class>> classFunction;

	public TokenClass(Function<T, List<Class>> function, boolean ignoreCase) {
		super((t) -> function.apply(t).get(0).getName(), ignoreCase);
		this.classFunction = function;
	}

	protected boolean isInvalidComparisonType(ComparisonType type) {
		return type != ComparisonType.EQUAL && type != ComparisonType.GREATER_EQUAL && type != ComparisonType.GREATER_THAN && type != ComparisonType.NOT_EQUAL;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean apply(T object) {
		List<Class> cReceivedL = classFunction.apply(object);
		try {
			Class cSet = Class.forName(getString());
			for (Class cReceived : cReceivedL) {
				switch (getComparisonType()) {
					case EQUAL:
					default:
						if (cSet == cReceived) {
							return true;
						}
						break;
					case NOT_EQUAL:
						if (cSet == cReceived) {
							return false;
						}
						break;
					case GREATER_THAN:
						if (cSet == cReceived) {
							return false;
						}
						if (cSet.isAssignableFrom(cReceived)) {
							return true;
						}
						break;
					case GREATER_EQUAL:
						if (cSet == cReceived || cSet.isAssignableFrom(cReceived)) {
							return true;
						}
						break;
				}
			}

			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
}
