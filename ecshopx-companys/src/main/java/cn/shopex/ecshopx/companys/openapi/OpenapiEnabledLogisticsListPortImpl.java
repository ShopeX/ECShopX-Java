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

package cn.shopex.ecshopx.companys.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiEnabledLogisticsListPort;
import cn.shopex.ecshopx.companys.service.companylogistics.CompanyLogisticsListService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OpenapiEnabledLogisticsListPortImpl implements OpenapiEnabledLogisticsListPort {

	private final CompanyLogisticsListService companyLogisticsListService;

	public OpenapiEnabledLogisticsListPortImpl(CompanyLogisticsListService companyLogisticsListService) {
		this.companyLogisticsListService = companyLogisticsListService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> listEnabled(long companyId, int distributorId, int supplierId) {
		Map<String, Object> serviceResult =
				companyLogisticsListService.getCompanyLogisticsList(
						companyId, distributorId, supplierId, null, "1");
		List<Map<String, Object>> mergedList =
				(List<Map<String, Object>>) serviceResult.getOrDefault("list", List.of());

		List<Map<String, Object>> result = new ArrayList<>(mergedList.size() + 1);
		Map<String, Object> other = new LinkedHashMap<>(2);
		other.put("corp_code", "OTHER");
		other.put("corp_name", "其他");
		result.add(other);

		if (mergedList.isEmpty()) {
			return result;
		}
		for (Map<String, Object> row : mergedList) {
			Map<String, Object> item = new LinkedHashMap<>(2);
			item.put("corp_code", Objects.toString(row.get("corp_code"), ""));
			item.put("corp_name", Objects.toString(row.get("corp_name"), ""));
			result.add(item);
		}
		return result;
	}
}
