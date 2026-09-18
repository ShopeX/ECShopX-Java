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
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagCategoryV2FailException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.TagsCategory;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.TagsCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagCategoryDeleteService {

	private final TagsCategoryMapper tagsCategoryMapper;
	private final MemberTagsMapper memberTagsMapper;

	public OpenapiThirdApiV2MemberTagCategoryDeleteService(
			TagsCategoryMapper tagsCategoryMapper, MemberTagsMapper memberTagsMapper) {
		this.tagsCategoryMapper = tagsCategoryMapper;
		this.memberTagsMapper = memberTagsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiDeleteTagCategory(long companyId, String categoryIdRaw) {
		if (!StringUtils.hasText(categoryIdRaw)) {
			throw missingParams("分类ID必填");
		}
		String categoryIdTrimmed = categoryIdRaw.trim();

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

		LambdaQueryWrapper<TagsCategory> deleteWrapper =
				new LambdaQueryWrapper<TagsCategory>()
						.eq(TagsCategory::getCompanyId, companyId)
						.eq(TagsCategory::getCategoryId, categoryIdLong);
		tagsCategoryMapper.delete(deleteWrapper);

		int categoryIdInt;
		try {
			categoryIdInt = Math.toIntExact(categoryIdLong);
		} catch (ArithmeticException e) {
			throw tagCategoryNotFound();
		}
		Long tagCount = memberTagsMapper.selectCount(
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getCategoryId, categoryIdInt));
		if (tagCount != null && tagCount > 0) {
			throw new OpenapiLegacyZeroCodeFailException("删除失败,该分类下已有标签");
		}

		Map<String, Object> data = new LinkedHashMap<>(1);
		data.put("status", Boolean.TRUE);
		return data;
	}

	private static Long parseCategoryIdForQuery(String trimmed) {
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static OpenapiMemberTagCategoryV2FailException missingParams(String message) {
		return new OpenapiMemberTagCategoryV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberTagCategoryV2FailException tagCategoryNotFound() {
		return new OpenapiMemberTagCategoryV2FailException(
				OpenapiErrorCode.MEMBER_TAGCATEGORY_NOT_FOUND, "该标签分类不存在");
	}
}
