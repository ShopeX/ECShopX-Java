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

package cn.shopex.ecshopx.promotions.service.order.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsInventoryPort;
import cn.shopex.ecshopx.promotions.service.GroupItemStoreService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateGroupsInventoryPortImpl implements OrderCreateGroupsInventoryPort {

	private final GroupItemStoreService groupItemStoreService;

	public OrderCreateGroupsInventoryPortImpl(GroupItemStoreService groupItemStoreService) {
		this.groupItemStoreService = groupItemStoreService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void minusGroupItemStoreIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!"normal_groups".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), longVal(od.get("company_id"), 0L));
		long actId = longVal(od.get("act_id"), longVal(pr.get("bargain_id"), 0L));
		if (actId <= 0L) {
			return;
		}
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		Object first = list.get(0);
		if (!(first instanceof Map<?, ?> row)) {
			return;
		}
		Map<String, Object> line = (Map<String, Object>) row;
		long itemId = longVal(line.get("item_id"), 0L);
		int num = intVal(line.get("num"), 1);
		if (!groupItemStoreService.minusGroupItemStore(companyId, actId, itemId, num)) {
			throw new ResourceException("拼团库存不足");
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
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

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
