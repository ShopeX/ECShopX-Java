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
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.service.MemberTagsMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagDeleteService {

	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagsMultiLangWriteService memberTagsMultiLangWriteService;

	public OpenapiThirdApiV2MemberTagDeleteService(
			MemberTagsMapper memberTagsMapper,
			MemberTagsMultiLangWriteService memberTagsMultiLangWriteService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagsMultiLangWriteService = memberTagsMultiLangWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiDeleteTag(long companyId, String tagIdRaw) {
		if (!StringUtils.hasText(tagIdRaw)) {
			throw missingParams("标签ID必填");
		}
		String tagIdTrimmed = tagIdRaw.trim();

		Long tagIdLong = parseTagIdForQuery(tagIdTrimmed);
		if (tagIdLong == null) {
			throw tagNotFound();
		}
		MemberTags existing = memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagId, tagIdLong)
						.last("LIMIT 1"));
		if (existing == null) {
			throw tagNotFound();
		}

		Long tagId = existing.getTagId();
		memberTagsMultiLangWriteService.deleteMultiLangRowsForMembersTags(tagId);

		LambdaQueryWrapper<MemberTags> deleteWrapper =
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagId, tagId);
		memberTagsMapper.delete(deleteWrapper);

		Map<String, Object> data = new LinkedHashMap<>(1);
		data.put("status", Boolean.TRUE);
		return data;
	}

	private static Long parseTagIdForQuery(String trimmed) {
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static OpenapiMemberTagV2FailException missingParams(String message) {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberTagV2FailException tagNotFound() {
		return new OpenapiMemberTagV2FailException(
				OpenapiErrorCode.MEMBER_TAG_NOT_FOUND, "该标签不存在");
	}
}
