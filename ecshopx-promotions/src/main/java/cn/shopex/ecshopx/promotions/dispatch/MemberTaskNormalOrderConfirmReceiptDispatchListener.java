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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskGrantTrigger;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskRewardGrantService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MemberTaskNormalOrderConfirmReceiptDispatchListener implements DispatchListener {

	private static final Logger log =
			LoggerFactory.getLogger(MemberTaskNormalOrderConfirmReceiptDispatchListener.class);

	private final MemberTaskRewardGrantService memberTaskRewardGrantService;

	public MemberTaskNormalOrderConfirmReceiptDispatchListener(
			MemberTaskRewardGrantService memberTaskRewardGrantService) {
		this.memberTaskRewardGrantService = memberTaskRewardGrantService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		long companyId = longVal(payload == null ? null : payload.get("company_id"));
		long orderId = longVal(payload == null ? null : payload.get("order_id"));
		log.debug(
				"member_task listener={} companyId={} orderId={}",
				"MemberTaskNormalOrderConfirmReceiptDispatchListener",
				companyId,
				orderId);
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		memberTaskRewardGrantService.grantPendingIfOrderDone(
				companyId, orderId, MemberTaskGrantTrigger.CONFIRM_RECEIPT);
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
