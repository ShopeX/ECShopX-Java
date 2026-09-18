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
import cn.shopex.ecshopx.shuyun.dispatch.TradeRefundFinishShuyunRefundSyncDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShuyunOpenPlatformTradeRefundFinishDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final TradeRefundFinishShuyunRefundSyncDispatchListener listener;

	public ShuyunOpenPlatformTradeRefundFinishDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry, TradeRefundFinishShuyunRefundSyncDispatchListener listener) {
		this.dispatchRegistry = dispatchRegistry;
		this.listener = listener;
	}

	@PostConstruct
	public void register() {
		dispatchRegistry.registerEventListener(
				ThirdPartyDispatchEventNames.EVENT_TRADE_REFUND_FINISH,
				ThirdPartyDispatchEventNames.LISTENER_SHUYUN_OPEN_PLATFORM_REFUND_SYNC_ON_TRADE_REFUND_FINISH,
				ListenerDispatchOptions.syncDefaults(),
				listener);
	}
}
