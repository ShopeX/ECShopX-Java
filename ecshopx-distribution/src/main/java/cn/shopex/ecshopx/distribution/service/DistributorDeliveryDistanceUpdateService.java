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
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorDeliveryDistanceUpdateService {

	private final DistributorWriteRepository distributorWriteRepository;
	private final ObjectMapper objectMapper;

	public DistributorDeliveryDistanceUpdateService(
			DistributorWriteRepository distributorWriteRepository, ObjectMapper objectMapper) {
		this.distributorWriteRepository = distributorWriteRepository;
		this.objectMapper = objectMapper;
	}

	public void setDistance(Map<String, Object> jwtUser, Map<String, Object> merged, int deliveryDistanceForDb) {
		Object opTypeObj = jwtUser.get("operator_type");
		String operatorType = opTypeObj == null ? "" : opTypeObj.toString().trim();

		long companyId = toLong(jwtUser.get("company_id"));
		if (companyId <= 0) {
			throw new BadRequestException("company_id 无效");
		}

		Long merchantIdOrNull = null;
		if ("merchant".equals(operatorType)) {
			Object mid = jwtUser.get("merchant_id");
			if (mid == null) {
				throw new BadRequestException("merchant_id 无效");
			}
			long m = toLong(mid);
			if (m <= 0) {
				throw new BadRequestException("merchant_id 无效");
			}
			merchantIdOrNull = m;
		}

		Object chosenRaw = null;
		List<Long> requestScope = null;
		if (ValuePresence.hasEffectiveValue(merged.get("distributor_id"))) {
			chosenRaw = merged.get("distributor_id");
			requestScope = normalizeToLongList(chosenRaw);
			if (requestScope.isEmpty()) {
				chosenRaw = null;
				requestScope = null;
			}
		} else if (ValuePresence.hasEffectiveValue(merged.get("distributorIds"))) {
			chosenRaw = merged.get("distributorIds");
			requestScope = normalizeToLongList(chosenRaw);
			if (requestScope.isEmpty()) {
				chosenRaw = null;
				requestScope = null;
			}
		}

		List<Long> staffAllowed = extractStaffDistributorIds(jwtUser);
		if ("staff".equals(operatorType) && !staffAllowed.isEmpty()) {
			boolean distributorFilterWasArray =
					chosenRaw != null && chosenRaw instanceof Collection && !(chosenRaw instanceof Map);
			if (requestScope != null && !requestScope.isEmpty() && distributorFilterWasArray) {
				requestScope = new ArrayList<>(requestScope);
				requestScope.retainAll(staffAllowed);
				if (requestScope.isEmpty()) {
					throw new BadRequestException("无可操作的店铺");
				}
			} else {
				requestScope = new ArrayList<>(staffAllowed);
			}
		} else if ("staff".equals(operatorType) && staffAllowed.isEmpty()) {
			if (requestScope != null && requestScope.isEmpty()) {
				throw new BadRequestException("无可操作的店铺");
			}
		}

		distributorWriteRepository.updateDeliveryDistanceForScope(companyId, merchantIdOrNull, requestScope, deliveryDistanceForDb);
	}

	private List<Long> extractStaffDistributorIds(Map<String, Object> jwtUser) {
		List<Map<String, Object>> distributors = parseDistributors(jwtUser.get("distributor_ids"));
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> d : distributors) {
			Object did = d.get("distributor_id");
			if (did != null) {
				out.add(toLong(did));
			}
		}
		return out;
	}

	private List<Long> normalizeToLongList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Map) {
			return List.of();
		}
		if (raw instanceof Collection<?> coll) {
			List<Long> out = new ArrayList<>();
			for (Object o : coll) {
				if (o == null) {
					continue;
				}
				if (o instanceof Map<?, ?> m) {
					Object did = m.get("distributor_id");
					if (did != null) {
						out.add(toLong(did));
					}
				} else {
					out.add(toLong(o));
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			return List.of(toLong(n));
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			String t = s.trim();
			if (t.startsWith("[")) {
				try {
					JsonNode arr = objectMapper.readTree(t);
					if (!arr.isArray()) {
						return List.of();
					}
					List<Long> out = new ArrayList<>();
					for (JsonNode n : arr) {
						if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual() && StringUtils.hasText(n.asText())) {
							out.add(Long.parseLong(n.asText().trim()));
						}
					}
					return out;
				} catch (Exception e) {
					return List.of();
				}
			}
			return List.of(toLong(t));
		}
		return List.of();
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
						n.fields().forEachRemaining(e -> row.put(e.getKey(), objectMapper.convertValue(e.getValue(), Object.class)));
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
		return Long.parseLong(o.toString());
	}
}
