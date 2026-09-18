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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagV2FailException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagBatchCoverService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final MembersMapper membersMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;

	public OpenapiThirdApiV2MemberTagBatchCoverService(
			MembersMapper membersMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService) {
		this.membersMapper = membersMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
	}

	public Map<String, Object> executeOpenapiBatchCoverMemberTags(
			long companyId, String mobilesRaw, String tagIdsRaw) {
		if (!StringUtils.hasText(mobilesRaw)) {
			throw missingParams("会员手机号必填");
		}

		List<String> mobiles = parseJsonStringArray(mobilesRaw, "会员手机号格式错误");
		List<Object> tagIdInputs = parseJsonTagIdArray(tagIdsRaw, "会员标签ID格式错误");

		List<Long> userIds = new ArrayList<>();
		if (!mobiles.isEmpty()) {
			List<Members> memberRows =
					membersMapper.selectList(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.in(Members::getMobile, mobiles));
			Map<String, Long> mobileToUserId = new HashMap<>();
			for (Members member : memberRows) {
				if (member.getMobile() != null) {
					mobileToUserId.put(member.getMobile(), member.getUserId());
				}
			}
			for (String mobile : mobiles) {
				if (!mobileToUserId.containsKey(mobile)) {
					throw memberNotFound(mobile);
				}
				userIds.add(mobileToUserId.get(mobile));
			}
		}

		List<Long> resolvedTagIds = List.of();
		if (!tagIdInputs.isEmpty()) {
			List<MemberTags> tagRows =
					memberTagsMapper.selectList(
							new LambdaQueryWrapper<MemberTags>()
									.eq(MemberTags::getCompanyId, companyId)
									.in(MemberTags::getTagId, toLongListForQuery(tagIdInputs)));
			resolvedTagIds =
					tagRows.stream()
							.map(MemberTags::getTagId)
							.filter(Objects::nonNull)
							.toList();

			for (Object tagIdInput : tagIdInputs) {
				if (!looseContainsTagId(resolvedTagIds, tagIdInput)) {
					throw tagNotFound(tagIdInput);
				}
			}
		}

		for (Long userId : userIds) {
			memberRelTagsBatchCreateService.createRelTagsByUserId(userId, resolvedTagIds, companyId);
		}
		return Map.of("status", true);
	}

	private static List<String> parseJsonStringArray(String raw, String formatErrorMessage) {
		JsonNode root = parseJsonArrayRoot(raw, formatErrorMessage);
		List<String> result = new ArrayList<>();
		for (JsonNode node : root) {
			if (node.isArray() || node.isObject()) {
				throw memberTagError(formatErrorMessage);
			}
			result.add(node.asText());
		}
		return result;
	}

	private static List<Object> parseJsonTagIdArray(String raw, String formatErrorMessage) {
		if (raw == null || !StringUtils.hasText(raw)) {
			throw memberTagError(formatErrorMessage);
		}
		JsonNode root = parseJsonArrayRoot(raw, formatErrorMessage);
		List<Object> result = new ArrayList<>();
		for (JsonNode node : root) {
			if (node.isArray() || node.isObject()) {
				throw memberTagError(formatErrorMessage);
			}
			if (node.isIntegralNumber()) {
				result.add(node.longValue());
			} else if (node.isFloatingPointNumber()) {
				result.add(node.asText());
			} else if (node.isTextual()) {
				result.add(node.asText());
			} else if (node.isBoolean()) {
				result.add(node.asText());
			} else if (node.isNull()) {
				result.add("");
			} else {
				result.add(node.asText());
			}
		}
		return result;
	}

	private static JsonNode parseJsonArrayRoot(String raw, String formatErrorMessage) {
		try {
			JsonNode root = JSON.readTree(raw.trim());
			if (!root.isArray()) {
				throw memberTagError(formatErrorMessage);
			}
			return root;
		} catch (OpenapiMemberTagV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw memberTagError(formatErrorMessage);
		}
	}

	private static List<Long> toLongListForQuery(List<Object> inputs) {
		List<Long> result = new ArrayList<>();
		for (Object input : inputs) {
			Long parsed = toLongLoose(input);
			result.add(parsed != null ? parsed : 0L);
		}
		return result;
	}

	private static boolean looseContainsTagId(List<Long> dbTagIds, Object input) {
		for (Long dbTagId : dbTagIds) {
			if (Objects.equals(toLongLoose(input), dbTagId)
					|| String.valueOf(input).equals(String.valueOf(dbTagId))) {
				return true;
			}
		}
		return false;
	}

	private static Long toLongLoose(Object input) {
		if (input == null) {
			return null;
		}
		if (input instanceof Long l) {
			return l;
		}
		if (input instanceof Integer i) {
			return i.longValue();
		}
		if (input instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(input).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static OpenapiMemberTagV2FailException missingParams(String message) {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberTagV2FailException memberTagError(String message) {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.MEMBER_TAG_ERROR, message);
	}

	private static OpenapiMemberTagV2FailException memberNotFound(String mobile) {
		return new OpenapiMemberTagV2FailException(
				OpenapiErrorCode.MEMBER_NOT_FOUND, mobile + "会员不存在");
	}

	private static OpenapiMemberTagV2FailException tagNotFound(Object tagId) {
		return new OpenapiMemberTagV2FailException(
				OpenapiErrorCode.MEMBER_TAG_NOT_FOUND, String.valueOf(tagId) + "标签不存在");
	}
}
