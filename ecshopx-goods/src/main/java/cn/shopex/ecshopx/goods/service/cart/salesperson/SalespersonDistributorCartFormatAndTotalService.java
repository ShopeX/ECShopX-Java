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

package cn.shopex.ecshopx.goods.service.cart.salesperson;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailPromotionActivityService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonDistributorCartFormatAndTotalService {

	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	private final DistributorListQueryService distributorListQueryService;

	public SalespersonDistributorCartFormatAndTotalService(
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService,
			DistributorListQueryService distributorListQueryService) {
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.wxappGoodsItemsDetailPromotionActivityService = wxappGoodsItemsDetailPromotionActivityService;
		this.distributorListQueryService = distributorListQueryService;
	}

	public Map<String, Object> apply(long companyId, long userId, Map<String, Object> handledCartResult, boolean isSubmit,
			long distributorId, String shopTypeKey, String acceptLanguageHeader) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validIn = (List<Map<String, Object>>) handledCartResult.get("valid_cart");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> invalidIn = (List<Map<String, Object>>) handledCartResult.get("invalid_cart");

		List<Map<String, Object>> validRows = deepCopyRows(validIn);
		if (validRows == null) {
			validRows = new ArrayList<>();
		}
		for (Map<String, Object> row : validRows) {
			row.put("company_id", companyId);
		}

		goodsItemsListPromotionEnrichmentService.enrich(validRows);

		for (Map<String, Object> row : validRows) {
			long itemId = toLong(row.get("item_id"));
			long distForAct = resolveDistributorIdForActivity(row, distributorId);
			Map<String, Object> act = wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, itemId, distForAct);
			if (act != null && !act.isEmpty()) {
				mergeActivityOntoRow(row, act);
			}
		}

		if (Boolean.FALSE.equals(handledCartResult.get("is_check_store"))) {
			for (Map<String, Object> row : validRows) {
				assertStoreCoversNum(row, isSubmit);
				Object ch = row.get("children");
				if (ch instanceof List<?> childList) {
					for (Object c : childList) {
						if (c instanceof Map<?, ?> cm) {
							@SuppressWarnings("unchecked")
							Map<String, Object> childRow = (Map<String, Object>) cm;
							assertStoreCoversNum(childRow, isSubmit);
						}
					}
				}
			}
			handledCartResult.put("is_check_store", Boolean.TRUE);
		}

		if (userId > 0L) {
			wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, validRows, acceptLanguageHeader);
		}

		List<Map<String, Object>> shopBuckets = buildAggregatedShopCarts(companyId, validRows, shopTypeKey);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("valid_cart", shopBuckets);
		out.put("invalid_cart", invalidIn != null ? invalidIn : List.of());
		Object checkStoreFlag = handledCartResult.get("is_check_store");
		out.put("is_check_store", checkStoreFlag instanceof Boolean b ? b : Boolean.FALSE);
		return out;
	}

	private static void assertStoreCoversNum(Map<String, Object> row, boolean isSubmit) {
		if (!isSubmit) {
			return;
		}
		long store = toLong(row.get("store"));
		long num = toLong(row.get("num"));
		if (store < num) {
			Object name = row.get("item_name");
			String itemName = name == null ? "" : name.toString();
			throw new ResourceException(itemName + "库存不足");
		}
	}

	private List<Map<String, Object>> buildAggregatedShopCarts(long companyId, List<Map<String, Object>> validRows, String shopTypeKey) {
		Map<Long, List<Map<String, Object>>> byShop = new LinkedHashMap<>();
		for (Map<String, Object> row : validRows) {
			long sid = toLong(row.get("shop_id"));
			byShop.computeIfAbsent(sid, k -> new ArrayList<>()).add(row);
		}
		List<Long> shopIds = new ArrayList<>(byShop.keySet());
		List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, shopIds);
		Map<Long, Distributor> distById =
				distributors.stream().filter(d -> d.getDistributorId() != null).collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));

		List<Map<String, Object>> shops = new ArrayList<>();
		for (Map.Entry<Long, List<Map<String, Object>>> e : byShop.entrySet()) {
			long shopId = e.getKey();
			List<Map<String, Object>> list = e.getValue();
			Distributor d = distById.get(shopId);
			Map<String, Object> shop = new LinkedHashMap<>();
			shop.put("shop_id", shopId);
			shop.put("shop_type", shopTypeKey);
			if (d != null) {
				shop.put("shop_name", d.getName() != null ? d.getName() : "");
				shop.put("address", d.getAddress() != null ? d.getAddress() : "");
				shop.put("mobile", d.getMobile() != null ? d.getMobile() : "");
				shop.put("lat", d.getLat() != null ? d.getLat() : "");
				shop.put("lng", d.getLng() != null ? d.getLng() : "");
				shop.put("hour", d.getHour() != null ? d.getHour() : "");
				shop.put("is_ziti", Boolean.TRUE.equals(d.getIsZiti()));
				shop.put("is_delivery", Boolean.TRUE.equals(d.getIsDelivery()));
			} else {
				shop.put("shop_name", "");
				shop.put("address", "");
				shop.put("mobile", "");
				shop.put("lat", "");
				shop.put("lng", "");
				shop.put("hour", "");
				shop.put("is_ziti", false);
				shop.put("is_delivery", true);
			}

			long itemFee = 0L;
			long totalNum = 0L;
			long totalCount = 0L;
			for (Map<String, Object> line : list) {
				if (!truthyChecked(line.get("is_checked"))) {
					continue;
				}
				long num = toLong(line.get("num"));
				if (num <= 0L) {
					continue;
				}
				long price = toLong(line.get("price"));
				itemFee += price * num;
				totalNum += num;
				totalCount += 1;
			}
			shop.put("item_fee", itemFee);
			shop.put("cart_total_price", itemFee);
			shop.put("cart_total_num", totalNum);
			shop.put("cart_total_count", totalCount);
			shop.put("discount_fee", 0L);
			shop.put("member_discount", 0L);
			shop.put("total_fee", itemFee);
			shop.put("used_activity", List.of());
			shop.put("used_activity_ids", List.of());
			shop.put("activity_grouping", List.of());
			shop.put("vipgrade_guide_title", "");
			shop.put("gift_activity", List.of());
			shop.put("plus_buy_activity", List.of());
			shop.put("list", list);
			shops.add(shop);
		}
		return shops;
	}

	private static void mergeActivityOntoRow(Map<String, Object> row, Map<String, Object> act) {
		for (Map.Entry<String, Object> en : act.entrySet()) {
			String k = en.getKey();
			if (!StringUtils.hasText(k)) {
				continue;
			}
			if (k.startsWith("activity") || k.startsWith("limited") || k.startsWith("seckill") || "list".equals(k) || "promotion_id".equals(k)) {
				row.putIfAbsent(k, en.getValue());
			}
		}
	}

	private static long resolveDistributorIdForActivity(Map<String, Object> row, long filterDistributorId) {
		long sid = toLong(row.get("shop_id"));
		if (sid > 0L) {
			return sid;
		}
		return filterDistributorId;
	}

	private static List<Map<String, Object>> deepCopyRows(List<Map<String, Object>> src) {
		if (src == null) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>(src.size());
		for (Map<String, Object> m : src) {
			out.add(new LinkedHashMap<>(m));
		}
		return out;
	}

	private static boolean truthyChecked(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
