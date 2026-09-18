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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.orders.service.workwechat.TradeFinishWorkWechatNotifyService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrdersTradeFinishWorkWechatDispatchListener implements DispatchListener {

	private final TradeFinishWorkWechatNotifyService tradeFinishWorkWechatNotifyService;

	public OrdersTradeFinishWorkWechatDispatchListener(
			TradeFinishWorkWechatNotifyService tradeFinishWorkWechatNotifyService) {
		this.tradeFinishWorkWechatNotifyService = tradeFinishWorkWechatNotifyService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		String companyIdStr = stringify(payload.get("company_id"));
		String orderIdStr = stringify(payload.get("order_id"));
		if (!StringUtils.hasText(companyIdStr) || !StringUtils.hasText(orderIdStr)) {
			return;
		}
		tradeFinishWorkWechatNotifyService.dispatchTradeFinishWorkWechatDeliveryWaitJobs(payload);
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
