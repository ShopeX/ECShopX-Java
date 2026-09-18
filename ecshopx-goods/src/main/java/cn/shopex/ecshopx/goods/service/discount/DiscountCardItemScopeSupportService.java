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

package cn.shopex.ecshopx.goods.service.discount;

import cn.shopex.ecshopx.common.discount.DiscountCardItemScopeGateway;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsRelTagsMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsTagsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardItemScopeSupportService implements DiscountCardItemScopeGateway {

	private final ItemsMapper itemsMapper;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsTagsMapper itemsTagsMapper;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsRelTagsMapper itemsRelTagsMapper;

	public DiscountCardItemScopeSupportService(ItemsMapper itemsMapper, ItemsCategoryRepository itemsCategoryRepository,
			ItemsTagsMapper itemsTagsMapper, ItemsAttributesRepository itemsAttributesRepository,
			ItemsRelTagsMapper itemsRelTagsMapper) {
		this.itemsMapper = itemsMapper;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsTagsMapper = itemsTagsMapper;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsRelTagsMapper = itemsRelTagsMapper;
	}

	@Override
	public String joinCategoryNames(long companyId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return "";
		}
		List<ItemsCategory> rows = itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, categoryIds);
		return rows.stream().map(ItemsCategory::getCategoryName).filter(Objects::nonNull).collect(Collectors.joining(","));
	}

	@Override
	public String joinTagNames(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return "";
		}
		List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(tagIds));
		List<ItemsTags> rows = itemsTagsMapper.selectBatchIds(distinct);
		return rows.stream().filter(t -> t.getCompanyId() != null && t.getCompanyId() == companyId)
				.map(ItemsTags::getTagName).filter(Objects::nonNull).collect(Collectors.joining(","));
	}

	@Override
	public String joinBrandNames(long companyId, List<Integer> brandIds) {
		if (brandIds == null || brandIds.isEmpty()) {
			return "";
		}
		List<Long> asLong = brandIds.stream().map(Integer::longValue).toList();
		List<ItemsAttributes> rows = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, asLong);
		return rows.stream().filter(a -> "brand".equals(a.getAttributeType()))
				.map(ItemsAttributes::getAttributeName).filter(Objects::nonNull).collect(Collectors.joining(","));
	}

	@Override
	public long countTagRelations(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId);
		if (tagIds.size() == 1) {
			w.eq(ItemsRelTags::getTagId, tagIds.get(0));
		} else {
			w.in(ItemsRelTags::getTagId, tagIds);
		}
		return itemsRelTagsMapper.selectCount(w);
	}

	private long count(LambdaQueryWrapper<Items> w) {
		return itemsMapper.selectCount(w);
	}

	private LambdaQueryWrapper<Items> baseWrapper(long companyId, Integer distributorIdFirstOrNull, boolean isDistributorScope) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemType, "normal").in(Items::getSpecialType, "normal", "drug")
				.eq(Items::getIsGift, false).eq(Items::getIsDefault, true);
		if (isDistributorScope && distributorIdFirstOrNull != null) {
			w.eq(Items::getDistributorId, distributorIdFirstOrNull);
		}
		return w;
	}

	@Override
	public long countApplicableItems(long companyId, Integer distributorIdFirstOrNull, boolean isDistributorScope,
			List<Long> categoryIds, List<Long> tagIds, List<Integer> brandIds) {
		boolean hasCategory = categoryIds != null && !categoryIds.isEmpty();
		boolean hasTag = tagIds != null && !tagIds.isEmpty();
		boolean hasBrand = brandIds != null && !brandIds.isEmpty();
		if (!hasCategory && !hasTag && !hasBrand) {
			return count(baseWrapper(companyId, distributorIdFirstOrNull, isDistributorScope));
		}
		LambdaQueryWrapper<Items> w = baseWrapper(companyId, distributorIdFirstOrNull, isDistributorScope);
		if (hasCategory) {
			List<String> cats = categoryIds.stream().map(String::valueOf).toList();
			w.in(Items::getItemCategory, cats);
		}
		if (hasBrand) {
			w.in(Items::getBrandId, brandIds);
		}
		if (hasTag) {
			String inList = tagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			w.inSql(Items::getItemId, "SELECT DISTINCT item_id FROM items_rel_tags WHERE company_id = " + companyId + " AND tag_id IN ("
					+ inList + ")");
		}
		return count(w);
	}
}
