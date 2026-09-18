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
import cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersaleCancelSendSaasErpDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(84)
public class TradeAftersaleCancelSendSaasErpDispatchListenerRegistrationConfig {

	private static final String LISTENER_TRADE_AFTERSALE_CANCEL_SEND_SAAS_ERP =
			"listener:thirdparty.listeners.TradeAftersaleCancelSendSaasErp";

	private final DispatchRegistry dispatchRegistry;
	private final TradeAftersaleCancelSendSaasErpDispatchListener tradeAftersaleCancelSendSaasErpDispatchListener;

	public TradeAftersaleCancelSendSaasErpDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			TradeAftersaleCancelSendSaasErpDispatchListener tradeAftersaleCancelSendSaasErpDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.tradeAftersaleCancelSendSaasErpDispatchListener = tradeAftersaleCancelSendSaasErpDispatchListener;
	}

	@PostConstruct
	public void registerTradeAftersaleCancelSendSaasErpListener() {
		dispatchRegistry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP,
				LISTENER_TRADE_AFTERSALE_CANCEL_SEND_SAAS_ERP,
				ListenerDispatchOptions.async("default", null),
				tradeAftersaleCancelSendSaasErpDispatchListener);
	}
}
