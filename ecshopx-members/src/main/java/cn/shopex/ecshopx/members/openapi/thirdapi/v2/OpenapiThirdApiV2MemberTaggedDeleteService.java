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
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import cn.shopex.ecshopx.members.service.systemtag.MemberSystemTagWriteGuard;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTaggedDeleteService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final MembersMapper membersMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;
	private final MemberSystemTagWriteGuard memberSystemTagWriteGuard;

	public OpenapiThirdApiV2MemberTaggedDeleteService(
			MembersMapper membersMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService,
			MemberSystemTagWriteGuard memberSystemTagWriteGuard) {
		this.membersMapper = membersMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
		this.memberSystemTagWriteGuard = memberSystemTagWriteGuard;
	}

	public Map<String, Object> executeOpenapiDeleteMemberTagged(
			long companyId, String mobileRaw, String tagIdsRaw) {
		if (!StringUtils.hasText(mobileRaw)) {
			throw missingParams("会员手机号必填");
		}
		if (!StringUtils.hasText(tagIdsRaw)) {
			throw missingParams("会员标签ID必填");
		}

		List<Long> requestedTagIds = parseRequestedTagIds(tagIdsRaw, "会员标签ID格式错误");
		if (requestedTagIds.isEmpty()) {
			return Map.of("status", true);
		}
		memberSystemTagWriteGuard.rejectTagIds(companyId, requestedTagIds);

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

		memberRelTagsBatchCreateService.userRelTagDeleteNoPush(
				companyId, List.of(userId), requestedTagIds);
		return Map.of("status", true);
	}

	private static List<Long> parseRequestedTagIds(String raw, String formatErrorMessage) {
		try {
			JsonNode root = JSON.readTree(raw.trim());
			if (!root.isArray()) {
				throw memberTagError(formatErrorMessage);
			}
			List<Long> ids = new ArrayList<>();
			for (JsonNode node : root) {
				if (node.isArray() || node.isObject()) {
					throw memberTagError(formatErrorMessage);
				}
				long v;
				if (node.isIntegralNumber()) {
					v = node.longValue();
				} else {
					String s = node.asText();
					if (!StringUtils.hasText(s)) {
						throw memberTagError(formatErrorMessage);
					}
					v = Long.parseLong(s.trim());
				}
				if (v > 0L) {
					ids.add(v);
				}
			}
			return ids;
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
