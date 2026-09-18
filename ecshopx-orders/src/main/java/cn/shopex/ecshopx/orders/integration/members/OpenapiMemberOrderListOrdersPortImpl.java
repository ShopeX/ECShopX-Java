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

package cn.shopex.ecshopx.orders.integration.members;

import cn.shopex.ecshopx.members.integration.orders.OpenapiMemberOrderListOrdersPort;
import cn.shopex.ecshopx.openapi.thirdapi.v1.orders.OpenapiThirdApiV1MemberOrderListOrdersService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberOrderListOrdersPortImpl implements OpenapiMemberOrderListOrdersPort {

	private final OpenapiThirdApiV1MemberOrderListOrdersService memberOrderListOrdersService;

	public OpenapiMemberOrderListOrdersPortImpl(
			OpenapiThirdApiV1MemberOrderListOrdersService memberOrderListOrdersService) {
		this.memberOrderListOrdersService = memberOrderListOrdersService;
	}

	@Override
	public Map<String, Object> queryOrderItemLists(Map<String, Object> orderFilter, int page, int pageSize) {
		return memberOrderListOrdersService.queryOrderItemLists(orderFilter, page, pageSize);
	}

	@Override
	public Map<Object, Long> sumTotalFeeByUserId(Map<String, Object> orderFilter) {
		return memberOrderListOrdersService.sumTotalFeeByUserId(orderFilter);
	}
}
