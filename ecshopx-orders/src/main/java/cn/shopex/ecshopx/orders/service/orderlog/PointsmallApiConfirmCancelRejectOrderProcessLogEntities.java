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

package cn.shopex.ecshopx.orders.service.orderlog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payload map for order process log when the merchant API rejects a pointsmall normal order refund cancel request.
 */
public final class PointsmallApiConfirmCancelRejectOrderProcessLogEntities {

	private PointsmallApiConfirmCancelRejectOrderProcessLogEntities() {}

	public static LinkedHashMap<String, Object> buildForAdminRejectRefund(
			long orderId,
			long companyId,
			long operatorId,
			String operatorType,
			String shopRejectReason,
			Map<String, Object> params) {
		LinkedHashMap<String, Object> entities = new LinkedHashMap<>();
		String normalizedOpType = normalizeOperatorType(operatorType);
		String reason = shopRejectReason == null ? "" : shopRejectReason.trim();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", normalizedOpType);
		entities.put("operator_id", operatorId);
		entities.put("remarks", "订单退款");
		entities.put("detail", "订单号：" + orderId + "，用户申请退款拒绝，拒绝原因：" + reason);
		entities.put("params", params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params));
		entities.put("is_show", Boolean.FALSE);
		return entities;
	}

	private static String normalizeOperatorType(String operatorType) {
		if (operatorType == null) {
			return "system";
		}
		String t = operatorType.trim();
		return t.isEmpty() ? "system" : t;
	}
}
