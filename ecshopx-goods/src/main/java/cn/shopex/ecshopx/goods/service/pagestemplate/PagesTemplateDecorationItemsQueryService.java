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

package cn.shopex.ecshopx.goods.service.pagestemplate;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonCartDistributorSkuReplaceService;
import cn.shopex.ecshopx.goods.service.items.EmployeePurchaseItemsSkuListService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PagesTemplateDecorationItemsQueryService {

	private final EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService;
	private final ItemsMapper itemsMapper;

	public PagesTemplateDecorationItemsQueryService(
			EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService,
			ItemsMapper itemsMapper) {
		this.employeePurchaseItemsSkuListService = employeePurchaseItemsSkuListService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.salespersonCartDistributorSkuReplaceService = salespersonCartDistributorSkuReplaceService;
		this.itemsMapper = itemsMapper;
	}

	public List<Map<String, Object>> queryItemListData(
			long companyId, List<Long> itemIds, long queryDistributorId, Map<String, Object> params) {
		if (CollectionUtils.isEmpty(itemIds)) {
			return List.of();
		}
		Map<String, Object> pack = employeePurchaseItemsSkuListService.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) pack.getOrDefault("list", List.of());
		if (list.isEmpty()) {
			return List.of();
		}
		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
		if ("standard".equals(productModel) && queryDistributorId > 0L) {
			salespersonCartDistributorSkuReplaceService.apply(companyId, queryDistributorId, list);
		}
		goodsItemsListPromotionEnrichmentService.enrich(list);
		List<Items> entities = itemsMapper.selectBatchIds(itemIds);
		Map<Long, Items> byItem =
				entities == null
						? Map.of()
						: entities.stream()
								.filter(Objects::nonNull)
								.filter(it -> it.getItemId() != null)
								.collect(Collectors.toMap(Items::getItemId, it -> it, (a, b) -> a, LinkedHashMap::new));
		for (Map<String, Object> row : list) {
			long iid = longOrZero(row.get("item_id"));
			Items it = byItem.get(iid);
			String brand = "";
			if (it != null && it.getGoodsBrand() != null) {
				brand = it.getGoodsBrand();
			}
			row.put("brand", brand);
			Object pa = row.get("promotion_activity");
			row.put("promotionActivity", pa);
		}
		return list;
	}

	public static Map<Long, Map<String, Object>> indexByItemId(List<Map<String, Object>> rows) {
		Map<Long, Map<String, Object>> keyed = new LinkedHashMap<>();
		if (rows == null) {
			return keyed;
		}
		for (Map<String, Object> row : rows) {
			long iid = longOrZero(row.get("item_id"));
			if (iid > 0L) {
				keyed.put(iid, row);
			}
		}
		return keyed;
	}

	private static long longOrZero(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
