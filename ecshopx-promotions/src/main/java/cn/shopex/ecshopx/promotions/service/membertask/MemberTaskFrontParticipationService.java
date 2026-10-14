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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskTypeCatalog;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionListMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** C 端历史参与任务（ECX-10434 · VshopAPI participations）。 */
@Service
public class MemberTaskFrontParticipationService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final DateTimeFormatter ROW_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

	private final MemberTaskCompletionListMapper memberTaskCompletionListMapper;

	public MemberTaskFrontParticipationService(
			MemberTaskCompletionListMapper memberTaskCompletionListMapper) {
		this.memberTaskCompletionListMapper = memberTaskCompletionListMapper;
	}

	public Map<String, Object> list(long companyId, long userId, int pageNo, int pageSize) {
		int safePageNo = Math.max(1, pageNo);
		int safePageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
		long total = memberTaskCompletionListMapper.countUserParticipations(companyId, userId);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("page_no", safePageNo);
		out.put("page_size", safePageSize);
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		long offset = (long) (safePageNo - 1) * (long) safePageSize;
		List<Map<String, Object>> rows =
				memberTaskCompletionListMapper.selectUserParticipationsPage(
						companyId, userId, offset, safePageSize);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			list.add(toItem(row));
		}
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> toItem(Map<String, Object> row) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("completion_id", longVal(row.get("completion_id")));
		item.put("task_id", longVal(row.get("task_id")));
		item.put("name", stringVal(row.get("task_name")));
		String typeCategory = stringVal(row.get("type_category"));
		String typeDetail = stringVal(row.get("type_detail"));
		item.put(
				"type_label",
				MemberTaskTypeCatalog.typeLabel(typeCategory, typeDetail));
		int completedAt = intVal(row.get("completed_at"));
		item.put("completed_at", completedAt);
		if (completedAt > 0) {
			item.put("completed_at_text", ROW_TS.format(Instant.ofEpochSecond(completedAt)));
		}
		String grantStatus = stringVal(row.get("grant_status"));
		if (grantStatus.isEmpty()) {
			item.put("grant_status", null);
		} else {
			item.put("grant_status", grantStatus);
		}
		item.put("user_task_status", MemberTaskFrontDisplaySupport.historyUserTaskStatus(grantStatus));
		item.put("grant_status_text", MemberTaskFrontDisplaySupport.frontGrantStatusText(grantStatus));
		Object reward = row.get("reward_summary");
		item.put("reward_summary", reward == null ? null : reward.toString());
		return item;
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

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}
}
