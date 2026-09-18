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

import cn.shopex.ecshopx.goods.domain.ItemsRelType;
import cn.shopex.ecshopx.goods.mapper.ItemsRelTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
public class ItemsRelTypeRepository {

	private final ItemsRelTypeMapper mapper;

	public ItemsRelTypeRepository(ItemsRelTypeMapper mapper) {
		this.mapper = mapper;
	}

	public List<ItemsRelType> listByItemIds(Collection<Long> itemIds) {
		if (CollectionUtils.isEmpty(itemIds)) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelType> w = new LambdaQueryWrapper<>();
		w.in(ItemsRelType::getItemId, itemIds);
		return mapper.selectList(w);
	}

	public void deleteAllByItemId(long itemId) {
		LambdaQueryWrapper<ItemsRelType> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelType::getItemId, itemId);
		mapper.delete(w);
	}

	public void insert(ItemsRelType row) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (row.getCreated() == null) {
			row.setCreated(now);
		}
		if (row.getUpdated() == null) {
			row.setUpdated(now);
		}
		mapper.insert(row);
	}
}
