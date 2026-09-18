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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeFrontCheckQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeFrontCheckListRow;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeFrontEmailEnterpriseRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeCheckService {

	private final EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper;
	private final EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	private final DistributorSelfMetaService distributorSelfMetaService;

	public EmployeeCheckService(
			EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper,
			EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorEmployeeListLookupService distributorEmployeeListLookupService,
			DistributorSelfMetaService distributorSelfMetaService) {
		this.employeeFrontCheckQueryMapper = employeeFrontCheckQueryMapper;
		this.emailVcodeRedisService = emailVcodeRedisService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorEmployeeListLookupService = distributorEmployeeListLookupService;
		this.distributorSelfMetaService = distributorSelfMetaService;
	}

	public Map<String, Object> doEmployeeCheck(long companyId, Map<String, Object> params) {
		Object rawAuth = params.get("auth_type");
		String authTypeTrimmed = rawAuth == null ? "" : rawAuth.toString().trim();
		String authTypeNorm = authTypeTrimmed.toLowerCase(Locale.ROOT);

		if ("email".equals(authTypeNorm)) {
			return doEmailBranch(companyId, params);
		}
		return doNonEmailBranch(companyId, params, authTypeNorm);
	}

	private Map<String, Object> doEmailBranch(long companyId, Map<String, Object> params) {
		long activityId = parseLongParam(params.get("activity_id"), 0L);
		List<Long> enterpriseIdsForSuffix = null;

		if (activityId > 0) {
			enterpriseIdsForSuffix = loadActivityEnterpriseIdsOrThrow(companyId, activityId);
		} else {
			long enterpriseId = parseLongParam(params.get("enterprise_id"), 0L);
			if (enterpriseId > 0) {
				enterpriseIdsForSuffix = List.of(enterpriseId);
			}
		}

		String email = stringOrEmpty(params.get("email")).trim();
		if (!StringUtils.hasText(email)) {
			throw new ResourceException("邮箱必填");
		}
		String vcode = stringOrEmpty(params.get("vcode")).trim();
		if (!StringUtils.hasText(vcode)) {
			throw new ResourceException("验证码必填");
		}
		int at = email.indexOf('@');
		if (at < 0) {
			throw new BadRequestException("请填写正确的邮箱");
		}
		String suffix = email.substring(at);

		if (!emailVcodeRedisService.verifyAndConsume(email, vcode)) {
			throw new ResourceException("验证码错误");
		}

		EmployeeFrontEmailEnterpriseRow row =
				employeeFrontCheckQueryMapper.selectFirstEnterpriseByEmailSuffix(
						companyId, suffix, enterpriseIdsForSuffix);
		if (row == null || row.getEnterpriseId() == null) {
			throw new ResourceException("未关联企业信息，请确认后再操作");
		}

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("enterprise_id", row.getEnterpriseId());
		item.put("enterprise_name", row.getName());
		item.put("enterprise_sn", row.getEnterpriseSn());
		item.put("auth_type", row.getAuthType());
		item.put("distributor_id", row.getDistributorId() == null ? 0 : row.getDistributorId());
		item.put("operator_id", row.getOperatorId() == null ? 0 : row.getOperatorId());

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", 1);
		result.put("list", List.of(item));
		return result;
	}

	private Map<String, Object> doNonEmailBranch(long companyId, Map<String, Object> params, String authTypeNorm) {
		ActivityEnterpriseContext ctx = resolveActivityEnterpriseContext(companyId, params, authTypeNorm);

		String mobileEncrypted = null;
		String account = null;
		String authCode = null;

		switch (authTypeNorm) {
			case "qr_code", "mobile" -> {
				String mobile = stringOrEmpty(params.get("mobile")).trim();
				if (!StringUtils.hasText(mobile)) {
					throw new ResourceException("手机号必填");
				}
				mobileEncrypted = sensitiveFieldEncryptor.encrypt(mobile);
			}
			case "account" -> {
				account = stringOrEmpty(params.get("account")).trim();
				if (!StringUtils.hasText(account)) {
					throw new ResourceException("账号必填");
				}
				authCode = stringOrEmpty(params.get("auth_code")).trim();
				if (!StringUtils.hasText(authCode)) {
					throw new ResourceException("密码必填");
				}
			}
			default -> throw new ResourceException("请选择正确的验证方式");
		}

		long total = employeeFrontCheckQueryMapper.countEmployeesWithRel(
				companyId,
				authTypeNorm,
				ctx.enterpriseIds(),
				ctx.singleEnterpriseId(),
				ctx.distributorId(),
				mobileEncrypted,
				account,
				authCode,
				ctx.requireUnboundUser());
		if (total == 0) {
			throw new ResourceException("未关联企业信息，请确认后再操作");
		}

		List<EmployeeFrontCheckListRow> rows = employeeFrontCheckQueryMapper.selectEmployeesWithRel(
				companyId,
				authTypeNorm,
				ctx.enterpriseIds(),
				ctx.singleEnterpriseId(),
				ctx.distributorId(),
				mobileEncrypted,
				account,
				authCode,
				ctx.requireUnboundUser());

		List<Map<String, Object>> list = new ArrayList<>();
		for (EmployeeFrontCheckListRow row : rows) {
			list.add(rowToSnakeMap(row));
		}
		for (Map<String, Object> rowMap : list) {
			Object n = rowMap.get("name");
			Object m = rowMap.get("mobile");
			if (n instanceof String ns) {
				rowMap.put("name", sensitiveFieldEncryptor.decrypt(ns));
			}
			if (m instanceof String ms) {
				rowMap.put("mobile", sensitiveFieldEncryptor.decrypt(ms));
			}
		}
		if (total > 0) {
			attachDistributorNames(companyId, list);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private void attachDistributorNames(long companyId, List<Map<String, Object>> list) {
		Set<Integer> distIds = new LinkedHashSet<>();
		for (Map<String, Object> rowMap : list) {
			Object d = rowMap.get("distributor_id");
			if (d instanceof Number num) {
				int id = num.intValue();
				if (id >= 0) {
					distIds.add(id);
				}
			}
		}
		int listSize = list.size();
		Map<Integer, String> idToName =
				distributorEmployeeListLookupService.distributorIdToName(companyId, distIds, listSize);
		Map<String, Object> selfRow = distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId);
		Object selfNameObj = selfRow.get("name");
		String selfName = selfNameObj == null ? "" : selfNameObj.toString();
		for (Map<String, Object> rowMap : list) {
			Object d = rowMap.get("distributor_id");
			int id = d instanceof Number ? ((Number) d).intValue() : -1;
			String distributorName;
			if (id == 0) {
				distributorName = selfName;
			} else {
				distributorName = idToName.getOrDefault(id, "");
			}
			rowMap.put("distributor_name", distributorName);
		}
	}

	/**
	 * 与 PHP {@code EmployeesService::__checkEmployee} 对齐：
	 * <ul>
	 *   <li>{@code activity_id > 0} 或 {@code enterprise_id > 0}：不限定 {@code user_id=0}，否则已绑定会员的员工行无法命中
	 *   <li>{@code mobile}/{@code qr_code}：始终不限定 {@code user_id=0}
	 *   <li>仅 {@code account} 且未指定活动/企业时保留 {@code user_id=0}
	 * </ul>
	 */
	private ActivityEnterpriseContext resolveActivityEnterpriseContext(
			long companyId, Map<String, Object> params, String authTypeNorm) {
		boolean mobileOrQr = "mobile".equals(authTypeNorm) || "qr_code".equals(authTypeNorm);
		long activityId = parseLongParam(params.get("activity_id"), 0L);
		if (activityId > 0) {
			List<Long> ids = loadActivityEnterpriseIdsOrThrow(companyId, activityId);
			return new ActivityEnterpriseContext(ids, null, null, false);
		}
		Integer distributorId = parsePositiveIntOrNull(params.get("distributor_id"));
		long enterpriseId = parseLongParam(params.get("enterprise_id"), 0L);
		Long singleEnt = enterpriseId > 0 ? enterpriseId : null;
		boolean requireUnbound = !mobileOrQr && enterpriseId <= 0;
		return new ActivityEnterpriseContext(null, singleEnt, distributorId, requireUnbound);
	}

	private List<Long> loadActivityEnterpriseIdsOrThrow(long companyId, long activityId) {
		List<Long> ids = employeeFrontCheckQueryMapper.selectEnterpriseIdsByActivity(companyId, activityId);
		if (ids == null || ids.isEmpty()) {
			throw new ResourceException("未关联企业信息，请确认后再操作");
		}
		return ids;
	}

	private static Map<String, Object> rowToSnakeMap(EmployeeFrontCheckListRow row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("operator_id", row.getOperatorId());
		m.put("name", row.getName());
		m.put("mobile", row.getMobile());
		m.put("account", row.getAccount());
		m.put("email", row.getEmail());
		m.put("auth_code", row.getAuthCode());
		m.put("enterprise_id", row.getEnterpriseId());
		m.put("user_id", row.getUserId());
		m.put("member_mobile", row.getMemberMobile());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("disabled", row.getDisabled());
		m.put("enterprise_sn", row.getEnterpriseSn());
		m.put("enterprise_name", row.getEnterpriseName());
		m.put("auth_type", row.getAuthType());
		return m;
	}

	private static long parseLongParam(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static Integer parsePositiveIntOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v > 0 ? v : null;
		}
		try {
			int v = Integer.parseInt(raw.toString().trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringOrEmpty(Object o) {
		return o == null ? "" : o.toString();
	}

	private record ActivityEnterpriseContext(
			List<Long> enterpriseIds, Long singleEnterpriseId, Integer distributorId, boolean requireUnboundUser) {}
}
