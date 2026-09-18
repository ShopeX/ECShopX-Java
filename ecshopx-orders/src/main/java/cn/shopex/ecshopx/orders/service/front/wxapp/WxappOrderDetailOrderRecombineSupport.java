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

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailOrderRecombineSupport {

	private final AftersalesDetailAggregateService aftersalesDetailAggregateService;

	public WxappOrderDetailOrderRecombineSupport(AftersalesDetailAggregateService aftersalesDetailAggregateService) {
		this.aftersalesDetailAggregateService = aftersalesDetailAggregateService;
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
		int canApplySum = 0;
		orderInfo.put("can_apply_aftersales", 0);
		String orderIdStr = str(orderInfo.get("order_id"));
		long companyId = longVal(orderInfo.get("company_id"));
		List<Map<String, Object>> itemList = new ArrayList<>();
		for (Object el : rawItems) {
			if (!(el instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) el;
			itemList.add(item);
			long subId = longVal(item.get("id"));
			int appliedNum = aftersalesDetailAggregateService.getAppliedNum(companyId, orderIdStr, subId);
			int num = intVal(item.get("num"));
			int deliveryItemNum = intVal(item.get("delivery_item_num"));
			if (isZitiOrder) {
				item.put("delivery_item_num", num);
				deliveryItemNum = num;
			}
			int cancelItemNum = intVal(item.get("cancel_item_num"));
			int left = deliveryItemNum + cancelItemNum - appliedNum;
			item.put("left_aftersales_num", left);
			item.put("show_aftersales", appliedNum > cancelItemNum ? 1 : 0);
			canApplySum += left;
			if (canApplySum > 0) {
				int autoClose = intVal(item.get("auto_close_aftersales_time"));
				int now = (int) (System.currentTimeMillis() / 1000L);
				if (autoClose > 0 && autoClose < now) {
					continue;
				}
				orderInfo.put("can_apply_aftersales", 1);
				if ("CANCEL".equals(str(orderInfo.get("order_status")))) {
					orderInfo.put("can_apply_aftersales", 0);
				}
			}
		}
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

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
