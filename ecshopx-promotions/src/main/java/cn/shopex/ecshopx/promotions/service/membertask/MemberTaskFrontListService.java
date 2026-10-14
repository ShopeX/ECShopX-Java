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

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCompletion;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskCycleKey;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskRewardGrant;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskTypeCatalog;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskUserTaskStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskUserTaskStatusResolver;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskCompletionMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskRewardGrantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** C 端任务列表（ECX-10434 · SSOT §11）。 */
@Service
public class MemberTaskFrontListService {

	private static final DateTimeFormatter TASK_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberTaskMapper memberTaskMapper;
	private final MemberTaskCompletionMapper memberTaskCompletionMapper;
	private final MemberTaskRewardGrantMapper memberTaskRewardGrantMapper;
	private final MemberTaskMemberTagReadPort memberTaskMemberTagReadPort;
	private final ObjectMapper objectMapper;

	public MemberTaskFrontListService(
			MemberTaskMapper memberTaskMapper,
			MemberTaskCompletionMapper memberTaskCompletionMapper,
			MemberTaskRewardGrantMapper memberTaskRewardGrantMapper,
			MemberTaskMemberTagReadPort memberTaskMemberTagReadPort,
			ObjectMapper objectMapper) {
		this.memberTaskMapper = memberTaskMapper;
		this.memberTaskCompletionMapper = memberTaskCompletionMapper;
		this.memberTaskRewardGrantMapper = memberTaskRewardGrantMapper;
		this.memberTaskMemberTagReadPort = memberTaskMemberTagReadPort;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> list(long companyId, long userId, Locale locale) {
		long now = System.currentTimeMillis() / 1000L;
		Set<Long> userTags = memberTaskMemberTagReadPort.userTagIds(companyId, userId);
		List<MemberTask> published =
				memberTaskMapper.selectList(
						new QueryWrapper<MemberTask>()
								.eq("company_id", companyId)
								.eq("published", 1)
								.orderByDesc("task_id"));
		List<Map<String, Object>> list = new ArrayList<>();
		for (MemberTask task : published) {
			if (!MemberTaskAudiencePolicy.visibleToUser(task, userTags, objectMapper)) {
				continue;
			}
			String activityDisplay = resolveDisplay(task, now);
			String cycleKey = MemberTaskCycleKey.current(task, now);
			MemberTaskCompletion completion =
					memberTaskCompletionMapper.selectOne(
							new QueryWrapper<MemberTaskCompletion>()
									.eq("task_id", task.getTaskId())
									.eq("user_id", userId)
									.eq("cycle_key", cycleKey)
									.eq("voided", 0)
									.orderByDesc("completion_id")
									.last("LIMIT 1"));
			String grantStatus = null;
			if (completion != null) {
				MemberTaskRewardGrant grant =
						memberTaskRewardGrantMapper.selectOne(
								new QueryWrapper<MemberTaskRewardGrant>()
										.eq("completion_id", completion.getCompletionId())
										.last("LIMIT 1"));
				if (grant != null) {
					grantStatus = grant.getStatus();
				}
			}
			String userTaskStatus =
					completion == null
							? MemberTaskUserTaskStatus.TODO
							: MemberTaskUserTaskStatusResolver.resolve(grantStatus);
			if (!MemberTaskUserTaskStatusResolver.shouldShowInFrontList(
					activityDisplay, userTaskStatus)) {
				continue;
			}
			list.add(toListItem(task, activityDisplay, userTaskStatus, readJumpPage(task), objectMapper));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", list.size());
		return out;
	}

	private static Map<String, Object> toListItem(
			MemberTask task,
			String activityDisplay,
			String userTaskStatus,
			String jumpPage,
			ObjectMapper objectMapper) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("task_id", task.getTaskId());
		item.put("name", task.getName());
		item.put("type_category", task.getTypeCategory());
		item.put("type_detail", task.getTypeDetail());
		item.put(
				"type_label",
				MemberTaskTypeCatalog.typeLabel(task.getTypeCategory(), task.getTypeDetail()));
		item.put("reward_summary", task.getRewardSummary());
		item.put("display_status", activityDisplay);
		item.put("user_task_status", userTaskStatus);
		int begin = task.getBeginTime() != null ? task.getBeginTime() : 0;
		int end = task.getEndTime() != null ? task.getEndTime() : 0;
		item.put("begin_time", begin);
		item.put("end_time", end);
		if (begin > 0) {
			item.put("begin_time_text", TASK_TIME_FMT.format(Instant.ofEpochSecond(begin)));
		}
		if (end > 0) {
			item.put("end_time_text", TASK_TIME_FMT.format(Instant.ofEpochSecond(end)));
		}
		if (jumpPage != null) {
			item.put("jump_page", jumpPage);
		}
		String frequencyType = task.getFrequencyType();
		item.put("frequency_type", frequencyType);
		item.put("frequency_period", task.getFrequencyPeriod());
		item.put(
				"frequency_text",
				MemberTaskFrontDisplaySupport.frequencyText(frequencyType, task.getFrequencyPeriod()));
		if (task.getTaskRule() != null && !task.getTaskRule().isBlank()) {
			item.put("task_rule", task.getTaskRule().trim());
		}
		Map<String, String> share =
				MemberTaskFrontDisplaySupport.shareFieldsFromRuleConfig(
						task.getRuleConfig(), objectMapper);
		share.forEach(item::put);
		return item;
	}

	private String readJumpPage(MemberTask task) {
		if (task.getJumpPage() != null && !task.getJumpPage().isBlank()) {
			return task.getJumpPage().trim();
		}
		String ruleConfigJson = task.getRuleConfig();
		if (ruleConfigJson == null || ruleConfigJson.isBlank()) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(ruleConfigJson);
			JsonNode jump = node.get("jump_page");
			if (jump != null && !jump.isNull() && jump.isTextual()) {
				String v = jump.asText().trim();
				return v.isEmpty() ? null : v;
			}
		} catch (Exception ignored) {
			// ignore malformed rule_config in list
		}
		return null;
	}

	private static String resolveDisplay(MemberTask row, long nowEpochSec) {
		int published = row.getPublished() != null ? row.getPublished() : 0;
		int offShelf = row.getOffShelf() != null ? row.getOffShelf() : 0;
		int begin = row.getBeginTime() != null ? row.getBeginTime() : 0;
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		return MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec);
	}
}
