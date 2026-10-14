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
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionListMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskStatsMapper;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskGrantStatusDisplay;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberTaskCompletionExportAsyncService {

	private static final Logger log = LoggerFactory.getLogger(MemberTaskCompletionExportAsyncService.class);

	private static final String EXPORT_TYPE = MemberTaskExportCsvTemplates.EXPORT_TYPE_COMPLETION;
	private static final int BATCH = 100;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter ROW_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskStatsMapper memberTaskStatsMapper;
	private final MemberTaskCompletionListMapper memberTaskCompletionListMapper;
	private final MemberAccountService memberAccountService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public MemberTaskCompletionExportAsyncService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskStatsMapper memberTaskStatsMapper,
			MemberTaskCompletionListMapper memberTaskCompletionListMapper,
			MemberAccountService memberAccountService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskStatsMapper = memberTaskStatsMapper;
		this.memberTaskCompletionListMapper = memberTaskCompletionListMapper;
		this.memberAccountService = memberAccountService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(MemberTaskCompletionExportJobContext ctx) {
		if (ctx.taskId() <= 0L) {
			return;
		}
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", ctx.taskId())
								.eq("company_id", ctx.companyId()));
		if (task == null) {
			return;
		}
		String grantFilter =
				MemberTaskGrantStatusDisplay.normalizeGrantStatusFilter(ctx.grantStatusFilter());
		long total =
				memberTaskCompletionListMapper.countCompletionDetails(
						ctx.companyId(), ctx.taskId(), grantFilter);
		if (total == 0L) {
			return;
		}
		long completionCount =
				memberTaskStatsMapper.countCompletions(ctx.companyId(), ctx.taskId());
		long completionUserCount =
				memberTaskStatsMapper.countCompletionUsers(ctx.companyId(), ctx.taskId());
		int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
		int end = task.getEndTime() != null ? task.getEndTime() : 0;
		List<String> preamble =
				MemberTaskExportCsvTemplates.completionPreambleLines(
						task.getName(),
						ctx.taskId(),
						formatTimeRange(begin, end),
						completionCount,
						completionUserCount);
		LinkedHashMap<String, String> titles =
				MemberTaskExportCsvTemplates.completionDataColumnKeysToHeader();
		List<Map<String, String>> allRows = new ArrayList<>();
		int pages = (int) Math.ceil(total / (double) BATCH);
		for (int page = 1; page <= pages; page++) {
			long offset = (long) (page - 1) * (long) BATCH;
			List<Map<String, Object>> records =
					memberTaskCompletionListMapper.selectCompletionDetailsPage(
							ctx.companyId(), ctx.taskId(), grantFilter, offset, BATCH);
			List<Long> pageUserIds = new ArrayList<>();
			for (Map<String, Object> row : records) {
				long uid = longVal(row.get("user_id"));
				if (uid > 0L) {
					pageUserIds.add(uid);
				}
			}
			pageUserIds = pageUserIds.stream().distinct().toList();
			Map<Long, Map<String, Object>> memberByUserId = new HashMap<>();
			if (!pageUserIds.isEmpty()) {
				List<Map<String, Object>> summaries =
						memberAccountService.listMemberSummariesByUserIds(ctx.companyId(), pageUserIds);
				for (Map<String, Object> s : summaries) {
					Object uidObj = s.get("user_id");
					if (uidObj == null) {
						continue;
					}
					long uid =
							uidObj instanceof Number n ? n.longValue() : Long.parseLong(uidObj.toString().trim());
					memberByUserId.put(uid, s);
				}
			}
			for (Map<String, Object> detail : records) {
				Map<String, String> row = new LinkedHashMap<>();
				long userId = longVal(detail.get("user_id"));
				Map<String, Object> mem = userId > 0L ? memberByUserId.get(userId) : null;
				row.put("nickname", mem == null ? "" : Objects.toString(mem.get("nickname"), ""));
				row.put("user_id", userId > 0L ? String.valueOf(userId) : "");
				int completedAt = intVal(detail.get("completed_at"));
				row.put(
						"completed_at",
						completedAt > 0
								? ROW_TS.format(Instant.ofEpochSecond(completedAt))
								: "");
				String grantStatus =
						detail.get("grant_status") == null
								? ""
								: detail.get("grant_status").toString();
				row.put("grant_status", MemberTaskGrantStatusDisplay.labelForStatus(grantStatus));
				Object err = detail.get("error_message");
				row.put("error_message", err == null ? "" : err.toString());
				allRows.add(row);
			}
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + ctx.companyId() + "会员任务完成明细";
		Map<String, String> uploaded =
				exportCsvFileService.exportCsv(fileBaseName, preamble, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("member_task export: csv upload failed taskId={}", ctx.taskId());
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

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object raw) {
		long v = longVal(raw);
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}
}
