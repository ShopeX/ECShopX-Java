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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapayDistributorListService {

	private final DistributorListQueryService distributorListQueryService;
	private final ObjectMapper objectMapper;

	public AdapayDistributorListService(
			DistributorListQueryService distributorListQueryService, ObjectMapper objectMapper) {
		this.distributorListQueryService = distributorListQueryService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributorList(Map<String, Object> jwtMap) {
		Object operatorTypeObj = jwtMap.get("operator_type");
		String operatorType = operatorTypeObj == null ? "" : operatorTypeObj.toString().trim();
		List<Object> list = new ArrayList<>();

		if ("dealer".equals(operatorType)) {
			Object raw = jwtMap.get("distributor_ids");
			if (raw instanceof List<?> rawList) {
				for (Object element : rawList) {
					list.add(element);
				}
			} else if (raw instanceof String s) {
				try {
					JsonNode node = objectMapper.readTree(s);
					if (node.isArray()) {
						List<Object> parsed = objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
						if (parsed != null) {
							for (Object o : parsed) {
								list.add(o);
							}
						}
					}
				} catch (JsonProcessingException | IllegalArgumentException e) {
					// leave list empty
				}
			}
		} else if ("admin".equals(operatorType)) {
			Object companyIdRaw = jwtMap.get("company_id");
			if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
				throw new UnauthorizedException("未登录");
			}
			long companyId = ((Number) companyIdRaw).longValue();
			List<Map<String, Object>> rows =
					distributorListQueryService.listDistributorIdAndNameByCompanyPaged(companyId, 1, 100);
			list.addAll(rows);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("list", list);
		return body;
	}
}
