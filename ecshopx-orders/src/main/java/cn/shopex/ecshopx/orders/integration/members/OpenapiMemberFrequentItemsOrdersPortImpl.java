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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberFrequentItemsOrdersPort;
import cn.shopex.ecshopx.orders.openapi.thirdapi.v1.OpenapiThirdApiV1MemberFrequentItemsOrdersService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberFrequentItemsOrdersPortImpl implements OpenapiMemberFrequentItemsOrdersPort {

	private final OpenapiThirdApiV1MemberFrequentItemsOrdersService frequentItemsOrdersService;

	public OpenapiMemberFrequentItemsOrdersPortImpl(
			OpenapiThirdApiV1MemberFrequentItemsOrdersService frequentItemsOrdersService) {
		this.frequentItemsOrdersService = frequentItemsOrdersService;
	}

	@Override
	public List<Map<String, Object>> getFrequentItemAggregates(
			long companyId, Object userId, String timeRange) {
		return frequentItemsOrdersService.getFrequentItemAggregates(companyId, userId, timeRange);
	}
}
