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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpOrderCancelStatusPort;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(SaasErpOrderCancelStatusPort.class)
public class UpdateOrderReviewStatusSaasErpAct implements SaasErpActHandler {

	private static final String SUCCESS_CANCEL_STATUS = "SUCCESS";

	private final SaasErpOrderCancelStatusPort orderCancelStatusPort;

	public UpdateOrderReviewStatusSaasErpAct(SaasErpOrderCancelStatusPort orderCancelStatusPort) {
		this.orderCancelStatusPort = orderCancelStatusPort;
	}

	@Override
	public String act() {
		return "update_order_status";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String orderId = params.get("order_id");
		if (orderId == null || orderId.isBlank()) {
			return SaasErpInboundResult.fail("订单信息有误");
		}
		orderId = orderId.trim();

		Optional<String> cancelStatus = orderCancelStatusPort.findCancelStatus(companyId, orderId);
		if (!matchesReviewSuccess(params, cancelStatus)) {
			return SaasErpInboundResult.fail("订单信息有误");
		}
		return SaasErpInboundResult.ok("操作成功", Map.of("status", true));
	}

	private static boolean matchesReviewSuccess(Map<String, String> params, Optional<String> cancelStatus) {
		if (cancelStatus.isEmpty()) {
			return false;
		}
		return "dead".equals(params.get("status"))
				&& "status".equals(params.get("type"))
				&& SUCCESS_CANCEL_STATUS.equals(cancelStatus.get());
	}
}
