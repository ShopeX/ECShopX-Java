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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionListMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/** Admin 单任务完成明细分页（ECX-10433）。 */
@Service
public class MemberTaskCompletionListService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter ROW_TS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskCompletionListMapper memberTaskCompletionListMapper;
	private final MemberAccountService memberAccountService;
	private final MessageSource messageSource;

	public MemberTaskCompletionListService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskCompletionListMapper memberTaskCompletionListMapper,
			MemberAccountService memberAccountService,
			MessageSource messageSource) {
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskCompletionListMapper = memberTaskCompletionListMapper;
		this.memberAccountService = memberAccountService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> listCompletions(
			long companyId,
			long taskId,
			String grantStatusRaw,
			int page,
			int pageSize,
			Locale locale) {
		MemberTask task =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId));
		if (task == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.member_task.task_not_found", null, "任务不存在", locale),
					404);
		}
		int safePage = Math.max(1, page);
		int safePageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
		String grantFilter = MemberTaskGrantStatusDisplay.normalizeGrantStatusFilter(grantStatusRaw);

		long total =
				memberTaskCompletionListMapper.countCompletionDetails(
						companyId, taskId, grantFilter);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("task_id", taskId);
		out.put("grant_status_filter", grantFilter);
		out.put("total_count", total);
		out.put("page", safePage);
		out.put("page_size", safePageSize);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		long offset = (long) (safePage - 1) * (long) safePageSize;
		List<Map<String, Object>> rows =
				memberTaskCompletionListMapper.selectCompletionDetailsPage(
						companyId, taskId, grantFilter, offset, safePageSize);
		List<Long> userIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			long uid = longVal(row.get("user_id"));
			if (uid > 0L) {
				userIds.add(uid);
			}
		}
		Map<Long, Map<String, Object>> memberByUserId = new HashMap<>();
		if (!userIds.isEmpty()) {
			List<Map<String, Object>> summaries =
					memberAccountService.listMemberSummariesByUserIds(companyId, userIds);
			for (Map<String, Object> s : summaries) {
				Object uidObj = s.get("user_id");
				if (uidObj != null) {
					long uid =
							uidObj instanceof Number n
									? n.longValue()
									: Long.parseLong(uidObj.toString().trim());
					memberByUserId.put(uid, s);
				}
			}
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			list.add(toListItem(row, memberByUserId));
		}
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> toListItem(
			Map<String, Object> row, Map<Long, Map<String, Object>> memberByUserId) {
		Map<String, Object> item = new LinkedHashMap<>();
		long completionId = longVal(row.get("completion_id"));
		long userId = longVal(row.get("user_id"));
		long grantId = longVal(row.get("grant_id"));
		int completedAt = intVal(row.get("completed_at"));
		String grantStatus = row.get("grant_status") == null ? "" : row.get("grant_status").toString();
		item.put("completion_id", completionId);
		item.put("user_id", userId);
		Map<String, Object> mem = userId > 0L ? memberByUserId.get(userId) : null;
		item.put("nickname", mem == null ? "" : Objects.toString(mem.get("nickname"), ""));
		item.put("completed_at", completedAt);
		item.put(
				"completed_at_text",
				completedAt > 0 ? ROW_TS.format(Instant.ofEpochSecond(completedAt)) : "");
		item.put("grant_id", grantId > 0L ? grantId : null);
		item.put("grant_status", grantStatus.isEmpty() ? null : grantStatus);
		item.put("grant_status_label", MemberTaskGrantStatusDisplay.labelForStatus(grantStatus));
		Object err = row.get("error_message");
		item.put("error_message", err == null ? "" : err.toString());
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
}
