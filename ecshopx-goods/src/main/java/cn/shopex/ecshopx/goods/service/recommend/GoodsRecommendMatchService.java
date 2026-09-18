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

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendDisplaySetting;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleMainItem;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendRuleRecommendItem;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleMainItemMapper;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendRuleRecommendItemMapper;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GoodsRecommendMatchService {

	private final GoodsRecommendDisplaySettingService displaySettingService;

	private final GoodsRecommendRuleMainItemMapper mainItemMapper;

	private final GoodsRecommendRuleRecommendItemMapper recommendItemMapper;

	private final ItemsMapper itemsMapper;

	private final GoodsRecommendSalabilityResolver salabilityResolver;

	private final GoodsRecommendGoodsIdResolver goodsIdResolver;

	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;

	public GoodsRecommendMatchService(
			GoodsRecommendDisplaySettingService displaySettingService,
			GoodsRecommendRuleMainItemMapper mainItemMapper,
			GoodsRecommendRuleRecommendItemMapper recommendItemMapper,
			ItemsMapper itemsMapper,
			GoodsRecommendSalabilityResolver salabilityResolver,
			GoodsRecommendGoodsIdResolver goodsIdResolver,
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService) {
		this.displaySettingService = displaySettingService;
		this.mainItemMapper = mainItemMapper;
		this.recommendItemMapper = recommendItemMapper;
		this.itemsMapper = itemsMapper;
		this.salabilityResolver = salabilityResolver;
		this.goodsIdResolver = goodsIdResolver;
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
	}

	public Map<String, Object> emptyMatchResult() {
		return emptyResult();
	}

	public Map<String, Object> match(
			long companyId,
			GoodsRecommendScene scene,
			List<Long> mainItemIds,
			long distributorId,
			List<Long> excludeItemIds,
			long userId,
			String acceptLanguageHeader) {
		if (mainItemIds == null || mainItemIds.isEmpty()) {
			return emptyResult();
		}
		SceneConfig config = loadSceneConfig(companyId, scene);
		if (!config.enabled()) {
			return emptyResult();
		}

		List<Long> orderedMainIds = dedupePreserveOrder(mainItemIds);
		Set<Long> contextItemIds = new LinkedHashSet<>(orderedMainIds);
		if (excludeItemIds != null) {
			contextItemIds.addAll(excludeItemIds);
		}
		Map<Long, Items> contextItemsById = loadItems(companyId, contextItemIds);
		Set<Long> excludeGoodsIds = goodsIdResolver.resolveGoodsIdSet(contextItemsById, contextItemIds);

		Map<Long, Long> mainItemToGoodsId = goodsIdResolver.itemIdsToGoodsIds(contextItemsById, orderedMainIds);
		Map<Long, Long> mainGoodsToRuleId = loadMainGoodsRuleMap(companyId, mainItemToGoodsId.values());
		Map<Long, Long> mainItemToRuleId = new LinkedHashMap<>();
		for (Long mainItemId : orderedMainIds) {
			Long goodsId = mainItemToGoodsId.get(mainItemId);
			if (goodsId != null) {
				Long ruleId = mainGoodsToRuleId.get(goodsId);
				if (ruleId != null) {
					mainItemToRuleId.put(mainItemId, ruleId);
				}
			}
		}
		List<MatchCandidate> candidates =
				buildCandidates(companyId, orderedMainIds, mainItemToRuleId);

		if (candidates.isEmpty()) {
			return emptyResult();
		}

		Set<Long> allItemIds = new LinkedHashSet<>();
		allItemIds.addAll(orderedMainIds);
		candidates.forEach(c -> allItemIds.add(c.recommendItemId()));

		Map<Long, Items> itemsById = loadItems(companyId, allItemIds);
		Map<Long, Items> defaultSkuBySpuId = salabilityResolver.loadDefaultSkus(companyId, itemsById.values());
		Map<Long, Integer> maxSkuStoreBySpuId =
				salabilityResolver.loadMaxSkuStoreBySpuId(companyId, itemsById.values());
		List<Long> platformItemIds =
				allItemIds.stream()
						.filter(
								id -> {
									Items it = itemsById.get(id);
									return it != null
											&& (it.getDistributorId() == null || it.getDistributorId() == 0);
								})
						.collect(Collectors.toList());
		GoodsRecommendSalabilityResolver.DistributorContext distributorContext =
				salabilityResolver.loadDistributorContext(companyId, distributorId, platformItemIds);
		Map<Long, DistributorItems> distributorItemsById = distributorContext.byItemId();
		Map<Long, Integer> distributorStoreBySpuId = distributorContext.storeBySpuId();

		List<ResolvedMatch> resolved = new ArrayList<>();
		Set<Long> seenRecommendGoods = new HashSet<>();
		for (MatchCandidate candidate : candidates) {
			Items recommendItem = itemsById.get(candidate.recommendItemId());
			if (recommendItem == null) {
				continue;
			}
			long recommendGoodsId = goodsIdResolver.resolveGoodsId(recommendItem);
			if (excludeGoodsIds.contains(recommendGoodsId)
					|| seenRecommendGoods.contains(recommendGoodsId)) {
				continue;
			}
			Items mainItem = itemsById.get(candidate.mainItemId());
			if (!salabilityResolver.isRecommendSellableForMatch(
					mainItem,
					recommendItem,
					companyId,
					distributorId,
					distributorItemsById,
					defaultSkuBySpuId,
					maxSkuStoreBySpuId,
					distributorStoreBySpuId)) {
				continue;
			}
			seenRecommendGoods.add(recommendGoodsId);
			resolved.add(new ResolvedMatch(candidate.mainItemId(), recommendItem, distributorItemsById.get(recommendItem.getItemId())));
		}

		sortResolved(resolved, config.sort(), itemsById, distributorItemsById);
		if (resolved.size() > config.limit()) {
			resolved = resolved.subList(0, config.limit());
		}

		List<Map<String, Object>> items = new ArrayList<>();
		for (ResolvedMatch row : resolved) {
			items.add(toItemView(row, defaultSkuBySpuId, maxSkuStoreBySpuId, distributorStoreBySpuId));
		}
		if (!items.isEmpty()) {
			wxappGoodsItemsListMemberPriceApplyService.applyForRows(
					companyId, userId, items, acceptLanguageHeader);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("items", items);
		out.put("total", items.size());
		return out;
	}

	private void sortResolved(
			List<ResolvedMatch> resolved,
			String sort,
			Map<Long, Items> itemsById,
			Map<Long, DistributorItems> distributorItemsById) {
		Comparator<ResolvedMatch> comparator;
		if (GoodsRecommendDisplaySort.PRICE_ASC.equals(sort)) {
			comparator =
					Comparator.comparingInt(
							(ResolvedMatch r) ->
									salabilityResolver.resolvePrice(
											r.item(), distributorItemsById.get(r.item().getItemId())));
		} else {
			comparator =
					Comparator.comparingLong(
									(ResolvedMatch r) ->
											salabilityResolver.resolveSales(
													r.item(), distributorItemsById.get(r.item().getItemId())))
							.reversed();
		}
		comparator =
				comparator
						.thenComparing(r -> r.item().getCreated() != null ? r.item().getCreated() : 0)
						.thenComparing(r -> r.item().getItemId());
		resolved.sort(comparator);
	}

	private Map<String, Object> toItemView(
			ResolvedMatch row,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId,
			Map<Long, Integer> distributorStoreBySpuId) {
		Items item = row.item();
		DistributorItems di = row.distributorRow();
		Map<String, Object> view = new LinkedHashMap<>();
		view.put("item_id", item.getItemId());
		view.put("item_name", item.getItemName());
		view.put("price", salabilityResolver.resolvePrice(item, di));
		view.put("sales", salabilityResolver.resolveSales(item, di));
		view.put("pics", firstPicUrl(item.getPics()));
		view.put("distributor_id", item.getDistributorId());
		view.put("approve_status", item.getApproveStatus());
		view.put(
				"store",
				salabilityResolver.resolveStore(
						item, di, defaultSkuBySpuId, maxSkuStoreBySpuId, distributorStoreBySpuId));
		view.put("matched_main_item_id", row.mainItemId());
		return view;
	}

	private List<MatchCandidate> buildCandidates(
			long companyId, List<Long> orderedMainIds, Map<Long, Long> mainItemToRuleId) {
		Set<Long> ruleIds = new LinkedHashSet<>(mainItemToRuleId.values());
		if (ruleIds.isEmpty()) {
			return List.of();
		}
		List<GoodsRecommendRuleRecommendItem> allRecommendRows =
				recommendItemMapper.selectList(
						new LambdaQueryWrapper<GoodsRecommendRuleRecommendItem>()
								.eq(GoodsRecommendRuleRecommendItem::getCompanyId, companyId)
								.in(GoodsRecommendRuleRecommendItem::getRuleId, ruleIds)
								.orderByAsc(GoodsRecommendRuleRecommendItem::getSort)
								.orderByAsc(GoodsRecommendRuleRecommendItem::getId));
		Set<Long> recommendGoodsIds = new LinkedHashSet<>();
		for (GoodsRecommendRuleRecommendItem row : allRecommendRows) {
			if (row.getGoodsId() != null) {
				recommendGoodsIds.add(row.getGoodsId());
			}
		}
		Map<Long, Long> recommendItemIdByGoodsId =
				goodsIdResolver.loadDisplayItemIdByGoodsId(companyId, recommendGoodsIds);
		Map<Long, List<GoodsRecommendRuleRecommendItem>> rowsByRuleId = new LinkedHashMap<>();
		for (GoodsRecommendRuleRecommendItem row : allRecommendRows) {
			rowsByRuleId.computeIfAbsent(row.getRuleId(), k -> new ArrayList<>()).add(row);
		}
		List<MatchCandidate> candidates = new ArrayList<>();
		for (Long mainItemId : orderedMainIds) {
			Long ruleId = mainItemToRuleId.get(mainItemId);
			if (ruleId == null) {
				continue;
			}
			for (GoodsRecommendRuleRecommendItem row : rowsByRuleId.getOrDefault(ruleId, List.of())) {
				Long recommendItemId = recommendItemIdByGoodsId.get(row.getGoodsId());
				if (recommendItemId != null && recommendItemId > 0) {
					candidates.add(new MatchCandidate(mainItemId, recommendItemId));
				}
			}
		}
		return candidates;
	}

	private Map<Long, Long> loadMainGoodsRuleMap(long companyId, Collection<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return Map.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (Long id : goodsIds) {
			if (id != null && id > 0) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<GoodsRecommendRuleMainItem> rows =
				mainItemMapper.selectList(
						new LambdaQueryWrapper<GoodsRecommendRuleMainItem>()
								.eq(GoodsRecommendRuleMainItem::getCompanyId, companyId)
								.in(GoodsRecommendRuleMainItem::getGoodsId, ids));
		Map<Long, Long> map = new LinkedHashMap<>();
		for (GoodsRecommendRuleMainItem row : rows) {
			map.putIfAbsent(row.getGoodsId(), row.getRuleId());
		}
		return map;
	}

	private Map<Long, Items> loadItems(long companyId, Set<Long> itemIds) {
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		return itemsMapper
				.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.in(Items::getItemId, itemIds))
				.stream()
				.collect(Collectors.toMap(Items::getItemId, i -> i, (a, b) -> a));
	}

	private SceneConfig loadSceneConfig(long companyId, GoodsRecommendScene scene) {
		Map<String, Object> setting = displaySettingService.getDisplaySetting(companyId);
		return switch (scene) {
			case DETAIL ->
					new SceneConfig(
							flag(setting.get("detail_enabled")),
							intVal(setting.get("detail_limit")),
							stringVal(setting.get("detail_sort")));
			case CART ->
					new SceneConfig(
							flag(setting.get("cart_enabled")),
							intVal(setting.get("cart_limit")),
							stringVal(setting.get("cart_sort")));
			case CHECKOUT ->
					new SceneConfig(
							flag(setting.get("checkout_enabled")),
							intVal(setting.get("checkout_limit")),
							stringVal(setting.get("checkout_sort")));
			case ORDER_DETAIL ->
					new SceneConfig(
							flag(setting.get("order_detail_enabled")),
							intVal(setting.get("order_detail_limit")),
							stringVal(setting.get("order_detail_sort")));
		};
	}

	private static Map<String, Object> emptyResult() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("items", List.of());
		out.put("total", 0);
		return out;
	}

	private static List<Long> dedupePreserveOrder(List<Long> ids) {
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (Long id : ids) {
			if (id != null && id > 0) {
				set.add(id);
			}
		}
		return new ArrayList<>(set);
	}

	private static boolean flag(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		return Objects.equals("1", String.valueOf(raw)) || Boolean.parseBoolean(String.valueOf(raw));
	}

	private static int intVal(Object raw) {
		if (raw instanceof Number n) {
			return GoodsRecommendDisplayDefaults.clampLimit(n.intValue());
		}
		try {
			return GoodsRecommendDisplayDefaults.clampLimit(Integer.parseInt(String.valueOf(raw)));
		} catch (Exception e) {
			return GoodsRecommendDisplayDefaults.DEFAULT_LIMIT;
		}
	}

	private static String firstPicUrl(String picsJson) {
		Object resolved = GoodsItemsListRowMapper.resolvePicsForListRow(picsJson);
		if (resolved instanceof List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			return first != null ? first.toString() : "";
		}
		if (resolved instanceof String s) {
			return s;
		}
		return "";
	}

	private static String stringVal(Object raw) {
		if (raw == null) {
			return GoodsRecommendDisplayDefaults.DEFAULT_SORT;
		}
		String s = raw.toString().trim();
		return GoodsRecommendDisplaySort.isValid(s) ? s : GoodsRecommendDisplayDefaults.DEFAULT_SORT;
	}

	private record SceneConfig(boolean enabled, int limit, String sort) {}

	private record MatchCandidate(long mainItemId, long recommendItemId) {}

	private record ResolvedMatch(long mainItemId, Items item, DistributorItems distributorRow) {}
}
