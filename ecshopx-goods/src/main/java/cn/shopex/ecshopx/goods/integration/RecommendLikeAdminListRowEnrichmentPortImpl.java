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

import cn.shopex.ecshopx.common.promotions.RecommendLikeAdminListRowEnrichmentPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemCrossBorderTaxQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendLikeAdminListRowEnrichmentPortImpl implements RecommendLikeAdminListRowEnrichmentPort {

	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final ItemsRepository itemsRepository;
	private final ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public RecommendLikeAdminListRowEnrichmentPortImpl(
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			ItemsRepository itemsRepository,
			ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.itemsRepository = itemsRepository;
		this.itemCrossBorderTaxQueryRepository = itemCrossBorderTaxQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	@Override
	public void enrichRows(
			long companyId,
			long userId,
			List<Map<String, Object>> rows,
			String acceptLanguageHeader) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<LinkedHashMap<String, Object>> baselines = new ArrayList<>(rows.size());
		for (Map<String, Object> row : rows) {
			baselines.add(new LinkedHashMap<>(row));
		}
		wxappGoodsItemsListMemberPriceApplyService.applyForRows(
				companyId, userId, rows, acceptLanguageHeader);
		goodsItemsListPromotionEnrichmentService.enrich(rows);
		applyCrossBorderTax(companyId, rows);
		itemsListMultiLangApplier.applyToRows(companyId, acceptLanguageHeader, rows);
		for (int i = 0; i < rows.size(); i++) {
			Map<String, Object> row = rows.get(i);
			Object itemNameSrc = row.get("item_name");
			row.put("itemName", itemNameSrc != null ? itemNameSrc : "");
			Object ty = row.get("type");
			// Optional join: when the item side is absent, type is null; keep null instead of coercing to "0".
			row.put("type", ty == null ? null : String.valueOf(ty));
			LinkedHashMap<String, Object> merged = baselines.get(i);
			merged.putAll(row);
			rows.set(i, merged);
		}
	}

	private void applyCrossBorderTax(long companyId, List<Map<String, Object>> list) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : list) {
			if (isCrossBorderType(row.get("type"))) {
				ids.add(longVal(row.get("item_id")));
			}
		}
		Map<Long, Items> byId = Map.of();
		if (!ids.isEmpty()) {
			List<Items> entities = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, ids);
			byId =
					entities.stream()
							.filter(i -> i.getItemId() != null)
							.collect(Collectors.toMap(Items::getItemId, i -> i, (a, b) -> a));
		}
		for (Map<String, Object> row : list) {
			if (!isCrossBorderType(row.get("type"))) {
				row.put("cross_border_tax", 0);
				row.put("cross_border_tax_rate", 0);
				continue;
			}
			long itemId = longVal(row.get("item_id"));
			Items it = byId.get(itemId);
			int priceFen = intVal(row.get("price"));
			String taxField = "price";
			int taxBase = priceFen;
			if (nonEmptyInt(row.get("member_price"))) {
				taxField = "member_price";
				taxBase = intVal(row.get("member_price"));
			}
			if (nonEmptyInt(row.get("activity_price"))) {
				taxField = "activity_price";
				taxBase = intVal(row.get("activity_price"));
			}
			BigDecimal ratePercent = itemCrossBorderTaxQueryRepository.resolveEffectiveTaxRatePercent(it, taxBase);
			BigDecimal base = BigDecimal.valueOf(taxBase);
			BigDecimal tax = base.multiply(ratePercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
			row.put("cross_border_tax_rate", ratePercent);
			row.put("cross_border_tax", tax.longValue());
			BigDecimal newPrice = base.add(tax);
			row.put(taxField, newPrice.longValue());
			if ("activity_price".equals(taxField)) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> acts = (List<Map<String, Object>>) row.get("promotion_activity");
				if (acts != null && !acts.isEmpty()) {
					Map<String, Object> last = acts.get(acts.size() - 1);
					last.put("activity_price", newPrice.longValue());
				}
			}
		}
	}

	private static boolean nonEmptyInt(Object v) {
		return intVal(v) > 0;
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Cross-border branch runs only when {@code type} is present and numerically {@code 1}. */
	private static boolean isCrossBorderType(Object type) {
		if (type == null) {
			return false;
		}
		return longVal(type) == 1L;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
