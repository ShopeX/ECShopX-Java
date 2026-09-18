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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeAdminListQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeAdminListRow;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import cn.shopex.ecshopx.employeepurchase.web.EmployeeDatapassBlockSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeListService {

	private final EmployeeAdminListQueryMapper employeeAdminListQueryMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final StringRedisTemplate companysRedisTemplate;

	public EmployeeListService(
			EmployeeAdminListQueryMapper employeeAdminListQueryMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorEmployeeListLookupService distributorEmployeeListLookupService,
			DistributorSelfMetaService distributorSelfMetaService,
				@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.employeeAdminListQueryMapper = employeeAdminListQueryMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorEmployeeListLookupService = distributorEmployeeListLookupService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public EmployeeAdminExportQuery resolveAdminExportQuery(String mobile, String account, String email,
			String memberMobile, String enterpriseIdRaw, String distributorIdRaw, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		String operatorType = stringOrEmpty(operatorJwt.get("operator_type"));
		Integer distributorIdFilter = null;
		if (Objects.equals("distributor", operatorType)) {
			distributorIdFilter = (int) readDistributorIdForOperator(operatorJwt, companyId);
		} else if (StringUtils.hasText(distributorIdRaw)) {
			try {
				distributorIdFilter = Integer.parseInt(distributorIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("distributor_id 无效");
			}
		}
		String mobileF = StringUtils.hasText(mobile) ? mobile : null;
		String accountF = StringUtils.hasText(account) ? account : null;
		String emailF = StringUtils.hasText(email) ? email : null;
		String memberMobileF = StringUtils.hasText(memberMobile) ? memberMobile : null;
		Long enterpriseId = null;
		if (StringUtils.hasText(enterpriseIdRaw)) {
			try {
				enterpriseId = Long.parseLong(enterpriseIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("enterprise_id 无效");
			}
		}
		return new EmployeeAdminExportQuery(companyId, distributorIdFilter, mobileF, accountF, emailF, memberMobileF,
				enterpriseId);
	}

	public long countForExport(EmployeeAdminExportQuery q) {
		return employeeAdminListQueryMapper.countList(q.companyId(), q.distributorId(), q.mobile(), q.account(),
				q.email(), q.memberMobile(), q.enterpriseId());
	}

	public Map<String, Object> getList(
			int page,
			int pageSize,
			String mobile,
			String account,
			String email,
			String memberMobile,
			String enterpriseIdRaw,
			String distributorIdRaw,
			Map<String, Object> operatorJwt,
			HttpServletRequest request) {
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize < 1 || pageSize > 100) {
			throw new BadRequestException("每页显示数量最大100");
		}
		EmployeeAdminExportQuery q = resolveAdminExportQuery(mobile, account, email, memberMobile, enterpriseIdRaw,
				distributorIdRaw, operatorJwt);
		long companyId = q.companyId();
		Integer distributorIdFilter = q.distributorId();
		String mobileF = q.mobile();
		String accountF = q.account();
		String emailF = q.email();
		String memberMobileF = q.memberMobile();
		Long enterpriseId = q.enterpriseId();
		long total = employeeAdminListQueryMapper.countList(
				companyId, distributorIdFilter, mobileF, accountF, emailF, memberMobileF, enterpriseId);
		long offset = (long) (page - 1) * pageSize;
		List<EmployeeAdminListRow> rows =
				total == 0
						? List.of()
						: employeeAdminListQueryMapper.selectListPage(
								companyId,
								distributorIdFilter,
								mobileF,
								accountF,
								emailF,
								memberMobileF,
								enterpriseId,
								offset,
								pageSize);
		List<Map<String, Object>> list = new ArrayList<>();
		for (EmployeeAdminListRow row : rows) {
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
			Map<Integer, String> idToName =
					distributorEmployeeListLookupService.distributorIdToName(companyId, distIds, pageSize);
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
		Object echo = EmployeeDatapassBlockSupport.resolveEchoValue(request);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		result.put("datapass_block", echo);
		if (EmployeeDatapassBlockSupport.shouldMask(echo) && !list.isEmpty()) {
			for (Map<String, Object> rowMap : list) {
				Object mob = rowMap.get("mobile");
				if (mob != null) {
					rowMap.put("mobile", DataMasking.maskMobile(mob.toString()));
				}
				Object mm = rowMap.get("member_mobile");
				if (mm != null) {
					rowMap.put("member_mobile", DataMasking.maskMobile(mm.toString()));
				}
				Object nm = rowMap.get("name");
				if (nm != null) {
					rowMap.put("name", DataMasking.maskTruename(nm.toString()));
				}
			}
		}
		return result;
	}

	static Map<String, Object> rowToSnakeMap(EmployeeAdminListRow row) {
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

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLong(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private long readDistributorIdForOperator(Map<String, Object> operatorJwt, long companyId) {
		Object raw = operatorJwt.get("distributor_id");
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : 0L;
		}
		try {
			long v = Long.parseLong(raw.toString().trim());
			return v > 0 ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOrEmpty(Object o) {
		return o == null ? "" : o.toString();
	}
}
