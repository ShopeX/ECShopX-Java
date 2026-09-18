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

package cn.shopex.ecshopx.thirdparty.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderAddPushMarketingCenterOnNormalOrderAddProcessor;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener implements DispatchListener {

	private final OrderAddPushMarketingCenterOnNormalOrderAddProcessor processor;

	public OrderAddPushMarketingCenterOnNormalOrderAddDispatchListener(
			OrderAddPushMarketingCenterOnNormalOrderAddProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		processor.handle(payload);
	}
}
