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

package cn.shopex.ecshopx.goods.service.itemsfav;

import cn.shopex.ecshopx.common.members.port.MemberItemsFavListGoodsEnrichmentPort;
import cn.shopex.ecshopx.distribution.service.distributor.DistributorCouponListAppendService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemCrossBorderTaxQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Enrichment pipeline for member item-favorites list rows.
 *
 * <p>Cross-border tax adjustment is inlined from {@code WxappGoodsItemsListServiceImpl#applyCrossBorderTax}
 * (approx. lines 313–360) and its numeric helpers.
 */
@Service
public class MemberItemsFavListGoodsEnrichmentPortImpl implements MemberItemsFavListGoodsEnrichmentPort {

	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final DistributorCouponListAppendService distributorCouponListAppendService;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository;
	private final ItemsRepository itemsRepository;

	public MemberItemsFavListGoodsEnrichmentPortImpl(
			WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator,
			DistributorCouponListAppendService distributorCouponListAppendService,
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			ItemCrossBorderTaxQueryRepository itemCrossBorderTaxQueryRepository,
			ItemsRepository itemsRepository) {
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
		this.distributorCouponListAppendService = distributorCouponListAppendService;
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.itemCrossBorderTaxQueryRepository = itemCrossBorderTaxQueryRepository;
		this.itemsRepository = itemsRepository;
	}

	@Override
	public List<Map<String, Object>> loadEnrichedRows(
			long companyId,
			long userId,
			List<Long> itemIds,
			Long distributorIdOrNull,
			int goodsBatchPageSize,
			String acceptLanguageHeader) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("user_id", userId);
		params.put("item_id", new ArrayList<>(itemIds));
		if (distributorIdOrNull != null) {
			params.put("distributor_id", distributorIdOrNull);
		}
		params.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, 1);
		params.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, goodsBatchPageSize);
		params.put(
				WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE,
				acceptLanguageHeader != null ? acceptLanguageHeader : "zh-CN");

		Map<String, Object> data =
				wxappGoodsItemsListQueryOrchestrator.queryShopItemListData(companyId, params, List.of());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		distributorCouponListAppendService.appendDistributorInfo(companyId, list);
		wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, list, acceptLanguageHeader);
		goodsItemsListPromotionEnrichmentService.enrich(list);
		applyCrossBorderTax(companyId, list);
		return list;
	}

	private void applyCrossBorderTax(long companyId, List<Map<String, Object>> list) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : list) {
			if (longVal(row.get("type")) == 1L) {
				ids.add(longVal(row.get("item_id")));
			}
		}
		Map<Long, Items> byId = Map.of();
		if (!ids.isEmpty()) {
			List<Items> entities = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, ids);
			byId = entities.stream()
					.filter(i -> i.getItemId() != null)
					.collect(Collectors.toMap(Items::getItemId, i -> i, (a, b) -> a));
		}
		for (Map<String, Object> row : list) {
			if (longVal(row.get("type")) != 1L) {
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
