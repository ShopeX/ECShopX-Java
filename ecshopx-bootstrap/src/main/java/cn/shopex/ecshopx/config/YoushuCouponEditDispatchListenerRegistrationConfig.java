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

import cn.shopex.ecshopx.common.dispatch.KaquanDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.youshu.dispatch.YoushuCouponEditDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(78)
public class YoushuCouponEditDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final YoushuCouponEditDispatchListener youshuCouponEditDispatchListener;

	public YoushuCouponEditDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry, YoushuCouponEditDispatchListener youshuCouponEditDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.youshuCouponEditDispatchListener = youshuCouponEditDispatchListener;
	}

	@PostConstruct
	public void registerYoushuCouponEditListener() {
		var options = ListenerDispatchOptions.async("default", null);
		dispatchRegistry.registerEventListener(
				KaquanDispatchEventNames.EVENT_COUPON_EDIT,
				KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON,
				options,
				youshuCouponEditDispatchListener);
		dispatchRegistry.registerEventListener(
				KaquanDispatchEventNames.EVENT_COUPON_EDIT_NEW_GIFT,
				KaquanDispatchEventNames.LISTENER_YOUSHU_BUNDLE_COUPON,
				options,
				youshuCouponEditDispatchListener);
	}
}
