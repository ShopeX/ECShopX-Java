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
import cn.shopex.ecshopx.orders.dispatch.UpdateGroupsActivityOrderDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(87)
public class UpdateGroupsActivityOrderDispatchListenerRegistrationConfig {

	private static final String LISTENER_NAME = "listener:orders.listeners.UpdateGroupsActivityOrder";

	private final DispatchRegistry dispatchRegistry;
	private final UpdateGroupsActivityOrderDispatchListener updateGroupsActivityOrderDispatchListener;

	public UpdateGroupsActivityOrderDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			UpdateGroupsActivityOrderDispatchListener updateGroupsActivityOrderDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.updateGroupsActivityOrderDispatchListener = updateGroupsActivityOrderDispatchListener;
	}

	@PostConstruct
	public void registerUpdateGroupsActivityOrderListener() {
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				updateGroupsActivityOrderDispatchListener);
		dispatchRegistry.registerEventListener(
				OrdersDispatchEventNames.EVENT_TRADE_FINISH_CSV292,
				LISTENER_NAME,
				ListenerDispatchOptions.syncDefaults(),
				updateGroupsActivityOrderDispatchListener);
	}
}
