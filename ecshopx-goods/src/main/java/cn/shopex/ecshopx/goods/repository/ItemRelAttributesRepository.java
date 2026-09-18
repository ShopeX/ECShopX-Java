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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.mapper.ItemRelAttributesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRelAttributesRepository {

	private final ItemRelAttributesMapper mapper;

	public ItemRelAttributesRepository(ItemRelAttributesMapper mapper) {
		this.mapper = mapper;
	}

	public List<ItemRelAttributes> listByItemIdsAndAttributeIds(long companyId, List<Long> itemIds, List<Long> attributeIds) {
		if (itemIds == null || itemIds.isEmpty() || attributeIds == null || attributeIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).in(ItemRelAttributes::getItemId, itemIds).in(ItemRelAttributes::getAttributeId, attributeIds)
				.last("LIMIT 100");
		return mapper.selectList(w);
	}

	public List<ItemRelAttributes> listByCompanyAndAttributeId(long companyId, long attributeId) {
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).eq(ItemRelAttributes::getAttributeId, attributeId);
		return mapper.selectList(w);
	}

	public boolean existsByCompanyAndAttributeAndValueIds(long companyId, long attributeId, Collection<Long> attributeValueIds) {
		if (attributeValueIds == null || attributeValueIds.isEmpty()) {
			return false;
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).eq(ItemRelAttributes::getAttributeId, attributeId)
				.in(ItemRelAttributes::getAttributeValueId, attributeValueIds).last("LIMIT 1");
		return mapper.selectCount(w) > 0;
	}

	public int deleteByCompanyIdAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).in(ItemRelAttributes::getItemId, itemIds);
		return mapper.delete(w);
	}

	public void insert(ItemRelAttributes row) {
		if (row.getAttributeSort() == null) {
			row.setAttributeSort(0);
		}
		mapper.insert(row);
	}

	/**
	 * OpenAPI goods_list 对齐：company_id + item_id IN，全局 LIMIT 100，无 ORDER BY。
	 */
	public List<ItemRelAttributes> listByCompanyAndItemIdsLimit100WithoutOrder(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId)
				.in(ItemRelAttributes::getItemId, itemIds)
				.last("LIMIT 100");
		return mapper.selectList(w);
	}

	/** 同一公司下多个 item_id 的关联属性，按 attribute_sort 升序。 */
	public List<ItemRelAttributes> listByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).in(ItemRelAttributes::getItemId, itemIds).orderByAsc(ItemRelAttributes::getAttributeSort);
		return mapper.selectList(w);
	}

	public List<ItemRelAttributes> listByCompanyAndItemId(long companyId, long itemId) {
		return listByCompanyAndItemIds(companyId, List.of(itemId));
	}

	public List<ItemRelAttributes> listByCompanyItemIdsAndAttributeType(long companyId, Collection<Long> itemIds, String attributeType) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).in(ItemRelAttributes::getItemId, itemIds).eq(ItemRelAttributes::getAttributeType, attributeType)
				.orderByAsc(ItemRelAttributes::getAttributeSort);
		return mapper.selectList(w);
	}

	public Optional<ItemRelAttributes> getOne(long companyId, long itemId, long attributeId, String attributeType) {
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(ItemRelAttributes::getCompanyId, companyId).eq(ItemRelAttributes::getItemId, itemId).eq(ItemRelAttributes::getAttributeId, attributeId)
				.eq(ItemRelAttributes::getAttributeType, attributeType).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	/** Updates only non-null fields on {@code patch} (except id). */
	public void updateById(long id, ItemRelAttributes patch) {
		LambdaUpdateWrapper<ItemRelAttributes> u = new LambdaUpdateWrapper<>();
		u.eq(ItemRelAttributes::getId, id);
		if (patch.getAttributeId() != null) {
			u.set(ItemRelAttributes::getAttributeId, patch.getAttributeId());
		}
		if (patch.getAttributeSort() != null) {
			u.set(ItemRelAttributes::getAttributeSort, patch.getAttributeSort());
		}
		if (patch.getAttributeValueId() != null) {
			u.set(ItemRelAttributes::getAttributeValueId, patch.getAttributeValueId());
		}
		if (patch.getCustomAttributeValue() != null) {
			u.set(ItemRelAttributes::getCustomAttributeValue, patch.getCustomAttributeValue());
		}
		if (patch.getImageUrl() != null && !patch.getImageUrl().isBlank()) {
			u.set(ItemRelAttributes::getImageUrl, patch.getImageUrl());
		}
		mapper.update(null, u);
	}

	public int deleteByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<ItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.in(ItemRelAttributes::getId, ids);
		return mapper.delete(w);
	}
}
