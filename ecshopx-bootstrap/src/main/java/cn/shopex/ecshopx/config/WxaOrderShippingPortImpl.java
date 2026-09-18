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

import cn.shopex.ecshopx.common.port.wechat.WxaOrderShippingPort;
import cn.shopex.ecshopx.wechat.service.wxa.WxaOrderShippingApiClient;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WxaOrderShippingPortImpl implements WxaOrderShippingPort {

	private final WxaOrderShippingApiClient client;

	public WxaOrderShippingPortImpl(WxaOrderShippingApiClient client) {
		this.client = client;
	}

	@Override
	public Map<String, Object> getOrder(String wxaAppId, String transactionId) {
		return client.getOrder(wxaAppId, transactionId);
	}

	@Override
	public Map<String, Object> uploadShippingInfo(String wxaAppId, Map<String, Object> params) {
		return client.uploadShippingInfo(wxaAppId, params);
	}
}
