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

package cn.shopex.ecshopx.deposit.openapi.thirdapi.v2;

import cn.shopex.ecshopx.deposit.domain.RechargeRule;
import cn.shopex.ecshopx.deposit.service.RechargeRuleListService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberRechargeRuleListService {

	private static final int PAGE_SIZE = 20;
	private static final int PAGE = 1;

	private final RechargeRuleListService rechargeRuleListService;

	public OpenapiThirdApiV2MemberRechargeRuleListService(RechargeRuleListService rechargeRuleListService) {
		this.rechargeRuleListService = rechargeRuleListService;
	}

	public Map<String, Object> executeOpenapiGetRechargeRuleList(long companyId) {
		Map<String, Object> raw =
				rechargeRuleListService.getRechargeRuleListPage(String.valueOf(companyId), PAGE_SIZE, PAGE);
		@SuppressWarnings("unchecked")
		List<RechargeRule> records = (List<RechargeRule>) raw.get("list");
		if (records == null || records.isEmpty()) {
			return null;
		}
		List<Map<String, Object>> mappedList = new ArrayList<>(records.size());
		for (RechargeRule entity : records) {
			mappedList.add(
					OpenapiThirdApiV2MemberRechargeRuleCreateService.formatOpenApiResponse(entity));
		}
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("list", mappedList);
		return result;
	}
}
