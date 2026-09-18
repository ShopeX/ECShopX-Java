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

package cn.shopex.ecshopx.popularize.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converts MyBatis map keys from camelCase to snake_case when the key contains uppercase letters. */
final class PromoterAdminListMapKeySnakeCaseUtil {

	private PromoterAdminListMapKeySnakeCaseUtil() {
	}

	static LinkedHashMap<String, Object> mapKeysCamelToSnake(Map<String, Object> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (src == null || src.isEmpty()) {
			return out;
		}
		for (Map.Entry<String, Object> e : src.entrySet()) {
			String k = e.getKey();
			if (k == null) {
				continue;
			}
			out.put(camelToSnakeKey(k), e.getValue());
		}
		return out;
	}

	private static String camelToSnakeKey(String name) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		boolean hasUpper = false;
		for (int i = 0; i < name.length(); i++) {
			if (Character.isUpperCase(name.charAt(i))) {
				hasUpper = true;
				break;
			}
		}
		if (!hasUpper) {
			return name;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i > 0) {
					sb.append('_');
				}
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
