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

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.orders.hfpay.dispatch.PopularizeWithdrawDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(80)
public class HfpayPopularizeWithdrawDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final PopularizeWithdrawDispatchListener popularizeWithdrawDispatchListener;

	public HfpayPopularizeWithdrawDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry, PopularizeWithdrawDispatchListener popularizeWithdrawDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.popularizeWithdrawDispatchListener = popularizeWithdrawDispatchListener;
	}

	@PostConstruct
	public void registerHfpayPopularizeWithdrawEventListeners() {
		dispatchRegistry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_POPULARIZE_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				popularizeWithdrawDispatchListener);
	}
}
