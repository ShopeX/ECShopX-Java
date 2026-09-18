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

package cn.shopex.ecshopx.onecode.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class OneCodeShopOperatorContextGate {

	private final OneCodeShopRoutePermissionService oneCodeShopRoutePermissionService;
	private final CompanysActivationService companysActivationService;
	private final ObjectMapper objectMapper;

	public OneCodeShopOperatorContextGate(
			OneCodeShopRoutePermissionService oneCodeShopRoutePermissionService,
			CompanysActivationService companysActivationService,
			ObjectMapper objectMapper) {
		this.oneCodeShopRoutePermissionService = oneCodeShopRoutePermissionService;
		this.companysActivationService = companysActivationService;
		this.objectMapper = objectMapper;
	}

	public void assertCreateBatchs(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.CREATE_BATCHS);
	}

	public void assertBatchsList(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.BATCHS_LIST);
	}

	public void assertThingsList(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.THINGS_LIST);
	}

	public void assertWxaOneCodeStream(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.WXA_ONE_CODE_STREAM);
	}

	public void assertBatchsDetail(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.BATCHS_DETAIL);
	}

	public void assertThingsDetail(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.THINGS_DETAIL);
	}

	public void assertUpdateBatchs(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.UPDATE_BATCHS);
	}

	public void assertDeleteBatchs(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.DELETE_BATCHS);
	}

	public void assertDeleteThings(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.DELETE_THINGS);
	}

	public void assertCreateThings(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.CREATE_THINGS);
	}

	public void assertUpdateThings(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, Object> user = readUser(request);
		assertShopOperatorContext(user, merged);
		assertRoute(user, OneCodeAction.UPDATE_THINGS);
	}

	private Map<String, Object> readUser(HttpServletRequest request) {
		Object userRaw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(userRaw instanceof Map<?, ?> map)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		return user;
	}

	private void assertRoute(Map<String, Object> user, OneCodeAction action) {
		String source = stringOrEmpty(user.get("source"));
		if ("salesperson_workwechat".equals(source)) {
			return;
		}
		switch (action) {
			case CREATE_BATCHS -> oneCodeShopRoutePermissionService.assertBatchsCreate(user);
			case BATCHS_LIST -> oneCodeShopRoutePermissionService.assertBatchsLists(user);
			case THINGS_LIST -> oneCodeShopRoutePermissionService.assertThingsLists(user);
			case WXA_ONE_CODE_STREAM -> oneCodeShopRoutePermissionService.assertBatchsWxaCode(user);
			case BATCHS_DETAIL -> oneCodeShopRoutePermissionService.assertBatchsDetail(user);
			case THINGS_DETAIL -> oneCodeShopRoutePermissionService.assertThingsDetail(user);
			case UPDATE_BATCHS -> oneCodeShopRoutePermissionService.assertBatchsUpdate(user);
			case DELETE_BATCHS -> oneCodeShopRoutePermissionService.assertBatchsDelete(user);
			case DELETE_THINGS -> oneCodeShopRoutePermissionService.assertThingsDelete(user);
			case CREATE_THINGS -> oneCodeShopRoutePermissionService.assertThingsCreate(user);
			case UPDATE_THINGS -> oneCodeShopRoutePermissionService.assertThingsUpdate(user);
		}
	}

	private void assertShopOperatorContext(Map<String, Object> user, Map<String, Object> merged) {
		String source = stringOrEmpty(user.get("source"));
		long companyId = toLong(user.get("company_id"));
		if (companyId <= 0L) {
			throw new ForbiddenException("未激活");
		}
		if (!"salesperson".equals(source)) {
			companysActivationService.assertShopOperatorCompanyActive(companyId);
		}

		String operatorType = stringOrEmpty(user.get("operator_type"));
		if (!StringUtils.hasText(operatorType)) {
			throw new BadRequestException("operator_type 不能为空");
		}
		List<Map<String, Object>> distributors = parseDistributors(user.get("distributor_ids"));
		if ("distributor".equals(operatorType) && distributors.isEmpty()) {
			throw new ForbiddenException("权限信息有误");
		}

		if ("distributor".equals(operatorType)) {
			long selected = toLong(user.get("distributor_id"));
			if (selected > 0L) {
				merged.put("distributor_id", (int) selected);
				Set<Long> allowed = extractDistributorIds(distributors);
				if (!allowed.contains(selected)) {
					throw new ForbiddenException("您没有权限管理此店铺");
				}
			}
			return;
		}

		if ("staff".equals(operatorType) && !distributors.isEmpty()) {
			assertDistributorSelectionAllowed(merged.get("distributor_id"), new ArrayList<>(extractDistributorIds(distributors)));
		}
	}

	private Set<Long> extractDistributorIds(List<Map<String, Object>> distributors) {
		Set<Long> ids = new HashSet<>();
		for (Map<String, Object> d : distributors) {
			Object did = d.get("distributor_id");
			if (did != null) {
				ids.add(toLong(did));
			}
		}
		return ids;
	}

	private void assertDistributorSelectionAllowed(Object distributorIdRaw, List<Long> allowedIds) {
		if (distributorIdRaw == null || allowedIds.isEmpty()) {
			return;
		}
		if (distributorIdRaw instanceof List<?> listRaw) {
			Set<Long> allow = new HashSet<>(allowedIds);
			for (Object o : listRaw) {
				if (o == null) {
					continue;
				}
				if (!allow.contains(toLong(o))) {
					throw new ForbiddenException("您没有权限管理此店铺");
				}
			}
			return;
		}
		long v = toLong(distributorIdRaw);
		if (v > 0L && !allowedIds.contains(v)) {
			throw new ForbiddenException("您没有权限管理此店铺");
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
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				List<Map<String, Object>> parsed = objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
				return parsed != null ? parsed : List.of();
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
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOrEmpty(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private enum OneCodeAction {
		CREATE_BATCHS,
		BATCHS_LIST,
		THINGS_LIST,
		WXA_ONE_CODE_STREAM,
		BATCHS_DETAIL,
		THINGS_DETAIL,
		UPDATE_BATCHS,
		DELETE_BATCHS,
		DELETE_THINGS,
		CREATE_THINGS,
		UPDATE_THINGS
	}
}
