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
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Registers {@code EVENT_TRADE_FINISH} listener after payment-success OPL (order 97) and before any
 * later trade-finish configs; uses synchronous dispatch to mirror legacy listener thread semantics.
 */
@Configuration
@Order(98)
public class OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListenerRegistrationConfig {

	private static final String LISTENER_NAME = "listener:orders.trade_finish_offline_pay_canceled_normal_refund";

	private final DispatchRegistry dispatchRegistry;
	private final OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener listener;

	public OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener listener) {
		this.dispatchRegistry = dispatchRegistry;
		this.listener = listener;
	}

	@PostConstruct
	public void registerOrdersTradeFinishOfflinePayCanceledNormalOrderRefundListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH_CSV292,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				listener);
	}
}
