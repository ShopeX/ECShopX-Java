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

import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.EmployeeRelRoles;
import cn.shopex.ecshopx.companys.dto.SelfDeliveryStaffAccountListRow;
import cn.shopex.ecshopx.companys.mapper.EmployeeRelRolesMapper;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.companys.service.employee.EmployeeUserSyncPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmployeeService {

	private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

	private static final Pattern MOBILE_STAFF = Pattern.compile("^1[3456789]\\d{9}$");

	/** {@code synctype} payload value after successful operator create. */
	private static final String EMPLOYEE_USER_SYNC_TYPE_ADD = "add";

	/** {@code synctype} payload value after successful operator update. */
	private static final String EMPLOYEE_USER_SYNC_TYPE_UPDATE = "update";

	/** {@code synctype} payload value after successful operator delete (queue envelope). */
	private static final String EMPLOYEE_USER_SYNC_TYPE_DELETE = "del";

	private final OperatorsQueryService operatorsQueryService;
	private final MerchantQueryService merchantQueryService;
	private final OperatorsCommandService operatorsCommandService;
	private final EmployeeRelRolesMapper employeeRelRolesMapper;
	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	private final EmployeeUserSyncPublisher employeeUserSyncPublisher;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;
	private final CompanysActivationService companysActivationService;
	private final TransactionTemplate transactionTemplate;

	public EmployeeService(
			OperatorsQueryService operatorsQueryService,
			MerchantQueryService merchantQueryService,
			OperatorsCommandService operatorsCommandService,
			EmployeeRelRolesMapper employeeRelRolesMapper,
			EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper,
			EmployeeUserSyncPublisher employeeUserSyncPublisher,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort,
			CompanysActivationService companysActivationService,
			PlatformTransactionManager transactionManager) {
		this.operatorsQueryService = operatorsQueryService;
		this.merchantQueryService = merchantQueryService;
		this.operatorsCommandService = operatorsCommandService;
		this.employeeRelRolesMapper = employeeRelRolesMapper;
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
		this.employeeUserSyncPublisher = employeeUserSyncPublisher;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
		this.companysActivationService = companysActivationService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public Map<String, Object> employeeLogin(Map<String, Object> params) {
		String username = params.get("username") != null ? params.get("username").toString() : null;
		String password = params.get("password") != null ? params.get("password").toString() : null;
		String logintype = params.get("logintype") != null ? params.get("logintype").toString() : "staff";
		Map<String, Object> filter = new HashMap<>();
		if (username != null && MOBILE_STAFF.matcher(username).matches()) {
			filter.put("mobile", username);
		} else {
			filter.put("login_name", username);
		}
		filter.put("operator_type", logintype);
		Map<String, Object> operator = operatorsQueryService.getInfo(filter);
		if (operator == null || operator.isEmpty()) {
			throw new UnauthorizedException("账号不存在");
		}
		Object dis = operator.get("is_disable");
		if (Boolean.TRUE.equals(dis) || Integer.valueOf(1).equals(dis) || "1".equals(String.valueOf(dis))) {
			throw new UnauthorizedException("账号已禁用");
		}
		String hash = operator.get("password") != null ? operator.get("password").toString() : "";
		if (password == null || !BCrypt.checkpw(password, hash)) {
			throw new UnauthorizedException("账号密码错误，请重新登录");
		}
		if ("distributor".equals(logintype)) {
			Object companyId = operator.get("company_id");
			Object merchantId = operator.get("merchant_id");
			long mid = merchantId != null ? toLong(merchantId) : 0L;
			if (mid > 0) {
				Merchant m = merchantQueryService.getInfo(toLong(companyId), mid, false);
				if (m == null) {
					throw new UnauthorizedException("商户未开启，请确认后再重试");
				}
			}
		}
		return operator;
	}

	public Map<String, Object> getInfoStaff(long operatorId, long companyId) {
		Map<String, Object> operator =
				operatorsQueryService.getInfo(Map.of("operator_id", operatorId, "company_id", companyId));
		if (operator == null || operator.isEmpty()) {
			return Collections.emptyMap();
		}
		if ("self_delivery_staff".equals(String.valueOf(operator.get("operator_type")))) {
			List<SelfDeliveryStaffAccountListRow> rows =
					employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(
							companyId, List.of(operatorId));
			if (rows != null && !rows.isEmpty()) {
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>(operator);
				mergeSelfDeliveryStaffAccountFields(merged, rows.get(0));
				return merged;
			}
		}
		return operator;
	}

	private static void mergeSelfDeliveryStaffAccountFields(
			LinkedHashMap<String, Object> target, SelfDeliveryStaffAccountListRow sd) {
		if (sd.getDistributorId() != null) {
			target.put("distributor_id", sd.getDistributorId());
		}
		if (sd.getShopId() != null) {
			target.put("shop_id", sd.getShopId());
		}
		if (sd.getStaffAttribute() != null) {
			target.put("staff_attribute", sd.getStaffAttribute());
		}
		if (sd.getStaffNo() != null) {
			target.put("staff_no", sd.getStaffNo());
		}
		if (sd.getStaffType() != null) {
			target.put("staff_type", sd.getStaffType());
		}
		if (sd.getPaymentMethod() != null) {
			target.put("payment_method", sd.getPaymentMethod());
		}
		if (sd.getPaymentFee() != null) {
			target.put("payment_fee", sd.getPaymentFee());
		}
		if (sd.getCreated() != null) {
			target.put("created", sd.getCreated());
		}
		if (sd.getUpdated() != null) {
			target.put("updated", sd.getUpdated());
		}
	}

	public Map<String, Object> createOperatorStaff(Map<String, Object> params) {
		return transactionTemplate.execute(status -> createOperatorStaffTransactional(params));
	}

	public Map<String, Object> updateOperatorStaff(Map<String, Object> params, Map<String, Object> filter) {
		return transactionTemplate.execute(status -> updateOperatorStaffTransactional(params, filter));
	}

	public void deleteOperatorStaff(long targetOperatorId, long companyId, long actingOperatorId) {
		transactionTemplate.executeWithoutResult(
				status -> deleteOperatorStaffTransactional(targetOperatorId, companyId, actingOperatorId));
	}

	private void deleteOperatorStaffTransactional(
			long targetOperatorId, long companyId, long actingOperatorId) {
		Map<String, Object> operatorsData = getInfoStaff(targetOperatorId, companyId);
		if (operatorsData.isEmpty()) {
			throw new ResourceException("请选择要删除的账号");
		}
		long rowCompanyId = toLong(operatorsData.get("company_id"));
		if (rowCompanyId != companyId) {
			throw new ForbiddenException("无权操作该账号");
		}

		String mobile = str(operatorsData.get("mobile"));

		if ("self_delivery_staff".equals(String.valueOf(operatorsData.get("operator_type")))) {
			employeeSelfDeliveryStaffMapper.deleteStaffRowByCompanyIdAndOperatorId(companyId, targetOperatorId);
		}

		LambdaQueryWrapper<EmployeeRelRoles> relDel = new LambdaQueryWrapper<>();
		relDel.eq(EmployeeRelRoles::getCompanyId, companyId)
				.eq(EmployeeRelRoles::getOperatorId, String.valueOf(targetOperatorId));
		int relDeleted = employeeRelRolesMapper.delete(relDel);
		if (relDeleted < 1) {
			throw new ResourceException("删除账号失败");
		}

		operatorsCommandService.deleteByOperatorIdAndCompanyId(companyId, targetOperatorId);

		LinkedHashMap<String, Object> deletedOperatorSnapshot = new LinkedHashMap<>(operatorsData);
		scheduleDeleteAfterCommitSideEffects(companyId, mobile, actingOperatorId, deletedOperatorSnapshot);
	}

	private void scheduleDeleteAfterCommitSideEffects(
			long companyId,
			String mobile,
			long actingOperatorId,
			Map<String, Object> deletedOperatorSnapshot) {
		Map<String, Object> syncPayload = new LinkedHashMap<>();
		syncPayload.put("company_id", companyId);
		syncPayload.put("mobile", mobile);
		syncPayload.put("synctype", EMPLOYEE_USER_SYNC_TYPE_DELETE);

		String opType =
				deletedOperatorSnapshot.get("operator_type") != null
						? deletedOperatorSnapshot.get("operator_type").toString()
						: "staff";
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("name", str(deletedOperatorSnapshot.get("username")));
		long deletedOperatorId = toLong(deletedOperatorSnapshot.get("operator_id"));
		boolean subDealer = "dealer".equals(opType) && isDealerSubAccount(deletedOperatorSnapshot);
		long adapayRelId = subDealer ? toLong(deletedOperatorSnapshot.get("dealer_parent_id")) : deletedOperatorId;

		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						employeeUserSyncPublisher.publishAfterCommit(syncPayload);
						if ("dealer".equals(opType)) {
							adapayOperationLogRecordPort.logRecord(
									logParams, actingOperatorId, "delete_operator_dealer", "merchant", actingOperatorId);
							adapayOperationLogRecordPort.logRecord(
									logParams, adapayRelId, "delete_operator_dealer", "dealer", actingOperatorId);
						}
					}
				});
	}

	private Map<String, Object> updateOperatorStaffTransactional(
			Map<String, Object> params, Map<String, Object> filter) {
		long targetOperatorId = ((Number) Objects.requireNonNull(filter.get("operator_id"))).longValue();
		long filterCompanyId = ((Number) Objects.requireNonNull(filter.get("company_id"))).longValue();

		Map<String, Object> old = operatorsQueryService.getInfo(Map.of("operator_id", targetOperatorId));
		if (old == null || old.isEmpty()) {
			throw new ResourceException("请选择要修改的账号");
		}
		long oldCompanyId = toLong(old.get("company_id"));
		if (oldCompanyId != filterCompanyId) {
			throw new ForbiddenException("无权操作该账号");
		}

		Map<String, Object> operatorUpdatePayload = buildOperatorUpdatePayload(params);
		Map<String, Object> updatedRow =
				operatorsCommandService.updateOperator(targetOperatorId, filterCompanyId, operatorUpdatePayload);

		long companyId = filterCompanyId;
		@SuppressWarnings("unchecked")
		List<String> roleIds = (List<String>) params.get("role_id");
		LambdaQueryWrapper<EmployeeRelRoles> del = new LambdaQueryWrapper<>();
		del.eq(EmployeeRelRoles::getCompanyId, companyId)
				.eq(EmployeeRelRoles::getOperatorId, String.valueOf(targetOperatorId));
		employeeRelRolesMapper.delete(del);
		if (roleIds != null && !roleIds.isEmpty()) {
			for (String roleId : roleIds) {
				EmployeeRelRoles rel = new EmployeeRelRoles();
				rel.setCompanyId(companyId);
				rel.setRoleId(roleId);
				rel.setOperatorId(String.valueOf(targetOperatorId));
				employeeRelRolesMapper.insert(rel);
			}
		}

		if ("self_delivery_staff".equals(params.get("operator_type"))) {
			Number feeRaw = (Number) params.get("payment_fee");
			int feeCents = scalePaymentFeeToCents(feeRaw);
			int shopId;
			Object merchantIdParam = params.get("merchant_id");
			if (merchantIdParam != null) {
				shopId = (int) toLong(merchantIdParam);
			} else {
				List<SelfDeliveryStaffAccountListRow> sdRows =
						employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(
								companyId, List.of(targetOperatorId));
				Integer existingShopId =
						(sdRows != null && !sdRows.isEmpty()) ? sdRows.get(0).getShopId() : null;
				if (existingShopId != null) {
					shopId = existingShopId;
				} else {
					Object oldMerchantId = old.get("merchant_id");
					shopId = oldMerchantId != null ? (int) toLong(oldMerchantId) : 0;
				}
			}
			int distributorId = lastDistributorIdFromContext(params);
			long now = System.currentTimeMillis() / 1000L;
			employeeSelfDeliveryStaffMapper.updateStaffRowByOperatorId(
					companyId,
					targetOperatorId,
					distributorId,
					shopId,
					str(params.get("staff_attribute")),
					str(params.get("staff_no")),
					str(params.get("staff_type")),
					str(params.get("payment_method")),
					feeCents,
					now);
		}

		String blackListOpType =
				old.get("operator_type") != null ? old.get("operator_type").toString() : "staff";
		scheduleUpdateAfterCommitSideEffects(params, updatedRow, targetOperatorId, blackListOpType);

		return updatedRow;
	}

	private void scheduleUpdateAfterCommitSideEffects(
			Map<String, Object> params,
			Map<String, Object> updatedRow,
			long targetOperatorId,
			String operatorTypeForBlacklist) {
		long companyId = toLong(params.get("company_id"));
		Map<String, Object> syncPayload = new LinkedHashMap<>();
		syncPayload.put("company_id", companyId);
		syncPayload.put("login_name", updatedRow.get("login_name"));
		syncPayload.put("mobile", updatedRow.get("mobile"));
		syncPayload.put("user_name", updatedRow.get("username"));
		syncPayload.put("password", updatedRow.get("password"));
		syncPayload.put("synctype", EMPLOYEE_USER_SYNC_TYPE_UPDATE);

		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						companysActivationService.setBlackTokenCache(targetOperatorId, operatorTypeForBlacklist);
						employeeUserSyncPublisher.publishAfterCommit(syncPayload);
					}
				});
	}

	private static Map<String, Object> buildOperatorUpdatePayload(Map<String, Object> params) {
		Map<String, Object> payload = new LinkedHashMap<>();
		if (params.containsKey("username")) {
			Object u = params.get("username");
			if (u != null) {
				String us = u.toString();
				if (!us.contains("*")) {
					payload.put("username", us.trim());
				}
			}
		}
		if (params.containsKey("mobile")) {
			Object m = params.get("mobile");
			if (m != null) {
				String ms = m.toString();
				if (!ms.contains("*")) {
					payload.put("mobile", ms.trim());
				}
			}
		}
		if (params.containsKey("head_portrait") && params.get("head_portrait") != null) {
			payload.put("head_portrait", params.get("head_portrait").toString());
		}
		if (params.containsKey("password")) {
			Object pw = params.get("password");
			if (pw != null && !pw.toString().isBlank()) {
				payload.put("password", pw.toString());
			}
		}
		if (params.containsKey("distributor_ids")) {
			payload.put("distributor_ids", params.get("distributor_ids"));
		}
		if (params.containsKey("shop_ids")) {
			payload.put("shop_ids", params.get("shop_ids"));
		}
		if (!params.containsKey("regionauth_id") || params.get("regionauth_id") == null) {
			payload.put("regionauth_id", 0L);
		} else {
			payload.put("regionauth_id", toLongObjectWithDefault(params.get("regionauth_id"), 0L));
		}
		if (params.containsKey("login_name")) {
			payload.put("login_name", str(params.get("login_name")));
		}
		if (params.containsKey("is_distributor_main")) {
			payload.put("is_distributor_main", params.get("is_distributor_main"));
		}
		// Passed into OperatorsCommandService.updateOperator for mobile/login uniqueness only (PHP parity); stripped before SQL.
		if (params.containsKey("operator_type") && params.get("operator_type") != null) {
			String ot = str(params.get("operator_type")).trim();
			if (!ot.isEmpty()) {
				payload.put("operator_type", ot);
			}
		}
		return payload;
	}

	private static Long toLongObjectWithDefault(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private Map<String, Object> createOperatorStaffTransactional(Map<String, Object> params) {
		Map<String, Object> operatorParams = buildOperatorInsertParams(params);
		Map<String, Object> createdRow;
		try {
			createdRow = operatorsCommandService.createOperator(operatorParams);
		} catch (ResourceException | BadRequestException | UnauthorizedException | ForbiddenException e) {
			throw e;
		} catch (RuntimeException e) {
			log.error("createOperator failed companyId={}", params.get("company_id"), e);
			throw e;
		}

		@SuppressWarnings("unchecked")
		List<String> roleIds = (List<String>) params.get("role_id");
		if (roleIds != null && !roleIds.isEmpty()) {
			long companyId = toLong(params.get("company_id"));
			long newOpId = toLong(createdRow.get("operator_id"));
			for (String roleId : roleIds) {
				EmployeeRelRoles rel = new EmployeeRelRoles();
				rel.setCompanyId(companyId);
				rel.setRoleId(roleId);
				rel.setOperatorId(String.valueOf(newOpId));
				employeeRelRolesMapper.insert(rel);
			}
		}

		if ("self_delivery_staff".equals(params.get("operator_type"))) {
			long companyId = toLong(params.get("company_id"));
			long newOpId = toLong(createdRow.get("operator_id"));
			Number feeRaw = (Number) params.get("payment_fee");
			int feeCents = scalePaymentFeeToCents(feeRaw);
			Object merchantIdParam = params.get("merchant_id");
			int shopId = merchantIdParam != null ? (int) toLong(merchantIdParam) : 0;
			int distributorId = lastDistributorIdFromContext(params);
			long now = System.currentTimeMillis() / 1000L;
			employeeSelfDeliveryStaffMapper.insertStaffRow(
					companyId,
					newOpId,
					distributorId,
					shopId,
					str(params.get("staff_attribute")),
					str(params.get("staff_no")),
					str(params.get("staff_type")),
					str(params.get("payment_method")),
					feeCents,
					now,
					now);
		}

		scheduleAfterCommitSideEffects(params, createdRow);

		return createdRow;
	}

	private void scheduleAfterCommitSideEffects(Map<String, Object> params, Map<String, Object> createdRow) {
		long companyId = toLong(params.get("company_id"));
		long jwtOperatorId = toLong(params.get("operator_id"));
		Map<String, Object> syncPayload = new LinkedHashMap<>();
		syncPayload.put("company_id", companyId);
		syncPayload.put("login_name", createdRow.get("login_name"));
		syncPayload.put("mobile", createdRow.get("mobile"));
		syncPayload.put("user_name", createdRow.get("username"));
		syncPayload.put("password", createdRow.get("password"));
		syncPayload.put("synctype", EMPLOYEE_USER_SYNC_TYPE_ADD);

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("name", str(params.get("username")));

		long newOperatorId = toLong(createdRow.get("operator_id"));
		boolean subDealer = "dealer".equals(params.get("operator_type")) && isDealerSubAccount(createdRow);
		long adapayRelId = subDealer ? toLong(createdRow.get("dealer_parent_id")) : newOperatorId;

		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						employeeUserSyncPublisher.publishAfterCommit(syncPayload);
						if ("dealer".equals(params.get("operator_type"))) {
							adapayOperationLogRecordPort.logRecord(
									logParams, jwtOperatorId, "create_operator_dealer", "merchant", jwtOperatorId);
							adapayOperationLogRecordPort.logRecord(
									logParams, adapayRelId, "create_operator_dealer", "dealer", jwtOperatorId);
						}
					}
				});
	}

	private static boolean isDealerSubAccount(Map<String, Object> createdRow) {
		Object v = createdRow.get("is_dealer_main");
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof Number n) {
			return n.intValue() == 0;
		}
		return "0".equals(String.valueOf(v));
	}

	private static int lastDistributorIdFromContext(Map<String, Object> params) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> ids = (List<Map<String, Object>>) params.get("distributor_ids");
		if (ids == null || ids.isEmpty()) {
			return 0;
		}
		Object d0 = ids.get(ids.size() - 1).get("distributor_id");
		if (d0 == null) {
			return 0;
		}
		if (d0 instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(d0.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int scalePaymentFeeToCents(Number feeRaw) {
		if (feeRaw == null) {
			return 0;
		}
		BigDecimal v =
				feeRaw instanceof BigDecimal b ? b : BigDecimal.valueOf(feeRaw.doubleValue());
		return v.multiply(BigDecimal.valueOf(100)).intValue();
	}

	private static Map<String, Object> buildOperatorInsertParams(Map<String, Object> params) {
		Map<String, Object> o = new LinkedHashMap<>();
		o.put("operator_type", params.get("operator_type"));
		o.put("login_name", params.get("login_name"));
		o.put("mobile", params.get("mobile"));
		o.put("company_id", params.get("company_id"));
		o.put("username", params.get("username"));
		o.put("head_portrait", params.get("head_portrait"));
		o.put("distributor_ids", params.get("distributor_ids"));
		o.put("regionauth_id", params.get("regionauth_id") != null ? params.get("regionauth_id") : 0L);
		o.put("shop_ids", params.get("shop_ids") != null ? params.get("shop_ids") : List.of());
		o.put("password", params.get("password"));
		o.put("dealer_parent_id", params.get("dealer_parent_id") != null ? params.get("dealer_parent_id") : "");
		o.put("is_dealer_main", params.get("is_dealer_main") != null ? params.get("is_dealer_main") : false);
		o.put(
				"is_distributor_main",
				params.get("is_distributor_main") != null ? params.get("is_distributor_main") : false);
		o.put("merchant_id", params.get("merchant_id") != null ? params.get("merchant_id") : 0L);
		Object contact = params.get("contact");
		if (contact != null && !contact.toString().isEmpty()) {
			o.put("contact", contact);
		}
		o.put("eid", "");
		o.put("passport_uid", "");
		return o;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
