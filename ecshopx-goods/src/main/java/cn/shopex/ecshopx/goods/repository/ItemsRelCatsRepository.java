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

import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.mapper.ItemsRelCatsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsRelCatsRepository {

	private final ItemsRelCatsMapper mapper;

	public ItemsRelCatsRepository(ItemsRelCatsMapper mapper) {
		this.mapper = mapper;
	}

	/** category_id IN categoryIds 且 company_id 匹配；用于收集 item_id。 */
	public List<ItemsRelCats> listByCompanyIdAndCategoryIdIn(long companyId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelCats::getCompanyId, companyId).in(ItemsRelCats::getCategoryId, categoryIds);
		return mapper.selectList(w);
	}

	public List<ItemsRelCats> listByCompanyIdAndItemIdIn(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelCats::getCompanyId, companyId).in(ItemsRelCats::getItemId, itemIds);
		return mapper.selectList(w);
	}

	/** company_id + category_id（单值，根待删类目）+ item_id IN itemIds；itemIds 为空时 no-op，返回 0。 */
	public int deleteByCompanyIdAndCategoryIdAndItemIdIn(long companyId, long categoryId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<ItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelCats::getCompanyId, companyId).eq(ItemsRelCats::getCategoryId, categoryId)
				.in(ItemsRelCats::getItemId, itemIds);
		return mapper.delete(w);
	}

	public int deleteByCompanyIdAndItemIdIn(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<ItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelCats::getCompanyId, companyId).in(ItemsRelCats::getItemId, itemIds);
		return mapper.delete(w);
	}

	public List<ItemsRelCats> listByCompanyIdAndItemId(long companyId, long itemId) {
		LambdaQueryWrapper<ItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelCats::getCompanyId, companyId).eq(ItemsRelCats::getItemId, itemId);
		return mapper.selectList(w);
	}

	public void insert(ItemsRelCats row) {
		mapper.insert(row);
	}

	public int deleteByCompanyIdAndItemIdAndCategoryIdIn(long companyId, long itemId, Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<ItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelCats::getCompanyId, companyId).eq(ItemsRelCats::getItemId, itemId).in(ItemsRelCats::getCategoryId, categoryIds);
		return mapper.delete(w);
	}
}
