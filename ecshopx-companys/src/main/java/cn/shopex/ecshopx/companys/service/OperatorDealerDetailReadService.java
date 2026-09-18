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

package cn.shopex.ecshopx.companys.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorDealerDetailReadService {

	private final OperatorsQueryService operatorsQueryService;

	public OperatorDealerDetailReadService(OperatorsQueryService operatorsQueryService) {
		this.operatorsQueryService = operatorsQueryService;
	}

	public Map<String, Object> buildDealerSection(long dealerId, long companyId) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("operator_id", dealerId);
		filter.put("company_id", companyId);
		Map<String, Object> operator = operatorsQueryService.getInfo(filter);
		if (operator == null || operator.isEmpty()) {
			return Map.of();
		}
		Map<String, Object> out = new LinkedHashMap<>(operator);
		out.put("role_data", List.of());
		return out;
	}
}
