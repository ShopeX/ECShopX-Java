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

import cn.shopex.ecshopx.common.port.goods.GoodsItemsListsByItemIdsReadPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GoodsItemsListsByItemIdsReadPortImpl implements GoodsItemsListsByItemIdsReadPort {

	private final ItemsRepository itemsRepository;

	public GoodsItemsListsByItemIdsReadPortImpl(ItemsRepository itemsRepository) {
		this.itemsRepository = itemsRepository;
	}

	@Override
	public List<Map<String, Object>> listRowsByItemIds(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		List<Items> rows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (Items it : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("item_id", it.getItemId());
			m.put("item_name", it.getItemName() != null ? it.getItemName() : "");
			m.put("goods_bn", it.getGoodsBn() != null ? it.getGoodsBn() : "");
			m.put("item_bn", it.getItemBn() != null ? it.getItemBn() : "");
			int gift = it.getIsGift() != null && it.getIsGift() ? 1 : 0;
			m.put("is_gift", gift);
			out.add(m);
		}
		return out;
	}
}
