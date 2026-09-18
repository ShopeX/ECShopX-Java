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

package cn.shopex.ecshopx.adapay.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes withdraw {@code rule} payloads to a legacy binary string for {@code adapay_withdraw_set.rule}.
 *
 * <p>In {@code s:N:"..."} segments, {@code N} is the UTF-8 byte length of the payload.
 */
public final class AdapayWithdrawRuleLegacySerializeUtil {

	private AdapayWithdrawRuleLegacySerializeUtil() {}

	/**
	 * Parses a stored {@code rule} column produced by {@link #serialize(Map)} back into a map.
	 */
	public static Map<String, Object> deserializeWithdrawRule(String serialized) {
		if (serialized == null || serialized.trim().isEmpty()) {
			return new LinkedHashMap<>();
		}
		String t = serialized.trim();
		if ("a:0:{}".equals(t)) {
			return new LinkedHashMap<>();
		}
		try {
			Parser p = new Parser(t);
			Object root = p.readValue();
			p.skipTrailingSpace();
			if (p.pos < p.s.length()) {
				throw new IllegalStateException("withdraw rule deserialize failed: trailing data");
			}
			if (!(root instanceof Map<?, ?> m)) {
				throw new IllegalStateException("withdraw rule deserialize failed: root is not a map");
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			return out;
		} catch (IllegalStateException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IllegalStateException("withdraw rule deserialize failed", e);
		}
	}

	public static String serialize(Map<String, Object> rule) {
		if (rule == null || rule.isEmpty()) {
			return "a:0:{}";
		}
		LinkedHashMap<String, Object> top = new LinkedHashMap<>();
		Object typeObj = rule.get("type");
		top.put("type", typeObj == null ? "" : String.valueOf(typeObj));
		top.put("filter", buildOrderedFilterMap(rule.get("filter")));
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
			writeSerializedArrayMap(buf, top);
			return buf.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("withdraw rule serialize failed", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> buildOrderedFilterMap(Object filterRaw) {
		Map<String, Object> filter;
		if (filterRaw instanceof Map<?, ?> m) {
			filter = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				filter.put(String.valueOf(e.getKey()), e.getValue());
			}
		} else {
			filter = new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
		for (String k : List.of("month", "day", "amount")) {
			if (filter.containsKey(k)) {
				ordered.put(k, filter.get(k));
			}
		}
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			if (!ordered.containsKey(e.getKey())) {
				ordered.put(e.getKey(), e.getValue());
			}
		}
		return ordered;
	}

	private static void writeSerializedArrayMap(ByteArrayOutputStream buf, Map<String, Object> map) throws IOException {
		buf.write('a');
		buf.write(':');
		buf.write(Integer.toString(map.size()).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('{');
		for (Map.Entry<String, Object> e : map.entrySet()) {
			writeSerializedString(buf, e.getKey());
			writeSerializedValue(buf, e.getValue());
		}
		buf.write('}');
	}

	private static void writeSerializedValue(ByteArrayOutputStream buf, Object v) throws IOException {
		if (v instanceof Map<?, ?> nestedRaw) {
			LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : nestedRaw.entrySet()) {
				nested.put(String.valueOf(e.getKey()), e.getValue());
			}
			writeSerializedArrayMap(buf, nested);
			return;
		}
		if (v instanceof Number n) {
			if (v instanceof Double || v instanceof Float) {
				writeSerializedString(buf, String.valueOf(v));
				return;
			}
			writeSerializedInt(buf, n.longValue());
			return;
		}
		writeSerializedString(buf, v != null ? v.toString() : "");
	}

	private static void writeSerializedInt(ByteArrayOutputStream buf, long n) throws IOException {
		buf.write('i');
		buf.write(':');
		buf.write(Long.toString(n).getBytes(StandardCharsets.US_ASCII));
		buf.write(';');
	}

	private static void writeSerializedString(ByteArrayOutputStream buf, String s) throws IOException {
		Objects.requireNonNull(s);
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

	private static final class Parser {
		final String s;
		int pos;

		Parser(String s) {
			this.s = s;
			this.pos = 0;
		}

		void skipTrailingSpace() {
			while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
				pos++;
			}
		}

		Object readValue() {
			if (pos >= s.length()) {
				throw new IllegalStateException("withdraw rule deserialize failed: unexpected end");
			}
			char c = s.charAt(pos);
			if (c == 'a') {
				return readArrayMap();
			}
			if (c == 'i') {
				return readIntValue();
			}
			if (c == 's') {
				return readStringValue();
			}
			throw new IllegalStateException("withdraw rule deserialize failed: unknown type prefix: " + c);
		}

		Map<String, Object> readArrayMap() {
			expect('a');
			expect(':');
			int n = readUnsignedInt();
			expect(':');
			expect('{');
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			for (int i = 0; i < n; i++) {
				Object key = readValue();
				if (!(key instanceof String)) {
					throw new IllegalStateException("withdraw rule deserialize failed: non-string key");
				}
				Object val = readValue();
				map.put((String) key, val);
			}
			expect('}');
			return map;
		}

		Long readIntValue() {
			expect('i');
			expect(':');
			long n = readSignedLong();
			expect(';');
			return n;
		}

		String readStringValue() {
			expect('s');
			expect(':');
			int byteLen = readUnsignedInt();
			expect(':');
			expect('"');
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(16, byteLen));
			int decoded = 0;
			while (decoded < byteLen) {
				if (pos >= s.length()) {
					throw new IllegalStateException("withdraw rule deserialize failed: truncated string");
				}
				int cp = Character.codePointAt(s, pos);
				if (cp == '\\') {
					pos++;
					if (pos >= s.length()) {
						throw new IllegalStateException("withdraw rule deserialize failed: truncated escape");
					}
					char e = s.charAt(pos++);
					if (e == '\\') {
						out.write('\\');
						decoded++;
					} else if (e == '"') {
						out.write('"');
						decoded++;
					} else if (e == '0') {
						out.write(0);
						decoded++;
					} else {
						throw new IllegalStateException("withdraw rule deserialize failed: unknown escape");
					}
				} else {
					int chCount = Character.charCount(cp);
					String unit = s.substring(pos, pos + chCount);
					byte[] enc = unit.getBytes(StandardCharsets.UTF_8);
					if (decoded + enc.length > byteLen) {
						throw new IllegalStateException("withdraw rule deserialize failed: string length mismatch");
					}
					out.write(enc, 0, enc.length);
					decoded += enc.length;
					pos += chCount;
				}
			}
			expect('"');
			expect(';');
			return out.toString(StandardCharsets.UTF_8);
		}

		int readUnsignedInt() {
			int start = pos;
			while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
				pos++;
			}
			if (pos == start) {
				throw new IllegalStateException("withdraw rule deserialize failed: expected digits");
			}
			return Integer.parseInt(s.substring(start, pos));
		}

		long readSignedLong() {
			boolean neg = false;
			if (pos < s.length() && s.charAt(pos) == '-') {
				neg = true;
				pos++;
			}
			int start = pos;
			while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
				pos++;
			}
			if (pos == start) {
				throw new IllegalStateException("withdraw rule deserialize failed: expected digits");
			}
			long v = Long.parseLong(s.substring(start, pos));
			return neg ? -v : v;
		}

		void expect(char c) {
			if (pos >= s.length() || s.charAt(pos) != c) {
				throw new IllegalStateException("withdraw rule deserialize failed: expected '" + c + "'");
			}
			pos++;
		}
	}
}
