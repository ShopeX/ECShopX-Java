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

package cn.shopex.ecshopx.goods.search.index;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.SearchIndexSourceMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** JDBC/MyBatis 物化 {@link SearchIndexSource}（含定制会员价与改价活动行）。 */
@Component
public class MybatisSearchIndexSourceLoader implements SearchIndexSourceLoader {

	private final SearchIndexSourceMapper searchIndexSourceMapper;
	private final ItemsMapper itemsMapper;
	private final DistributorItemsMapper distributorItemsMapper;
	private final SearchIndexMemberActivityRepository memberActivityRepository;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsRepository itemsTagsRepository;

	public MybatisSearchIndexSourceLoader(
			SearchIndexSourceMapper searchIndexSourceMapper,
			ItemsMapper itemsMapper,
			DistributorItemsMapper distributorItemsMapper,
			SearchIndexMemberActivityRepository memberActivityRepository,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagsRepository itemsTagsRepository) {
		this.searchIndexSourceMapper = searchIndexSourceMapper;
		this.itemsMapper = itemsMapper;
		this.distributorItemsMapper = distributorItemsMapper;
		this.memberActivityRepository = memberActivityRepository;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
	}

	@Override
	public List<SearchIndexSource> fetchPage(
			long companyId,
			Long distributorIdOrNull,
			long afterDefaultItemId,
			long afterDistributorId,
			int limit) {
		if (limit <= 0) {
			return List.of();
		}
		Long distributorFilter = distributorIdOrNull != null && distributorIdOrNull > 0 ? distributorIdOrNull : null;
		List<SearchIndexSource> out = new ArrayList<>(limit);
		long cursorDefaultItemId = afterDefaultItemId;
		long cursorDistributorId = afterDistributorId;
		// 店品键可能因 items 已删等无法物化；空物化页不能当作扫描结束，否则会在孤儿键前缀处提前退出（如 company 38）。
		while (out.size() < limit) {
			List<SearchIndexStoreItemKey> keys =
					searchIndexSourceMapper.selectStoreItemKeys(
							companyId, distributorFilter, cursorDefaultItemId, cursorDistributorId, limit);
			if (keys.isEmpty()) {
				break;
			}
			for (SearchIndexStoreItemKey key : keys) {
				loadStoreItem(companyId, key.distributorId(), key.defaultItemId()).ifPresent(out::add);
				if (out.size() >= limit) {
					break;
				}
			}
			SearchIndexStoreItemKey lastKey = keys.get(keys.size() - 1);
			cursorDefaultItemId = lastKey.defaultItemId();
			cursorDistributorId = lastKey.distributorId();
			if (out.size() >= limit || keys.size() < limit) {
				break;
			}
		}
		return out;
	}

	@Override
	public Optional<SearchIndexSource> fetchOne(long companyId, long distributorId, long defaultItemId) {
		return loadStoreItem(companyId, distributorId, defaultItemId);
	}

	private Optional<SearchIndexSource> loadStoreItem(long companyId, long distributorId, long defaultItemId) {
		Items anchor = loadAnchorItem(companyId, defaultItemId);
		if (anchor == null) {
			return Optional.empty();
		}
		List<Items> skuRows =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.and(
										w ->
												w.eq(Items::getItemId, defaultItemId)
														.or()
														.eq(Items::getDefaultItemId, defaultItemId)));
		if (skuRows.isEmpty()) {
			return Optional.empty();
		}
		Map<Long, DistributorItems> distByItemId = loadDistributorRows(companyId, distributorId, skuRows);
		long hqPriceFen = priceFen(anchor.getPrice());
		long marketPriceFen = anchor.getMarketPrice() != null ? anchor.getMarketPrice().longValue() : 0L;
		Long distributorPriceFen = resolveDistributorPriceFen(distributorId, defaultItemId, anchor, distByItemId);
		long sales = resolveSales(distributorId, defaultItemId, anchor, distByItemId);
		long listedAt = resolveListedAt(distributorId, defaultItemId, anchor, distByItemId);
		List<SearchIndexSource.Sku> skus = buildSkus(skuRows, distByItemId, distributorId);
		if (skus.isEmpty()) {
			return Optional.empty();
		}
		List<Long> skuItemIds = skuRows.stream().map(Items::getItemId).filter(id -> id != null && id > 0).toList();
		SearchIndexMemberPriceMaterializer.Buckets memberPrices =
				memberActivityRepository.loadMemberPrices(companyId, defaultItemId, skuItemIds);
		List<SearchIndexSource.Activity> activities =
				memberActivityRepository.loadActivities(companyId, skuItemIds);
		List<String> tagNames = resolveTagNames(companyId, defaultItemId);
		return Optional.of(
				SearchIndexSource.builder()
						.companyId(companyId)
						.distributorId(distributorId)
						.defaultItemId(defaultItemId)
						.itemName(anchor.getItemName())
						.brief(anchor.getBrief())
						.itemBn(anchor.getItemBn())
						.barcode(anchor.getBarcode())
						.tags(tagNames)
						.approveStatus(anchor.getApproveStatus())
						.auditStatus(anchor.getAuditStatus())
						.categoryIds(parseCategoryIds(anchor.getItemCategory()))
						.hqPriceFen(hqPriceFen)
						.distributorPriceFen(distributorPriceFen)
						.marketPriceFen(marketPriceFen)
						.sales(sales)
						.listedAt(listedAt)
						.memberPriceGrade(memberPrices.grade())
						.memberPriceVipGrade(memberPrices.vipGrade())
						.skus(skus)
						.activities(activities)
						.build());
	}

	private Items loadAnchorItem(long companyId, long defaultItemId) {
		Items direct = itemsMapper.selectOne(
				new LambdaQueryWrapper<Items>()
						.eq(Items::getCompanyId, companyId)
						.eq(Items::getItemId, defaultItemId)
						.last("LIMIT 1"));
		if (direct != null) {
			return direct;
		}
		return itemsMapper.selectOne(
				new LambdaQueryWrapper<Items>()
						.eq(Items::getCompanyId, companyId)
						.eq(Items::getDefaultItemId, defaultItemId)
						.last("LIMIT 1"));
	}

	private Map<Long, DistributorItems> loadDistributorRows(
			long companyId, long distributorId, List<Items> skuRows) {
		if (distributorId <= 0) {
			return Map.of();
		}
		List<Long> itemIds = skuRows.stream().map(Items::getItemId).toList();
		List<DistributorItems> distRows =
				distributorItemsMapper.selectList(
						new LambdaQueryWrapper<DistributorItems>()
								.eq(DistributorItems::getCompanyId, companyId)
								.eq(DistributorItems::getDistributorId, distributorId)
								.in(DistributorItems::getItemId, itemIds));
		Map<Long, DistributorItems> map = new HashMap<>();
		for (DistributorItems row : distRows) {
			map.put(row.getItemId(), row);
		}
		return map;
	}

	private static List<SearchIndexSource.Sku> buildSkus(
			List<Items> skuRows, Map<Long, DistributorItems> distByItemId, long distributorId) {
		List<SearchIndexSource.Sku> skus = new ArrayList<>(skuRows.size());
		for (Items item : skuRows) {
			if (item.getItemId() == null) {
				continue;
			}
			long store;
			String approveStatus = item.getApproveStatus() != null ? item.getApproveStatus() : "instock";
			if (distributorId > 0) {
				DistributorItems dist = distByItemId.get(item.getItemId());
				if (dist == null) {
					continue;
				}
				store = dist.getStore() != null ? dist.getStore() : 0L;
			} else {
				store = item.getStore() != null ? item.getStore().longValue() : 0L;
			}
			skus.add(new SearchIndexSource.Sku(item.getItemId(), store, approveStatus));
		}
		return List.copyOf(skus);
	}

	private static Long resolveDistributorPriceFen(
			long distributorId,
			long defaultItemId,
			Items anchor,
			Map<Long, DistributorItems> distByItemId) {
		if (distributorId <= 0) {
			return null;
		}
		DistributorItems spuDist = distByItemId.get(defaultItemId);
		if (spuDist != null && spuDist.getPrice() != null && spuDist.getPrice() > 0) {
			return spuDist.getPrice();
		}
		for (Map.Entry<Long, DistributorItems> e : distByItemId.entrySet()) {
			if (e.getValue().getPrice() != null && e.getValue().getPrice() > 0) {
				return e.getValue().getPrice();
			}
		}
		if (anchor.getItemId() != null) {
			DistributorItems anchorDist = distByItemId.get(anchor.getItemId());
			if (anchorDist != null && anchorDist.getPrice() != null && anchorDist.getPrice() > 0) {
				return anchorDist.getPrice();
			}
		}
		return null;
	}

	private static long priceFen(Integer price) {
		return price != null ? price.longValue() : 0L;
	}

	/**
	 * 累计销量：映射 {@code items.sales} / {@code distribution_distributor_items.sales}，写入 ES {@code sales}。
	 */
	private static long resolveSales(
			long distributorId, long defaultItemId, Items anchor, Map<Long, DistributorItems> distByItemId) {
		if (distributorId > 0) {
			DistributorItems spuDist = distByItemId.get(defaultItemId);
			if (spuDist != null && spuDist.getSales() != null && spuDist.getSales() > 0) {
				return spuDist.getSales();
			}
			long max = 0L;
			for (DistributorItems row : distByItemId.values()) {
				if (row.getSales() != null) {
					max = Math.max(max, row.getSales());
				}
			}
			return max;
		}
		return anchor.getSales() != null ? Math.max(0L, anchor.getSales().longValue()) : 0L;
	}

	/** 上架/进店时间：店品 {@code distribution_distributor_items.created}，否则 {@code items.created}（epoch 秒）。 */
	private static long resolveListedAt(
			long distributorId, long defaultItemId, Items anchor, Map<Long, DistributorItems> distByItemId) {
		if (distributorId > 0) {
			DistributorItems spuDist = distByItemId.get(defaultItemId);
			if (spuDist != null && spuDist.getCreated() != null && spuDist.getCreated() > 0) {
				return spuDist.getCreated();
			}
			for (DistributorItems row : distByItemId.values()) {
				if (row.getCreated() != null && row.getCreated() > 0) {
					return row.getCreated();
				}
			}
		}
		return anchor.getCreated() != null ? anchor.getCreated().longValue() : 0L;
	}

	private List<String> resolveTagNames(long companyId, long defaultItemId) {
		List<ItemsRelTags> rels =
				itemsRelTagsRepository.listRowsByCompanyIdAndItemIdIn(companyId, List.of(defaultItemId));
		List<String> names = new ArrayList<>(rels.size());
		for (ItemsRelTags rel : rels) {
			if (rel.getTagId() == null) {
				continue;
			}
			ItemsTags tag = itemsTagsRepository.selectById(rel.getTagId());
			if (tag != null && tag.getTagName() != null && !tag.getTagName().isBlank()) {
				names.add(tag.getTagName().trim());
			}
		}
		return List.copyOf(names);
	}

	private static List<Long> parseCategoryIds(String itemCategory) {
		if (itemCategory == null || itemCategory.isBlank()) {
			return List.of();
		}
		try {
			return List.of(Long.parseLong(itemCategory.trim()));
		} catch (NumberFormatException ignored) {
			return List.of();
		}
	}
}
