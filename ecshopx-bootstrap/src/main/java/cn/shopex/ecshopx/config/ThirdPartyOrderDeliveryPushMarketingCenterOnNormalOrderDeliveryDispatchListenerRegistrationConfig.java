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
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn("youshuNormalOrderDeliveryDispatchListenerRegistrationConfig")
public class ThirdPartyOrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener
			orderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener;

	public ThirdPartyOrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener
					orderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.orderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener =
				orderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener;
	}

	@PostConstruct
	public void registerThirdPartyOrderDeliveryPushMarketingCenterOnNormalOrderDeliveryListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				orderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener);
	}
}
