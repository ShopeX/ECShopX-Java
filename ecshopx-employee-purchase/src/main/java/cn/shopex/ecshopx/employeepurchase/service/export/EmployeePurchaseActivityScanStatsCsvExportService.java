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

package cn.shopex.ecshopx.employeepurchase.service.export;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityAdminExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityEnterpriseBehaviorStatsService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityScanStatsCsvExportService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final String EXPORT_TYPE = "employee_purchase_activity_scan_stats";

	private final ActivitiesMapper activitiesMapper;
	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final ActivityEnterpriseBehaviorStatsService activityEnterpriseBehaviorStatsService;
	private final EnterprisesMapper enterprisesMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public EmployeePurchaseActivityScanStatsCsvExportService(
			ActivitiesMapper activitiesMapper,
			ActivityEnterprisesMapper activityEnterprisesMapper,
			ActivityEnterpriseBehaviorStatsService activityEnterpriseBehaviorStatsService,
			EnterprisesMapper enterprisesMapper,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.activitiesMapper = activitiesMapper;
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.activityEnterpriseBehaviorStatsService = activityEnterpriseBehaviorStatsService;
		this.enterprisesMapper = enterprisesMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(ActivityAdminExportQuery query) {
		long companyId = query.companyId();
		long activityId = query.activityId();
		Activities activity = requireActivity(companyId, activityId, query.distributorId());
		boolean passphraseEnabled = Boolean.TRUE.equals(activity.getIsPassphraseEnabled());

		List<Map<String, Object>> participations = loadParticipatingEnterprises(companyId, activityId);
		if (participations.isEmpty()) {
			throw new ResourceException("导出有误,暂无数据导出");
		}

		Map<Long, Map<String, Object>> statsByEnterprise =
				indexStatsByEnterprise(
						activityEnterpriseBehaviorStatsService.loadEnterpriseStatsForActivity(
								companyId, activityId));

		LinkedHashMap<String, String> titleHeaders = buildTitleHeaders(passphraseEnabled);
		List<Map<String, String>> csvRows = new ArrayList<>();
		int[] totals = new int[5];

		for (Map<String, Object> participation : participations) {
			long enterpriseId = longVal(participation.get("enterprise_id"));
			Map<String, Object> stats = statsByEnterprise.getOrDefault(enterpriseId, Map.of());
			int scanCount = intVal(stats.get("scan_count"));
			int scanUserCount = intVal(stats.get("scan_user_count"));
			int verifyUserCount = intVal(stats.get("passphrase_verify_user_count"));
			int bindUserCount = intVal(stats.get("bind_user_count"));
			int orderUserCount = intVal(stats.get("order_user_count"));

			totals[0] += scanCount;
			totals[1] += scanUserCount;
			totals[2] += verifyUserCount;
			totals[3] += bindUserCount;
			totals[4] += orderUserCount;

			LinkedHashMap<String, String> line = new LinkedHashMap<>();
			line.put("enterprise_name", stringVal(participation.get("enterprise_name")));
			line.put("enterprise_sn", stringVal(participation.get("enterprise_sn")));
			line.put("scan_count", Integer.toString(scanCount));
			line.put("scan_user_count", Integer.toString(scanUserCount));
			if (passphraseEnabled) {
				line.put("passphrase_verify_user_count", Integer.toString(verifyUserCount));
			}
			line.put("bind_user_count", Integer.toString(bindUserCount));
			line.put("order_user_count", Integer.toString(orderUserCount));
			csvRows.add(line);
		}

		LinkedHashMap<String, String> totalLine = new LinkedHashMap<>();
		totalLine.put("enterprise_name", "合计");
		totalLine.put("enterprise_sn", "");
		totalLine.put("scan_count", Integer.toString(totals[0]));
		totalLine.put("scan_user_count", Integer.toString(totals[1]));
		if (passphraseEnabled) {
			totalLine.put("passphrase_verify_user_count", Integer.toString(totals[2]));
		}
		totalLine.put("bind_user_count", Integer.toString(totals[3]));
		totalLine.put("order_user_count", Integer.toString(totals[4]));
		csvRows.add(totalLine);

		String fileBase = FILE_TS.format(ZonedDateTime.now(CN)) + "_activity_" + activityId + "_scan_stats";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, titleHeaders, csvRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				companyId,
				query.operatorId(),
				EXPORT_TYPE,
				uploaded.get("filename"),
				uploaded.get("url"),
				finishSec);
	}

	public void validateExportable(ActivityAdminExportQuery query) {
		requireActivity(query.companyId(), query.activityId(), query.distributorId());
		List<Map<String, Object>> participations =
				loadParticipatingEnterprises(query.companyId(), query.activityId());
		if (participations.isEmpty()) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
	}

	private Activities requireActivity(long companyId, long activityId, Integer distributorId) {
		var wrapper =
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, activityId);
		if (distributorId != null) {
			wrapper.eq(Activities::getDistributorId, distributorId);
		}
		Activities activity = activitiesMapper.selectOne(wrapper.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		return activity;
	}

	private List<Map<String, Object>> loadParticipatingEnterprises(long companyId, long activityId) {
		List<ActivityEnterprises> rows =
				activityEnterprisesMapper.selectList(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.eq(ActivityEnterprises::getActivityId, activityId)
								.orderByAsc(ActivityEnterprises::getEnterpriseId));
		if (rows.isEmpty()) {
			return List.of();
		}
		Set<Long> enterpriseIds =
				rows.stream()
						.map(ActivityEnterprises::getEnterpriseId)
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.collect(Collectors.toSet());
		Map<Long, Enterprises> enterpriseById = loadEnterprises(companyId, enterpriseIds);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (ActivityEnterprises row : rows) {
			if (row.getEnterpriseId() == null || row.getEnterpriseId() <= 0L) {
				continue;
			}
			Enterprises ent = enterpriseById.get(row.getEnterpriseId());
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("enterprise_id", row.getEnterpriseId());
			m.put("enterprise_name", ent == null ? "" : nullToEmpty(ent.getName()));
			m.put("enterprise_sn", ent == null ? "" : nullToEmpty(ent.getEnterpriseSn()));
			out.add(m);
		}
		return out;
	}

	private Map<Long, Map<String, Object>> indexStatsByEnterprise(List<Map<String, Object>> statsList) {
		LinkedHashMap<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : statsList) {
			long enterpriseId = longVal(row.get("enterprise_id"));
			if (enterpriseId > 0L) {
				out.put(enterpriseId, row);
			}
		}
		return out;
	}

	private Map<Long, Enterprises> loadEnterprises(long companyId, Set<Long> enterpriseIds) {
		if (enterpriseIds.isEmpty()) {
			return Map.of();
		}
		List<Enterprises> list =
				enterprisesMapper.selectList(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.in(Enterprises::getId, enterpriseIds));
		LinkedHashMap<Long, Enterprises> out = new LinkedHashMap<>();
		for (Enterprises e : list) {
			if (e.getId() != null) {
				out.put(e.getId(), e);
			}
		}
		return out;
	}

	private static LinkedHashMap<String, String> buildTitleHeaders(boolean passphraseEnabled) {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("enterprise_name", "企业名称");
		title.put("enterprise_sn", "企业编码");
		title.put("scan_count", "扫码次数");
		title.put("scan_user_count", "扫码人数");
		if (passphraseEnabled) {
			title.put("passphrase_verify_user_count", "验证口令人数");
		}
		title.put("bind_user_count", "绑定人数");
		title.put("order_user_count", "下单人数");
		return title;
	}

	private static String nullToEmpty(String v) {
		return v == null ? "" : v;
	}

	private static long longVal(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString();
	}
}
