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
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeRefundSendOmeDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeRefundPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.TradeRefundSendSaasErpDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemLinkTradeRefundDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final SystemLinkTradeRefundSendOmeDispatchListener systemLinkTradeRefundSendOmeDispatchListener;
	private final ThirdPartyTradeRefundPushMarketingCenterDispatchListener
			thirdPartyTradeRefundPushMarketingCenterDispatchListener;
	private final TradeRefundSendSaasErpDispatchListener tradeRefundSendSaasErpDispatchListener;

	public SystemLinkTradeRefundDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			SystemLinkTradeRefundSendOmeDispatchListener systemLinkTradeRefundSendOmeDispatchListener,
			ThirdPartyTradeRefundPushMarketingCenterDispatchListener
					thirdPartyTradeRefundPushMarketingCenterDispatchListener,
			TradeRefundSendSaasErpDispatchListener tradeRefundSendSaasErpDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.systemLinkTradeRefundSendOmeDispatchListener = systemLinkTradeRefundSendOmeDispatchListener;
		this.thirdPartyTradeRefundPushMarketingCenterDispatchListener =
				thirdPartyTradeRefundPushMarketingCenterDispatchListener;
		this.tradeRefundSendSaasErpDispatchListener = tradeRefundSendSaasErpDispatchListener;
	}

	@PostConstruct
	public void registerTradeRefundListeners() {
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:systemlink.trade_refund_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				systemLinkTradeRefundSendOmeDispatchListener);
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:thirdparty.trade_refund_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				thirdPartyTradeRefundPushMarketingCenterDispatchListener);
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_REFUND,
				"listener:thirdparty.listeners.TradeRefundSendSaasErp",
				ListenerDispatchOptions.async("default", null),
				tradeRefundSendSaasErpDispatchListener);
	}
}
