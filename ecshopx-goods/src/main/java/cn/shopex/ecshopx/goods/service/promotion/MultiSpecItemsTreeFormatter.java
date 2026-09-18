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

package cn.shopex.ecshopx.goods.service.promotion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class MultiSpecItemsTreeFormatter {

	private MultiSpecItemsTreeFormatter() {}

	/**
	 * Merges rows by {@code default_item_id} / {@code item_id} and nests multi-spec rows under {@code spec_items}.
	 */
	public static List<Map<String, Object>> formatItemsList(List<Map<String, Object>> rows) {
		Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long actualItemId = toLong(row.get("item_id"));
			long def = toLong(row.get("default_item_id"));
			long itemIdKey = (def > 0L) ? def : actualItemId;
			if (!result.containsKey(itemIdKey)) {
				Map<String, Object> main = new LinkedHashMap<>(row);
				main.put("item_id", itemIdKey);
				result.put(itemIdKey, main);
			}
			if (isMultiSpecRow(row)) {
				Map<String, Object> main = result.get(itemIdKey);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> specs =
						(List<Map<String, Object>>) main.computeIfAbsent("spec_items", k -> new ArrayList<>());
				specs.add(row);
			}
		}
		return new ArrayList<>(result.values());
	}

	private static boolean isMultiSpecRow(Map<String, Object> row) {
		Object ns = row.get("nospec");
		return ns instanceof Boolean z
				? !z
				: "false".equalsIgnoreCase(String.valueOf(ns)) || "0".equals(String.valueOf(ns));
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
