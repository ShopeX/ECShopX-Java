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

package cn.shopex.ecshopx.members.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Normalizes MyBatis {@code Map} row keys from {@code map-underscore-to-camel-case} aliases to API
 * snake_case field names expected by admin list consumers.
 */
public final class MemberAdminListJdbcMapSnakeCase {

	private MemberAdminListJdbcMapSnakeCase() {
	}

	public static void normalizeTopLevelKeys(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return;
		}
		List<Map.Entry<String, Object>> entries = new ArrayList<>(row.entrySet());
		for (Map.Entry<String, Object> e : entries) {
			String key = e.getKey();
			if (key == null) {
				continue;
			}
			String snake = camelToSnakeColumnKey(key);
			if (snake.equals(key)) {
				continue;
			}
			if (!row.containsKey(snake)) {
				row.put(snake, e.getValue());
			}
			row.remove(key);
		}
	}

	private static String camelToSnakeColumnKey(String camel) {
		boolean hasUpper = false;
		for (int i = 0; i < camel.length(); i++) {
			if (Character.isUpperCase(camel.charAt(i))) {
				hasUpper = true;
				break;
			}
		}
		if (!hasUpper) {
			return camel;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < camel.length(); i++) {
			char c = camel.charAt(i);
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
