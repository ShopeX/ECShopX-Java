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

package cn.shopex.ecshopx.members.listener;

import cn.shopex.ecshopx.common.event.TradeRefundSpringEvent;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagCodes;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagPayloadIds;
import cn.shopex.ecshopx.members.service.systemtag.NewCustomerTagReconcileService;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TradeRefundNewCustomerListener {

	private final NewCustomerTagReconcileService newCustomerTagReconcileService;

	public TradeRefundNewCustomerListener(NewCustomerTagReconcileService newCustomerTagReconcileService) {
		this.newCustomerTagReconcileService = newCustomerTagReconcileService;
	}

	@EventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void onTradeRefund(TradeRefundSpringEvent event) {
		Map<String, Object> payload = event == null ? null : event.getPayload();
		if (!"SUCCESS".equalsIgnoreCase(MemberSystemTagPayloadIds.text(payload, "refund_status"))) {
			return;
		}
		long companyId = MemberSystemTagPayloadIds.companyId(payload);
		long userId = MemberSystemTagPayloadIds.userId(payload);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		newCustomerTagReconcileService.reconcile(companyId, userId, MemberSystemTagCodes.SOURCE_REFUND);
	}
}
