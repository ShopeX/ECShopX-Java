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
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import cn.shopex.ecshopx.members.service.segment.MemberSegmentRuleMatchedUsersQueryService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 人群规则创建编排：圈选命中且标签非空时调用 {@link MemberRelTagsBatchCreateService} 建立会员与标签关系；
 * 事务提交后的导购侧同步沿用既有协调与统一异步调度策略，不改变对外契约。
 */
@Service
public class MemberTagSegmentRuleCreateService {

	private static final Logger log = LoggerFactory.getLogger(MemberTagSegmentRuleCreateService.class);

	private final ObjectMapper objectMapper;
	private final MemberSegmentRuleMapper memberSegmentRuleMapper;
	private final MemberSegmentRuleMatchedUsersQueryService matchedUsersQueryService;
	private final MemberRelTagsBatchCreateService relTagsBatchCreateService;

	public MemberTagSegmentRuleCreateService(
			ObjectMapper objectMapper,
			MemberSegmentRuleMapper memberSegmentRuleMapper,
			MemberSegmentRuleMatchedUsersQueryService matchedUsersQueryService,
			MemberRelTagsBatchCreateService relTagsBatchCreateService) {
		this.objectMapper = objectMapper;
		this.memberSegmentRuleMapper = memberSegmentRuleMapper;
		this.matchedUsersQueryService = matchedUsersQueryService;
		this.relTagsBatchCreateService = relTagsBatchCreateService;
	}

	public Map<String, Object> createSegmentRule(
			long companyId,
			String operatorType,
			long jwtDistributorId,
			Map<String, Object> merged,
			JsonNode conditionJson) {
		validateRuleName(merged);
		validateDescription(merged);
		validateConditionShape(conditionJson);
		validateConditionNonEmpty(conditionJson);
		List<Long> tagIdList = parseTagIds(merged);

		int status = resolveStatus(merged);
		long distributorId = resolveDistributorId(operatorType, jwtDistributorId, merged);

		Object ruleNameObj = merged.get("rule_name");
		String ruleName = String.valueOf(ruleNameObj).trim();
		String description = "";
		if (merged.containsKey("description") && merged.get("description") != null) {
			description = String.valueOf(merged.get("description"));
		}

		try {
			MemberSegmentRule entity = new MemberSegmentRule();
			entity.setCompanyId(companyId);
			entity.setDistributorId(distributorId);
			entity.setRuleName(ruleName);
			entity.setDescription(description);
			entity.setRuleConfig(objectMapper.writeValueAsString(conditionJson));
			entity.setTagIds(objectMapper.writeValueAsString(tagIdList));
			entity.setStatus(status);
			long now = Instant.now().getEpochSecond();
			entity.setCreated(now);
			entity.setUpdated(now);

			memberSegmentRuleMapper.insert(entity);

			List<Long> matchedUserIds = new ArrayList<>();
			int taggedCount = 0;
			try {
				matchedUserIds =
						new ArrayList<>(
								matchedUsersQueryService.queryMatchedUserIds(conditionJson, companyId, distributorId));
				if (!matchedUserIds.isEmpty() && !tagIdList.isEmpty()) {
					relTagsBatchCreateService.createRelTags(matchedUserIds, tagIdList, companyId);
					taggedCount = matchedUserIds.size();
					log.info(
							"[createSegmentRule] 圈选规则创建并打标签成功 rule_id={} rule_name={} matched_count={} tagged_count={} tag_ids={}",
							entity.getRuleId(),
							entity.getRuleName(),
							matchedUserIds.size(),
							taggedCount,
							tagIdList);
				}
			} catch (Exception e) {
				log.error(
						"[createSegmentRule] 打标签失败 rule_id={} error={}",
						entity.getRuleId(),
						e.getMessage());
			}

			LinkedHashMap<String, Object> res = new LinkedHashMap<>();
			res.put("rule_id", String.valueOf(entity.getRuleId()));
			res.put("rule_name", entity.getRuleName());
			res.put("description", entity.getDescription() != null ? entity.getDescription() : "");
			res.put("matched_count", matchedUserIds.size());
			res.put("tagged_count", taggedCount);
			res.put("status", "success");
			return res;
		} catch (ResourceException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new ResourceException("创建规则失败：" + e.getOriginalMessage());
		} catch (Exception e) {
			throw new ResourceException("创建规则失败：" + e.getMessage());
		}
	}

	public Map<String, Object> previewSegmentRule(
			long companyId, String operatorType, long jwtDistributorId, Map<String, Object> merged) {
		JsonNode conditionJson = parsePreviewConditionToJsonNode(merged);
		validateConditionShape(conditionJson);
		validateConditionNonEmpty(conditionJson);
		long distributorId = resolveDistributorId(operatorType, jwtDistributorId, merged);
		try {
			List<Long> userIds =
					matchedUsersQueryService.queryMatchedUserIds(conditionJson, companyId, distributorId);
			LinkedHashMap<String, Object> res = new LinkedHashMap<>();
			res.put("matched_count", userIds.size());
			res.put("user_ids", new ArrayList<>(userIds));
			return res;
		} catch (Exception e) {
			String msg = e.getMessage() == null ? "" : e.getMessage();
			throw new ResourceException("查询失败：" + msg);
		}
	}

	private JsonNode parsePreviewConditionToJsonNode(Map<String, Object> merged) {
		Object raw = merged.get("condition");
		if (raw == null) {
			throw new BadRequestException("规则配置必填且必须是数组");
		}
		if (raw instanceof String) {
			throw new BadRequestException("规则配置必填且必须是数组");
		}
		return objectMapper.valueToTree(raw);
	}

	private void validateRuleName(Map<String, Object> merged) {
		Object v = merged.get("rule_name");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			throw new BadRequestException("规则名称必填且不能超过100个字符");
		}
		String s = String.valueOf(v).trim();
		if (s.length() > 100) {
			throw new BadRequestException("规则名称必填且不能超过100个字符");
		}
	}

	private void validateDescription(Map<String, Object> merged) {
		if (!merged.containsKey("description") || merged.get("description") == null) {
			return;
		}
		Object v = merged.get("description");
		if (!(v instanceof String)) {
			throw new BadRequestException("人群说明不能超过255个字符");
		}
		String s = (String) v;
		if (s.length() > 255) {
			throw new BadRequestException("人群说明不能超过255个字符");
		}
	}

	private void validateConditionShape(JsonNode conditionJson) {
		if (conditionJson == null || conditionJson.isNull() || !conditionJson.isArray()) {
			throw new BadRequestException("规则配置必填且必须是数组");
		}
	}

	private void validateConditionNonEmpty(JsonNode conditionJson) {
		if (conditionJson.size() == 0) {
			throw new BadRequestException("规则配置不能为空");
		}
	}

	private List<Long> parseTagIds(Map<String, Object> merged) {
		Object raw = merged.get("tag_ids");
		if (raw == null) {
			throw new BadRequestException("标签ID数组必填且必须是数组");
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> col) {
			for (Object el : col) {
				Long id = toNullableLong(el);
				if (id == null) {
					throw new BadRequestException("标签ID数组必填且必须是数组");
				}
				out.add(id);
			}
		} else {
			JsonNode n = objectMapper.valueToTree(raw);
			if (!n.isArray()) {
				throw new BadRequestException("标签ID数组必填且必须是数组");
			}
			for (JsonNode el : n) {
				if (el.isNumber()) {
					out.add(el.longValue());
				} else if (el.isTextual()) {
					try {
						out.add(Long.parseLong(el.asText().trim()));
					} catch (NumberFormatException e) {
						throw new BadRequestException("标签ID数组必填且必须是数组");
					}
				} else {
					throw new BadRequestException("标签ID数组必填且必须是数组");
				}
			}
		}
		if (out.isEmpty()) {
			throw new BadRequestException("标签ID数组必填且必须是数组");
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

	private static int resolveStatus(Map<String, Object> merged) {
		if (!merged.containsKey("status") || merged.get("status") == null) {
			return 1;
		}
		return looseIntFromObject(merged.get("status"));
	}

	private static int looseIntFromObject(Object o) {
		String s = String.valueOf(o).trim();
		try {
			return (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private long resolveDistributorId(String operatorType, long jwtDistributorId, Map<String, Object> merged) {
		if (Objects.equals(operatorType, "distributor")) {
			return jwtDistributorId > 0 ? jwtDistributorId : 0L;
		}
		if (merged.containsKey("distributor_id") && merged.get("distributor_id") != null) {
			return looseIntFromObjectToLong(merged.get("distributor_id"));
		}
		return 0L;
	}

	private static long looseIntFromObjectToLong(Object o) {
		String s = String.valueOf(o).trim();
		try {
			return (long) (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
