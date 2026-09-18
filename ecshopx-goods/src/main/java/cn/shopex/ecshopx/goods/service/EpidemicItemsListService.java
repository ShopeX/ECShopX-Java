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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EpidemicItemsListService {

	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public EpidemicItemsListService(ItemsQueryRepository itemsQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	public Map<String, Object> listEpidemicItems(long companyId, Integer distributorIdFilter, int page, int pageSize,
			String countryCode) {
		long total = itemsQueryRepository.countEpidemicItems(companyId, distributorIdFilter);
		Map<String, Object> out = new LinkedHashMap<>();
		if (total == 0L) {
			out.put("total_count", 0L);
			out.put("list", List.of());
			return out;
		}

		List<Items> records = itemsQueryRepository.pageEpidemicItems(companyId, distributorIdFilter, page, pageSize);
		List<Map<String, Object>> rowMaps = new ArrayList<>(records.size());
		for (Items it : records) {
			Map<String, Object> row = new LinkedHashMap<>();
			Long itemId = it.getItemId();
			Long companyIdVal = it.getCompanyId();
			Integer store = it.getStore();
			String itemBn = it.getItemBn();
			String barcode = it.getBarcode();
			String itemName = it.getItemName();

			row.put("item_id", itemId);
			row.put("company_id", companyIdVal);
			row.put("store", store);
			row.put("item_bn", itemBn);
			row.put("barcode", barcode);
			row.put("item_name", itemName);

			row.put("itemId", itemId);
			row.put("consumeType", "");
			row.put("itemName", itemName);
			row.put("itemBn", itemBn);
			row.put("companyId", companyIdVal);
			row.put("item_main_cat_id", "");
			row.put("nospec", false);
			row.put("is_medicine", 0);

			rowMaps.add(row);
		}

		itemsListMultiLangApplier.applyListLangForItems(companyId, countryCode, rowMaps);
		for (Map<String, Object> row : rowMaps) {
			row.put("itemName", row.get("item_name"));
		}

		out.put("total_count", total);
		out.put("list", rowMaps);
		return out;
	}
}
