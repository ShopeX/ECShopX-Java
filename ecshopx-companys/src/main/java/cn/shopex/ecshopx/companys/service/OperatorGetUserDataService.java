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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.mapper.SupplierOperatorReadMapper;
import cn.shopex.ecshopx.companys.service.employee.OperatorAccountOutsideLangReadService;
import cn.shopex.ecshopx.companys.service.operator.OperatorAdminAppDetailEnrichPort;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOperatorSalespersonInfoPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorGetUserDataService {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final MarketingCenterOperatorSalespersonInfoPort marketingCenterOperatorSalespersonInfoPort;

	private final OperatorsQueryService operatorsQueryService;

	private final OperatorAdminAppDetailEnrichPort operatorAdminAppDetailEnrichPort;

	private final SupplierOperatorReadMapper supplierOperatorReadMapper;


	private final ObjectMapper objectMapper;

	private final OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService;

	private final OperatorsMapper operatorsMapper;

	public OperatorGetUserDataService(
			MarketingCenterOperatorSalespersonInfoPort marketingCenterOperatorSalespersonInfoPort,
			OperatorsQueryService operatorsQueryService,
			OperatorAdminAppDetailEnrichPort operatorAdminAppDetailEnrichPort,
			SupplierOperatorReadMapper supplierOperatorReadMapper,
				ObjectMapper objectMapper,
			OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService,
			OperatorsMapper operatorsMapper) {
		this.marketingCenterOperatorSalespersonInfoPort = marketingCenterOperatorSalespersonInfoPort;
		this.operatorsQueryService = operatorsQueryService;
		this.operatorAdminAppDetailEnrichPort = operatorAdminAppDetailEnrichPort;
		this.supplierOperatorReadMapper = supplierOperatorReadMapper;
		this.objectMapper = objectMapper;
		this.operatorAccountOutsideLangReadService = operatorAccountOutsideLangReadService;
		this.operatorsMapper = operatorsMapper;
	}

	public Map<String, Object> getUserData(HttpServletRequest request, String isAppRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		String logintypeClaim = stringOrEmpty(jwt.get("logintype"));
		long companyId = readPositiveLongSameAsOperatorsController(jwt, "company_id");

		String source = String.valueOf(jwt.getOrDefault("source", "")).trim();
		if ("salesperson_workwechat".equals(source)) {
			String workUserid = String.valueOf(jwt.getOrDefault("work_userid", "")).trim();
			Map<String, Object> userInfo = marketingCenterOperatorSalespersonInfoPort.fetchSalespersonInfoByWorkUserid(
					companyId, workUserid, isAppDetailTruthy(isAppRaw));
			if (userInfo == null) {
				userInfo = new LinkedHashMap<>();
			}
			if (userInfo.containsKey("mobile")
					&& userInfo.get("mobile") != null
					&& !String.valueOf(userInfo.get("mobile")).trim().isEmpty()) {
				userInfo.put("mobile", DataMasking.maskMobile(String.valueOf(userInfo.get("mobile"))));
			}
			userInfo.put("logintype", "salesperson_workwechat");
			return userInfo;
		}

		Long opJwt = parseLongLoose(jwt.get("operator_id"));
		long operatorIdForFilter = (opJwt != null && opJwt > 0L) ? opJwt : 0L;
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_id", operatorIdForFilter);

		Map<String, Object> result = operatorsQueryService.getInfo(filter);
		if (result == null || result.isEmpty()) {
			result = new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> mutableResult = new LinkedHashMap<>(result);
		coalesceSnakeCaseFromCamelCaseAliases(mutableResult);
		hydrateUsernameIfMissing(companyId, mutableResult);

		if (isAppDetailTruthy(isAppRaw)) {
			operatorAdminAppDetailEnrichPort.enrichAppDetailForOperator(companyId, mutableResult);
		} else {
			decodeJsonColumnsIntoOperatorMap(mutableResult);
		}

		String requestLangTag = requestOperatorLangTag(request);
		operatorAccountOutsideLangReadService.applyForAccountList(companyId, requestLangTag, List.of(mutableResult));

		if (mutableResult.containsKey("password") && legacyTruthy(mutableResult.get("password"))) {
			mutableResult.remove("password");
		}
		mutableResult.put("logintype", logintypeClaim.isEmpty() ? "admin" : logintypeClaim);

		if ("supplier".equals(String.valueOf(mutableResult.get("logintype")).trim())) {
			Long parsed = parseLongLoose(mutableResult.get("operator_id"));
			if (parsed == null || parsed <= 0L) {
				mutableResult.put("supplier_check_status", 0);
			} else {
				Integer v = supplierOperatorReadMapper.selectIsCheckByCompanyIdAndOperatorId(companyId, parsed);
				mutableResult.put("supplier_check_status", v != null ? v : 0);
			}
		}

		ensureOperatorInfoNullPlaceholders(mutableResult);
		return mutableResult;
	}

	private static String requestOperatorLangTag(HttpServletRequest request) {
		String cc = request.getParameter("country_code");
		return StringUtils.hasText(cc) ? cc.trim() : "zh-CN";
	}

	/**
	 * Aligns JDBC/MyBatis map keys with legacy snake_case field names when global
	 * underscore-to-camelCase mapping produced camelCase aliases only.
	 */
	private static void coalesceSnakeCaseFromCamelCaseAliases(LinkedHashMap<String, Object> m) {
		coalesceKey(m, "operator_id", "operatorId");
		coalesceKey(m, "company_id", "companyId");
		coalesceKey(m, "login_name", "loginName");
		coalesceKey(m, "operator_type", "operatorType");
		coalesceKey(m, "merchant_id", "merchantId");
		coalesceKey(m, "regionauth_id", "regionauthId");
		coalesceKey(m, "username", "userName");
		coalesceKey(m, "head_portrait", "headPortrait");
		coalesceKey(m, "passport_uid", "passportUid");
		coalesceKey(m, "shopex_bind_account", "shopexBindAccount");
		coalesceKey(m, "split_ledger_info", "splitLedgerInfo");
		coalesceKey(m, "adapay_open_account_time", "adapayOpenAccountTime");
		coalesceKey(m, "dealer_parent_id", "dealerParentId");
		coalesceKey(m, "distributor_ids", "distributorIds");
		coalesceKey(m, "shop_ids", "shopIds");
		coalesceKey(m, "is_disable", "isDisable");
		coalesceKey(m, "is_dealer_main", "isDealerMain");
		coalesceKey(m, "is_merchant_main", "isMerchantMain");
		coalesceKey(m, "is_distributor_main", "isDistributorMain");
	}

	private static void coalesceKey(LinkedHashMap<String, Object> m, String snake, String camel) {
		if (m.containsKey(snake)) {
			m.remove(camel);
			return;
		}
		if (m.containsKey(camel)) {
			m.put(snake, m.remove(camel));
		}
	}

	/**
	 * Ensures the same top-level keys as the legacy operator row (null when unset), so JSON
	 * payloads expose explicit nulls instead of omitted entries.
	 */
	private static void ensureOperatorInfoNullPlaceholders(LinkedHashMap<String, Object> m) {
		putNullIfAbsent(m, "eid");
		putNullIfAbsent(m, "passport_uid");
		putNullIfAbsent(m, "shopex_bind_account");
		putNullIfAbsent(m, "head_portrait");
		putNullIfAbsent(m, "contact");
		putNullIfAbsent(m, "adapay_open_account_time");
		putNullIfAbsent(m, "dealer_parent_id");
		if (!m.containsKey("distributor_ids")) {
			m.put("distributor_ids", null);
		}
	}

	private static void putNullIfAbsent(LinkedHashMap<String, Object> m, String key) {
		if (!m.containsKey(key)) {
			m.put(key, null);
		}
	}

	private void hydrateUsernameIfMissing(long companyId, LinkedHashMap<String, Object> m) {
		Object u = m.get("username");
		if (u != null && StringUtils.hasText(String.valueOf(u))) {
			return;
		}
		Long oid = parseLongLoose(m.get("operator_id"));
		if (oid == null || oid <= 0L) {
			return;
		}
		Operators op = operatorsMapper.selectOne(new LambdaQueryWrapper<Operators>()
				.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getOperatorId, oid)
				.select(Operators::getUsername));
		if (op != null) {
			m.put("username", op.getUsername());
		}
	}

	private void decodeJsonColumnsIntoOperatorMap(LinkedHashMap<String, Object> opMap) {
		Object shopRaw = opMap.get("shop_ids");
		if (shopRaw instanceof String s && StringUtils.hasText(s)) {
			opMap.put("shop_ids", decodeShopIdsValue(s));
		}
		Object distRaw = opMap.get("distributor_ids");
		if (distRaw instanceof String s && StringUtils.hasText(s)) {
			opMap.put("distributor_ids", decodeDistributorIdsJson(s));
		}
	}

	private Object decodeShopIdsValue(String raw) {
		try {
			Object parsed = objectMapper.readValue(raw, Object.class);
			if (parsed instanceof List<?> || parsed instanceof Map<?, ?>) {
				return parsed;
			}
			return new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> decodeDistributorIdsJson(String raw) {
		try {
			List<Object> parsed = objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
			if (parsed == null) {
				return new ArrayList<>();
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : parsed) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						if (e.getKey() instanceof String k) {
							row.put(k, e.getValue());
						}
					}
					out.add(row);
				}
			}
			return out;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static boolean isAppDetailTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (Boolean.TRUE.equals(raw)) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static Long parseLongLoose(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				try {
					return new BigDecimal(t).longValue();
				} catch (Exception e2) {
					return null;
				}
			}
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				return new BigDecimal(s).longValue();
			} catch (Exception e2) {
				return null;
			}
		}
	}

	private static long readPositiveLongSameAsOperatorsController(Map<String, Object> jwt, String key) {
		Object v = jwt.get(key);
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static boolean legacyTruthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b.booleanValue();
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		if (o instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (o instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		String t = String.valueOf(o).trim();
		return !t.isEmpty() && !"0".equals(t);
	}
}
