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

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.hfpay.dispatch.DistributionEditHfEnterapplyInitDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(77)
public class HfpayDistributionEditEnterapplyDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final DistributionEditHfEnterapplyInitDispatchListener distributionEditHfEnterapplyInitDispatchListener;

	public HfpayDistributionEditEnterapplyDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			DistributionEditHfEnterapplyInitDispatchListener distributionEditHfEnterapplyInitDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.distributionEditHfEnterapplyInitDispatchListener = distributionEditHfEnterapplyInitDispatchListener;
	}

	@PostConstruct
	public void registerHfpayDistributionEditEnterapplyListener() {
		dispatchRegistry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT_CSV290,
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT_EDIT,
				ListenerDispatchOptions.async("default", null),
				distributionEditHfEnterapplyInitDispatchListener);
	}
}
