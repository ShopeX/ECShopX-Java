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
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** JDBC/MyBatis 物化 {@link SearchIndexSource}（会员价/活动字段留空，Task 5 补齐）。 */
@Component
public class MybatisSearchIndexSourceLoader implements SearchIndexSourceLoader {

	private final SearchIndexSourceMapper searchIndexSourceMapper;
	private final ItemsMapper itemsMapper;
	private final DistributorItemsMapper distributorItemsMapper;

	public MybatisSearchIndexSourceLoader(
			SearchIndexSourceMapper searchIndexSourceMapper,
			ItemsMapper itemsMapper,
			DistributorItemsMapper distributorItemsMapper) {
		this.searchIndexSourceMapper = searchIndexSourceMapper;
		this.itemsMapper = itemsMapper;
		this.distributorItemsMapper = distributorItemsMapper;
	}

	@Override
	public List<SearchIndexSource> fetchPage(
			long companyId,
			Long distributorIdOrNull,
			long afterDefaultItemId,
			long afterDistributorId,
			int limit) {
		Long distributorFilter = distributorIdOrNull != null && distributorIdOrNull > 0 ? distributorIdOrNull : null;
		List<SearchIndexStoreItemKey> keys =
				searchIndexSourceMapper.selectStoreItemKeys(
						companyId, distributorFilter, afterDefaultItemId, afterDistributorId, limit);
		List<SearchIndexSource> out = new ArrayList<>(keys.size());
		for (SearchIndexStoreItemKey key : keys) {
			loadStoreItem(companyId, key.distributorId(), key.defaultItemId()).ifPresent(out::add);
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
		List<SearchIndexSource.Sku> skus = buildSkus(skuRows, distByItemId, distributorId);
		if (skus.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(
				SearchIndexSource.builder()
						.companyId(companyId)
						.distributorId(distributorId)
						.defaultItemId(defaultItemId)
						.hqPriceFen(hqPriceFen)
						.distributorPriceFen(distributorPriceFen)
						.marketPriceFen(marketPriceFen)
						.skus(skus)
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
}
