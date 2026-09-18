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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorItemsExportRequestGate {

	private final CompanysActivationService companysActivationService;
	private final DistributorMenuPermissionService distributorMenuPermissionService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributorItemsExportRequestGate(
			CompanysActivationService companysActivationService,
			DistributorMenuPermissionService distributorMenuPermissionService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.distributorMenuPermissionService = distributorMenuPermissionService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void assertCanExport(HttpServletRequest request, Map<String, Object> merged) {
		Object userRaw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(userRaw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ((Map<?, ?>) userRaw).entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object opTypeObj = user.get("operator_type");
		String operatorType = opTypeObj != null ? opTypeObj.toString().trim() : "";
		if (!StringUtils.hasText(operatorType)) {
			throw new BadRequestException("operator_type 不能为空");
		}

		Object sourceObj = user.get("source");
		String source = sourceObj != null ? sourceObj.toString() : "";
		if (!"salesperson_workwechat".equals(source)) {
			distributorMenuPermissionService.assertRouteAllowed(
					user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_ITEM_EXPORTLIST);
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		if (!"salesperson".equals(source)) {
			companysActivationService.assertShopOperatorCompanyActive(companyId);
		}

		List<Map<String, Object>> distributors = parseDistributors(user.get("distributor_ids"));
		if ("distributor".equals(operatorType) && distributors.isEmpty()) {
			throw new ForbiddenException("权限信息有误");
		}

		Object coalesced = coalesceDistributorId(merged.get("distributor_id"));
		if (isEmptyAfterCoalesce(coalesced)) {
			throw new ResourceException("请选择店铺");
		}
		merged.put("distributor_id", coalesced);
		Optional<Long> targetNumeric = tryParseDistributorIdLong(coalesced);

		Object opIdObj = user.get("operator_id");
		long operatorId = opIdObj != null ? toLong(opIdObj) : 0L;

		if ("distributor".equals(operatorType) && targetNumeric.isPresent()) {
			long targetDistributorId = targetNumeric.get();
			Set<Long> allowed = new HashSet<>();
			for (Map<String, Object> d : distributors) {
				Object did = d.get("distributor_id");
				if (did != null) {
					allowed.add(toLong(did));
				}
			}
			long selected = user.get("distributor_id") != null ? toLong(user.get("distributor_id")) : 0L;
			if (selected > 0L && targetDistributorId != selected) {
				throw new ForbiddenException("您没有权限管理此店铺");
			}
			if (!allowed.isEmpty() && !allowed.contains(targetDistributorId)) {
				throw new ForbiddenException("您没有权限管理此店铺");
			}
		} else if ("staff".equals(operatorType) && !distributors.isEmpty() && targetNumeric.isPresent()) {
			long targetDistributorId = targetNumeric.get();
			List<Long> allowedIds = new ArrayList<>();
			for (Map<String, Object> d : distributors) {
				Object did = d.get("distributor_id");
				if (did != null) {
					allowedIds.add(toLong(did));
				}
			}
			if (!allowedIds.isEmpty()) {
				Set<Long> allow = new HashSet<>(allowedIds);
				if (!allow.contains(targetDistributorId)) {
					throw new ForbiddenException("您没有权限管理此店铺");
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

	/** Returns the distributor_id as a Long or trimmed String, defaulting to {@code 0L} for null/blank/"0" values. */
	private static Object coalesceDistributorId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(raw).trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return 0L;
		}
		return t;
	}

	/** True when the coalesced distributor id is unset, numeric zero, or blank/zero string. */
	private static boolean isEmptyAfterCoalesce(Object v) {
		if (v instanceof Long l) {
			return l == 0L;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return !StringUtils.hasText(t) || "0".equals(t);
		}
		return false;
	}

	private static Optional<Long> tryParseDistributorIdLong(Object coalesced) {
		if (coalesced instanceof Number n) {
			return Optional.of(n.longValue());
		}
		if (coalesced instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return Optional.empty();
			}
			try {
				return Optional.of(Long.parseLong(t));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
