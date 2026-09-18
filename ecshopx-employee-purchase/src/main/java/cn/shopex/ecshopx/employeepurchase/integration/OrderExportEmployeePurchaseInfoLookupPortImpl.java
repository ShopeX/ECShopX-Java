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

import cn.shopex.ecshopx.common.port.order.OrderExportEmployeePurchaseInfoLookupPort;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderExportEmployeePurchaseInfoLookupPortImpl implements OrderExportEmployeePurchaseInfoLookupPort {

	private final OrdersRelActivityMapper ordersRelActivityMapper;

	public OrderExportEmployeePurchaseInfoLookupPortImpl(OrdersRelActivityMapper ordersRelActivityMapper) {
		this.ordersRelActivityMapper = ordersRelActivityMapper;
	}

	@Override
	public Map<Long, Info> lookupByOrderIds(long companyId, List<Long> orderIds) {
		Map<Long, Info> out = new HashMap<>();
		if (orderIds == null || orderIds.isEmpty()) {
			return out;
		}
		for (Long orderId : orderIds) {
			if (orderId == null || orderId <= 0L) {
				continue;
			}
			Map<String, Object> row = ordersRelActivityMapper.selectDetailForOrder(companyId, orderId);
			if (row == null || row.isEmpty()) {
				continue;
			}
			out.put(
					orderId,
					new Info(
							stringVal(row.get("type")),
							stringVal(row.get("employee_name")),
							stringVal(row.get("enterprise_name")),
							stringVal(row.get("purchase_mode")),
							toLongOrNull(row.get("activity_id")),
							stringVal(row.get("activity_name"))));
		}
		return out;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static Long toLongOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
