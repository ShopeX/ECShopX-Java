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

package cn.shopex.ecshopx.reservation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Legacy serialize format for {@code reservation_setting.reservation_num_limit}
 * (flat associative array: {@code limit_type} string + {@code limit_days} or {@code limit_nums} int).
 *
 * <p>In {@code s:N:"..."} segments, {@code N} is the <strong>UTF-8 byte length</strong> of the payload.
 */
public final class ReservationNumLimitCodec {

	private ReservationNumLimitCodec() {}

	public static String serialize(Map<String, Object> limit) {
		Objects.requireNonNull(limit, "limit");
		if (limit.isEmpty()) {
			return "a:0:{}";
		}
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(limit.size() * 48 + 16);
			buf.write('a');
			buf.write(':');
			buf.write(Integer.toString(limit.size()).getBytes(StandardCharsets.US_ASCII));
			buf.write(':');
			buf.write('{');
			for (Map.Entry<String, Object> e : limit.entrySet()) {
				writeSerializedString(buf, e.getKey());
				Object v = e.getValue();
				if (v instanceof Number n) {
					writeSerializedInt(buf, n.longValue());
				} else if (v instanceof Boolean b) {
					writeSerializedInt(buf, b ? 1L : 0L);
				} else {
					writeSerializedString(buf, v != null ? v.toString() : "");
				}
			}
			buf.write('}');
			return buf.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("reservation_num_limit serialize failed", e);
		}
	}

	/**
	 * Parses a legacy serialized map for API use. {@code optional} is reserved for forward compatibility
	 * (e.g. JSON fallback); may be null.
	 */
	public static Map<String, Object> parse(String raw, ObjectMapper optional) {
		if (raw == null || raw.isBlank()) {
			return new LinkedHashMap<>();
		}
		String t = raw.trim();
		if ("a:0:{}".equals(t)) {
			return new LinkedHashMap<>();
		}
		try {
			Cursor c = new Cursor(t);
			Object v = readValue(c);
			c.skipTrailingWhitespace();
			if (c.p != t.length()) {
				return new LinkedHashMap<>();
			}
			if (v instanceof Map<?, ?> map) {
				LinkedHashMap<String, Object> out = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : map.entrySet()) {
					out.put(String.valueOf(e.getKey()), e.getValue());
				}
				return out;
			}
		} catch (RuntimeException ignored) {
			// fall through
		}
		return new LinkedHashMap<>();
	}

	private static void writeSerializedInt(ByteArrayOutputStream buf, long n) throws IOException {
		buf.write('i');
		buf.write(':');
		buf.write(Long.toString(n).getBytes(StandardCharsets.US_ASCII));
		buf.write(';');
	}

	private static void writeSerializedString(ByteArrayOutputStream buf, String s) throws IOException {
		byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
		buf.write('s');
		buf.write(':');
		buf.write(Integer.toString(utf8.length).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('"');
		for (byte b : utf8) {
			int u = b & 0xFF;
			if (u == '\\') {
				buf.write('\\');
				buf.write('\\');
			} else if (u == '"') {
				buf.write('\\');
				buf.write('"');
			} else if (u == 0) {
				buf.write('\\');
				buf.write('0');
			} else {
				buf.write(b);
			}
		}
		buf.write('"');
		buf.write(';');
	}

	private static Object readValue(Cursor c) {
		if (c.p >= c.s.length()) {
			throw new IllegalArgumentException("unexpected end");
		}
		char t = c.s.charAt(c.p);
		return switch (t) {
			case 'i' -> readInt(c);
			case 's' -> readSerializedString(c);
			case 'a' -> readSerializedArray(c);
			default -> throw new IllegalArgumentException("unsupported type: " + t);
		};
	}

	private static Long readInt(Cursor c) {
		c.expect('i');
		c.expect(':');
		int start = c.p;
		while (c.p < c.s.length() && (c.s.charAt(c.p) == '-' || Character.isDigit(c.s.charAt(c.p)))) {
			c.p++;
		}
		long v = Long.parseLong(c.s.substring(start, c.p));
		c.expect(';');
		return v;
	}

	private static String readSerializedString(Cursor c) {
		c.expect('s');
		c.expect(':');
		int len = c.readIntLiteral();
		c.expect(':');
		c.expect('"');
		StringBuilder sb = new StringBuilder(len);
		int end = c.p + len;
		if (end > c.s.length()) {
			throw new IllegalArgumentException("string length overflow");
		}
		while (c.p < end) {
			char ch = c.s.charAt(c.p++);
			if (ch == '\\' && c.p < c.s.length()) {
				char esc = c.s.charAt(c.p++);
				sb.append(switch (esc) {
					case '\\' -> '\\';
					case '"' -> '"';
					case '0' -> '\0';
					default -> esc;
				});
			} else {
				sb.append(ch);
			}
		}
		c.expect('"');
		c.expect(';');
		return sb.toString();
	}

	private static Map<Object, Object> readSerializedArray(Cursor c) {
		c.expect('a');
		c.expect(':');
		int n = c.readIntLiteral();
		c.expect(':');
		c.expect('{');
		LinkedHashMap<Object, Object> map = new LinkedHashMap<>(n);
		for (int i = 0; i < n; i++) {
			Object k = readValue(c);
			Object v = readValue(c);
			map.put(k, v);
		}
		c.expect('}');
		return map;
	}

	private static final class Cursor {
		final String s;
		int p;

		Cursor(String s) {
			this.s = s;
			this.p = 0;
		}

		void expect(char ch) {
			if (p >= s.length() || s.charAt(p) != ch) {
				throw new IllegalArgumentException("expected " + ch);
			}
			p++;
		}

		int readIntLiteral() {
			int start = p;
			if (p < s.length() && s.charAt(p) == '-') {
				p++;
			}
			while (p < s.length() && Character.isDigit(s.charAt(p))) {
				p++;
			}
			return Integer.parseInt(s.substring(start, p));
		}

		char read() {
			return s.charAt(p++);
		}

		void skipTrailingWhitespace() {
			while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
				p++;
			}
		}
	}
}
