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

package cn.shopex.ecshopx.goods.service.recommend;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.goods.GoodsRecommendCheckoutMergePort;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleMainItem;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleRecommendItem;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMainItemMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleRecommendItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class GoodsRecommendCheckoutMergeService implements GoodsRecommendCheckoutMergePort {

	public static final String CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS =
			"_checkout_recommend_merge_request_items";

	public static final String CHECKOUT_RECOMMEND_REQUEST_ITEMS = "_checkout_recommend_request_items";

	public static final String ITEM_IS_RECOMMEND = "is_recommend";

	private static final Set<String> SUPPORTED_ORDER_TYPES =
			Set.of(
					"normal",
					"normal_drug",
					"normal_shopguide",
					"normal_community",
					"normal_excard",
					"normal_employee_purchase",
					"normal_shopadmin");

	private final GoodsRecommendRuleMainItemMapper mainItemMapper;

	private final GoodsRecommendRuleRecommendItemMapper recommendItemMapper;

	private final ItemsMapper itemsMapper;

	private final GoodsRecommendSalabilityResolver salabilityResolver;

	private final GoodsRecommendGoodsIdResolver goodsIdResolver;

	private final MessageSource messageSource;

	public GoodsRecommendCheckoutMergeService(
			GoodsRecommendRuleMainItemMapper mainItemMapper,
			GoodsRecommendRuleRecommendItemMapper recommendItemMapper,
			ItemsMapper itemsMapper,
			GoodsRecommendSalabilityResolver salabilityResolver,
			GoodsRecommendGoodsIdResolver goodsIdResolver,
			MessageSource messageSource) {
		this.mainItemMapper = mainItemMapper;
		this.recommendItemMapper = recommendItemMapper;
		this.itemsMapper = itemsMapper;
		this.salabilityResolver = salabilityResolver;
		this.goodsIdResolver = goodsIdResolver;
		this.messageSource = messageSource;
	}

	@Override
	public void apply(long companyId, Map<String, Object> params) {
		if (params == null || !params.containsKey("recommend_item_id")) {
			return;
		}
		List<GoodsRecommendRequestParser.RecommendAddLine> recommendLines;
		try {
			recommendLines = GoodsRecommendRequestParser.parseRecommendItemLines(params.get("recommend_item_id"));
		} catch (IllegalArgumentException e) {
			throw badRequest(GoodsRecommendErrorCodes.ITEM_IDS_INVALID);
		}
		params.remove("recommend_item_id");
		if (recommendLines.isEmpty()) {
			return;
		}

		String orderType = stringVal(params.get("order_type")).toLowerCase();
		if (!SUPPORTED_ORDER_TYPES.contains(orderType)) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ORDER_TYPE_UNSUPPORTED);
		}

		GoodsRecommendRequestParser.RecommendMergeDistributorIdResult distributorResult =
				GoodsRecommendRequestParser.resolveDistributorIdForRecommendMerge(
						salabilityResolver.resolveProductModel(companyId), params.get("distributor_id"));
		if (!distributorResult.isOk()) {
			throw badRequest(distributorResult.errorCode());
		}

		List<Map<String, Object>> items = extractItemLines(params.get("items"));
		if (items.isEmpty() && !isMemberCartCheckout(params)) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_CONTEXT_INVALID);
		}

		List<Map<String, Object>> recommendRows =
				buildRecommendRows(
						companyId, distributorResult.distributorId(), recommendLines, extractItemIds(items));
		params.put(CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS, Boolean.TRUE);
		params.put(CHECKOUT_RECOMMEND_REQUEST_ITEMS, recommendRows);
		if (!isMemberCartCheckout(params)) {
			items.addAll(recommendRows);
			params.put("items", items);
		}
	}

	private List<Map<String, Object>> buildRecommendRows(
			long companyId,
			long distributorId,
			List<GoodsRecommendRequestParser.RecommendAddLine> recommendLines,
			List<Long> settlementItemIds) {
		List<Items> recommendItems = new ArrayList<>();
		for (GoodsRecommendRequestParser.RecommendAddLine line : recommendLines) {
			recommendItems.add(loadItem(companyId, line.itemId()));
		}
		Map<Long, Items> defaultSkuBySpuId = salabilityResolver.loadDefaultSkus(companyId, recommendItems);
		Map<Long, Integer> maxSkuStoreBySpuId =
				salabilityResolver.loadMaxSkuStoreBySpuId(companyId, recommendItems);
		List<Long> platformItemIds = new ArrayList<>();
		for (Items item : recommendItems) {
			if (item.getDistributorId() == null || item.getDistributorId() == 0) {
				platformItemIds.add(item.getItemId());
			}
		}
		GoodsRecommendSalabilityResolver.DistributorContext distributorContext =
				salabilityResolver.loadDistributorContext(companyId, distributorId, platformItemIds);
		Map<Long, DistributorItems> distributorItems = distributorContext.byItemId();
		List<Map<String, Object>> recommendRows = new ArrayList<>();
		for (int i = 0; i < recommendLines.size(); i++) {
			GoodsRecommendRequestParser.RecommendAddLine line = recommendLines.get(i);
			Items recommendItem = recommendItems.get(i);
			validateRecommendRelation(companyId, recommendItem, settlementItemIds);
			if (!salabilityResolver.isSellable(
					recommendItem,
					companyId,
					distributorId,
					distributorItems,
					defaultSkuBySpuId,
					maxSkuStoreBySpuId,
					distributorContext.storeBySpuId())) {
				throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
			}
			mergeRecommendLine(recommendRows, line.itemId(), line.num());
		}
		return recommendRows;
	}

	private void validateRecommendRelation(
			long companyId, Items recommendItem, List<Long> settlementItemIds) {
		long recommendGoodsId = goodsIdResolver.resolveGoodsId(recommendItem);
		List<GoodsRecommendRuleRecommendItem> recommendRows =
				recommendItemMapper.selectList(
						new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
								.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
								.eq(GoodsRecommendRuleRecommendItem::getGoodsId, recommendGoodsId));
		if (recommendRows.isEmpty()) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}
		if (settlementItemIds.isEmpty()) {
			return;
		}
		Map<Long, Items> settlementItems = loadItems(companyId, settlementItemIds);
		java.util.Set<Long> settlementGoodsIds = new java.util.HashSet<>();
		for (Long itemId : settlementItemIds) {
			Items item = settlementItems.get(itemId);
			long goodsId = goodsIdResolver.resolveGoodsId(item);
			if (goodsId > 0) {
				settlementGoodsIds.add(goodsId);
			}
		}
		if (settlementGoodsIds.isEmpty()) {
			return;
		}
		java.util.Set<Long> ruleIds = new java.util.HashSet<>();
		for (GoodsRecommendRuleRecommendItem row : recommendRows) {
			if (row.getRuleId() != null) {
				ruleIds.add(row.getRuleId());
			}
		}
		long mainHits =
				mainItemMapper.selectCount(
						new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
								.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
								.in(GoodsRecommendRuleMainItem::getRuleId, ruleIds)
								.in(GoodsRecommendRuleMainItem::getGoodsId, settlementGoodsIds));
		if (mainHits == 0) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}
	}

	private Map<Long, Items> loadItems(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		return itemsMapper
				.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.in(Items::getItemId, itemIds))
				.stream()
				.collect(
						java.util.stream.Collectors.toMap(Items::getItemId, i -> i, (a, b) -> a));
	}

	private Items loadItem(long companyId, long itemId) {
		Items item =
				itemsMapper.selectOne(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.eq(Items::getItemId, itemId)
								.last("LIMIT 1"));
		if (item == null) {
			throw badRequest(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN);
		}
		return item;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> extractItemLines(Object raw) {
		if (raw instanceof String s) {
			String trimmed = s.trim();
			if (trimmed.isEmpty() || "[]".equals(trimmed)) {
				return new ArrayList<>();
			}
			try {
				raw = GoodsRecommendRequestParser.parseJsonArray(trimmed);
			} catch (IllegalArgumentException e) {
				return new ArrayList<>();
			}
		}
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (el instanceof Map<?, ?> map) {
				out.add(new LinkedHashMap<>((Map<String, Object>) map));
			}
		}
		return out;
	}

	private static List<Long> extractItemIds(List<Map<String, Object>> lines) {
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> line : lines) {
			long itemId = longVal(line.get("item_id"), 0L);
			if (itemId > 0) {
				out.add(itemId);
			}
		}
		return out;
	}

	private static boolean isMemberCartCheckout(Map<String, Object> params) {
		String cartType = stringVal(params.get("cart_type"));
		return "cart".equalsIgnoreCase(cartType) || "fastbuy".equalsIgnoreCase(cartType);
	}

	public static boolean isRecommendLine(Map<String, Object> line) {
		return line != null && Boolean.TRUE.equals(line.get(ITEM_IS_RECOMMEND));
	}

	private static void mergeRecommendLine(List<Map<String, Object>> lines, long checkoutSkuId, int num) {
		for (Map<String, Object> line : lines) {
			if (longVal(line.get("item_id"), 0L) == checkoutSkuId) {
				int existing = (int) longVal(line.get("num"), 0L);
				line.put("num", existing + num);
				return;
			}
		}
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", checkoutSkuId);
		line.put("num", num);
		line.put("activity_type", "normal");
		line.put(ITEM_IS_RECOMMEND, Boolean.TRUE);
		lines.add(line);
	}

	private BadRequestException badRequest(String errorCode) {
		return new BadRequestException(GoodsRecommendErrorMessages.message(messageSource, errorCode));
	}

	private static long longVal(Object raw, long def) {
		if (raw == null) {
			return def;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}
}
