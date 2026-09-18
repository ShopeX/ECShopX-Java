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
import cn.shopex.ecshopx.orders.service.brokerage.TradeFinishCountBrokerageBusService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeFinishCountBrokerageDispatchListener implements DispatchListener {

	private final TradeFinishCountBrokerageBusService tradeFinishCountBrokerageBusService;

	public TradeFinishCountBrokerageDispatchListener(TradeFinishCountBrokerageBusService tradeFinishCountBrokerageBusService) {
		this.tradeFinishCountBrokerageBusService = tradeFinishCountBrokerageBusService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		if (isBlankOrInvalidOrderId(payload.get("order_id"))) {
			return;
		}
		if (isPointPayType(payload.get("pay_type"))) {
			return;
		}
		tradeFinishCountBrokerageBusService.handleTradeFinishRow(payload);
	}

	private static boolean isPointPayType(Object raw) {
		if (raw == null) {
			return false;
		}
		return "point".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	/**
	 * Mirrors early-return when order id is empty / non-positive before loading the order row.
	 */
	private static boolean isBlankOrInvalidOrderId(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.longValue() <= 0L;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return true;
		}
		try {
			return Long.parseLong(s) <= 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
