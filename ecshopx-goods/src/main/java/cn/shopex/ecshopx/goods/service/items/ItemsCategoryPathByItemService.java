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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryRowMaps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 按类目 id 构建与列表/详情接口一致的商品分类树片段。
 */
@Service
public class ItemsCategoryPathByItemService {

	private final ItemsCategoryRepository itemsCategoryRepository;

	public ItemsCategoryPathByItemService(ItemsCategoryRepository itemsCategoryRepository) {
		this.itemsCategoryRepository = itemsCategoryRepository;
	}

	public List<Map<String, Object>> getCategoryPathById(long companyId, long categoryId, boolean requireMainCategory) {
		if (categoryId <= 0) {
			return List.of();
		}
		ItemsCategory anchor = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, categoryId).orElse(null);
		if (anchor == null) {
			return List.of();
		}
		if (requireMainCategory && !Boolean.TRUE.equals(anchor.getIsMainCategory())) {
			return List.of();
		}

		Set<Long> loadIds = new LinkedHashSet<>();
		String p = anchor.getPath();
		if (StringUtils.hasText(p)) {
			for (String part : p.split(",")) {
				if (StringUtils.hasText(part)) {
					try {
						loadIds.add(Long.parseLong(part.trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		loadIds.add(categoryId);

		Map<Long, ItemsCategory> byId = new LinkedHashMap<>();
		for (ItemsCategory c : itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, loadIds)) {
			byId.put(c.getCategoryId(), c);
		}
		for (ItemsCategory c : itemsCategoryRepository.listByCompanyAndParentId(companyId, categoryId)) {
			byId.putIfAbsent(c.getCategoryId(), c);
		}

		List<Map<String, Object>> nodes = byId.values().stream().map(ItemsCategoryRowMaps::fromListEntity).collect(Collectors.toList());
		return buildTree(nodes, 0L, 0, true);
	}

	private List<Map<String, Object>> buildTree(List<Map<String, Object>> nodes, long parentId, int level, boolean showChildren) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> raw : nodes) {
			long pid = parseLong(raw.get("parent_id"));
			if (pid != parentId) {
				continue;
			}
			Map<String, Object> v = new LinkedHashMap<>(raw);
			int catLevel = parseInt(v.get("category_level"));
			List<Map<String, Object>> children = buildTree(nodes, parseLong(v.get("category_id")), level + 1, showChildren);
			if (catLevel == 3) {
				v.remove("children");
			} else if (!children.isEmpty()) {
				v.put("children", children);
			} else {
				v.put("children", List.of());
				if (!showChildren && isZeroOrFalseMain(v.get("is_main_category"))) {
					v.remove("children");
				}
			}
			v.put("level", level);
			out.add(v);
		}
		return out;
	}

	private static boolean isZeroOrFalseMain(Object isMain) {
		if (isMain == null) {
			return true;
		}
		String s = isMain.toString().trim();
		return "0".equals(s) || "false".equalsIgnoreCase(s);
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
