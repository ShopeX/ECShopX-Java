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

import cn.shopex.ecshopx.common.dispatch.WechatDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.WxShopsAddDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WxShopsAddDispatchPublisherImpl implements WxShopsAddDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public WxShopsAddDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(Map<String, Object> payload) {
		dispatchFacade.publishEvent(
				WechatDispatchEventNames.EVENT_WX_SHOPS_ADD, payload, DispatchOptions.eventDefaults());
	}
}
