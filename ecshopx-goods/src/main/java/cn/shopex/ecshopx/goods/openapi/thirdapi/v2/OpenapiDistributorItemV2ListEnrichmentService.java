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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiDistributorItemV2ListEnrichmentService {

	private final DistributorItemsRepository distributorItemsRepository;

	public OpenapiDistributorItemV2ListEnrichmentService(
			DistributorItemsRepository distributorItemsRepository) {
		this.distributorItemsRepository = distributorItemsRepository;
	}

	public void apply(long companyId, long distributorId, List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}

		List<Long> spuIds =
				list.stream().map(r -> longVal(r.get("item_id"))).filter(id -> id > 0).distinct().toList();
		if (spuIds.isEmpty()) {
			return;
		}

		List<DistributorItems> distRows =
				distributorItemsRepository.listByCompanyDistributorAndDefaultItemIdIn(
						companyId, distributorId, spuIds);

		Map<Long, Map<Long, DistributorItems>> distributorItemList = new HashMap<>();
		Map<Long, Integer> itemGoodsCanSaleArray = new HashMap<>();

		for (DistributorItems row : distRows) {
			long defaultItemId = row.getDefaultItemId() != null ? row.getDefaultItemId() : 0L;
			long itemId = row.getItemId() != null ? row.getItemId() : 0L;
			if (defaultItemId <= 0 || itemId <= 0) {
				continue;
			}
			distributorItemList
					.computeIfAbsent(defaultItemId, k -> new HashMap<>())
					.put(itemId, row);
			if (isTruthyGoodsCanSale(row.getGoodsCanSale())) {
				itemGoodsCanSaleArray.put(defaultItemId, 1);
			}
		}

		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> item = list.get(i);
			long spuId = longVal(item.get("item_id"));
			Map<Long, DistributorItems> skuMap = distributorItemList.get(spuId);
			DistributorItems spuRow = skuMap != null ? skuMap.get(spuId) : null;

			int store = (int) longVal(item.get("store"));
			long priceFen = longVal(item.get("price"));
			int goodsCanSale = 0;
			int isTotalStore = 1;

			if (spuRow != null) {
				goodsCanSale = itemGoodsCanSaleArray.getOrDefault(spuId, 0);
				isTotalStore = Boolean.TRUE.equals(spuRow.getIsTotalStore()) ? 1 : 0;
				if (isTotalStore == 0) {
					if (spuRow.getStore() != null) {
						store = spuRow.getStore().intValue();
					}
					if (spuRow.getPrice() != null) {
						priceFen = spuRow.getPrice();
					}
				}
			}

			Map<String, Object> out = new LinkedHashMap<>();
			out.put("distributor_id", (int) distributorId);
			out.put("item_id", (int) spuId);
			out.put("item_code", str(item.get("item_bn")));
			out.put("item_name", str(item.get("item_name")));
			out.put("store", store);
			out.put("price", fenToYuanString(priceFen));
			out.put("goods_can_sale", goodsCanSale);
			out.put("is_total_store", isTotalStore);
			out.put("status", str(item.get("approve_status")));
			list.set(i, out);
		}
	}

	private static boolean isTruthyGoodsCanSale(Boolean goodsCanSale) {
		if (goodsCanSale == null) {
			return false;
		}
		return goodsCanSale;
	}

	private static String fenToYuanString(long fen) {
		return BigDecimal.valueOf(fen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
