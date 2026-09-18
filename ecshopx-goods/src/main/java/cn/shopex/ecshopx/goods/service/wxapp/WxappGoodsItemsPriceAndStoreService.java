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

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.goods.service.items.DistributorItemsDetailMergeService;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsPriceAndStoreService {

	private static final List<String> PRICE_STORE_WHITELIST = List.of(
			"item_id",
			"price",
			"market_price",
			"cost_price",
			"member_price",
			"act_price",
			"activity_price",
			"vip_price",
			"svip_price",
			"store");

	private final WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	private final OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader;
	private final DistributorItemsDetailMergeService distributorItemsDetailMergeService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final WxappGoodsItemsDetailActivityMergeService wxappGoodsItemsDetailActivityMergeService;
	private final WxappGoodsItemsDetailNormalBranchService wxappGoodsItemsDetailNormalBranchService;
	private final SkuValidMarketingActivityService skuValidMarketingActivityService;
	private final LangueProperties langueProperties;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;

	public WxappGoodsItemsPriceAndStoreService(WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService,
			OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader,
			DistributorItemsDetailMergeService distributorItemsDetailMergeService,
			PlatformItemsDetailCoreService platformItemsDetailCoreService,
			WxappGoodsItemsDetailActivityMergeService wxappGoodsItemsDetailActivityMergeService,
			WxappGoodsItemsDetailNormalBranchService wxappGoodsItemsDetailNormalBranchService,
			SkuValidMarketingActivityService skuValidMarketingActivityService,
			LangueProperties langueProperties,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher) {
		this.wxappGoodsItemsDetailPromotionActivityService = wxappGoodsItemsDetailPromotionActivityService;
		this.operatorCartCompanyProductModelReader = operatorCartCompanyProductModelReader;
		this.distributorItemsDetailMergeService = distributorItemsDetailMergeService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.wxappGoodsItemsDetailActivityMergeService = wxappGoodsItemsDetailActivityMergeService;
		this.wxappGoodsItemsDetailNormalBranchService = wxappGoodsItemsDetailNormalBranchService;
		this.skuValidMarketingActivityService = skuValidMarketingActivityService;
		this.langueProperties = langueProperties;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
	}

	public Map<String, Object> execute(HttpServletRequest request, String pathItemIdRaw, long companyId, long userId, String authorizerAppId,
			long distributorId) {
		Long pathItemId = parsePathItemIdStrict(pathItemIdRaw);
		if (pathItemId == null) {
			return Map.of("item_id", 0L);
		}
		Map<String, Object> promotionActivityData =
				wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, pathItemId, distributorId);
		List<Long> limitItemIds = deriveLimitItemIds(promotionActivityData);
		long effectiveItemId = pathItemId;
		if (!limitItemIds.isEmpty() && !limitItemIds.contains(pathItemId)) {
			effectiveItemId = limitItemIds.get(0);
		}
		List<Long> limitItemIdsForDetail = limitItemIds.isEmpty() ? List.of() : List.copyOf(limitItemIds);
		String productModel = operatorCartCompanyProductModelReader.getProductModel(companyId);
		Map<String, Object> data;
		if (distributorId > 0) {
			data = distributorItemsDetailMergeService.merge(companyId, effectiveItemId, distributorId, authorizerAppId, productModel, limitItemIdsForDetail);
		} else {
			data = platformItemsDetailCoreService.build(companyId, effectiveItemId, authorizerAppId, limitItemIdsForDetail);
		}
		if (isInvalidDetail(data)) {
			return Map.of("item_id", 0L);
		}
		Object baselineStore = data.get("item_total_store") != null ? data.get("item_total_store") : data.get("store");
		String activityType = promotionActivityData == null ? "" : String.valueOf(promotionActivityData.get("activity_type"));
		boolean activityBranch = promotionActivityData != null
				&& ("limited_time_sale".equals(activityType) || "seckill".equals(activityType) || "group".equals(activityType));
		if (activityBranch) {
			wxappGoodsItemsDetailActivityMergeService.applyActivityBranchToDetail(data, promotionActivityData, effectiveItemId, companyId, userId,
					distributorId, skuValidMarketingActivityService);
		} else {
			wxappGoodsItemsDetailNormalBranchService.applyNormalBranchToDetail(data, companyId, userId, distributorId, RequestLangTag.current(langueProperties));
		}
		Object mergedStore = data.get("item_total_store") != null ? data.get("item_total_store") : data.get("store");
		data.put("store", mergedStore);
		// 拼团/秒杀跳过配送合成；限时特惠按归属方配送能力合成
		boolean applyDisplayTotal = !ItemLogisticsStoreEnricher.isActivitySkipDisplay(Map.of("activity_type", activityType));
		itemLogisticsStoreEnricher.enrichDetailAndApplyDisplayTotal(
				companyId, distributorId, data, applyDisplayTotal);
		applyGroupStoreMin(data, promotionActivityData, effectiveItemId, baselineStore);
		return projectPriceAndStoreWhitelist(data);
	}

	private static Long parsePathItemIdStrict(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 1L) {
				return null;
			}
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private List<Long> deriveLimitItemIds(Map<String, Object> promotionActivityData) {
		if (promotionActivityData == null) {
			return List.of();
		}
		if ("limited_buy".equals(String.valueOf(promotionActivityData.get("activity_type")))) {
			return List.of();
		}
		Object listObj = promotionActivityData.get("list");
		if (!(listObj instanceof Map<?, ?> lm)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object v : lm.values()) {
			if (v instanceof Map<?, ?> row) {
				Object iid = row.get("item_id");
				if (iid instanceof Number n) {
					out.add(n.longValue());
				}
			}
		}
		return out;
	}

	private static boolean isInvalidDetail(Map<String, Object> r) {
		if (r == null || r.isEmpty()) {
			return true;
		}
		Object id = r.get("item_id");
		if (id == null) {
			return true;
		}
		long v;
		if (id instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(id.toString().trim());
			} catch (NumberFormatException e) {
				return true;
			}
		}
		return v < 1L;
	}

	private static void applyGroupStoreMin(Map<String, Object> data, Map<String, Object> promotionActivityData, long effectiveItemId,
			Object baselineStore) {
		if (promotionActivityData == null) {
			return;
		}
		if (!"group".equals(String.valueOf(promotionActivityData.get("activity_type")))) {
			return;
		}
		Map<String, Object> row = activityListRow(promotionActivityData, effectiveItemId);
		if (row == null || !row.containsKey("store")) {
			return;
		}
		long current = toLongForStore(data.get("store"));
		long baseline = toLongForStore(baselineStore);
		data.put("store", Math.min(current, baseline));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> activityListRow(Map<String, Object> promotionActivityData, long itemId) {
		Object listObj = promotionActivityData.get("list");
		if (!(listObj instanceof Map<?, ?> listMap)) {
			return null;
		}
		Map<String, Map<String, Object>> m = (Map<String, Map<String, Object>>) listMap;
		Map<String, Object> r = m.get(String.valueOf(itemId));
		if (r != null) {
			return r;
		}
		Object keyed = m.get(itemId);
		return keyed instanceof Map<?, ?> km ? (Map<String, Object>) km : null;
	}

	private static long toLongForStore(Object o) {
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

	private static Map<String, Object> projectPriceAndStoreWhitelist(Map<String, Object> data) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (String k : PRICE_STORE_WHITELIST) {
			if (data.containsKey(k)) {
				result.put(k, data.get(k));
			}
		}
		Object specRaw = data.get("spec_items");
		if (specRaw instanceof List<?> specList) {
			List<Map<String, Object>> projected = new ArrayList<>();
			for (Object o : specList) {
				if (o instanceof Map<?, ?> rm) {
					Map<String, Object> row = new LinkedHashMap<>();
					@SuppressWarnings("unchecked")
					Map<String, Object> src = (Map<String, Object>) rm;
					for (String k : PRICE_STORE_WHITELIST) {
						if (src.containsKey(k)) {
							row.put(k, src.get(k));
						}
					}
					projected.add(row);
				}
			}
			result.put("spec_items", projected);
		}
		return result;
	}
}
