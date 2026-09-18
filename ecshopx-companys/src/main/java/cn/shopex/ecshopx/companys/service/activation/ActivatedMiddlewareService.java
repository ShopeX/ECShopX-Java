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

package cn.shopex.ecshopx.companys.service.activation;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

@Service
public class ActivatedMiddlewareService {

	private static final String OPERATOR_SELECT_DISTRIBUTOR = "operator.select.distributor";

	private static final Set<String> PATH_WHITELIST =
			Set.of("companys.setting", "account.roles.permission", "operator.get.data", "currency.default");
	private static final Set<String> DELIVERY_STAFF_PATHS = Set.of(
			"order.deliverypackag.confirm",
			"order.deliverystaff.confirm",
			"order.deliverystaff.cancel",
			"datacube.deliverystaff.data",
			"datacube.deliverystaff.data.export");

	private final CompanysActivationService companysActivationService;
	private final OperatorRoleMenuAliasService operatorRoleMenuAliasService;
	private final ShopMenuService shopMenuService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${common.check-superadmin-permission:false}")
	private boolean checkSuperadminPermission;

	public ActivatedMiddlewareService(
			CompanysActivationService companysActivationService,
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			ShopMenuService shopMenuService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void handle(HttpServletRequest request, HandlerMethod handlerMethod, Map<String, Object> user) {
		if (user == null) {
			return;
		}
		String routeAlias = resolveRouteAlias(handlerMethod);
		Object source = user.get("source");
		String sourceStr = source != null ? source.toString() : "";
		if (!"salesperson_workwechat".equals(sourceStr) && StringUtils.hasText(routeAlias)) {
			checkPermission(user, routeAlias);
		}

		Object companyIdObj = user.get("company_id");
		if (companyIdObj == null) {
			throw new ResourceException("未激活", 500, 400002);
		}
		long companyId = longOf(companyIdObj);
		if (companyId <= 0) {
			throw new ResourceException("未激活", 500, 400002);
		}

		if (!"salesperson".equals(sourceStr)) {
			try {
				companysActivationService.assertShopOperatorCompanyActive(companyId);
			} catch (RuntimeException ex) {
				throw toActivatedResourceException(ex, "未激活或者被禁止登入", 400002);
			}
		}

		String operatorType = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		List<Map<String, Object>> distributors = parseDistributors(user.get("distributor_ids"));
		if ("distributor".equals(operatorType) && distributors.isEmpty()) {
			throw new ResourceException("权限信息有误", 500, 400002);
		}

		applyDistributorContext(request, user, companyId, operatorType, distributors);
	}

	private static String resolveRouteAlias(HandlerMethod handlerMethod) {
		Activated methodAnn = handlerMethod.getMethodAnnotation(Activated.class);
		if (methodAnn != null && StringUtils.hasText(methodAnn.routeAlias())) {
			return methodAnn.routeAlias();
		}
		Activated classAnn = handlerMethod.getBeanType().getAnnotation(Activated.class);
		if (classAnn != null && StringUtils.hasText(classAnn.routeAlias())) {
			return classAnn.routeAlias();
		}
		return "";
	}

	private void checkPermission(Map<String, Object> user, String routeAlias) {
		if (!checkSuperadminPermission) {
			String operatorType = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
			if (operatorType.isEmpty() || "admin".equals(operatorType)) {
				return;
			}
			if ("distributor".equals(operatorType)
					&& (OPERATOR_SELECT_DISTRIBUTOR.equals(routeAlias) || "distributor.list".equals(routeAlias))) {
				return;
			}
		}
		if (PATH_WHITELIST.contains(routeAlias)) {
			return;
		}
		String operatorType = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		if ("self_delivery_staff".equals(operatorType) && DELIVERY_STAFF_PATHS.contains(routeAlias)) {
			return;
		}
		long companyId = longOf(user.get("company_id"));
		Object operatorIdRaw = user.get("operator_id");
		List<String> aliases;
		if (operatorIdRaw == null) {
			aliases = null;
		} else {
			aliases = operatorRoleMenuAliasService.listShopMenuAliases(companyId, longOf(operatorIdRaw));
		}
		if (aliases == null && "staff".equals(operatorType)) {
			throw new ResourceException("帐号没有绑定角色，请联系管理员添加", 500, 42014);
		}
		if (aliases == null) {
			aliases = List.of();
		}
		int version = menuVersionForOperatorType(operatorType);
		List<String> apis = shopMenuService.collectApisFromMenus(version, aliases);
		if (apis.contains(routeAlias)) {
			return;
		}
		throw new ResourceException("您没有操作权限【" + routeAlias + "】", 500, 400500);
	}

	private void applyDistributorContext(
			HttpServletRequest request,
			Map<String, Object> user,
			long companyId,
			String operatorType,
			List<Map<String, Object>> distributors) {
		Map<String, Object> userAuthData = new LinkedHashMap<>();
		if ("distributor".equals(operatorType)) {
			Object distributorIdRaw = user.get("distributor_id");
			if (distributorIdRaw != null && longOf(distributorIdRaw) > 0L) {
				userAuthData.put("distributor_id", longOf(distributorIdRaw));
			}
			List<Long> distributorIds = extractDistributorIds(distributors);
			if (userAuthData.containsKey("distributor_id")) {
				long selected = longOf(userAuthData.get("distributor_id"));
				if (!distributorIds.isEmpty() && !distributorIds.contains(selected)) {
					throw new ResourceException("您没有权限管理此店铺", 500, 400002);
				}
			}
			if (!distributorIds.isEmpty()) {
				userAuthData.put("distributorIds", distributorIds);
			}
		} else {
			Object distributorIdParam = firstDistributorIdFromRequest(request);
			if ("staff".equals(operatorType) && !distributors.isEmpty()) {
				List<Long> distributorIds = resolveStaffDistributorIds(user, distributors);
				assertStaffDistributorAccess(distributorIdParam, distributorIds);
				if (!distributorIds.isEmpty()) {
					userAuthData.put("distributorIds", distributorIds);
				}
			}
		}
		if (!userAuthData.isEmpty()) {
			Object distributorId = userAuthData.get("distributor_id");
			if (distributorId != null) {
				request.setAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID, distributorId);
			}
			Object distributorIds = userAuthData.get("distributorIds");
			if (distributorIds != null) {
				request.setAttribute(ActivatedRequestAttributes.DISTRIBUTOR_IDS, distributorIds);
			}
		}
	}

	private List<Long> resolveStaffDistributorIds(Map<String, Object> user, List<Map<String, Object>> distributors) {
		Object regionauthIdObj = user.get("regionauth_id");
		long regionauthId = regionauthIdObj != null ? longOf(regionauthIdObj) : 0L;
		if (regionauthId > 0) {
			List<Long> ids = new ArrayList<>();
			for (Map<String, Object> d : distributors) {
				if (d.size() == 1 && d.containsKey("distributor_id")) {
					Object did = d.get("distributor_id");
					if (did != null) {
						ids.add(longOf(did));
					}
				} else if (!d.isEmpty()) {
					for (Object v : d.values()) {
						if (v instanceof Number || v instanceof String) {
							ids.add(longOf(v));
						}
					}
				}
			}
			return ids;
		}
		return extractDistributorIds(distributors);
	}

	private static void assertStaffDistributorAccess(Object distributorIdParam, List<Long> distributorIds) {
		if (distributorIdParam == null || distributorIds.isEmpty()) {
			return;
		}
		if (distributorIdParam instanceof List<?> list) {
			Set<Long> allowed = new HashSet<>(distributorIds);
			if (list.size() != list.stream().filter(id -> allowed.contains(longOf(id))).count()) {
				throw new ResourceException("您没有权限管理此店铺", 500, 400002);
			}
			return;
		}
		if (isNumericDistributorId(distributorIdParam) && !distributorIds.contains(longOf(distributorIdParam))) {
			throw new ResourceException("您没有权限管理此店铺", 500, 400002);
		}
	}

	private static Object firstDistributorIdFromRequest(HttpServletRequest request) {
		String raw = request.getParameter("distributor_id");
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		if (raw.startsWith("[") && raw.endsWith("]")) {
			return raw;
		}
		String[] parts = request.getParameterValues("distributor_id");
		if (parts != null && parts.length > 1) {
			List<Long> ids = new ArrayList<>();
			for (String part : parts) {
				if (StringUtils.hasText(part)) {
					ids.add(Long.parseLong(part.trim()));
				}
			}
			return ids;
		}
		return raw.trim();
	}

	private static List<Long> extractDistributorIds(List<Map<String, Object>> distributors) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> d : distributors) {
			Object did = d.get("distributor_id");
			if (did != null) {
				ids.add(longOf(did));
			}
		}
		return ids;
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

	private static int menuVersionForOperatorType(String operatorType) {
		if ("distributor".equals(operatorType)) {
			return 3;
		}
		if ("dealer".equals(operatorType)) {
			return 5;
		}
		if ("merchant".equals(operatorType)) {
			return 6;
		}
		if ("supplier".equals(operatorType)) {
			return 7;
		}
		return 1;
	}

	private static boolean isNumericDistributorId(Object distributorId) {
		if (distributorId instanceof Number) {
			return true;
		}
		try {
			Long.parseLong(distributorId.toString().trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static ResourceException toActivatedResourceException(RuntimeException ex, String fallback, int code) {
		if (ex instanceof ResourceException resourceException) {
			return resourceException;
		}
		String message = ex.getMessage();
		if (!StringUtils.hasText(message)) {
			message = fallback;
		}
		return new ResourceException(message, 500, code);
	}

	private static long longOf(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
