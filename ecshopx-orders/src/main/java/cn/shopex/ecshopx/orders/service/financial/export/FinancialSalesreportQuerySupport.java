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

package cn.shopex.ecshopx.orders.service.financial.export;

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FinancialSalesreportQuerySupport {

	private FinancialSalesreportQuerySupport() {}

	public static LambdaQueryWrapper<NormalOrdersItems> toWrapper(LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<NormalOrdersItems> w = new LambdaQueryWrapper<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			switch (key) {
				case "company_id" -> w.eq(NormalOrdersItems::getCompanyId, toLong(val));
				case "delivery_status" -> w.eq(NormalOrdersItems::getDeliveryStatus, String.valueOf(val).trim());
				case "order_id" -> applyOrderIdEq(w, val);
				case "create_time|gte" -> w.ge(NormalOrdersItems::getCreateTime, toIntEpoch(val));
				case "create_time|lte" -> w.le(NormalOrdersItems::getCreateTime, toIntEpoch(val));
				case "delivery_time|gte" -> w.ge(NormalOrdersItems::getDeliveryTime, toIntEpoch(val));
				case "delivery_time|lte" -> w.le(NormalOrdersItems::getDeliveryTime, toIntEpoch(val));
				case "item_id_in" -> applyItemIdIn(w, val);
				case "item_id_impossible" -> w.apply("1 = 0");
				default -> {
					// ignore unknown keys
				}
			}
		}
		return w;
	}

	private static void applyOrderIdEq(LambdaQueryWrapper<NormalOrdersItems> w, Object val) {
		String s = String.valueOf(val).trim();
		if (s.isEmpty()) {
			return;
		}
		try {
			w.eq(NormalOrdersItems::getOrderId, Long.parseLong(s));
		} catch (NumberFormatException ignored) {
			w.apply("order_id = {0}", s);
		}
	}

	@SuppressWarnings("unchecked")
	private static void applyItemIdIn(LambdaQueryWrapper<NormalOrdersItems> w, Object val) {
		if (!(val instanceof List<?> raw) || raw.isEmpty()) {
			w.apply("1 = 0");
			return;
		}
		List<Long> ids = (List<Long>) raw;
		w.in(NormalOrdersItems::getItemId, ids);
	}

	private static int toIntEpoch(Object val) {
		if (val instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(val).trim());
	}

	private static long toLong(Object val) {
		if (val instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(val).trim());
	}
}
