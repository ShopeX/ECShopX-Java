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

package cn.shopex.ecshopx.employeepurchase.integration;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseEmptyCartPort;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartDeleteService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseFastBuyRedisService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateEmployeePurchaseEmptyCartPortImpl implements OrderCreateEmployeePurchaseEmptyCartPort {

	private final EmployeePurchaseCartDeleteService employeePurchaseCartDeleteService;
	private final EmployeePurchaseFastBuyRedisService employeePurchaseFastBuyRedisService;

	public OrderCreateEmployeePurchaseEmptyCartPortImpl(
			EmployeePurchaseCartDeleteService employeePurchaseCartDeleteService,
			EmployeePurchaseFastBuyRedisService employeePurchaseFastBuyRedisService) {
		this.employeePurchaseCartDeleteService = employeePurchaseCartDeleteService;
		this.employeePurchaseFastBuyRedisService = employeePurchaseFastBuyRedisService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void emptyCart(NormalOrderCreateParams p, long userId) {
		Map<String, Object> pr = p.getParams();
		long companyId = longVal(pr.get("company_id"), 0L);
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long cartUserId = userId > 0L ? userId : longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L || cartUserId <= 0L) {
			return;
		}
		String cartType = stringVal(pr.get("cart_type"));
		if (cartType.isEmpty()) {
			cartType = "cart";
		}
		if ("fastbuy".equals(cartType)) {
			employeePurchaseFastBuyRedisService.setFastBuyCart(
					companyId, enterpriseId, activityId, cartUserId, new LinkedHashMap<>());
			return;
		}
		Object itemsRaw = pr.get("items");
		if (!(itemsRaw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		List<Long> itemIds = collectItemIds(list);
		if (itemIds.isEmpty()) {
			return;
		}
		employeePurchaseCartDeleteService.deleteByItemIds(
				companyId, cartUserId, enterpriseId, activityId, itemIds);
	}

	private static List<Long> collectItemIds(List<?> list) {
		List<Long> itemIds = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> row) {
				long itemId = longVal(row.get("item_id"), 0L);
				if (itemId > 0L) {
					itemIds.add(itemId);
				}
			}
		}
		return itemIds;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
