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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdminOrderDetailAftersalesApplier {

	private final AftersalesDetailAggregateService aftersalesDetailAggregateService;

	public AdminOrderDetailAftersalesApplier(AftersalesDetailAggregateService aftersalesDetailAggregateService) {
		this.aftersalesDetailAggregateService = aftersalesDetailAggregateService;
	}

	public void apply(Map<String, Object> orderInfo, List<Map<String, Object>> items) {
		int canApplyAftersalesSum = 0;
		orderInfo.put("can_apply_aftersales", 0);
		long companyId = longVal(orderInfo.get("company_id"));
		String orderIdStr = str(orderInfo.get("order_id"));
		int now = (int) (System.currentTimeMillis() / 1000L);

		for (Map<String, Object> item : items) {
			int deliveryItemNum = intVal(item.get("delivery_item_num"));
			if (deliveryItemNum <= 0) {
				continue;
			}
			long subOrderId = longVal(item.get("id"));
			int appliedNum = aftersalesDetailAggregateService.getAppliedNum(companyId, orderIdStr, subOrderId);
			int cancelItemNum = intVal(item.get("cancel_item_num"));
			int leftAftersalesNum = deliveryItemNum + cancelItemNum - appliedNum;
			item.put("left_aftersales_num", leftAftersalesNum);
			item.put("show_aftersales", appliedNum > cancelItemNum ? 1 : 0);

			int autoClose = intVal(item.get("auto_close_aftersales_time"));
			if (autoClose > 0 && autoClose < now) {
				canApplyAftersalesSum += leftAftersalesNum;
				continue;
			}
			orderInfo.put("can_apply_aftersales", 1);
			canApplyAftersalesSum += leftAftersalesNum;
			if (canApplyAftersalesSum > 0) {
				orderInfo.put("can_apply_aftersales", 1);
			}
			if ("CANCEL".equals(str(orderInfo.get("order_status")))) {
				orderInfo.put("can_apply_aftersales", 0);
			}
		}
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
