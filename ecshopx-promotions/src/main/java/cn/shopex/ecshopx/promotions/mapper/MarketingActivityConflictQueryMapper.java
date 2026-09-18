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

package cn.shopex.ecshopx.promotions.mapper;

import cn.shopex.ecshopx.promotions.dto.MarketingActivityConflictRow;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityItemBindingRow;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketingActivityConflictQueryMapper {

	List<Long> selectMarketingIdsUseBound0Overlapping(
			@Param("companyId") long companyId,
			@Param("marketingTypes") List<String> marketingTypes,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<MarketingActivityItemBindingRow> selectActiveMarketingItemBindings(
			@Param("companyId") long companyId, @Param("nowSeconds") long nowSeconds);

	List<MarketingActivityItemBindingRow> selectPromotionItemBindings(
			@Param("companyId") long companyId,
			@Param("itemIds") Collection<Long> itemIds,
			@Param("itemType") String itemType,
			@Param("nowSeconds") long nowSeconds);

	List<MarketingActivityConflictRow> selectOverlappingMarketingActivities(
			@Param("companyId") long companyId,
			@Param("marketingIds") List<Long> marketingIds,
			@Param("marketingTypes") List<String> marketingTypes,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemCategoryCategoryType(
			@Param("itemCategories") Collection<?> itemCategories,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemCategoryNormalItems(
			@Param("itemCategories") Collection<?> itemCategories,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemCategoryBrandType(
			@Param("itemCategories") Collection<?> itemCategories,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemCategoryTagType(
			@Param("itemCategories") Collection<?> itemCategories,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemTagsTagToTag(
			@Param("tagIds") Collection<Long> tagIds, @Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemTagsNormalItems(
			@Param("tagIds") Collection<Long> tagIds,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemTagsBrandType(
			@Param("tagIds") Collection<Long> tagIds,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemTagsCategoryType(
			@Param("tagIds") Collection<Long> tagIds,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemBrandBrandType(@Param("brandIds") Collection<Long> brandIds);

	List<Long> selectMarketingIdsByItemBrandNormalItems(
			@Param("brandIds") Collection<Long> brandIds,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemBrandTagType(
			@Param("brandIds") Collection<Long> brandIds,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	List<Long> selectMarketingIdsByItemBrandCategoryType(
			@Param("brandIds") Collection<Long> brandIds,
			@Param("windowEnd") long windowEnd,
			@Param("overlapStart") long overlapStart);

	long countItemsByGoodsIdsAndItemIds(
			@Param("companyId") long companyId,
			@Param("itemIds") Collection<Long> itemIds,
			@Param("goodsIds") Collection<Long> goodsIds);

	long countItemsByItemCategoryAndGoodsIds(
			@Param("companyId") long companyId,
			@Param("itemCategories") Collection<?> itemCategories,
			@Param("goodsIds") Collection<Long> goodsIds);

	long countItemsByBrandIdsAndGoodsIds(
			@Param("companyId") long companyId,
			@Param("brandIds") Collection<Long> brandIds,
			@Param("goodsIds") Collection<Long> goodsIds);

	List<Long> selectDistinctTagIdsByItemIds(
			@Param("companyId") long companyId, @Param("itemIds") Collection<Long> itemIds);

	long countItemsByItemCategoryAndItemIds(
			@Param("companyId") long companyId,
			@Param("itemCategories") Collection<?> itemCategories,
			@Param("itemIds") Collection<Long> itemIds);

	long countItemsByBrandIdsAndItemIds(
			@Param("companyId") long companyId,
			@Param("brandIds") Collection<Long> brandIds,
			@Param("itemIds") Collection<Long> itemIds);

	long countPromotionGroupOverlapForItemIds(
			@Param("companyId") long companyId,
			@Param("itemIds") Collection<Long> itemIds,
			@Param("beginTime") long beginTime,
			@Param("endTime") long endTime,
			@Param("excludeGroupId") Long excludeGroupId);

	long countBargainOverlapForItemIds(
			@Param("companyId") long companyId,
			@Param("itemIds") Collection<Long> itemIds,
			@Param("beginTime") long beginTime,
			@Param("endTime") long endTime,
			@Param("excludeBargainId") Long excludeBargainId);
}
