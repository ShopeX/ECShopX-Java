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
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskTypeCatalog;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Admin 任务详情（编辑回显，对齐 {@link MemberTaskSaveService} 保存体）。 */
@Service
public class MemberTaskDetailService {

	private final MemberTaskMapper memberTaskMapper;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;
	private final MemberTaskRewardsDisplayService memberTaskRewardsDisplayService;
	private final MemberTaskRuleConfigDisplayService memberTaskRuleConfigDisplayService;

	public MemberTaskDetailService(
			MemberTaskMapper memberTaskMapper,
			MessageSource messageSource,
			ObjectMapper objectMapper,
			MemberTaskRewardsDisplayService memberTaskRewardsDisplayService,
			MemberTaskRuleConfigDisplayService memberTaskRuleConfigDisplayService) {
		this.memberTaskMapper = memberTaskMapper;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
		this.memberTaskRewardsDisplayService = memberTaskRewardsDisplayService;
		this.memberTaskRuleConfigDisplayService = memberTaskRuleConfigDisplayService;
	}

	public Map<String, Object> detail(long companyId, long taskId, Locale locale) {
		MemberTask row =
				memberTaskMapper.selectOne(
						new LambdaQueryWrapper<MemberTask>()
								.eq(MemberTask::getTaskId, taskId)
								.eq(MemberTask::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.member_task.task_not_found", null, "任务不存在", locale),
					404);
		}
		long now = System.currentTimeMillis() / 1000L;
		return toDetailResponse(row, now);
	}

	private Map<String, Object> toDetailResponse(MemberTask row, long nowEpochSec) {
		int published = row.getPublished() != null ? row.getPublished() : 0;
		int offShelf = row.getOffShelf() != null ? row.getOffShelf() : 0;
		int begin = row.getBeginTime() != null ? row.getBeginTime() : 0;
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		String category = row.getTypeCategory() != null ? row.getTypeCategory() : "";
		String detail = row.getTypeDetail() != null ? row.getTypeDetail() : "";

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("task_id", row.getTaskId());
		out.put("config_version", row.getConfigVersion() == null ? 1L : row.getConfigVersion());
		out.put(
				"display_status",
				MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec));
		out.put("name", row.getName());
		out.put("type_category", category);
		out.put("type_detail", detail);
		out.put("type_label", MemberTaskTypeCatalog.typeLabel(category, detail));
		long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
		out.put(
				"rewards",
				memberTaskRewardsDisplayService.buildRewards(companyId, row.getRewardConfig()));
		out.put("begin_time", begin > 0 ? begin : null);
		out.put("end_time", end > 0 ? end : null);
		out.put("begin_time_text", begin > 0 ? MemberTaskListService.formatEpoch(begin) : "");
		out.put("end_time_text", end > 0 ? MemberTaskListService.formatEpoch(end) : "");
		out.put("frequency_type", row.getFrequencyType());
		out.put("frequency_period", row.getFrequencyPeriod());
		out.put("audience_mode", row.getAudienceMode());
		out.put("audience_tag_ids", parseAudienceTagIds(row.getAudienceTagIds()));
		out.put("audience_tag_combine", row.getAudienceTagCombine());
		out.put("task_rule", row.getTaskRule());
		out.put("reward_config", parseJsonObject(row.getRewardConfig()));
		out.put("rule_config", enrichRuleConfigForDetail(companyId, row.getRuleConfig()));
		out.put("jump_page", row.getJumpPage());
		return out;
	}

	private List<Long> parseAudienceTagIds(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (node == null || !node.isArray()) {
				return List.of();
			}
			List<Long> out = new ArrayList<>();
			for (JsonNode n : node) {
				if (n != null && n.isNumber() && n.asLong() > 0L) {
					out.add(n.asLong());
				}
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}

	private Object parseJsonObject(String json) {
		if (!StringUtils.hasText(json)) {
			return Map.of();
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (node == null || node.isNull()) {
				return Map.of();
			}
			return objectMapper.convertValue(node, Object.class);
		} catch (Exception e) {
			return Map.of();
		}
	}

	@SuppressWarnings("unchecked")
	private Object enrichRuleConfigForDetail(long companyId, String ruleConfigJson) {
		Object parsed = parseJsonObject(ruleConfigJson);
		if (!(parsed instanceof Map<?, ?> map)) {
			return parsed;
		}
		Map<String, Object> rule = new LinkedHashMap<>((Map<String, Object>) map);
		return memberTaskRuleConfigDisplayService.enrichForAdminDetail(companyId, rule);
	}
}
