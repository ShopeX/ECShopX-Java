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

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeAfterLogiSendOmeDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.TradeAfterLogiSendSaasErpDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemLinkTradeAftersalesLogiDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final SystemLinkTradeAfterLogiSendOmeDispatchListener systemLinkTradeAfterLogiSendOmeDispatchListener;
	private final TradeAfterLogiSendSaasErpDispatchListener tradeAfterLogiSendSaasErpDispatchListener;

	public SystemLinkTradeAftersalesLogiDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			SystemLinkTradeAfterLogiSendOmeDispatchListener systemLinkTradeAfterLogiSendOmeDispatchListener,
			TradeAfterLogiSendSaasErpDispatchListener tradeAfterLogiSendSaasErpDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.systemLinkTradeAfterLogiSendOmeDispatchListener = systemLinkTradeAfterLogiSendOmeDispatchListener;
		this.tradeAfterLogiSendSaasErpDispatchListener = tradeAfterLogiSendSaasErpDispatchListener;
	}

	@PostConstruct
	public void registerTradeAftersalesLogiListeners() {
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_LOGI,
				"listener:systemlink.trade_after_logi_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				systemLinkTradeAfterLogiSendOmeDispatchListener);
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_LOGI,
				"listener:thirdparty.listeners.TradeAfterLogiSendSaasErp",
				ListenerDispatchOptions.async("default", null),
				tradeAfterLogiSendSaasErpDispatchListener);
	}
}
