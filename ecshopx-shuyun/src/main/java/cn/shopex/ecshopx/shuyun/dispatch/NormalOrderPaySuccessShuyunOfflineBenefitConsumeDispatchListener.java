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

package cn.shopex.ecshopx.shuyun.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.shuyun.service.openplatform.OfflineBenefitConsumePushService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NormalOrderPaySuccessShuyunOfflineBenefitConsumeDispatchListener implements DispatchListener {

	private final OfflineBenefitConsumePushService consumePushService;

	public NormalOrderPaySuccessShuyunOfflineBenefitConsumeDispatchListener(
			OfflineBenefitConsumePushService consumePushService) {
		this.consumePushService = consumePushService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		String orderId = payload.get("order_id") == null ? "" : String.valueOf(payload.get("order_id")).trim();
		if (companyId < 1 || !StringUtils.hasText(orderId)) {
			return;
		}
		if (payload.containsKey("user_id") && toLong(payload.get("user_id")) == 0) {
			return;
		}
		if (payload.containsKey("total_fee") && toInt(payload.get("total_fee")) <= 0) {
			return;
		}
		consumePushService.handlePaySuccess(companyId, orderId);
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int toInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (Exception e) {
			return 0;
		}
	}
}
