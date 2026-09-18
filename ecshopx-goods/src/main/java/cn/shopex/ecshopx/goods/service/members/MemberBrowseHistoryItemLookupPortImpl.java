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

package cn.shopex.ecshopx.goods.service.members;

import cn.shopex.ecshopx.common.members.port.MemberBrowseHistoryItemLookupPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MemberBrowseHistoryItemLookupPortImpl implements MemberBrowseHistoryItemLookupPort {

	private final ItemsMapper itemsMapper;

	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public MemberBrowseHistoryItemLookupPortImpl(
			ItemsMapper itemsMapper, ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsMapper = itemsMapper;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	@Override
	public Optional<ItemRow> findByItemId(long itemId) {
		if (itemId <= 0L) {
			return Optional.empty();
		}
		Items row = itemsMapper.selectById(itemId);
		if (row == null) {
			return Optional.empty();
		}
		return Optional.of(new ItemRow(row.getItemId(), row.getDefaultItemId()));
	}

	@Override
	public Map<Long, Map<String, Object>> listItemRowsForBrowseHistory(
			Collection<Long> itemIds, long companyIdForLang, String countryCodeOrLanguageTag) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> distinctPositiveOrder = new ArrayList<>();
		Set<Long> seen = new LinkedHashSet<>();
		for (Long id : itemIds) {
			if (id == null || id <= 0L) {
				continue;
			}
			if (seen.add(id)) {
				distinctPositiveOrder.add(id);
			}
		}
		if (distinctPositiveOrder.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.in(Items::getItemId, distinctPositiveOrder);
		List<Items> loaded = itemsMapper.selectList(w);
		List<Map<String, Object>> rowsForLang = new ArrayList<>();
		for (Items it : loaded) {
			rowsForLang.add(GoodsItemsListRowMapper.toRow(it));
		}
		itemsListMultiLangApplier.applyToRows(companyIdForLang, countryCodeOrLanguageTag, rowsForLang);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rowsForLang) {
			Object i = row.get("item_id");
			if (!(i instanceof Number n)) {
				continue;
			}
			out.put(n.longValue(), row);
		}
		return out;
	}
}
