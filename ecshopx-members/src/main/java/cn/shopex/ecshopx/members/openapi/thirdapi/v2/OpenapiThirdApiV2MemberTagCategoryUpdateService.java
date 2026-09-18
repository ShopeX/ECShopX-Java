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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryV2FailException;
import cn.shopex.ecshopx.members.domain.TagsCategory;
import cn.shopex.ecshopx.members.mapper.TagsCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagCategoryUpdateService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final TagsCategoryMapper tagsCategoryMapper;

	public OpenapiThirdApiV2MemberTagCategoryUpdateService(TagsCategoryMapper tagsCategoryMapper) {
		this.tagsCategoryMapper = tagsCategoryMapper;
	}

	public Map<String, Object> executeOpenapiUpdateTagCategory(
			long companyId,
			String categoryIdRaw,
			String categoryNameRaw,
			String sortRaw,
			boolean sortParamPresent) {
		if (!StringUtils.hasText(categoryIdRaw)) {
			throw missingParams("分类ID必填");
		}
		String categoryIdTrimmed = categoryIdRaw.trim();

		if (!StringUtils.hasText(categoryNameRaw)) {
			throw missingParams("分类名称必填");
		}
		String categoryName = categoryNameRaw.trim();

		Long categoryIdLong = parseCategoryIdForQuery(categoryIdTrimmed);
		if (categoryIdLong == null) {
			throw tagCategoryNotFound();
		}
		TagsCategory existing = tagsCategoryMapper.selectOne(
				new LambdaQueryWrapper<TagsCategory>()
						.eq(TagsCategory::getCompanyId, companyId)
						.eq(TagsCategory::getCategoryId, categoryIdLong)
						.last("LIMIT 1"));
		if (existing == null) {
			throw tagCategoryNotFound();
		}

		TagsCategory duplicate = tagsCategoryMapper.selectOne(
				new LambdaQueryWrapper<TagsCategory>()
						.eq(TagsCategory::getCompanyId, companyId)
						.eq(TagsCategory::getCategoryName, categoryName)
						.ne(TagsCategory::getCategoryId, categoryIdLong)
						.last("LIMIT 1"));
		if (duplicate != null) {
			throw tagCategoryExist();
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<TagsCategory> updateWrapper = new LambdaUpdateWrapper<TagsCategory>()
				.eq(TagsCategory::getCompanyId, companyId)
				.eq(TagsCategory::getCategoryId, categoryIdLong)
				.set(TagsCategory::getCategoryName, categoryName)
				.set(TagsCategory::getUpdated, now);
		if (sortParamPresent && phpTruthy(sortRaw)) {
			updateWrapper.set(TagsCategory::getSort, phpIntval(sortRaw));
		}
		tagsCategoryMapper.update(null, updateWrapper);

		TagsCategory updated = tagsCategoryMapper.selectOne(
				new LambdaQueryWrapper<TagsCategory>()
						.eq(TagsCategory::getCompanyId, companyId)
						.eq(TagsCategory::getCategoryId, categoryIdLong)
						.last("LIMIT 1"));
		if (updated == null) {
			throw tagCategoryNotFound();
		}
		return formatOpenApiResponse(updated);
	}

	private static Long parseCategoryIdForQuery(String trimmed) {
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
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

	private static Map<String, Object> formatOpenApiResponse(TagsCategory entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("category_id", entity.getCategoryId());
		row.put("category_name", entity.getCategoryName());
		row.put("sort", entity.getSort());
		row.put("created", formatEpochSeconds(entity.getCreated()));
		row.put("updated", formatEpochSeconds(entity.getUpdated()));
		return row;
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null) {
			return null;
		}
		return Instant.ofEpochSecond(epoch.longValue()).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}

	private static OpenapiMemberTagCategoryV2FailException missingParams(String message) {
		return new OpenapiMemberTagCategoryV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberTagCategoryV2FailException tagCategoryNotFound() {
		return new OpenapiMemberTagCategoryV2FailException(
				OpenapiErrorCode.MEMBER_TAGCATEGORY_NOT_FOUND, "该标签分类不存在");
	}

	private static OpenapiMemberTagCategoryV2FailException tagCategoryExist() {
		return new OpenapiMemberTagCategoryV2FailException(
				OpenapiErrorCode.MEMBER_TAGCATEGORY_EXIST, "该分类名已存在");
	}
}
