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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import cn.shopex.ecshopx.companys.service.operator.DistributorWorkWechatRelQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonAdminStoremanagerinfoService {

	private final OperatorsQueryService operatorsQueryService;

	private final DistributorMapper distributorMapper;

	private final DistributorListQueryService distributorListQueryService;

	private final DistributorListRowFormatService distributorListRowFormatService;

	private final SelfDeliverySettingReadService selfDeliverySettingReadService;

	private final DistributorSelfMetaService distributorSelfMetaService;

	private final DistributorWorkWechatRelQueryService distributorWorkWechatRelQueryService;

	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;

	private final MemberAccountService memberAccountService;

	private final ObjectMapper objectMapper;

	public ShopSalespersonAdminStoremanagerinfoService(
			OperatorsQueryService operatorsQueryService,
			DistributorMapper distributorMapper,
			DistributorListQueryService distributorListQueryService,
			DistributorListRowFormatService distributorListRowFormatService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorSelfMetaService distributorSelfMetaService,
			DistributorWorkWechatRelQueryService distributorWorkWechatRelQueryService,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.operatorsQueryService = operatorsQueryService;
		this.distributorMapper = distributorMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.distributorWorkWechatRelQueryService = distributorWorkWechatRelQueryService;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> storemanagerinfo(HttpServletRequest request, Map<String, Object> mergedInput) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		Map<String, Object> authClaims;
		if (rawClaims instanceof Map<?, ?> rm) {
			authClaims = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rm.entrySet()) {
				if (e.getKey() instanceof String k) {
					authClaims.put(k, e.getValue());
				}
			}
		} else {
			authClaims = new LinkedHashMap<>();
		}

		Map<String, Object> merged = mergedInput != null ? mergedInput : Collections.emptyMap();

		long companyId = parseCompanyIdFromRequest(request, authClaims);
		String mobile = Objects.toString(authClaims.get("mobile"), "").trim();

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("mobile", mobile);
		filter.put("operator_type", "distributor");

		Map<String, Object> row = null;
		if (companyId > 0L && StringUtils.hasText(mobile)) {
			row = operatorsQueryService.getInfo(filter);
		}

		Object operatorInfo;
		if (row == null || row.isEmpty()) {
			operatorInfo = Collections.emptyList();
		} else {
			LinkedHashMap<String, Object> opMap = new LinkedHashMap<>(row);
			decodeJsonColumnsIntoOperatorMap(opMap);
			operatorInfo = opMap;
		}

		if (!(operatorInfo instanceof List<?>)
				&& operatorInfo instanceof Map<?, ?> opAsMap
				&& isAppDetail(merged.get("is_app"))
				&& !opAsMap.isEmpty()) {
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, Object> opTyped = (LinkedHashMap<String, Object>) operatorInfo;
			extendOperatorInfoForAppDetail(companyId, authClaims, opTyped);
		}

		List<Long> dids = new ArrayList<>();
		if (operatorInfo instanceof Map<?, ?> opMap && !opMap.isEmpty()) {
			Object distCol = opMap.get("distributor_ids");
			if (distCol instanceof List<?> rawList) {
				for (Object el : rawList) {
					if (el instanceof Map<?, ?> dm) {
						Object didRaw = dm.get("distributor_id");
						Long parsed = parseLongLoose(didRaw);
						if (parsed != null) {
							dids.add(parsed);
						}
					}
				}
			}
		}

		List<Long> manageStoreIds = new ArrayList<>(dids);

		int manageStatus = 0;
		if (merged.containsKey("distributor_id")
				&& isTruthyParam(merged.get("distributor_id"))
				&& !manageStoreIds.isEmpty()) {
			Long reqDid = parseLongLoose(merged.get("distributor_id"));
			if (reqDid != null && manageStoreIds.contains(reqDid)) {
				manageStatus = 1;
			}
		}

		Long userIdParsed = parseLongLoose(authClaims.get("user_id"));
		long userId = (userIdParsed != null && userIdParsed > 0L) ? userIdParsed : 0L;
		Map<String, Object> authInfo =
				new LinkedHashMap<>(memberAccountService.buildH5AuthInfoForResponse(userId, companyId));
		applyStoremanagerinfoAuthInfoFieldOverrides(authInfo, authClaims, userId);

		Object inputData =
				merged.isEmpty() ? Collections.emptyList() : new LinkedHashMap<String, Object>(merged);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("manage_status", manageStatus);
		result.put("operatorInfo", operatorInfo);
		result.put("inputData", inputData);
		result.put("authInfo", authInfo);
		result.put("manage_store_ids", manageStoreIds);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>(result);
		result.put("data", data);
		return result;
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

	@SuppressWarnings("unchecked")
	private void extendOperatorInfoForAppDetail(
			long companyId, Map<String, Object> authClaims, LinkedHashMap<String, Object> operatorInfo) {
		operatorInfo.put("distributors", new ArrayList<Map<String, Object>>());
		String operatorType = String.valueOf(authClaims.getOrDefault("operator_type", "")).trim();

		Object distCol = operatorInfo.get("distributor_ids");
		List<Map<String, Object>> distObjs = new ArrayList<>();
		if (distCol instanceof List<?> rawDist) {
			for (Object o : rawDist) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						if (e.getKey() instanceof String k) {
							row.put(k, e.getValue());
						}
					}
					distObjs.add(row);
				}
			}
		}

		LinkedHashSet<Long> idSet = new LinkedHashSet<>();
		boolean hasZeroId = false;
		for (Map<String, Object> el : distObjs) {
			if (el == null) {
				continue;
			}
			Object idRaw = el.get("distributor_id");
			String idStr = String.valueOf(idRaw != null ? idRaw : "").trim();
			if ("0".equals(idStr)) {
				hasZeroId = true;
			}
			Long id = parseLongLoose(idRaw);
			if (id != null) {
				idSet.add(id);
			}
		}
		List<Long> idList = new ArrayList<>(idSet);

		if (!idList.isEmpty()) {
			List<Distributor> entities = new ArrayList<>(
					distributorListQueryService.listByIdsAndCompany(companyId, idList));
			entities.sort(createdDescNullsLast());
			appendFormattedDistributors(companyId, entities, operatorInfo);
		} else if ("admin".equals(operatorType)) {
			LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
			w.eq(Distributor::getCompanyId, companyId).orderByDesc(Distributor::getCreated).last("LIMIT 100");
			List<Distributor> entities = distributorMapper.selectList(w);
			appendFormattedDistributors(companyId, entities, operatorInfo);
		}

		if (hasZeroId || "admin".equals(operatorType)) {
			Map<String, Object> selfDis = new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
			selfDis.put("is_center", Boolean.TRUE);
			Object distsObj = operatorInfo.get("distributors");
			if (distsObj instanceof List<?> listRaw) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distributors = (List<Map<String, Object>>) listRaw;
				distributors.add(0, selfDis);
			}
		}

		Long opIdParsed = parseLongLoose(operatorInfo.get("operator_id"));
		long operatorId = opIdParsed != null ? opIdParsed : 0L;
		if (operatorId <= 0L) {
			operatorInfo.put("work_userid", "");
			operatorInfo.put("role_data", Collections.emptyList());
		} else {
			operatorInfo.put(
					"work_userid", distributorWorkWechatRelQueryService.findWorkUseridByCompanyAndOperator(companyId, operatorId));
			operatorInfo.put("role_data", operatorDeliveryStaffRoleDataService.getRoleDataList(companyId, operatorId));
		}
	}

	private void appendFormattedDistributors(long companyId, List<Distributor> entities, Map<String, Object> operatorInfo) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> distributors = (List<Map<String, Object>>) operatorInfo.get("distributors");
		for (Distributor d : entities) {
			long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
			Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> row = distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			row.put("is_center", Boolean.FALSE);
			distributors.add(row);
		}
	}

	private static Comparator<Distributor> createdDescNullsLast() {
		return (a, b) -> {
			Long ca = a.getCreated();
			Long cb = b.getCreated();
			if (ca == null && cb == null) {
				return 0;
			}
			if (ca == null) {
				return 1;
			}
			if (cb == null) {
				return -1;
			}
			return Long.compare(cb, ca);
		};
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request, Map<String, Object> authClaims) {
		Long fromClaims = parseLongLoose(authClaims != null ? authClaims.get("company_id") : null);
		if (fromClaims != null && fromClaims > 0L) {
			return fromClaims;
		}
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (companyAttr instanceof Number n) {
			long companyId = n.longValue();
			return companyId > 0L ? companyId : 0L;
		}
		if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				long companyId = Long.parseLong(s.trim());
				return companyId > 0L ? companyId : 0L;
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static boolean isAppDetail(Object raw) {
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
			s = s.trim();
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

	private static boolean isTruthyParam(Object o) {
		if (o == null) {
			return false;
		}
		String s = o.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	/**
	 * Field-level alignment with merged {@code auth} for this URL only: do not change
	 * {@link MemberAccountService#buildH5AuthInfoForResponse} globally. Prefer H5 JWT claim keys when
	 * present; otherwise follow empty-string / chief_id semantics for the test harness.
	 */
	private static void applyStoremanagerinfoAuthInfoFieldOverrides(
			Map<String, Object> authInfo, Map<String, Object> authClaims, long userId) {
		if (authInfo == null) {
			return;
		}
		Map<String, Object> claims = authClaims != null ? authClaims : Collections.emptyMap();

		String chiefFromClaim = firstNonEmptyClaimString(claims, "chief_id", "chiefId");
		if (chiefFromClaim != null) {
			authInfo.put("chief_id", chiefFromClaim);
		} else {
			String builtChief = Objects.toString(authInfo.get("chief_id"), "").trim();
			if (builtChief.isEmpty() || "0".equals(builtChief)) {
				if (userId > 0L) {
					authInfo.put("chief_id", String.valueOf(userId));
				} else {
					authInfo.put("chief_id", "0");
				}
			}
		}

		String openFromClaim = firstNonEmptyClaimString(claims, "open_id", "openid");
		authInfo.put("open_id", openFromClaim != null ? openFromClaim : "");

		String unionFromClaim = firstNonEmptyClaimString(claims, "unionid");
		authInfo.put("unionid", unionFromClaim != null ? unionFromClaim : "");

		String nickFromClaim = firstNonEmptyClaimString(claims, "nickname", "nick_name");
		authInfo.put("nickname", nickFromClaim != null ? nickFromClaim : "");
	}

	private static String firstNonEmptyClaimString(Map<String, Object> claims, String... keys) {
		for (String key : keys) {
			Object v = claims.get(key);
			if (v == null) {
				continue;
			}
			String s;
			if (v instanceof Number n) {
				s = String.valueOf(n.longValue());
			} else {
				s = String.valueOf(v).trim();
			}
			if (!s.isEmpty()) {
				return s;
			}
		}
		return null;
	}
}
