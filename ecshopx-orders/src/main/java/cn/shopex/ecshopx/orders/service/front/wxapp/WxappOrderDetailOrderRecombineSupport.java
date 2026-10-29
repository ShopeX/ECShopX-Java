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

import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailAftersalesApplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailOrderRecombineSupport {

	private final AdminOrderDetailAftersalesApplier adminOrderDetailAftersalesApplier;

	public WxappOrderDetailOrderRecombineSupport(
			AdminOrderDetailAftersalesApplier adminOrderDetailAftersalesApplier) {
		this.adminOrderDetailAftersalesApplier = adminOrderDetailAftersalesApplier;
	}

	@SuppressWarnings("unchecked")
	public void applyIfPresent(String associationOrderType, Map<String, Object> result) {
		if ("memberCard".equals(associationOrderType) || "service".equals(associationOrderType)) {
			return;
		}
		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null) {
			return;
		}
		Object itemsObj = orderInfo.get("items");
		if (!(itemsObj instanceof List<?> rawItems)) {
			return;
		}
		String receiptType = str(orderInfo.get("receipt_type"));
		boolean isZitiOrder = false;
		if ("ziti".equals(receiptType)) {
			String zs = str(orderInfo.get("ziti_status"));
			if ("DONE".equals(zs) || "NOTZITI".equals(zs)) {
				isZitiOrder = true;
			}
		}
		List<Map<String, Object>> itemList = new ArrayList<>();
		for (Object el : rawItems) {
			if (!(el instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) el;
			itemList.add(item);
			if (isZitiOrder) {
				item.put("delivery_item_num", intVal(item.get("num")));
			}
		}
		adminOrderDetailAftersalesApplier.apply(orderInfo, itemList);
		List<Map<String, Object>> logistics = new ArrayList<>();
		List<Map<String, Object>> normal = new ArrayList<>();
		for (Map<String, Object> item : itemList) {
			if (isLogisticsItem(item)) {
				logistics.add(item);
			} else {
				normal.add(item);
			}
		}
		orderInfo.put("is_split", Boolean.FALSE);
		if (!logistics.isEmpty() && !normal.isEmpty()) {
			orderInfo.put("is_split", Boolean.TRUE);
		}
		if (normal.isEmpty()) {
			orderInfo.put("items", logistics);
			orderInfo.put("logistics_items", new ArrayList<Map<String, Object>>());
		} else {
			orderInfo.put("items", normal);
			orderInfo.put("logistics_items", logistics);
		}
	}

	private static boolean isLogisticsItem(Map<String, Object> item) {
		Object flag = item.get("is_logistics");
		if (Boolean.TRUE.equals(flag)) {
			return true;
		}
		return flag instanceof Number n && n.intValue() == 1;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
