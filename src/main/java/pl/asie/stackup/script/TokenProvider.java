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

import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TokenProvider {
	public static final TokenProvider INSTANCE = new TokenProvider();
	private final Map<String, Supplier<Token>> tokenMap = new HashMap<>();

	public void addToken(String key, Supplier<Token> t) {
		tokenMap.put(key, t);
	}

	@Nonnull
	public Pair<String, Token> getToken(PushbackReader r) throws IOException {
		int c;
		boolean invToken = false;
		StringBuilder key = new StringBuilder();
		ScriptHandler.cutWhitespace(r);

		c = r.read();
		if (c == '!') {
			invToken = true;
		} else {
			r.unread(c);
		}

		while (Character.isAlphabetic((c = r.read()))) {
			key.appendCodePoint(c);
		}
		r.unread(c);
		ScriptHandler.cutWhitespace(r);
		Supplier<Token> s = tokenMap.get(key.toString());
		if (s != null) {
			Token t = s.get();
			t.setInvert(invToken);
			return Pair.of(key.toString(), t);
		} else {
			return Pair.of(key.toString(), null);
		}
	}
}
