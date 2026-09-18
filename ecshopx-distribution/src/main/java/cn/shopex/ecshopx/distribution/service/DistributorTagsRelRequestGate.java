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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorTagsRelRequestGate {

	private final ObjectMapper objectMapper;

	public DistributorTagsRelRequestGate(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void assertCallerMayUseDistributors(
			HttpServletRequest request,
			Map<String, Object> user,
			long companyId,
			List<Long> distributorIdsToTouch) {
		Object opTypeObj = user.get("operator_type");
		String operatorType = opTypeObj != null ? opTypeObj.toString().trim() : "";
		if (!StringUtils.hasText(operatorType)) {
			throw new BadRequestException("operator_type 不能为空");
		}

		List<Map<String, Object>> distributors = parseDistributors(user.get("distributor_ids"));

		if ("distributor".equals(operatorType)) {
			if (distributors.isEmpty()) {
				throw new ForbiddenException("权限信息有误");
			}
			Set<Long> allowed = new HashSet<>();
			for (Map<String, Object> d : distributors) {
				Object did = d.get("distributor_id");
				if (did != null) {
					allowed.add(toLong(did));
				}
			}
			long selected = user.get("distributor_id") != null ? toLong(user.get("distributor_id")) : 0L;
			if (selected > 0L) {
				if (!allowed.contains(selected)) {
					throw new ForbiddenException("您没有权限管理此店铺");
				}
				for (long id : distributorIdsToTouch) {
					if (id != selected && !allowed.contains(id)) {
						throw new ForbiddenException("您没有权限管理此店铺");
					}
				}
			} else {
				for (long id : distributorIdsToTouch) {
					if (!allowed.isEmpty() && !allowed.contains(id)) {
						throw new ForbiddenException("您没有权限管理此店铺");
					}
				}
			}
			return;
		}

		if ("staff".equals(operatorType) && !distributors.isEmpty()) {
			List<Long> allowedIds = new ArrayList<>();
			for (Map<String, Object> d : distributors) {
				Object did = d.get("distributor_id");
				if (did != null) {
					allowedIds.add(toLong(did));
				}
			}
			if (!allowedIds.isEmpty()) {
				for (long id : distributorIdsToTouch) {
					if (!allowedIds.contains(id)) {
						throw new ForbiddenException("您没有权限管理此店铺");
					}
				}
			}
		}
	}

	private List<Map<String, Object>> parseDistributors(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					out.add(row);
				} else if (o instanceof Number n) {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("distributor_id", n.longValue());
					out.add(row);
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				JsonNode arr = objectMapper.readTree(s);
				if (!arr.isArray()) {
					return List.of();
				}
				List<Map<String, Object>> out = new ArrayList<>();
				for (JsonNode n : arr) {
					if (n.isObject()) {
						Map<String, Object> row = new LinkedHashMap<>();
						n.fields()
								.forEachRemaining(e -> row.put(e.getKey(), objectMapper.convertValue(e.getValue(), Object.class)));
						out.add(row);
					} else if (n.isNumber()) {
						Map<String, Object> row = new LinkedHashMap<>();
						row.put("distributor_id", n.longValue());
						out.add(row);
					}
				}
				return out;
			} catch (Exception e) {
				return List.of();
			}
		}
		return List.of();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		return Long.parseLong(o.toString());
	}
}
