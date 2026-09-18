/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.companys.service.activation;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Codec for associative maps and scalar strings in the wire format expected by Shopex license and
 * AuthCode gateways ({@code a:} associative array, {@code s:} length-prefixed UTF-8 string,
 * {@code i:} integer).
 */
public final class DiscuzAuthPayloadCodec {

	private DiscuzAuthPayloadCodec() {}

	public static String serializeAssocStringMap(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("a:").append(map.size()).append(":{");
		for (Map.Entry<String, ?> e : map.entrySet()) {
			sb.append(serializeWireScalarString(e.getKey()));
			sb.append(serializeValue(e.getValue()));
		}
		sb.append("}");
		return sb.toString();
	}

	public static String serializeWireScalarString(String s) {
		if (s == null) {
			s = "";
		}
		byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
		String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
		return "s:" + utf8.length + ":\"" + escaped + "\";";
	}

	public static String serializeValue(Object v) {
		if (v == null) {
			return serializeWireScalarString("");
		}
		if (v instanceof Number n) {
			return "i:" + n.longValue() + ";";
		}
		return serializeWireScalarString(String.valueOf(v));
	}

	public static Object readRoot(String s) {
		Cursor c = new Cursor(s.trim());
		Object v = readValue(c);
		c.skipTrailingWhitespace();
		if (c.p != c.s.length()) {
			throw new IllegalArgumentException("trailing data");
		}
		return v;
	}

	public static Map<String, Object> readAssocMap(String s) {
		Object o = readRoot(s);
		if (o instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			return out;
		}
		throw new IllegalArgumentException("expected array map");
	}

	private static Object readValue(Cursor c) {
		if (c.p >= c.s.length()) {
			throw new IllegalArgumentException("unexpected end");
		}
		char t = c.s.charAt(c.p);
		return switch (t) {
			case 'N' -> readNull(c);
			case 'b' -> readBoolean(c);
			case 'i' -> readInt(c);
			case 'd' -> readDouble(c);
			case 's' -> readSerializedString(c);
			case 'a' -> readSerializedArray(c);
			default -> throw new IllegalArgumentException("unknown type: " + t);
		};
	}

	private static Object readNull(Cursor c) {
		c.expect('N');
		c.expect(';');
		return null;
	}

	private static Boolean readBoolean(Cursor c) {
		c.expect('b');
		c.expect(':');
		char bit = c.read();
		c.expect(';');
		return bit == '1';
	}

	private static Long readInt(Cursor c) {
		c.expect('i');
		c.expect(':');
		int sign = 1;
		if (c.peek() == '+') {
			c.p++;
		} else if (c.peek() == '-') {
			sign = -1;
			c.p++;
		}
		long v = 0L;
		boolean any = false;
		while (c.p < c.s.length()) {
			char d = c.s.charAt(c.p);
			if (d == ';') {
				c.p++;
				if (!any) {
					throw new IllegalArgumentException("empty int");
				}
				return v * sign;
			}
			if (d < '0' || d > '9') {
				throw new IllegalArgumentException("invalid int");
			}
			any = true;
			v = v * 10L + (d - '0');
			c.p++;
		}
		throw new IllegalArgumentException("unterminated int");
	}

	private static Double readDouble(Cursor c) {
		c.expect('d');
		c.expect(':');
		int start = c.p;
		while (c.p < c.s.length() && c.s.charAt(c.p) != ';') {
			c.p++;
		}
		String num = c.s.substring(start, c.p);
		c.expect(';');
		return Double.parseDouble(num);
	}

	private static String readSerializedString(Cursor c) {
		c.expect('s');
		c.expect(':');
		int byteLen = readUnsignedInt(c, ':');
		c.expect('"');
		ByteArrayOutputStream raw = new ByteArrayOutputStream(Math.max(16, byteLen + 4));
		while (c.p < c.s.length()) {
			char ch = c.s.charAt(c.p);
			if (ch == '"') {
				c.p++;
				break;
			}
			if (ch == '\\') {
				c.p++;
				if (c.p >= c.s.length()) {
					throw new IllegalArgumentException("unterminated escape");
				}
				char esc = c.s.charAt(c.p++);
				if (esc == '\\') {
					raw.write('\\');
				} else if (esc == '"') {
					raw.write('"');
				} else if (esc == '0') {
					raw.write(0);
				} else {
					raw.write('\\');
					appendUtf8CodeUnit(raw, esc);
				}
				continue;
			}
			int cp = Character.codePointAt(c.s, c.p);
			c.p += Character.charCount(cp);
			appendUtf8(raw, cp);
		}
		if (raw.size() != byteLen) {
			throw new IllegalArgumentException("string byte length mismatch");
		}
		c.expect(';');
		return raw.toString(StandardCharsets.UTF_8);
	}

	private static Object readSerializedArray(Cursor c) {
		c.expect('a');
		c.expect(':');
		int n = readUnsignedInt(c, ':');
		c.expect('{');
		List<Object> keys = new ArrayList<>(n);
		List<Object> vals = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			keys.add(readValue(c));
			vals.add(readValue(c));
		}
		c.expect('}');
		boolean listLike = true;
		for (int i = 0; i < n; i++) {
			Object ko = keys.get(i);
			if (!(ko instanceof Long l) || l.intValue() != i) {
				listLike = false;
				break;
			}
		}
		if (listLike) {
			return new ArrayList<>(vals);
		}
		LinkedHashMap<String, Object> map = new LinkedHashMap<>(Math.max(16, n * 2));
		for (int i = 0; i < n; i++) {
			map.put(keyToString(keys.get(i)), vals.get(i));
		}
		return map;
	}

	private static String keyToString(Object k) {
		if (k instanceof String s) {
			return s;
		}
		if (k instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		if (k instanceof Boolean b) {
			return b ? "1" : "0";
		}
		return String.valueOf(k);
	}

	private static int readUnsignedInt(Cursor c, char terminator) {
		int start = c.p;
		while (c.p < c.s.length()) {
			char ch = c.s.charAt(c.p);
			if (ch == terminator) {
				String digits = c.s.substring(start, c.p);
				c.p++;
				if (digits.isEmpty()) {
					throw new IllegalArgumentException("empty length");
				}
				return Integer.parseInt(digits);
			}
			if (ch < '0' || ch > '9') {
				throw new IllegalArgumentException("invalid int digit");
			}
			c.p++;
		}
		throw new IllegalArgumentException("unterminated int");
	}

	private static void appendUtf8(ByteArrayOutputStream raw, int cp) {
		if (cp < 0x80) {
			raw.write(cp);
		} else if (cp < 0x800) {
			raw.write(0xc0 | (cp >> 6));
			raw.write(0x80 | (cp & 0x3f));
		} else if (cp < 0x10000) {
			raw.write(0xe0 | (cp >> 12));
			raw.write(0x80 | ((cp >> 6) & 0x3f));
			raw.write(0x80 | (cp & 0x3f));
		} else {
			raw.write(0xf0 | (cp >> 18));
			raw.write(0x80 | ((cp >> 12) & 0x3f));
			raw.write(0x80 | ((cp >> 6) & 0x3f));
			raw.write(0x80 | (cp & 0x3f));
		}
	}

	private static void appendUtf8CodeUnit(ByteArrayOutputStream raw, char ch) {
		appendUtf8(raw, ch);
	}

	private static final class Cursor {
		private final String s;
		private int p;

		private Cursor(String s) {
			this.s = s;
		}

		private char peek() {
			return p < s.length() ? s.charAt(p) : 0;
		}

		private char read() {
			if (p >= s.length()) {
				throw new IllegalArgumentException("unexpected end");
			}
			return s.charAt(p++);
		}

		private void expect(char x) {
			if (p >= s.length() || s.charAt(p) != x) {
				throw new IllegalArgumentException("expected '" + x + "' at " + p);
			}
			p++;
		}

		private void skipTrailingWhitespace() {
			while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
				p++;
			}
		}
	}
}
