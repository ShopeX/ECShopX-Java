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
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTaggedDeleteService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final MembersMapper membersMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;

	public OpenapiThirdApiV2MemberTaggedDeleteService(
			MembersMapper membersMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService) {
		this.membersMapper = membersMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
	}

	public Map<String, Object> executeOpenapiDeleteMemberTagged(
			long companyId, String mobileRaw, String tagIdsRaw) {
		if (!StringUtils.hasText(mobileRaw)) {
			throw missingParams("会员手机号必填");
		}
		if (!StringUtils.hasText(tagIdsRaw)) {
			throw missingParams("会员标签ID必填");
		}

		parseJsonArrayRootOnly(tagIdsRaw, "会员标签ID格式错误");

		Members member =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getMobile, mobileRaw.trim())
								.last("LIMIT 1"));
		Long userId = member != null ? member.getUserId() : null;
		if (userId == null || userId <= 0) {
			throw memberNotFoundDefault();
		}

		List<MemberRelTags> relRows =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getUserId, userId));
		List<Long> relTagIds =
				relRows.stream()
						.map(MemberRelTags::getTagId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		if (relTagIds.isEmpty()) {
			return Map.of("status", true);
		}

		List<MemberTags> tagRows =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.in(MemberTags::getTagId, relTagIds));
		List<Long> resolvedTagIds =
				tagRows.stream()
						.map(MemberTags::getTagId)
						.filter(Objects::nonNull)
						.toList();

		if (resolvedTagIds.isEmpty()) {
			return Map.of("status", true);
		}

		memberRelTagsBatchCreateService.userRelTagDeleteNoPush(
				companyId, List.of(userId), resolvedTagIds);
		return Map.of("status", true);
	}

	private static void parseJsonArrayRootOnly(String raw, String formatErrorMessage) {
		try {
			JsonNode root = JSON.readTree(raw.trim());
			if (!root.isArray()) {
				throw memberTagError(formatErrorMessage);
			}
		} catch (OpenapiMemberTagV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw memberTagError(formatErrorMessage);
		}
	}

	private static OpenapiMemberTagV2FailException missingParams(String message) {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberTagV2FailException memberTagError(String message) {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.MEMBER_TAG_ERROR, message);
	}

	private static OpenapiMemberTagV2FailException memberNotFoundDefault() {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
	}
}
