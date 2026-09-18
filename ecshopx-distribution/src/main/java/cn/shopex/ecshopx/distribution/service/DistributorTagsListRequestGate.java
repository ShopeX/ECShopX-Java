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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorTagsListRequestGate {

	private final CompanysActivationService companysActivationService;
	private final DistributorMenuPermissionService distributorMenuPermissionService;

	public DistributorTagsListRequestGate(
			CompanysActivationService companysActivationService,
			DistributorMenuPermissionService distributorMenuPermissionService) {
		this.companysActivationService = companysActivationService;
		this.distributorMenuPermissionService = distributorMenuPermissionService;
	}

	public Map<String, Object> validateBeforeList(HttpServletRequest request, Map<String, Object> merged) {
		Object userRaw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(userRaw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : rawMap.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		long companyId = toLong(user.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_LIST);

		validatePaginationFromRequest(request, merged);
		return user;
	}

	private static void validatePaginationFromRequest(HttpServletRequest request, Map<String, Object> merged) {
		Map<String, String[]> pm = request.getParameterMap();
		if (!pm.containsKey("page")) {
			throw new ResourceException("page 必填");
		}
		if (!pm.containsKey("pageSize")) {
			throw new ResourceException("pageSize 必填");
		}

		String pageTrim = trimOrEmpty(request.getParameter("page"));
		String pageSizeTrim = trimOrEmpty(request.getParameter("pageSize"));

		int page = isFalsyForPagination(pageTrim) ? 1 : parseIntOrDefault(pageTrim, 1);
		int pageSize = isFalsyForPagination(pageSizeTrim) ? 20 : parseIntOrDefault(pageSizeTrim, 20);

		long offsetBase = (page - 1L) * (long) pageSize;
		if (offsetBase < 0) {
			throw new ResourceException("TAG_LIST_OFFSET:" + offsetBase);
		}

		merged.put("page", page);
		merged.put("pageSize", pageSize);
	}

	private static String trimOrEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static boolean isFalsyForPagination(String trimmed) {
		return trimmed.isEmpty() || "0".equals(trimmed);
	}

	private static int parseIntOrDefault(String s, int defaultVal) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
