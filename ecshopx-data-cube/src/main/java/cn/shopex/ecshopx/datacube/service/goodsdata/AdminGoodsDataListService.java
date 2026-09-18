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

package cn.shopex.ecshopx.datacube.service.goodsdata;

import cn.shopex.ecshopx.datacube.mapper.GoodsDataAggregateRow;
import cn.shopex.ecshopx.datacube.mapper.GoodsDataMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminGoodsDataListService {

	private final GoodsDataMapper goodsDataMapper;

	private final ItemsRepository itemsRepository;

	private final ItemsCategoryRepository itemsCategoryRepository;

	public AdminGoodsDataListService(
			GoodsDataMapper goodsDataMapper,
			ItemsRepository itemsRepository,
			ItemsCategoryRepository itemsCategoryRepository) {
		this.goodsDataMapper = goodsDataMapper;
		this.itemsRepository = itemsRepository;
		this.itemsCategoryRepository = itemsCategoryRepository;
	}

	public List<Map<String, Object>> getGoodsDataList(AdminGoodsDataFilter filter) {
		List<GoodsDataAggregateRow> aggregates = goodsDataMapper.selectAggregatesByFilter(filter);
		long totalQty = 0L;
		long totalFixCents = 0L;
		long totalSettleCents = 0L;

		List<Map<String, Object>> out = new ArrayList<>();
		for (int k = 0; k < aggregates.size(); k++) {
			GoodsDataAggregateRow row = aggregates.get(k);
			long qty = row.getSumSalesCount() != null ? row.getSumSalesCount() : 0L;
			long fixCents = row.getSumFixedAmountCount() != null ? row.getSumFixedAmountCount() : 0L;
			long settleCents = row.getSumSettleAmountCount() != null ? row.getSumSettleAmountCount() : 0L;
			totalQty += qty;
			totalFixCents += fixCents;
			totalSettleCents += settleCents;

			Long itemId = row.getItemId();
			if (itemId == null) {
				continue;
			}
			Items item = itemsRepository.findByItemId(itemId);
			if (item == null) {
				continue;
			}

			String itemName = item.getItemName() != null ? item.getItemName() : "";
			String product = itemName.replace("#", "");
			String itemCategory = item.getItemCategory() != null ? item.getItemCategory() : "";
			String topLevel = itemsCategoryRepository.findFirstCategoryNameByPathCommaSuffix(itemCategory).orElse("");

			Map<String, Object> line = new LinkedHashMap<>();
			line.put("no", k + 1);
			line.put("sap_code", item.getItemBn() != null ? item.getItemBn() : "");
			line.put("top_level", topLevel);
			line.put("product", product);
			line.put("quantity", qty);
			line.put("fix_price", centsToPlainString(fixCents));
			line.put("settle_price", centsToPlainString(settleCents));
			out.add(line);
		}

		Map<String, Object> totalRow = new LinkedHashMap<>();
		totalRow.put("no", "总计");
		totalRow.put("sap_code", "");
		totalRow.put("top_level", "");
		totalRow.put("product", "");
		totalRow.put("quantity", totalQty);
		totalRow.put("fix_price", centsToPlainString(totalFixCents));
		totalRow.put("settle_price", centsToPlainString(totalSettleCents));
		out.add(totalRow);
		return out;
	}

	private static String centsToPlainString(long cents) {
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}
}
