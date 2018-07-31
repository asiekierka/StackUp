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

import java.util.function.Function;

public class TokenResourceLocation<T> extends TokenString<T> {
	public TokenResourceLocation(Function<T, String> function) {
		super(function, false);
	}

	@Override
	public boolean apply(T object) {
		String[] str1 = function.apply(object).split(":");
		String[] str2 = getString().split(":");
		if (str1.length == 2 && str2.length == 2) {
			if (!str2[0].equals("*") && !compare(str1[0], str2[0])) {
				return false;
			}

			//noinspection RedundantIfStatement
			if (!str2[1].equals("*") && !compare(str1[1], str2[1])) {
				return false;
			}

			return true;
		} else {
			return false;
		}
	}
}
