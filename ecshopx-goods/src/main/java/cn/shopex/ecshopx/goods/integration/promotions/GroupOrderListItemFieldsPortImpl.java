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

package cn.shopex.ecshopx.goods.integration.promotions;

import cn.shopex.ecshopx.common.goods.port.GroupOrderListItemFieldsPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GroupOrderListItemFieldsPortImpl implements GroupOrderListItemFieldsPort {

	private final ItemsMapper itemsMapper;

	public GroupOrderListItemFieldsPortImpl(ItemsMapper itemsMapper) {
		this.itemsMapper = itemsMapper;
	}

	@Override
	public Map<Long, Map<String, Object>> loadDisplayByItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return new LinkedHashMap<>();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (Long id : itemIds) {
			if (id != null && id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return new LinkedHashMap<>();
		}
		List<Items> rows =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.in(Items::getItemId, ids));
		LinkedHashMap<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Items it : rows) {
			Long itemId = it.getItemId();
			if (itemId == null) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("itemId", itemId);
			m.put("itemName", it.getItemName());
			Integer p = it.getPrice();
			m.put("price", p == null ? null : p.longValue());
			m.put("pics", it.getPics());
			out.put(itemId, m);
		}
		return out;
	}
}
