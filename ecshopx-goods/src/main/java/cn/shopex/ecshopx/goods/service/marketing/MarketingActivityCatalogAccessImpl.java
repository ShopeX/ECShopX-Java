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

package cn.shopex.ecshopx.goods.service.marketing;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsRelTagsMapper;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsListFilter;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.goods.service.items.EmployeePurchaseItemsSkuListService;
import cn.shopex.ecshopx.goods.service.promotion.MultiSpecItemsTreeFormatter;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityCatalogAccessImpl implements MarketingActivityCatalogAccess {

	private final EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService;
	private final MarketingActivityItemListCatalogSupport marketingActivityItemListCatalogSupport;
	private final ItemsMapper itemsMapper;
	private final ItemsRelTagsMapper itemsRelTagsMapper;
	private final ItemsRepository itemsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;

	public MarketingActivityCatalogAccessImpl(
			EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService,
			MarketingActivityItemListCatalogSupport marketingActivityItemListCatalogSupport,
			ItemsMapper itemsMapper,
			ItemsRelTagsMapper itemsRelTagsMapper,
			ItemsRepository itemsRepository,
			ItemsTagsRepository itemsTagsRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			DistributorListQueryService distributorListQueryService,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator) {
		this.employeePurchaseItemsSkuListService = employeePurchaseItemsSkuListService;
		this.marketingActivityItemListCatalogSupport = marketingActivityItemListCatalogSupport;
		this.itemsMapper = itemsMapper;
		this.itemsRelTagsMapper = itemsRelTagsMapper;
		this.itemsRepository = itemsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
	}

	@Override
	public Map<String, Object> loadSkuItemsList(long companyId, List<Long> itemIds) {
		return employeePurchaseItemsSkuListService.loadSkuItemsList(companyId, itemIds);
	}

	@Override
	public Map<String, Object> loadSkuItemsListForMarketingGift(long companyId, List<Long> itemIds) {
		return employeePurchaseItemsSkuListService.loadSkuItemsListForMarketingGift(companyId, itemIds);
	}

	@Override
	public List<Map<String, Object>> loadItemIdAndTypeRows(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		List<Long> distinctIds = itemIds.stream().filter(Objects::nonNull).distinct().toList();
		if (distinctIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, distinctIds);
		List<Items> rows = itemsMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		Map<Long, Items> byId = rows.stream().collect(Collectors.toMap(Items::getItemId, Function.identity(), (a, b) -> a));
		List<Map<String, Object>> ordered = new ArrayList<>();
		for (Long id : distinctIds) {
			Items it = byId.get(id);
			if (it == null) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("item_id", id);
			m.put("type", it.getType());
			ordered.add(m);
		}
		return ordered;
	}

	@Override
	public boolean anyGiftItem(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return false;
		}
		List<Long> ids = itemIds.stream().filter(Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			return false;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, ids).eq(Items::getIsGift, true);
		Long c = itemsMapper.selectCount(w);
		return c != null && c > 0;
	}

	@Override
	public List<Long> listDistinctTagIdsByItemIds(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		List<Long> ids = itemIds.stream().filter(Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).in(ItemsRelTags::getItemId, ids);
		List<ItemsRelTags> rows = itemsRelTagsMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		return rows.stream()
				.map(ItemsRelTags::getTagId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	@Override
	public List<Long> listItemIdsByGoodsId(long companyId, long goodsId) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getGoodsId, goodsId);
		List<Items> rows = itemsMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		return rows.stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
	}

	@Override
	public Map<Long, Long> loadItemPriceByItemId(long companyId, List<Long> itemIds) {
		Map<Long, Long> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		List<Long> ids = itemIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
		if (ids.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, ids);
		List<Items> rows = itemsMapper.selectList(w);
		if (rows == null) {
			return out;
		}
		for (Items it : rows) {
			if (it.getItemId() != null && it.getPrice() != null) {
				out.put(it.getItemId(), it.getPrice().longValue());
			}
		}
		return out;
	}

	@Override
	public Map<String, Long> mapItemBnToItemIdForCompany(long companyId, Collection<String> itemBns) {
		return itemsRepository.mapItemBnToItemIdForCompany(companyId, itemBns);
	}

	@Override
	public Map<String, Object> loadFullStoreMarketingActivityItemListData(
			long companyId, List<Long> distributorScope, int page, int pageSize, boolean orderByItemIdDesc) {
		return marketingActivityItemListCatalogSupport.loadFullStoreMarketingActivityItemListData(
				companyId, distributorScope, page, pageSize, orderByItemIdDesc);
	}

	@Override
	public Map<String, Object> loadSkuItemsPageForMarketingActivityItemList(
			long companyId, List<Long> itemIds, int page, int pageSize) {
		return marketingActivityItemListCatalogSupport.loadSkuItemsPageForMarketingActivityItemList(
				companyId, itemIds, page, pageSize);
	}

	@Override
	public List<Long> listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(long companyId, List<Long> tagIds) {
		return marketingActivityItemListCatalogSupport.listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(companyId, tagIds);
	}

	@Override
	public long countByCompanyIdAndTagIdsIn(long companyId, List<Long> tagIds) {
		return marketingActivityItemListCatalogSupport.countByCompanyIdAndTagIdsIn(companyId, tagIds);
	}

	@Override
	public List<Long> listItemIdsForCategoryFilter(
			long companyId, List<Long> activityMainCategoryIds, int page, int pageSize) {
		return marketingActivityItemListCatalogSupport.listItemIdsForCategoryFilter(
				companyId, activityMainCategoryIds, page, pageSize);
	}

	@Override
	public long countItemsForCategoryFilter(long companyId, List<Long> activityMainCategoryIds) {
		return marketingActivityItemListCatalogSupport.countItemsForCategoryFilter(companyId, activityMainCategoryIds);
	}

	@Override
	public List<Long> listItemIdsForBrandFilter(long companyId, List<Long> brandIds, int page, int pageSize) {
		return marketingActivityItemListCatalogSupport.listItemIdsForBrandFilter(companyId, brandIds, page, pageSize);
	}

	@Override
	public long countItemsForBrandFilter(long companyId, List<Long> brandIds) {
		return marketingActivityItemListCatalogSupport.countItemsForBrandFilter(companyId, brandIds);
	}

	@Override
	public List<Map<String, Object>> listTagsForMarketingActivityInfo(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		ItemsTagsListFilter filter = new ItemsTagsListFilter();
		filter.setCompanyId(companyId);
		filter.setTagIdsIn(tagIds);
		Page<ItemsTags> page = new Page<>(1, 500, false);
		IPage<ItemsTags> p = itemsTagsRepository.selectPageByFilter(page, filter);
		List<ItemsTags> rows = p.getRecords();
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> distIds =
				rows.stream().map(ItemsTags::getDistributorId).filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
		Map<Long, Distributor> distById = Map.of();
		if (!distIds.isEmpty()) {
			distById = distributorListQueryService.listByIdsAndCompany(companyId, distIds).stream()
					.collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsTags t : rows) {
			out.add(itemsTagToRow(t, distById));
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> listBrandAttributesForMarketingActivityInfo(long companyId, List<Long> brandAttributeIds) {
		if (brandAttributeIds == null || brandAttributeIds.isEmpty()) {
			return List.of();
		}
		List<ItemsAttributes> attrs =
				itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, brandAttributeIds);
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsAttributes a : attrs) {
			if (a.getAttributeType() != null && "brand".equalsIgnoreCase(a.getAttributeType().trim())) {
				out.add(itemsAttributeToRow(a));
			}
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> buildMarketingActivityItemTreeLists(long companyId, List<Long> itemIdsOrdered) {
		if (itemIdsOrdered == null || itemIdsOrdered.isEmpty()) {
			return List.of();
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list =
				(List<Map<String, Object>>) loadSkuItemsListForMarketingGift(companyId, itemIdsOrdered).get("list");
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		return MultiSpecItemsTreeFormatter.formatItemsList(list);
	}

	@Override
	public List<Long> listMarketingIdsHitBySkuItem(
			long companyId, long itemId, List<Long> candidateMarketingIds, int nowEpochSeconds) {
		if (candidateMarketingIds == null || candidateMarketingIds.isEmpty()) {
			return List.of();
		}
		Items it = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (it == null) {
			return List.of();
		}
		long goodsId = it.getGoodsId() != null ? it.getGoodsId() : 0L;
		long brandId = it.getBrandId() != null ? it.getBrandId().longValue() : 0L;
		Long categoryId = parsePositiveLongOrNull(it.getItemCategory());
		List<Long> tagProbeItemIds = new ArrayList<>();
		Long def = it.getDefaultItemId();
		if (def != null && def > 0L) {
			tagProbeItemIds.add(def);
		}
		tagProbeItemIds.add(itemId);
		List<Long> tagIds = listDistinctTagIdsForItemIds(companyId, tagProbeItemIds);
		List<Long> distinctCandidates = candidateMarketingIds.stream().filter(Objects::nonNull).distinct().toList();
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId)
				.in(MarketingActivityItems::getMarketingId, distinctCandidates)
				.le(MarketingActivityItems::getStartTime, nowEpochSeconds)
				.ge(MarketingActivityItems::getEndTime, nowEpochSeconds);
		w.and(or -> {
			or.nested(n -> n.eq(MarketingActivityItems::getItemType, "normal")
					.and(nn -> {
						nn.eq(MarketingActivityItems::getItemId, itemId);
						if (goodsId > 0L) {
							nn.or().eq(MarketingActivityItems::getGoodsId, goodsId);
						}
					}));
			if (!tagIds.isEmpty()) {
				or.or(n -> n.eq(MarketingActivityItems::getItemType, "tag").in(MarketingActivityItems::getItemId, tagIds));
			}
			if (brandId > 0L) {
				or.or(n -> n.eq(MarketingActivityItems::getItemType, "brand").eq(MarketingActivityItems::getItemId, brandId));
			}
			if (categoryId != null) {
				or.or(n -> n.eq(MarketingActivityItems::getItemType, "category").eq(MarketingActivityItems::getItemId, categoryId));
			}
		});
		List<MarketingActivityItems> hits = marketingActivityItemsMapper.selectList(w);
		if (hits == null || hits.isEmpty()) {
			return List.of();
		}
		HashSet<Long> hitSet = new HashSet<>();
		for (MarketingActivityItems h : hits) {
			if (h.getMarketingId() != null) {
				hitSet.add(h.getMarketingId());
			}
		}
		List<Long> ordered = new ArrayList<>();
		for (Long mid : candidateMarketingIds) {
			if (mid != null && hitSet.contains(mid)) {
				ordered.add(mid);
			}
		}
		return ordered;
	}

	@Override
	public Map<String, Object> loadWxappItemListDataForSeckillGetInfo(
			long companyId, long userId, List<Long> itemIds, String acceptLanguage) {
		if (itemIds == null || itemIds.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}
		LinkedHashMap<String, Object> work = new LinkedHashMap<>();
		work.put("company_id", Long.valueOf(companyId));
		List<Long> orderedDistinct = new ArrayList<>();
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		for (Long id : itemIds) {
			if (id == null || id <= 0L) {
				continue;
			}
			if (seen.add(id)) {
				orderedDistinct.add(id);
			}
		}
		if (orderedDistinct.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}
		work.put("item_id", orderedDistinct);
		work.put("user_id", userId);
		work.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, Integer.valueOf(1));
		work.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, Integer.valueOf(100));
		work.put(
				WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE,
				StringUtils.hasText(acceptLanguage) ? acceptLanguage.trim() : "zh-CN");
		return wxappGoodsItemsListQueryOrchestrator.queryItemListData(companyId, work, List.of());
	}

	@Override
	public Map<String, Object> loadItemNarrowMapForSkuMarketing(long companyId, long itemId) {
		Items it = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (it == null) {
			return null;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		long dist = it.getDistributorId() != null ? it.getDistributorId().longValue() : 0L;
		m.put("distributor_id", dist);
		m.put("goods_id", it.getGoodsId() != null ? it.getGoodsId() : 0L);
		m.put("item_id", it.getItemId());
		m.put("default_item_id", it.getDefaultItemId() != null ? it.getDefaultItemId() : 0L);
		m.put("brand_id", it.getBrandId() != null ? it.getBrandId().longValue() : 0L);
		m.put("item_category", it.getItemCategory());
		return m;
	}

	private List<Long> listDistinctTagIdsForItemIds(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		List<Long> ids = itemIds.stream().filter(Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsRelTags> w = new LambdaQueryWrapper<>();
		w.eq(ItemsRelTags::getCompanyId, companyId).in(ItemsRelTags::getItemId, ids);
		List<ItemsRelTags> rows = itemsRelTagsMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		return rows.stream()
				.map(ItemsRelTags::getTagId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	private static Long parsePositiveLongOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> itemsTagToRow(ItemsTags t, Map<Long, Distributor> distById) {
		Map<String, Object> m = new LinkedHashMap<>();
		putLong(m, "tag_id", t.getTagId());
		putLong(m, "company_id", t.getCompanyId());
		m.put("tag_name", t.getTagName());
		m.put("tag_color", t.getTagColor());
		putLong(m, "distributor_id", t.getDistributorId());
		m.put("font_color", t.getFontColor());
		m.put("description", t.getDescription());
		m.put("tag_icon", t.getTagIcon());
		m.put("front_show", t.getFrontShow());
		m.put("created", t.getCreated());
		m.put("updated", t.getUpdated());
		long did = t.getDistributorId() != null ? t.getDistributorId() : 0L;
		if (did > 0) {
			Distributor d = distById.get(did);
			m.put("distributor_name", d != null && StringUtils.hasText(d.getName()) ? d.getName() : "");
			m.put("is_platform", false);
		} else {
			m.put("distributor_name", "平台");
			m.put("is_platform", true);
		}
		return m;
	}

	private static Map<String, Object> itemsAttributeToRow(ItemsAttributes a) {
		Map<String, Object> m = new LinkedHashMap<>();
		putLong(m, "attribute_id", a.getAttributeId());
		putLong(m, "company_id", a.getCompanyId());
		m.put("attribute_type", a.getAttributeType());
		m.put("attribute_name", a.getAttributeName());
		m.put("attribute_memo", a.getAttributeMemo());
		m.put("attribute_sort", a.getAttributeSort());
		putLong(m, "shop_id", a.getShopId());
		return m;
	}

	private static void putLong(Map<String, Object> m, String key, Long v) {
		if (v != null) {
			m.put(key, v);
		}
	}
}
