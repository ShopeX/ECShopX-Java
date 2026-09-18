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
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityApplyRulesService {

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MemberAccountService memberAccountService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public MarketingActivityApplyRulesService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MemberAccountService memberAccountService,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.memberAccountService = memberAccountService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> applyActivityRules(
			long companyId, long marketingId, long userId, Map<String, Object> cartParams) {
		long totalPrice = longParam(cartParams.get("total_price"));
		if (totalPrice <= 0L) {
			return Map.of();
		}
		MarketingActivity detail =
				marketingActivityMapper.selectOne(
						new LambdaQueryWrapper<MarketingActivity>()
								.eq(MarketingActivity::getCompanyId, companyId)
								.eq(MarketingActivity::getMarketingId, marketingId));
		if (detail == null || !isOngoing(detail)) {
			return Map.of();
		}
		if (!memberGradeAllowed(detail, userId, companyId)) {
			return Map.of();
		}
		int joinLimit = detail.getJoinLimit() != null ? detail.getJoinLimit() : 0;
		if (joinLimit > 0) {
			int used = marketingJoinCount(companyId, marketingId, userId);
			if (used > 0 && used >= joinLimit) {
				return Map.of();
			}
		}
		String mt = detail.getMarketingType() == null ? "" : detail.getMarketingType();
		String ct = detail.getConditionType() == null ? "totalfee" : detail.getConditionType();
		List<Map<String, Object>> rules = parseConditionRules(detail.getConditionValue());
		Map<String, Object> activityRow = activityToRuleMap(detail, rules);
		return switch (mt) {
			case "full_discount" -> applyFullDiscount(ct, activityRow, cartParams, totalPrice);
			case "full_minus" -> applyFullMinus(ct, activityRow, cartParams, totalPrice);
			case "full_gift" -> applyFullGift(ct, activityRow, cartParams, totalPrice, companyId, detail);
			case "plus_price_buy" -> applyPlusPriceBuy(ct, activityRow, cartParams, totalPrice, companyId);
			default -> Map.of();
		};
	}

	private Map<String, Object> applyPlusPriceBuy(
			String conditionType,
			Map<String, Object> activity,
			Map<String, Object> cartParams,
			long totalPrice,
			long companyId) {
		if ("quantity".equals(conditionType)) {
			long totalNum = longParam(cartParams.get("total_num"));
			if (totalNum <= 0L) {
				return Map.of();
			}
			return applyPlusPriceBuyQuantity(activity, totalNum, companyId);
		}
		return applyPlusPriceBuyTotalfee(activity, totalPrice, companyId);
	}

	private Map<String, Object> applyPlusPriceBuyTotalfee(
			Map<String, Object> activity, long totalFee, long companyId) {
		List<PlusPriceTier> tiers = plusPriceTiersFromActivity(activity, true);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		long marketingId = longParam(activity.get("marketing_id"));
		String discountDesc = "";
		String plusPriceYuan = "0";
		Map<String, Object> plusBuyItems = Map.of();
		int last = tiers.size() - 1;
		if (totalFee >= tiers.get(last).threshold) {
			PlusPriceTier t = tiers.get(last);
			discountDesc =
					"消费满" + yuanLabel(t.threshold) + "元，加价" + t.plusPriceYuan + "元换购商品";
			plusPriceYuan = t.plusPriceYuan;
			plusBuyItems = loadPlusBuyItemsForActivity(companyId, marketingId);
		} else if (totalFee < tiers.get(0).threshold) {
			plusBuyItems = Map.of();
		} else {
			for (int i = 0; i < last; i++) {
				PlusPriceTier t = tiers.get(i);
				PlusPriceTier next = tiers.get(i + 1);
				if (totalFee >= t.threshold && totalFee < next.threshold) {
					discountDesc =
							"消费满" + yuanLabel(t.threshold) + "元，加价" + t.plusPriceYuan + "元换购商品";
					plusPriceYuan = t.plusPriceYuan;
					plusBuyItems = loadPlusBuyItemsForActivity(companyId, marketingId);
					break;
				}
			}
		}
		return buildPlusPriceBuyResult(activity, marketingId, discountDesc, plusPriceYuan, plusBuyItems);
	}

	private Map<String, Object> applyPlusPriceBuyQuantity(
			Map<String, Object> activity, long totalNum, long companyId) {
		List<PlusPriceTier> tiers = plusPriceTiersFromActivity(activity, false);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		long marketingId = longParam(activity.get("marketing_id"));
		String discountDesc = "";
		String plusPriceYuan = "0";
		Map<String, Object> plusBuyItems = Map.of();
		int last = tiers.size() - 1;
		if (totalNum >= tiers.get(last).threshold) {
			PlusPriceTier t = tiers.get(last);
			discountDesc = "消费满" + t.threshold + "件，加价" + t.plusPriceYuan + "元换购商品";
			plusPriceYuan = t.plusPriceYuan;
			plusBuyItems = loadPlusBuyItemsForActivity(companyId, marketingId);
		} else if (totalNum < tiers.get(0).threshold) {
			plusBuyItems = Map.of();
		} else {
			for (int i = 0; i < last; i++) {
				PlusPriceTier t = tiers.get(i);
				PlusPriceTier next = tiers.get(i + 1);
				if (totalNum >= t.threshold && totalNum < next.threshold) {
					discountDesc = "消费满" + t.threshold + "件，加价" + t.plusPriceYuan + "元换购商品";
					plusPriceYuan = t.plusPriceYuan;
					plusBuyItems = loadPlusBuyItemsForActivity(companyId, marketingId);
					break;
				}
			}
		}
		return buildPlusPriceBuyResult(activity, marketingId, discountDesc, plusPriceYuan, plusBuyItems);
	}

	private Map<String, Object> buildPlusPriceBuyResult(
			Map<String, Object> activity,
			long marketingId,
			String discountDesc,
			String plusPriceYuan,
			Map<String, Object> plusBuyItems) {
		long activityId = plusBuyItems.isEmpty() ? 0L : marketingId;
		int joinLimit = intParam(activity.get("join_limit"));
		Map<String, Object> discountInfo = new LinkedHashMap<>();
		discountInfo.put("type", "plus_price_buy");
		discountInfo.put("id", marketingId);
		discountInfo.put("rule", discountDesc);
		discountInfo.put("info", activity.get("marketing_name"));
		discountInfo.put("discount_fee", 0);
		discountInfo.put("max_limit", joinLimit);
		discountInfo.put("plus_price", plusPriceYuan);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("discount_desc", discountInfo);
		result.put("activity_id", activityId);
		result.put("plus_buy_items", plusBuyItems);
		return result;
	}

	private Map<String, Object> loadPlusBuyItemsForActivity(long companyId, long marketingId) {
		List<MarketingGiftItems> plusRows =
				marketingGiftItemsMapper.selectList(
						new LambdaQueryWrapper<MarketingGiftItems>()
								.eq(MarketingGiftItems::getCompanyId, companyId)
								.eq(MarketingGiftItems::getMarketingId, marketingId)
								.orderByAsc(MarketingGiftItems::getId));
		if (plusRows.isEmpty()) {
			return Map.of();
		}
		List<Long> itemIds =
				plusRows.stream().map(MarketingGiftItems::getItemId).filter(Objects::nonNull).distinct().toList();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList =
				itemIds.isEmpty()
						? List.of()
						: (List<Map<String, Object>>)
								marketingActivityCatalogAccess
										.loadSkuItemsListForMarketingGift(companyId, itemIds)
										.get("list");
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : skuList) {
			long iid = longParam(skuRow.get("item_id"));
			if (iid > 0L) {
				skuByItemId.put(iid, skuRow);
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (MarketingGiftItems g : plusRows) {
			Long itemId = g.getItemId();
			if (itemId == null) {
				continue;
			}
			Map<String, Object> sku = skuByItemId.get(itemId);
			if (sku == null || sku.isEmpty()) {
				continue;
			}
			Map<String, Object> merged = new LinkedHashMap<>(giftEntityToMap(g));
			merged.put("plus_price", g.getPrice() != null ? g.getPrice() : 0);
			merged.putAll(sku);
			out.put(String.valueOf(itemId), merged);
		}
		return out;
	}

	private static List<PlusPriceTier> plusPriceTiersFromActivity(Map<String, Object> activity, boolean totalfee) {
		Object raw = activity.get("condition_value");
		List<Map<String, Object>> rules = new ArrayList<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					rules.add(row);
				}
			}
		}
		List<PlusPriceTier> tiers = new ArrayList<>();
		for (Map<String, Object> rule : rules) {
			long full = parseFullThreshold(rule.get("full"), totalfee);
			String plusPriceYuan =
					rule.get("price") == null ? "0" : rule.get("price").toString().trim();
			tiers.add(new PlusPriceTier(full, plusPriceYuan));
		}
		tiers.sort((a, b) -> Long.compare(a.threshold, b.threshold));
		return tiers;
	}

	private Map<String, Object> applyFullDiscount(
			String conditionType, Map<String, Object> activity, Map<String, Object> cartParams, long totalPrice) {
		if ("quantity".equals(conditionType)) {
			long totalNum = longParam(cartParams.get("total_num"));
			if (totalNum <= 0L) {
				return Map.of();
			}
			return applyFullDiscountQuantity(activity, totalNum, totalPrice);
		}
		return applyFullDiscountTotalfee(activity, totalPrice);
	}

	private Map<String, Object> applyFullMinus(
			String conditionType, Map<String, Object> activity, Map<String, Object> cartParams, long totalPrice) {
		if ("quantity".equals(conditionType)) {
			long totalNum = longParam(cartParams.get("total_num"));
			if (totalNum <= 0L) {
				return Map.of();
			}
			return applyFullMinusQuantity(activity, totalNum, totalPrice);
		}
		return applyFullMinusTotalfee(activity, totalPrice);
	}

	private Map<String, Object> applyFullGift(
			String conditionType,
			Map<String, Object> activity,
			Map<String, Object> cartParams,
			long totalPrice,
			long companyId,
			MarketingActivity detail) {
		if ("quantity".equals(conditionType)) {
			long totalNum = longParam(cartParams.get("total_num"));
			if (totalNum <= 0L) {
				return Map.of();
			}
			return applyFullGiftQuantity(activity, totalNum, totalPrice, companyId, detail);
		}
		return applyFullGiftTotalfee(activity, totalPrice, companyId, detail);
	}

	private Map<String, Object> applyFullGiftTotalfee(
			Map<String, Object> activity, long totalFee, long companyId, MarketingActivity detail) {
		List<Tier> tiers = tiersFromActivity(activity, true);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		long marketingId = longParam(activity.get("marketing_id"));
		List<Map<String, Object>> giftItems = List.of();
		long matchedThreshold = 0L;
		for (int i = tiers.size() - 1; i >= 0; i--) {
			Tier t = tiers.get(i);
			if (totalFee >= t.thresholdFen) {
				matchedThreshold = t.thresholdFen;
				giftItems = loadGiftItemsForTier(companyId, marketingId, (int) matchedThreshold);
				if (intParam(activity.get("in_proportion")) == 1 && matchedThreshold > 0L && !giftItems.isEmpty()) {
					long multiple = totalFee / matchedThreshold;
					multiplyGiftNums(giftItems, multiple);
				}
				break;
			}
		}
		return buildFullGiftResult(activity, detail, giftItems, matchedThreshold, true);
	}

	private Map<String, Object> applyFullGiftQuantity(
			Map<String, Object> activity, long totalNum, long totalFee, long companyId, MarketingActivity detail) {
		List<Tier> tiers = tiersFromActivity(activity, false);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		long marketingId = longParam(activity.get("marketing_id"));
		List<Map<String, Object>> giftItems = List.of();
		long matchedThreshold = 0L;
		for (int i = tiers.size() - 1; i >= 0; i--) {
			Tier t = tiers.get(i);
			if (totalNum >= t.thresholdFen) {
				matchedThreshold = t.thresholdFen;
				giftItems = loadGiftItemsForTier(companyId, marketingId, (int) matchedThreshold);
				if (intParam(activity.get("in_proportion")) == 1 && matchedThreshold > 0L && !giftItems.isEmpty()) {
					long multiple = totalNum / matchedThreshold;
					multiplyGiftNums(giftItems, multiple);
				}
				break;
			}
		}
		return buildFullGiftResult(activity, detail, giftItems, matchedThreshold, false);
	}

	private Map<String, Object> buildFullGiftResult(
			Map<String, Object> activity,
			MarketingActivity detail,
			List<Map<String, Object>> giftItems,
			long matchedThreshold,
			boolean totalfee) {
		long marketingId = longParam(activity.get("marketing_id"));
		long activityId = giftItems.isEmpty() ? 0L : marketingId;
		long totalDiscountFee = 0L;
		for (Map<String, Object> gift : giftItems) {
			totalDiscountFee += longParam(gift.get("price"));
		}
		String discountDesc = buildFullGiftRuleDesc(giftItems, matchedThreshold, totalfee);
		int joinLimit = detail.getJoinLimit() != null ? detail.getJoinLimit() : 0;
		long maxLimit = joinLimit == 0 ? Long.MAX_VALUE : joinLimit;
		Map<String, Object> discountInfo = new LinkedHashMap<>();
		discountInfo.put("type", "full_gift");
		discountInfo.put("id", marketingId);
		discountInfo.put("rule", discountDesc);
		discountInfo.put("info", activity.get("marketing_name"));
		discountInfo.put("discount_fee", totalDiscountFee);
		discountInfo.put("max_limit", maxLimit);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("discount_desc", discountInfo);
		result.put("activity_id", activityId);
		result.put("gifts", giftItems);
		result.put("discount_fee", totalDiscountFee);
		return result;
	}

	private static String buildFullGiftRuleDesc(
			List<Map<String, Object>> giftItems, long matchedThreshold, boolean totalfee) {
		if (giftItems.isEmpty() || matchedThreshold <= 0L) {
			return "";
		}
		String prefix =
				totalfee
						? "消费满" + yuanLabel(matchedThreshold) + "元，送赠品："
						: "消费满" + matchedThreshold + "件，送赠品：";
		StringBuilder sb = new StringBuilder(prefix);
		for (Map<String, Object> gift : giftItems) {
			sb.append(stringVal(gift.get("item_name")))
					.append(" x ")
					.append(intParam(gift.get("gift_num")))
					.append("；");
		}
		return sb.toString();
	}

	private List<Map<String, Object>> loadGiftItemsForTier(long companyId, long marketingId, int filterFull) {
		List<MarketingGiftItems> giftRows =
				marketingGiftItemsMapper.selectList(
						new LambdaQueryWrapper<MarketingGiftItems>()
								.eq(MarketingGiftItems::getCompanyId, companyId)
								.eq(MarketingGiftItems::getMarketingId, marketingId)
								.eq(MarketingGiftItems::getFilterFull, filterFull)
								.orderByAsc(MarketingGiftItems::getId));
		if (giftRows.isEmpty()) {
			return List.of();
		}
		List<Long> itemIds =
				giftRows.stream().map(MarketingGiftItems::getItemId).filter(Objects::nonNull).distinct().toList();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList =
				itemIds.isEmpty()
						? List.of()
						: (List<Map<String, Object>>)
								marketingActivityCatalogAccess
										.loadSkuItemsListForMarketingGift(companyId, itemIds)
										.get("list");
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : skuList) {
			long iid = longParam(skuRow.get("item_id"));
			if (iid > 0L) {
				skuByItemId.put(iid, skuRow);
			}
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (MarketingGiftItems g : giftRows) {
			Long itemId = g.getItemId();
			if (itemId == null) {
				continue;
			}
			Map<String, Object> sku = skuByItemId.get(itemId);
			if (sku == null || sku.isEmpty()) {
				continue;
			}
			Map<String, Object> giftBase = giftEntityToMap(g);
			Map<String, Object> merged = new LinkedHashMap<>(giftBase);
			merged.putAll(sku);
			// SKU putAll 不得冲掉活动赠品数量；库存/售价以 SKU 为准（对齐 PHP array_merge）
			merged.put("gift_num", giftBase.get("gift_num"));
			if (giftBase.get("item_name") != null) {
				merged.putIfAbsent("itemName", giftBase.get("item_name"));
			}
			out.add(merged);
		}
		return out;
	}

	private static void multiplyGiftNums(List<Map<String, Object>> giftItems, long multiple) {
		if (multiple <= 1L) {
			return;
		}
		for (Map<String, Object> gift : giftItems) {
			int base = intParam(gift.get("gift_num"));
			gift.put("gift_num", (int) (base * multiple));
		}
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
		m.put("gift_num", g.getGiftNum() != null ? g.getGiftNum() : 0);
		m.put("pics", decodePicsForGiftRow(g.getPics()));
		m.put("without_return", g.getWithoutReturn() != null ? g.getWithoutReturn() : false);
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

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static Map<String, Object> applyFullDiscountTotalfee(Map<String, Object> activity, long totalFee) {
		List<Tier> tiers = tiersFromActivity(activity, true);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		long discountPrice = 0L;
		String discountDesc = "";
		int last = tiers.size() - 1;
		if (totalFee >= tiers.get(last).thresholdFen) {
			discountPrice = percentOff(totalFee, tiers.get(last).value);
			discountDesc =
					"消费满" + yuanLabel(tiers.get(last).thresholdFen) + "元，给予" + tiers.get(last).value + "%优惠";
		} else if (totalFee < tiers.get(0).thresholdFen) {
			discountPrice = 0L;
		} else {
			for (int i = 0; i < last; i++) {
				Tier t = tiers.get(i);
				Tier next = tiers.get(i + 1);
				if (totalFee >= t.thresholdFen && totalFee < next.thresholdFen) {
					discountPrice = percentOff(totalFee, t.value);
					discountDesc = "消费满" + yuanLabel(t.thresholdFen) + "元，给予" + t.value + "%优惠";
					break;
				}
			}
		}
		if (discountPrice < 0L) {
			discountPrice = 0L;
		}
		return buildDiscountResult(activity, discountPrice, discountDesc, "full_discount");
	}

	private static Map<String, Object> applyFullDiscountQuantity(
			Map<String, Object> activity, long totalNum, long totalFee) {
		List<Tier> tiers = tiersFromActivity(activity, false);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		long discountPrice = 0L;
		String discountDesc = "";
		int last = tiers.size() - 1;
		if (totalNum >= tiers.get(last).thresholdFen) {
			discountPrice = percentOff(totalFee, tiers.get(last).value);
			discountDesc = "消费满" + tiers.get(last).thresholdFen + "件，给予" + tiers.get(last).value + "%优惠";
		} else if (totalNum < tiers.get(0).thresholdFen) {
			discountPrice = 0L;
		} else {
			for (int i = 0; i < last; i++) {
				Tier t = tiers.get(i);
				Tier next = tiers.get(i + 1);
				if (totalNum >= t.thresholdFen && totalNum < next.thresholdFen) {
					discountPrice = percentOff(totalFee, t.value);
					discountDesc = "消费满" + t.thresholdFen + "件，给予" + t.value + "%优惠";
					break;
				}
			}
		}
		if (discountPrice < 0L || totalFee < discountPrice) {
			discountPrice = 0L;
		}
		return buildDiscountResult(activity, discountPrice, discountDesc, "full_discount");
	}

	private static Map<String, Object> applyFullMinusTotalfee(Map<String, Object> activity, long totalFee) {
		List<Tier> tiers = tiersFromActivity(activity, true);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		int canRepeat = intParam(activity.get("canjoin_repeat"));
		long discountPrice = 0L;
		String discountDesc = "";
		int last = tiers.size() - 1;
		if (totalFee >= tiers.get(last).thresholdFen) {
			if (canRepeat == 1 && tiers.get(last).thresholdFen > 0L) {
				long multiple = totalFee / tiers.get(last).thresholdFen;
				discountPrice = tiers.get(last).value * multiple;
				discountDesc =
						"消费满"
								+ yuanLabel(tiers.get(last).thresholdFen)
								+ "元，减"
								+ fenToYuanLabel(tiers.get(last).value)
								+ "元,且上不封顶";
			} else {
				discountPrice = tiers.get(last).value;
				discountDesc =
						"消费满"
								+ yuanLabel(tiers.get(last).thresholdFen)
								+ "元，减"
								+ fenToYuanLabel(tiers.get(last).value)
								+ "元";
			}
		} else if (totalFee < tiers.get(0).thresholdFen) {
			discountPrice = 0L;
		} else {
			for (int i = 0; i < last; i++) {
				Tier t = tiers.get(i);
				Tier next = tiers.get(i + 1);
				if (totalFee >= t.thresholdFen && totalFee < next.thresholdFen) {
					discountPrice = t.value;
					discountDesc =
							"消费满" + yuanLabel(t.thresholdFen) + "元，减" + fenToYuanLabel(t.value) + "元";
					break;
				}
			}
		}
		if (discountPrice < 0L) {
			discountPrice = 0L;
		}
		return buildDiscountResult(activity, discountPrice, discountDesc, "full_minus");
	}

	private static Map<String, Object> applyFullMinusQuantity(
			Map<String, Object> activity, long totalNum, long totalFee) {
		List<Tier> tiers = tiersFromActivity(activity, false);
		if (tiers.isEmpty()) {
			return Map.of();
		}
		int canRepeat = intParam(activity.get("canjoin_repeat"));
		long discountPrice = 0L;
		String discountDesc = "";
		int last = tiers.size() - 1;
		if (totalNum >= tiers.get(last).thresholdFen) {
			if (canRepeat == 1 && tiers.get(last).thresholdFen > 0L) {
				long multiple = totalNum / tiers.get(last).thresholdFen;
				discountPrice = tiers.get(last).value * multiple;
				discountDesc =
						"消费满"
								+ tiers.get(last).thresholdFen
								+ "件，减"
								+ fenToYuanLabel(tiers.get(last).value)
								+ "元,且上不封顶";
			} else {
				discountPrice = tiers.get(last).value;
				discountDesc =
						"消费满" + tiers.get(last).thresholdFen + "件，减" + fenToYuanLabel(tiers.get(last).value) + "元";
			}
		} else if (totalNum < tiers.get(0).thresholdFen) {
			discountPrice = 0L;
		} else {
			for (int i = 0; i < last; i++) {
				Tier t = tiers.get(i);
				Tier next = tiers.get(i + 1);
				if (totalNum >= t.thresholdFen && totalNum < next.thresholdFen) {
					discountPrice = t.value;
					discountDesc = "消费满" + t.thresholdFen + "件，减" + fenToYuanLabel(t.value) + "元";
					break;
				}
			}
		}
		if (discountPrice < 0L || totalFee < discountPrice) {
			discountPrice = 0L;
		}
		return buildDiscountResult(activity, discountPrice, discountDesc, "full_minus");
	}

	private static Map<String, Object> buildDiscountResult(
			Map<String, Object> activity, long discountPrice, String discountDesc, String type) {
		if (discountPrice <= 0L || !StringUtils.hasText(discountDesc)) {
			return Map.of();
		}
		Map<String, Object> discountInfo = new LinkedHashMap<>();
		discountInfo.put("type", type);
		discountInfo.put("id", activity.get("marketing_id"));
		discountInfo.put("rule", discountDesc);
		discountInfo.put("info", activity.get("marketing_name"));
		discountInfo.put("discount_fee", discountPrice);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("discount_fee", discountPrice);
		result.put("discount_desc", discountInfo);
		return result;
	}

	private static List<Tier> tiersFromActivity(Map<String, Object> activity, boolean totalfee) {
		Object raw = activity.get("condition_value");
		List<Map<String, Object>> rules = new ArrayList<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					rules.add(row);
				}
			}
		}
		List<Tier> tiers = new ArrayList<>();
		for (Map<String, Object> rule : rules) {
			long full = parseFullThreshold(rule.get("full"), totalfee);
			long val = parseDiscountValue(rule, totalfee);
			tiers.add(new Tier(full, val));
		}
		tiers.sort((a, b) -> Long.compare(a.thresholdFen, b.thresholdFen));
		return tiers;
	}

	private static long parseFullThreshold(Object full, boolean totalfee) {
		if (full == null) {
			return 0L;
		}
		if (totalfee) {
			BigDecimal yuan = new BigDecimal(full.toString().trim());
			return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
		}
		if (full instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(full.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseDiscountValue(Map<String, Object> rule, boolean totalfee) {
		Object minus = rule.get("minus");
		Object discount = rule.get("discount");
		Object raw = minus != null ? minus : discount;
		if (raw == null) {
			return 0L;
		}
		if (minus != null) {
			BigDecimal yuan = new BigDecimal(raw.toString().trim());
			return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long percentOff(long totalFee, long percent) {
		return BigDecimal.valueOf(totalFee)
				.multiply(BigDecimal.valueOf(percent))
				.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
				.longValue();
	}

	private Map<String, Object> activityToRuleMap(MarketingActivity detail, List<Map<String, Object>> rules) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("marketing_id", detail.getMarketingId());
		row.put("marketing_type", detail.getMarketingType());
		row.put("marketing_name", detail.getMarketingName());
		row.put("condition_type", detail.getConditionType());
		row.put("condition_value", rules);
		row.put("canjoin_repeat", detail.getCanjoinRepeat());
		row.put("in_proportion", Boolean.TRUE.equals(detail.getInProportion()) ? 1 : 0);
		row.put("join_limit", detail.getJoinLimit() != null ? detail.getJoinLimit() : 0);
		return row;
	}

	private List<Map<String, Object>> parseConditionRules(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			Object node = objectMapper.readValue(json.trim(), Object.class);
			if (node instanceof List<?> list) {
				List<Map<String, Object>> out = new ArrayList<>();
				for (Object o : list) {
					if (o instanceof Map<?, ?> m) {
						Map<String, Object> row = new LinkedHashMap<>();
						for (Map.Entry<?, ?> e : m.entrySet()) {
							row.put(String.valueOf(e.getKey()), e.getValue());
						}
						out.add(row);
					}
				}
				return out;
			}
		} catch (Exception ignored) {
		}
		return List.of();
	}

	private boolean isOngoing(MarketingActivity detail) {
		if (!"agree".equals(detail.getCheckStatus())) {
			return false;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Integer releaseTime = detail.getReleaseTime();
		if (releaseTime != null && releaseTime != 0 && releaseTime > now) {
			return false;
		}
		Integer start = detail.getStartTime();
		Integer end = detail.getEndTime();
		return start != null && end != null && now >= start && now < end;
	}

	private boolean memberGradeAllowed(MarketingActivity detail, long userId, long companyId) {
		List<?> grades = decodeJsonList(detail.getValidGrade());
		if (grades == null || grades.isEmpty()) {
			return true;
		}
		Long userGrade = resolveUserGrade(userId, companyId);
		if (userGrade == null) {
			return false;
		}
		return gradeListContains(grades, userGrade);
	}

	private int marketingJoinCount(long companyId, long marketingId, long userId) {
		String key = "MarketingUserJoinNum:" + companyId + ":" + marketingId;
		Object hv = stringRedisTemplate.opsForHash().get(key, "user_" + userId);
		if (hv == null || !StringUtils.hasText(hv.toString())) {
			return 0;
		}
		try {
			return Integer.parseInt(hv.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
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
		if (userId <= 0L) {
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

	private static int intParam(Object raw) {
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String yuanLabel(long fen) {
		if (fen % 100L == 0L) {
			return String.valueOf(fen / 100L);
		}
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String fenToYuanLabel(long fen) {
		return yuanLabel(fen);
	}

	private record Tier(long thresholdFen, long value) {}

	private record PlusPriceTier(long threshold, String plusPriceYuan) {}
}
