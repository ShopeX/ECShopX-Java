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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerTags;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * H5 方案标签列表：查询未禁用的设计师标签，按标签类目分组；每组包含类目维度字段及子数组 {@code tags}（元素为 {@code tag_id}、{@code tag_name}）。
 */
@Service
public class KujialeDesignerTagsH5ListService {

	private final KujialeDesignerTagsMapper kujialeDesignerTagsMapper;

	public KujialeDesignerTagsH5ListService(KujialeDesignerTagsMapper kujialeDesignerTagsMapper) {
		this.kujialeDesignerTagsMapper = kujialeDesignerTagsMapper;
	}

	public List<Map<String, Object>> getTagsListTree() {
		LambdaQueryWrapper<KujialeDesignerTags> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(KujialeDesignerTags::getIsDisabled, 0)
				.orderByAsc(KujialeDesignerTags::getTagCategoryId)
				.orderByAsc(KujialeDesignerTags::getId);
		List<KujialeDesignerTags> list = kujialeDesignerTagsMapper.selectList(wrapper);
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		LinkedHashMap<String, Map<String, Object>> byCategory = new LinkedHashMap<>();
		for (KujialeDesignerTags row : list) {
			String categoryKey = row.getTagCategoryId() == null ? "" : row.getTagCategoryId();
			Map<String, Object> tagEntry = new LinkedHashMap<>();
			tagEntry.put("tag_id", row.getTagId());
			tagEntry.put("tag_name", row.getTagName());

			Map<String, Object> root = byCategory.get(categoryKey);
			if (root != null && !root.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> tags = (List<Map<String, Object>>) root.get("tags");
				tags.add(tagEntry);
			} else {
				Map<String, Object> newRoot = new LinkedHashMap<>();
				newRoot.put("id", row.getId());
				newRoot.put("tag_category_id", row.getTagCategoryId());
				newRoot.put("tag_category_name", row.getTagCategoryName());
				newRoot.put("type", row.getType());
				newRoot.put("is_multiple_selected", row.getIsMultipleSelected());
				newRoot.put("is_disabled", row.getIsDisabled());
				newRoot.put("created", row.getCreated());
				newRoot.put("updated", row.getUpdated());
				List<Map<String, Object>> tags = new ArrayList<>();
				tags.add(tagEntry);
				newRoot.put("tags", tags);
				byCategory.put(categoryKey, newRoot);
			}
		}
		return new ArrayList<>(byCategory.values());
	}
}
