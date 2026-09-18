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
import cn.shopex.ecshopx.thirdparty.dispatch.DistributionEditPushMarketingCenterDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(76)
public class ThirdPartyDistributionEditPushMarketingCenterDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final DistributionEditPushMarketingCenterDispatchListener distributionEditPushMarketingCenterDispatchListener;

	public ThirdPartyDistributionEditPushMarketingCenterDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			DistributionEditPushMarketingCenterDispatchListener distributionEditPushMarketingCenterDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.distributionEditPushMarketingCenterDispatchListener = distributionEditPushMarketingCenterDispatchListener;
	}

	@PostConstruct
	public void registerThirdPartyDistributionEditPushMarketingCenterListener() {
		dispatchRegistry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT,
				DistributionDispatchEventNames.LISTENER_THIRDPARTY_DISTRIBUTION_EDIT_PUSH_MARKETING_CENTER,
				ListenerDispatchOptions.async("default", null),
				distributionEditPushMarketingCenterDispatchListener);
	}
}
