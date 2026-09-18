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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagCreateService {

	private static final String DEFAULT_TAG_COLOR = "rgba(255, 25, 57, 1)";
	private static final String DEFAULT_FONT_COLOR = "rgba(255, 255, 255, 1)";
	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagsMultiLangWriteService memberTagsMultiLangWriteService;

	public OpenapiThirdApiV2MemberTagCreateService(
			MemberTagsMapper memberTagsMapper,
			MemberTagsMultiLangWriteService memberTagsMultiLangWriteService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagsMultiLangWriteService = memberTagsMultiLangWriteService;
	}

	public Map<String, Object> executeOpenapiCreateTag(
			long companyId,
			String tagNameRaw,
			String categoryIdRaw,
			String descriptionRaw,
			String tagColorRaw,
			String fontColorRaw) {
		if (!StringUtils.hasText(tagNameRaw)) {
			throw missingParams("分类名称必填");
		}
		String tagName = tagNameRaw.trim();

		int categoryId = parseCategoryId(categoryIdRaw);
		String description = resolveNullableDescription(descriptionRaw);
		String tagColor = resolvePhpElvisColor(tagColorRaw, DEFAULT_TAG_COLOR);
		String fontColor = resolvePhpElvisColor(fontColorRaw, DEFAULT_FONT_COLOR);

		MemberTags existing = memberTagsMapper.selectOne(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getTagName, tagName)
						.last("LIMIT 1"));
		if (existing != null) {
			throw tagExist();
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		MemberTags entity = new MemberTags();
		entity.setCompanyId(companyId);
		entity.setTagName(tagName);
		entity.setCategoryId(categoryId);
		entity.setDescription(description);
		entity.setTagColor(tagColor);
		entity.setFontColor(fontColor);
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);
		memberTagsMapper.insert(entity);

		Map<String, Object> multiLangParams = new LinkedHashMap<>();
		multiLangParams.put("tag_name", tagName);
		memberTagsMultiLangWriteService.afterTagCreate(
				entity.getTagId(), companyId, multiLangParams, "zh-CN");

		return formatOpenApiResponse(entity);
	}

	private static String resolvePhpElvisColor(String raw, String defaultColor) {
		if (raw == null) {
			return defaultColor;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty() || "0".equals(trimmed)) {
			return defaultColor;
		}
		return trimmed;
	}

	private static int parseCategoryId(String raw) {
		if (raw == null || !StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String resolveNullableDescription(String raw) {
		if (raw == null || !StringUtils.hasText(raw)) {
			return null;
		}
		return raw.trim();
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

	private static OpenapiMemberTagV2FailException tagExist() {
		return new OpenapiMemberTagV2FailException(OpenapiErrorCode.MEMBER_TAG_EXIST, "该标签名已存在");
	}
}
