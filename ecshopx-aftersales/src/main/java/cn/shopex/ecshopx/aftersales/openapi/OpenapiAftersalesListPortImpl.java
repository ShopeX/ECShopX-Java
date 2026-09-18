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

package cn.shopex.ecshopx.aftersales.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesListPort;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2AftersalesListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiAftersalesListPortImpl implements OpenapiAftersalesListPort {

	private final OpenapiThirdApiV2AftersalesListService aftersalesListService;

	public OpenapiAftersalesListPortImpl(OpenapiThirdApiV2AftersalesListService aftersalesListService) {
		this.aftersalesListService = aftersalesListService;
	}

	@Override
	public Map<String, Object> list(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean timeBeginTruthy,
			String timeBeginRaw,
			boolean timeEndTruthy,
			String timeEndRaw) {
		return aftersalesListService.list(
				companyId,
				page,
				pageSize,
				mobileTruthy,
				mobileRaw,
				timeBeginTruthy,
				timeBeginRaw,
				timeEndTruthy,
				timeEndRaw);
	}
}
