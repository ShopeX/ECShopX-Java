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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeTaskBrokerageCountExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final PopularizeTaskBrokerageCountListQueryService listQueryService;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ExportCsvFileService exportCsvFileService;

	public PopularizeTaskBrokerageCountExportService(
			PopularizeTaskBrokerageCountListQueryService listQueryService,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ExportCsvFileService exportCsvFileService) {
		this.listQueryService = listQueryService;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Object exportTaskBrokerageCount(
			long companyId,
			String page,
			String pageSize,
			String promoterMobile,
			String itemName,
			String timeStart,
			String timeEnd,
			String planDate,
			boolean datapassBlock) {
		Long resolvedUserId = null;
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(promoterMobile)) {
			String promoterTrim = promoterMobile.trim();
			String enc = sensitiveFieldEncryptor.encrypt(promoterTrim);
			Members found =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getMobile, enc));
			if (found == null || found.getUserId() == null) {
				LinkedHashMap<String, Object> dataPayload = new LinkedHashMap<>();
				dataPayload.put("total_count", Integer.valueOf(0));
				dataPayload.put("list", Collections.emptyList());
				return ApiResult.ok(dataPayload);
			}
			resolvedUserId = found.getUserId();
		}

		Map<String, Object> filter = buildFilter(companyId, resolvedUserId, itemName, timeStart, timeEnd, planDate);
		Map<String, Object> countPeek = listQueryService.getTaskBrokerageCountList(filter, "*", 1, 1);
		long total = ((Number) countPeek.get("total_count")).longValue();
		if (total == 0) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, String> titles = buildTitles();
		List<Map<String, String>> csvRows = new ArrayList<>();
		int limit = 500;
		int fileNum = (int) Math.ceil(total / 500.0);
		for (int p = 1; p <= fileNum; p++) {
			Map<String, Object> batch = listQueryService.getTaskBrokerageCountList(filter, "*", p, limit);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) batch.get("list");
			@SuppressWarnings("unchecked")
			Map<String, Object> taskWrap = listQueryService.taskBrokerageListForExport(list, companyId);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> taskRows = (List<Map<String, Object>>) taskWrap.get("list");
			appendCsvRows(list, taskRows, datapassBlock, titles, csvRows);
		}

		String fileBase =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
						.format(ZonedDateTime.now(SHANGHAI))
						+ "任务佣金统计";
		Map<String, String> exportResult = exportCsvFileService.exportCsv(fileBase, titles, csvRows);
		validateExportResult(exportResult);

		LinkedHashMap<String, String> s3 = new LinkedHashMap<>();
		s3.put("filedir", exportResult.get("filedir"));
		s3.put("filename", exportResult.get("filename"));
		s3.put("url", exportResult.get("url"));
		return s3;
	}

	private Map<String, Object> buildFilter(
			long companyId,
			Long resolvedUserId,
			String itemName,
			String timeStart,
			String timeEnd,
			String planDate) {
		return listQueryService.buildCountListFilter(companyId, resolvedUserId, itemName, timeStart, timeEnd, planDate);
	}

	private static LinkedHashMap<String, String> buildTitles() {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("promoter_mobile", "推广员手机号");
		titles.put("rebate_type", "任务类型");
		titles.put("plan_date", "账期");
		titles.put("item_bn", "商品编码");
		titles.put("item_name", "商品名称");
		titles.put("item_spec_desc", "商品规格");
		titles.put("total_fee", "已完成总销售额");
		titles.put("finish_num", "已完成销售数量");
		titles.put("wait_num", "待确认数量");
		titles.put("close_num", "已关闭数量");
		titles.put("rebate_money", "分销奖金");
		titles.put("status", "是否达标");
		titles.put("wait", "待统计的订单");
		titles.put("finish", "已完成的订单");
		titles.put("close", "已关闭的订单");
		return titles;
	}

	private void appendCsvRows(
			List<Map<String, Object>> countRows,
			List<Map<String, Object>> taskRows,
			boolean datapassBlock,
			LinkedHashMap<String, String> titles,
			List<Map<String, String>> outCsvRows) {
		if (countRows == null) {
			return;
		}
		for (Map<String, Object> value : countRows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(value);
			if (datapassBlock) {
				Object mob = row.get("promoter_mobile");
				String plain = mob == null ? "" : String.valueOf(mob);
				if (StringUtils.hasText(plain)) {
					row.put("promoter_mobile", DataMasking.maskMobile(plain));
				}
			}
			List<String> waitIds = new ArrayList<>();
			List<String> finishIds = new ArrayList<>();
			List<String> closeIds = new ArrayList<>();
			long rCompany = toLong(row.get("company_id"));
			long rItem = toLong(row.get("item_id"));
			long rUser = toLong(row.get("user_id"));
			for (Map<String, Object> v : taskRows) {
				if (toLong(v.get("company_id")) == rCompany
						&& toLong(v.get("item_id")) == rItem
						&& toLong(v.get("user_id")) == rUser) {
					String oid = v.get("order_id") == null ? "" : String.valueOf(v.get("order_id")).trim();
					String wrapped = "\t" + oid + "\t";
					String st = v.get("status") == null ? "" : String.valueOf(v.get("status"));
					switch (st) {
						case "wait" -> waitIds.add(wrapped);
						case "finish" -> finishIds.add(wrapped);
						case "close" -> closeIds.add(wrapped);
						default -> { }
					}
				}
			}
			row.put("wait", waitIds);
			row.put("finish", finishIds);
			row.put("close", closeIds);

			LinkedHashMap<String, String> csvLine = new LinkedHashMap<>();
			for (String k : titles.keySet()) {
				csvLine.put(k, cellForTitleKey(k, row));
			}
			outCsvRows.add(csvLine);
		}
	}

	private static String cellForTitleKey(String k, Map<String, Object> value) {
		if ("status".equals(k)) {
			int stNum = toInt(value.get("status"));
			if (stNum != 0) {
				return "已达标";
			}
			Object ld = value.get("limit_desc");
			return ld == null ? "" : String.valueOf(ld);
		}
		if ("rebate_money".equals(k) || "total_fee".equals(k)) {
			return PopularizeTaskBrokerageCountListQueryService.formatYenFromFen(value.get(k));
		}
		if ("rebate_type".equals(k)) {
			String rt = value.get("rebate_type") == null ? "" : String.valueOf(value.get("rebate_type"));
			return "total_num".equals(rt) ? "按总数量" : "按总金额";
		}
		if ("wait".equals(k) || "finish".equals(k) || "close".equals(k)) {
			@SuppressWarnings("unchecked")
			List<String> ids = (List<String>) value.get(k);
			if (ids == null || ids.isEmpty()) {
				return "";
			}
			return String.join("\n", ids);
		}
		if ("item_bn".equals(k)) {
			Object raw = value.get("item_bn");
			if (raw == null) {
				return "";
			}
			return String.valueOf(raw);
		}
		Object v = value.get(k);
		return v == null ? "" : String.valueOf(v);
	}

	private static void validateExportResult(Map<String, String> exportResult) {
		if (exportResult == null
				|| exportResult.isEmpty()
				|| !StringUtils.hasText(exportResult.get("filedir"))
				|| !StringUtils.hasText(exportResult.get("filename"))
				|| !StringUtils.hasText(exportResult.get("url"))) {
			throw new ResourceException("导出失败");
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o).trim());
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(o).trim());
	}
}
