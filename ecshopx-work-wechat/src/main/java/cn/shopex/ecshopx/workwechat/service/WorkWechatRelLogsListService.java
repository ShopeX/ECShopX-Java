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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.wechat.WorkWechatRelSalespersonBatchPort;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRelLogs;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelLogsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkWechatRelLogsListService {

	private final WorkWechatRelLogsMapper workWechatRelLogsMapper;
	private final WorkWechatRelSalespersonBatchPort salespersonBatchPort;

	public WorkWechatRelLogsListService(WorkWechatRelLogsMapper workWechatRelLogsMapper,
			WorkWechatRelSalespersonBatchPort salespersonBatchPort) {
		this.workWechatRelLogsMapper = workWechatRelLogsMapper;
		this.salespersonBatchPort = salespersonBatchPort;
	}

	public Map<String, Object> getWorkWechatRelLogsList(String userIdRaw, String pageRaw, String pageSizeRaw,
			String isFriendRaw) {
		long userIdFilter = parseLongOrZero(userIdRaw);
		int page = parseIntStrictOrZero(pageRaw);
		int pageSize = parseIntStrictOrZero(pageSizeRaw);

		LambdaQueryWrapper<WorkWechatRelLogs> countWrapper = buildBaseWrapper(userIdFilter, isFriendRaw);
		Long totalCount = workWechatRelLogsMapper.selectCount(countWrapper);
		long total = totalCount == null ? 0L : totalCount;
		if (total == 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		List<WorkWechatRelLogs> logRows;
		if (pageSize > 0) {
			LambdaQueryWrapper<WorkWechatRelLogs> pageWrapper = buildBaseWrapper(userIdFilter, isFriendRaw);
			pageWrapper.orderByDesc(WorkWechatRelLogs::getId);
			Page<WorkWechatRelLogs> p = new Page<>(page, pageSize, false);
			logRows = workWechatRelLogsMapper.selectPage(p, pageWrapper).getRecords();
		} else {
			LambdaQueryWrapper<WorkWechatRelLogs> listWrapper = buildBaseWrapper(userIdFilter, isFriendRaw);
			listWrapper.orderByDesc(WorkWechatRelLogs::getId);
			logRows = workWechatRelLogsMapper.selectList(listWrapper);
		}

		List<Long> spIds = new ArrayList<>();
		for (WorkWechatRelLogs r : logRows) {
			if (r.getSalespersonId() != null) {
				spIds.add(r.getSalespersonId());
			}
		}

		Map<Long, Map<String, Object>> salespersonMap = salespersonBatchPort.loadBySalespersonIds(spIds);

		List<Map<String, Object>> listOut = new ArrayList<>();
		for (WorkWechatRelLogs r : logRows) {
			listOut.add(toLogRow(r, salespersonMap));
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", listOut);
		return body;
	}

	private static LambdaQueryWrapper<WorkWechatRelLogs> buildBaseWrapper(long userIdFilter, String isFriendRaw) {
		LambdaQueryWrapper<WorkWechatRelLogs> w = new LambdaQueryWrapper<>();
		w.eq(WorkWechatRelLogs::getUserId, userIdFilter);
		if (queryParamTruthy(isFriendRaw)) {
			w.apply("is_friend = {0}", parseIntStrictOrZero(isFriendRaw));
		}
		return w;
	}

	private static Map<String, Object> toLogRow(WorkWechatRelLogs entity,
			Map<Long, Map<String, Object>> salespersonMap) {
		long sid = entity.getSalespersonId() == null ? 0L : entity.getSalespersonId();
		Map<String, Object> spInfo = salespersonMap.get(sid);
		Object salespersonInfo = (spInfo == null || spInfo.isEmpty()) ? Collections.emptyList() : spInfo;

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("company_id", entity.getCompanyId());
		row.put("work_userid", entity.getWorkUserid() == null ? "" : entity.getWorkUserid());
		row.put("salesperson_id", entity.getSalespersonId());
		row.put("external_userid", entity.getExternalUserid() == null ? "" : entity.getExternalUserid());
		row.put("unionid", entity.getUnionid() == null ? "" : entity.getUnionid());
		row.put("user_id", entity.getUserId());
		row.put("is_friend", Boolean.TRUE.equals(entity.getIsFriend()) ? 1 : 0);
		row.put("created", entity.getCreated());

		String raw = entity.getRemarks();
		if (raw != null && raw.indexOf(':') >= 0) {
			String[] parts = raw.split(":", -1);
			String logType = parts[0];
			String remarksOut = parts.length > 1 ? parts[1] : "";
			row.put("log_type", logType);
			row.put("remarks", remarksOut);
		} else {
			row.put("remarks", raw);
		}

		row.put("salesperson_info", salespersonInfo);
		return row;
	}

	private static boolean queryParamTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
	}

	private static int parseIntStrictOrZero(String raw) {
		if (raw == null) {
			return 0;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLongOrZero(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
