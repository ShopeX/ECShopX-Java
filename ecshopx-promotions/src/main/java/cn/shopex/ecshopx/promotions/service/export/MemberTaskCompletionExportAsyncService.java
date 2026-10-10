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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

	private static final String EXPORT_TYPE = "export_member_task_completion";
	private static final int BATCH = 100;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter ROW_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberAccountService memberAccountService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public MemberTaskCompletionExportAsyncService(
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberAccountService memberAccountService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberAccountService = memberAccountService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(MemberTaskCompletionExportJobContext ctx) {
		if (ctx.taskId() <= 0L) {
			return;
		}
		QueryWrapper<MemberTaskCompletion> countWrapper = new QueryWrapper<>();
		countWrapper
				.eq("company_id", ctx.companyId())
				.eq("task_id", ctx.taskId())
				.eq("voided", 0);
		long total = memberTaskCompletionMapper.selectCount(countWrapper);
		if (total == 0L) {
			return;
		}
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("user_id", "会员编号");
		titles.put("nickname", "完成会员");
		titles.put("completed_at", "完成时间");
		titles.put("grant_status", "奖励发放状态");
		titles.put("error_message", "发放失败原因");
		List<Map<String, String>> allRows = new ArrayList<>();
		QueryWrapper<MemberTaskCompletion> pageWrapper = new QueryWrapper<>();
		pageWrapper
				.eq("company_id", ctx.companyId())
				.eq("task_id", ctx.taskId())
				.eq("voided", 0)
				.orderByAsc("completion_id");
		int pages = (int) Math.ceil(total / (double) BATCH);
		for (int page = 1; page <= pages; page++) {
			Page<MemberTaskCompletion> mpPage = new Page<>(page, BATCH, false);
			memberTaskCompletionMapper.selectPage(mpPage, pageWrapper);
			List<MemberTaskCompletion> records = mpPage.getRecords();
			List<Long> pageUserIds =
					records.stream()
							.map(MemberTaskCompletion::getUserId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
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
			for (MemberTaskCompletion completion : records) {
				MemberTaskRewardGrant grant =
						memberTaskRewardGrantMapper.selectOne(
								new QueryWrapper<MemberTaskRewardGrant>()
										.eq("completion_id", completion.getCompletionId())
										.last("LIMIT 1"));
				Map<String, String> row = new LinkedHashMap<>();
				long userId = completion.getUserId() != null ? completion.getUserId() : 0L;
				row.put("user_id", userId > 0L ? String.valueOf(userId) : "");
				Map<String, Object> mem = userId > 0L ? memberByUserId.get(userId) : null;
				row.put("nickname", mem == null ? "" : Objects.toString(mem.get("nickname"), ""));
				int completedAt =
						completion.getCompletedAt() != null ? completion.getCompletedAt() : 0;
				row.put(
						"completed_at",
						completedAt > 0
								? ROW_TS.format(Instant.ofEpochSecond(completedAt))
								: "");
				row.put("grant_status", grantStatusLabel(grant));
				row.put(
						"error_message",
						grant == null || grant.getErrorMessage() == null ? "" : grant.getErrorMessage());
				allRows.add(row);
			}
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + ctx.companyId() + "会员任务完成明细";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
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

	private static String grantStatusLabel(MemberTaskRewardGrant grant) {
		if (grant == null || grant.getStatus() == null) {
			return "";
		}
		return switch (grant.getStatus()) {
			case "pending" -> "待发放";
			case "granted" -> "已发放";
			case "failed" -> "发放失败";
			case "cancelled" -> "已作废";
			default -> grant.getStatus();
		};
	}
}
