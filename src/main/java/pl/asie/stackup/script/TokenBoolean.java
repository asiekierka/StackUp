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
import java.util.function.Predicate;

public class TokenBoolean<T> extends Token<T> {
	protected final Predicate<T> function;

	public TokenBoolean(Predicate<T> function) {
		this.function = function;
	}

	@Override
	public void parse(PushbackReader reader) throws IOException, TokenException {
		// pass
	}

	@Override
	public boolean apply(T object) {
		return function.test(object);
	}
}
