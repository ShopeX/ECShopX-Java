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
import cn.shopex.ecshopx.common.dispatch.NormalOrderPaySuccessDispatchPublisher;
import cn.shopex.ecshopx.orders.service.tradefinish.OrdersTradeFinishNormalOrderPaySuccessApplyService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener implements DispatchListener {

	private final OrdersTradeFinishNormalOrderPaySuccessApplyService ordersTradeFinishNormalOrderPaySuccessApplyService;
	private final NormalOrderPaySuccessDispatchPublisher normalOrderPaySuccessDispatchPublisher;

	public OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener(
			OrdersTradeFinishNormalOrderPaySuccessApplyService ordersTradeFinishNormalOrderPaySuccessApplyService,
			NormalOrderPaySuccessDispatchPublisher normalOrderPaySuccessDispatchPublisher) {
		this.ordersTradeFinishNormalOrderPaySuccessApplyService = ordersTradeFinishNormalOrderPaySuccessApplyService;
		this.normalOrderPaySuccessDispatchPublisher = normalOrderPaySuccessDispatchPublisher;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		ordersTradeFinishNormalOrderPaySuccessApplyService
				.markPayedIfNeeded(payload)
				.ifPresent(normalOrderPaySuccessDispatchPublisher::publish);
	}
}
