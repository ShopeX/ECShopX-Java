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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberSegmentRule;
import cn.shopex.ecshopx.members.mapper.MemberSegmentRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MemberTagSegmentRuleUpdateService {

	private final ObjectMapper objectMapper;
	private final MemberSegmentRuleMapper memberSegmentRuleMapper;

	public MemberTagSegmentRuleUpdateService(
			ObjectMapper objectMapper, MemberSegmentRuleMapper memberSegmentRuleMapper) {
		this.objectMapper = objectMapper;
		this.memberSegmentRuleMapper = memberSegmentRuleMapper;
	}

	public Map<String, Object> updateSegmentRule(
			long companyId,
			String operatorType,
			long jwtDistributorId,
			long ruleId,
			boolean distributorIdQueryKeyPresent,
			String distributorIdQueryParamRaw,
			Map<String, Object> merged) {
		JsonNode conditionNode = null;
		if (submitted(merged, "rule_name")) {
			Object v = merged.get("rule_name");
			if (!(v instanceof String)) {
				throw new BadRequestException("规则名称不能超过100个字符");
			}
			String s = ((String) v).trim();
			if (s.length() > 100) {
				throw new BadRequestException("规则名称不能超过100个字符");
			}
		}
		if (submitted(merged, "description")) {
			Object v = merged.get("description");
			if (!(v instanceof String)) {
				throw new BadRequestException("人群说明不能超过255个字符");
			}
			String s = (String) v;
			if (s.length() > 255) {
				throw new BadRequestException("人群说明不能超过255个字符");
			}
		}
		if (submitted(merged, "condition")) {
			Object raw = merged.get("condition");
			if (raw instanceof String) {
				throw new BadRequestException("规则配置不能为空");
			}
			if (!(raw instanceof Map<?, ?> || raw instanceof List<?>)) {
				throw new BadRequestException("规则配置不能为空");
			}
			conditionNode = objectMapper.valueToTree(raw);
			boolean ok = (conditionNode.isArray() && conditionNode.size() > 0)
					|| (conditionNode.isObject() && conditionNode.size() > 0);
			if (!ok) {
				throw new BadRequestException("规则配置不能为空");
			}
		}
		List<Long> parsedTagIdList = null;
		if (submitted(merged, "tag_ids")) {
			parsedTagIdList = parseTagIdsForUpdate(merged);
		}

		LambdaQueryWrapper<MemberSegmentRule> q = new LambdaQueryWrapper<>();
		q.eq(MemberSegmentRule::getRuleId, ruleId).eq(MemberSegmentRule::getCompanyId, companyId);
		appendDistributorScope(q, operatorType, jwtDistributorId, distributorIdQueryKeyPresent, distributorIdQueryParamRaw);
		MemberSegmentRule row = memberSegmentRuleMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("规则不存在或无权限编辑");
		}

		LambdaUpdateWrapper<MemberSegmentRule> uw = new LambdaUpdateWrapper<>();
		uw.eq(MemberSegmentRule::getRuleId, ruleId).eq(MemberSegmentRule::getCompanyId, companyId);
		appendDistributorScope(uw, operatorType, jwtDistributorId, distributorIdQueryKeyPresent, distributorIdQueryParamRaw);

		boolean any = false;
		if (submitted(merged, "rule_name")) {
			uw.set(MemberSegmentRule::getRuleName, ((String) merged.get("rule_name")).trim());
			any = true;
		}
		if (submitted(merged, "description")) {
			uw.set(MemberSegmentRule::getDescription, (String) merged.get("description"));
			any = true;
		}
		if (submitted(merged, "condition")) {
			uw.set(MemberSegmentRule::getRuleConfig, writeJsonOrThrow(conditionNode));
			any = true;
		}
		if (submitted(merged, "tag_ids")) {
			uw.set(MemberSegmentRule::getTagIds, writeJsonOrThrow(parsedTagIdList));
			any = true;
		}
		if (submitted(merged, "status")) {
			uw.set(MemberSegmentRule::getStatus, looseIntFromObject(merged.get("status")));
			any = true;
		}
		if (!any) {
			throw new BadRequestException("没有需要更新的数据");
		}
		uw.set(MemberSegmentRule::getUpdated, Instant.now().getEpochSecond());

		try {
			int affected = memberSegmentRuleMapper.update(null, uw);
			if (affected == 0) {
				throw new ResourceException("更新规则失败：未查询到更新数据");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(
					"更新规则失败：" + (e.getMessage() == null ? "" : e.getMessage()));
		}

		MemberSegmentRule fresh = memberSegmentRuleMapper.selectById(ruleId);
		if (fresh == null) {
			throw new ResourceException("更新规则失败：未查询到更新数据");
		}
		LinkedHashMap<String, Object> res = new LinkedHashMap<>();
		res.put("rule_id", fresh.getRuleId());
		res.put("rule_name", fresh.getRuleName());
		res.put("description", fresh.getDescription() != null ? fresh.getDescription() : "");
		res.put("status", "success");
		return res;
	}

	private String writeJsonOrThrow(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw wrapUpdateFailureResource(e);
		}
	}

	private static ResourceException wrapUpdateFailureResource(JsonProcessingException e) {
		String jacksonDetail = e.getOriginalMessage();
		if (jacksonDetail == null || jacksonDetail.isEmpty()) {
			jacksonDetail = e.getMessage();
		}
		if (jacksonDetail == null) {
			jacksonDetail = "";
		}
		return new ResourceException("更新规则失败：" + jacksonDetail);
	}

	private List<Long> parseTagIdsForUpdate(Map<String, Object> merged) {
		Object raw = merged.get("tag_ids");
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> col) {
			for (Object el : col) {
				Long id = toNullableLong(el);
				if (id == null) {
					throw new BadRequestException("标签ID数组必须是数组");
				}
				out.add(id);
			}
			return out;
		}
		JsonNode n = objectMapper.valueToTree(raw);
		if (!n.isArray()) {
			throw new BadRequestException("标签ID数组必须是数组");
		}
		for (JsonNode el : n) {
			if (el.isNumber()) {
				out.add(el.longValue());
			} else if (el.isTextual()) {
				try {
					out.add(Long.parseLong(el.asText().trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException("标签ID数组必须是数组");
				}
			} else {
				throw new BadRequestException("标签ID数组必须是数组");
			}
		}
		return out;
	}

	private static Long toNullableLong(Object el) {
		if (el == null) {
			return null;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static int looseIntFromObject(Object o) {
		String s = String.valueOf(o).trim();
		try {
			return (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0;
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

	private static boolean submitted(Map<String, Object> m, String k) {
		return m.containsKey(k) && m.get(k) != null;
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

	private static void appendDistributorScope(
			LambdaUpdateWrapper<MemberSegmentRule> wrapper,
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
