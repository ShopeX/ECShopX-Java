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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.members.domain.TagsCategory;
import cn.shopex.ecshopx.members.mapper.TagsCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TagsCategoryListService {

	private final TagsCategoryMapper tagsCategoryMapper;

	public TagsCategoryListService(TagsCategoryMapper tagsCategoryMapper) {
		this.tagsCategoryMapper = tagsCategoryMapper;
	}

	public Object getTagsCategoryInfo(long companyId, String categoryIdPath) {
		String raw = categoryIdPath == null ? "" : categoryIdPath.trim();
		if (raw.isEmpty()) {
			return List.of();
		}
		long categoryIdLong;
		try {
			categoryIdLong = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			return List.of();
		}
		LambdaQueryWrapper<TagsCategory> w = new LambdaQueryWrapper<TagsCategory>()
				.eq(TagsCategory::getCompanyId, companyId)
				.eq(TagsCategory::getCategoryId, categoryIdLong);
		TagsCategory row = tagsCategoryMapper.selectOne(w);
		if (row == null) {
			return List.of();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("category_id", row.getCategoryId());
		m.put("category_name", row.getCategoryName());
		m.put("sort", row.getSort() == null ? Long.valueOf(1L) : row.getSort());
		m.put("company_id", row.getCompanyId());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}

	public Map<String, Object> getTagsCategoryList(long companyId, int page, int pageSize) {
		LambdaQueryWrapper<TagsCategory> countW =
				new LambdaQueryWrapper<TagsCategory>().eq(TagsCategory::getCompanyId, companyId);
		LambdaQueryWrapper<TagsCategory> listW = new LambdaQueryWrapper<TagsCategory>()
				.eq(TagsCategory::getCompanyId, companyId)
				.select(TagsCategory::getCategoryName, TagsCategory::getCategoryId, TagsCategory::getSort);

		long total = tagsCategoryMapper.selectCount(countW);
		int totalCount = (int) Math.min(Integer.MAX_VALUE, total);

		List<Map<String, Object>> list;
		if (total == 0) {
			list = List.of();
		} else if (pageSize <= 0) {
			List<TagsCategory> entities = tagsCategoryMapper.selectList(listW);
			list = entities.stream().map(TagsCategoryListService::toListRow).toList();
		} else {
			Page<TagsCategory> mpPage = new Page<>((long) page, (long) pageSize, false);
			tagsCategoryMapper.selectPage(mpPage, listW);
			List<TagsCategory> entities = mpPage.getRecords();
			list = entities.stream().map(TagsCategoryListService::toListRow).toList();
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	private static Map<String, Object> toListRow(TagsCategory e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("category_id", String.valueOf(e.getCategoryId()));
		m.put("category_name", e.getCategoryName());
		m.put("sort", e.getSort() == null ? Integer.valueOf(1) : e.getSort().intValue());
		return m;
	}
}
