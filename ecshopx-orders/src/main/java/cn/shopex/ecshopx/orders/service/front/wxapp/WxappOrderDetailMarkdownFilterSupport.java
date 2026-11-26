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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C 端订单详情出参过滤：不向前台暴露订单改价（type=mark_down）。
 */
final class WxappOrderDetailMarkdownFilterSupport {

	private static final String MARK_DOWN_TYPE = "mark_down";

	private WxappOrderDetailMarkdownFilterSupport() {}

	@SuppressWarnings("unchecked")
	static void stripMarkDown(Map<String, Object> orderInfo) {
		if (orderInfo == null || orderInfo.isEmpty()) {
			return;
		}
		if (orderInfo.containsKey("discount_info")) {
			orderInfo.put("discount_info", withoutMarkDown(orderInfo.get("discount_info")));
		}
		Object itemsRaw = orderInfo.get("items");
		if (!(itemsRaw instanceof List<?> items)) {
			return;
		}
		for (Object el : items) {
			if (!(el instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) el;
			if (!item.containsKey("discount_info")) {
				continue;
			}
			item.put("discount_info", withoutMarkDown(item.get("discount_info")));
		}
	}

	@SuppressWarnings("unchecked")
	private static Object withoutMarkDown(Object discountInfo) {
		if (discountInfo instanceof List<?> list) {
			List<Map<String, Object>> kept = new ArrayList<>();
			for (Object el : list) {
				if (!(el instanceof Map<?, ?> m)) {
					continue;
				}
				Map<String, Object> entry = (Map<String, Object>) m;
				if (isMarkDown(entry)) {
					continue;
				}
				kept.add(entry);
			}
			return kept;
		}
		if (discountInfo instanceof Map<?, ?> map) {
			Map<String, Object> kept = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				String key = String.valueOf(e.getKey());
				Object val = e.getValue();
				if (MARK_DOWN_TYPE.equals(key)) {
					continue;
				}
				if (val instanceof Map<?, ?> vm && isMarkDown((Map<String, Object>) vm)) {
					continue;
				}
				kept.put(key, val);
			}
			return kept;
		}
		return discountInfo;
	}

	private static boolean isMarkDown(Map<String, Object> entry) {
		Object type = entry.get("type");
		return type != null && MARK_DOWN_TYPE.equals(String.valueOf(type).trim());
	}
}
