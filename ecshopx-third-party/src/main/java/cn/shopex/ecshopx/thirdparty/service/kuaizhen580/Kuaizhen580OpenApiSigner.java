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

package cn.shopex.ecshopx.thirdparty.service.kuaizhen580;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Kuaizhen580OpenApiSigner {

	private Kuaizhen580OpenApiSigner() {}

	public static String signPayload(Map<String, Object> params, String clientSecret) {
		Object cleaned = removeEmptyValues(params);
		Object sorted = kSort(cleaned);
		@SuppressWarnings("unchecked")
		Map<String, Object> sortedMap = (Map<String, Object>) sorted;
		String signBase = buildTopLevelSignString(sortedMap);
		signBase += "key=" + clientSecret;
		return md5Upper(signBase);
	}

	private static String buildTopLevelSignString(Map<String, Object> sortedMap) {
		StringBuilder str = new StringBuilder();
		for (Map.Entry<String, Object> e : sortedMap.entrySet()) {
			String key = e.getKey();
			Object value = e.getValue();
			if (value instanceof Map<?, ?> m) {
				str.append(key).append('=').append(signStringByMap(m)).append('&');
			} else if (value instanceof List<?> list) {
				str.append(key).append('=').append(signStringByList(list)).append('&');
			} else if (value != null && !"".equals(value)) {
				str.append(key).append('=').append(value).append('&');
			}
		}
		return str.toString();
	}

	private static String signStringByMap(Map<?, ?> map) {
		TreeMap<String, Object> sorted = new TreeMap<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			sorted.put(String.valueOf(e.getKey()), e.getValue());
		}
		StringBuilder stringBuffer = new StringBuilder("{");
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object value = e.getValue();
			if (value == null || "".equals(value)) {
				continue;
			}
			stringBuffer.append(e.getKey()).append(':');
			if (value instanceof Map<?, ?> nested) {
				stringBuffer.append(signStringByMap(nested)).append(',');
			} else if (value instanceof List<?> list) {
				stringBuffer.append(signStringByList(list)).append(',');
			} else {
				stringBuffer.append(value).append(',');
			}
		}
		if (stringBuffer.length() > 1) {
			stringBuffer.setLength(stringBuffer.length() - 1);
		}
		stringBuffer.append('}');
		return stringBuffer.toString();
	}

	private static String signStringByList(List<?> list) {
		if (list == null || list.isEmpty()) {
			return "";
		}
		StringBuilder stringBuffer = new StringBuilder("[");
		for (Object value : list) {
			if (value instanceof Map<?, ?> m) {
				stringBuffer.append(signStringByMap(m)).append(',');
			} else if (value instanceof List<?> nested) {
				stringBuffer.append(signStringByList(nested)).append(',');
			} else {
				stringBuffer.append(value).append(',');
			}
		}
		if (stringBuffer.length() > 1) {
			stringBuffer.setLength(stringBuffer.length() - 1);
		}
		stringBuffer.append(']');
		return stringBuffer.toString();
	}

	@SuppressWarnings("unchecked")
	private static Object removeEmptyValues(Object value) {
		if (value instanceof Map<?, ?> rawMap) {
			Map<String, Object> result = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				String key = String.valueOf(e.getKey());
				Object v = e.getValue();
				if (v instanceof Map<?, ?>) {
					Object nv = removeEmptyValues(v);
					if (isNonEmpty(nv)) {
						result.put(key, nv);
					}
				} else if (v instanceof List<?> list) {
					if (!list.isEmpty()) {
						Object nv = removeEmptyValues(v);
						if (isNonEmpty(nv)) {
							result.put(key, nv);
						}
					} else {
						result.put(key, list);
					}
				} else {
					if (!"".equals(v)) {
						result.put(key, v);
					}
				}
			}
			return result;
		}
		if (value instanceof List<?> list) {
			if (list.isEmpty()) {
				return list;
			}
			List<Object> result = new ArrayList<>();
			for (Object item : list) {
				if (item instanceof Map<?, ?> || item instanceof List<?>) {
					Object ni = removeEmptyValues(item);
					if (isNonEmpty(ni)) {
						result.add(ni);
					}
				} else {
					if (!"".equals(item)) {
						result.add(item);
					}
				}
			}
			return result;
		}
		return value;
	}

	private static boolean isNonEmpty(Object o) {
		if (o instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (o instanceof List<?> l) {
			return !l.isEmpty();
		}
		return o != null && !"".equals(o);
	}

	private static Object kSort(Object value) {
		if (value instanceof Map<?, ?> raw) {
			TreeMap<String, Object> sorted = new TreeMap<>();
			for (Map.Entry<?, ?> e : raw.entrySet()) {
				sorted.put(String.valueOf(e.getKey()), kSort(e.getValue()));
			}
			return sorted;
		}
		if (value instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object item : list) {
				out.add(kSort(item));
			}
			return out;
		}
		return value;
	}

	private static String md5Upper(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString().toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException("快诊580药品同步失败");
		}
	}
}
