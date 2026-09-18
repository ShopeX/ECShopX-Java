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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsItemsCreateRequestGate {

	public static final String ROUTE_ALIAS_GOODS_ITEMS_UPDATE = "goods.items.update";

	private final CompanysActivationService companysActivationService;
	private final GoodsSuperadminRoutePermissionService goodsSuperadminRoutePermissionService;
	private final ObjectMapper objectMapper;

	public GoodsItemsCreateRequestGate(
			CompanysActivationService companysActivationService,
			GoodsSuperadminRoutePermissionService goodsSuperadminRoutePermissionService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.goodsSuperadminRoutePermissionService = goodsSuperadminRoutePermissionService;
		this.objectMapper = objectMapper;
	}

	public void validateBeforeCreate(HttpServletRequest request, Map<String, Object> merged) {
		validateShopOperatorContext(request, merged, GoodsSuperadminRoutePermissionService.ROUTE_ALIAS_GOODS_ITEMS_CREATE);
	}

	public void validateBeforeAudit(HttpServletRequest request, Map<String, Object> merged) {
		validateShopOperatorContext(request, merged, GoodsSuperadminRoutePermissionService.ROUTE_ALIAS_GOODS_ITEMS_AUDIT);
	}

	public void validateBeforeDelete(HttpServletRequest request, Map<String, Object> merged) {
		validateShopOperatorContext(request, merged, GoodsSuperadminRoutePermissionService.ROUTE_ALIAS_GOODS_ITEMS_DELETE);
	}

	public void validateBeforeUpdate(HttpServletRequest request, Map<String, Object> merged) {
		validateShopOperatorContext(request, merged, ROUTE_ALIAS_GOODS_ITEMS_UPDATE);
	}

	private void validateShopOperatorContext(HttpServletRequest request, Map<String, Object> merged, String routeAlias) {
		Object userRaw = request.getAttribute(cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
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
			if (GoodsSuperadminRoutePermissionService.ROUTE_ALIAS_GOODS_ITEMS_AUDIT.equals(routeAlias)) {
				goodsSuperadminRoutePermissionService.assertCanAuditItems(user);
			} else {
				goodsSuperadminRoutePermissionService.assertCanCreateItems(user, routeAlias);
			}
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

		if ("distributor".equals(operatorType)) {
			long selected = user.get("distributor_id") != null ? toLong(user.get("distributor_id")) : 0L;
			if (selected > 0L) {
				merged.put("distributor_id", (int) selected);
				Set<Long> allowed = new HashSet<>();
				for (Map<String, Object> d : distributors) {
					Object did = d.get("distributor_id");
					if (did != null) {
						allowed.add(toLong(did));
					}
				}
				if (!allowed.contains(selected)) {
					throw new ForbiddenException("您没有权限管理此店铺");
				}
			}
		} else {
			Object regionAuthObj = user.get("regionauth_id");
			long regionauthId = regionAuthObj != null ? toLong(regionAuthObj) : 0L;
			List<Long> allowedIds = new ArrayList<>();
			if ("staff".equals(operatorType) && !distributors.isEmpty()) {
				if (regionauthId > 0) {
					for (Map<String, Object> d : distributors) {
						Object did = d.get("distributor_id");
						if (did != null) {
							allowedIds.add(toLong(did));
						}
					}
				} else {
					for (Map<String, Object> d : distributors) {
						Object did = d.get("distributor_id");
						if (did != null) {
							allowedIds.add(toLong(did));
						}
					}
				}
			}
			if (!allowedIds.isEmpty()) {
				assertDistributorSelectionAllowed(merged.get("distributor_id"), allowedIds);
			}
		}
	}

	private void assertDistributorSelectionAllowed(Object distributorIdRaw, List<Long> allowedIds) {
		if (distributorIdRaw == null) {
			return;
		}
		if (distributorIdRaw instanceof List<?> listRaw) {
			List<Long> requested = new ArrayList<>();
			for (Object o : listRaw) {
				if (o != null) {
					requested.add(toLong(o));
				}
			}
			if (requested.isEmpty()) {
				return;
			}
			Set<Long> allow = new HashSet<>(allowedIds);
			for (Long r : requested) {
				if (!allow.contains(r)) {
					throw new ForbiddenException("您没有权限管理此店铺");
				}
			}
			return;
		}
		if (distributorIdRaw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				return;
			}
			if (!allowedIds.contains(v)) {
				throw new ForbiddenException("您没有权限管理此店铺");
			}
			return;
		}
		if (distributorIdRaw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (!StringUtils.hasText(t)) {
				return;
			}
			final long v;
			try {
				v = Long.parseLong(t);
			} catch (NumberFormatException e) {
				return;
			}
			if (v <= 0) {
				return;
			}
			if (!allowedIds.contains(v)) {
				throw new ForbiddenException("您没有权限管理此店铺");
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
		return Long.parseLong(o.toString());
	}
}
