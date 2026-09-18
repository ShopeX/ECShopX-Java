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

import cn.shopex.ecshopx.common.port.order.OrderDetailEmployeePurchaseEnrichmentPort;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailEmployeePurchaseEnrichmentPortImpl implements OrderDetailEmployeePurchaseEnrichmentPort {

	private final OrdersRelActivityMapper ordersRelActivityMapper;

	public OrderDetailEmployeePurchaseEnrichmentPortImpl(OrdersRelActivityMapper ordersRelActivityMapper) {
		this.ordersRelActivityMapper = ordersRelActivityMapper;
	}

	@Override
	public void enrich(long companyId, long orderId, String orderClass, Map<String, Object> orderInfo) {
		Map<String, Object> row = ordersRelActivityMapper.selectDetailForOrder(companyId, orderId);
		if (row == null || row.isEmpty()) {
			orderInfo.put("orders_purchase_info", null);
			return;
		}

		String purchaseMode = row.get("purchase_mode") == null ? "" : String.valueOf(row.get("purchase_mode"));
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("order_id", toLong(row.get("order_id")));
		detail.put("user_id", toLong(row.get("user_id")));
		detail.put("enterprise_id", toLong(row.get("enterprise_id")));
		detail.put("activity_id", toLong(row.get("activity_id")));
		detail.put("enterprise_name", row.get("enterprise_name"));
		detail.put("type", row.get("type") == null ? "" : String.valueOf(row.get("type")));
		detail.put("employee_name", row.get("employee_name"));
		detail.put("purchase_mode", purchaseMode);
		detail.put("purchase_mode_desc", PurchaseModeSupport.desc(purchaseMode));
		detail.put("activity_name", row.get("activity_name"));
		orderInfo.put("orders_purchase_info", detail);

		orderInfo.put("purchase_mode", purchaseMode.isEmpty() ? null : purchaseMode);
		orderInfo.put("purchase_mode_desc", PurchaseModeSupport.desc(purchaseMode));
		orderInfo.put("employee_purchase_activity_id", toLong(row.get("activity_id")));
		orderInfo.put(
				"employee_purchase_activity_name",
				row.get("activity_name") == null ? "" : String.valueOf(row.get("activity_name")));

		if (!"employee_purchase".equals(orderClass)) {
			return;
		}
		orderInfo.put("enterprise_id", toLong(row.get("enterprise_id")));
		orderInfo.put("activity_id", toLong(row.get("activity_id")));
		orderInfo.put("if_share_store", toBoolean(row.get("if_share_store")));
		orderInfo.put("close_modify_time", toIntegerOrNull(row.get("close_modify_time")));
	}

	private static long toLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean toBoolean(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return Boolean.parseBoolean(String.valueOf(v).trim());
	}

	private static Integer toIntegerOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
