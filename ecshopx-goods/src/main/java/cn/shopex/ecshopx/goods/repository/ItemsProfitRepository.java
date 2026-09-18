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

import cn.shopex.ecshopx.goods.domain.ItemsProfit;
import cn.shopex.ecshopx.goods.mapper.ItemsProfitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsProfitRepository {

	private final ItemsProfitMapper mapper;

	public ItemsProfitRepository(ItemsProfitMapper mapper) {
		this.mapper = mapper;
	}

	public void deleteByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsProfit> w = new LambdaQueryWrapper<>();
		w.eq(ItemsProfit::getCompanyId, companyId).in(ItemsProfit::getItemId, itemIds);
		mapper.delete(w);
	}

	public int insertRow(ItemsProfit row) {
		return mapper.insert(row);
	}

	public Map<Long, ItemsProfit> mapByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		Map<Long, ItemsProfit> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<ItemsProfit> w = new LambdaQueryWrapper<>();
		w.eq(ItemsProfit::getCompanyId, companyId).in(ItemsProfit::getItemId, itemIds);
		for (ItemsProfit row : mapper.selectList(w)) {
			if (row.getItemId() != null && !out.containsKey(row.getItemId())) {
				out.put(row.getItemId(), row);
			}
		}
		return out;
	}
}
