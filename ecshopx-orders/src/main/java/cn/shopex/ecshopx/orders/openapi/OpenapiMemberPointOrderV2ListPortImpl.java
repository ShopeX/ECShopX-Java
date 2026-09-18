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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointOrderV2ListPort;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.OpenapiThirdApiV2MemberPointOrderListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberPointOrderV2ListPortImpl implements OpenapiMemberPointOrderV2ListPort {

	private final OpenapiThirdApiV2MemberPointOrderListService listService;

	public OpenapiMemberPointOrderV2ListPortImpl(
			OpenapiThirdApiV2MemberPointOrderListService listService) {
		this.listService = listService;
	}

	@Override
	public Map<String, Object> listMemberPointOrders(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean orderIdPresent,
			String orderIdRaw,
			int page,
			int pageSize) {
		return listService.executeOpenapiList(
				companyId,
				mobilePresent,
				mobileRaw,
				orderIdPresent,
				orderIdRaw,
				page,
				pageSize);
	}
}
