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
 * Order process log entities map for admin confirm-cancel when the refund sheet is READY and the audit rejects
 * the refund (non-agree path on abstract normal orders).
 */
public final class AbstractAdminApiConfirmCancelRejectRefundReadyOrderProcessLogEntities {

	private AbstractAdminApiConfirmCancelRejectRefundReadyOrderProcessLogEntities() {}

	public static Map<String, Object> buildForRejectRefundReady(
			long orderId,
			long companyId,
			long operatorId,
			String operatorType,
			String shopRejectReason,
			Map<String, Object> params) {
		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		String normalizedOpType = normalizeOperatorType(operatorType);
		String reason = shopRejectReason == null ? "" : shopRejectReason.trim();
		log.put("order_id", orderId);
		log.put("company_id", companyId);
		log.put("operator_type", normalizedOpType);
		log.put("operator_id", operatorId);
		log.put("remarks", "订单退款");
		log.put("detail", "订单号：" + orderId + "，后台管理员拒绝退款，拒绝原因：" + reason);
		log.put("params", params);
		log.put("is_show", Boolean.FALSE);
		return log;
	}

	private static String normalizeOperatorType(String operatorType) {
		if (operatorType == null) {
			return "system";
		}
		String t = operatorType.trim();
		return t.isEmpty() ? "system" : t;
	}
}
