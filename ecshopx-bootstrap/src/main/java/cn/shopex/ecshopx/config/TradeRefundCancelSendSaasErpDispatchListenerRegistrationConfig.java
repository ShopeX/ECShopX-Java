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

import cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.thirdparty.dispatch.TradeRefundCancelSendSaasErpDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(85)
public class TradeRefundCancelSendSaasErpDispatchListenerRegistrationConfig {

	private static final String LISTENER_TRADE_REFUND_CANCEL_SEND_SAAS_ERP =
			"listener:thirdparty.listeners.TradeRefundCancelSendSaasErp";

	private final DispatchRegistry dispatchRegistry;
	private final TradeRefundCancelSendSaasErpDispatchListener tradeRefundCancelSendSaasErpDispatchListener;

	public TradeRefundCancelSendSaasErpDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			TradeRefundCancelSendSaasErpDispatchListener tradeRefundCancelSendSaasErpDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.tradeRefundCancelSendSaasErpDispatchListener = tradeRefundCancelSendSaasErpDispatchListener;
	}

	@PostConstruct
	public void registerTradeRefundCancelSendSaasErpListener() {
		dispatchRegistry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_CANCEL_SAAS_ERP,
				LISTENER_TRADE_REFUND_CANCEL_SEND_SAAS_ERP,
				ListenerDispatchOptions.async("default", null),
				tradeRefundCancelSendSaasErpDispatchListener);
	}
}
