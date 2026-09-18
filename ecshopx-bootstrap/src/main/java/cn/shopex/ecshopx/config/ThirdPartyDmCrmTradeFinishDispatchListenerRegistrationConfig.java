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
import cn.shopex.ecshopx.thirdparty.dispatch.ThirdPartyDmCrmTradeFinishDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(82)
public class ThirdPartyDmCrmTradeFinishDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final ThirdPartyDmCrmTradeFinishDispatchListener thirdPartyDmCrmTradeFinishDispatchListener;

	public ThirdPartyDmCrmTradeFinishDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			ThirdPartyDmCrmTradeFinishDispatchListener thirdPartyDmCrmTradeFinishDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.thirdPartyDmCrmTradeFinishDispatchListener = thirdPartyDmCrmTradeFinishDispatchListener;
	}

	@PostConstruct
	public void registerThirdPartyDmCrmTradeFinishListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				"listener:thirdparty.dm_crm.trade_finish",
				ListenerDispatchOptions.asyncDefaults(),
				thirdPartyDmCrmTradeFinishDispatchListener);
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH_CSV292,
				"listener:thirdparty.dm_crm.trade_finish",
				ListenerDispatchOptions.asyncDefaults(),
				thirdPartyDmCrmTradeFinishDispatchListener);
	}
}
