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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.distribution.service.DistributorBatchApiRowQueryService;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 前台用户已领券列表：分页查询、卡模板与多语言 enrichment、门店 POI 与展示用 count。
 */
@Service
public class UserDiscountWxappUserCardListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(SHANGHAI);

	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardsMultiLangReadService discountCardsMultiLangReadService;
	private final UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler;
	private final DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;
	private final WxShopsListForUserDiscountService wxShopsListForUserDiscountService;

	public UserDiscountWxappUserCardListService(
			UserDiscountMapper userDiscountMapper,
			DiscountCardsMapper discountCardsMapper,
			DiscountCardsMultiLangReadService discountCardsMultiLangReadService,
			UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler,
			DistributorBatchApiRowQueryService distributorBatchApiRowQueryService,
			WxShopsListForUserDiscountService wxShopsListForUserDiscountService) {
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.discountCardsMultiLangReadService = discountCardsMultiLangReadService;
		this.userDiscountUserRecordDetailAssembler = userDiscountUserRecordDetailAssembler;
		this.distributorBatchApiRowQueryService = distributorBatchApiRowQueryService;
		this.wxShopsListForUserDiscountService = wxShopsListForUserDiscountService;
	}

	public Map<String, Object> build(
			long companyId,
			long userId,
			String mobile,
			int pageNo,
			int pageSize,
			String shopIdRaw,
			String amount,
			String code,
			String cardId,
			String useScenes,
			String usePlatform,
			String status) {
		Objects.requireNonNull(mobile, "mobile");
		int ps = pageSize > 50 ? 50 : (pageSize <= 0 ? 20 : pageSize);
		int pn = pageNo < 1 ? 1 : pageNo;
		int now = (int) (System.currentTimeMillis() / 1000L);

		LambdaQueryWrapper<UserDiscount> wCount = buildFilterWrapper(companyId, userId, now, amount, code, cardId, useScenes, usePlatform, status);
		long cardTotal = userDiscountMapper.selectCount(wCount);

		List<UserDiscount> pageRows;
		if (cardTotal > 0L) {
			LambdaQueryWrapper<UserDiscount> wPage = buildFilterWrapper(companyId, userId, now, amount, code, cardId, useScenes, usePlatform, status);
			Page<UserDiscount> page = new Page<>(pn, ps, false);
			userDiscountMapper.selectPage(page, wPage);
			pageRows = page.getRecords();
		} else {
			pageRows = List.of();
		}

		List<Map<String, Object>> enriched = enrichPageRows(companyId, pageRows);
		return assembleForResponse(companyId, shopIdRaw, pageRows, enriched);
	}

	private LambdaQueryWrapper<UserDiscount> buildFilterWrapper(
			long companyId,
			long userId,
			int now,
			String amount,
			String code,
			String cardId,
			String useScenes,
			String usePlatform,
			String status) {
		LambdaQueryWrapper<UserDiscount> w = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getUserId, userId)
				.gt(UserDiscount::getEndDate, now);
		if (ValuePresence.hasEffectiveValue(amount)) {
			Long amt = tryParseLong(amount.trim());
			if (amt != null) {
				w.le(UserDiscount::getLeastCost, amt)
						.le(UserDiscount::getBeginDate, now)
						.notIn(UserDiscount::getCardType, List.of("gift"));
			}
		}
		if (ValuePresence.hasEffectiveValue(code)) {
			w.eq(UserDiscount::getCode, code.trim());
		}
		if (ValuePresence.hasEffectiveValue(cardId)) {
			Long cid = tryParseLong(cardId.trim());
			if (cid != null && cid > 0L) {
				w.eq(UserDiscount::getCardId, cid);
			}
		}
		if (ValuePresence.hasEffectiveValue(useScenes)) {
			w.eq(UserDiscount::getUseScenes, useScenes.trim());
		}
		if (ValuePresence.hasEffectiveValue(usePlatform)) {
			w.eq(UserDiscount::getUsePlatform, usePlatform.trim());
		}
		if (ValuePresence.hasEffectiveValue(status)) {
			Integer st = tryParseInt(status.trim());
			if (st != null) {
				w.eq(UserDiscount::getStatus, st);
			}
		}
		w.orderByAsc(UserDiscount::getStatus).orderByAsc(UserDiscount::getEndDate);
		return w;
	}

	private static Long tryParseLong(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer tryParseInt(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private record CardLangOverlay(String title, String description) {}

	private Map<Long, CardLangOverlay> loadCardLangOverlays(long companyId, List<UserDiscount> pageRows) {
		Set<Long> cardIds = new LinkedHashSet<>();
		for (UserDiscount ud : pageRows) {
			if (ud.getCardId() != null && ud.getCardId() > 0L) {
				cardIds.add(ud.getCardId());
			}
		}
		if (cardIds.isEmpty()) {
			return Map.of();
		}
		List<DiscountCards> cards = discountCardsMapper.selectList(new LambdaQueryWrapper<DiscountCards>()
				.in(DiscountCards::getCardId, cardIds));
		Map<Long, CardLangOverlay> out = new LinkedHashMap<>();
		for (DiscountCards dc : cards) {
			long cid = dc.getCardId();
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("title", dc.getTitle());
			m.put("description", dc.getDescription());
			discountCardsMultiLangReadService.overlay(companyId, cid, m);
			Object t = m.get("title");
			Object desc = m.get("description");
			out.put(
					cid,
					new CardLangOverlay(
							t != null ? t.toString() : "",
							desc != null ? desc.toString() : ""));
		}
		return out;
	}

	private List<Map<String, Object>> enrichPageRows(long companyId, List<UserDiscount> pageRows) {
		Map<Long, CardLangOverlay> langByCardId = loadCardLangOverlays(companyId, pageRows);
		Set<Long> allDistributorIds = new LinkedHashSet<>();
		List<Map<String, Object>> stage = new ArrayList<>();
		for (UserDiscount ud : pageRows) {
			Map<String, Object> row = userDiscountUserRecordDetailAssembler.toDetailMap(ud);
			long cid = ud.getCardId() != null ? ud.getCardId() : 0L;
			CardLangOverlay lang = langByCardId.get(cid);
			if (lang != null) {
				row.put("title", lang.title());
				row.put("description", lang.description());
			} else {
				row.put("description", "");
			}
			Object relItem = row.get("rel_item_ids");
			boolean useAll = relItem instanceof String s && "all".equalsIgnoreCase(s.trim());
			row.put("use_all_items", useAll);
			collectDistributorIds(row.get("rel_distributor_ids"), allDistributorIds);
			stage.add(row);
		}
		Map<Long, Map<String, Object>> distRows =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(companyId, allDistributorIds);
		List<Map<String, Object>> enriched = new ArrayList<>();
		for (Map<String, Object> row : stage) {
			row.put("distributor_info", buildDistributorInfoList(row.get("rel_distributor_ids"), distRows));
			enriched.add(row);
		}
		return enriched;
	}

	private static void collectDistributorIds(Object relDist, Collection<Long> sink) {
		if (!(relDist instanceof List<?> list)) {
			return;
		}
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			try {
				long id = o instanceof Number n ? n.longValue() : Long.parseLong(o.toString().trim());
				if (id > 0L) {
					sink.add(id);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid id
			}
		}
	}

	private static List<Map<String, Object>> buildDistributorInfoList(Object relDist, Map<Long, Map<String, Object>> byId) {
		List<Map<String, Object>> info = new ArrayList<>();
		if (!(relDist instanceof List<?> list)) {
			return info;
		}
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			long id;
			try {
				id = o instanceof Number n ? n.longValue() : Long.parseLong(o.toString().trim());
			} catch (NumberFormatException e) {
				continue;
			}
			Map<String, Object> row = byId.get(id);
			if (row != null) {
				info.add(row);
			}
		}
		return info;
	}

	private Map<String, Object> assembleForResponse(
			long companyId,
			String shopIdRaw,
			List<UserDiscount> pageRows,
			List<Map<String, Object>> enriched) {
		String shopFilterKey = null;
		if (ValuePresence.hasEffectiveValue(shopIdRaw)) {
			shopFilterKey = String.valueOf(shopIdRaw).trim();
		}
		List<Map<String, Object>> cardDataLists = new ArrayList<>();
		for (Map<String, Object> row : enriched) {
			if (!passesShopFilter(row, shopFilterKey)) {
				continue;
			}
			Map<String, Object> outRow = new LinkedHashMap<>(row);
			putCoupon(outRow);
			formatBeginEndAsYmdShanghai(outRow);
			cardDataLists.add(outRow);
		}

		if (!pageRows.isEmpty()) {
			Map<String, Object> poi = wxShopsListForUserDiscountService.listShopsPoi(companyId, "all");
			Map<Long, String> wxShopIdToStoreName = buildWxShopIdToStoreName(poi);
			int poiTotalCount = intFromObject(poi.get("total_count"));
			for (Map<String, Object> row : cardDataLists) {
				List<String> shopNames = buildStoreNameList(row.get("rel_shops_ids"), wxShopIdToStoreName);
				row.put("storeList", shopNames);
				boolean ifall = shopNames.isEmpty() || shopNames.size() == poiTotalCount;
				row.put("ifall", ifall);
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("list", cardDataLists);
		result.put("count", cardDataLists.size());
		return result;
	}

	private static boolean passesShopFilter(Map<String, Object> row, String shopFilterKey) {
		if (shopFilterKey == null) {
			return true;
		}
		Object shops = row.get("rel_shops_ids");
		if (!(shops instanceof List<?> list)) {
			return true;
		}
		for (Object o : list) {
			if (o != null && shopFilterKey.equals(String.valueOf(o))) {
				return true;
			}
		}
		return false;
	}

	private static void putCoupon(Map<String, Object> cardMap) {
		Map<String, Object> coupon = new LinkedHashMap<>();
		coupon.put("card_id", cardMap.get("card_id"));
		coupon.put("title", cardMap.get("title"));
		coupon.put("code", cardMap.get("code"));
		coupon.put("card_type", cardMap.get("card_type"));
		String ct = cardMap.get("card_type") == null ? "" : String.valueOf(cardMap.get("card_type")).trim();
		if ("cash".equals(ct)) {
			coupon.put("least_cost", cardMap.get("least_cost"));
			coupon.put("reduce_cost", cardMap.get("reduce_cost"));
		} else if ("discount".equals(ct)) {
			Object disc = cardMap.get("discount");
			coupon.put("discount", disc == null ? "" : String.valueOf(disc));
		}
		cardMap.put("coupon", coupon);
	}

	private static void formatBeginEndAsYmdShanghai(Map<String, Object> row) {
		for (String key : new String[] {"begin_date", "end_date"}) {
			Object v = row.get(key);
			if (v instanceof Number n) {
				int sec = n.intValue();
				if (sec > 0) {
					row.put(key, YMD.format(Instant.ofEpochSecond(sec)));
				} else {
					row.put(key, "");
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, String> buildWxShopIdToStoreName(Map<String, Object> poi) {
		Object listObj = poi.get("list");
		Map<Long, String> m = new LinkedHashMap<>();
		if (!(listObj instanceof List<?> list)) {
			return m;
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			Map<String, Object> rm = (Map<String, Object>) row;
			Object wx = rm.get("wxShopId");
			if (wx == null) {
				wx = rm.get("wx_shop_id");
			}
			long id = wx instanceof Number n ? n.longValue() : 0L;
			if (id <= 0L) {
				continue;
			}
			Object name = rm.get("storeName");
			if (name == null) {
				name = rm.get("store_name");
			}
			m.put(id, name != null ? name.toString() : "");
		}
		return m;
	}

	private static List<String> buildStoreNameList(Object relShopsIds, Map<Long, String> wxShopIdToStoreName) {
		List<String> names = new ArrayList<>();
		if (!(relShopsIds instanceof List<?> list)) {
			return names;
		}
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			long id = o instanceof Number n ? n.longValue() : parseLongLenient(o.toString());
			if (id <= 0L) {
				continue;
			}
			String name = wxShopIdToStoreName.get(id);
			if (name != null) {
				names.add(name);
			}
		}
		return names;
	}

	private static long parseLongLenient(String s) {
		if (s == null) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intFromObject(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
