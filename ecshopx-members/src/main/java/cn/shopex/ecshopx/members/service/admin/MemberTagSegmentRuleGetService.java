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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberSegmentRule;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberSegmentRuleMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MemberTagSegmentRuleGetService {

	private final MemberSegmentRuleMapper memberSegmentRuleMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final ObjectMapper objectMapper;

	public MemberTagSegmentRuleGetService(
			MemberSegmentRuleMapper memberSegmentRuleMapper,
			MemberTagsMapper memberTagsMapper,
			ObjectMapper objectMapper) {
		this.memberSegmentRuleMapper = memberSegmentRuleMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSegmentRule(
			long companyId,
			String operatorType,
			long jwtDistributorId,
			long ruleId,
			boolean distributorIdQueryKeyPresent,
			String distributorIdQueryParamRaw) {
		LambdaQueryWrapper<MemberSegmentRule> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(MemberSegmentRule::getRuleId, ruleId);
		wrapper.eq(MemberSegmentRule::getCompanyId, companyId);
		appendDistributorScope(
				wrapper,
				operatorType,
				jwtDistributorId,
				distributorIdQueryKeyPresent,
				distributorIdQueryParamRaw);

		MemberSegmentRule rule = memberSegmentRuleMapper.selectOne(wrapper);
		if (rule == null) {
			throw new ResourceException("规则不存在或无权限查看");
		}

		List<Long> tagIdList = parseTagIds(rule.getTagIds());

		List<Map<String, Object>> tags;
		if (tagIdList.isEmpty()) {
			tags = Collections.emptyList();
		} else {
			LambdaQueryWrapper<MemberTags> tw = new LambdaQueryWrapper<>();
			tw.eq(MemberTags::getCompanyId, companyId);
			tw.in(MemberTags::getTagId, tagIdList);
			tw.select(MemberTags::getTagId, MemberTags::getTagName);
			List<MemberTags> tagRows = memberTagsMapper.selectList(tw);
			tags = new ArrayList<>(tagRows.size());
			for (MemberTags tagRow : tagRows) {
				LinkedHashMap<String, Object> tagRowMap = new LinkedHashMap<>();
				tagRowMap.put(
						"tag_id",
						Long.valueOf(Objects.requireNonNullElse(tagRow.getTagId(), 0L)));
				tagRowMap.put("tag_name", tagRow.getTagName() == null ? "" : tagRow.getTagName());
				tags.add(tagRowMap);
			}
		}

		Object condition = parseCondition(rule.getRuleConfig());

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("rule_id", rule.getRuleId());
		body.put("rule_name", rule.getRuleName() == null ? "" : rule.getRuleName());
		body.put("description", rule.getDescription() == null ? "" : rule.getDescription());
		body.put("condition", condition);
		body.put("tag_ids", tagIdList);
		body.put("tags", tags);
		body.put("status", rule.getStatus() == null ? 0 : rule.getStatus());
		body.put("created", rule.getCreated());
		Long updatedVal = rule.getUpdated();
		if (updatedVal == null || updatedVal <= 0) {
			body.put("updated", null);
		} else {
			body.put("updated", updatedVal);
		}
		return body;
	}

	private List<Long> parseTagIds(String raw) {
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		try {
			List<Long> parsed = objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
			if (parsed == null) {
				return Collections.emptyList();
			}
			for (Long el : parsed) {
				if (el == null) {
					return Collections.emptyList();
				}
			}
			return parsed;
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
	}

	private Object parseCondition(String cfg) {
		if (cfg == null || cfg.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(cfg, Object.class);
		} catch (JsonProcessingException e) {
			return new LinkedHashMap<>();
		}
	}

	private static long parseDistributorIdQueryLong(String distributorIdQueryParamRaw) {
		String s = distributorIdQueryParamRaw == null ? "" : distributorIdQueryParamRaw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return (long) (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static void appendDistributorScope(
			LambdaQueryWrapper<MemberSegmentRule> wrapper,
			String operatorType,
			long jwtDistributorId,
			boolean distributorIdQueryKeyPresent,
			String distributorIdQueryParamRaw) {
		if (Objects.equals(operatorType, "distributor")) {
			if (jwtDistributorId > 0) {
				wrapper.eq(MemberSegmentRule::getDistributorId, jwtDistributorId);
			}
		} else {
			if (distributorIdQueryKeyPresent) {
				wrapper.eq(
						MemberSegmentRule::getDistributorId,
						parseDistributorIdQueryLong(distributorIdQueryParamRaw));
			}
		}
	}
}
