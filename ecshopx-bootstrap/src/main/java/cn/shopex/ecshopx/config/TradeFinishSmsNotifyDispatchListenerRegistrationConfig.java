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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.orders.dispatch.TradeFinishSmsNotifyDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(78)
public class TradeFinishSmsNotifyDispatchListenerRegistrationConfig {

	private static final String LISTENER_NAME = "listener:orders.listeners.TradeFinishSmsNotify";

	private final DispatchRegistry dispatchRegistry;
	private final TradeFinishSmsNotifyDispatchListener tradeFinishSmsNotifyDispatchListener;

	public TradeFinishSmsNotifyDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			TradeFinishSmsNotifyDispatchListener tradeFinishSmsNotifyDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.tradeFinishSmsNotifyDispatchListener = tradeFinishSmsNotifyDispatchListener;
	}

	@PostConstruct
	public void registerTradeFinishSmsNotifyListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				tradeFinishSmsNotifyDispatchListener);
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH_CSV292,
				LISTENER_NAME,
				ListenerDispatchOptions.async("sms", null),
				tradeFinishSmsNotifyDispatchListener);
	}
}
