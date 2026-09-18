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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsCategoryDeleteService {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsQueryRepository itemsQueryRepository;

	public ItemsCategoryDeleteService(ItemsCategoryRepository itemsCategoryRepository,
			ItemsRelCatsRepository itemsRelCatsRepository, ItemsQueryRepository itemsQueryRepository) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsQueryRepository = itemsQueryRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteItemsCategory(long companyId, long categoryId) {
		List<Long> ids = getItemIdsByCatId(companyId, categoryId);
		if (!ids.isEmpty()) {
			long n = itemsQueryRepository.countByCompanyAndItemIdIn(companyId, ids);
			if (n > 0) {
				throw new ResourceException("分类下存在商品");
			}
		}

		List<Long> mainCatIds = getMainCatChildIdsBy(companyId, categoryId);
		List<Long> mainCatIdsForItemCheck = new ArrayList<>(mainCatIds.size() + 1);
		mainCatIdsForItemCheck.addAll(mainCatIds);
		mainCatIdsForItemCheck.add(categoryId);

		long itemTotalCount = itemsQueryRepository.countByCompanyAndItemCategoryIn(companyId, mainCatIdsForItemCheck);
		if (itemTotalCount > 0) {
			throw new ResourceException("类目下存在商品");
		}

		if (!ids.isEmpty()) {
			itemsRelCatsRepository.deleteByCompanyIdAndCategoryIdAndItemIdIn(companyId, categoryId, ids);
		}

		itemsCategoryRepository.deleteByCategoryIdAndCompanyId(categoryId, companyId);
		boolean result = true;
		boolean resultChild = true;
		boolean resultChildSun = true;

		Optional<ItemsCategory> directChildProbe = itemsCategoryRepository.findOneByCompanyIdAndParentId(companyId, categoryId);
		if (directChildProbe.isPresent()) {
			List<ItemsCategory> resultChildList = itemsCategoryRepository.listEntitiesByCompanyIdAndParentId(companyId, categoryId);
			itemsCategoryRepository.deleteByParentIdAndCompanyId(categoryId, companyId);
			resultChild = true;
			if (!resultChildList.isEmpty()) {
				for (ItemsCategory v : resultChildList) {
					List<ItemsCategory> resultChildSunList = itemsCategoryRepository
							.listEntitiesByCompanyIdAndParentId(companyId, v.getCategoryId());
					if (!resultChildSunList.isEmpty()) {
						itemsCategoryRepository.deleteByParentIdAndCompanyId(v.getCategoryId(), companyId);
						resultChildSun = true;
					}
				}
			}
		}

		if (result && resultChild && resultChildSun) {
			return;
		}
		throw new ResourceException("删除失败");
	}

	private List<Long> getItemIdsByCatId(long companyId, long categoryId) {
		List<Long> treeIds = getItemsCategoryIds(companyId, categoryId);
		if (treeIds.isEmpty()) {
			return List.of();
		}
		List<ItemsRelCats> rows = itemsRelCatsRepository.listByCompanyIdAndCategoryIdIn(companyId, treeIds);
		return rows.stream().map(ItemsRelCats::getItemId).distinct().collect(Collectors.toList());
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

	private List<Long> getMainCatChildIdsBy(long companyId, long categoryId) {
		Optional<ItemsCategory> info = itemsCategoryRepository.findOneByCompanyIdAndCategoryIdAndIsMainCategory(companyId,
				categoryId, true);
		if (info.isEmpty()) {
			return List.of();
		}
		String path = info.get().getPath();
		if (path == null || path.isEmpty()) {
			return List.of();
		}
		String[] segments = path.split(",", -1);
		if (segments.length == 2) {
			return itemsCategoryRepository.listCategoryIdsByCompanyIdParentIdAndIsMainCategory(companyId, categoryId, true);
		}
		return itemsCategoryRepository.listCategoryIdsByPathLikeChildOf(companyId, categoryId);
	}
}
