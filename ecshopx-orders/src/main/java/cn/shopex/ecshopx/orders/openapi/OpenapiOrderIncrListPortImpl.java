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

import cn.shopex.ecshopx.common.openapi.OpenapiOrderIncrListPort;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2OrderIncrListService;
import org.springframework.stereotype.Component;

@Component
public class OpenapiOrderIncrListPortImpl implements OpenapiOrderIncrListPort {

	private final OpenapiThirdApiV2OrderIncrListService incrListService;

	public OpenapiOrderIncrListPortImpl(OpenapiThirdApiV2OrderIncrListService incrListService) {
		this.incrListService = incrListService;
	}

	@Override
	public Object executeIncrOrderList(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean shopCodeTruthy,
			String shopCodeRaw,
			boolean startModifiedTruthy,
			String startModifiedRaw,
			boolean endModifiedTruthy,
			String endModifiedRaw,
			boolean isSelfPresent,
			String isSelfRaw,
			boolean orderStatusPresent,
			String orderStatusRaw,
			boolean payStatusPresent,
			String payStatusRaw) {
		return incrListService.execute(
				companyId,
				page,
				pageSize,
				mobileTruthy,
				mobileRaw,
				shopCodeTruthy,
				shopCodeRaw,
				startModifiedTruthy,
				startModifiedRaw,
				endModifiedTruthy,
				endModifiedRaw,
				isSelfPresent,
				isSelfRaw,
				orderStatusPresent,
				orderStatusRaw,
				payStatusPresent,
				payStatusRaw);
	}
}
