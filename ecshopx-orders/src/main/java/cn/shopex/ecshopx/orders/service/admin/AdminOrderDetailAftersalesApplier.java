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
import java.util.LinkedHashMap;
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
		orderInfo.put("can_apply_aftersales", 0);
		long companyId = longVal(orderInfo.get("company_id"));
		String orderIdStr = str(orderInfo.get("order_id"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		boolean canApplyAftersales = false;

		for (Map<String, Object> item : items) {
			int num = intVal(item.get("num"));
			int deliveryItemNum = intVal(item.get("delivery_item_num"));
			long subOrderId = longVal(item.get("id"));
			int cancelItemNum = intVal(item.get("cancel_item_num"));
			AftersalesDetailAggregateService.OrderItemAftersalesQuota quota =
					aftersalesDetailAggregateService.calculateItemQuota(
							companyId, orderIdStr, subOrderId, num, deliveryItemNum, cancelItemNum);
			item.put("left_aftersales_num", quota.leftAftersalesNum());
			item.put("left_refund_only_num", quota.leftRefundOnlyNum());
			item.put("show_aftersales", quota.appliedNum() > cancelItemNum ? 1 : 0);
			applyRefundAmount(companyId, orderIdStr, subOrderId, item);

			int autoClose = intVal(item.get("auto_close_aftersales_time"));
			if (quota.leftAftersalesNum() + quota.leftRefundOnlyNum() > 0
					&& !(autoClose > 0 && autoClose < now)) {
				canApplyAftersales = true;
			}
		}

		boolean blocked =
				"PENDING".equals(str(orderInfo.get("delivery_status")))
						|| "CANCEL".equals(str(orderInfo.get("order_status")));
		if (canApplyAftersales && !blocked) {
			orderInfo.put("can_apply_aftersales", 1);
		}
	}

	private void applyRefundAmount(
			long companyId, String orderId, long subOrderId, Map<String, Object> item) {
		int totalFee = intVal(item.get("total_fee"));
		int pointFee = intVal(item.get("point_fee"));
		AftersalesDetailAggregateService.AppliedRefundAmount refundAmount =
				aftersalesDetailAggregateService.getAppliedRefundAmount(companyId, orderId, subOrderId);
		if (!refundAmount.exists()) {
			item.put("after_sales_fee", 0);
			item.put("remain_fee", totalFee);
			item.put("remain_point", pointFee);
			return;
		}
		Map<String, Object> afterSalesFee = new LinkedHashMap<>();
		afterSalesFee.put("refund_fee", refundAmount.refundFee());
		afterSalesFee.put("refund_point", refundAmount.refundPoint());
		item.put("after_sales_fee", afterSalesFee);
		item.put("remain_fee", totalFee - refundAmount.refundFee());
		item.put("remain_point", pointFee - refundAmount.refundPoint());
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
