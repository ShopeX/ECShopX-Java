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
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** C 端可售/库存/主图判断（SSOT §5.3，对齐 RecommendLikeMapper） */
@Component
public class GoodsRecommendSalabilityResolver {

	private final DistributorItemsMapper distributorItemsMapper;

	private final ItemsMapper itemsMapper;

	private final ItemsCategoryDistributorIdResolver productModelResolver;

	public GoodsRecommendSalabilityResolver(
			DistributorItemsMapper distributorItemsMapper,
			ItemsMapper itemsMapper,
			ItemsCategoryDistributorIdResolver productModelResolver) {
		this.distributorItemsMapper = distributorItemsMapper;
		this.itemsMapper = itemsMapper;
		this.productModelResolver = productModelResolver;
	}

	public String resolveProductModel(long companyId) {
		return productModelResolver.resolveProductModel(companyId);
	}

	public boolean isValidDistributorContext(long companyId, long distributorId) {
		String productModel = resolveProductModel(companyId);
		if ("standard".equals(productModel)) {
			return distributorId > 0;
		}
		return distributorId == 0;
	}

	public Map<Long, DistributorItems> loadDistributorItems(long distributorId, List<Long> itemIds) {
		return loadDistributorContext(0L, distributorId, itemIds).byItemId();
	}

	/**
	 * 加载门店商品行：含 SPU item_id 及同 SPU 下 SKU 行（default_item_id），与详情页 merge 范围一致。
	 */
	public DistributorContext loadDistributorContext(
			long companyId, long distributorId, List<Long> spuItemIds) {
		if (distributorId <= 0 || spuItemIds.isEmpty()) {
			return DistributorContext.empty();
		}
		LambdaQueryWrapper<DistributorItems> wrapper =
				new LambdaQueryWrapper<DistributorItems>()
						.eq(DistributorItems::getDistributorId, distributorId)
						.and(
								w ->
										w.in(DistributorItems::getItemId, spuItemIds)
												.or()
												.in(DistributorItems::getDefaultItemId, spuItemIds));
		if (companyId > 0) {
			wrapper.eq(DistributorItems::getCompanyId, companyId);
		}
		List<DistributorItems> rows = distributorItemsMapper.selectList(wrapper);
		Map<Long, DistributorItems> byItemId = new HashMap<>();
		Map<Long, Integer> storeBySpuId = new HashMap<>();
		for (DistributorItems row : rows) {
			byItemId.put(row.getItemId(), row);
			if (Boolean.TRUE.equals(row.getIsTotalStore()) || row.getStore() == null) {
				continue;
			}
			storeBySpuId.merge(distributorSpuIdKey(row), row.getStore().intValue(), Integer::sum);
		}
		return new DistributorContext(byItemId, storeBySpuId);
	}

	public record DistributorContext(
			Map<Long, DistributorItems> byItemId, Map<Long, Integer> storeBySpuId) {

		static DistributorContext empty() {
			return new DistributorContext(Map.of(), Map.of());
		}
	}

	/** 按 SPU 批量加载 default_item_id 对应 SKU（SSOT G4） */
	public Map<Long, Items> loadDefaultSkus(long companyId, Collection<Items> spuItems) {
		if (spuItems == null || spuItems.isEmpty()) {
			return Map.of();
		}
		Set<Long> skuIds = new HashSet<>();
		Map<Long, Long> spuToSkuId = new HashMap<>();
		for (Items spu : spuItems) {
			if (spu == null || spu.getItemId() == null) {
				continue;
			}
			Long defId = spu.getDefaultItemId();
			if (defId != null && defId > 0 && !defId.equals(spu.getItemId())) {
				skuIds.add(defId);
				spuToSkuId.put(spu.getItemId(), defId);
			}
		}
		if (skuIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Items> skuById =
				itemsMapper
						.selectList(
								new LambdaQueryWrapper<Items>()
										.eq(Items::getCompanyId, companyId)
										.in(Items::getItemId, skuIds))
						.stream()
						.collect(
								java.util.stream.Collectors.toMap(
										Items::getItemId, i -> i, (a, b) -> a));
		Map<Long, Items> defaultSkuBySpuId = new HashMap<>();
		for (Map.Entry<Long, Long> entry : spuToSkuId.entrySet()) {
			Items sku = skuById.get(entry.getValue());
			if (sku != null) {
				defaultSkuBySpuId.put(entry.getKey(), sku);
			}
		}
		return defaultSkuBySpuId;
	}

	/** 按 SPU 聚合同 goods 下各 SKU 的最大库存（多规格任一有库存即视为 SPU 有库存） */
	public Map<Long, Integer> loadMaxSkuStoreBySpuId(long companyId, Collection<Items> items) {
		if (items == null || items.isEmpty()) {
			return Map.of();
		}
		Set<Long> spuIds = new HashSet<>();
		for (Items item : items) {
			if (item == null || item.getItemId() == null) {
				continue;
			}
			spuIds.add(spuIdKey(item));
		}
		if (spuIds.isEmpty()) {
			return Map.of();
		}
		List<Items> rows =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.and(
										w ->
												w.in(Items::getItemId, spuIds)
														.or()
														.in(Items::getDefaultItemId, spuIds)));
		Map<Long, Integer> maxBySpuId = new HashMap<>();
		for (Items row : rows) {
			if (row.getItemId() == null) {
				continue;
			}
			long spuId = spuIdKey(row);
			int store = row.getStore() != null ? row.getStore() : 0;
			maxBySpuId.merge(spuId, store, Math::max);
		}
		return maxBySpuId;
	}

	/** 结算加购：多规格 SPU 解析为有门店库存的 SKU（对齐详情页选规格下单） */
	public long resolveCheckoutSkuItemId(
			Items recommendItem,
			long companyId,
			DistributorContext distributorContext,
			Map<Long, Integer> maxSkuStoreBySpuId,
			Map<Long, Integer> distributorStoreBySpuId) {
		if (recommendItem == null || recommendItem.getItemId() == null) {
			return 0L;
		}
		long spuId = spuIdKey(recommendItem);
		List<Items> rows =
				itemsMapper.selectList(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.and(
										w ->
												w.eq(Items::getItemId, spuId)
														.or()
														.eq(Items::getDefaultItemId, spuId)));
		if (rows.isEmpty()) {
			return recommendItem.getItemId();
		}
		List<Items> skuRows = new ArrayList<>();
		for (Items row : rows) {
			if (row.getItemId() == null) {
				continue;
			}
			if (!Objects.equals(row.getItemId(), spuId)) {
				skuRows.add(row);
			}
		}
		if (skuRows.isEmpty() || isSingleSpec(recommendItem)) {
			return recommendItem.getItemId();
		}
		long bestItemId = recommendItem.getItemId();
		int bestStore = -1;
		for (Items sku : skuRows) {
			DistributorItems diRow =
					resolveMainDistributorRow(sku.getItemId(), spuId, distributorContext.byItemId());
			int store = resolveSkuDistributorStore(diRow);
			if (store > bestStore) {
				bestStore = store;
				bestItemId = sku.getItemId();
			}
		}
		return bestStore > 0 ? bestItemId : recommendItem.getItemId();
	}

	private static int resolveSkuDistributorStore(DistributorItems distributorRow) {
		if (distributorRow == null
				|| Boolean.TRUE.equals(distributorRow.getIsTotalStore())
				|| distributorRow.getStore() == null) {
			return 0;
		}
		return distributorRow.getStore().intValue();
	}

	private static boolean isSingleSpec(Items item) {
		return item != null && "true".equalsIgnoreCase(String.valueOf(item.getNospec()).trim());
	}

	public boolean hasValidPics(Items item) {
		if (item == null || !StringUtils.hasText(item.getPics())) {
			return false;
		}
		String pics = item.getPics().trim();
		return !pics.equals("[]") && !pics.equals("null");
	}

	public boolean isPairSellable(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId) {
		return isPairSellable(
				mainItem, recommendItem, companyId, distributorId, distributorItemsByItemId, Map.of());
	}

	public boolean isPairSellable(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId) {
		return isPairSellable(
				mainItem,
				recommendItem,
				companyId,
				distributorId,
				distributorItemsByItemId,
				defaultSkuBySpuId,
				Map.of());
	}

	public boolean isPairSellable(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId) {
		return isPairSellable(
				mainItem,
				recommendItem,
				companyId,
				distributorId,
				distributorItemsByItemId,
				defaultSkuBySpuId,
				maxSkuStoreBySpuId,
				Map.of());
	}

	public boolean isPairSellable(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId,
			Map<Long, Integer> distributorStoreBySpuId) {
		return isSellable(
						mainItem,
						companyId,
						distributorId,
						distributorItemsByItemId,
						defaultSkuBySpuId,
						maxSkuStoreBySpuId,
						distributorStoreBySpuId)
				&& isSellable(
						recommendItem,
						companyId,
						distributorId,
						distributorItemsByItemId,
						defaultSkuBySpuId,
						maxSkuStoreBySpuId,
						distributorStoreBySpuId);
	}

	/**
	 * C 端 match（PRD F-014）：仅过滤推荐商品的可售/库存/主图；主商品库存不参与 match 过滤。
	 * checkout-add 仍使用 {@link #isPairSellable} 做双向校验。
	 */
	public boolean isRecommendSellableForMatch(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId) {
		return isRecommendSellableForMatch(
				mainItem,
				recommendItem,
				companyId,
				distributorId,
				distributorItemsByItemId,
				defaultSkuBySpuId,
				Map.of());
	}

	public boolean isRecommendSellableForMatch(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId) {
		return isRecommendSellableForMatch(
				mainItem,
				recommendItem,
				companyId,
				distributorId,
				distributorItemsByItemId,
				defaultSkuBySpuId,
				maxSkuStoreBySpuId,
				Map.of());
	}

	public boolean isRecommendSellableForMatch(
			Items mainItem,
			Items recommendItem,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId,
			Map<Long, Integer> distributorStoreBySpuId) {
		if (mainItem == null || recommendItem == null) {
			return false;
		}
		if (!isSameShopAsMainForMatch(mainItem, recommendItem, companyId, distributorId)) {
			return false;
		}
		return isSellable(
				recommendItem,
				companyId,
				distributorId,
				distributorItemsByItemId,
				defaultSkuBySpuId,
				maxSkuStoreBySpuId,
				distributorStoreBySpuId);
	}

	private boolean isSameShopAsMainForMatch(
			Items mainItem, Items recommendItem, long companyId, long distributorId) {
		String productModel = resolveProductModel(companyId);
		long mainDist = distributorIdOf(mainItem);
		long recDist = distributorIdOf(recommendItem);
		if ("standard".equals(productModel) && distributorId > 0) {
			if (mainDist > 0 && mainDist != distributorId) {
				return false;
			}
			if (recDist > 0 && recDist != distributorId) {
				return false;
			}
		}
		if (mainDist > 0 && recDist > 0) {
			return mainDist == recDist;
		}
		return true;
	}

	private static long distributorIdOf(Items item) {
		if (item == null || item.getDistributorId() == null || item.getDistributorId() <= 0) {
			return 0L;
		}
		return item.getDistributorId().longValue();
	}

	public boolean isSellable(
			Items item,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId) {
		return isSellable(item, companyId, distributorId, distributorItemsByItemId, Map.of());
	}

	public boolean isSellable(
			Items item,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId) {
		return isSellable(
				item, companyId, distributorId, distributorItemsByItemId, defaultSkuBySpuId, Map.of(), Map.of());
	}

	public boolean isSellable(
			Items item,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId) {
		return isSellable(
				item,
				companyId,
				distributorId,
				distributorItemsByItemId,
				defaultSkuBySpuId,
				maxSkuStoreBySpuId,
				Map.of());
	}

	public boolean isSellable(
			Items item,
			long companyId,
			long distributorId,
			Map<Long, DistributorItems> distributorItemsByItemId,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId,
			Map<Long, Integer> distributorStoreBySpuId) {
		if (item == null) {
			return false;
		}
		String productModel = resolveProductModel(companyId);
		if ("standard".equals(productModel) && distributorId > 0) {
			Integer itemDistId = item.getDistributorId();
			if (itemDistId != null && itemDistId > 0 && itemDistId.longValue() != distributorId) {
				return false;
			}
		}
		boolean useDistributorJoin =
				"standard".equals(productModel)
						&& distributorId > 0
						&& (item.getDistributorId() == null || item.getDistributorId() == 0);
		if (useDistributorJoin) {
			DistributorItems di =
					resolveMainDistributorRow(item.getItemId(), spuIdKey(item), distributorItemsByItemId);
			if (di == null || !Boolean.TRUE.equals(di.getIsCanSale())) {
				return false;
			}
			return resolveStore(
							item,
							di,
							defaultSkuBySpuId,
							maxSkuStoreBySpuId,
							distributorStoreBySpuId)
					> 0;
		}
		if (!"onsale".equals(item.getApproveStatus())) {
			return false;
		}
		return resolveStore(item, null, defaultSkuBySpuId, maxSkuStoreBySpuId, distributorStoreBySpuId) > 0;
	}

	public int resolveStore(Items item, DistributorItems distributorRow) {
		return resolveStore(item, distributorRow, Map.of());
	}

	public int resolveStore(Items item, DistributorItems distributorRow, Map<Long, Items> defaultSkuBySpuId) {
		return resolveStore(item, distributorRow, defaultSkuBySpuId, Map.of(), Map.of());
	}

	public int resolveStore(
			Items item,
			DistributorItems distributorRow,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId) {
		return resolveStore(item, distributorRow, defaultSkuBySpuId, maxSkuStoreBySpuId, Map.of());
	}

	public int resolveStore(
			Items item,
			DistributorItems distributorRow,
			Map<Long, Items> defaultSkuBySpuId,
			Map<Long, Integer> maxSkuStoreBySpuId,
			Map<Long, Integer> distributorStoreBySpuId) {
		if (distributorRow != null) {
			if (Boolean.TRUE.equals(distributorRow.getIsTotalStore())) {
				return resolveSpuStore(item, defaultSkuBySpuId, maxSkuStoreBySpuId);
			}
			long spuId = spuIdKey(item);
			int aggregated =
					distributorStoreBySpuId != null
							? distributorStoreBySpuId.getOrDefault(spuId, 0)
							: 0;
			if (aggregated > 0) {
				return aggregated;
			}
			return distributorRow.getStore() != null ? distributorRow.getStore().intValue() : 0;
		}
		return resolveSpuStore(item, defaultSkuBySpuId, maxSkuStoreBySpuId);
	}

	static DistributorItems resolveMainDistributorRow(
			long itemId, long spuId, Map<Long, DistributorItems> distributorItemsByItemId) {
		if (distributorItemsByItemId == null || distributorItemsByItemId.isEmpty()) {
			return null;
		}
		DistributorItems direct = distributorItemsByItemId.get(itemId);
		if (direct != null) {
			return direct;
		}
		DistributorItems fallback = null;
		for (DistributorItems row : distributorItemsByItemId.values()) {
			if (spuId != distributorSpuIdKey(row)) {
				continue;
			}
			if (fallback == null) {
				fallback = row;
			}
			if (Objects.equals(row.getItemId(), spuId)) {
				return row;
			}
		}
		return fallback;
	}

	private static long distributorSpuIdKey(DistributorItems row) {
		Long defaultItemId = row.getDefaultItemId();
		if (defaultItemId != null && defaultItemId > 0) {
			return defaultItemId;
		}
		return row.getItemId();
	}

	private static long spuIdKey(Items item) {
		Long defaultItemId = item.getDefaultItemId();
		if (defaultItemId != null && defaultItemId > 0) {
			return defaultItemId;
		}
		return item.getItemId();
	}

	private static int resolveSpuStore(
			Items item, Map<Long, Items> defaultSkuBySpuId, Map<Long, Integer> maxSkuStoreBySpuId) {
		if (item == null) {
			return 0;
		}
		int maxSibling =
				maxSkuStoreBySpuId != null
						? maxSkuStoreBySpuId.getOrDefault(spuIdKey(item), 0)
						: 0;
		Items stockItem = defaultSkuBySpuId.get(item.getItemId());
		int fromDefault = 0;
		if (stockItem != null && stockItem.getStore() != null) {
			fromDefault = stockItem.getStore();
		} else if (item.getStore() != null) {
			fromDefault = item.getStore();
		}
		return Math.max(fromDefault, maxSibling);
	}

	public int resolvePrice(Items item, DistributorItems distributorRow) {
		if (distributorRow != null && distributorRow.getPrice() != null && distributorRow.getPrice() > 0) {
			return distributorRow.getPrice().intValue();
		}
		return item.getPrice() != null ? item.getPrice() : 0;
	}

	public long resolveSales(Items item, DistributorItems distributorRow) {
		if (distributorRow != null && distributorRow.getSales() != null) {
			return distributorRow.getSales();
		}
		return item.getSales() != null ? item.getSales().longValue() : 0L;
	}
}
