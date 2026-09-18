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

package cn.shopex.ecshopx.orders.service.groups;

import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishOnPayPort;
import cn.shopex.ecshopx.orders.service.group.NormalGroupsTradeFinishRefundOrchestrator;
import cn.shopex.ecshopx.orders.service.group.NormalGroupsTradeFinishRefundOrchestrator.RefundBranchOutcome;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UpdateGroupsActivityOrderBusService {

	private static final Logger log = LoggerFactory.getLogger(UpdateGroupsActivityOrderBusService.class);

	private final NormalGroupsTradeFinishOnPayPort normalGroupsTradeFinishOnPayPort;
	private final NormalGroupsTradeFinishRefundOrchestrator normalGroupsTradeFinishRefundOrchestrator;

	public UpdateGroupsActivityOrderBusService(
			NormalGroupsTradeFinishOnPayPort normalGroupsTradeFinishOnPayPort,
			NormalGroupsTradeFinishRefundOrchestrator normalGroupsTradeFinishRefundOrchestrator) {
		this.normalGroupsTradeFinishOnPayPort = normalGroupsTradeFinishOnPayPort;
		this.normalGroupsTradeFinishRefundOrchestrator = normalGroupsTradeFinishRefundOrchestrator;
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRowSnakeCase) {
		if (tradeRowSnakeCase == null || tradeRowSnakeCase.isEmpty()) {
			return;
		}
		Long companyId = parsePositiveLong(tradeRowSnakeCase.get("company_id"));
		Long orderId = parsePositiveLong(tradeRowSnakeCase.get("order_id"));
		Long userId = parsePositiveLong(tradeRowSnakeCase.get("user_id"));
		if (companyId == null || orderId == null || userId == null) {
			log.debug("UpdateGroupsActivityOrder: skip, missing company_id/order_id/user_id");
			return;
		}
		String source = normalize(tradeRowSnakeCase.get("trade_source_type"));
		if (!"normal_groups".equalsIgnoreCase(source)) {
			return;
		}
		Object tradeState = tradeRowSnakeCase.get("trade_state");
		if (tradeState != null && !"SUCCESS".equalsIgnoreCase(String.valueOf(tradeState).trim())) {
			log.debug("UpdateGroupsActivityOrder: skip, trade_state not SUCCESS");
			return;
		}
		try {
			RefundBranchOutcome refundOutcome =
					normalGroupsTradeFinishRefundOrchestrator
							.tryRefundDisabledMemberInFormedTeamAndPublishTradeUpdate(companyId, userId, orderId);
			if (refundOutcome == RefundBranchOutcome.REFUND_PUBLISHED
					|| refundOutcome == RefundBranchOutcome.REFUND_SKIPPED) {
				return;
			}
			normalGroupsTradeFinishOnPayPort.onTradePaySuccess(companyId, userId, orderId);
		} catch (Throwable t) {
			log.debug("UpdateGroupsActivityOrder: mark payed failed: {}", t.toString());
		}
	}

	private static String normalize(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : "";
	}

	private static Long parsePositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
