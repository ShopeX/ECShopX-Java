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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Read/write {@code reservation_default_work_shift.work_shift_data} in legacy serialize format
 * (nested associative arrays: weekday → {@code typeId} / optional fields).
 *
 * <p>In {@code s:N:"..."} segments, {@code N} is the <strong>UTF-8 byte length</strong> of the decoded string
 * payload, not the Java {@code char} count (multi-byte characters count multiple bytes).
 */
public final class WorkShiftDataCodec {

	private WorkShiftDataCodec() {}

	/**
	 * Serializes {@code weekday → (field → value)} as a legacy associative array string (outer and inner {@code a:}).
	 */
	public static String serializeWorkShiftData(Map<String, Map<String, String>> workShiftData) {
		Objects.requireNonNull(workShiftData, "workShiftData");
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(workShiftData.size() * 64 + 32);
			writeAssociativeStringToStringMap(buf, workShiftData);
			return buf.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("work_shift_data serialize failed", e);
		}
	}

	/**
	 * Parses root weekday map for API / lookup use. If the trimmed value starts with {@code '{'}, tries JSON first
	 * (for Java-written test rows); on failure or otherwise, tries legacy array unserialize.
	 */
	public static Map<String, Object> parseWorkShiftDataRoot(String raw, ObjectMapper jsonMapper) {
		Objects.requireNonNull(jsonMapper, "jsonMapper");
		if (raw == null || raw.isBlank()) {
			return new LinkedHashMap<>();
		}
		String t = raw.trim();
		if (t.startsWith("{")) {
			try {
				LinkedHashMap<String, Object> m =
						jsonMapper.readValue(t, new TypeReference<LinkedHashMap<String, Object>>() {});
				return m != null ? m : new LinkedHashMap<>();
			} catch (JsonProcessingException ignored) {
				// fall through to legacy format
			}
		}
		try {
			Cursor c = new Cursor(t);
			Object v = readValue(c);
			c.skipTrailingWhitespace();
			if (c.p != t.length()) {
				throw new IllegalArgumentException("trailing data after root value");
			}
			if (v instanceof Map<?, ?> map) {
				return normalizeRootMap(map);
			}
		} catch (RuntimeException ignored) {
			// fall through
		}
		if (!t.startsWith("{")) {
			try {
				LinkedHashMap<String, Object> m =
						jsonMapper.readValue(t, new TypeReference<LinkedHashMap<String, Object>>() {});
				return m != null ? m : new LinkedHashMap<>();
			} catch (JsonProcessingException ignored) {
			}
		}
		return new LinkedHashMap<>();
	}

	private static void writeAssociativeStringToStringMap(
			ByteArrayOutputStream buf, Map<String, Map<String, String>> workShiftData) throws IOException {
		buf.write('a');
		buf.write(':');
		buf.write(Integer.toString(workShiftData.size()).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('{');
		for (Map.Entry<String, Map<String, String>> e : workShiftData.entrySet()) {
			writeSerializedString(buf, e.getKey());
			Map<String, String> inner = e.getValue();
			writeAssociativeStringMap(buf, inner != null ? inner : Map.of());
		}
		buf.write('}');
	}

	private static void writeAssociativeStringMap(ByteArrayOutputStream buf, Map<String, String> inner)
			throws IOException {
		buf.write('a');
		buf.write(':');
		buf.write(Integer.toString(inner.size()).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('{');
		for (Map.Entry<String, String> e : inner.entrySet()) {
			writeSerializedString(buf, e.getKey());
			String v = e.getValue();
			writeSerializedString(buf, v != null ? v : "");
		}
		buf.write('}');
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

	private static Map<String, Object> normalizeRootMap(Map<?, ?> map) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			out.put(keyToString(e.getKey()), normalizeNestedValue(e.getValue()));
		}
		return out;
	}

	private static Object normalizeNestedValue(Object v) {
		if (v instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				inner.put(keyToString(e.getKey()), normalizeNestedValue(e.getValue()));
			}
			return inner;
		}
		return v;
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
		return Objects.toString(k, "");
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
			case 'O' -> throw new IllegalArgumentException("serialized objects are not supported");
			default -> throw new IllegalArgumentException("unknown serialized type: " + t);
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
		if (bit == '0') {
			return Boolean.FALSE;
		}
		if (bit == '1') {
			return Boolean.TRUE;
		}
		throw new IllegalArgumentException("invalid boolean");
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
			throw new IllegalArgumentException(
					"string byte length mismatch: expected " + byteLen + ", actual " + raw.size());
		}
		c.expect(';');
		return raw.toString(StandardCharsets.UTF_8);
	}

	private static Map<String, Object> readSerializedArray(Cursor c) {
		c.expect('a');
		c.expect(':');
		int n = readUnsignedInt(c, ':');
		c.expect('{');
		LinkedHashMap<String, Object> map = new LinkedHashMap<>(Math.max(16, n * 2));
		for (int i = 0; i < n; i++) {
			Object keyObj = readValue(c);
			Object valObj = readValue(c);
			map.put(keyToString(keyObj), normalizeNestedValue(valObj));
		}
		c.expect('}');
		return map;
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
		if (Character.isHighSurrogate(ch)) {
			throw new IllegalArgumentException("lone surrogate");
		}
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
