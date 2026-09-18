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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemRowsSortKey;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityGoodsListQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityItemsListQueryMapper;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityGoodsListSqlFilter;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseActivityItemListService {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final EmployeePurchaseActivityGoodsListQueryMapper activityGoodsListQueryMapper;
	private final EmployeePurchaseActivityItemsListQueryMapper activityItemsListQueryMapper;
	private final ItemsMapper itemsMapper;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ObjectMapper objectMapper;
	private final ActivitiesMapper activitiesMapper;

	public EmployeePurchaseActivityItemListService(
			ItemsCategoryRepository itemsCategoryRepository,
			EmployeePurchaseActivityGoodsListQueryMapper activityGoodsListQueryMapper,
			EmployeePurchaseActivityItemsListQueryMapper activityItemsListQueryMapper,
			ItemsMapper itemsMapper,
			DistributorItemsRepository distributorItemsRepository,
			ObjectMapper objectMapper,
			ActivitiesMapper activitiesMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.activityGoodsListQueryMapper = activityGoodsListQueryMapper;
		this.activityItemsListQueryMapper = activityItemsListQueryMapper;
		this.itemsMapper = itemsMapper;
		this.distributorItemsRepository = distributorItemsRepository;
		this.objectMapper = objectMapper;
		this.activitiesMapper = activitiesMapper;
	}

	public Map<String, Object> getActivityItemList(
			long companyId,
			long distributorId,
			long activityId,
			int page,
			int pageSize,
			Long mainCatId,
			Long category,
			String itemName,
			String itemBn,
			Integer shelfStatus) {
		ActivityGoodsListSqlFilter filter = new ActivityGoodsListSqlFilter();
		filter.setCompanyId(companyId);
		filter.setActivityId(activityId);
		if (shelfStatus != null) {
			filter.setShelfStatus(shelfStatus);
		}

		applyMainCategoryFilter(filter, companyId, mainCatId);
		applySalesCategoryFilter(filter, companyId, category);
		if (ValuePresence.hasEffectiveValue(itemName)) {
			filter.setItemName(itemName.trim());
		}
		if (ValuePresence.hasEffectiveValue(itemBn)) {
			filter.setItemBn(itemBn.trim());
		}

		long totalCount = activityGoodsListQueryMapper.countDistinctGoods(filter);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		if (totalCount == 0) {
			result.put("list", List.of());
			return result;
		}

		long offset = (long) (page - 1) * pageSize;
		List<Long> goodsIds = activityGoodsListQueryMapper.selectDistinctGoodsIdsPage(filter, offset, pageSize);
		if (goodsIds.isEmpty()) {
			result.put("list", List.of());
			return result;
		}

		List<Map<String, Object>> rawRows =
				activityItemsListQueryMapper.selectActivityItemRows(
						companyId, activityId, goodsIds, ActivityItemRowsSortKey.ITEM_ID_DESC, shelfStatus);
		Set<Long> itemIdSet = rawRows.stream().map(r -> toLong(r.get("item_id"))).collect(Collectors.toSet());

		Map<Long, List<String>> specPartsByItemId = new LinkedHashMap<>();
		if (!itemIdSet.isEmpty()) {
			List<Map<String, Object>> attrRows =
					activityItemsListQueryMapper.selectItemSpecAttributes(new ArrayList<>(itemIdSet));
			for (Map<String, Object> ar : attrRows) {
				Long iid = toLong(ar.get("item_id"));
				String piece;
				if (ar.get("custom_attribute_value") != null) {
					piece = ar.get("custom_attribute_value").toString();
				} else {
					Object named = ar.get("attribuattribute_valuete_name");
					piece = named == null ? "" : named.toString();
				}
				specPartsByItemId.computeIfAbsent(iid, k -> new ArrayList<>()).add(piece);
			}
		}

		Map<Long, Integer> distributorStoreByItemId = new HashMap<>();
		if (distributorId > 0 && !itemIdSet.isEmpty()) {
			List<DistributorItems> relRows =
					distributorItemsRepository.listByDistributorAndItemIds(companyId, distributorId, itemIdSet);
			for (DistributorItems di : relRows) {
				if (di.getItemId() != null) {
					long st = di.getStore() == null ? 0L : di.getStore();
					distributorStoreByItemId.put(di.getItemId(), (int) Math.min(st, Integer.MAX_VALUE));
				}
			}
		}

		LinkedHashMap<Long, Map<String, Object>> byGoodsId = new LinkedHashMap<>();
		for (Map<String, Object> raw : rawRows) {
			Map<String, Object> row = decorateActivityItemRow(raw, specPartsByItemId, true);
			long gid = toLong(row.get("goods_id"));
			if (byGoodsId.containsKey(gid)) {
				Map<String, Object> head = byGoodsId.get(gid);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> specItems =
						(List<Map<String, Object>>) head.computeIfAbsent("spec_items", k -> new ArrayList<>());
				specItems.add(copyRowForSpecItems(row));
			} else {
				byGoodsId.put(gid, row);
				if (isMultiSpec(row.get("nospec"))) {
					List<Map<String, Object>> specItems = new ArrayList<>();
					specItems.add(copyRowForSpecItems(row));
					row.put("spec_items", specItems);
				}
			}
		}

		List<Map<String, Object>> list = new ArrayList<>(byGoodsId.values());
		for (Map<String, Object> head : list) {
			if (isMultiSpec(head.get("nospec"))) {
				long gid = toLong(head.get("goods_id"));
				LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
				w.eq(Items::getCompanyId, companyId).eq(Items::getGoodsId, gid);
				head.put("total_item_spec", itemsMapper.selectCount(w));
			}
			if (distributorId > 0) {
				long iid = toLong(head.get("item_id"));
				head.put("store", distributorStoreByItemId.getOrDefault(iid, 0));
			}
		}

		result.put("list", list);
		return result;
	}

	/**
	 * H5 活动商品列表：不应用销售分类过滤（与既有 H5 契约一致）；分销商库存覆写关闭（内部聚合 distributorId 恒为 0）。
	 */
	public Map<String, Object> getActivityItemListForFront(
			long companyId,
			long activityId,
			int page,
			int pageSize,
			Long mainCatId,
			String itemName,
			String itemBn,
			String keywords,
			Integer goodsSort,
			boolean distributorIdForVisibilityPositive) {
		ActivityGoodsListSqlFilter filter = new ActivityGoodsListSqlFilter();
		filter.setCompanyId(companyId);
		filter.setActivityId(activityId);
		filter.setShelfStatus(1);

		applyMainCategoryFilter(filter, companyId, mainCatId);
		if (distributorIdForVisibilityPositive) {
			filter.setRequireCanSaleJoin(true);
		} else {
			filter.setApproveStatusIn(List.of("onsale", "only_show"));
		}
		if (ValuePresence.hasEffectiveValue(itemName)) {
			filter.setItemName(itemName.trim());
		}
		if (ValuePresence.hasEffectiveValue(itemBn)) {
			filter.setItemBn(itemBn.trim());
		}
		if (ValuePresence.hasEffectiveValue(keywords)) {
			filter.setKeywords(keywords.trim());
		}

		long totalCount = activityGoodsListQueryMapper.countDistinctGoods(filter);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		putPurchaseModeRoot(result, companyId, activityId);
		if (totalCount == 0) {
			result.put("list", List.of());
			return result;
		}

		long offset = (long) (page - 1) * pageSize;
		List<Long> goodsIds = activityGoodsListQueryMapper.selectDistinctGoodsIdsPage(filter, offset, pageSize);
		if (goodsIds.isEmpty()) {
			result.put("list", List.of());
			return result;
		}

		ActivityItemRowsSortKey sortKey = resolveFrontGoodsSort(goodsSort);
		List<Map<String, Object>> rawRows =
				activityItemsListQueryMapper.selectActivityItemRows(companyId, activityId, goodsIds, sortKey, 1);
		Set<Long> itemIdSet = rawRows.stream().map(r -> toLong(r.get("item_id"))).collect(Collectors.toSet());

		LinkedHashMap<Long, Map<String, Object>> byGoodsId = new LinkedHashMap<>();
		for (Map<String, Object> raw : rawRows) {
			Map<String, Object> row = decorateActivityItemRow(raw, Map.of(), false);
			long gid = toLong(row.get("goods_id"));
			if (byGoodsId.containsKey(gid)) {
				Map<String, Object> head = byGoodsId.get(gid);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> specItems =
						(List<Map<String, Object>>) head.computeIfAbsent("spec_items", k -> new ArrayList<>());
				specItems.add(copyRowForSpecItems(row));
			} else {
				byGoodsId.put(gid, row);
				if (isMultiSpec(row.get("nospec"))) {
					List<Map<String, Object>> specItems = new ArrayList<>();
					specItems.add(copyRowForSpecItems(row));
					row.put("spec_items", specItems);
				}
			}
		}

		List<Map<String, Object>> list = new ArrayList<>(byGoodsId.values());
		for (Map<String, Object> head : list) {
			if (isMultiSpec(head.get("nospec"))) {
				long gid = toLong(head.get("goods_id"));
				LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
				w.eq(Items::getCompanyId, companyId).eq(Items::getGoodsId, gid);
				head.put("total_item_spec", itemsMapper.selectCount(w));
			}
		}

		result.put("list", list);
		return result;
	}

	private static ActivityItemRowsSortKey resolveFrontGoodsSort(Integer goodsSort) {
		if (goodsSort == null) {
			return ActivityItemRowsSortKey.SORT_DESC;
		}
		return switch (goodsSort) {
			case 1 -> ActivityItemRowsSortKey.SALES;
			case 2 -> ActivityItemRowsSortKey.ACTIVITY_PRICE_DESC;
			case 3 -> ActivityItemRowsSortKey.ACTIVITY_PRICE_ASC;
			default -> ActivityItemRowsSortKey.SORT_DESC;
		};
	}

	private void applyMainCategoryFilter(ActivityGoodsListSqlFilter filter, long companyId, Long mainCatId) {
		if (!ValuePresence.hasEffectiveValue(mainCatId)) {
			return;
		}
		Optional<ItemsCategory> info =
				itemsCategoryRepository.findOneByCompanyIdAndCategoryIdAndIsMainCategory(companyId, mainCatId, true);
		if (info.isEmpty()) {
			return;
		}
		filter.setMainCatIds(resolveMainCategoryIdStrings(companyId, mainCatId, info.get()));
	}

	private List<String> resolveMainCategoryIdStrings(long companyId, long selectedCategoryId, ItemsCategory info) {
		String path = info.getPath();
		int depth = (path == null || path.isEmpty()) ? 0 : path.split(",", -1).length;
		if (depth == 3) {
			return List.of(String.valueOf(selectedCategoryId));
		}
		if (depth == 2) {
			List<Long> ids = itemsCategoryRepository.listCategoryIdsByCompanyIdParentIdAndIsMainCategory(
					companyId, selectedCategoryId, true);
			if (ids.isEmpty()) {
				return List.of("-1");
			}
			return ids.stream().map(String::valueOf).collect(Collectors.toList());
		}
		List<Long> ids = itemsCategoryRepository.listCategoryIdsByPathLikeChildOf(companyId, selectedCategoryId);
		if (ids.isEmpty()) {
			return List.of("-1");
		}
		return ids.stream().map(String::valueOf).collect(Collectors.toList());
	}

	private void applySalesCategoryFilter(ActivityGoodsListSqlFilter filter, long companyId, Long category) {
		if (!ValuePresence.hasEffectiveValue(category)) {
			return;
		}
		long categoryId = category;
		List<Long> ids = new ArrayList<>();
		ids.add(categoryId);
		List<Long> firstLevel = itemsCategoryRepository.listCategoryIdsByParentId(companyId, categoryId);
		if (!firstLevel.isEmpty()) {
			ids.addAll(firstLevel);
			List<Long> secondLevel = itemsCategoryRepository.listCategoryIdsByParentIds(companyId, firstLevel);
			ids.addAll(secondLevel);
		}
		filter.setCategoryIds(ids);
	}

	private Map<String, Object> decorateActivityItemRow(
			Map<String, Object> raw,
			Map<Long, List<String>> specPartsByItemId,
			boolean includeItemSpecDesc) {
		Map<String, Object> row = new LinkedHashMap<>(raw);
		row.put("pics", parsePicsJson(row.get("pics")));
		if (includeItemSpecDesc) {
			Long itemId = toLong(row.get("item_id"));
			if (isMultiSpec(row.get("nospec"))) {
				List<String> parts = specPartsByItemId.get(itemId);
				if (parts != null) {
					row.put("item_spec_desc", String.join(",", parts));
				}
			}
		}
		row.put("updated", raw.get("updated"));
		return row;
	}

	private Object parsePicsJson(Object picsRaw) {
		if (picsRaw == null) {
			return null;
		}
		String s = picsRaw.toString();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (Exception e) {
			return null;
		}
	}

	private static Map<String, Object> copyRowForSpecItems(Map<String, Object> row) {
		Map<String, Object> copy = new LinkedHashMap<>(row);
		copy.remove("spec_items");
		return copy;
	}

	private static boolean isMultiSpec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		if (nospec instanceof Number n) {
			return n.intValue() == 0;
		}
		String s = nospec.toString().trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private void putPurchaseModeRoot(Map<String, Object> result, long companyId, long activityId) {
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			result.put("purchase_mode", null);
			return;
		}
		result.put("purchase_mode", activity.getPurchaseMode());
	}

	private static long toLong(Object v) {
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
