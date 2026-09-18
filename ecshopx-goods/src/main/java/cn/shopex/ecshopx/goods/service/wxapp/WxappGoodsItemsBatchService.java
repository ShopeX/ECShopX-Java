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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批量获取商品基本信息（图片、ID、名称、价格），对齐 ecshopx-api-open {@code Items@getBatchItems}。
 */
@Service
public class WxappGoodsItemsBatchService {

	private final ItemsRepository itemsRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public WxappGoodsItemsBatchService(ItemsRepository itemsRepository, ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsRepository = itemsRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	public List<Map<String, Object>> execute(long companyId, String itemIdsStr, String countryCode) {
		if (!StringUtils.hasText(itemIdsStr)) {
			return List.of();
		}
		List<Long> itemIds = parseItemIds(itemIdsStr);
		if (itemIds.isEmpty()) {
			return List.of();
		}
		List<Items> loaded = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
		List<Map<String, Object>> rows = new ArrayList<>(loaded.size());
		for (Items it : loaded) {
			rows.add(toBatchRow(it));
		}
		itemsListMultiLangApplier.applyToRows(companyId, countryCode, rows);
		for (Map<String, Object> row : rows) {
			Object name = row.get("item_name");
			if (name != null) {
				row.put("itemName", name);
			}
		}
		return rows;
	}

	private static Map<String, Object> toBatchRow(Items it) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", it.getItemId());
		m.put("item_name", it.getItemName());
		m.put("itemName", it.getItemName());
		m.put("price", it.getPrice() != null ? it.getPrice() : 0);
		m.put("pics", GoodsItemsListRowMapper.resolvePicsForListRow(it.getPics()));
		return m;
	}

	/** 对齐 PHP {@code array_filter(array_map('intval', explode(',', $itemIdsStr)))}。 */
	static List<Long> parseItemIds(String itemIdsStr) {
		List<Long> out = new ArrayList<>();
		Set<Long> seen = new LinkedHashSet<>();
		for (String part : itemIdsStr.split(",")) {
			String t = part.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			long v = phpIntval(t);
			if (v > 0L && seen.add(v)) {
				out.add(v);
			}
		}
		return out;
	}

	private static long phpIntval(String raw) {
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			try {
				return (long) Double.parseDouble(raw);
			} catch (NumberFormatException ignored) {
				return 0L;
			}
		}
	}
}
