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
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityListService {

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MarketingActivityItemListActivityQuerySupport marketingActivityItemListActivityQuerySupport;
	private final MarketingActivityListMultiLangReadService marketingActivityListMultiLangReadService;
	private final DistributorListQueryService distributorListQueryService;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final ObjectMapper objectMapper;

	public MarketingActivityListService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MarketingActivityItemListActivityQuerySupport marketingActivityItemListActivityQuerySupport,
			MarketingActivityListMultiLangReadService marketingActivityListMultiLangReadService,
			DistributorListQueryService distributorListQueryService,
			VipGradeListQueryService vipGradeListQueryService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.marketingActivityItemListActivityQuerySupport = marketingActivityItemListActivityQuerySupport;
		this.marketingActivityListMultiLangReadService = marketingActivityListMultiLangReadService;
		this.distributorListQueryService = distributorListQueryService;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getMarketingActivityList(
			long companyId,
			String operatorType,
			Long jwtDistributorId,
			String marketingType,
			String marketingIdRaw,
			String marketingName,
			String startTimeInput,
			String endTimeInput,
			String status,
			String itemType,
			String storeIdRaw,
			String distributorIdRaw,
			int page,
			int pageSize,
			String acceptLanguageHeader) {
		long nowEpoch = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<MarketingActivity> activityWrapper = new LambdaQueryWrapper<>();
		activityWrapper.eq(MarketingActivity::getCompanyId, companyId).eq(MarketingActivity::getMarketingType, marketingType);
		Long marketingIdFilter = parseOptionalLong(marketingIdRaw);
		if (marketingIdFilter != null && marketingIdFilter > 0L) {
			activityWrapper.eq(MarketingActivity::getMarketingId, marketingIdFilter);
		}
		if (StringUtils.hasText(marketingName)) {
			String escaped = escapeSqlLike(marketingName.trim());
			activityWrapper.apply("marketing_name LIKE CONCAT('%',{0},'%') ESCAPE '\\\\'", escaped);
		}
		if (StringUtils.hasText(status)) {
			String st = status.trim().toLowerCase(Locale.ROOT);
			switch (st) {
				case "waiting" -> activityWrapper
						.ge(MarketingActivity::getStartTime, (int) nowEpoch)
						.ge(MarketingActivity::getEndTime, (int) nowEpoch);
				case "ongoing" -> activityWrapper
						.le(MarketingActivity::getStartTime, (int) nowEpoch)
						.ge(MarketingActivity::getEndTime, (int) nowEpoch);
				case "end" -> activityWrapper.le(MarketingActivity::getEndTime, (int) nowEpoch);
				default -> {
					// unknown status: ignore filter
				}
			}
		}
		if (shouldApplyItemTypeFilter(itemType)) {
			activityWrapper.eq(MarketingActivity::getItemType, itemType.trim());
		}
		long effectiveDistributorId =
				"distributor".equals(operatorType) ? parseLongPreferQueryThenJwt(distributorIdRaw, jwtDistributorId) : 0L;
		if ("distributor".equals(operatorType)) {
			activityWrapper.apply("shop_ids LIKE {0}", "%," + effectiveDistributorId + ",%");
		} else {
			Long storeId = parseOptionalLong(storeIdRaw);
			if (storeId != null && storeId >= 0L) {
				activityWrapper.apply("shop_ids LIKE {0}", "%," + storeId + ",%");
			}
		}
		if (StringUtils.hasText(startTimeInput) && StringUtils.hasText(endTimeInput)) {
			Integer startEpoch = tryParseCreatedEpoch(startTimeInput);
			Integer endEpoch = tryParseCreatedEpoch(endTimeInput);
			if (startEpoch != null && endEpoch != null) {
				activityWrapper.ge(MarketingActivity::getCreated, startEpoch).le(MarketingActivity::getCreated, endEpoch);
			}
		}
		if (effectiveDistributorId > 0L && "distributor".equals(operatorType)) {
			activityWrapper
					.eq(MarketingActivity::getSourceId, effectiveDistributorId)
					.eq(MarketingActivity::getSourceType, "distributor");
		}
		activityWrapper.orderByDesc(MarketingActivity::getMarketingId);
		Page<MarketingActivity> p = new Page<>(page, pageSize);
		marketingActivityMapper.selectPage(p, activityWrapper);
		long totalCount = p.getTotal();
		List<MarketingActivity> records = p.getRecords() == null ? List.of() : p.getRecords();
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (MarketingActivity entity : records) {
			Map<String, Object> row =
					new LinkedHashMap<>(marketingActivityItemListActivityQuerySupport.buildActivityPayloadForItemList(entity));
			row.put("items", List.of());
			row.put("itemTreeLists", List.of());
			listMaps.add(row);
		}
		attachGiftsForPage(companyId, records, listMaps);
		if (!listMaps.isEmpty()) {
			marketingActivityListMultiLangReadService.applyListTranslations(companyId, listMaps, acceptLanguageHeader);
			applySourceNames(companyId, listMaps);
			String distributorIdForEdit =
					"distributor".equals(operatorType) ? String.valueOf(effectiveDistributorId) : distributorIdRaw;
			applyMemberGradeAndEditBtn(companyId, listMaps, distributorIdForEdit);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		return out;
	}

	private void attachGiftsForPage(
			long companyId, List<MarketingActivity> records, List<Map<String, Object>> listMaps) {
		List<Long> mids = pageMarketingIds(records);
		if (mids.isEmpty()) {
			for (Map<String, Object> row : listMaps) {
				row.put("gifts", List.of());
				row.put("giftsItemLists", List.of());
			}
			return;
		}
		LambdaQueryWrapper<MarketingGiftItems> giftWrapper = new LambdaQueryWrapper<>();
		giftWrapper
				.eq(MarketingGiftItems::getCompanyId, companyId)
				.in(MarketingGiftItems::getMarketingId, mids)
				.orderByAsc(MarketingGiftItems::getId);
		List<MarketingGiftItems> giftRows = marketingGiftItemsMapper.selectList(giftWrapper);
		List<Long> giftItemIds =
				giftRows.stream().map(MarketingGiftItems::getItemId).filter(Objects::nonNull).distinct().toList();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList =
				giftItemIds.isEmpty()
						? List.of()
						: (List<Map<String, Object>>)
								marketingActivityCatalogAccess
										.loadSkuItemsListForMarketingGift(companyId, giftItemIds)
										.get("list");
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : skuList) {
			Object iid = skuRow.get("item_id");
			if (iid instanceof Number n) {
				skuByItemId.putIfAbsent(n.longValue(), skuRow);
			}
		}
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftGoodsByMid = new LinkedHashMap<>();
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftItemsByMid = new LinkedHashMap<>();
		for (MarketingGiftItems g : giftRows) {
			Long mid = g.getMarketingId();
			if (mid == null) {
				continue;
			}
			Object priceKey = priceKeyForGrouping(g.getPrice());
			Map<String, Object> giftMap = giftEntityToMap(g);
			Map<String, Object> sku = g.getItemId() != null ? skuByItemId.get(g.getItemId()) : null;
			Map<Object, List<Map<String, Object>>> relGoods = relGiftGoodsByMid.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			Map<Object, List<Map<String, Object>>> relItems = relGiftItemsByMid.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			if (sku == null || sku.isEmpty()) {
				giftMap.put("status", "invalid");
			} else {
				giftMap.put("status", "valid");
				Map<String, Object> mergedSku = new LinkedHashMap<>(sku);
				mergedSku.put("gift_num", g.getGiftNum() != null ? g.getGiftNum() : 0);
				mergedSku.put("without_return", g.getWithoutReturn() != null ? g.getWithoutReturn() : false);
				addToPriceBucket(relItems, priceKey, mergedSku);
			}
			addToPriceBucket(relGoods, priceKey, giftMap);
		}
		for (Map<String, Object> row : listMaps) {
			long mid;
			try {
				mid = Long.parseLong(String.valueOf(row.get("marketing_id")));
			} catch (NumberFormatException e) {
				continue;
			}
			String mt = row.get("marketing_type") == null ? "" : String.valueOf(row.get("marketing_type"));
			Map<Object, List<Map<String, Object>>> relG = relGiftGoodsByMid.getOrDefault(mid, Map.of());
			Map<Object, List<Map<String, Object>>> relI = relGiftItemsByMid.getOrDefault(mid, Map.of());
			switch (mt) {
				case "full_gift" -> applyFullGiftGifts(row, relG, relI);
				case "plus_price_buy" -> applyPlusPriceBuyGifts(row, relG, relI);
				default -> {
					row.put("gifts", List.of());
					row.put("giftsItemLists", List.of());
				}
			}
			if (!row.containsKey("gifts")) {
				row.put("gifts", List.of());
			}
			if (!row.containsKey("giftsItemLists")) {
				row.put("giftsItemLists", List.of());
			}
		}
	}

	private static List<Long> pageMarketingIds(List<MarketingActivity> records) {
		return records.stream().map(MarketingActivity::getMarketingId).filter(Objects::nonNull).distinct().toList();
	}

	private void applySourceNames(long companyId, List<Map<String, Object>> listMaps) {
		List<Long> distributorIds = new ArrayList<>();
		for (Map<String, Object> row : listMaps) {
			if (!"distributor".equals(String.valueOf(row.get("source_type")))) {
				continue;
			}
			try {
				long sid = Long.parseLong(String.valueOf(row.get("source_id")));
				if (sid > 0L) {
					distributorIds.add(sid);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		List<Long> distinctIds = distributorIds.stream().distinct().toList();
		if (distinctIds.isEmpty()) {
			for (Map<String, Object> row : listMaps) {
				row.put("source_name", "");
			}
			return;
		}
		List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId, distinctIds);
		Map<Long, String> idToName = new LinkedHashMap<>();
		for (Distributor d : dists) {
			long id = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			idToName.put(id, d.getName() != null ? d.getName() : "");
		}
		for (Map<String, Object> row : listMaps) {
			if (!"distributor".equals(String.valueOf(row.get("source_type")))) {
				row.put("source_name", "");
				continue;
			}
			long parsedSourceId = 0L;
			try {
				parsedSourceId = Long.parseLong(String.valueOf(row.get("source_id")));
			} catch (NumberFormatException ignored) {
			}
			row.put("source_name", idToName.getOrDefault(parsedSourceId, ""));
		}
	}

	private void applyMemberGradeAndEditBtn(long companyId, List<Map<String, Object>> listMaps, String distributorIdRaw) {
		LinkedHashMap<String, Map<String, Object>> memberGrade = new LinkedHashMap<>();
		for (Map<String, Object> cardRow : memberCardGradeQueryService.getGradeListByCompanyId(companyId, false)) {
			Object gid = cardRow.get("grade_id");
			if (gid instanceof Number n) {
				memberGrade.put(String.valueOf(n.longValue()), cardRow);
			} else if (gid != null && StringUtils.hasText(gid.toString())) {
				try {
					memberGrade.put(String.valueOf(Long.parseLong(gid.toString().trim())), cardRow);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		List<Map<String, Object>> vipRows = vipGradeListQueryService.listDataVipGrade(companyId, true);
		for (Map<String, Object> vipRow : vipRows) {
			Object lv = vipRow.get("lv_type");
			String key = lv == null ? "" : String.valueOf(lv);
			if (StringUtils.hasText(key)) {
				memberGrade.put(key, vipRow);
			}
		}
		long sourceIdForEdit = LeadingNumberParser.parseAsLong(distributorIdRaw == null ? "" : distributorIdRaw);
		for (Map<String, Object> row : listMaps) {
			long rowSourceId = 0L;
			try {
				rowSourceId = Long.parseLong(String.valueOf(row.get("source_id")));
			} catch (NumberFormatException ignored) {
			}
			String rowSourceType = row.get("source_type") == null ? "" : String.valueOf(row.get("source_type"));
			String editBtn;
			if (rowSourceId != sourceIdForEdit) {
				if ("staff".equals(rowSourceType) && sourceIdForEdit == 0L) {
					editBtn = "Y";
				} else {
					editBtn = "N";
				}
			} else {
				editBtn = "Y";
			}
			row.put("edit_btn", editBtn);
			List<Object> validGrade = normalizeValidGradeList(row.get("valid_grade"));
			if (validGrade.isEmpty()) {
				row.put("member_grade", List.of());
				continue;
			}
			ArrayList<String> mg = new ArrayList<>(Collections.nCopies(validGrade.size(), null));
			for (int k = 0; k < validGrade.size(); k++) {
				Object keyObj = validGrade.get(k);
				String keyStr = keyObj == null ? "" : String.valueOf(keyObj);
				Map<String, Object> hit = memberGrade.get(keyStr);
				if (hit != null && hit.get("grade_name") != null) {
					mg.set(k, String.valueOf(hit.get("grade_name")));
				}
			}
			row.put("member_grade", mg);
		}
	}

	private List<Object> normalizeValidGradeList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				Object parsed = objectMapper.readValue(s.trim(), Object.class);
				if (parsed instanceof List<?> l) {
					return new ArrayList<>(l);
				}
				return List.of();
			} catch (Exception e) {
				return List.of();
			}
		}
		if (raw instanceof List<?> l) {
			return new ArrayList<>(l);
		}
		return List.of();
	}

	private static boolean shouldApplyItemTypeFilter(String itemType) {
		if (!StringUtils.hasText(itemType)) {
			return false;
		}
		return !"0".equals(itemType.trim());
	}

	private static Integer tryParseCreatedEpoch(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		char c0 = t.charAt(0);
		if (!Character.isDigit(c0) && c0 != '+' && c0 != '-') {
			return null;
		}
		try {
			long v = Long.parseLong(LeadingNumberParser.parseAsString(t));
			if (v < (long) Integer.MIN_VALUE || v > (long) Integer.MAX_VALUE) {
				return null;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseLongPreferQueryThenJwt(String distributorIdRaw, Long jwtDistributorId) {
		Long q = parseOptionalLong(distributorIdRaw);
		if (q != null) {
			return q;
		}
		return jwtDistributorId != null ? jwtDistributorId : 0L;
	}

	private static Long parseOptionalLong(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		char c0 = t.charAt(0);
		if (!Character.isDigit(c0) && c0 != '+' && c0 != '-') {
			return null;
		}
		try {
			long v = Long.parseLong(LeadingNumberParser.parseAsString(t));
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Object priceJsonValue(Object priceKey) {
		if (priceKey instanceof Integer i && i == 0) {
			return 0;
		}
		return priceKey;
	}

	private static Object priceKeyForGrouping(Integer priceCents) {
		if (priceCents == null || priceCents == 0) {
			return 0;
		}
		return BigDecimal.valueOf(priceCents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static void addToPriceBucket(
			Map<Object, List<Map<String, Object>>> byPrice, Object key, Map<String, Object> row) {
		byPrice.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
	}

	private Map<String, Object> giftEntityToMap(MarketingGiftItems g) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (g.getId() != null) {
			m.put("id", g.getId());
		}
		if (g.getMarketingId() != null) {
			m.put("marketing_id", g.getMarketingId());
		}
		if (g.getCompanyId() != null) {
			m.put("company_id", g.getCompanyId());
		}
		if (g.getItemId() != null) {
			m.put("item_id", g.getItemId());
		}
		m.put("item_type", g.getItemType());
		m.put("item_name", g.getItemName());
		m.put("price", g.getPrice());
		m.put("store", g.getStore());
		m.put("gift_num", g.getGiftNum());
		m.put("pics", decodePicsForGiftRow(g.getPics()));
		m.put("without_return", g.getWithoutReturn());
		m.put("condition_type", g.getConditionType());
		m.put("filter_full", g.getFilterFull());
		m.put("item_spec_desc", g.getItemSpecDesc());
		m.put("created", g.getCreated());
		m.put("updated", g.getUpdated());
		return m;
	}

	private Object decodePicsForGiftRow(String picsJson) {
		if (picsJson == null || picsJson.isBlank()) {
			return List.of("null");
		}
		try {
			Object parsed = objectMapper.readValue(picsJson.trim(), List.class);
			if (parsed instanceof List<?> l) {
				return new ArrayList<>(l);
			}
		} catch (Exception ignored) {
		}
		return List.of("null");
	}

	private static void applyFullGiftGifts(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		List<Map<String, Object>> g0 = relGiftGoods.get(0);
		if (g0 != null && !g0.isEmpty()) {
			result.put("gifts", new ArrayList<>(g0));
		}
		List<Map<String, Object>> i0 = relGiftItems.get(0);
		if (i0 != null && !i0.isEmpty()) {
			result.put("giftsItemLists", new ArrayList<>(i0));
		}
	}

	private static void applyPlusPriceBuyGifts(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		if (relGiftGoods.isEmpty() || relGiftItems.isEmpty()) {
			return;
		}
		List<Map<String, Object>> giftsOut = new ArrayList<>();
		List<Map<String, Object>> giftsItemListsOut = new ArrayList<>();
		for (Map.Entry<Object, List<Map<String, Object>>> en : relGiftGoods.entrySet()) {
			Object pk = en.getKey();
			List<Map<String, Object>> goodsList = en.getValue();
			List<Map<String, Object>> itemList = relGiftItems.get(pk);
			if (itemList == null || itemList.isEmpty() || goodsList == null || goodsList.isEmpty()) {
				continue;
			}
			Map<String, Object> g1 = new LinkedHashMap<>();
			g1.put("price", priceJsonValue(pk));
			g1.put("gift_item", new ArrayList<>(goodsList));
			giftsOut.add(g1);
			Map<String, Object> g2 = new LinkedHashMap<>();
			g2.put("price", priceJsonValue(pk));
			g2.put("gift_item", new ArrayList<>(itemList));
			giftsItemListsOut.add(g2);
		}
		if (!giftsOut.isEmpty()) {
			result.put("gifts", giftsOut);
			result.put("giftsItemLists", giftsItemListsOut);
		}
	}
}
