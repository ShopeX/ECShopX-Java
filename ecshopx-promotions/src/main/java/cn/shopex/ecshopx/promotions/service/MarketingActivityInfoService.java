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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityCategory;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityCategoryMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityInfoService {

	private final MarketingActivityItemListActivityQuerySupport marketingActivityItemListActivityQuerySupport;
	private final MarketingActivityInfoAssemblySupport marketingActivityInfoAssemblySupport;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MarketingActivityCategoryMapper marketingActivityCategoryMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final DistributorListQueryService distributorListQueryService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public MarketingActivityInfoService(
			MarketingActivityItemListActivityQuerySupport marketingActivityItemListActivityQuerySupport,
			MarketingActivityInfoAssemblySupport marketingActivityInfoAssemblySupport,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			MarketingActivityCategoryMapper marketingActivityCategoryMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			DistributorListQueryService distributorListQueryService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.marketingActivityItemListActivityQuerySupport = marketingActivityItemListActivityQuerySupport;
		this.marketingActivityInfoAssemblySupport = marketingActivityInfoAssemblySupport;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.marketingActivityCategoryMapper = marketingActivityCategoryMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.distributorListQueryService = distributorListQueryService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getMarketingActivityInfo(long companyId, long marketingId) {
		Optional<MarketingActivity> opt = marketingActivityItemListActivityQuerySupport.loadActivityRow(companyId, marketingId);
		if (opt.isEmpty()) {
			throw new ResourceException("活动不存在");
		}
		MarketingActivity row = opt.get();
		Map<String, Object> result = new LinkedHashMap<>(marketingActivityItemListActivityQuerySupport.buildActivityPayloadForItemList(row));
		result.put("marketing_desc", row.getMarketingDesc());
		result.put("ad_pic", row.getAdPic());
		result.put("free_postage", row.getFreePostage());
		result.put("is_increase_purchase", row.getIsIncreasePurchase());
		result.put("rel_marketing_id", String.valueOf(row.getRelMarketingId() != null ? row.getRelMarketingId() : 0L));

		String marketingType = row.getMarketingType() != null ? row.getMarketingType() : "";

		LambdaQueryWrapper<MarketingActivityItems> itemW = new LambdaQueryWrapper<>();
		itemW.eq(MarketingActivityItems::getCompanyId, companyId)
				.eq(MarketingActivityItems::getMarketingId, marketingId)
				.orderByAsc(MarketingActivityItems::getId);
		List<MarketingActivityItems> activityItemRows = marketingActivityItemsMapper.selectList(itemW);
		List<Long> orderedActivityItemIds = new ArrayList<>();
		for (MarketingActivityItems ar : activityItemRows) {
			if (ar.getItemId() != null) {
				orderedActivityItemIds.add(ar.getItemId());
			}
		}
		List<Long> uniqueSkuIds = orderedActivityItemIds.stream().filter(Objects::nonNull).distinct().toList();
		Set<Long> catalogItemIds = new HashSet<>();
		if (!uniqueSkuIds.isEmpty()) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> skuList =
					(List<Map<String, Object>>)
							marketingActivityCatalogAccess.loadSkuItemsList(companyId, uniqueSkuIds).get("list");
			for (Map<String, Object> skuRow : skuList) {
				Object iid = skuRow.get("item_id");
				if (iid instanceof Number n) {
					catalogItemIds.add(n.longValue());
				}
			}
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (MarketingActivityItems ar : activityItemRows) {
			Map<String, Object> itemMap = activityItemRowToMap(ar);
			Long itemId = ar.getItemId();
			itemMap.put("status", itemId != null && catalogItemIds.contains(itemId) ? "valid" : "invalid");
			items.add(itemMap);
		}
		result.put("items", items);
		result.put(
				"itemTreeLists",
				marketingActivityCatalogAccess.buildMarketingActivityItemTreeLists(companyId, orderedActivityItemIds));

		LambdaQueryWrapper<MarketingGiftItems> giftW = new LambdaQueryWrapper<>();
		giftW.eq(MarketingGiftItems::getCompanyId, companyId)
				.eq(MarketingGiftItems::getMarketingId, marketingId)
				.orderByAsc(MarketingGiftItems::getId);
		List<MarketingGiftItems> giftRows = marketingGiftItemsMapper.selectList(giftW);
		List<Long> giftItemIds =
				giftRows.stream().map(MarketingGiftItems::getItemId).filter(Objects::nonNull).distinct().toList();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> giftSkuList =
				giftItemIds.isEmpty()
						? List.of()
						: (List<Map<String, Object>>)
								marketingActivityCatalogAccess
										.loadSkuItemsListForMarketingGift(companyId, giftItemIds)
										.get("list");
		Map<Long, Map<String, Object>> giftSkuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : giftSkuList) {
			Object iid = skuRow.get("item_id");
			if (iid instanceof Number n) {
				giftSkuByItemId.putIfAbsent(n.longValue(), skuRow);
			}
		}
		Map<Object, List<Map<String, Object>>> relGiftGoods = new LinkedHashMap<>();
		Map<Object, List<Map<String, Object>>> relGiftItems = new LinkedHashMap<>();
		for (MarketingGiftItems g : giftRows) {
			Object priceKey = priceKeyForGrouping(g.getPrice());
			Map<String, Object> giftMap = giftEntityToMap(g);
			Map<String, Object> sku = g.getItemId() != null ? giftSkuByItemId.get(g.getItemId()) : null;
			if (sku == null || sku.isEmpty()) {
				giftMap.put("status", "invalid");
			} else {
				giftMap.put("status", "valid");
				Map<String, Object> mergedSku = new LinkedHashMap<>(sku);
				mergedSku.put("gift_num", g.getGiftNum() != null ? g.getGiftNum() : 0);
				mergedSku.put("without_return", g.getWithoutReturn() != null ? g.getWithoutReturn() : false);
				addToPriceBucket(relGiftItems, priceKey, mergedSku);
			}
			addToPriceBucket(relGiftGoods, priceKey, giftMap);
		}
		switch (marketingType) {
			case "full_gift" -> applyFullGiftGifts(result, relGiftGoods, relGiftItems);
			case "plus_price_buy" -> applyPlusPriceBuyGifts(result, relGiftGoods, relGiftItems);
			default -> {
				result.put("gifts", List.of());
				result.put("giftsItemLists", List.of());
			}
		}

		int startEpoch = row.getStartTime() != null ? row.getStartTime() : 0;
		int endEpoch = row.getEndTime() != null ? row.getEndTime() : 0;
		marketingActivityInfoAssemblySupport.applyStartEndTimeStringOverrides(result, startEpoch, endEpoch);

		Object shopIdsVal = result.get("shop_ids");
		List<Long> filtered = marketingActivityInfoAssemblySupport.filterTruthyShopIds(shopIdsVal);
		if (!filtered.isEmpty()) {
			List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId, filtered);
			List<Map<String, Object>> storeRows = new ArrayList<>();
			for (Distributor d : dists) {
				long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
				int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
				Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
				storeRows.add(distributorListRowFormatService.formatStoreRow(d, setting, objectMapper));
			}
			result.put("storeLists", storeRows);
		}

		LambdaQueryWrapper<MarketingActivityCategory> catWrapper = new LambdaQueryWrapper<>();
		catWrapper.eq(MarketingActivityCategory::getCompanyId, companyId)
				.eq(MarketingActivityCategory::getMarketingId, marketingId)
				.orderByAsc(MarketingActivityCategory::getId);
		List<MarketingActivityCategory> catRows = marketingActivityCategoryMapper.selectList(catWrapper);
		marketingActivityInfoAssemblySupport.putCategorySplitsFromRows(catRows, result);
		if (!result.containsKey("rel_category_ids")) {
			result.put("rel_category_ids", List.of());
		}
		if (!result.containsKey("item_category")) {
			result.put("item_category", List.of());
		}

		result.put("rel_tag_ids", result.get("tag_ids"));
		List<Long> tagLongs = coerceTagBrandIds(result.get("tag_ids"));
		result.put("tag_list", marketingActivityCatalogAccess.listTagsForMarketingActivityInfo(companyId, tagLongs));

		result.put("rel_brand_ids", result.get("brand_ids"));
		List<Long> brandLongs = coerceTagBrandIds(result.get("brand_ids"));
		result.put("brand_list", marketingActivityCatalogAccess.listBrandAttributesForMarketingActivityInfo(companyId, brandLongs));

		return result;
	}

	private static void applyFullGiftGifts(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		List<Map<String, Object>> g0 = relGiftGoods.get(0);
		if (g0 != null && !g0.isEmpty()) {
			result.put("gifts", new ArrayList<>(g0));
		}
		List<Map<String, Object>> i0 = relGiftItems.get(0);
		if (i0 != null && !i0.isEmpty()) {
			result.put("giftsItemLists", new ArrayList<>(i0));
		}
	}

	private static void applyPlusPriceBuyGifts(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		if (relGiftGoods.isEmpty() || relGiftItems.isEmpty()) {
			return;
		}
		List<Map<String, Object>> giftsOut = new ArrayList<>();
		List<Map<String, Object>> giftsItemListsOut = new ArrayList<>();
		for (Map.Entry<Object, List<Map<String, Object>>> en : relGiftGoods.entrySet()) {
			Object pk = en.getKey();
			List<Map<String, Object>> goodsList = en.getValue();
			List<Map<String, Object>> itemList = relGiftItems.get(pk);
			if (itemList == null || itemList.isEmpty() || goodsList == null || goodsList.isEmpty()) {
				continue;
			}
			Map<String, Object> g1 = new LinkedHashMap<>();
			g1.put("price", priceJsonValue(pk));
			g1.put("gift_item", new ArrayList<>(goodsList));
			giftsOut.add(g1);
			Map<String, Object> g2 = new LinkedHashMap<>();
			g2.put("price", priceJsonValue(pk));
			g2.put("gift_item", new ArrayList<>(itemList));
			giftsItemListsOut.add(g2);
		}
		if (!giftsOut.isEmpty()) {
			result.put("gifts", giftsOut);
			result.put("giftsItemLists", giftsItemListsOut);
		}
	}

	private static Object priceJsonValue(Object priceKey) {
		if (priceKey instanceof Integer i && i == 0) {
			return 0;
		}
		return priceKey;
	}

	private static Object priceKeyForGrouping(Integer priceCents) {
		if (priceCents == null || priceCents == 0) {
			return 0;
		}
		return BigDecimal.valueOf(priceCents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static void addToPriceBucket(
			Map<Object, List<Map<String, Object>>> byPrice, Object key, Map<String, Object> row) {
		byPrice.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
	}

	private Map<String, Object> activityItemRowToMap(MarketingActivityItems r) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (r.getId() != null) {
			m.put("id", r.getId());
		}
		if (r.getMarketingId() != null) {
			m.put("marketing_id", r.getMarketingId());
		}
		if (r.getItemId() != null) {
			m.put("item_id", r.getItemId());
		}
		if (r.getGoodsId() != null) {
			m.put("goods_id", r.getGoodsId());
		}
		m.put("is_show", r.getIsShow());
		m.put("item_spec_desc", r.getItemSpecDesc());
		m.put("marketing_type", r.getMarketingType());
		m.put("item_type", r.getItemType());
		m.put("item_name", r.getItemName());
		m.put("price", r.getPrice());
		m.put("item_brief", r.getItemBrief());
		m.put("pics", decodePicsForActivityItemRow(r.getPics()));
		m.put("promotion_tag", r.getPromotionTag());
		m.put("start_time", r.getStartTime());
		m.put("end_time", r.getEndTime());
		if (r.getCompanyId() != null) {
			m.put("company_id", r.getCompanyId());
		}
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}

	private Object decodePicsForActivityItemRow(String picsJson) {
		if (picsJson == null || picsJson.isBlank()) {
			return List.of("null");
		}
		try {
			Object parsed = objectMapper.readValue(picsJson.trim(), List.class);
			if (parsed instanceof List<?> l) {
				return new ArrayList<>(l);
			}
		} catch (Exception ignored) {
		}
		return List.of("null");
	}

	private static Map<String, Object> giftEntityToMap(MarketingGiftItems g) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (g.getId() != null) {
			m.put("id", g.getId());
		}
		if (g.getMarketingId() != null) {
			m.put("marketing_id", g.getMarketingId());
		}
		if (g.getCompanyId() != null) {
			m.put("company_id", g.getCompanyId());
		}
		if (g.getItemId() != null) {
			m.put("item_id", g.getItemId());
		}
		m.put("item_type", g.getItemType());
		m.put("item_name", g.getItemName());
		m.put("price", g.getPrice());
		m.put("store", g.getStore());
		m.put("gift_num", g.getGiftNum());
		m.put("pics", g.getPics());
		m.put("without_return", g.getWithoutReturn());
		m.put("condition_type", g.getConditionType());
		m.put("filter_full", g.getFilterFull());
		m.put("item_spec_desc", g.getItemSpecDesc());
		m.put("created", g.getCreated());
		m.put("updated", g.getUpdated());
		return m;
	}

	private static List<Long> coerceTagBrandIds(Object raw) {
		if (!(raw instanceof List<?> l)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : l) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null && StringUtils.hasText(o.toString())) {
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return out;
	}
}
