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

package cn.shopex.ecshopx.goods.service.recommend;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 商品推荐规则：item_id 与稳定产品键 goods_id 的归一化与展示解析。 */
@Component
public class GoodsRecommendGoodsIdResolver {

	private final ItemsMapper itemsMapper;

	public GoodsRecommendGoodsIdResolver(ItemsMapper itemsMapper) {
		this.itemsMapper = itemsMapper;
	}

	public long resolveGoodsId(Items item) {
		if (item == null) {
			return 0L;
		}
		Long goodsId = item.getGoodsId();
		if (goodsId != null && goodsId > 0) {
			return goodsId;
		}
		return item.getItemId() != null ? item.getItemId() : 0L;
	}

	public Map<Long, Long> itemIdsToGoodsIds(Map<Long, Items> itemsById, Collection<Long> itemIds) {
		Map<Long, Long> out = new LinkedHashMap<>();
		if (itemIds == null) {
			return out;
		}
		for (Long itemId : itemIds) {
			if (itemId == null || itemId <= 0) {
				continue;
			}
			Items item = itemsById != null ? itemsById.get(itemId) : null;
			long goodsId = resolveGoodsId(item);
			if (goodsId > 0) {
				out.put(itemId, goodsId);
			}
		}
		return out;
	}

	public Set<Long> resolveGoodsIdSet(Map<Long, Items> itemsById, Collection<Long> itemIds) {
		Set<Long> out = new HashSet<>();
		itemIdsToGoodsIds(itemsById, itemIds).values().forEach(out::add);
		return out;
	}

	/** 按 goods_id 解析当前展示 SKU（优先 is_default=true）。 */
	public Map<Long, Items> loadDisplayItemsByGoodsId(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return Map.of();
		}
		Set<Long> ids = new HashSet<>();
		for (Long id : goodsIds) {
			if (id != null && id > 0) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<Items> rows =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.in(Items::getGoodsId, ids)
								.orderByDesc(Items::getIsDefault)
								.orderByAsc(Items::getItemId));
		Map<Long, Items> byGoodsId = new LinkedHashMap<>();
		for (Items row : rows) {
			long goodsId = resolveGoodsId(row);
			if (goodsId > 0) {
				byGoodsId.putIfAbsent(goodsId, row);
			}
		}
		return byGoodsId;
	}

	public Map<Long, Long> loadDisplayItemIdByGoodsId(long companyId, Collection<Long> goodsIds) {
		Map<Long, Items> display = loadDisplayItemsByGoodsId(companyId, goodsIds);
		Map<Long, Long> out = new HashMap<>();
		for (Map.Entry<Long, Items> entry : display.entrySet()) {
			if (entry.getValue().getItemId() != null) {
				out.put(entry.getKey(), entry.getValue().getItemId());
			}
		}
		return out;
	}
}
