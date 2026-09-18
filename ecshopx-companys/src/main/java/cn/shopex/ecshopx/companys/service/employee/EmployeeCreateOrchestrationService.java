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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.dto.DistributorIdRef;
import cn.shopex.ecshopx.companys.dto.EmployeeCreateRequest;
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
 * 创建企业员工账号：合并 JWT 与请求体、分层校验、分支编排后调用 {@link EmployeeService#createOperatorStaff}。
 */
@Service
public class EmployeeCreateOrchestrationService {

	private static final Set<String> HTTP_OPERATOR_TYPES = Set.of(
			"staff", "merchant", "distributor", "dealer", "supplier", "agent", "self_delivery_staff");

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789]\\d{9}$");

	private static final Set<String> PLATFORM_LIKE_JWT = Set.of("admin", "staff", "merchant");

	private final MemberOperatorContextService memberOperatorContextService;
	private final OperatorsQueryService operatorsQueryService;
	private final OperatorsMapper operatorsMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	private final EmployeeService employeeService;

	public EmployeeCreateOrchestrationService(
			MemberOperatorContextService memberOperatorContextService,
			OperatorsQueryService operatorsQueryService,
			OperatorsMapper operatorsMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper,
			EmployeeService employeeService) {
		this.memberOperatorContextService = memberOperatorContextService;
		this.operatorsQueryService = operatorsQueryService;
		this.operatorsMapper = operatorsMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
		this.employeeService = employeeService;
	}

	public Map<String, Object> execute(EmployeeCreateRequest body, Map<String, Object> jwtUserData) {
		long companyId = EmployeeOrchestrationSupport.requireLongClaim(jwtUserData, "company_id", "companyId");
		long jwtOperatorId = EmployeeOrchestrationSupport.requireLongClaim(jwtUserData, "operator_id", "operatorId");
		String jwtOperatorType = EmployeeOrchestrationSupport.strClaim(jwtUserData, "operator_type", "operatorType");
		Long jwtMerchantId = EmployeeOrchestrationSupport.longClaimOrNull(jwtUserData, "merchant_id", "merchantId");
		Long jwtDistributorId = EmployeeOrchestrationSupport.longClaimOrNull(jwtUserData, "distributor_id", "distributorId");

		Map<String, Object> p = new LinkedHashMap<>();
		p.put("company_id", companyId);
		p.put("operator_id", jwtOperatorId);
		p.put("distributor_id", jwtDistributorId != null ? jwtDistributorId : 0L);
		if ("merchant".equals(jwtOperatorType) && jwtMerchantId != null) {
			p.put("merchant_id", jwtMerchantId);
		}
		p.put("login_name", nullToEmpty(body.getLoginName()));
		p.put("mobile", nullToEmpty(body.getMobile()));
		p.put("username", body.getUsername());
		p.put("head_portrait", body.getHeadPortrait());
		p.put("password", body.getPassword());
		p.put("operator_type", body.getOperatorType());
		p.put("regionauth_id", body.getRegionauthId());
		p.put("contact", body.getContact());
		if (body.getRoleId() != null && !body.getRoleId().isEmpty()) {
			p.put("role_id", new ArrayList<>(body.getRoleId()));
		}
		if (body.getDistributorIds() != null && !body.getDistributorIds().isEmpty()) {
			p.put("distributor_ids", EmployeeOrchestrationSupport.toDistributorMapList(body.getDistributorIds()));
		} else {
			p.put("distributor_ids", new ArrayList<Map<String, Object>>());
		}
		if (body.getShopIds() != null && !body.getShopIds().isEmpty()) {
			p.put("shop_ids", new ArrayList<>(body.getShopIds()));
		} else {
			p.put("shop_ids", new ArrayList<>());
		}

		validatePrimaryRules(body, p);

		String newOpType = body.getOperatorType();
		if ("dealer".equals(newOpType)) {
			if (isBlank(body.getContact())) {
				throw new BadRequestException("联系人姓名必填");
			}
			Map<String, Object> memberCtx =
					memberOperatorContextService.resolve(jwtOperatorId, jwtOperatorType, jwtDistributorId);
			if ("dealer".equals(memberCtx.get("operator_type"))) {
				p.put("dealer_parent_id", String.valueOf(memberCtx.get("operator_id")));
				p.put("is_dealer_main", false);
			} else {
				p.put("is_dealer_main", true);
			}
		}

		if ("self_delivery_staff".equals(newOpType)) {
			p.put("staff_type", nullToEmpty(body.getStaffType()));
			p.put("staff_no", nullToEmpty(body.getStaffNo()));
			p.put("staff_attribute", nullToEmpty(body.getStaffAttribute()));
			p.put("payment_method", nullToEmpty(body.getPaymentMethod()));
			p.put("payment_fee", body.getPaymentFee());
			EmployeeOrchestrationSupport.validateSelfDeliveryStaffFiveFields(
					body.getStaffType(),
					body.getStaffNo(),
					body.getStaffAttribute(),
					body.getPaymentMethod(),
					body.getPaymentFee());
		}

		if ("distributor".equals(newOpType)) {
			if (PLATFORM_LIKE_JWT.contains(jwtOperatorType)) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds =
						(List<Map<String, Object>>) p.get("distributor_ids");
				if (distIds != null && !distIds.isEmpty()) {
					for (Map<String, Object> item : distIds) {
						Object did = item.get("distributor_id");
						Long distributorId = EmployeeOrchestrationSupport.longFromDistributorIdObject(did);
						if (distributorId == null) {
							continue;
						}
						if (operatorsMapper.countDistributorMainConflict(companyId, distributorId) >= 1L) {
							Object name = item.get("name");
							String shopName = name != null ? name.toString() : "";
							throw new ResourceException("店铺【" + shopName + "】已经有超级管理员！");
						}
					}
				}
				p.put("is_distributor_main", true);
				p.remove("role_id");
			}
			if ("distributor".equals(jwtOperatorType)) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds =
						(List<Map<String, Object>>) p.get("distributor_ids");
				if (distIds == null || distIds.size() != 1) {
					throw new ResourceException("必须关联一个店铺！");
				}
				@SuppressWarnings("unchecked")
				List<String> roles = (List<String>) p.get("role_id");
				if (roles == null || roles.isEmpty()) {
					throw new ResourceException("至少关联一个角色！");
				}
				Map<String, Object> filter = new HashMap<>(2);
				filter.put("operator_id", jwtOperatorId);
				Map<String, Object> cur = operatorsQueryService.getInfo(filter);
				if (cur == null || cur.isEmpty()) {
					throw new ResourceException("没有账号信息");
				}
				Object mid = cur.get("merchant_id");
				p.put("merchant_id", mid != null ? toLong(mid) : 0L);
			}
		}

		if ("self_delivery_staff".equals(newOpType)) {
			if ("platform".equals(nullToEmpty(body.getStaffType()))) {
				Map<String, Object> selfRow = distributionDistributorSelfReadMapper.selectSelfStoreRow(companyId);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds =
						(List<Map<String, Object>>) p.get("distributor_ids");
				List<Map<String, Object>> list = distIds != null ? new ArrayList<>(distIds) : new ArrayList<>();
				Map<String, Object> append = new LinkedHashMap<>();
				Object did = selfRow != null ? selfRow.get("distributor_id") : 0L;
				Object nm = selfRow != null ? selfRow.get("name") : "";
				append.put("distributor_id", did);
				append.put("name", nm != null ? nm : "");
				list.add(append);
				p.put("distributor_ids", list);
			} else {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distIds =
						(List<Map<String, Object>>) p.get("distributor_ids");
				if (distIds == null || distIds.isEmpty()) {
					throw new ResourceException("必须关联一个店铺！");
				}
			}
			String staffNo = nullToEmpty(body.getStaffNo());
			if (employeeSelfDeliveryStaffMapper.countByStaffNoAndCompanyId(staffNo, companyId) >= 1L) {
				throw new ResourceException("配送员编号已存在！");
			}
		}

		if (!p.containsKey("regionauth_id") || p.get("regionauth_id") == null) {
			p.put("regionauth_id", 0L);
		}

		String mobile = nullToEmpty(p.get("mobile"));
		if (!MOBILE_PATTERN.matcher(mobile).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}

		return employeeService.createOperatorStaff(p);
	}

	private static void validatePrimaryRules(EmployeeCreateRequest body, Map<String, Object> p) {
		if (isBlank(body.getMobile())) {
			throw new BadRequestException("手机号必填");
		}
		if (body.getPassword() == null || body.getPassword().isEmpty()) {
			throw new BadRequestException("登录密码必填");
		}
		String ot = body.getOperatorType();
		if (isBlank(ot) || !HTTP_OPERATOR_TYPES.contains(ot)) {
			throw new BadRequestException("账号类型错误");
		}
		if ("supplier".equals(ot) && isBlank(body.getLoginName())) {
			throw new BadRequestException("供应商编号必填");
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.isEmpty();
	}

	private static String nullToEmpty(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

}
