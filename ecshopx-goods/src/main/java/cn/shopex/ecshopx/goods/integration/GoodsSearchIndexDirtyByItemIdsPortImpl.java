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

package cn.shopex.ecshopx.goods.integration;

import cn.shopex.ecshopx.common.port.goods.GoodsSearchIndexDirtyByItemIdsPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.search.DefaultItemIdResolver;
import cn.shopex.ecshopx.goods.search.sync.GoodsSearchIndexDirtyFanout;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class GoodsSearchIndexDirtyByItemIdsPortImpl implements GoodsSearchIndexDirtyByItemIdsPort {

	private final ItemsRepository itemsRepository;
	private final GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout;

	public GoodsSearchIndexDirtyByItemIdsPortImpl(
			ItemsRepository itemsRepository, GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout) {
		this.itemsRepository = itemsRepository;
		this.goodsSearchIndexDirtyFanout = goodsSearchIndexDirtyFanout;
	}

	@Override
	public void scheduleUpsertForItemIds(long companyId, Collection<Long> itemIds) {
		if (companyId <= 0 || CollectionUtils.isEmpty(itemIds)) {
			return;
		}
		Set<Long> defaultItemIds = resolveDefaultItemIds(companyId, itemIds);
		for (long defaultItemId : defaultItemIds) {
			goodsSearchIndexDirtyFanout.scheduleUpsertForDefaultItem(companyId, defaultItemId);
		}
	}

	private Set<Long> resolveDefaultItemIds(long companyId, Collection<Long> itemIds) {
		List<Long> distinct = itemIds.stream().filter(id -> id != null && id > 0).distinct().toList();
		if (distinct.isEmpty()) {
			return Set.of();
		}
		List<Items> rows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, distinct);
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		for (Items row : rows) {
			out.add(DefaultItemIdResolver.resolve(row));
		}
		for (Long itemId : distinct) {
			if (rows.stream().noneMatch(r -> itemId.equals(r.getItemId()))) {
				out.add(itemId);
			}
		}
		return out;
	}
}
