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
import cn.shopex.ecshopx.orders.service.invoice.TradeFinishFapiaoBusService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TradeFinishFapiaoDispatchListener implements DispatchListener {

	private final TradeFinishFapiaoBusService tradeFinishFapiaoBusService;

	public TradeFinishFapiaoDispatchListener(TradeFinishFapiaoBusService tradeFinishFapiaoBusService) {
		this.tradeFinishFapiaoBusService = tradeFinishFapiaoBusService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		if (!"SUCCESS".equals(trimToEmpty(payload.get("trade_state")))) {
			return;
		}
		tradeFinishFapiaoBusService.handleTradeFinishRow(payload);
	}

	private static String trimToEmpty(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
