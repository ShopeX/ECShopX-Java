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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.dto.DistributorIdRef;
import cn.shopex.ecshopx.companys.dto.EmployeeUpdateRequest;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.EmployeeService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 编辑企业员工账号：校验与分支编排后调用 {@link EmployeeService#updateOperatorStaff}。
 */
@Service
public class EmployeeUpdateOrchestrationService {

	private static final Set<String> PLATFORM_LIKE_JWT = Set.of("admin", "staff", "merchant");

	private static final Pattern PASSWORD_COMPLEXITY =
			Pattern.compile(
					"^(?!^[0-9]+$)(?!^[a-z]+$)(?!^[A-Z]+$)(?!^[^A-z0-9]+$)^[^\\s\\x{4e00}-\\x{9fa5}]{8,}$",
					Pattern.UNICODE_CHARACTER_CLASS);

	private final OperatorsQueryService operatorsQueryService;
	private final OperatorsMapper operatorsMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	private final EmployeeService employeeService;

	public EmployeeUpdateOrchestrationService(
			OperatorsQueryService operatorsQueryService,
			OperatorsMapper operatorsMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper,
			EmployeeService employeeService) {
		this.operatorsQueryService = operatorsQueryService;
		this.operatorsMapper = operatorsMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
		this.employeeService = employeeService;
	}

	public Map<String, Object> execute(EmployeeUpdateRequest body, Map<String, Object> jwt, long pathOperatorId) {
		long companyId = EmployeeOrchestrationSupport.requireLongClaim(jwt, "company_id", "companyId");
		long jwtOperatorId = EmployeeOrchestrationSupport.requireLongClaim(jwt, "operator_id", "operatorId");
		String jwtOperatorType = EmployeeOrchestrationSupport.strClaim(jwt, "operator_type", "operatorType");
		Long jwtMerchantId = EmployeeOrchestrationSupport.longClaimOrNull(jwt, "merchant_id", "merchantId");

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		if ("merchant".equals(jwtOperatorType) && jwtMerchantId != null) {
			params.put("merchant_id", jwtMerchantId);
		}

		if (body.getUsername() != null) {
			params.put("username", body.getUsername());
		}
		if (body.getLoginName() != null) {
			params.put("login_name", body.getLoginName());
		}
		if (body.getMobile() != null) {
			params.put("mobile", body.getMobile());
		}
		if (body.getHeadPortrait() != null) {
			params.put("head_portrait", body.getHeadPortrait());
		}
		if (body.getPassword() != null && !body.getPassword().isBlank()) {
			if (!PASSWORD_COMPLEXITY.matcher(body.getPassword()).matches()) {
				throw new ResourceException("密码至少8位以上，至少由数字、字母或特殊字符中两种及以上方式组成");
			}
			params.put("password", body.getPassword());
		}
		if (body.getRegionauthId() == null) {
			params.put("regionauth_id", 0L);
		} else {
			params.put("regionauth_id", body.getRegionauthId());
		}
		if (body.getRoleId() != null) {
			params.put("role_id", new ArrayList<>(body.getRoleId()));
		}
		if (body.getShopIds() != null) {
			params.put("shop_ids", new ArrayList<>(body.getShopIds()));
		}
		if (body.getOperatorType() != null) {
			params.put("operator_type", body.getOperatorType());
		}

		if (body.getDistributorIds() != null) {
			List<DistributorIdRef> deduped = dedupeDistributorIds(body.getDistributorIds());
			params.put("distributor_ids", EmployeeOrchestrationSupport.toDistributorMapList(deduped));
		}

		String newOpType = body.getOperatorType();

		if ("distributor".equals(newOpType)) {
			if (PLATFORM_LIKE_JWT.contains(jwtOperatorType)) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds = (List<Map<String, Object>>) params.get("distributor_ids");
				if (distIds != null && !distIds.isEmpty()) {
					for (Map<String, Object> item : distIds) {
						Object did = item.get("distributor_id");
						Long distributorId = EmployeeOrchestrationSupport.longFromDistributorIdObject(did);
						if (distributorId == null) {
							continue;
						}
						if (operatorsMapper.countDistributorMainConflictExcludingOperatorId(
										companyId, distributorId, pathOperatorId)
								>= 1L) {
							Object name = item.get("name");
							String shopName = name != null ? name.toString() : "";
							throw new ResourceException("店铺【" + shopName + "】已经有超级管理员！");
						}
					}
				}
				params.put("is_distributor_main", Boolean.TRUE);
			}
			if ("distributor".equals(jwtOperatorType)) {
				Map<String, Object> targetFilter = new HashMap<>(4);
				targetFilter.put("operator_id", pathOperatorId);
				targetFilter.put("company_id", companyId);
				Map<String, Object> target = operatorsQueryService.getInfo(targetFilter);
				boolean isDistributorMain = isTruthyDistributorMain(target != null ? target.get("is_distributor_main") : null);
				if (!isDistributorMain) {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> distIds = (List<Map<String, Object>>) params.get("distributor_ids");
					if (distIds == null || distIds.size() != 1) {
						throw new ResourceException("必须关联一个店铺！");
					}
					@SuppressWarnings("unchecked")
					List<String> roles = (List<String>) params.get("role_id");
					if (roles == null || roles.isEmpty()) {
						throw new ResourceException("至少关联一个角色！");
					}
				}
				Map<String, Object> filterJwt = new HashMap<>(2);
				filterJwt.put("operator_id", jwtOperatorId);
				Map<String, Object> cur = operatorsQueryService.getInfo(filterJwt);
				if (cur == null || cur.isEmpty()) {
					throw new ResourceException("没有账号信息");
				}
				Object mid = cur.get("merchant_id");
				params.put("merchant_id", mid != null ? toLong(mid) : 0L);
			}
		}

		if ("distributor".equals(jwtOperatorType)) {
			params.remove("distributor_ids");
		}

		if ("self_delivery_staff".equals(newOpType)) {
			params.put("staff_type", nullToEmpty(body.getStaffType()));
			params.put("staff_no", nullToEmpty(body.getStaffNo()));
			params.put("staff_attribute", nullToEmpty(body.getStaffAttribute()));
			params.put("payment_method", nullToEmpty(body.getPaymentMethod()));
			params.put("payment_fee", body.getPaymentFee());
			EmployeeOrchestrationSupport.validateSelfDeliveryStaffFiveFields(
					body.getStaffType(),
					body.getStaffNo(),
					body.getStaffAttribute(),
					body.getPaymentMethod(),
					body.getPaymentFee());
			if ("platform".equals(nullToEmpty(body.getStaffType()))) {
				Map<String, Object> selfRow = distributionDistributorSelfReadMapper.selectSelfStoreRow(companyId);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds = (List<Map<String, Object>>) params.get("distributor_ids");
				List<Map<String, Object>> list = distIds != null ? new ArrayList<>(distIds) : new ArrayList<>();
				Map<String, Object> append = new LinkedHashMap<>();
				Object did = selfRow != null ? selfRow.get("distributor_id") : 0L;
				Object nm = selfRow != null ? selfRow.get("name") : "";
				append.put("distributor_id", did);
				append.put("name", nm != null ? nm : "");
				list.add(append);
				params.put("distributor_ids", list);
			} else if (!"distributor".equals(jwtOperatorType)) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds = (List<Map<String, Object>>) params.get("distributor_ids");
				if (distIds == null || distIds.isEmpty()) {
					throw new ResourceException("必须关联一个店铺！");
				}
			}
			String staffNo = nullToEmpty(body.getStaffNo());
			if (employeeSelfDeliveryStaffMapper.countByStaffNoAndCompanyIdExcludingOperatorId(staffNo, companyId, pathOperatorId)
					>= 1L) {
				throw new ResourceException("配送员编号已存在！");
			}
		}

		Map<String, Object> filter = new HashMap<>(4);
		filter.put("company_id", companyId);
		filter.put("operator_id", pathOperatorId);
		return employeeService.updateOperatorStaff(params, filter);
	}

	private static List<DistributorIdRef> dedupeDistributorIds(List<DistributorIdRef> raw) {
		LinkedHashMap<String, DistributorIdRef> first = new LinkedHashMap<>();
		for (DistributorIdRef r : raw) {
			if (r == null) {
				continue;
			}
			Object did = r.getDistributorId();
			if (did == null) {
				continue;
			}
			String k = EmployeeOrchestrationSupport.normalizeDistributorIdForJsonLookup(did);
			first.putIfAbsent(k, r);
		}
		return new ArrayList<>(first.values());
	}

	private static String nullToEmpty(Object o) {
		return o == null ? "" : o.toString();
	}

	private static boolean isTruthyDistributorMain(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean b) {
			return b;
		}
		if (value instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = value.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
