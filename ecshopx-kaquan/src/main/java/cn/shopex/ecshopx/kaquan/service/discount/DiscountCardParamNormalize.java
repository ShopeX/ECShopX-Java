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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class DiscountCardParamNormalize {

	private static final ObjectMapper OM = new ObjectMapper();

	private DiscountCardParamNormalize() {}

	/** {@code isset($x) && is_array($x) ? $x : []} */
	public static List<Object> phpArrayOrEmpty(Object raw) {
		if (raw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		return Collections.emptyList();
	}

	/**
	 * 与计划 §1 rel_distributor_ids / distributor_id 规范化一致。
	 */
	public static List<String> normalizeToStringList(Object raw) {
		if (raw == null) {
			return Collections.emptyList();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					String s = String.valueOf(o).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
			}
			return out;
		}
		if (raw instanceof String[] arr) {
			return Arrays.stream(arr).map(String::trim).filter(s -> !s.isEmpty()).toList();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return Collections.emptyList();
			}
			if (t.startsWith("[") && t.endsWith("]")) {
				try {
					List<Object> parsed = OM.readValue(t, new TypeReference<>() {});
					return normalizeToStringList(parsed);
				} catch (Exception e) {
					return splitComma(t);
				}
			}
			return splitComma(t);
		}
		String single = String.valueOf(raw).trim();
		return single.isEmpty() ? Collections.emptyList() : List.of(single);
	}

	private static List<String> splitComma(String t) {
		String[] parts = t.split(",");
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			String x = p.trim();
			if (!x.isEmpty()) {
				out.add(x);
			}
		}
		return out;
	}

	public static String stringVal(Object o) {
		return o == null ? null : String.valueOf(o).trim();
	}

	public static boolean isTruthyString(Object o) {
		String s = stringVal(o);
		return StringUtils.hasText(s) && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	/** PHP {@code isset($x) && $x}：空集合/空 Map/空串/0/false 视为无值。 */
	public static boolean hasPhpTruthyBodyValue(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (o instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return isTruthyString(o);
	}

	public static int intFromMap(Map<String, Object> m, String key, int defaultVal) {
		if (m == null || !m.containsKey(key)) {
			return defaultVal;
		}
		return parseIntFlexible(m.get(key), defaultVal);
	}

	public static int parseIntFlexible(Object o, int defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	public static long longFromObject(Object o, long defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	public static boolean isNumericString(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Number) {
			return true;
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isNumericOptional(Map<String, Object> m, String key) {
		if (m == null || !m.containsKey(key) || m.get(key) == null) {
			return true;
		}
		Object v = m.get(key);
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return true;
		}
		return isNumericString(v);
	}

	public static Set<String> allowedCouponTypes() {
		Set<String> s = new LinkedHashSet<>();
		s.add("mall");
		s.add("guide");
		return s;
	}

	public static String normalizeCouponType(Object raw) {
		String v = stringVal(raw);
		if (!StringUtils.hasText(v)) {
			return "mall";
		}
		String lower = v.toLowerCase(Locale.ROOT);
		return allowedCouponTypes().contains(lower) ? lower : "mall";
	}
}
