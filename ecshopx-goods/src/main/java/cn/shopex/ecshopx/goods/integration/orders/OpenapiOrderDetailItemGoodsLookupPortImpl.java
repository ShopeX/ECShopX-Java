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

package cn.shopex.ecshopx.goods.integration.orders;

import cn.shopex.ecshopx.common.openapi.OpenapiOrderDetailItemGoodsLookupPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiOrderDetailItemGoodsLookupPortImpl implements OpenapiOrderDetailItemGoodsLookupPort {

	private final ItemsRepository itemsRepository;

	public OpenapiOrderDetailItemGoodsLookupPortImpl(ItemsRepository itemsRepository) {
		this.itemsRepository = itemsRepository;
	}

	@Override
	public Map<Long, Long> lookupGoodsIds(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		List<Items> rows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
		Map<Long, Long> out = new LinkedHashMap<>();
		for (Items it : rows) {
			if (it.getItemId() == null || it.getGoodsId() == null) {
				continue;
			}
			out.put(it.getItemId(), it.getGoodsId());
		}
		return out;
	}
}
