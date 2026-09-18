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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivitiesAdminListMapper;
import cn.shopex.ecshopx.employeepurchase.support.ActivityListDisplayStatusQuery;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeePurchaseActivitiesAdminListFilter;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityEnterpriseBehaviorStatsService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
public class EmployeePurchaseAdminActivityListService {

	private final EmployeePurchaseActivitiesAdminListMapper activitiesAdminListMapper;
	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final EmployeePurchaseActivityListRowAssembler activityListRowAssembler;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ActivityEnterpriseBehaviorStatsService activityEnterpriseBehaviorStatsService;
	private final EnterpriseConfigService enterpriseConfigService;

	public EmployeePurchaseAdminActivityListService(
			EmployeePurchaseActivitiesAdminListMapper activitiesAdminListMapper,
			ActivityEnterprisesMapper activityEnterprisesMapper,
			EmployeePurchaseActivityListRowAssembler activityListRowAssembler,
			DistributorListQueryService distributorListQueryService,
			DistributorSelfMetaService distributorSelfMetaService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ActivityEnterpriseBehaviorStatsService activityEnterpriseBehaviorStatsService,
			EnterpriseConfigService enterpriseConfigService) {
		this.activitiesAdminListMapper = activitiesAdminListMapper;
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.activityListRowAssembler = activityListRowAssembler;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.activityEnterpriseBehaviorStatsService = activityEnterpriseBehaviorStatsService;
		this.enterpriseConfigService = enterpriseConfigService;
	}

	public Map<String, Object> getActivityList(
			long companyId,
			Map<String, Object> operatorJwt,
			int page,
			int pageSize,
			String name,
			Integer displayTimeBegin,
			Integer buyTimeBegin,
			Integer buyTimeEnd,
			Long enterpriseId,
			String status,
			String distributorIdParam) {
		if (enterpriseId != null && enterpriseId > 0) {
			List<ActivityEnterprises> enterpriseRows =
					activityEnterprisesMapper.selectList(
							Wrappers.<ActivityEnterprises>lambdaQuery()
									.eq(ActivityEnterprises::getCompanyId, companyId)
									.eq(ActivityEnterprises::getEnterpriseId, enterpriseId));
			List<Long> idIn =
					enterpriseRows.stream()
							.map(ActivityEnterprises::getActivityId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
			if (idIn.isEmpty()) {
				return emptyListResult();
			}
			return queryAndAssemble(
					companyId,
					operatorJwt,
					page,
					pageSize,
					name,
					displayTimeBegin,
					buyTimeBegin,
					buyTimeEnd,
					status,
					distributorIdParam,
					idIn);
		}
		return queryAndAssemble(
				companyId,
				operatorJwt,
				page,
				pageSize,
				name,
				displayTimeBegin,
				buyTimeBegin,
				buyTimeEnd,
				status,
				distributorIdParam,
				null);
	}

	private Map<String, Object> queryAndAssemble(
			long companyId,
			Map<String, Object> operatorJwt,
			int page,
			int pageSize,
			String name,
			Integer displayTimeBegin,
			Integer buyTimeBegin,
			Integer buyTimeEnd,
			String status,
			String distributorIdParam,
			List<Long> idIn) {
		EmployeePurchaseActivitiesAdminListFilter f = new EmployeePurchaseActivitiesAdminListFilter();
		f.setCompanyId(companyId);
		if (idIn != null) {
			f.setIdIn(idIn);
		}

		boolean isDistributor = "distributor".equals(stringOrEmpty(operatorJwt.get("operator_type")));
		if (isDistributor) {
			long d = readDistributorIdForOperator(operatorJwt, companyId);
			f.setDistributorId((int) d);
		} else if (StringUtils.hasText(distributorIdParam)) {
			try {
				f.setDistributorId((int) Long.parseLong(distributorIdParam.trim()));
			} catch (NumberFormatException ignored) {
			}
		}

		if (StringUtils.hasText(name)) {
			f.setNameContains(name.trim());
		}

		List<String> statusSlugs = ActivityListDisplayStatusQuery.statusSlugsForFilterOrNull(status);
		boolean multiStatusFilter = statusSlugs != null && statusSlugs.size() > 1;
		String statusTrim =
				statusSlugs != null && statusSlugs.size() == 1 ? statusSlugs.get(0) : (StringUtils.hasText(status) ? status.trim() : "");
		boolean queryNotStarted = "not_started".equals(statusTrim);
		if (displayTimeBegin != null && !queryNotStarted && !multiStatusFilter) {
			f.setDisplayTimeGt(displayTimeBegin);
		}

		if (buyTimeBegin != null) {
			f.setBuyTimeBegin(buyTimeBegin);
		}
		if (buyTimeEnd != null) {
			f.setBuyTimeEnd(buyTimeEnd);
		}

		if (!multiStatusFilter) {
			switch (statusTrim) {
				case "not_started" -> f.setStatusVirtual("not_started");
				case "warm_up" -> f.setStatusVirtual("warm_up");
				case "ongoing" -> f.setStatusVirtual("ongoing");
				case "pending" -> f.setStatusVirtual("pending");
				case "cancel" -> f.setStatusDbEquals("cancel");
				case "over" -> f.setStatusVirtual("over");
				default -> {
				}
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000);
		long total = activitiesAdminListMapper.countByFilter(f, now);
		if (total == 0L && !multiStatusFilter) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", 0L);
			out.put("list", List.of());
			return out;
		}

		long offset = multiStatusFilter ? 0L : (long) (page - 1) * pageSize;
		int fetchSize = multiStatusFilter ? safeIntSize(Math.max(total, pageSize)) : pageSize;
		List<Activities> rows =
				total == 0L && multiStatusFilter
						? List.of()
						: activitiesAdminListMapper.selectPageByFilter(f, now, offset, fetchSize);

		LinkedHashSet<Integer> distIds = new LinkedHashSet<>();
		for (Activities a : rows) {
			Integer did = a.getDistributorId();
			if (did != null && did >= 0) {
				distIds.add(did);
			}
		}
		List<Long> positiveIds =
				distIds.stream().filter(id -> id > 0).map(Integer::longValue).distinct().toList();
		Map<Long, String> nameByDistId = new LinkedHashMap<>();
		for (Distributor d : distributorListQueryService.listByIdsAndCompany(companyId, positiveIds)) {
			if (d.getDistributorId() != null) {
				nameByDistId.put(
						d.getDistributorId(), d.getName() != null ? d.getName() : "");
			}
		}
		String zeroName = "";
		if (distIds.contains(0)) {
			Object nm = distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId).get("name");
			zeroName = nm == null ? "" : nm.toString();
		}

		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		List<Long> activityIds = rows.stream().map(Activities::getId).filter(Objects::nonNull).toList();
		Map<Long, Map<String, Object>> statsByActivityId =
				activityEnterpriseBehaviorStatsService.loadActivityLevelStats(companyId, activityIds);
		Map<Long, List<Integer>> perCapitaByActivityId =
				enterpriseConfigService.loadPerCapitaFeesByActivityIds(companyId, activityIds);
		for (Activities row : rows) {
			List<Integer> fees =
					perCapitaByActivityId.getOrDefault(
							row.getId() == null ? -1L : row.getId(), List.of());
			Map<String, Object> m = activityListRowAssembler.toRow(row, fees);
			applyActivityListStatusRewrite(m, row, now);
			m.put("distributor_name", distributorNameForRow(row.getDistributorId(), nameByDistId, zeroName));
			Map<String, Object> stats = statsByActivityId.get(row.getId());
			if (stats != null) {
				m.putAll(stats);
			} else {
				m.put("scan_count", 0);
				m.put("scan_user_count", 0);
				m.put("passphrase_verify_user_count", 0);
				m.put("bind_user_count", 0);
				m.put("order_user_count", 0);
			}
			list.add(m);
		}

		if (multiStatusFilter && statusSlugs != null) {
			Set<String> allowed = new LinkedHashSet<>(statusSlugs);
			list =
					list.stream()
							.filter(row -> {
								Object st = row.get("status");
								return st != null && allowed.contains(st.toString());
							})
							.toList();
			total = list.size();
			int fromIndex = (page - 1) * pageSize;
			if (fromIndex >= list.size()) {
				list = List.of();
			} else {
				int toIndex = Math.min(fromIndex + pageSize, list.size());
				list = list.subList(fromIndex, toIndex);
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> emptyListResult() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 0L);
		out.put("list", List.of());
		return out;
	}

	private static void applyActivityListStatusRewrite(Map<String, Object> m, Activities row, int now) {
		ActivityListDisplayStatusQuery.applyDisplayStatusRewrite(
				m,
				row.getStatus(),
				row.getDisplayTime() == null ? 0 : row.getDisplayTime(),
				row.getEmployeeBeginTime() == null ? 0 : row.getEmployeeBeginTime(),
				row.getEmployeeEndTime() == null ? 0 : row.getEmployeeEndTime(),
				row.getRelativeBeginTime() == null ? null : row.getRelativeBeginTime().longValue(),
				row.getRelativeEndTime() == null ? null : row.getRelativeEndTime().longValue(),
				now);
	}

	private static String distributorNameForRow(
			Integer distributorId, Map<Long, String> nameByDistId, String zeroName) {
		if (distributorId == null) {
			return "";
		}
		if (distributorId == 0) {
			return zeroName;
		}
		return nameByDistId.getOrDefault(distributorId.longValue(), "");
	}

	private static String stringOrEmpty(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static int safeIntSize(long size) {
		if (size <= 0L) {
			return 0;
		}
		return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
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

}
