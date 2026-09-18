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
import cn.shopex.ecshopx.thirdparty.dispatch.OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn("youshuNormalOrderAddDispatchListenerRegistrationConfig")
public class ThirdPartyOrderAddPushMarketingCenterOnNormalOrderAddDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener
			orderAddPushMarketingCenterOnNormalOrderAddDispatchListener;

	public ThirdPartyOrderAddPushMarketingCenterOnNormalOrderAddDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener
					orderAddPushMarketingCenterOnNormalOrderAddDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.orderAddPushMarketingCenterOnNormalOrderAddDispatchListener =
				orderAddPushMarketingCenterOnNormalOrderAddDispatchListener;
	}

	@PostConstruct
	public void registerThirdPartyOrderAddPushMarketingCenterOnNormalOrderAddListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD,
				ListenerDispatchOptions.async("default", null),
				orderAddPushMarketingCenterOnNormalOrderAddDispatchListener);
	}
}
