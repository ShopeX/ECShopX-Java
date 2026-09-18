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
import cn.shopex.ecshopx.youshu.dispatch.DistributionEditYoushuDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(77)
public class YoushuDistributionEditDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final DistributionEditYoushuDispatchListener distributionEditYoushuDispatchListener;

	public YoushuDistributionEditDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			DistributionEditYoushuDispatchListener distributionEditYoushuDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.distributionEditYoushuDispatchListener = distributionEditYoushuDispatchListener;
	}

	@PostConstruct
	public void registerYoushuDistributionEditListener() {
		dispatchRegistry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT,
				DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION,
				ListenerDispatchOptions.async("default", null),
				distributionEditYoushuDispatchListener);
	}
}
