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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseCartCountService {

	private final CartMapper cartMapper;

	public EmployeePurchaseCartCountService(CartMapper cartMapper) {
		this.cartMapper = cartMapper;
	}

	public Map<String, Object> countCart(
			long companyId, long userId, long enterpriseId, long activityId) {
		Map<String, Object> raw =
				cartMapper.selectCartCountSummary(companyId, userId, enterpriseId, activityId);
		if (raw == null || raw.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("cart_count", 0);
			empty.put("item_count", 0);
			return empty;
		}
		int cartCount = toIntLikeIntval(getMapValueIgnoreCase(raw, "cart_count"));
		int itemCount = toIntLikeIntval(getMapValueIgnoreCase(raw, "item_count"));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("cart_count", cartCount);
		out.put("item_count", itemCount);
		return out;
	}

	private static Object getMapValueIgnoreCase(Map<String, Object> m, String key) {
		Object direct = m.get(key);
		if (direct != null) {
			return direct;
		}
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}

	private static int toIntLikeIntval(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			if (s.indexOf('.') >= 0) {
				return (int) Double.parseDouble(s);
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
