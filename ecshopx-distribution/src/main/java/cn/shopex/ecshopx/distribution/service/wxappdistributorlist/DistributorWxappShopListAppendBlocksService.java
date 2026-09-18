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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist;

import cn.shopex.ecshopx.distribution.dto.DistributorTagRelRow;
import cn.shopex.ecshopx.distribution.mapper.DistributionStoreMarketingActivityMapper;
import cn.shopex.ecshopx.distribution.repository.WxappDistributorItemsAppendJdbcRepository;
import cn.shopex.ecshopx.distribution.repository.WxappItemTagFilteredItemIdsJdbcRepository;
import cn.shopex.ecshopx.distribution.repository.WxappKaquanDiscountCardOngoingJdbcRepository;
import cn.shopex.ecshopx.distribution.service.DistributorTagRelQueryService;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorWxappShopListAppendBlocksService {

	private final DistributorTagRelQueryService distributorTagRelQueryService;
	private final WxappKaquanDiscountCardOngoingJdbcRepository wxappKaquanDiscountCardOngoingJdbcRepository;
	private final DistributionStoreMarketingActivityMapper distributionStoreMarketingActivityMapper;
	private final TradeRateMapper tradeRateMapper;
	private final WxappItemTagFilteredItemIdsJdbcRepository wxappItemTagFilteredItemIdsJdbcRepository;
	private final WxappDistributorItemsAppendJdbcRepository wxappDistributorItemsAppendJdbcRepository;

	public DistributorWxappShopListAppendBlocksService(
			DistributorTagRelQueryService distributorTagRelQueryService,
			WxappKaquanDiscountCardOngoingJdbcRepository wxappKaquanDiscountCardOngoingJdbcRepository,
			DistributionStoreMarketingActivityMapper distributionStoreMarketingActivityMapper,
			TradeRateMapper tradeRateMapper,
			WxappItemTagFilteredItemIdsJdbcRepository wxappItemTagFilteredItemIdsJdbcRepository,
			WxappDistributorItemsAppendJdbcRepository wxappDistributorItemsAppendJdbcRepository) {
		this.distributorTagRelQueryService = distributorTagRelQueryService;
		this.wxappKaquanDiscountCardOngoingJdbcRepository = wxappKaquanDiscountCardOngoingJdbcRepository;
		this.distributionStoreMarketingActivityMapper = distributionStoreMarketingActivityMapper;
		this.tradeRateMapper = tradeRateMapper;
		this.wxappItemTagFilteredItemIdsJdbcRepository = wxappItemTagFilteredItemIdsJdbcRepository;
		this.wxappDistributorItemsAppendJdbcRepository = wxappDistributorItemsAppendJdbcRepository;
	}

	public void appendAllConfiguredBlocks(
			long companyId,
			int showTag,
			int showDiscount,
			int showMarketingActivity,
			int showSalesCount,
			int showScore,
			int showItems,
			List<Long> itemTagIds,
			List<Map<String, Object>> listRows,
			long nowEpochSecond,
			Map<Long, Long> netSalesByDistributorId) {
		if (listRows.isEmpty()) {
			return;
		}
		List<Long> pageIds = new ArrayList<>();
		for (Map<String, Object> row : listRows) {
			Object did = row.get("distributor_id");
			if (did instanceof Number n) {
				pageIds.add(n.longValue());
			}
		}
		if (showTag != 0) {
			List<DistributorTagRelRow> relRows =
					distributorTagRelQueryService.listRelWithTagsByCompanyAndDistributorIds(companyId, pageIds);
			Map<Long, List<Map<String, Object>>> byDist = new LinkedHashMap<>();
			for (DistributorTagRelRow rr : relRows) {
				byDist.computeIfAbsent(rr.getDistributorId(), k -> new ArrayList<>()).add(relRowToMap(rr));
			}
			for (Map<String, Object> row : listRows) {
				long did = longOf(row.get("distributor_id"));
				row.put("tagList", byDist.getOrDefault(did, List.of()));
			}
		}
		if (showDiscount != 0) {
			List<Map<String, Object>> cards =
					wxappKaquanDiscountCardOngoingJdbcRepository.listOngoingShopCards(companyId, pageIds);
			Map<Long, List<Map<String, Object>>> byShop = new LinkedHashMap<>();
			for (Map<String, Object> c : cards) {
				Object sid = c.get("source_id");
				if (!(sid instanceof Number n)) {
					continue;
				}
				byShop.computeIfAbsent(n.longValue(), k -> new ArrayList<>()).add(c);
			}
			for (Map<String, Object> row : listRows) {
				long did = longOf(row.get("distributor_id"));
				row.put("discountCardList", byShop.getOrDefault(did, List.of()));
			}
		}
		if (showMarketingActivity != 0) {
			for (Map<String, Object> row : listRows) {
				long did = longOf(row.get("distributor_id"));
				List<Map<String, Object>> acts =
						distributionStoreMarketingActivityMapper.selectOngoingMarketingActivitiesForDistributor(
								companyId, did, nowEpochSecond);
				row.put("marketingActivityList", acts == null ? List.of() : acts);
			}
		}
		if (showSalesCount != 0) {
			for (Map<String, Object> row : listRows) {
				long did = longOf(row.get("distributor_id"));
				long sc = netSalesByDistributorId.getOrDefault(did, 0L);
				row.put("sales_count", Integer.valueOf((int) Math.max(0L, Math.min(sc, Integer.MAX_VALUE))));
			}
		}
		if (showScore != 0 && !pageIds.isEmpty()) {
			Map<Long, BigDecimal> avgByDist = new LinkedHashMap<>();
			List<Map<String, Object>> starRows =
					tradeRateMapper.selectAvgStarBatchByCompanyAndDistributorIds(companyId, pageIds);
			if (starRows != null) {
				for (Map<String, Object> m : starRows) {
					Object did = m.get("distributor_id");
					Object av = m.get("avg_star");
					if (did instanceof Number n && av instanceof BigDecimal bd) {
						avgByDist.put(n.longValue(), bd);
					}
				}
			}
			for (Map<String, Object> row : listRows) {
				long did = longOf(row.get("distributor_id"));
				LinkedHashMap<String, Object> score = new LinkedHashMap<>();
				score.put("avg_star", "5.0");
				score.put("default", Integer.valueOf(1));
				BigDecimal avg = avgByDist.get(did);
				if (avg != null && Double.isFinite(avg.doubleValue())) {
					score.put("avg_star", String.format(Locale.US, "%.1f", avg));
					score.put("default", Integer.valueOf(0));
				}
				row.put("scoreList", score);
			}
		}
		if (showItems != 0) {
			List<Long> filterItemIds =
					itemTagIds == null || itemTagIds.isEmpty()
							? List.of()
							: wxappItemTagFilteredItemIdsJdbcRepository.listItemIdsByCompanyAndTagIds(companyId, itemTagIds);
			for (Map<String, Object> row : listRows) {
				long did = longOf(row.get("distributor_id"));
				List<Map<String, Object>> items =
						wxappDistributorItemsAppendJdbcRepository.listTopItemsForDistributor(
								companyId, did, filterItemIds, 10, 0);
				row.put("itemList", items);
			}
		}
	}

	private static Map<String, Object> relRowToMap(DistributorTagRelRow rr) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_id", rr.getDistributorId() != null ? rr.getDistributorId().toString() : "");
		m.put("tag_id", rr.getTagId() != null ? rr.getTagId().toString() : "");
		m.put("company_id", rr.getCompanyId() != null ? rr.getCompanyId().toString() : "");
		m.put("tag_name", rr.getTagName());
		m.put("tag_color", rr.getTagColor());
		m.put("font_color", rr.getFontColor());
		m.put("description", rr.getDescription());
		m.put("tag_icon", rr.getTagIcon());
		m.put("front_show", rr.getFrontShow() != null ? rr.getFrontShow().toString() : "");
		m.put("created", rr.getCreated() != null ? rr.getCreated().toString() : "");
		m.put("updated", rr.getUpdated() != null ? rr.getUpdated().toString() : "");
		m.put("merchant_name", "");
		m.put("distribution_type", "");
		return m;
	}

	private static long longOf(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null || !StringUtils.hasText(String.valueOf(v))) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
