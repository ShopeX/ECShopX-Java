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

package cn.shopex.ecshopx.common.goods;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Catalog reads for marketing-activity flows. Implemented in {@code ecshopx-goods} so
 * {@code ecshopx-promotions} stays free of a Maven dependency on {@code ecshopx-goods}
 * (goods already depends on promotions).
 */
public interface MarketingActivityCatalogAccess {

	Map<String, Object> loadSkuItemsList(long companyId, List<Long> itemIds);

	/**
	 * SKU rows for marketing gift attachment on item detail: keeps {@code pics} as decoded JSON arrays (no list-thumb
	 * single-URL replacement).
	 */
	Map<String, Object> loadSkuItemsListForMarketingGift(long companyId, List<Long> itemIds);

	/**
	 * Loads {@code item_id} and {@code type} for existing SKUs (same column semantics as catalog list APIs).
	 */
	List<Map<String, Object>> loadItemIdAndTypeRows(long companyId, List<Long> itemIds);

	boolean anyGiftItem(long companyId, List<Long> itemIds);

	List<Long> listDistinctTagIdsByItemIds(long companyId, List<Long> itemIds);

	List<Long> listItemIdsByGoodsId(long companyId, long goodsId);

	Map<Long, Long> loadItemPriceByItemId(long companyId, List<Long> itemIds);

	/** Batch-resolve {@code item_bn} to {@code item_id} for a company (trimmed keys, first row wins on duplicates). */
	Map<String, Long> mapItemBnToItemIdForCompany(long companyId, Collection<String> itemBns);

	/**
	 * Full-store segment: item list pages under company + distributor scope (intermediate shape: only {@code
	 * total_count} and {@code list} with {@code item_id} rows).
	 */
	Map<String, Object> loadFullStoreMarketingActivityItemListData(
			long companyId, List<Long> distributorScope, int page, int pageSize, boolean orderByItemIdDesc);

	/**
	 * SKU segment: paged items query with optional {@code item_id} filter; goods-side enrichment only (no activity
	 * fields).
	 */
	Map<String, Object> loadSkuItemsPageForMarketingActivityItemList(
			long companyId, List<Long> itemIds, int page, int pageSize);

	List<Long> listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(long companyId, List<Long> tagIds);

	long countByCompanyIdAndTagIdsIn(long companyId, List<Long> tagIds);

	List<Long> listItemIdsForCategoryFilter(long companyId, List<Long> activityMainCategoryIds, int page, int pageSize);

	long countItemsForCategoryFilter(long companyId, List<Long> activityMainCategoryIds);

	List<Long> listItemIdsForBrandFilter(long companyId, List<Long> brandIds, int page, int pageSize);

	long countItemsForBrandFilter(long companyId, List<Long> brandIds);

	List<Map<String, Object>> listTagsForMarketingActivityInfo(long companyId, List<Long> tagIds);

	List<Map<String, Object>> listBrandAttributesForMarketingActivityInfo(long companyId, List<Long> brandAttributeIds);

	List<Map<String, Object>> buildMarketingActivityItemTreeLists(long companyId, List<Long> itemIdsOrdered);

	/**
	 * Resolves marketing ids whose activity-item rows hit the given SKU within the candidate set (normal / tag /
	 * brand / category), for the current time window on {@code promotions_marketing_activity_items}.
	 */
	List<Long> listMarketingIdsHitBySkuItem(long companyId, long itemId, List<Long> candidateMarketingIds, int nowEpochSeconds);

	/**
	 * Narrow item fields for SKU marketing: distributor, goods id, and related catalog keys.
	 */
	@Nullable
	Map<String, Object> loadItemNarrowMapForSkuMarketing(long companyId, long itemId);

	/**
	 * Wxapp item list enrichment for seckill H5 getinfo: filter {@code company_id} + {@code item_id[]}, fixed internal
	 * list page 1 and page size 100.
	 *
	 * @param companyId tenant
	 * @param userId H5 member id; 0 when anonymous (FrontNoAuth)
	 * @param itemIds SKU {@code item_id} list (order-preserving dedupe)
	 * @param acceptLanguage raw Accept-Language; implementation defaults to zh-CN when blank
	 * @return map aligned with wxapp item list query (at least {@code list}, {@code total_count})
	 */
	Map<String, Object> loadWxappItemListDataForSeckillGetInfo(
			long companyId, long userId, List<Long> itemIds, String acceptLanguage);
}
