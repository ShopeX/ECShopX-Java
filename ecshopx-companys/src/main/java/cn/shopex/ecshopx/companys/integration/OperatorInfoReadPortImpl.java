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

package cn.shopex.ecshopx.companys.integration;

import cn.shopex.ecshopx.common.port.companys.OperatorInfoReadPort;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorInfoReadPortImpl implements OperatorInfoReadPort {

	private final OperatorsQueryService operatorsQueryService;

	public OperatorInfoReadPortImpl(OperatorsQueryService operatorsQueryService) {
		this.operatorsQueryService = operatorsQueryService;
	}

	@Override
	public Map<String, Object> getInfo(long companyId, long operatorId, String operatorType) {
		if (operatorId <= 0L) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_id", operatorId);
		if (operatorType != null && !operatorType.isBlank()) {
			filter.put("operator_type", operatorType);
		}
		Map<String, Object> row = operatorsQueryService.getInfo(filter);
		return row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row);
	}
}
