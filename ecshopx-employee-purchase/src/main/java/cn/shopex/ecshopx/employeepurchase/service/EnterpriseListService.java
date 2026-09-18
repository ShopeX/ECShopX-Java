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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.domain.EnterpriseEmailBox;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EnterpriseListService {

	private final EnterprisesMapper enterprisesMapper;
	private final EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;
	private final DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final StringRedisTemplate companysRedisTemplate;

	public EnterpriseListService(
			EnterprisesMapper enterprisesMapper,
			EnterpriseEmailBoxMapper enterpriseEmailBoxMapper,
			DistributorEmployeeListLookupService distributorEmployeeListLookupService,
			DistributorSelfMetaService distributorSelfMetaService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.enterprisesMapper = enterprisesMapper;
		this.enterpriseEmailBoxMapper = enterpriseEmailBoxMapper;
		this.distributorEmployeeListLookupService = distributorEmployeeListLookupService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	/**
	 * @param enterpriseIdFilter {@code null} = no id filter; empty = force empty result; otherwise {@code IN}
	 */
	public Map<String, Object> getEnterprisesList(
			int page,
			int pageSize,
			String name,
			String enterpriseSn,
			String authType,
			String distributorIdRaw,
			boolean hasDisabled,
			String disabledRaw,
			List<Long> enterpriseIdFilter,
			boolean hasEmployeeCheckEnabled,
			String isEmployeeCheckEnabledRaw,
			Map<String, Object> operatorJwt) {
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize < 1 || pageSize > 100) {
			throw new BadRequestException("每页显示数量最大100");
		}
		long companyId = readCompanyId(operatorJwt);
		boolean distributorOperator = Objects.equals("distributor", stringOrEmpty(operatorJwt.get("operator_type")));

		LambdaQueryWrapper<Enterprises> w = new LambdaQueryWrapper<>();
		w.eq(Enterprises::getCompanyId, companyId);
		if (distributorOperator) {
			w.eq(Enterprises::getDistributorId, (int) readDistributorIdForOperator(operatorJwt, companyId));
		} else if (StringUtils.hasText(distributorIdRaw)) {
			int qDid;
			try {
				qDid = Integer.parseInt(distributorIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("distributor_id 无效");
			}
			w.eq(Enterprises::getDistributorId, qDid);
		}
		if (StringUtils.hasText(name)) {
			w.like(Enterprises::getName, "%" + name.trim() + "%");
		}
		if (hasDisabled) {
			w.eq(Enterprises::getDisabled, parseDisabledToBoolean(disabledRaw));
		}
		if (StringUtils.hasText(enterpriseSn)) {
			w.like(Enterprises::getEnterpriseSn, "%" + enterpriseSn.trim() + "%");
		}
		if (enterpriseIdFilter != null) {
			if (enterpriseIdFilter.isEmpty()) {
				w.apply("1 = 0");
			} else {
				w.in(Enterprises::getId, enterpriseIdFilter);
			}
		}
		if (StringUtils.hasText(authType)) {
			w.eq(Enterprises::getAuthType, authType.trim());
		}
		if (hasEmployeeCheckEnabled) {
			w.eq(Enterprises::getIsEmployeeCheckEnabled, "true".equals(isEmployeeCheckEnabledRaw));
		}
		w.orderByAsc(Enterprises::getSort).orderByDesc(Enterprises::getCreated);

		Page<Enterprises> p = new Page<>(page, pageSize);
		enterprisesMapper.selectPage(p, w);
		long total = p.getTotal();
		List<Enterprises> records = p.getRecords();
		if (total == 0 || records.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", total);
			empty.put("list", List.of());
			return empty;
		}

		List<Long> enterpriseIds =
				records.stream().map(Enterprises::getId).filter(Objects::nonNull).distinct().toList();
		Map<Long, EnterpriseEmailBox> emailMap = new LinkedHashMap<>();
		if (!enterpriseIds.isEmpty()) {
			List<EnterpriseEmailBox> boxes =
					enterpriseEmailBoxMapper.selectList(
							new LambdaQueryWrapper<EnterpriseEmailBox>()
									.eq(EnterpriseEmailBox::getCompanyId, companyId)
									.in(EnterpriseEmailBox::getEnterpriseId, enterpriseIds));
			emailMap =
					boxes.stream()
							.collect(
									Collectors.toMap(EnterpriseEmailBox::getEnterpriseId, Function.identity(), (a, b) -> a));
		}

		Set<Integer> storeIds = new LinkedHashSet<>();
		for (Enterprises row : records) {
			Integer did = row.getDistributorId();
			if (did != null && did >= 0) {
				storeIds.add(did);
			}
		}
		Map<Integer, String> idToName = Map.of();
		String selfName = "";
		if (!storeIds.isEmpty()) {
			idToName = distributorEmployeeListLookupService.distributorIdToName(companyId, storeIds, pageSize);
			Map<String, Object> selfRow = distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId);
			Object selfNameObj = selfRow.get("name");
			selfName = selfNameObj == null ? "" : selfNameObj.toString();
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (Enterprises row : records) {
			list.add(toListRowMap(row, emailMap, idToName, selfName));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private static Map<String, Object> toListRowMap(
			Enterprises row,
			Map<Long, EnterpriseEmailBox> emailMap,
			Map<Integer, String> idToName,
			String selfName) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("operator_id", row.getOperatorId());
		m.put("qr_code_bg_image", row.getQrCodeBgImage());
		m.put("name", row.getName());
		m.put("enterprise_sn", row.getEnterpriseSn());
		m.put("logo", row.getLogo());
		m.put("auth_type", row.getAuthType());
		m.put("disabled", Boolean.TRUE.equals(row.getDisabled()));
		m.put("sort", row.getSort());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("is_employee_check_enabled", Boolean.TRUE.equals(row.getIsEmployeeCheckEnabled()) ? "true" : "false");

		boolean emailAuth = "email".equals(row.getAuthType());
		EnterpriseEmailBox box = emailAuth && row.getId() != null ? emailMap.get(row.getId()) : null;
		m.put("relay_host", box != null && box.getRelayHost() != null ? box.getRelayHost() : "");
		m.put("smtp_port", box != null && box.getSmtpPort() != null ? box.getSmtpPort() : "");
		m.put("email_user", box != null && box.getUser() != null ? box.getUser() : "");
		m.put("email_password", box != null && box.getPassword() != null ? box.getPassword() : "");
		m.put("email_suffix", box != null && box.getSuffix() != null ? box.getSuffix() : "");

		int distId = row.getDistributorId() != null ? row.getDistributorId() : -1;
		String distributorName;
		if (distId == 0) {
			distributorName = selfName;
		} else if (idToName.isEmpty()) {
			distributorName = "";
		} else {
			distributorName = idToName.getOrDefault(distId, "");
		}
		m.put("distributor_name", distributorName);
		return m;
	}

	private static boolean parseDisabledToBoolean(String disabledRaw) {
		try {
			String s = disabledRaw == null ? "" : disabledRaw.trim();
			if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
				return true;
			}
			if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
				return false;
			}
			int v = Integer.parseInt(s);
			return v != 0;
		} catch (NumberFormatException e) {
			throw new BadRequestException("disabled 无效");
		}
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
