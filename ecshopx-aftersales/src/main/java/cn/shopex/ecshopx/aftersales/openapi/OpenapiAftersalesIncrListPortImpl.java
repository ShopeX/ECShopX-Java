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

import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesIncrListPort;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2AftersalesIncrListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiAftersalesIncrListPortImpl implements OpenapiAftersalesIncrListPort {

	private final OpenapiThirdApiV2AftersalesIncrListService incrListService;

	public OpenapiAftersalesIncrListPortImpl(OpenapiThirdApiV2AftersalesIncrListService incrListService) {
		this.incrListService = incrListService;
	}

	@Override
	public Map<String, Object> incrList(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean startModifiedTruthy,
			String startModifiedRaw,
			boolean endModifiedTruthy,
			String endModifiedRaw,
			boolean aftersalesTypeTruthy,
			String aftersalesTypeRaw,
			boolean aftersalesStatusTruthy,
			String aftersalesStatusRaw,
			boolean progressTruthy,
			String progressRaw) {
		return incrListService.incrList(
				companyId,
				page,
				pageSize,
				mobileTruthy,
				mobileRaw,
				startModifiedTruthy,
				startModifiedRaw,
				endModifiedTruthy,
				endModifiedRaw,
				aftersalesTypeTruthy,
				aftersalesTypeRaw,
				aftersalesStatusTruthy,
				aftersalesStatusRaw,
				progressTruthy,
				progressRaw);
	}
}
