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

package cn.shopex.ecshopx.orders.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiOrderListV2Port;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2OrderListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiOrderListV2PortImpl implements OpenapiOrderListV2Port {

	private final OpenapiThirdApiV2OrderListService orderListService;

	public OpenapiOrderListV2PortImpl(OpenapiThirdApiV2OrderListService orderListService) {
		this.orderListService = orderListService;
	}

	@Override
	public Map<String, Object> list(
			long companyId,
			String mobileQueryParam,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean mobilePresent,
			String mobileRaw,
			boolean mobileTruthy,
			String unionidRaw,
			int page,
			Integer pageSize,
			boolean pageOverridden) {
		return orderListService.executeOpenapiOrderList(
				companyId,
				mobileQueryParam,
				unionidQueryParam,
				body,
				mobilePresent,
				mobileRaw,
				mobileTruthy,
				unionidRaw,
				page,
				pageSize,
				pageOverridden);
	}
}
