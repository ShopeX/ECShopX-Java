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
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import cn.shopex.ecshopx.promotions.service.sku.SkuMarketingActivityRuleFormatterRegistry;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SkuValidMarketingActivityService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private static final Set<String> GOODS_DETAIL_PLUSITEM_STRIP_KEYS = Set.of(
			"activity_price",
			"commission_ratio",
			"data_source",
			"distributor_name",
			"gross_profit_rate",
			"intro",
			"itemCatName",
			"itemMainCatName",
			"item_cat_id",
			"item_holder",
			"operator_name",
			"promotion_activity",
			"purchase_agreement",
			"supplier_name",
			"tagList");

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MemberAccountService memberAccountService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final ShopMenuService shopMenuService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final SkuMarketingActivityRuleFormatterRegistry skuMarketingActivityRuleFormatterRegistry;

	public SkuValidMarketingActivityService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MemberAccountService memberAccountService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			VipGradeListQueryService vipGradeListQueryService,
			ShopMenuService shopMenuService,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			SkuMarketingActivityRuleFormatterRegistry skuMarketingActivityRuleFormatterRegistry) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.memberAccountService = memberAccountService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.shopMenuService = shopMenuService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.skuMarketingActivityRuleFormatterRegistry = skuMarketingActivityRuleFormatterRegistry;
	}

	public List<Map<String, Object>> getValidMarketingActivityForCartItems(
			long companyId, List<Long> itemIds, long userId, long distributorId) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LinkedHashMap<Long, Map<String, Object>> byId = new LinkedHashMap<>();
		for (Long itemId : itemIds) {
			if (itemId == null || itemId <= 0L) {
				continue;
			}
			List<Map<String, Object>> one =
					getValidMarketingActivityByItemId(companyId, itemId, userId, distributorId);
			if (one == null) {
				continue;
			}
			for (Map<String, Object> act : one) {
				long mid = longParam(act.get("marketing_id"));
				if (mid > 0L) {
					byId.putIfAbsent(mid, act);
				}
			}
		}
		return new ArrayList<>(byId.values());
	}

	@Nullable
	public List<Map<String, Object>> getValidMarketingActivityByItemId(
			long companyId, @Nullable Long itemId, long userId, long distributorId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> aw = new LambdaQueryWrapper<>();
		aw.eq(MarketingActivity::getCompanyId, companyId)
				.le(MarketingActivity::getStartTime, now)
				.ge(MarketingActivity::getEndTime, now);
		List<MarketingActivity> activityList = marketingActivityMapper.selectList(aw);
		if (activityList == null || activityList.isEmpty()) {
			return null;
		}
		List<Long> marketingIdOrder =
				activityList.stream().map(MarketingActivity::getMarketingId).filter(Objects::nonNull).toList();
		List<Long> effectiveMarketingIds;
		if (itemId == null) {
			effectiveMarketingIds = new ArrayList<>(marketingIdOrder);
		} else {
			effectiveMarketingIds =
					marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(companyId, itemId, marketingIdOrder, now);
		}
		Map<Long, Map<String, Object>> relItemArr = loadRelItemMaps(companyId, effectiveMarketingIds, now);
		Set<Long> allMarketingIds = activityList.stream()
				.map(MarketingActivity::getMarketingId)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftGoodsByMid;
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftItemsByMid;
		{
			GiftBuckets buckets = loadGiftBuckets(companyId, allMarketingIds);
			relGiftGoodsByMid = buckets.relGiftGoodsByMid();
			relGiftItemsByMid = buckets.relGiftItemsByMid();
		}
		Long userGrade = resolveUserGrade(userId, companyId);
		LinkedHashMap<String, Map<String, Object>> memberGradeIndex = buildMemberGradeIndex(companyId);
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		List<Map<String, Object>> resultList = new ArrayList<>();
		for (MarketingActivity value : activityList) {
			if ("member_preference".equals(value.getMarketingType())) {
				continue;
			}
			Long mid = value.getMarketingId();
			if (mid == null) {
				continue;
			}
			int useBound = value.getUseBound() != null ? value.getUseBound() : 0;
			if (itemId != null && useBound > 0 && !effectiveMarketingIds.contains(mid)) {
				continue;
			}
			if (useBound > 0 && !relItemArr.containsKey(mid)) {
				continue;
			}
			List<String> shopIds = parseShopIds(value.getShopIds());
			if (distributorId > 0 && !shopIds.isEmpty() && !shopIds.contains("all") && !shopIds.contains(String.valueOf(distributorId))) {
				continue;
			}
			long sourceId = value.getSourceId() != null ? value.getSourceId() : 0L;
			if ("platform".equals(productModel) && distributorId >= 0 && distributorId != sourceId) {
				continue;
			}
			List<?> validGradeDecoded = decodeJsonList(value.getValidGrade());
			if (validGradeDecoded != null
					&& !validGradeDecoded.isEmpty()
					&& userGrade != null
					&& !gradeListContains(validGradeDecoded, userGrade)) {
				continue;
			}
			Map<String, Object> row = buildActivityRow(value, now, userId, relItemArr.get(mid), validGradeDecoded, memberGradeIndex);
			Map<Object, List<Map<String, Object>>> relG = relGiftGoodsByMid.getOrDefault(mid, Map.of());
			Map<Object, List<Map<String, Object>>> relI = relGiftItemsByMid.getOrDefault(mid, Map.of());
			String mt = value.getMarketingType() == null ? "" : value.getMarketingType();
			switch (mt) {
				case "full_gift" -> applyFullGiftGiftsForSkuRow(row, relG, relI);
				case "plus_price_buy" -> applyPlusPriceBuyGiftsForSkuRow(row, relG, relI);
				default -> { }
			}
			resultList.add(row);
		}
		return resultList.isEmpty() ? null : resultList;
	}

	@Nullable
	public List<Map<String, Object>> getValidMarketingActivityByGoodsId(
			long companyId, long goodsId, long userId, long distributorId) {
		if (goodsId <= 0L) {
			return null;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> aw = new LambdaQueryWrapper<>();
		aw.eq(MarketingActivity::getCompanyId, companyId)
				.le(MarketingActivity::getStartTime, now)
				.ge(MarketingActivity::getEndTime, now);
		List<MarketingActivity> activityList = marketingActivityMapper.selectList(aw);
		if (activityList == null || activityList.isEmpty()) {
			return null;
		}
		List<Long> marketingIdOrder =
				activityList.stream().map(MarketingActivity::getMarketingId).filter(Objects::nonNull).toList();
		Map<Long, List<Long>> relItemIdsByMarketing = buildRelItemIdsByMarketing(companyId, goodsId, marketingIdOrder, now);
		List<Long> effectiveMarketingIds = marketingIdOrder.stream()
				.filter(relItemIdsByMarketing::containsKey)
				.toList();
		Map<Long, Map<String, Object>> relItemArr =
				loadRelItemMapsForGoodsDetail(companyId, effectiveMarketingIds, now, relItemIdsByMarketing);
		Set<Long> allMarketingIds = activityList.stream()
				.map(MarketingActivity::getMarketingId)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		GiftBuckets buckets = loadGiftBucketsForGoodsDetail(companyId, allMarketingIds, userId);
		Long userGrade = resolveUserGrade(userId, companyId);
		LinkedHashMap<String, Map<String, Object>> memberGradeIndex = buildMemberGradeIndex(companyId);
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		List<Map<String, Object>> resultList = new ArrayList<>();
		for (MarketingActivity value : activityList) {
			if ("member_preference".equals(value.getMarketingType())) {
				continue;
			}
			Long mid = value.getMarketingId();
			if (mid == null) {
				continue;
			}
			int useBound = value.getUseBound() != null ? value.getUseBound() : 0;
			if (useBound > 0 && !effectiveMarketingIds.contains(mid)) {
				continue;
			}
			if (useBound > 0 && !relItemArr.containsKey(mid)) {
				continue;
			}
			List<String> shopIds = parseShopIds(value.getShopIds());
			if (distributorId > 0 && !shopIds.isEmpty() && !shopIds.contains("all") && !shopIds.contains(String.valueOf(distributorId))) {
				continue;
			}
			long sourceId = value.getSourceId() != null ? value.getSourceId() : 0L;
			if ("platform".equals(productModel) && distributorId >= 0 && distributorId != sourceId) {
				continue;
			}
			List<?> validGradeDecoded = decodeJsonList(value.getValidGrade());
			if (validGradeDecoded != null
					&& !validGradeDecoded.isEmpty()
					&& userGrade != null
					&& !gradeListContains(validGradeDecoded, userGrade)) {
				continue;
			}
			Map<String, Object> row = buildActivityRow(value, now, userId, relItemArr.get(mid), validGradeDecoded, memberGradeIndex);
			applyGoodsDetailActivityWireFormat(row, value);
			Map<Object, List<Map<String, Object>>> relG = buckets.relGiftGoodsByMid().getOrDefault(mid, Map.of());
			Map<Object, List<Map<String, Object>>> relI = buckets.relGiftItemsByMid().getOrDefault(mid, Map.of());
			String mt = value.getMarketingType() == null ? "" : value.getMarketingType();
			if ("full_gift".equals(mt)) {
				applyFullGiftGiftsForGoodsDetailRow(row, relG, relI);
			} else if ("plus_price_buy".equals(mt)) {
				applyPlusPriceBuyGiftsForGoodsDetailRow(row, relG, relI);
			}
			resultList.add(row);
		}
		return resultList.isEmpty() ? null : resultList;
	}

	private Map<Long, List<Long>> buildRelItemIdsByMarketing(
			long companyId, long goodsId, List<Long> marketingIdOrder, int now) {
		List<Long> skuIds = marketingActivityCatalogAccess.listItemIdsByGoodsId(companyId, goodsId);
		if (skuIds == null || skuIds.isEmpty() || marketingIdOrder == null || marketingIdOrder.isEmpty()) {
			return Map.of();
		}
		Map<Long, LinkedHashSet<Long>> acc = new LinkedHashMap<>();
		for (Long skuId : skuIds) {
			if (skuId == null || skuId <= 0L) {
				continue;
			}
			List<Long> mids =
					marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(companyId, skuId, marketingIdOrder, now);
			for (Long mid : mids) {
				if (mid != null) {
					acc.computeIfAbsent(mid, k -> new LinkedHashSet<>()).add(skuId);
				}
			}
		}
		Map<Long, List<Long>> out = new LinkedHashMap<>();
		for (Map.Entry<Long, LinkedHashSet<Long>> en : acc.entrySet()) {
			out.put(en.getKey(), new ArrayList<>(en.getValue()));
		}
		return out;
	}

	private Map<Long, Map<String, Object>> loadRelItemMapsForGoodsDetail(
			long companyId,
			List<Long> marketingIds,
			int now,
			Map<Long, List<Long>> relItemIdsByMarketing) {
		if (marketingIds == null || marketingIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId)
				.in(MarketingActivityItems::getMarketingId, marketingIds)
				.le(MarketingActivityItems::getStartTime, now)
				.ge(MarketingActivityItems::getEndTime, now)
				.orderByAsc(MarketingActivityItems::getMarketingId)
				.orderByAsc(MarketingActivityItems::getItemId)
				.orderByDesc(MarketingActivityItems::getStartTime);
		List<MarketingActivityItems> rows = marketingActivityItemsMapper.selectList(w);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		if (rows == null) {
			return out;
		}
		for (MarketingActivityItems r : rows) {
			Long mid = r.getMarketingId();
			if (mid == null || r.getItemId() == null) {
				continue;
			}
			Map<String, Object> bucket = out.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			Map<String, Object> itemRow = activityItemRowToPhpMap(r);
			List<Long> relIds = relItemIdsByMarketing.get(mid);
			if (relIds == null || relIds.isEmpty()) {
				bucket.put(String.valueOf(r.getItemId()), itemRow);
				continue;
			}
			for (Long relItemId : relIds) {
				if (relItemId != null) {
					bucket.put(String.valueOf(relItemId), itemRow);
				}
			}
		}
		return out;
	}

	private Map<Long, Map<String, Object>> loadRelItemMaps(long companyId, List<Long> marketingIds, int now) {
		if (marketingIds == null || marketingIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId)
				.in(MarketingActivityItems::getMarketingId, marketingIds)
				.le(MarketingActivityItems::getStartTime, now)
				.ge(MarketingActivityItems::getEndTime, now);
		List<MarketingActivityItems> rows = marketingActivityItemsMapper.selectList(w);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		if (rows == null) {
			return out;
		}
		for (MarketingActivityItems r : rows) {
			Long mid = r.getMarketingId();
			if (mid == null || r.getItemId() == null) {
				continue;
			}
			Map<String, Object> bucket = out.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			bucket.put(String.valueOf(r.getItemId()), activityItemRowToMap(r));
		}
		return out;
	}

	private record GiftBuckets(
			Map<Long, Map<Object, List<Map<String, Object>>>> relGiftGoodsByMid,
			Map<Long, Map<Object, List<Map<String, Object>>>> relGiftItemsByMid) {}

	private GiftBuckets loadGiftBuckets(long companyId, Set<Long> marketingIds) {
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftGoodsByMid = new LinkedHashMap<>();
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftItemsByMid = new LinkedHashMap<>();
		if (marketingIds.isEmpty()) {
			return new GiftBuckets(relGiftGoodsByMid, relGiftItemsByMid);
		}
		LambdaQueryWrapper<MarketingGiftItems> giftWrapper = new LambdaQueryWrapper<>();
		giftWrapper.eq(MarketingGiftItems::getCompanyId, companyId).in(MarketingGiftItems::getMarketingId, marketingIds).orderByAsc(MarketingGiftItems::getId);
		List<MarketingGiftItems> giftRows = marketingGiftItemsMapper.selectList(giftWrapper);
		List<Long> giftItemIds =
				giftRows.stream().map(MarketingGiftItems::getItemId).filter(Objects::nonNull).distinct().toList();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList =
				giftItemIds.isEmpty()
						? List.of()
						: (List<Map<String, Object>>) marketingActivityCatalogAccess
								.loadSkuItemsListForMarketingGift(companyId, giftItemIds)
								.get("list");
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : skuList) {
			Object iid = skuRow.get("item_id");
			if (iid instanceof Number n) {
				skuByItemId.putIfAbsent(n.longValue(), skuRow);
			}
		}
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
			if (sku != null && !sku.isEmpty()) {
				addToPriceBucket(relItems, priceKey, new LinkedHashMap<>(sku));
			}
			addToPriceBucket(relGoods, priceKey, giftMap);
		}
		return new GiftBuckets(relGiftGoodsByMid, relGiftItemsByMid);
	}

	private GiftBuckets loadGiftBucketsForGoodsDetail(long companyId, Set<Long> marketingIds, long userId) {
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftGoodsByMid = new LinkedHashMap<>();
		Map<Long, Map<Object, List<Map<String, Object>>>> relGiftItemsByMid = new LinkedHashMap<>();
		if (marketingIds.isEmpty()) {
			return new GiftBuckets(relGiftGoodsByMid, relGiftItemsByMid);
		}
		LambdaQueryWrapper<MarketingGiftItems> giftWrapper = new LambdaQueryWrapper<>();
		giftWrapper.eq(MarketingGiftItems::getCompanyId, companyId).in(MarketingGiftItems::getMarketingId, marketingIds).orderByAsc(MarketingGiftItems::getId);
		List<MarketingGiftItems> giftRows = marketingGiftItemsMapper.selectList(giftWrapper);
		List<Long> giftItemIds =
				giftRows.stream().map(MarketingGiftItems::getItemId).filter(Objects::nonNull).distinct().toList();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList =
				giftItemIds.isEmpty()
						? List.of()
						: (List<Map<String, Object>>) marketingActivityCatalogAccess
								.loadWxappItemListDataForSeckillGetInfo(companyId, userId, giftItemIds, "zh-CN")
								.get("list");
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : skuList) {
			long iid = longParam(skuRow.get("item_id"));
			if (iid > 0L) {
				skuByItemId.putIfAbsent(iid, skuRow);
			}
		}
		for (MarketingGiftItems g : giftRows) {
			Long mid = g.getMarketingId();
			if (mid == null) {
				continue;
			}
			Object priceKey = priceKeyForGrouping(g.getPrice());
			Map<String, Object> giftMap = giftEntityToGoodsDetailGiftMap(g);
			Map<String, Object> sku = g.getItemId() != null ? skuByItemId.get(g.getItemId()) : null;
			Map<Object, List<Map<String, Object>>> relGoods = relGiftGoodsByMid.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			Map<Object, List<Map<String, Object>>> relItems = relGiftItemsByMid.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			addToPriceBucket(relGoods, priceKey, giftMap);
			if (sku != null && !sku.isEmpty()) {
				Map<String, Object> merged = skuRowPhpWireMap(new LinkedHashMap<>(sku));
				merged.put("gift", new LinkedHashMap<>(giftMap));
				addToPriceBucket(relItems, priceKey, merged);
			}
		}
		return new GiftBuckets(relGiftGoodsByMid, relGiftItemsByMid);
	}

	private Map<String, Object> buildActivityRow(
			MarketingActivity value,
			int now,
			long userId,
			@Nullable Map<String, Object> itemsMap,
			@Nullable List<?> validGradeDecoded,
			LinkedHashMap<String, Map<String, Object>> memberGradeIndex) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("marketing_id", String.valueOf(value.getMarketingId()));
		row.put("marketing_type", value.getMarketingType());
		row.put("marketing_name", value.getMarketingName());
		row.put("marketing_desc", value.getMarketingDesc());
		int st = value.getStartTime() != null ? value.getStartTime() : 0;
		int et = value.getEndTime() != null ? value.getEndTime() : 0;
		row.put("start_time", st);
		row.put("end_time", et);
		row.put("release_time", releaseTimePayload(value.getReleaseTime()));
		row.put("used_platform", value.getUsedPlatform());
		row.put("use_bound", value.getUseBound());
		row.put("use_shop", value.getUseShop());
		row.put("shop_ids", parseShopIds(value.getShopIds()));
		row.put("valid_grade", validGradeDecoded != null ? validGradeDecoded : List.of());
		row.put("condition_type", value.getConditionType());
		Object conditionValueDecoded = decodeConditionValue(value.getConditionValue());
		row.put("condition_value", conditionValueDecoded);
		row.put("canjoin_repeat", value.getCanjoinRepeat());
		row.put("join_limit", value.getJoinLimit());
		row.put("free_postage", value.getFreePostage());
		row.put("promotion_tag", value.getPromotionTag());
		row.put("check_status", value.getCheckStatus());
		row.put("reason", value.getReason());
		row.put("item_type", value.getItemType());
		row.put("is_increase_purchase", value.getIsIncreasePurchase());
		row.put("ad_pic", value.getAdPic());
		row.put("tag_ids", decodeJsonList(value.getTagIds()));
		row.put("brand_ids", decodeJsonList(value.getBrandIds()));
		row.put("in_proportion", value.getInProportion());
		row.put("rel_marketing_id", String.valueOf(value.getRelMarketingId() != null ? value.getRelMarketingId() : 0L));
		row.put("activity_background", value.getActivityBackground());
		row.put("navbar_color", value.getNavbarColor());
		row.put("timeBackgroundColor", value.getTimeBackgroundColor());
		row.put("company_id", String.valueOf(value.getCompanyId()));
		row.put("created", value.getCreated());
		row.put("updated", value.getUpdated());
		row.put("source_type", value.getSourceType());
		row.put("source_id", String.valueOf(value.getSourceId() != null ? value.getSourceId() : 0L));
		if (itemsMap == null || itemsMap.isEmpty()) {
			row.put("items", List.of());
		} else {
			row.put("items", new LinkedHashMap<>(itemsMap));
		}
		row.put("member_grade", buildMemberGradeNameList(validGradeDecoded, memberGradeIndex));
		row.put(
				"condition_rules",
				skuMarketingActivityRuleFormatterRegistry.formatRules(
						value.getMarketingType(), value.getConditionType(), conditionValueDecoded));
		row.put("start_date", formatEpoch(st));
		row.put("end_date", formatEpoch(et));
		if (now >= nz(et)) {
			row.put("status", "end");
		} else if (now >= nz(st) && now < nz(et)) {
			row.put("status", "ongoing");
			row.put("last_seconds", Math.max(0, nz(et) - now));
		} else {
			row.put("status", "waiting");
		}
		if (userId > 0) {
			String key = "MarketingUserJoinNum:" + value.getCompanyId() + ":" + value.getMarketingId();
			Object hv = stringRedisTemplate.opsForHash().get(key, "user_" + userId);
			int used = 0;
			if (hv != null && StringUtils.hasText(hv.toString())) {
				try {
					used = Integer.parseInt(hv.toString().trim());
				} catch (NumberFormatException ignored) {
				}
			}
			row.put("usedCount", used);
		}
		return row;
	}

	private static Object releaseTimePayload(Integer rt) {
		if (rt == null || rt == 0) {
			return null;
		}
		return rt;
	}

	private static int nz(Integer v) {
		return v != null ? v : 0;
	}

	private static String formatEpoch(int epoch) {
		if (epoch <= 0) {
			return "";
		}
		return DT_FMT.format(Instant.ofEpochSecond(epoch));
	}

	private static List<String> parseShopIds(String raw) {
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return List.of("all");
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(s -> StringUtils.hasText(s) && !"0".equals(s))
				.collect(Collectors.toList());
	}

	private Object decodeConditionValue(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json.trim(), Object.class);
		} catch (Exception e) {
			return List.of();
		}
	}

	private List<?> decodeJsonList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private static boolean gradeListContains(List<?> validGrade, long userGrade) {
		for (Object o : validGrade) {
			if (o instanceof Number n && n.longValue() == userGrade) {
				return true;
			}
			if (o != null) {
				String s = o.toString().trim();
				if (s.equals(String.valueOf(userGrade))) {
					return true;
				}
				try {
					if (Long.parseLong(s) == userGrade) {
						return true;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return false;
	}

	private Long resolveUserGrade(long userId, long companyId) {
		if (userId <= 0) {
			return null;
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object g = info.get("grade_id");
		if (g instanceof Number n) {
			return n.longValue();
		}
		if (g != null && StringUtils.hasText(g.toString())) {
			try {
				return Long.parseLong(g.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	private LinkedHashMap<String, Map<String, Object>> buildMemberGradeIndex(long companyId) {
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
		return memberGrade;
	}

	private static List<String> buildMemberGradeNameList(
			@Nullable List<?> validGrade, LinkedHashMap<String, Map<String, Object>> memberGrade) {
		if (validGrade == null || validGrade.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object keyObj : validGrade) {
			String keyStr = keyObj == null ? "" : String.valueOf(keyObj);
			Map<String, Object> hit = memberGrade.get(keyStr);
			if (hit != null && hit.get("grade_name") != null) {
				out.add(String.valueOf(hit.get("grade_name")));
			}
		}
		return out;
	}

	private static Map<String, Object> activityItemRowToMap(MarketingActivityItems r) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (r.getId() != null) {
			m.put("id", r.getId());
		}
		if (r.getMarketingId() != null) {
			m.put("marketing_id", r.getMarketingId());
		}
		if (r.getCompanyId() != null) {
			m.put("company_id", r.getCompanyId());
		}
		if (r.getItemId() != null) {
			m.put("item_id", r.getItemId());
		}
		if (r.getGoodsId() != null) {
			m.put("goods_id", r.getGoodsId());
		}
		m.put("is_show", r.getIsShow());
		m.put("item_spec_desc", r.getItemSpecDesc());
		m.put("marketing_type", r.getMarketingType());
		m.put("item_type", r.getItemType());
		m.put("item_name", r.getItemName());
		m.put("price", r.getPrice());
		m.put("item_brief", r.getItemBrief());
		m.put("pics", r.getPics());
		m.put("promotion_tag", r.getPromotionTag());
		m.put("start_time", r.getStartTime());
		m.put("end_time", r.getEndTime());
		m.put("status", r.getStatus());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
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

	private static void addToPriceBucket(Map<Object, List<Map<String, Object>>> byPrice, Object key, Map<String, Object> row) {
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
		m.put("price", g.getPrice() != null ? g.getPrice() : 0);
		m.put("store", g.getStore() != null ? g.getStore() : 0);
		m.put("gift_num", g.getGiftNum());
		m.put("pics", decodePicsForGiftRow(g.getPics()));
		m.put("without_return", g.getWithoutReturn());
		m.put("condition_type", g.getConditionType());
		m.put("filter_full", formatFilterFullForGiftRow(g));
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

	private static String formatFilterFullForGiftRow(MarketingGiftItems g) {
		Integer fv = g.getFilterFull();
		if ("totalfee".equals(g.getConditionType())) {
			int cents = fv != null ? fv : 0;
			return BigDecimal.valueOf(cents)
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
					.toPlainString();
		}
		return String.valueOf(fv != null ? fv : "");
	}

	private static void applyFullGiftGiftsForSkuRow(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		List<Map<String, Object>> items = relGiftItems.get(0);
		if (items == null || items.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> giftByItemId = new LinkedHashMap<>();
		List<Map<String, Object>> giftRows = relGiftGoods.get(0);
		if (giftRows != null) {
			for (Map<String, Object> gift : giftRows) {
				long itemId = longParam(gift.get("item_id"));
				if (itemId > 0L) {
					giftByItemId.put(itemId, gift);
				}
			}
		}
		List<Map<String, Object>> giftsOut = new ArrayList<>();
		for (Map<String, Object> item : items) {
			Map<String, Object> row = new LinkedHashMap<>(item);
			Map<String, Object> gift = giftByItemId.get(longParam(item.get("item_id")));
			if (gift != null) {
				row.put("gift", gift);
			}
			giftsOut.add(row);
		}
		result.put("gifts", giftsOut);
	}

	private static void applyPlusPriceBuyGiftsForSkuRow(
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
			result.put("plusitems", new ArrayList<>(giftsOut));
		}
	}

	private static long longParam(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void applyGoodsDetailActivityWireFormat(Map<String, Object> row, MarketingActivity value) {
		row.put("tag_ids", StringUtils.hasText(value.getTagIds()) ? value.getTagIds().trim() : "[]");
		row.put("brand_ids", StringUtils.hasText(value.getBrandIds()) ? value.getBrandIds().trim() : "[]");
		row.put("start_time", scalarToPhpString(row.get("start_time")));
		row.put("end_time", scalarToPhpString(row.get("end_time")));
		row.put("used_platform", scalarToPhpString(row.get("used_platform")));
		row.put("use_bound", scalarToPhpString(row.get("use_bound")));
		row.put("use_shop", scalarToPhpString(row.get("use_shop")));
		row.put("canjoin_repeat", boolToPhpZeroOne(row.get("canjoin_repeat")));
		row.put("free_postage", boolToPhpZeroOne(row.get("free_postage")));
		row.put("in_proportion", boolToPhpZeroOne(row.get("in_proportion")));
		row.put("join_limit", scalarToPhpString(row.get("join_limit")));
		row.put("created", scalarToPhpString(row.get("created")));
		row.put("updated", scalarToPhpString(row.get("updated")));
		Object items = row.get("items");
		if (items instanceof Map<?, ?> itemsMap) {
			Map<String, Object> wired = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : itemsMap.entrySet()) {
				if (en.getValue() instanceof Map<?, ?> itemRow) {
					@SuppressWarnings("unchecked")
					Map<String, Object> cast = (Map<String, Object>) itemRow;
					wired.put(String.valueOf(en.getKey()), new LinkedHashMap<>(cast));
				}
			}
			row.put("items", wired);
		} else if (items instanceof List<?> list && list.isEmpty()) {
			row.put("items", new LinkedHashMap<>());
		}
	}

	private static Map<String, Object> activityItemRowToPhpMap(MarketingActivityItems r) {
		Map<String, Object> m = new LinkedHashMap<>();
		putPhpField(m, "id", r.getId());
		putPhpField(m, "marketing_id", r.getMarketingId());
		putPhpField(m, "item_id", r.getItemId());
		putPhpField(m, "goods_id", r.getGoodsId());
		putPhpField(m, "is_show", r.getIsShow());
		putPhpField(m, "item_spec_desc", r.getItemSpecDesc());
		putPhpField(m, "marketing_type", r.getMarketingType());
		putPhpField(m, "item_type", r.getItemType());
		putPhpField(m, "item_name", r.getItemName());
		putPhpField(m, "price", r.getPrice());
		m.put("item_brief", r.getItemBrief());
		putPhpField(m, "pics", r.getPics());
		putPhpField(m, "promotion_tag", r.getPromotionTag());
		putPhpField(m, "start_time", r.getStartTime());
		putPhpField(m, "end_time", r.getEndTime());
		putPhpField(m, "status", r.getStatus());
		putPhpField(m, "created", r.getCreated());
		putPhpField(m, "updated", r.getUpdated());
		if (r.getCompanyId() != null) {
			putPhpField(m, "company_id", r.getCompanyId());
		}
		return m;
	}

	private Map<String, Object> giftEntityToGoodsDetailGiftMap(MarketingGiftItems g) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (g.getId() != null) {
			m.put("id", String.valueOf(g.getId()));
		}
		if (g.getMarketingId() != null) {
			m.put("marketing_id", String.valueOf(g.getMarketingId()));
		}
		if (g.getItemId() != null) {
			m.put("item_id", String.valueOf(g.getItemId()));
		}
		m.put("item_type", g.getItemType());
		m.put("item_name", g.getItemName());
		m.put("price", g.getPrice() != null ? g.getPrice() : 0);
		m.put("store", g.getStore() != null ? g.getStore() : 0);
		m.put("gift_num", g.getGiftNum() != null ? g.getGiftNum() : 0);
		m.put("pics", decodePicsForGiftRow(g.getPics()));
		m.put("without_return", Boolean.TRUE.equals(g.getWithoutReturn()));
		m.put("condition_type", g.getConditionType());
		m.put("filter_full", formatFilterFullForGiftRow(g));
		m.put("item_spec_desc", g.getItemSpecDesc());
		if (g.getCreated() != null) {
			m.put("created", g.getCreated());
		}
		if (g.getUpdated() != null) {
			m.put("updated", g.getUpdated());
		}
		if (g.getCompanyId() != null) {
			m.put("company_id", String.valueOf(g.getCompanyId()));
		}
		return m;
	}

	private Map<String, Object> giftEntityToPhpMap(MarketingGiftItems g) {
		Map<String, Object> m = new LinkedHashMap<>();
		putPhpField(m, "id", g.getId());
		putPhpField(m, "marketing_id", g.getMarketingId());
		putPhpField(m, "company_id", g.getCompanyId());
		putPhpField(m, "item_id", g.getItemId());
		putPhpField(m, "item_type", g.getItemType());
		putPhpField(m, "item_name", g.getItemName());
		m.put("price", g.getPrice() != null ? g.getPrice() : 0);
		m.put("store", g.getStore() != null ? g.getStore() : 0);
		putPhpField(m, "gift_num", g.getGiftNum());
		m.put("pics", decodePicsForGiftRow(g.getPics()));
		m.put("without_return", Boolean.TRUE.equals(g.getWithoutReturn()));
		putPhpField(m, "condition_type", g.getConditionType());
		m.put("filter_full", formatFilterFullForGiftRow(g));
		putPhpField(m, "item_spec_desc", g.getItemSpecDesc());
		putPhpField(m, "created", g.getCreated());
		putPhpField(m, "updated", g.getUpdated());
		return m;
	}

	private static void applyPlusPriceBuyGiftsForGoodsDetailRow(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		result.put("gifts", List.of());
		if (relGiftItems.isEmpty()) {
			return;
		}
		List<Map<String, Object>> plusitems = new ArrayList<>();
		for (List<Map<String, Object>> tierItems : relGiftItems.values()) {
			if (tierItems == null) {
				continue;
			}
			for (Map<String, Object> sku : tierItems) {
				Map<String, Object> row = skuRowPhpWireMap(new LinkedHashMap<>(sku));
				if (!row.containsKey("gift")) {
					long itemId = longParam(sku.get("item_id"));
					Map<String, Object> gift = findGiftForSku(relGiftGoods, itemId);
					if (gift != null) {
						row.put("gift", new LinkedHashMap<>(gift));
					}
				}
				stripGoodsDetailPlusitemFields(row);
				plusitems.add(row);
			}
		}
		if (!plusitems.isEmpty()) {
			result.put("plusitems", plusitems);
		}
	}

	private static void stripGoodsDetailPlusitemFields(Map<String, Object> row) {
		for (String key : GOODS_DETAIL_PLUSITEM_STRIP_KEYS) {
			row.remove(key);
		}
	}

	private static void applyFullGiftGiftsForGoodsDetailRow(
			Map<String, Object> result,
			Map<Object, List<Map<String, Object>>> relGiftGoods,
			Map<Object, List<Map<String, Object>>> relGiftItems) {
		List<Map<String, Object>> items = relGiftItems.get(0);
		if (items == null || items.isEmpty()) {
			result.put("gifts", List.of());
			return;
		}
		Map<Long, Map<String, Object>> giftByItemId = new LinkedHashMap<>();
		List<Map<String, Object>> giftRows = relGiftGoods.get(0);
		if (giftRows != null) {
			for (Map<String, Object> gift : giftRows) {
				long itemId = longParam(gift.get("item_id"));
				if (itemId > 0L) {
					giftByItemId.put(itemId, gift);
				}
			}
		}
		List<Map<String, Object>> giftsOut = new ArrayList<>();
		for (Map<String, Object> item : items) {
			Map<String, Object> row = deepPhpWireMap(new LinkedHashMap<>(item));
			Map<String, Object> gift = giftByItemId.get(longParam(item.get("item_id")));
			if (gift != null) {
				row.put("gift", deepPhpWireMap(new LinkedHashMap<>(gift)));
			}
			giftsOut.add(row);
		}
		result.put("gifts", giftsOut);
	}

	private static Map<String, Object> findGiftForSku(Map<Object, List<Map<String, Object>>> relGiftGoods, long itemId) {
		if (itemId <= 0L) {
			return null;
		}
		for (List<Map<String, Object>> gifts : relGiftGoods.values()) {
			if (gifts == null) {
				continue;
			}
			for (Map<String, Object> g : gifts) {
				if (longParam(g.get("item_id")) == itemId) {
					return g;
				}
			}
		}
		return null;
	}

	private static Map<String, Object> skuRowPhpWireMap(Map<String, Object> source) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> en : source.entrySet()) {
			String key = en.getKey();
			Object value = en.getValue();
			if ("nospec".equals(key)) {
				out.put(key, value);
				continue;
			}
			if ("gift".equals(key) && value instanceof Map<?, ?> giftMap) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cast = (Map<String, Object>) giftMap;
				out.put(key, new LinkedHashMap<>(cast));
				continue;
			}
			out.put(key, skuRowPhpWireValue(value));
		}
		return out;
	}

	private static Object skuRowPhpWireValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> nested = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : map.entrySet()) {
				nested.put(String.valueOf(en.getKey()), skuRowPhpWireValue(en.getValue()));
			}
			return nested;
		}
		if (value instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object el : list) {
				out.add(skuRowPhpWireValue(el));
			}
			return out;
		}
		if (value instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (value instanceof Number n) {
			return scalarToPhpString(n);
		}
		return value;
	}

	private static Map<String, Object> deepPhpWireMap(Map<String, Object> source) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> en : source.entrySet()) {
			out.put(en.getKey(), phpWireValue(en.getValue()));
		}
		return out;
	}

	private static Object phpWireValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> nested = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : map.entrySet()) {
				nested.put(String.valueOf(en.getKey()), phpWireValue(en.getValue()));
			}
			return nested;
		}
		if (value instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object el : list) {
				out.add(phpWireValue(el));
			}
			return out;
		}
		if (value instanceof Boolean b) {
			return b;
		}
		if (value instanceof Number n) {
			return scalarToPhpString(n);
		}
		return value;
	}

	private static void putPhpField(Map<String, Object> target, String key, Object value) {
		if (value == null) {
			return;
		}
		if (value instanceof Boolean b) {
			target.put(key, b ? "1" : "0");
			return;
		}
		target.put(key, scalarToPhpString(value));
	}

	private static String scalarToPhpString(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Boolean b) {
			return b ? "1" : "0";
		}
		return value.toString();
	}

	private static Map<String, Object> stripNullValues(Map<String, Object> source) {
		source.entrySet().removeIf(e -> e.getValue() == null);
		return source;
	}

	private static String boolToPhpZeroOne(Object value) {
		if (value instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (value instanceof Number n) {
			return n.intValue() != 0 ? "1" : "0";
		}
		if (value == null) {
			return "0";
		}
		String s = value.toString().trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return "0";
		}
		return "1";
	}
}
