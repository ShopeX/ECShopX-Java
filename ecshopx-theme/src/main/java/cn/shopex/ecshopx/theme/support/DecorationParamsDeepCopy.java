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

package cn.shopex.ecshopx.theme.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deep-copies decoration params maps/lists up to a fixed depth for isolated filter passes.
 */
public final class DecorationParamsDeepCopy {

	private static final int MAX_DEPTH = 6;

	private DecorationParamsDeepCopy() {}

	public static Map<String, Object> copy(Map<String, Object> root) {
		if (root == null) {
			return new LinkedHashMap<>();
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = (Map<String, Object>) (Map<?, ?>) root;
		return copyMap(cast, 0);
	}

	private static Map<String, Object> copyMap(Map<String, Object> src, int depth) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (depth >= MAX_DEPTH) {
			out.putAll(src);
			return out;
		}
		for (Map.Entry<String, Object> e : src.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			out.put(k, copyValue(v, depth + 1));
		}
		return out;
	}

	private static List<Object> copyList(List<?> src, int depth) {
		List<Object> out = new ArrayList<>(src.size());
		if (depth >= MAX_DEPTH) {
			out.addAll(src);
			return out;
		}
		for (Object v : src) {
			out.add(copyValue(v, depth + 1));
		}
		return out;
	}

	private static Object copyValue(Object v, int depth) {
		if (v == null) {
			return null;
		}
		if (v instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> sm = (Map<String, Object>) (Map<?, ?>) m;
			return depth >= MAX_DEPTH ? m : copyMap(sm, depth);
		}
		if (v instanceof List<?> list) {
			return depth >= MAX_DEPTH ? v : copyList(list, depth);
		}
		return v;
	}
}
