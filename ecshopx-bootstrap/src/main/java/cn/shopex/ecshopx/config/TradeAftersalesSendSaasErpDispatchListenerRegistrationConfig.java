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
import cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersalesSendSaasErpDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(83)
public class TradeAftersalesSendSaasErpDispatchListenerRegistrationConfig {

	private static final String LISTENER_TRADE_AFTERSALES_SEND_SAAS_ERP =
			"listener:thirdparty.listeners.TradeAftersalesSendSaasErp";

	private final DispatchRegistry dispatchRegistry;
	private final TradeAftersalesSendSaasErpDispatchListener tradeAftersalesSendSaasErpDispatchListener;

	public TradeAftersalesSendSaasErpDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			TradeAftersalesSendSaasErpDispatchListener tradeAftersalesSendSaasErpDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.tradeAftersalesSendSaasErpDispatchListener = tradeAftersalesSendSaasErpDispatchListener;
	}

	@PostConstruct
	public void registerTradeAftersalesSendSaasErpListener() {
		dispatchRegistry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_SAAS_ERP,
				LISTENER_TRADE_AFTERSALES_SEND_SAAS_ERP,
				ListenerDispatchOptions.async("default", null),
				tradeAftersalesSendSaasErpDispatchListener);
	}
}
