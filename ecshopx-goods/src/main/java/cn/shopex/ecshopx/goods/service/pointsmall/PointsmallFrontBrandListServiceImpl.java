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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PointsmallFrontBrandListServiceImpl implements PointsmallFrontBrandListService {

	private final ItemsRepository itemsRepository;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemsAttributesRepository itemsAttributesRepository;

	public PointsmallFrontBrandListServiceImpl(ItemsRepository itemsRepository,
			ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemsAttributesRepository itemsAttributesRepository) {
		this.itemsRepository = itemsRepository;
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemsAttributesRepository = itemsAttributesRepository;
	}

	@Override
	public Map<String, Object> getBrandList(PointsmallFrontBrandFilter filter) {
		long companyId = filter.companyId();
		List<Long> itemIds = new ArrayList<>();
		List<Long> mainCats = filter.itemCategoryIds();
		if (mainCats != null && !mainCats.isEmpty()) {
			List<Long> fromMain = itemsRepository.selectDistinctItemIdsByCompanyAndItemCategoryIn(companyId, mainCats,
					filter.itemNameKeyword(), filter.distributorId());
			if (fromMain.isEmpty()) {
				return Map.of("total_count", 0L, "list", List.of());
			}
			itemIds = new ArrayList<>(fromMain);
		}
		Long saleCat = filter.saleCategoryId();
		if (saleCat != null && saleCat > 0) {
			List<Long> treeIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, saleCat);
			if (treeIds == null || treeIds.isEmpty()) {
				return Map.of("total_count", 0L, "list", List.of());
			}
			itemIds = new ArrayList<>(treeIds);
		}
		List<Long> brandIds = itemsRepository.selectDistinctPositiveBrandIdsByCompanyAndItemIdIn(companyId, itemIds,
				filter.itemNameKeyword(), filter.distributorId());
		Map<String, Object> brandPayload = new LinkedHashMap<>();
		brandPayload.put("total_count", (long) brandIds.size());
		if (brandIds.isEmpty()) {
			brandPayload.put("list", List.of());
		} else {
			List<ItemsAttributes> attrs = itemsAttributesRepository.listBrandAttributesByCompanyAndAttributeIdsOrdered(companyId, brandIds);
			List<Map<String, Object>> rows = attrs.stream().map(PointsmallFrontBrandListServiceImpl::toAttributeRow).toList();
			brandPayload.put("total_count", (long) rows.size());
			brandPayload.put("list", rows);
		}
		return Map.of("brand_list", brandPayload);
	}

	private static Map<String, Object> toAttributeRow(ItemsAttributes e) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (e.getAttributeId() != null) {
			m.put("attribute_id", String.valueOf(e.getAttributeId()));
		} else {
			m.put("attribute_id", "");
		}
		m.put("attribute_name", e.getAttributeName() != null ? e.getAttributeName() : "");
		m.put("attribute_sort", e.getAttributeSort());
		m.put("is_show", e.getIsShow() != null ? e.getIsShow() : "");
		m.put("is_image", e.getIsImage() != null ? e.getIsImage() : "");
		m.put("image_url", e.getImageUrl() != null ? e.getImageUrl() : "");
		return m;
	}
}
