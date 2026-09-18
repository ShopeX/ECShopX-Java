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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.EnterpriseEmailBox;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontEnterpriseListService {

	private final EnterprisesMapper enterprisesMapper;
	private final EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final DistributorInfoResolveService distributorInfoResolveService;

	public FrontEnterpriseListService(
			EnterprisesMapper enterprisesMapper,
			EnterpriseEmailBoxMapper enterpriseEmailBoxMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			ActivityEnterprisesMapper activityEnterprisesMapper,
			DistributorEmployeeListLookupService distributorEmployeeListLookupService,
			DistributorSelfMetaService distributorSelfMetaService,
			DistributorInfoResolveService distributorInfoResolveService) {
		this.enterprisesMapper = enterprisesMapper;
		this.enterpriseEmailBoxMapper = enterpriseEmailBoxMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.distributorEmployeeListLookupService = distributorEmployeeListLookupService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.distributorInfoResolveService = distributorInfoResolveService;
	}

	public Map<String, Object> listForWxappEnterprises(
			long companyId, Integer page, Integer pageSize, String enterpriseSn, Long enterpriseId) {
		int p = page == null || page <= 0 ? 1 : page;
		int ps = pageSize == null || pageSize <= 0 ? 20 : pageSize;

		LambdaQueryWrapper<Enterprises> w = new LambdaQueryWrapper<>();
		w.eq(Enterprises::getCompanyId, companyId);
		w.eq(Enterprises::getDisabled, false);
		if (StringUtils.hasText(enterpriseSn)) {
			w.eq(Enterprises::getEnterpriseSn, enterpriseSn.trim());
		}
		if (enterpriseId != null && enterpriseId > 0L) {
			w.eq(Enterprises::getId, enterpriseId);
		}
		w.orderByAsc(Enterprises::getSort).orderByDesc(Enterprises::getCreated);

		Page<Enterprises> pageQuery = new Page<>(p, ps);
		enterprisesMapper.selectPage(pageQuery, w);
		long total = pageQuery.getTotal();
		List<Enterprises> records = pageQuery.getRecords();
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
			idToName = distributorEmployeeListLookupService.distributorIdToName(companyId, storeIds, ps);
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

	public List<Map<String, Object>> listUserEnterprises(
			long companyId,
			long userId,
			String disabled,
			long distributorId,
			long activityId) {
		LambdaQueryWrapper<Employees> employeeWrapper = new LambdaQueryWrapper<>();
		employeeWrapper.eq(Employees::getCompanyId, companyId).eq(Employees::getUserId, userId);
		LambdaQueryWrapper<Relatives> relativeWrapper = new LambdaQueryWrapper<>();
		relativeWrapper.eq(Relatives::getCompanyId, companyId).eq(Relatives::getUserId, userId);

		applyEmployeesRelativesDisabledFilter(disabled, employeeWrapper, relativeWrapper);

		if (distributorId > 0) {
			int did = (int) distributorId;
			employeeWrapper.eq(Employees::getDistributorId, did);
			relativeWrapper.eq(Relatives::getDistributorId, did);
		}

		if (activityId > 0) {
			List<ActivityEnterprises> actRows =
					activityEnterprisesMapper.selectList(
							Wrappers.<ActivityEnterprises>lambdaQuery()
									.eq(ActivityEnterprises::getCompanyId, companyId)
									.eq(ActivityEnterprises::getActivityId, activityId));
			if (actRows.isEmpty()) {
				return List.of();
			}
			List<Long> actEnterpriseIds =
					actRows.stream()
							.map(ActivityEnterprises::getEnterpriseId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
			if (actEnterpriseIds.isEmpty()) {
				return List.of();
			}
			employeeWrapper.in(Employees::getEnterpriseId, actEnterpriseIds);
			relativeWrapper.in(Relatives::getEnterpriseId, actEnterpriseIds);
		}

		List<Employees> employees = employeesMapper.selectList(employeeWrapper);
		List<Relatives> relatives = relativesMapper.selectList(relativeWrapper);

		Set<Long> enterpriseIdSet = new LinkedHashSet<>();
		for (Employees e : employees) {
			if (e.getEnterpriseId() != null) {
				enterpriseIdSet.add(e.getEnterpriseId());
			}
		}
		for (Relatives r : relatives) {
			if (r.getEnterpriseId() != null) {
				enterpriseIdSet.add(r.getEnterpriseId());
			}
		}
		if (enterpriseIdSet.isEmpty()) {
			return List.of();
		}

		List<Enterprises> enterpriseRows =
				enterprisesMapper.selectList(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getDisabled, false)
								.in(Enterprises::getId, enterpriseIdSet));
		Map<Long, Enterprises> enterpriseById =
				enterpriseRows.stream()
						.filter(er -> er.getId() != null)
						.collect(Collectors.toMap(Enterprises::getId, Function.identity(), (a, b) -> a));

		LinkedHashMap<Long, Map<String, Object>> deduped = new LinkedHashMap<>();

		for (Employees e : employees) {
			Long eid = e.getEnterpriseId();
			if (eid == null) {
				continue;
			}
			Enterprises enterprise = enterpriseById.get(eid);
			if (enterprise == null) {
				continue;
			}
			String authType = enterprise.getAuthType();
			String effectiveAuth = "qr_code".equals(authType) ? "mobile" : authType;
			String loginAccount =
					switch (effectiveAuth != null ? effectiveAuth : "mobile") {
						case "email" -> e.getEmail();
						case "account" -> e.getAccount();
						default -> e.getMobile();
					};
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("company_id", enterprise.getCompanyId());
			row.put("name", enterprise.getName());
			row.put("enterprise_id", eid);
			row.put("enterprise_sn", enterprise.getEnterpriseSn());
			row.put("logo", enterprise.getLogo());
			row.put("login_account", loginAccount);
			row.put("disabled", Boolean.TRUE.equals(e.getDisabled()) ? 1 : 0);
			row.put("is_employee", 1);
			row.put("is_relative", 0);
			deduped.put(eid, row);
		}

		for (Relatives r : relatives) {
			Long eid = r.getEnterpriseId();
			if (eid == null) {
				continue;
			}
			Enterprises enterprise = enterpriseById.get(eid);
			if (enterprise == null) {
				continue;
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("company_id", enterprise.getCompanyId());
			row.put("name", enterprise.getName());
			row.put("enterprise_id", eid);
			row.put("enterprise_sn", enterprise.getEnterpriseSn());
			row.put("logo", enterprise.getLogo());
			row.put("login_account", r.getMemberMobile());
			row.put("disabled", Boolean.TRUE.equals(r.getDisabled()) ? 1 : 0);
			row.put("is_employee", 0);
			row.put("is_relative", 1);
			deduped.put(eid, row);
		}

		return new ArrayList<>(deduped.values());
	}

	private static void applyEmployeesRelativesDisabledFilter(
			String disabled,
			LambdaQueryWrapper<Employees> employeesWrapper,
			LambdaQueryWrapper<Relatives> relativesWrapper) {
		if (disabled == null) {
			return;
		}
		String t = disabled.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return;
		}
		try {
			int v = Integer.parseInt(t);
			if (v == 0 || v == 1) {
				boolean eqTrue = v == 1;
				employeesWrapper.eq(Employees::getDisabled, eqTrue);
				relativesWrapper.eq(Relatives::getDisabled, eqTrue);
			}
		} catch (NumberFormatException ignored) {
			// omit disabled filter for non-integer query values
		}
	}

	public Map<String, Object> getEnterpriseDistributorForUser(
			long companyId, long enterpriseId, String requestLang) {
		Enterprises enterprise =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getId, enterpriseId));
		if (enterprise == null) {
			throw new ResourceException("获取企业失败");
		}
		if (enterprise.getCompanyId() != null && !enterprise.getCompanyId().equals(companyId)) {
			throw new ResourceException("获取企业失败");
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		Integer did = enterprise.getDistributorId();
		if (did == null || did == 0) {
			out.put("distributor_id", 0L);
			out.put("distributor_name", "");
			return out;
		}

		long requested = did.longValue();
		Optional<Map<String, Object>> resolved =
				distributorInfoResolveService.resolveStoreDetail(companyId, requested, requestLang);
		if (resolved.isPresent()) {
			Map<String, Object> row = resolved.get();
			long displayId = parseDistributorIdFromRow(row, requested);
			Object nameObj = row.get("name");
			String distributorName = nameObj == null ? "" : nameObj.toString();
			out.put("distributor_id", displayId);
			out.put("distributor_name", distributorName);
			return out;
		}

		out.put("distributor_id", requested);
		out.put("distributor_name", "");
		return out;
	}

	private static long parseDistributorIdFromRow(Map<String, Object> row, long fallback) {
		Object idObj = row.get("distributor_id");
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		if (idObj != null) {
			try {
				return Long.parseLong(idObj.toString().trim());
			} catch (NumberFormatException ignored) {
				return fallback;
			}
		}
		return fallback;
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
		m.put("disabled", row.getDisabled());
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
}
