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

package cn.shopex.ecshopx.companys.service.employee;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.dto.DistributorMerchantRow;
import cn.shopex.ecshopx.companys.dto.EmployeeAccountManagementListQuery;
import cn.shopex.ecshopx.companys.dto.SelfDeliveryStaffAccountListRow;
import cn.shopex.ecshopx.companys.dto.SupplierOperatorNameRow;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.mapper.SupplierOperatorReadMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeManagementListService {

	private static final Set<String> DISTRIBUTOR_MAIN_JWT_TYPES = Set.of("admin", "staff", "merchant");

	private final OperatorsQueryService operatorsQueryService;
	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;
	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final SupplierOperatorReadMapper supplierOperatorReadMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public EmployeeManagementListService(
			OperatorsQueryService operatorsQueryService,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			SupplierOperatorReadMapper supplierOperatorReadMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.operatorsQueryService = operatorsQueryService;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.supplierOperatorReadMapper = supplierOperatorReadMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.operatorAccountOutsideLangReadService = operatorAccountOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getListData(
			Map<String, Object> jwt,
			EmployeeAccountManagementListQuery query,
			int datapassBlock,
			String requestLangTag) {
		Long companyId = EmployeeOrchestrationSupport.longClaimOrNull(jwt, "company_id", "companyId");
		if (companyId == null) {
			throw new BadRequestException("参数 company_id 错误");
		}
		long jwtOperatorId = EmployeeOrchestrationSupport.requireLongClaim(jwt, "operator_id", "operatorId");
		String jwtOperatorType = EmployeeOrchestrationSupport.strClaim(jwt, "operator_type", "operatorType");
		Long jwtMerchantId = EmployeeOrchestrationSupport.longClaimOrNull(jwt, "merchant_id", "merchantId");

		if ("distributor".equals(jwtOperatorType)) {
			assertDistributorOperatorJwtAndSelection(jwt, jwtOperatorId, companyId);
		}
		if ("staff".equals(jwtOperatorType)) {
			assertStaffDistributorQueryAllowed(jwt, query);
		}

		String inputOperatorType =
				StringUtils.hasText(query.getOperatorType()) ? query.getOperatorType().trim() : null;

		AccountManagementOperatorsFilter filter = new AccountManagementOperatorsFilter();
		filter.setCompanyId(companyId);
		if ("merchant".equals(jwtOperatorType) && jwtMerchantId != null) {
			filter.setMerchantId(jwtMerchantId);
		}
		if ("distributor".equals(inputOperatorType) && DISTRIBUTOR_MAIN_JWT_TYPES.contains(jwtOperatorType)) {
			filter.setIsDistributorMain(1);
		}
		if (query.getOperatorId() != null && !query.getOperatorId().isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (String s : query.getOperatorId()) {
				if (!StringUtils.hasText(s)) {
					continue;
				}
				try {
					ids.add(Long.parseLong(s.trim()));
				} catch (NumberFormatException ignored) {
					// skip invalid id
				}
			}
			if (!ids.isEmpty()) {
				filter.setOperatorIds(ids);
			}
		}
		if (StringUtils.hasText(query.getMobile())) {
			filter.setMobile(query.getMobile().trim());
		}
		String supplierName = query.getSupplierName();
		if (supplierName != null) {
			supplierName = supplierName.trim();
		}
		if (StringUtils.hasText(supplierName)) {
			List<Long> supOpIds =
					supplierOperatorReadMapper.selectOperatorIdsByCompanyIdAndSupplierNameLike(
							companyId, supplierName);
			if (!supOpIds.isEmpty()) {
				filter.setOperatorIds(supOpIds);
			}
		}
		if (StringUtils.hasText(query.getUsername())) {
			filter.setUsernameContains(query.getUsername().trim());
		}
		String isDisable = query.getIsDisable();
		if ("0".equals(isDisable) || "1".equals(isDisable)) {
			filter.setIsDisable(isDisable);
		}
		if (StringUtils.hasText(query.getLoginName())) {
			filter.setLoginName(query.getLoginName().trim());
		}
		if (StringUtils.hasText(query.getRoleId())) {
			filter.setRoleId(query.getRoleId().trim());
		}
		if (StringUtils.hasText(query.getPaymentMethod())) {
			List<Long> pmIds =
					employeeSelfDeliveryStaffMapper.selectOperatorIdsByCompanyIdAndPaymentMethod(
							companyId, query.getPaymentMethod().trim());
			if (!pmIds.isEmpty()) {
				filter.setOperatorIds(pmIds);
			}
		}
		if (StringUtils.hasText(query.getStaffType())) {
			List<Long> stIds =
					employeeSelfDeliveryStaffMapper.selectOperatorIdsByCompanyIdAndStaffType(
							companyId, query.getStaffType().trim());
			if (!stIds.isEmpty()) {
				filter.setOperatorIds(stIds);
			}
		}
		filter.setOperatorType(inputOperatorType);
		applyDistributorIdFilter(filter, jwt, query.getDistributorId());

		if ("self_delivery_staff".equals(inputOperatorType) && jwtMerchantId != null && jwtMerchantId > 0) {
			List<DistributorMerchantRow> dRows =
					distributionDistributorSelfReadMapper.listDistributorRowsByMerchant(
							jwtMerchantId, companyId);
			if (dRows != null && !dRows.isEmpty()) {
				List<Long> distributorIds = new ArrayList<>();
				for (DistributorMerchantRow row : dRows) {
					if (row.getDistributorId() != null) {
						distributorIds.add(row.getDistributorId());
					}
				}
				if (!distributorIds.isEmpty()) {
					filter.setDistributorIds(distributorIds);
					filter.setMerchantId(null);
				}
			}
		}

		if (filter.getMobile() != null && !filter.getMobile().isEmpty()) {
			filter.setMobile(sensitiveFieldEncryptor.encrypt(filter.getMobile()));
		}

		int page = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
		int pageSize = (query.getPageSize() != null && query.getPageSize() > 0) ? query.getPageSize() : 20;
		List<AccountManagementOperatorsFilter.OrderBy> orderBy =
				List.of(new AccountManagementOperatorsFilter.OrderBy(
						"created", AccountManagementOperatorsFilter.OrderBy.Direction.DESC));

		long total = operatorsQueryService.countAccountManagementList(filter);
		int offset = pageSize * (page - 1);
		List<Operators> operatorsRows =
				total > 0
						? operatorsQueryService.pageAccountManagementList(filter, offset, pageSize, orderBy)
						: List.of();

		Map<Long, SelfDeliveryStaffAccountListRow> selfByOp = new LinkedHashMap<>();
		if ("self_delivery_staff".equals(filter.getOperatorType()) && total > 0 && !operatorsRows.isEmpty()) {
			List<Long> opIds =
					operatorsRows.stream()
							.map(Operators::getOperatorId)
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
			List<SelfDeliveryStaffAccountListRow> sRows =
					employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(
							companyId, opIds);
			for (SelfDeliveryStaffAccountListRow r : sRows) {
				if (r.getOperatorId() != null) {
					selfByOp.put(r.getOperatorId(), r);
				}
			}
		}

		Map<Long, List<Map<String, Object>>> roleBatch =
				operatorDeliveryStaffRoleDataService.roleDataRowsForOperators(companyId, operatorsRows);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Operators op : operatorsRows) {
			Map<String, Object> row = operatorToResponseRow(op);
			Long oid = op.getOperatorId();
			if (oid != null) {
				row.put("role_data", roleBatch.getOrDefault(oid, List.of()));
			} else {
				row.put("role_data", List.of());
			}
			SelfDeliveryStaffAccountListRow sd = oid != null ? selfByOp.get(oid) : null;
			if (sd != null) {
				mergeSelfDelivery(row, sd);
			}
			list.add(row);
		}

		if (!list.isEmpty()) {
			operatorAccountOutsideLangReadService.applyForAccountList(companyId, requestLangTag, list);
			if (datapassBlock != 0) {
				for (Map<String, Object> row : list) {
					Object u = row.get("username");
					Object m = row.get("mobile");
					row.put(
							"username",
							DataMasking.maskTruenameIfBlocked(
									u != null ? u.toString() : null, datapassBlock));
					row.put(
							"mobile",
							DataMasking.maskMobileIfBlocked(
									m != null ? m.toString() : null, datapassBlock));
				}
			}
			if ("distributor".equals(jwtOperatorType)) {
				long pruneShopDistributorId = resolvePruneShopDistributorId(jwt, jwtOperatorId, companyId);
				list = pruneDistributorSubshops(list, pruneShopDistributorId);
			}
			if ("supplier".equals(inputOperatorType)) {
				attachSupplierNames(companyId, list);
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		result.put("datapass_block", datapassBlock);
		result.put("filter", buildFilterEcho(filter));
		return result;
	}

	public Map<String, Object> listSelfDeliveryStaffForH5Wxapp(
			long companyId, List<Long> selfDeliveryOperatorIds, String requestLangTag) {
		if (companyId <= 0L) {
			throw new BadRequestException("参数 company_id 错误");
		}
		AccountManagementOperatorsFilter f = new AccountManagementOperatorsFilter();
		f.setCompanyId(companyId);
		f.setOperatorType("self_delivery_staff");
		if (selfDeliveryOperatorIds != null && !selfDeliveryOperatorIds.isEmpty()) {
			f.setOperatorIds(selfDeliveryOperatorIds);
		}
		List<AccountManagementOperatorsFilter.OrderBy> orderBy =
				List.of(new AccountManagementOperatorsFilter.OrderBy(
						"created", AccountManagementOperatorsFilter.OrderBy.Direction.DESC));
		int pageSize = 500;
		int offset = 0;

		long total = operatorsQueryService.countAccountManagementList(f);
		List<Operators> operatorsRows =
				total > 0L
						? operatorsQueryService.pageAccountManagementList(f, offset, pageSize, orderBy)
						: List.of();

		Map<Long, SelfDeliveryStaffAccountListRow> selfByOp = new LinkedHashMap<>();
		if ("self_delivery_staff".equals(f.getOperatorType()) && total > 0L && !operatorsRows.isEmpty()) {
			List<Long> opIds =
					operatorsRows.stream()
							.map(Operators::getOperatorId)
							.filter(Objects::nonNull)
							.toList();
			if (!opIds.isEmpty()) {
				List<SelfDeliveryStaffAccountListRow> sRows =
						employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(
								companyId, opIds);
				for (SelfDeliveryStaffAccountListRow r : sRows) {
					if (r.getOperatorId() != null) {
						selfByOp.put(r.getOperatorId(), r);
					}
				}
			}
		}

		Map<Long, List<Map<String, Object>>> roleBatch =
				operatorDeliveryStaffRoleDataService.roleDataRowsForOperators(companyId, operatorsRows);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Operators op : operatorsRows) {
			Map<String, Object> row = operatorToResponseRow(op);
			Long oid = op.getOperatorId();
			row.put("role_data", oid != null ? roleBatch.getOrDefault(oid, List.of()) : List.of());
			if (oid != null && selfByOp.containsKey(oid)) {
				mergeSelfDelivery(row, selfByOp.get(oid));
			}
			list.add(row);
		}

		if (!list.isEmpty()) {
			operatorAccountOutsideLangReadService.applyForAccountList(companyId, requestLangTag, list);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private long resolvePruneShopDistributorId(Map<String, Object> jwt, long operatorId, long companyId) {
		return normalizePositiveOperatorId(jwt.get("distributor_id")).orElse(0L);
	}

	private List<Map<String, Object>> pruneDistributorSubshops(List<Map<String, Object>> list, long pruneId) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object raw = row.get("distributor_ids");
			List<Map<String, Object>> parsed = parseDistributorIdsList(raw);
			List<Map<String, Object>> kept = new ArrayList<>();
			for (Map<String, Object> item : parsed) {
				Object did = item.get("distributor_id");
				long d = toLong(did, -1L);
				if (d == pruneId) {
					kept.add(item);
				}
			}
			if (kept.isEmpty()) {
				continue;
			}
			Map<String, Object> copy = new LinkedHashMap<>(row);
			copy.put("distributor_ids", kept);
			out.add(copy);
		}
		return out;
	}

	private void assertDistributorOperatorJwtAndSelection(
			Map<String, Object> jwt, long jwtOperatorId, long companyId) {
		Set<Long> allowed = parseAllowedDistributorIdsFromJwt(jwt.get("distributor_ids"));
		if (allowed.isEmpty()) {
			throw new ResourceException("权限信息有误");
		}
		long pruneShopDistributorId = resolvePruneShopDistributorId(jwt, jwtOperatorId, companyId);
		if (pruneShopDistributorId > 0 && !allowed.contains(pruneShopDistributorId)) {
			throw new ForbiddenException("您没有权限管理此店铺");
		}
	}

	private void assertStaffDistributorQueryAllowed(
			Map<String, Object> jwt, EmployeeAccountManagementListQuery query) {
		Set<Long> allowed = parseAllowedDistributorIdsFromJwt(jwt.get("distributor_ids"));
		if (allowed.isEmpty()) {
			return;
		}
		String q = query.getDistributorId();
		if (!StringUtils.hasText(q)) {
			return;
		}
		OptionalLong queryDid = normalizePositiveOperatorId(q.trim());
		if (queryDid.isPresent()
				&& queryDid.getAsLong() > 0
				&& !allowed.contains(queryDid.getAsLong())) {
			throw new ForbiddenException("您没有权限管理此店铺");
		}
	}

	private Set<Long> parseAllowedDistributorIdsFromJwt(Object raw) {
		Set<Long> out = new HashSet<>();
		if (raw == null) {
			return out;
		}
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				addAllowedDistributorIdFromJwt(o, out);
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				JsonNode arr = objectMapper.readTree(s);
				if (!arr.isArray()) {
					return out;
				}
				for (JsonNode n : arr) {
					if (n.isObject()) {
						JsonNode idNode = n.get("distributor_id");
						if (idNode != null && idNode.isNumber()) {
							long v = idNode.longValue();
							if (v > 0) {
								out.add(v);
							}
						} else if (idNode != null && idNode.isTextual()) {
							addPositiveLongStringToSet(idNode.asText(), out);
						}
					} else if (n.isNumber()) {
						long v = n.longValue();
						if (v > 0) {
							out.add(v);
						}
					}
				}
			} catch (Exception e) {
				return out;
			}
		}
		return out;
	}

	private static void addAllowedDistributorIdFromJwt(Object o, Set<Long> out) {
		if (o instanceof Map<?, ?> m) {
			Object did = m.get("distributor_id");
			addLongObjectToAllowedSet(did, out);
		} else if (o instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				out.add(v);
			}
		}
	}

	private static void addLongObjectToAllowedSet(Object did, Set<Long> out) {
		if (did == null) {
			return;
		}
		if (did instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				out.add(v);
			}
			return;
		}
		addPositiveLongStringToSet(did.toString(), out);
	}

	private static void addPositiveLongStringToSet(String s, Set<Long> out) {
		if (!StringUtils.hasText(s)) {
			return;
		}
		try {
			long v = Long.parseLong(s.trim());
			if (v > 0) {
				out.add(v);
			}
		} catch (NumberFormatException ignored) {
			// skip
		}
	}

	private void attachSupplierNames(long companyId, List<Map<String, Object>> list) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object oid = row.get("operator_id");
			long id = toLong(oid, 0L);
			if (id > 0) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			for (Map<String, Object> row : list) {
				row.put("supplier_name", "");
			}
			return;
		}
		List<SupplierOperatorNameRow> rows =
				supplierOperatorReadMapper.selectSupplierNameRowsByCompanyIdAndOperatorIds(companyId, ids);
		Map<Long, String> nameByOp =
				rows.stream()
						.filter(r -> r.getOperatorId() != null)
						.collect(
								Collectors.toMap(
										SupplierOperatorNameRow::getOperatorId,
										r -> r.getSupplierName() != null ? r.getSupplierName() : "",
										(a, b) -> b));
		for (Map<String, Object> row : list) {
			long oid = toLong(row.get("operator_id"), 0L);
			row.put("supplier_name", nameByOp.getOrDefault(oid, ""));
		}
	}

	private static long toLong(Object o, long defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private List<Map<String, Object>> parseDistributorIdsList(Object raw) {
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> map) {
					Map<String, Object> one = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : map.entrySet()) {
						one.put(String.valueOf(e.getKey()), e.getValue());
					}
					out.add(one);
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				List<Map<String, Object>> parsed =
						objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
				return parsed != null ? parsed : new ArrayList<>();
			} catch (Exception e) {
				return new ArrayList<>();
			}
		}
		return new ArrayList<>();
	}

	private void mergeSelfDelivery(Map<String, Object> row, SelfDeliveryStaffAccountListRow sd) {
		if (sd.getDistributorId() != null) {
			row.put("distributor_id", sd.getDistributorId());
		}
		if (sd.getShopId() != null) {
			row.put("shop_id", sd.getShopId());
		}
		if (sd.getStaffAttribute() != null) {
			row.put("staff_attribute", sd.getStaffAttribute());
		}
		if (sd.getStaffNo() != null) {
			row.put("staff_no", sd.getStaffNo());
		}
		if (sd.getStaffType() != null) {
			row.put("staff_type", sd.getStaffType());
		}
		if (sd.getPaymentMethod() != null) {
			row.put("payment_method", sd.getPaymentMethod());
		}
		if (sd.getPaymentFee() != null) {
			row.put("payment_fee", sd.getPaymentFee());
		}
		if (sd.getCreated() != null) {
			row.put("created", sd.getCreated());
		}
		if (sd.getUpdated() != null) {
			row.put("updated", sd.getUpdated());
		}
	}

	private Map<String, Object> operatorToResponseRow(Operators op) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("operator_id", op.getOperatorId());
		m.put("mobile", op.getMobile());
		m.put("login_name", op.getLoginName());
		m.put("password", op.getPassword());
		m.put("eid", op.getEid());
		m.put("passport_uid", op.getPassportUid());
		m.put("operator_type", op.getOperatorType());
		m.put("shop_ids", parseJsonArray(op.getShopIds()));
		m.put("distributor_ids", parseDistributorIdsForResponse(op.getDistributorIds()));
		m.put("company_id", op.getCompanyId());
		m.put("username", op.getUsername());
		m.put("head_portrait", op.getHeadPortrait());
		m.put("regionauth_id", op.getRegionauthId());
		m.put("split_ledger_info", op.getSplitLedgerInfo());
		m.put("contact", op.getContact());
		m.put("is_disable", op.getIsDisable());
		m.put("adapay_open_account_time", op.getAdapayOpenAccountTime());
		m.put("dealer_parent_id", op.getDealerParentId());
		m.put("is_dealer_main", op.getIsDealerMain());
		m.put("created", op.getCreated());
		m.put("updated", op.getUpdated());
		m.put("merchant_id", op.getMerchantId());
		m.put("is_merchant_main", op.getIsMerchantMain());
		m.put("is_distributor_main", op.getIsDistributorMain());
		return m;
	}

	private List<Object> parseJsonArray(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<Object> parsed = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
			return parsed != null ? parsed : new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> parseDistributorIdsForResponse(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<Map<String, Object>> parsed =
					objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
			return parsed != null ? parsed : new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private Map<String, Object> buildFilterEcho(AccountManagementOperatorsFilter f) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", f.getCompanyId());
		if (f.getMerchantId() != null) {
			m.put("merchant_id", f.getMerchantId());
		}
		if (f.getIsDistributorMain() != null) {
			m.put("is_distributor_main", f.getIsDistributorMain());
		}
		if (f.getOperatorIds() != null && !f.getOperatorIds().isEmpty()) {
			m.put("operator_id", new ArrayList<>(f.getOperatorIds()));
		}
		if (f.getMobile() != null && !f.getMobile().isEmpty()) {
			m.put("mobile", f.getMobile());
		}
		if (StringUtils.hasText(f.getUsernameContains())) {
			m.put("username|contains", f.getUsernameContains());
		}
		if (StringUtils.hasText(f.getIsDisable())) {
			m.put("is_disable", f.getIsDisable());
		}
		if (StringUtils.hasText(f.getLoginName())) {
			m.put("login_name", f.getLoginName());
		}
		if (StringUtils.hasText(f.getRoleId())) {
			m.put("role_id", f.getRoleId());
		}
		m.put("operator_type", f.getOperatorType());
		if (f.getDistributorIds() != null && !f.getDistributorIds().isEmpty()) {
			m.put("distributor_ids", new ArrayList<>(f.getDistributorIds()));
		}
		return m;
	}

	private void applyDistributorIdFilter(
			AccountManagementOperatorsFilter filter, Map<String, Object> jwt, String queryDistributorId) {
		OptionalLong longFromJwt = normalizePositiveOperatorId(jwt.get("distributor_id"));
		OptionalLong fromQuery = OptionalLong.empty();
		if (StringUtils.hasText(queryDistributorId)) {
			String q = queryDistributorId.trim();
			if (StringUtils.hasText(q)) {
				fromQuery = normalizePositiveOperatorId(q);
			}
		}
		OptionalLong effective = fromQuery.isPresent() ? fromQuery : longFromJwt;
		if (effective.isPresent() && effective.getAsLong() > 0) {
			filter.setDistributorIds(List.of(effective.getAsLong()));
		}
	}

	private static OptionalLong normalizePositiveOperatorId(Object raw) {
		if (raw == null) {
			return OptionalLong.empty();
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? OptionalLong.of(v) : OptionalLong.empty();
		}
		if (raw instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty() || "0".equals(s)) {
				return OptionalLong.empty();
			}
			try {
				long v = Long.parseLong(s);
				return v > 0 ? OptionalLong.of(v) : OptionalLong.empty();
			} catch (NumberFormatException e) {
				return OptionalLong.empty();
			}
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return OptionalLong.empty();
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? OptionalLong.of(v) : OptionalLong.empty();
		} catch (NumberFormatException e) {
			return OptionalLong.empty();
		}
	}
}
