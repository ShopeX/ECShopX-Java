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
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ItemsCategoryItemIdResolver {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;

	public ItemsCategoryItemIdResolver(ItemsCategoryRepository itemsCategoryRepository, ItemsRelCatsRepository itemsRelCatsRepository) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
	}

	/**
	 * main_cat_id 展开子类目 → item_category 列可用的 id 字符串列表。
	 */
	public List<String> expandMainCategoryIdsToItemCategoryKeys(long companyId, List<Long> mainCatIds) {
		if (mainCatIds == null || mainCatIds.isEmpty()) {
			return List.of();
		}
		Set<String> keys = new LinkedHashSet<>();
		for (Long rootId : mainCatIds) {
			if (rootId == null) {
				continue;
			}
			for (Long id : getMainCatChildIdsBy(companyId, rootId)) {
				keys.add(String.valueOf(id));
			}
			keys.add(String.valueOf(rootId));
		}
		return new ArrayList<>(keys);
	}

	/**
	 * 销售类目树 → item_id 列表。
	 */
	public List<Long> getItemIdsByCategoryTree(long companyId, long categoryId) {
		List<Long> treeIds = getItemsCategoryIds(companyId, categoryId);
		if (treeIds.isEmpty()) {
			return List.of();
		}
		List<ItemsRelCats> rows = itemsRelCatsRepository.listByCompanyIdAndCategoryIdIn(companyId, treeIds);
		return rows.stream().map(ItemsRelCats::getItemId).distinct().collect(Collectors.toList());
	}

	/**
	 * 主类目下子类目 id 顺序与 {@link #getMainCatChildIdsBy} 一致，末尾追加主类目 id 一次（不去重）。
	 */
	public List<Long> expandMainCategoryIdsForPointsmallExport(long companyId, long mainCatId) {
		List<Long> children = getMainCatChildIdsBy(companyId, mainCatId);
		ArrayList<Long> out = new ArrayList<>(children);
		out.add(mainCatId);
		return out;
	}

	private List<Long> getMainCatChildIdsBy(long companyId, long categoryId) {
		Optional<ItemsCategory> info = itemsCategoryRepository.findOneByCompanyIdAndCategoryIdAndIsMainCategory(companyId, categoryId, true);
		if (info.isEmpty()) {
			return List.of();
		}
		String path = info.get().getPath();
		if (path == null || path.isEmpty()) {
			return List.of(-1L);
		}
		String[] segments = path.split(",", -1);
		List<Long> mainCatIds;
		if (segments.length == 2) {
			mainCatIds = itemsCategoryRepository.listCategoryIdsByCompanyIdParentIdAndIsMainCategory(companyId, categoryId, true);
		} else {
			mainCatIds = itemsCategoryRepository.listCategoryIdsByPathLikeChildOf(companyId, categoryId);
		}
		return mainCatIds.isEmpty() ? List.of(-1L) : mainCatIds;
	}

	private List<Long> getItemsCategoryIds(long companyId, long categoryId) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		ids.add(categoryId);
		List<Long> level1 = itemsCategoryRepository.listCategoryIdsByParentId(companyId, categoryId);
		if (!level1.isEmpty()) {
			ids.addAll(level1);
			List<Long> level2 = itemsCategoryRepository.listCategoryIdsByParentIds(companyId, level1);
			if (!level2.isEmpty()) {
				ids.addAll(level2);
			}
		}
		return new ArrayList<>(ids);
	}
}
