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

package cn.shopex.ecshopx.common.web;

import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Builds a {@link Map} from {@link HttpServletRequest#getParameterMap()} for {@link FlexibleBody} and merge helpers:
 * bracket-array query keys {@code name[]=a&name[]=b} and repeated keys become {@link List} values under {@code name};
 * indexed keys {@code name[0]=a&name[1]=b} become a {@link List} under {@code name} in index order;
 * nested keys {@code name[0][field]=a} become nested maps/lists under {@code name};
 * scalar parameters stay as {@link String}.
 */
public final class FlexibleHttpServletParameterMap {

	private static final Pattern INDEXED_BRACKET_KEY = Pattern.compile("^([^\\[]+)\\[(\\d+)\\]$");
	private static final Pattern BRACKET_SEGMENT = Pattern.compile("\\[([^\\]]*)\\]");
	private static final Pattern NUMERIC_KEY = Pattern.compile("^[0-9]+$");

	private FlexibleHttpServletParameterMap() {}

	public static Map<String, Object> toObjectMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((rawKey, rawValues) -> {
			if (rawValues == null) {
				return;
			}
			List<String> vals = new ArrayList<>();
			for (String s : rawValues) {
				vals.add(s != null ? s.trim() : "");
			}
			if (vals.isEmpty()) {
				return;
			}
			boolean arraySuffix = rawKey.endsWith("[]");
			String key = arraySuffix ? rawKey.substring(0, rawKey.length() - 2) : rawKey;
			if (!StringUtils.hasText(key)) {
				return;
			}
			Object value;
			if (vals.size() > 1) {
				value = new ArrayList<>(vals);
			} else if (arraySuffix) {
				value = new ArrayList<>(vals);
			} else {
				value = vals.get(0);
			}
			putMerging(m, key, value);
		});
		foldNestedBracketKeys(m);
		foldIndexedBracketKeys(m);
		mergeActivatedAttributes(request, m);
		return m;
	}

	private static void mergeActivatedAttributes(HttpServletRequest request, Map<String, Object> m) {
		Object distributorId = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
		if (distributorId != null && !m.containsKey("distributor_id")) {
			m.put("distributor_id", distributorId);
		}
		Object distributorIds = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_IDS);
		if (distributorIds != null && !m.containsKey("distributorIds")) {
			m.put("distributorIds", distributorIds);
		}
	}

	/**
	 * Merges multi-segment bracket keys such as {@code rates[0][item_id]} into nested structures under
	 * {@code rates}, then converts all-numeric-key maps to ordered lists.
	 */
	private static void foldNestedBracketKeys(Map<String, Object> m) {
		List<String> nestedKeys = new ArrayList<>();
		for (String key : m.keySet()) {
			if (key.indexOf('[') < 0) {
				continue;
			}
			if (INDEXED_BRACKET_KEY.matcher(key).matches()) {
				continue;
			}
			nestedKeys.add(key);
		}
		for (String key : nestedKeys) {
			Object value = m.remove(key);
			if (value == null) {
				continue;
			}
			List<String> segments = parseBracketKeySegments(key);
			if (segments.size() < 2) {
				m.put(key, value);
				continue;
			}
			putAtNestedPath(m, segments, value);
		}
		deepConvertNumericKeyMapsInPlace(m);
	}

	private static List<String> parseBracketKeySegments(String key) {
		int idx = key.indexOf('[');
		if (idx < 0) {
			return List.of(key);
		}
		String root = key.substring(0, idx);
		List<String> segments = new ArrayList<>();
		segments.add(root);
		Matcher matcher = BRACKET_SEGMENT.matcher(key.substring(idx));
		while (matcher.find()) {
			segments.add(matcher.group(1));
		}
		return segments;
	}

	@SuppressWarnings("unchecked")
	private static void putAtNestedPath(Map<String, Object> root, List<String> segments, Object value) {
		Map<String, Object> current = root;
		for (int i = 0; i < segments.size() - 1; i++) {
			String seg = segments.get(i);
			Object existing = current.get(seg);
			if (existing instanceof Map<?, ?> existingMap) {
				current = (Map<String, Object>) existingMap;
			} else {
				Map<String, Object> child = new LinkedHashMap<>();
				current.put(seg, child);
				current = child;
			}
		}
		String leaf = segments.get(segments.size() - 1);
		if (current.containsKey(leaf)) {
			current.put(leaf, mergeValueObjects(current.get(leaf), value));
		} else {
			current.put(leaf, value);
		}
	}

	private static void deepConvertNumericKeyMapsInPlace(Map<String, Object> m) {
		for (String key : new ArrayList<>(m.keySet())) {
			m.put(key, deepConvertNumericKeyMaps(m.get(key)));
		}
	}

	@SuppressWarnings("unchecked")
	private static Object deepConvertNumericKeyMaps(Object value) {
		if (value instanceof Map<?, ?> raw) {
			Map<String, Object> map = (Map<String, Object>) raw;
			Map<String, Object> converted = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				converted.put(entry.getKey(), deepConvertNumericKeyMaps(entry.getValue()));
			}
			if (!converted.isEmpty()
					&& converted.keySet().stream().allMatch(k -> NUMERIC_KEY.matcher(k).matches())) {
				List<Object> ordered = new ArrayList<>();
				converted.entrySet().stream()
						.sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())))
						.forEach(e -> ordered.add(e.getValue()));
				return ordered;
			}
			return converted;
		}
		if (value instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object element : list) {
				out.add(deepConvertNumericKeyMaps(element));
			}
			return out;
		}
		return value;
	}

	/** Merges {@code base[0]}, {@code base[1]}, … into {@code base} as an ordered list. */
	private static void foldIndexedBracketKeys(Map<String, Object> m) {
		Map<String, TreeMap<Integer, Object>> grouped = new LinkedHashMap<>();
		List<String> remove = new ArrayList<>();
		for (String key : new ArrayList<>(m.keySet())) {
			Matcher matcher = INDEXED_BRACKET_KEY.matcher(key);
			if (!matcher.matches()) {
				continue;
			}
			String base = matcher.group(1);
			if (!StringUtils.hasText(base)) {
				continue;
			}
			int idx = Integer.parseInt(matcher.group(2));
			grouped.computeIfAbsent(base, k -> new TreeMap<>()).put(idx, m.get(key));
			remove.add(key);
		}
		for (String key : remove) {
			m.remove(key);
		}
		for (Map.Entry<String, TreeMap<Integer, Object>> ge : grouped.entrySet()) {
			String base = ge.getKey();
			List<Object> ordered = new ArrayList<>();
			for (Object cell : ge.getValue().values()) {
				appendAsListElements(cell, ordered);
			}
			putMerging(m, base, ordered);
		}
	}

	private static void putMerging(Map<String, Object> m, String key, Object incoming) {
		if (!m.containsKey(key)) {
			m.put(key, incoming);
			return;
		}
		m.put(key, mergeValueObjects(m.get(key), incoming));
	}

	private static Object mergeValueObjects(Object a, Object b) {
		List<Object> out = new ArrayList<>();
		appendAsListElements(a, out);
		appendAsListElements(b, out);
		return out;
	}

	private static void appendAsListElements(Object v, List<Object> out) {
		if (v instanceof List<?> list) {
			out.addAll(list);
		} else {
			out.add(v);
		}
	}
}
