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

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NormalOrderAddDispatchPublisherImpl implements NormalOrderAddDispatchPublisher {

	private static final DispatchOptions NORMAL_ORDER_ADD_PARENT_PUBLISH_OPTIONS =
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					null,
					null,
					RetryPolicy.platformDefault());

	private final DispatchFacade dispatchFacade;

	public NormalOrderAddDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(Map<String, Object> payload) {
		Objects.requireNonNull(payload, "payload");
		dispatchFacade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				payload,
				NORMAL_ORDER_ADD_PARENT_PUBLISH_OPTIONS);
	}
}
