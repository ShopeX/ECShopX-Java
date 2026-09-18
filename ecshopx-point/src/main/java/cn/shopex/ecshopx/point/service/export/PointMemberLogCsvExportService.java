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

package cn.shopex.ecshopx.point.service.export;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.service.PointMemberJournalTypeDescriptions;
import cn.shopex.ecshopx.point.service.PointMemberListService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointMemberLogCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(PointMemberLogCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final DateTimeFormatter ROW_DT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
	private static final int BATCH = 2000;
	private static final String EXPORT_TYPE = "member_point_logs";

	private final PointMemberListService pointMemberListService;
	private final MemberAccountService memberAccountService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public PointMemberLogCsvExportService(
			PointMemberListService pointMemberListService,
			MemberAccountService memberAccountService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.pointMemberListService = pointMemberListService;
		this.memberAccountService = memberAccountService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(PointMemberLogExportContext ctx) {
		long total = pointMemberListService.countLocalPointMemberLogs(
				ctx.companyId(), ctx.userIdParam(), ctx.mobile(), ctx.username(), ctx.name(), ctx.dateBegin(), ctx.dateEnd());
		if (total == 0) {
			return;
		}
		int pages = (int) Math.ceil(total / (double) BATCH);

		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("created", "时间");
		titles.put("username", "微信昵称");
		titles.put("name", "用户名");
		titles.put("mobile", "手机号");
		titles.put("in_outcome", "积分变动");
		titles.put("order_id", "订单号");
		titles.put("journal_type_desc", "变动类型");
		titles.put("point_desc", "记录");
		titles.put("point", "当前剩余积分");

		List<Map<String, String>> allRows = new ArrayList<>();
		for (int page = 1; page <= pages; page++) {
			List<PointMemberLog> logs = pointMemberListService.pageLocalPointMemberLogs(
					ctx.companyId(),
					ctx.userIdParam(),
					ctx.mobile(),
					ctx.username(),
					ctx.name(),
					ctx.dateBegin(),
					ctx.dateEnd(),
					page,
					BATCH);
			List<Long> uids = new ArrayList<>();
			for (PointMemberLog lg : logs) {
				if (lg.getUserId() != null) {
					uids.add(lg.getUserId());
				}
			}
			Map<Long, Map<String, Object>> userData = new HashMap<>();
			if (!uids.isEmpty()) {
				for (Map<String, Object> s : memberAccountService.listMemberSummariesByUserIds(ctx.companyId(), uids)) {
					Object id = s.get("user_id");
					if (id instanceof Number n) {
						userData.put(n.longValue(), s);
					}
				}
			}
			for (PointMemberLog lg : logs) {
				allRows.add(buildRow(lg, userData, ctx.datapassBlock()));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + ctx.companyId() + "member_point_logs";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				0L,
				ctx.supplierId(),
				EXPORT_TYPE,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}

	private static Map<String, String> buildRow(
			PointMemberLog log, Map<Long, Map<String, Object>> userData, boolean datapassBlock) {
		long uid = log.getUserId() != null ? log.getUserId() : 0L;
		Map<String, Object> u = userData.get(uid);
		String username = u != null && u.get("username") != null ? String.valueOf(u.get("username")) : "";
		String name = u != null && u.get("name") != null ? String.valueOf(u.get("name")) : "";
		String mobile = u != null && u.get("mobile") != null ? String.valueOf(u.get("mobile")) : "";
		if (!datapassBlock) {
			mobile = DataMasking.maskUname(mobile);
		}

		int income = log.getIncome() != null ? log.getIncome() : 0;
		int outcome = log.getOutcome() != null ? log.getOutcome() : 0;
		String inOutcome;
		if (income > 0) {
			inOutcome = "+" + income;
		} else if (outcome > 0) {
			inOutcome = "-" + outcome;
		} else {
			inOutcome = "0";
		}

		int createdSec = log.getCreated() != null ? log.getCreated() : 0;
		String createdStr = ROW_DT.format(Instant.ofEpochSecond(createdSec));

		Map<String, String> row = new LinkedHashMap<>();
		row.put("created", createdStr);
		row.put("username", username);
		row.put("name", name);
		row.put("mobile", mobile);
		row.put("in_outcome", inOutcome);
		row.put("order_id", formatOrderIdCell(log.getOrderId()));
		row.put("journal_type_desc", PointMemberJournalTypeDescriptions.forType(log.getJournalType()));
		row.put("point_desc", log.getPointDesc() != null ? log.getPointDesc() : "");
		row.put("point", String.valueOf(PointMemberListService.parseSPointFromPointDesc(log.getPointDesc())));
		return row;
	}

	private static String formatOrderIdCell(String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return "";
		}
		return "\"'" + orderId.trim() + "\"";
	}
}
