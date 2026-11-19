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
import cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersaleUpdateSendSaasErpDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(84)
public class TradeAftersaleUpdateSendSaasErpDispatchListenerRegistrationConfig {

	private static final String LISTENER_TRADE_AFTERSALE_UPDATE_SEND_SAAS_ERP =
			"listener:thirdparty.listeners.TradeAftersaleUpdateSendSaasErp";

	private final DispatchRegistry dispatchRegistry;
	private final TradeAftersaleUpdateSendSaasErpDispatchListener tradeAftersaleUpdateSendSaasErpDispatchListener;

	public TradeAftersaleUpdateSendSaasErpDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			TradeAftersaleUpdateSendSaasErpDispatchListener tradeAftersaleUpdateSendSaasErpDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.tradeAftersaleUpdateSendSaasErpDispatchListener = tradeAftersaleUpdateSendSaasErpDispatchListener;
	}

	@PostConstruct
	public void registerTradeAftersaleUpdateSendSaasErpListener() {
		dispatchRegistry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_AFTERSALES_UPDATE_SAAS_ERP,
				LISTENER_TRADE_AFTERSALE_UPDATE_SEND_SAAS_ERP,
				ListenerDispatchOptions.async("default", null),
				tradeAftersaleUpdateSendSaasErpDispatchListener);
	}
}
