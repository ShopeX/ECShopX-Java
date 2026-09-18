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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagCategoryCreateService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final TagsCategoryMapper tagsCategoryMapper;

	public OpenapiThirdApiV2MemberTagCategoryCreateService(TagsCategoryMapper tagsCategoryMapper) {
		this.tagsCategoryMapper = tagsCategoryMapper;
	}

	public Map<String, Object> executeOpenapiCreateTagCategory(
			long companyId, String categoryNameRaw, String sortRaw, boolean sortParamPresent) {
		if (!StringUtils.hasText(categoryNameRaw)) {
			throw missingParams("分类名称必填");
		}
		String categoryName = categoryNameRaw.trim();

		long sort = resolveSortPhpIntval(sortRaw, sortParamPresent);

		TagsCategory existing = tagsCategoryMapper.selectOne(
				new LambdaQueryWrapper<TagsCategory>()
						.eq(TagsCategory::getCompanyId, companyId)
						.eq(TagsCategory::getCategoryName, categoryName)
						.last("LIMIT 1"));
		if (existing != null) {
			throw tagCategoryExist();
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		TagsCategory entity = new TagsCategory();
		entity.setCategoryName(categoryName);
		entity.setCompanyId(companyId);
		entity.setSort(sort);
		entity.setCreated(now);
		entity.setUpdated(now);
		tagsCategoryMapper.insert(entity);

		return formatOpenApiResponse(entity);
	}

	private static long resolveSortPhpIntval(String sortRaw, boolean sortParamPresent) {
		if (!sortParamPresent) {
			return 1L;
		}
		if (sortRaw == null) {
			return 1L;
		}
		return phpIntval(sortRaw);
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

	private static OpenapiMemberTagCategoryV2FailException tagCategoryExist() {
		return new OpenapiMemberTagCategoryV2FailException(
				OpenapiErrorCode.MEMBER_TAGCATEGORY_EXIST, "该分类名已存在");
	}
}
