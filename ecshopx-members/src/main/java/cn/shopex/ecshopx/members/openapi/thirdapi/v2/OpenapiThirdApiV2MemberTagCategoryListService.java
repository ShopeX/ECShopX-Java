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

import cn.shopex.ecshopx.members.domain.TagsCategory;
import cn.shopex.ecshopx.members.mapper.TagsCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberTagCategoryListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final TagsCategoryMapper tagsCategoryMapper;

	public OpenapiThirdApiV2MemberTagCategoryListService(TagsCategoryMapper tagsCategoryMapper) {
		this.tagsCategoryMapper = tagsCategoryMapper;
	}

	public Map<String, Object> executeOpenapiGetTagCategoryList(
			long companyId, int page, int pageSize, String categoryNameRaw) {
		LambdaQueryWrapper<TagsCategory> baseFilter = new LambdaQueryWrapper<TagsCategory>()
				.eq(TagsCategory::getCompanyId, companyId);
		if (phpTruthyCategoryName(categoryNameRaw)) {
			baseFilter.like(TagsCategory::getCategoryName, categoryNameRaw);
		}

		long totalCount = tagsCategoryMapper.selectCount(baseFilter);

		LambdaQueryWrapper<TagsCategory> listWrapper = new LambdaQueryWrapper<TagsCategory>()
				.eq(TagsCategory::getCompanyId, companyId)
				.select(
						TagsCategory::getCategoryName,
						TagsCategory::getCategoryId,
						TagsCategory::getSort,
						TagsCategory::getCreated,
						TagsCategory::getUpdated);
		if (phpTruthyCategoryName(categoryNameRaw)) {
			listWrapper.like(TagsCategory::getCategoryName, categoryNameRaw);
		}

		Page<TagsCategory> pageRequest = new Page<>(page, pageSize, false);
		tagsCategoryMapper.selectPage(pageRequest, listWrapper);
		List<TagsCategory> entities = pageRequest.getRecords();

		List<Map<String, Object>> list = entities.stream()
				.map(OpenapiThirdApiV2MemberTagCategoryListService::formatOpenApiListRow)
				.toList();
		return formatListStruct(totalCount, list, page, pageSize);
	}

	private static boolean phpTruthyCategoryName(String raw) {
		if (raw == null) {
			return false;
		}
		if (raw.isEmpty()) {
			return false;
		}
		return !"0".equals(raw);
	}

	private static Map<String, Object> formatOpenApiListRow(TagsCategory entity) {
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

	private static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}
}
