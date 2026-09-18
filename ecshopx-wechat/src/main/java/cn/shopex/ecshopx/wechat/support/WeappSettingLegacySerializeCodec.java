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

package cn.shopex.ecshopx.wechat.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Legacy binary string format for wechat decoration {@code params} columns (UTF-8 segments, nested
 * arrays/maps, scalars), wire-compatible with historical DB content.
 */
public final class WeappSettingLegacySerializeCodec {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private WeappSettingLegacySerializeCodec() {}

	/**
	 * Decodes a stored {@code params} value to maps, lists, or scalars. Blank input yields an empty map.
	 *
	 * @throws IllegalArgumentException when the payload is not valid legacy serialized form and not a legacy JSON object
	 * @throws RuntimeException for other parse failures (e.g. JSON engine)
	 */
	public static Object decode(String raw) {
		if (raw == null || raw.isEmpty()) {
			return new LinkedHashMap<String, Object>();
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return new LinkedHashMap<String, Object>();
		}
		char c0 = t.charAt(0);
		if (c0 == '{') {
			return decodeJsonObject(t);
		}
		if (c0 == 'a'
				|| c0 == 'i'
				|| c0 == 's'
				|| c0 == 'd'
				|| c0 == 'b'
				|| c0 == 'N') {
			try {
				Parser p = new Parser(t);
				Object root = p.readValue();
				p.skipTrailingSpace();
				if (p.pos < p.s.length()) {
					throw new IllegalArgumentException("params deserialize failed: trailing data");
				}
				return root;
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("params deserialize failed", e);
			}
		}
		throw new IllegalArgumentException("params deserialize failed: unrecognized format");
	}

	private static Map<String, Object> decodeJsonObject(String t) {
		try {
			JsonNode n = OBJECT_MAPPER.readTree(t);
			if (n == null || !n.isObject()) {
				throw new IllegalArgumentException("params JSON root must be an object");
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			var it = n.fields();
			while (it.hasNext()) {
				var e = it.next();
				out.put(e.getKey(), OBJECT_MAPPER.convertValue(e.getValue(), Object.class));
			}
			return out;
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("params JSON parse failed", e);
		}
	}

	public static String serialize(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return "a:0:{}";
		}
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(Math.max(64, map.size() * 16));
			writeSerializedArray(buf, map);
			return buf.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("params serialize failed", e);
		}
	}

	/**
	 * Root-level serialization for weakly-typed inputs: {@code null} becomes {@code N;}, strings may be parsed as JSON
	 * object/array when trim-valid; otherwise serialized as quoted string payloads. Does not throw validation
	 * exceptions for param shape.
	 */
	public static String serializeLoose(Object params) {
		if (params == null) {
			return "N;";
		}
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(64);
			if (params instanceof Boolean || params instanceof Number) {
				writeSerializedValue(buf, params);
				return buf.toString(StandardCharsets.UTF_8);
			}
			if (params instanceof String s) {
				String t = s.trim();
				if (t.isEmpty()) {
					writeSerializedString(buf, s);
					return buf.toString(StandardCharsets.UTF_8);
				}
				try {
					JsonNode root = OBJECT_MAPPER.readTree(t);
					if (root == null || root.isNull()) {
						buf.write('N');
						buf.write(';');
						return buf.toString(StandardCharsets.UTF_8);
					}
					if (root.isObject()) {
						LinkedHashMap<String, Object> map = new LinkedHashMap<>();
						var it = root.fields();
						while (it.hasNext()) {
							var e = it.next();
							map.put(e.getKey(), OBJECT_MAPPER.convertValue(e.getValue(), Object.class));
						}
						if (map.isEmpty()) {
							return "a:0:{}";
						}
						writeSerializedArray(buf, map);
						return buf.toString(StandardCharsets.UTF_8);
					}
					if (root.isArray()) {
						List<Object> list = new ArrayList<>(root.size());
						for (JsonNode el : root) {
							list.add(OBJECT_MAPPER.convertValue(el, Object.class));
						}
						writeSerializedList(buf, list);
						return buf.toString(StandardCharsets.UTF_8);
					}
				} catch (JsonProcessingException ignored) {
					// treat whole string as opaque quoted payload
				}
				writeSerializedString(buf, s);
				return buf.toString(StandardCharsets.UTF_8);
			}
			if (params instanceof Map<?, ?> mapRaw) {
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : mapRaw.entrySet()) {
					map.put(String.valueOf(e.getKey()), e.getValue());
				}
				if (map.isEmpty()) {
					return "a:0:{}";
				}
				writeSerializedArray(buf, map);
				return buf.toString(StandardCharsets.UTF_8);
			}
			if (params instanceof List<?> listRaw) {
				List<Object> list = new ArrayList<>(listRaw.size());
				for (Object item : listRaw) {
					list.add(item);
				}
				writeSerializedList(buf, list);
				return buf.toString(StandardCharsets.UTF_8);
			}
			writeSerializedString(buf, String.valueOf(params));
			return buf.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("params serialize failed", e);
		}
	}

	private static void writeSerializedArray(ByteArrayOutputStream buf, Map<String, Object> map) throws IOException {
		int n = map.size();
		boolean indexed = isSequentialIndexedStringKeys(map, n);
		buf.write('a');
		buf.write(':');
		buf.write(Integer.toString(n).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('{');
		if (indexed) {
			for (int i = 0; i < n; i++) {
				writeSerializedIntKey(buf, i);
				writeSerializedValue(buf, map.get(String.valueOf(i)));
			}
		} else {
			for (Map.Entry<String, Object> e : map.entrySet()) {
				writeSerializedString(buf, e.getKey());
				writeSerializedValue(buf, e.getValue());
			}
		}
		buf.write('}');
	}

	private static boolean isSequentialIndexedStringKeys(Map<String, Object> map, int n) {
		for (int i = 0; i < n; i++) {
			if (!map.containsKey(String.valueOf(i))) {
				return false;
			}
		}
		return true;
	}

	private static void writeSerializedIntKey(ByteArrayOutputStream buf, int k) throws IOException {
		buf.write('i');
		buf.write(':');
		buf.write(Integer.toString(k).getBytes(StandardCharsets.US_ASCII));
		buf.write(';');
	}

	@SuppressWarnings("unchecked")
	private static void writeSerializedValue(ByteArrayOutputStream buf, Object v) throws IOException {
		if (v == null) {
			buf.write('N');
			buf.write(';');
			return;
		}
		if (v instanceof Boolean b) {
			buf.write('b');
			buf.write(':');
			buf.write(b ? '1' : '0');
			buf.write(';');
			return;
		}
		if (v instanceof Number num) {
			if (v instanceof Double || v instanceof Float) {
				buf.write('d');
				buf.write(':');
				String ds = formatDoubleForLegacySerialize(num.doubleValue());
				buf.write(ds.getBytes(StandardCharsets.US_ASCII));
				buf.write(';');
				return;
			}
			writeSerializedInt(buf, num.longValue());
			return;
		}
		if (v instanceof String s) {
			writeSerializedString(buf, s);
			return;
		}
		if (v instanceof Map<?, ?> nestedRaw) {
			LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : nestedRaw.entrySet()) {
				nested.put(String.valueOf(e.getKey()), e.getValue());
			}
			writeSerializedArray(buf, nested);
			return;
		}
		if (v instanceof List<?> list) {
			writeSerializedList(buf, (List<Object>) list);
			return;
		}
		writeSerializedString(buf, String.valueOf(v));
	}

	private static void writeSerializedList(ByteArrayOutputStream buf, List<Object> list) throws IOException {
		int n = list.size();
		buf.write('a');
		buf.write(':');
		buf.write(Integer.toString(n).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('{');
		for (int i = 0; i < n; i++) {
			writeSerializedIntKey(buf, i);
			writeSerializedValue(buf, list.get(i));
		}
		buf.write('}');
	}

	private static String formatDoubleForLegacySerialize(double d) {
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return "0";
		}
		long asLong = (long) d;
		if (asLong == d && asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE) {
			return String.valueOf(asLong);
		}
		return Double.toString(d);
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
				throw new IllegalArgumentException("params deserialize failed: unexpected end");
			}
			char c = s.charAt(pos);
			return switch (c) {
				case 'a' -> readArray();
				case 'i' -> readIntValue();
				case 's' -> readStringValue();
				case 'd' -> readDoubleValue();
				case 'b' -> readBoolValue();
				case 'N' -> readNullValue();
				default -> throw new IllegalArgumentException("params deserialize failed: unknown type: " + c);
			};
		}

		Object readNullValue() {
			expect('N');
			expect(';');
			return null;
		}

		Boolean readBoolValue() {
			expect('b');
			expect(':');
			if (pos >= s.length()) {
				throw new IllegalArgumentException("params deserialize failed: bool truncated");
			}
			char v = s.charAt(pos++);
			if (v != '0' && v != '1') {
				throw new IllegalArgumentException("params deserialize failed: bool value");
			}
			expect(';');
			return v == '1';
		}

		Double readDoubleValue() {
			expect('d');
			expect(':');
			int start = pos;
			while (pos < s.length() && s.charAt(pos) != ';') {
				pos++;
			}
			if (pos == start || pos >= s.length()) {
				throw new IllegalArgumentException("params deserialize failed: double truncated");
			}
			String fragment = s.substring(start, pos);
			try {
				double d = Double.parseDouble(fragment);
				expect(';');
				return d;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("params deserialize failed: double parse", e);
			}
		}

		Object readArray() {
			expect('a');
			expect(':');
			int n = readUnsignedInt();
			expect(':');
			expect('{');
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			for (int i = 0; i < n; i++) {
				Object key = readValue();
				Object val = readValue();
				map.put(keyToString(key), val);
			}
			expect('}');
			if (n == 0) {
				return new ArrayList<>();
			}
			if (isSequentialZeroBasedIntStringKeys(map, n)) {
				List<Object> list = new ArrayList<>(n);
				for (int i = 0; i < n; i++) {
					list.add(map.get(String.valueOf(i)));
				}
				return list;
			}
			return map;
		}

		private static boolean isSequentialZeroBasedIntStringKeys(LinkedHashMap<String, Object> map, int n) {
			if (n <= 0) {
				return false;
			}
			for (int i = 0; i < n; i++) {
				if (!map.containsKey(String.valueOf(i))) {
					return false;
				}
			}
			return true;
		}

		private static String keyToString(Object key) {
			if (key instanceof String str) {
				return str;
			}
			if (key instanceof Number num) {
				return String.valueOf(num.longValue());
			}
			throw new IllegalArgumentException("params deserialize failed: unsupported array key type");
		}

		Long readIntValue() {
			expect('i');
			expect(':');
			long v = readSignedLong();
			expect(';');
			return v;
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
					throw new IllegalArgumentException("params deserialize failed: truncated string");
				}
				int cp = Character.codePointAt(s, pos);
				if (cp == '\\') {
					pos++;
					if (pos >= s.length()) {
						throw new IllegalArgumentException("params deserialize failed: truncated escape");
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
						throw new IllegalArgumentException("params deserialize failed: unknown escape");
					}
				} else {
					int chCount = Character.charCount(cp);
					String unit = s.substring(pos, pos + chCount);
					byte[] enc = unit.getBytes(StandardCharsets.UTF_8);
					if (decoded + enc.length > byteLen) {
						throw new IllegalArgumentException("params deserialize failed: string length mismatch");
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
				throw new IllegalArgumentException("params deserialize failed: expected digits");
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
				throw new IllegalArgumentException("params deserialize failed: expected digits");
			}
			long v = Long.parseLong(s.substring(start, pos));
			return neg ? -v : v;
		}

		void expect(char c) {
			if (pos >= s.length() || s.charAt(pos) != c) {
				throw new IllegalArgumentException("params deserialize failed: expected '" + c + "'");
			}
			pos++;
		}
	}
}
