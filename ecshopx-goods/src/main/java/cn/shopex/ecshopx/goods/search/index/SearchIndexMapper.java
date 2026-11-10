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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 {@link SearchIndexSource} 物化为 {@link GoodsSearchDocument}。
 */
public class SearchIndexMapper {

	private static final Set<String> INDEX_ACTIVITY_TYPES = Set.of("seckill", "limited_time_sale", "group");

	public GoodsSearchDocument toDocument(SearchIndexSource src) {
		long salePrice = resolveSalePriceFen(src);
		String id = GoodsSearchDocumentId.of(src.companyId(), src.distributorId(), src.defaultItemId());
		List<GoodsSearchDocument.Sku> skus = mapSkus(src.skus());
		List<GoodsSearchDocument.Activity> activities = mapActivities(src.activities());
		GoodsSearchDocument.MemberPrices memberPrices = new GoodsSearchDocument.MemberPrices(
				copyPositivePrices(src.memberPriceGrade()),
				copyPositivePrices(src.memberPriceVipGrade()));
		return new GoodsSearchDocument(
				id,
				src.companyId(),
				src.distributorId(),
				src.defaultItemId(),
				nullToEmpty(src.itemName()),
				nullToEmpty(src.brief()),
				nullToEmpty(src.itemBn()),
				nullToEmpty(src.barcode()),
				src.tags() != null ? src.tags() : List.of(),
				src.approveStatus() != null ? src.approveStatus() : "onsale",
				src.auditStatus() != null ? src.auditStatus() : "approved",
				src.categoryIds() != null ? src.categoryIds() : List.of(),
				salePrice,
				src.marketPriceFen(),
				src.sales(),
				src.listedAt(),
				memberPrices,
				skus,
				activities);
	}

	private static long resolveSalePriceFen(SearchIndexSource src) {
		Long distributorPrice = src.distributorPriceFen();
		if (distributorPrice != null && distributorPrice > 0) {
			return distributorPrice;
		}
		return src.hqPriceFen();
	}

	private static List<GoodsSearchDocument.Sku> mapSkus(List<SearchIndexSource.Sku> skus) {
		List<GoodsSearchDocument.Sku> mapped = new ArrayList<>(skus.size());
		for (SearchIndexSource.Sku sku : skus) {
			mapped.add(new GoodsSearchDocument.Sku(sku.skuId(), sku.store(), sku.approveStatus()));
		}
		return List.copyOf(mapped);
	}

	private static List<GoodsSearchDocument.Activity> mapActivities(List<SearchIndexSource.Activity> activities) {
		List<GoodsSearchDocument.Activity> mapped = new ArrayList<>();
		for (SearchIndexSource.Activity activity : activities) {
			if (!INDEX_ACTIVITY_TYPES.contains(activity.type())) {
				continue;
			}
			int priority = activity.priority() != null ? activity.priority() : activityPriorityForType(activity.type());
			mapped.add(new GoodsSearchDocument.Activity(
					activity.type(),
					activity.activityId(),
					activity.activityPrice(),
					activity.startAt(),
					activity.endAt(),
					priority));
		}
		return List.copyOf(mapped);
	}

	private static int activityPriorityForType(String type) {
		return switch (type) {
			case "seckill", "limited_time_sale" -> 1;
			case "group" -> 2;
			default -> Integer.MAX_VALUE;
		};
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	private static Map<Long, Long> copyPositivePrices(Map<Long, Long> prices) {
		return prices.entrySet().stream()
				.filter(e -> e.getValue() != null && e.getValue() > 0)
				.collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
