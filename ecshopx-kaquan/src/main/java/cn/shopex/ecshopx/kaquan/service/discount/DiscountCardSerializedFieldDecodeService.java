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

package cn.shopex.ecshopx.kaquan.service.discount;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 卡券表中 {@code text_image_list}、{@code time_limit} 历史字段：优先 JSON，失败则解析 legacy {@code a:}/{@code s:} 数组序列化子集。
 */
@Service
public class DiscountCardSerializedFieldDecodeService {

	private final ObjectMapper objectMapper;

	public DiscountCardSerializedFieldDecodeService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Object decodeTextImageList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		Object json = tryParseJson(t);
		if (json != null) {
			return json;
		}
		try {
			return LegacySerializedArrayReader.readRoot(t);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> decodeTimeLimit(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		JsonNode node = tryParseJsonNode(t);
		if (node != null && node.isArray()) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (JsonNode el : node) {
				if (el.isObject()) {
					try {
						out.add(objectMapper.convertValue(el, Map.class));
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
			return out;
		}
		try {
			Object v = LegacySerializedArrayReader.readRoot(t);
			return normalizeToMapList(v);
		} catch (RuntimeException ignored) {
			return List.of();
		}
	}

	private Object tryParseJson(String t) {
		if (!t.startsWith("{") && !t.startsWith("[")) {
			return null;
		}
		try {
			return objectMapper.readValue(t, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private JsonNode tryParseJsonNode(String t) {
		if (!t.startsWith("{") && !t.startsWith("[")) {
			return null;
		}
		try {
			return objectMapper.readTree(t);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> normalizeToMapList(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					out.add((Map<String, Object>) m);
				}
			}
			return out;
		}
		if (v instanceof Map<?, ?> map) {
			boolean allNumericKeys = true;
			List<Integer> ord = new ArrayList<>();
			for (Object k : map.keySet()) {
				if (k instanceof Number n) {
					ord.add(n.intValue());
				} else if (k instanceof String s) {
					try {
						ord.add(Integer.parseInt(s.trim()));
					} catch (NumberFormatException e) {
						allNumericKeys = false;
						break;
					}
				} else {
					allNumericKeys = false;
					break;
				}
			}
			if (allNumericKeys && !ord.isEmpty()) {
				ord.sort(Integer::compareTo);
				List<Map<String, Object>> out = new ArrayList<>();
				for (int idx : ord) {
					Object el = map.get(String.valueOf(idx));
					if (el == null) {
						el = map.get(idx);
					}
					if (el instanceof Map<?, ?> m) {
						out.add((Map<String, Object>) m);
					}
				}
				return out;
			}
		}
		return List.of();
	}

	/** 只读解析 legacy 数组序列化（{@code a:}/{@code s:} 等）：禁止对象类型 {@code O:}。 */
	private static final class LegacySerializedArrayReader {

		private static Object readRoot(String s) {
			Cursor c = new Cursor(s.trim());
			Object v = readValue(c);
			c.skipTrailingWhitespace();
			if (c.p != c.s.length()) {
				throw new IllegalArgumentException("trailing data");
			}
			return v;
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
				case 'O' -> throw new IllegalArgumentException("object type not allowed");
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
}
