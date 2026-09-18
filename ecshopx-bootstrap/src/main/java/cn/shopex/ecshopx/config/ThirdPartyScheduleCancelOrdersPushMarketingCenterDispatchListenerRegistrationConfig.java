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
import cn.shopex.ecshopx.thirdparty.dispatch.ScheduleCancelOrdersPushMarketingCenterDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

@Configuration
@DependsOn("ordersNormalOrderCancelAutoPassDispatchListenerRegistrationConfig")
@Order(84)
public class ThirdPartyScheduleCancelOrdersPushMarketingCenterDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final ScheduleCancelOrdersPushMarketingCenterDispatchListener
			scheduleCancelOrdersPushMarketingCenterDispatchListener;

	public ThirdPartyScheduleCancelOrdersPushMarketingCenterDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			ScheduleCancelOrdersPushMarketingCenterDispatchListener scheduleCancelOrdersPushMarketingCenterDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.scheduleCancelOrdersPushMarketingCenterDispatchListener = scheduleCancelOrdersPushMarketingCenterDispatchListener;
	}

	@PostConstruct
	public void registerThirdPartyScheduleCancelOrdersPushMarketingCenterListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CANCEL,
				"listener:thirdparty.schedule_cancel_orders_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				scheduleCancelOrdersPushMarketingCenterDispatchListener);
	}
}
