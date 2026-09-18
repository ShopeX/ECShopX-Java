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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagUpdateService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagsMultiLangWriteService memberTagsMultiLangWriteService;

	public OpenapiThirdApiV2MemberTagUpdateService(
			MemberTagsMapper memberTagsMapper,
			MemberTagsMultiLangWriteService memberTagsMultiLangWriteService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagsMultiLangWriteService = memberTagsMultiLangWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiUpdateTag(
			long companyId,
			String tagIdRaw,
			String tagNameRaw,
			String descriptionRaw,
			boolean descriptionParamPresent,
			String tagColorRaw,
			String fontColorRaw,
			String categoryIdRaw,
			boolean categoryIdParamPresent) {
		if (!StringUtils.hasText(tagIdRaw)) {
			throw missingParams("标签ID必填");
		}
		String tagIdTrimmed = tagIdRaw.trim();

		if (!StringUtils.hasText(tagNameRaw)) {
			throw missingParams("标签名称必填");
		}
		String tagName = tagNameRaw.trim();

		if (!StringUtils.hasText(tagColorRaw)) {
			throw missingParams("标签颜色必填");
		}
		String tagColor = tagColorRaw.trim();

		if (!StringUtils.hasText(fontColorRaw)) {
			throw missingParams("标签字体颜色必填");
		}
		String fontColor = fontColorRaw.trim();

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

		MemberTags duplicate = memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagName, tagName)
						.ne(MemberTags::getTagId, tagIdLong)
						.last("LIMIT 1"));
		if (duplicate != null) {
			throw tagExist();
		}

		String description;
		if (!descriptionParamPresent) {
			description = null;
		} else {
			description = resolveNullableDescription(descriptionRaw);
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<MemberTags> updateWrapper = new LambdaUpdateWrapper<MemberTags>()
				.eq(MemberTags::getCompanyId, companyId)
				.eq(MemberTags::getTagId, tagIdLong)
				.set(MemberTags::getTagName, tagName)
				.set(MemberTags::getDescription, description)
				.set(MemberTags::getTagColor, tagColor)
				.set(MemberTags::getFontColor, fontColor)
				.set(MemberTags::getUpdated, nowSec);
		if (categoryIdParamPresent && phpTruthy(categoryIdRaw)) {
			updateWrapper.set(MemberTags::getCategoryId, (int) phpIntval(categoryIdRaw));
		}
		memberTagsMapper.update(null, updateWrapper);

		Map<String, Object> multiLangParams = new LinkedHashMap<>();
		multiLangParams.put("tag_name", tagName);
		memberTagsMultiLangWriteService.afterTagUpdate(
				tagIdLong, companyId, multiLangParams, "zh-CN");

		MemberTags updated = memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagId, tagIdLong)
						.last("LIMIT 1"));
		if (updated == null) {
			throw tagNotFound();
		}
		return formatOpenApiResponse(updated);
	}

	private static Long parseTagIdForQuery(String trimmed) {
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String resolveNullableDescription(String raw) {
		if (raw == null || !StringUtils.hasText(raw)) {
			return null;
		}
		return raw.trim();
	}

	private static boolean phpTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return false;
		}
		return !"0".equals(s);
	}

	private static long phpIntval(String raw) {
		if (raw == null || raw.isEmpty()) {
			return 0L;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		int i = 0;
		while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '-')) {
			i++;
		}
		if (i == 0 || (i == 1 && s.charAt(0) == '-')) {
			return 0L;
		}
		try {
			return Long.parseLong(s.substring(0, i));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> formatOpenApiResponse(MemberTags entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("tag_id", entity.getTagId());
		row.put("tag_name", entity.getTagName());
		row.put("category_id", entity.getCategoryId());
		row.put("description", entity.getDescription());
		row.put("tag_color", entity.getTagColor());
		row.put("font_color", entity.getFontColor());
		row.put("created", formatEpochSeconds(entity.getCreated()));
		row.put("updated", formatEpochSeconds(entity.getUpdated()));
		return row;
	}

	private static String formatEpochSeconds(Long epoch) {
		if (epoch == null) {
			return null;
		}
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}

	private static OpenapiMemberTagV2FailException missingParams(String message) {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberTagV2FailException tagNotFound() {
		return new OpenapiMemberTagV2FailException(
				OpenapiErrorCode.MEMBER_TAG_NOT_FOUND, "该标签不存在");
	}

	private static OpenapiMemberTagV2FailException tagExist() {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.MEMBER_TAG_EXIST, "该标签名已存在");
	}
}
