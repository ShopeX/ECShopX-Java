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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 人群规则「下单商品」params：详情扩字段 / 圈选抽 item_id。 */
public final class MemberTagSegmentRuleOrderItemParamsSupport {

	public record ItemView(String itemName, Object pics) {}

	private MemberTagSegmentRuleOrderItemParamsSupport() {}

	@SuppressWarnings("unchecked")
	public static void enrichConditionInPlace(Object condition, Map<Long, ItemView> byId) {
		if (condition instanceof List<?> list) {
			for (Object el : list) {
				enrichConditionInPlace(el, byId);
			}
			return;
		}
		if (!(condition instanceof Map<?, ?> raw)) {
			return;
		}
		Map<String, Object> map = (Map<String, Object>) raw;
		Object type = map.get("type");
		if ("orderItem".equals(type == null ? "" : String.valueOf(type).trim())) {
			map.put("params", enrichParams(map.get("params"), byId));
			for (Map.Entry<String, Object> e : map.entrySet()) {
				if ("params".equals(e.getKey())) {
					continue;
				}
				Object v = e.getValue();
				if (v instanceof List<?> || v instanceof Map<?, ?>) {
					enrichConditionInPlace(v, byId);
				}
			}
			return;
		}
		for (Object v : map.values()) {
			if (v instanceof List<?> || v instanceof Map<?, ?>) {
				enrichConditionInPlace(v, byId);
			}
		}
	}

	public static List<Long> collectItemIds(Object condition) {
		Set<Long> ids = new LinkedHashSet<>();
		collectItemIdsInto(condition, ids);
		return List.copyOf(ids);
	}

	public static List<Long> extractItemIds(JsonNode params) {
		if (params == null || !params.isArray() || params.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> itemIds = new ArrayList<>();
		for (JsonNode n : params) {
			long id = readItemId(n);
			if (id > 0L) {
				itemIds.add(id);
			}
		}
		return itemIds;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> enrichParams(Object paramsRaw, Map<Long, ItemView> byId) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (!(paramsRaw instanceof List<?> list)) {
			return out;
		}
		Map<Long, ItemView> views = byId == null ? Map.of() : byId;
		for (Object el : list) {
			long itemId = readItemIdObject(el);
			if (itemId <= 0L) {
				continue;
			}
			ItemView view = views.get(itemId);
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("item_id", itemId);
			row.put("item_name", view == null || view.itemName() == null ? "" : view.itemName());
			Object pics = view == null || view.pics() == null ? List.of() : view.pics();
			row.put("pics", pics);
			out.add(row);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static void collectItemIdsInto(Object node, Set<Long> ids) {
		if (node instanceof List<?> list) {
			for (Object el : list) {
				collectItemIdsInto(el, ids);
			}
			return;
		}
		if (!(node instanceof Map<?, ?> raw)) {
			return;
		}
		Map<String, Object> map = (Map<String, Object>) raw;
		Object type = map.get("type");
		if ("orderItem".equals(type == null ? "" : String.valueOf(type).trim())) {
			Object params = map.get("params");
			if (params instanceof List<?> list) {
				for (Object el : list) {
					long id = readItemIdObject(el);
					if (id > 0L) {
						ids.add(id);
					}
				}
			}
		}
		for (Object v : map.values()) {
			if (v instanceof List<?> || v instanceof Map<?, ?>) {
				collectItemIdsInto(v, ids);
			}
		}
	}

	private static long readItemId(JsonNode n) {
		if (n == null || n.isNull()) {
			return 0L;
		}
		if (n.isNumber()) {
			return n.asLong(0L);
		}
		if (n.isTextual()) {
			return parseLongOrZero(n.asText());
		}
		if (n.isObject()) {
			JsonNode id = n.get("item_id");
			if (id == null) {
				return 0L;
			}
			if (id.isNumber()) {
				return id.asLong(0L);
			}
			if (id.isTextual()) {
				return parseLongOrZero(id.asText());
			}
		}
		return 0L;
	}

	@SuppressWarnings("unchecked")
	private static long readItemIdObject(Object el) {
		if (el == null) {
			return 0L;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			return parseLongOrZero(s);
		}
		if (el instanceof Map<?, ?> m) {
			Object id = m.get("item_id");
			if (id instanceof Number n) {
				return n.longValue();
			}
			if (id != null) {
				return parseLongOrZero(String.valueOf(id));
			}
		}
		return 0L;
	}

	private static long parseLongOrZero(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
