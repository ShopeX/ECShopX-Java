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

import cn.shopex.ecshopx.common.openapi.OpenapiOrderSoldListPort;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2OrderSoldListService;
import org.springframework.stereotype.Component;

@Component
public class OpenapiOrderSoldListPortImpl implements OpenapiOrderSoldListPort {

	private final OpenapiThirdApiV2OrderSoldListService soldListService;

	public OpenapiOrderSoldListPortImpl(OpenapiThirdApiV2OrderSoldListService soldListService) {
		this.soldListService = soldListService;
	}

	@Override
	public Object executeSoldOrderList(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean timeBeginTruthy,
			String timeBeginRaw,
			boolean timeEndTruthy,
			String timeEndRaw,
			boolean shopCodeTruthy,
			String shopCodeRaw,
			boolean isSelfPresent,
			String isSelfRaw,
			boolean orderStatusPresent,
			String orderStatusRaw,
			boolean payStatusPresent,
			String payStatusRaw) {
		return soldListService.execute(
				companyId,
				page,
				pageSize,
				mobileTruthy,
				mobileRaw,
				timeBeginTruthy,
				timeBeginRaw,
				timeEndTruthy,
				timeEndRaw,
				shopCodeTruthy,
				shopCodeRaw,
				isSelfPresent,
				isSelfRaw,
				orderStatusPresent,
				orderStatusRaw,
				payStatusPresent,
				payStatusRaw);
	}
}
