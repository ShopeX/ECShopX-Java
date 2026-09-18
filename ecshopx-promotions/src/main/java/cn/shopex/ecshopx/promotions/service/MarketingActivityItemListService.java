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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketingActivityItemListService {

	private static final Logger log = LoggerFactory.getLogger(MarketingActivityItemListService.class);

	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MarketingActivityItemListActivityQuerySupport activityQuerySupport;
	private final MarketingActivityItemListRelItemsQuerySupport relItemsQuerySupport;
	private final ObjectMapper objectMapper;

	public MarketingActivityItemListService(
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MarketingActivityItemListActivityQuerySupport activityQuerySupport,
			MarketingActivityItemListRelItemsQuerySupport relItemsQuerySupport,
			ObjectMapper objectMapper) {
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.activityQuerySupport = activityQuerySupport;
		this.relItemsQuerySupport = relItemsQuerySupport;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getActivityItemList(long companyId, Long marketingId, int page, int pageSize) {
		return getActivityItemList(companyId, marketingId, page, pageSize, false);
	}

	public Map<String, Object> getActivityItemList(
			long companyId, Long marketingId, int page, int pageSize, boolean onlyIsShowRelRows) {
		Optional<MarketingActivity> activityRow = activityQuerySupport.loadActivityRow(companyId, marketingId);
		Map<String, Object> activityInfoMap =
				activityRow.map(activityQuerySupport::buildActivityPayloadForItemList).orElseGet(Collections::emptyMap);
		Integer useBound = activityRow.map(MarketingActivity::getUseBound).orElse(null);
		boolean fullStoreBranch = useBound == null || useBound == 0;
		List<Long> distributorScope =
				Objects.requireNonNull(
						activityQuerySupport.buildDistributorScopeForFullStore(activityRow, companyId, marketingId),
						"distributorScope");

		Map<String, Object> rel;
		String itemType;
		if (fullStoreBranch) {
			rel = marketingActivityCatalogAccess.loadFullStoreMarketingActivityItemListData(
					companyId, distributorScope, page, pageSize, onlyIsShowRelRows);
			itemType = "all";
		} else {
			Optional<MarketingActivityItems> relItem =
					relItemsQuerySupport.findFirstRelForType(companyId, marketingId, onlyIsShowRelRows);
			itemType = relItem.map(MarketingActivityItems::getItemType).orElse(null);
			if ("tag".equals(itemType) || "brand".equals(itemType) || "category".equals(itemType)) {
				rel = relItemsQuerySupport.listAllForTagBrandCategory(companyId, marketingId, onlyIsShowRelRows);
			} else {
				rel = relItemsQuerySupport.listPagedForNormal(companyId, marketingId, page, pageSize, onlyIsShowRelRows);
			}
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> relList = (List<Map<String, Object>>) rel.getOrDefault("list", List.of());
		List<Long> itemIds = extractItemIds(relList);
		long totalCount = readTotalCount(rel.get("total_count"));

		if (!fullStoreBranch && "category".equals(itemType)) {
			List<Long> mainCategoryIds = new ArrayList<>(itemIds);
			itemIds = marketingActivityCatalogAccess.listItemIdsForCategoryFilter(
					companyId, mainCategoryIds, page, pageSize);
			totalCount = marketingActivityCatalogAccess.countItemsForCategoryFilter(companyId, mainCategoryIds);
		}
		if (!fullStoreBranch && "tag".equals(itemType)) {
			List<Long> tagIds = new ArrayList<>(itemIds);
			itemIds = marketingActivityCatalogAccess.listItemIdsByCompanyIdAndTagIdsUnpagedOrdered(companyId, tagIds);
			totalCount = marketingActivityCatalogAccess.countByCompanyIdAndTagIdsIn(companyId, tagIds);
		}
		if (!fullStoreBranch && "brand".equals(itemType)) {
			List<Long> brandKeys = new ArrayList<>(itemIds);
			itemIds = marketingActivityCatalogAccess.listItemIdsForBrandFilter(companyId, brandKeys, page, pageSize);
			totalCount = marketingActivityCatalogAccess.countItemsForBrandFilter(companyId, brandKeys);
		}

		Map<String, Object> skuMap =
				marketingActivityCatalogAccess.loadSkuItemsPageForMarketingActivityItemList(
						companyId, itemIds, 1, 2000);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuMap.getOrDefault("list", List.of());

		boolean activityRowMissing = activityRow.isEmpty();
		if (activityRowMissing) {
			log.warn("marketing_activity_row_missing_assembling_item_list");
		}
		List<Map<String, Object>> finalList = new ArrayList<>();
		for (Map<String, Object> sku : skuList) {
			finalList.add(mergeActivityFieldsOntoSkuRow(sku, activityInfoMap));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", finalList);
		if (fullStoreBranch) {
			Map<String, Object> newFilter = new LinkedHashMap<>();
			newFilter.put("company_id", String.valueOf(companyId));
			newFilter.put("distributor_id", 0L);
			Map<String, Object> brandList = new LinkedHashMap<>();
			brandList.put("total_count", 0);
			brandList.put("list", List.of());
			data.put("newFilter", newFilter);
			data.put("select_tags_list", List.of());
			data.put("brand_list", brandList);
		}
		data.put("activity", activityRow.isPresent() ? activityInfoMap : Collections.emptyList());
		return data;
	}

	private Map<String, Object> mergeActivityFieldsOntoSkuRow(Map<String, Object> sku, Map<String, Object> activityInfo) {
		Map<String, Object> out = new LinkedHashMap<>(sku);
		out.put("pics", parsePicsJson(out.get("pics")));
		out.put("marketing_id", activityInfo.get("marketing_id"));
		out.put("marketing_type", activityInfo.get("marketing_type"));
		out.put("promotion_tag", activityInfo.get("promotion_tag"));
		out.put("start_time", activityInfo.get("start_time"));
		out.put("end_time", activityInfo.get("end_time"));
		out.put("company_id", activityInfo.get("company_id"));
		out.put("is_show", Boolean.TRUE);
		Object store = out.get("store");
		out.put("store", store != null ? store : 0);
		Object spec = out.get("item_spec_desc");
		out.put("item_spec_desc", spec != null ? spec : "");
		Object itemName = out.get("item_name");
		out.put("item_brief", itemName != null ? itemName : "");
		out.put("status", Boolean.TRUE);
		return out;
	}

	private Object parsePicsJson(Object picsRaw) {
		if (picsRaw == null) {
			return null;
		}
		if (picsRaw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		String s = picsRaw.toString();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static long readTotalCount(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static List<Long> extractItemIds(List<Map<String, Object>> relList) {
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> line : relList) {
			Object id = line.get("item_id");
			if (id instanceof Number n) {
				ids.add(n.longValue());
			} else if (id != null) {
				try {
					ids.add(Long.parseLong(id.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return ids;
	}
}
