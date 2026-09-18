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
import cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeAftersalesSendOmeDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemLinkTradeAftersalesDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final SystemLinkTradeAftersalesSendOmeDispatchListener systemLinkTradeAftersalesSendOmeDispatchListener;
	private final ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener
			thirdPartyTradeAftersalesPushMarketingCenterDispatchListener;

	public SystemLinkTradeAftersalesDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			SystemLinkTradeAftersalesSendOmeDispatchListener systemLinkTradeAftersalesSendOmeDispatchListener,
			ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener
					thirdPartyTradeAftersalesPushMarketingCenterDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.systemLinkTradeAftersalesSendOmeDispatchListener = systemLinkTradeAftersalesSendOmeDispatchListener;
		this.thirdPartyTradeAftersalesPushMarketingCenterDispatchListener =
				thirdPartyTradeAftersalesPushMarketingCenterDispatchListener;
	}

	@PostConstruct
	public void registerTradeAftersalesListeners() {
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				systemLinkTradeAftersalesSendOmeDispatchListener);
		dispatchRegistry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				"listener:thirdparty.trade_aftersales_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				thirdPartyTradeAftersalesPushMarketingCenterDispatchListener);
	}
}
