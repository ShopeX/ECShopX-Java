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

import cn.shopex.ecshopx.goods.domain.ItemsRelPointAccess;
import cn.shopex.ecshopx.goods.mapper.ItemsRelPointAccessMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsRelPointAccessRepository {

	private final ItemsRelPointAccessMapper mapper;

	public ItemsRelPointAccessRepository(ItemsRelPointAccessMapper mapper) {
		this.mapper = mapper;
	}

	public void deleteByItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsRelPointAccess> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelPointAccess::getCompanyId, companyId).in(ItemsRelPointAccess::getItemId, itemIds);
		mapper.delete(w);
	}

	public ItemsRelPointAccess findByCompanyAndItemId(long companyId, long itemId) {
		LambdaQueryWrapper<ItemsRelPointAccess> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelPointAccess::getCompanyId, companyId).eq(ItemsRelPointAccess::getItemId, itemId);
		return mapper.selectOne(w);
	}

	public Map<Long, Long> mapPointByItemId(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<ItemsRelPointAccess> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelPointAccess::getCompanyId, companyId).in(ItemsRelPointAccess::getItemId, itemIds);
		Map<Long, Long> out = new HashMap<>();
		for (ItemsRelPointAccess row : mapper.selectList(w)) {
			if (row.getItemId() != null && row.getPoint() != null) {
				out.put(row.getItemId(), row.getPoint());
			}
		}
		return out;
	}

	public void upsertForItem(long companyId, long itemId, long point) {
		LambdaQueryWrapper<ItemsRelPointAccess> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelPointAccess::getCompanyId, companyId).eq(ItemsRelPointAccess::getItemId, itemId);
		ItemsRelPointAccess existing = mapper.selectOne(w);
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (existing != null) {
			existing.setPoint(point);
			existing.setUpdated(now);
			mapper.updateById(existing);
		} else {
			ItemsRelPointAccess row = new ItemsRelPointAccess();
			row.setCompanyId(companyId);
			row.setItemId(itemId);
			row.setPoint(point);
			row.setCreated(now);
			row.setUpdated(now);
			mapper.insert(row);
		}
	}
}
