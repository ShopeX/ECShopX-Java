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

package cn.shopex.ecshopx.promotions.service.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskTypeCatalog;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskStatsMapper;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskAttributedMetricsDisplay;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberTaskOverviewExportAsyncService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskOverviewExportAsyncService.class);

	public static final String EXPORT_TYPE = "export_member_task_overview";

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter ROW_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskStatsMapper memberTaskStatsMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public MemberTaskOverviewExportAsyncService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskStatsMapper memberTaskStatsMapper,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskStatsMapper = memberTaskStatsMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(MemberTaskOverviewExportJobContext ctx) {
		long now = Instant.now().getEpochSecond();
		List<MemberTask> tasks =
				memberTaskMapper.selectAdminListForExport(
						ctx.companyId(),
						trimToNull(ctx.taskIdKeyword()),
						trimToNull(ctx.nameKeyword()),
						trimToNull(ctx.typeDetail()),
						trimToNull(ctx.displayStatus()),
						ctx.filterBegin(),
						ctx.filterEnd(),
						now);
		if (tasks == null || tasks.isEmpty()) {
			return;
		}
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("task_id", "任务ID");
		titles.put("name", "任务名称");
		titles.put("type_label", "任务类型");
		titles.put("task_time", "任务时间");
		titles.put("reward_summary", "任务奖励");
		titles.put("completion_count", "任务完成次数");
		titles.put("completion_user_count", "任务完成人数");
		titles.put("attributed_spend", "通过任务累计消费");
		titles.put("attributed_join_count", "通过任务累计入会");
		List<Map<String, String>> rows = new ArrayList<>();
		for (MemberTask task : tasks) {
			long taskId = task.getTaskId() != null ? task.getTaskId() : 0L;
			if (taskId <= 0L) {
				continue;
			}
			long completionCount =
					memberTaskStatsMapper.countCompletions(ctx.companyId(), taskId);
			long completionUserCount =
					memberTaskStatsMapper.countCompletionUsers(ctx.companyId(), taskId);
			long attributedSpendFen =
					memberTaskStatsMapper.sumAttributedOrderSpendFen(ctx.companyId(), taskId);
			long attributedJoinCount =
					memberTaskStatsMapper.countAttributedJoinUsers(ctx.companyId(), taskId);
			int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
			int end = task.getEndTime() != null ? task.getEndTime() : 0;
			String timeRange = formatTimeRange(begin, end);
			Map<String, String> row = new LinkedHashMap<>();
			row.put("task_id", String.valueOf(taskId));
			row.put("name", task.getName() == null ? "" : task.getName());
			row.put(
					"type_label",
					MemberTaskTypeCatalog.typeLabel(task.getTypeCategory(), task.getTypeDetail()));
			row.put("task_time", timeRange);
			row.put("reward_summary", task.getRewardSummary() == null ? "" : task.getRewardSummary());
			row.put("completion_count", String.valueOf(completionCount));
			row.put("completion_user_count", String.valueOf(completionUserCount));
			row.put(
					"attributed_spend",
					MemberTaskAttributedMetricsDisplay.spendYuanText(attributedSpendFen));
			row.put("attributed_join_count", String.valueOf(attributedJoinCount));
			rows.add(row);
		}
		if (rows.isEmpty()) {
			return;
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + ctx.companyId() + "会员任务完成概览";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, rows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("member_task overview export: csv upload failed companyId={}", ctx.companyId());
			return;
		}
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				ctx.merchantId(),
				ctx.supplierId(),
				EXPORT_TYPE,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}

	private static String formatTimeRange(int begin, int end) {
		if (begin <= 0 && end <= 0) {
			return "";
		}
		String b = begin > 0 ? ROW_TS.format(Instant.ofEpochSecond(begin)) : "";
		String e = end > 0 ? ROW_TS.format(Instant.ofEpochSecond(end)) : "";
		if (b.isEmpty()) {
			return e;
		}
		if (e.isEmpty()) {
			return b;
		}
		return b + " ~ " + e;
	}

	private static String trimToNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		return t.isEmpty() ? null : t;
	}
}
