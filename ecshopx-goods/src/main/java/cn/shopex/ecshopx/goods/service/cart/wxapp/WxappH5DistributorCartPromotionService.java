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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.PlusBuyCartRedisKeys;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailPromotionActivityService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.service.LimitPersonBuyService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityApplyRulesService;
import cn.shopex.ecshopx.promotions.service.SeckillUserBuysStoreService;
import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappH5DistributorCartPromotionService {

	private final SkuValidMarketingActivityService skuValidMarketingActivityService;
	private final MarketingActivityApplyRulesService marketingActivityApplyRulesService;
	private final ShopMenuService shopMenuService;
	private final WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	private final MemberAccountService memberAccountService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final LimitPersonBuyService limitPersonBuyService;
	private final SeckillUserBuysStoreService seckillUserBuysStoreService;
	private final StringRedisTemplate stringRedisTemplate;

	public WxappH5DistributorCartPromotionService(
			SkuValidMarketingActivityService skuValidMarketingActivityService,
			MarketingActivityApplyRulesService marketingActivityApplyRulesService,
			ShopMenuService shopMenuService,
			WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService,
			MemberAccountService memberAccountService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			LimitPersonBuyService limitPersonBuyService,
			SeckillUserBuysStoreService seckillUserBuysStoreService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.skuValidMarketingActivityService = skuValidMarketingActivityService;
		this.marketingActivityApplyRulesService = marketingActivityApplyRulesService;
		this.shopMenuService = shopMenuService;
		this.wxappGoodsItemsDetailPromotionActivityService = wxappGoodsItemsDetailPromotionActivityService;
		this.memberAccountService = memberAccountService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.limitPersonBuyService = limitPersonBuyService;
		this.seckillUserBuysStoreService = seckillUserBuysStoreService;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public Map<String, Object> apply(long companyId, long userId, List<Map<String, Object>> cartLines, String userDevice) {
		Map<String, Object> emptyTotals = emptyTotals();
		if (cartLines == null || cartLines.isEmpty()) {
			return emptyTotals;
		}
		applyLimitedTimeSaleActOnCartLines(companyId, userId, cartLines, userDevice, false);
		return applyMarketingAndTotals(companyId, userId, cartLines, userDevice);
	}

	public Map<String, Object> applyMarketingAndTotals(
			long companyId, long userId, List<Map<String, Object>> cartLines, String userDevice) {
		Map<String, Object> emptyTotals = emptyTotals();
		if (cartLines == null || cartLines.isEmpty()) {
			return emptyTotals;
		}
		attachPromotions(companyId, userId, cartLines, userDevice);
		return computeTotals(companyId, userId, cartLines);
	}

	public void applyLimitedTimeSaleActOnCartLines(
			long companyId, long userId, List<Map<String, Object>> cartLines, String userDevice, boolean isCheckout) {
		if ("pc".equals(userDevice)) {
			return;
		}
		Map<Long, Map<String, Object>> limitedTimeSaleLimit = new LinkedHashMap<>();
		for (Map<String, Object> cart : cartLines) {
			if ("package".equals(stringVal(cart.get("activity_type")))) {
				continue;
			}
			long itemId = longVal(cart.get("item_id"));
			if (itemId <= 0L) {
				continue;
			}
			long shopId = longVal(cart.get("shop_id"));
			Map<String, Object> activityData =
					wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, itemId, shopId);
			if (activityData == null) {
				continue;
			}
			String activityType = stringVal(activityData.get("activity_type"));
			if (!"limited_time_sale".equals(activityType) && !"limited_buy".equals(activityType)) {
				continue;
			}
			if ("limited_buy".equals(activityType) && !"shop_offline".equals(stringVal(cart.get("shop_type")))) {
				attachLimitedBuyOnCartLine(companyId, cart, activityData);
				continue;
			}
			if (!"limited_time_sale".equals(activityType)) {
				continue;
			}
			Object listRaw = activityData.get("list");
			if (!(listRaw instanceof Map<?, ?> listMap)) {
				continue;
			}
			Object itemRaw = listMap.get(String.valueOf(itemId));
			if (!(itemRaw instanceof Map<?, ?> itemDataRaw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> itemData = (Map<String, Object>) itemDataRaw;
			@SuppressWarnings("unchecked")
			Map<String, Object> info =
					activityData.get("info") instanceof Map<?, ?> im ? (Map<String, Object>) im : Map.of();
			int num = intVal(cart.get("num"));
			if (isCheckout) {
				assertLimitedTimeSaleRemaining(companyId, userId, cart, info, itemData, itemId, num);
			}
			long basePrice = longVal(itemData.get("price"));
			long activityPrice = longVal(itemData.get("activity_price"));
			cart.put("price", basePrice);
			cart.put("activity_price", activityPrice);
			cart.put("total_fee", Long.toString(activityPrice * num));
			cart.put("is_last_price", Boolean.TRUE);
			Map<String, Object> limitedTimeSaleAct = new LinkedHashMap<>();
			limitedTimeSaleAct.put("activity_id", info.get("seckill_id"));
			limitedTimeSaleAct.put("marketing_type", "limited_time_sale");
			limitedTimeSaleAct.put("marketing_name", info.get("activity_name"));
			limitedTimeSaleAct.put("limit_total_money", info.get("limit_total_money"));
			limitedTimeSaleAct.put("limit_money", info.get("limit_money"));
			limitedTimeSaleAct.put("validity_period", info.get("validity_period"));
			limitedTimeSaleAct.put("is_free_shipping", info.get("is_free_shipping"));
			limitedTimeSaleAct.put("third_params", info.get("otherext"));
			limitedTimeSaleAct.put("promotion_tag", "限时优惠");
			cart.put("limitedTimeSaleAct", limitedTimeSaleAct);
			if (isCheckout && (longVal(info.get("limit_total_money")) > 0L || longVal(info.get("limit_money")) > 0L)) {
				accumulateLimitedTimeSaleMoneyLimit(
						limitedTimeSaleLimit, companyId, userId, cart, info, itemId, activityPrice * num);
			}
		}
		assertLimitedTimeSaleMoneyLimits(limitedTimeSaleLimit);
	}

	public void applyLimitedTimeSaleCheckoutMoneyOnCartInfo(
			Map<String, Object> cartInfo, Map<String, Object> activityData, boolean isCheckout) {
		if (cartInfo == null || activityData == null || activityData.isEmpty()) {
			return;
		}
		if (!"limited_time_sale".equals(stringVal(activityData.get("activity_type")))) {
			return;
		}
		long itemId = longVal(cartInfo.get("item_id"));
		Object listRaw = activityData.get("list");
		if (!(listRaw instanceof Map<?, ?> listMap)) {
			return;
		}
		Object itemRaw = listMap.get(String.valueOf(itemId));
		if (!(itemRaw instanceof Map<?, ?> itemDataRaw)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> itemData = (Map<String, Object>) itemDataRaw;
		@SuppressWarnings("unchecked")
		Map<String, Object> info =
				activityData.get("info") instanceof Map<?, ?> im ? (Map<String, Object>) im : Map.of();
		int num = intVal(cartInfo.get("num"));
		long activityPrice = longVal(itemData.get("activity_price"));
		long totalFee = activityPrice * num;
		cartInfo.put("price", itemData.get("price"));
		cartInfo.put("activity_price", activityPrice);
		cartInfo.put("total_fee", Long.toString(totalFee));
		if (!isCheckout) {
			return;
		}
		long limitMoney = longVal(info.get("limit_money"));
		long limitTotalMoney = longVal(info.get("limit_total_money"));
		if (limitMoney <= 0L && limitTotalMoney <= 0L) {
			return;
		}
		long companyId = longVal(cartInfo.get("company_id"));
		long userId = longVal(cartInfo.get("user_id"));
		long seckillId = longVal(info.get("seckill_id"));
		Map<String, Object> buyData = seckillUserBuysStoreService.getUserBuysData(seckillId, companyId, userId, itemId);
		long userBuyTotalPrice = longVal(buyData.get("userBuyTotalPrcie"));
		if (limitMoney > 0L && totalFee < limitMoney) {
			throw new ResourceException("活动单笔最少购买" + fenToYuanLabel(limitMoney) + "元");
		}
		if (limitTotalMoney > 0L && totalFee > limitTotalMoney - userBuyTotalPrice) {
			throw new ResourceException("活动总限额" + fenToYuanLabel(limitTotalMoney) + "元");
		}
	}

	private void accumulateLimitedTimeSaleMoneyLimit(
			Map<Long, Map<String, Object>> limitedTimeSaleLimit,
			long companyId,
			long listUserId,
			Map<String, Object> cart,
			Map<String, Object> info,
			long itemId,
			long lineTotalFee) {
		long seckillId = longVal(info.get("seckill_id"));
		Map<String, Object> row = limitedTimeSaleLimit.get(seckillId);
		if (row != null) {
			row.put("total_fee", longVal(row.get("total_fee")) + lineTotalFee);
			return;
		}
		row = new LinkedHashMap<>();
		row.put("total_fee", lineTotalFee);
		row.put("limit_total_money", info.get("limit_total_money"));
		row.put("limit_money", info.get("limit_money"));
		long userId = longVal(cart.get("user_id"));
		if (userId <= 0L) {
			userId = listUserId;
		}
		Map<String, Object> buyData = seckillUserBuysStoreService.getUserBuysData(seckillId, companyId, userId, itemId);
		row.put("user_buy_total_price", longVal(buyData.get("userBuyTotalPrcie")));
		limitedTimeSaleLimit.put(seckillId, row);
	}

	private static void assertLimitedTimeSaleMoneyLimits(Map<Long, Map<String, Object>> limitedTimeSaleLimit) {
		if (limitedTimeSaleLimit == null || limitedTimeSaleLimit.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : limitedTimeSaleLimit.values()) {
			long limitMoney = longVal(row.get("limit_money"));
			long totalFee = longVal(row.get("total_fee"));
			if (limitMoney > 0L && totalFee < limitMoney) {
				throw new ResourceException("活动单笔最少购买" + fenToYuanLabel(limitMoney) + "元");
			}
			long limitTotalMoney = longVal(row.get("limit_total_money"));
			long userBuyTotalPrice = longVal(row.get("user_buy_total_price"));
			if (limitTotalMoney > 0L && totalFee > limitTotalMoney - userBuyTotalPrice) {
				throw new ResourceException("活动总限额" + fenToYuanLabel(limitTotalMoney) + "元");
			}
		}
	}

	private static String fenToYuanLabel(long fen) {
		if (fen % 100L == 0L) {
			return Long.toString(fen / 100L);
		}
		return BigDecimal.valueOf(fen).movePointLeft(2).stripTrailingZeros().toPlainString();
	}

	public void assertLimitedTimeSaleOnAdd(
			long companyId, long userId, long itemId, int num, Map<String, Object> activityData) {
		if (activityData == null || activityData.isEmpty()) {
			return;
		}
		if (!"limited_time_sale".equals(stringVal(activityData.get("activity_type")))) {
			return;
		}
		Object listRaw = activityData.get("list");
		if (!(listRaw instanceof Map<?, ?> listMap)) {
			return;
		}
		Object itemRaw = listMap.get(String.valueOf(itemId));
		if (!(itemRaw instanceof Map<?, ?> itemDataRaw)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> itemData = (Map<String, Object>) itemDataRaw;
		@SuppressWarnings("unchecked")
		Map<String, Object> info =
				activityData.get("info") instanceof Map<?, ?> im ? (Map<String, Object>) im : Map.of();
		long seckillId = longVal(info.get("seckill_id"));
		long limitNum = longVal(itemData.get("limit_num"));
		if (limitNum <= 0L) {
			return;
		}
		Map<String, Object> buyData = seckillUserBuysStoreService.getUserBuysData(seckillId, companyId, userId, itemId);
		long userBuyStore = longVal(buyData.get("userBuyStore"));
		if (limitNum - userBuyStore < num) {
			throw new ResourceException("超出限购数量");
		}
	}

	private void assertLimitedTimeSaleRemaining(
			long companyId,
			long listUserId,
			Map<String, Object> cart,
			Map<String, Object> info,
			Map<String, Object> itemData,
			long itemId,
			int num) {
		long limitNum = longVal(itemData.get("limit_num"));
		if (limitNum <= 0L) {
			return;
		}
		long seckillId = longVal(info.get("seckill_id"));
		long userId = longVal(cart.get("user_id"));
		if (userId <= 0L) {
			userId = listUserId;
		}
		Map<String, Object> buyData = seckillUserBuysStoreService.getUserBuysData(seckillId, companyId, userId, itemId);
		long userBuyStore = longVal(buyData.get("userBuyStore"));
		if (limitNum - userBuyStore < num) {
			throw new ResourceException(stringVal(cart.get("item_name")) + "超出限购数量");
		}
	}

	/**
	 * 加购时累计限购校验（对齐 PHP DistributorCartObject 商品限购分支）。
	 */
	public void assertLimitedBuyOnAdd(
			long companyId, long userId, long itemId, long shopId, int num, Map<String, Object> activityData) {
		if (activityData == null || activityData.isEmpty()) {
			return;
		}
		if (!"limited_buy".equals(stringVal(activityData.get("activity_type")))) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> info =
				activityData.get("info") instanceof Map<?, ?> im ? (Map<String, Object>) im : null;
		if (info == null) {
			return;
		}
		Object validGradeRaw = info.get("valid_grade");
		List<?> validGrade = validGradeRaw instanceof List<?> l ? l : List.of();
		if (!isHaveVip(userId, companyId, validGrade)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> rule =
				info.get("rule") instanceof Map<?, ?> rm ? (Map<String, Object>) rm : Map.of();
		long limitTotal = longVal(rule.get("limit"));
		Long distributorFilter = null;
		if ("shop".equals(stringVal(info.get("limit_type")))) {
			distributorFilter = shopId;
		}
		long limitNumber =
				limitPersonBuyService.getLimitPersonBuyNumber(companyId, userId, itemId, distributorFilter);
		if (num + limitNumber > limitTotal) {
			throw new ResourceException("超出限购数量");
		}
	}

	private void attachLimitedBuyOnCartLine(
			long companyId, Map<String, Object> cart, Map<String, Object> activityData) {
		@SuppressWarnings("unchecked")
		Map<String, Object> info =
				activityData.get("info") instanceof Map<?, ?> im ? (Map<String, Object>) im : null;
		if (info == null) {
			return;
		}
		long userId = longVal(cart.get("user_id"));
		Object validGradeRaw = info.get("valid_grade");
		List<?> validGrade = validGradeRaw instanceof List<?> l ? l : List.of();
		if (!isHaveVip(userId, companyId, validGrade)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> rule =
				info.get("rule") instanceof Map<?, ?> rm ? (Map<String, Object>) rm : Map.of();
		long limitTotal = longVal(rule.get("limit"));
		long itemId = longVal(cart.get("item_id"));
		long limitNumber = limitPersonBuyService.getLimitPersonBuyNumber(companyId, userId, itemId);
		long num = longVal(cart.get("num")) + limitNumber;
		if (num > limitTotal) {
			String itemName = stringVal(cart.get("item_name"));
			throw new ResourceException(itemName + "超出限购数量");
		}
		Map<String, Object> limitedBuy = new LinkedHashMap<>();
		limitedBuy.put("activity_id", info.get("limit_id"));
		limitedBuy.put("marketing_type", "limited_buy");
		limitedBuy.put("marketing_name", info.get("limit_name"));
		limitedBuy.put("limit_total", rule.get("limit"));
		limitedBuy.put("limit_buy", limitTotal - limitNumber);
		limitedBuy.put("buy", limitNumber);
		limitedBuy.put("rule", rule);
		limitedBuy.put("promotion_tag", "限购商品");
		cart.put("limitedBuy", limitedBuy);
	}

	private boolean isHaveVip(long userId, long companyId, List<?> validGrade) {
		if (userId <= 0L) {
			return false;
		}
		if (validGrade == null || validGrade.isEmpty()) {
			return true;
		}
		String userGradeToken = resolveUserGradeToken(userId, companyId);
		if (!StringUtils.hasText(userGradeToken)) {
			return false;
		}
		return gradeListContainsToken(validGrade, userGradeToken);
	}

	private String resolveUserGradeToken(long userId, long companyId) {
		Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (Boolean.TRUE.equals(vip.get("valid"))
				&& Boolean.TRUE.equals(vip.get("is_vip"))
				&& vip.get("vip_type") != null
				&& StringUtils.hasText(String.valueOf(vip.get("vip_type")).trim())) {
			String vipType = String.valueOf(vip.get("vip_type")).trim();
			if (!"normal".equals(vipType)) {
				return vipType;
			}
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object g = info.get("grade_id");
		if (g instanceof Number n && n.longValue() > 0L) {
			return String.valueOf(n.longValue());
		}
		if (g != null && StringUtils.hasText(g.toString())) {
			String s = g.toString().trim();
			try {
				long id = Long.parseLong(s);
				if (id > 0L) {
					return String.valueOf(id);
				}
			} catch (NumberFormatException ignored) {
				if (!"0".equals(s)) {
					return s;
				}
			}
		}
		return "";
	}

	private static boolean gradeListContainsToken(List<?> validGrade, String userGradeToken) {
		String token = userGradeToken.trim();
		for (Object o : validGrade) {
			if (o == null) {
				continue;
			}
			if (o.toString().trim().equals(token)) {
				return true;
			}
		}
		return false;
	}

	private void attachPromotions(
			long companyId, long userId, List<Map<String, Object>> cartData, String userDevice) {
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> cart : cartData) {
			long iid = longVal(cart.get("item_id"));
			if (iid > 0L) {
				itemIds.add(iid);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		long distributorId = 0L;
		if (!cartData.isEmpty()) {
			distributorId = longVal(cartData.get(0).get("shop_id"));
		}
		List<Map<String, Object>> activityData =
				skuValidMarketingActivityService.getValidMarketingActivityForCartItems(
						companyId, itemIds, userId, distributorId);
		if (activityData.isEmpty()) {
			return;
		}
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		for (Map<String, Object> activity : activityData) {
			long activityId = longVal(activity.get("marketing_id"));
			@SuppressWarnings("unchecked")
			Map<String, Object> relItems =
					activity.get("items") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
			Map<String, Object> activityCopy = new LinkedHashMap<>(activity);
			activityCopy.remove("items");
			int joinLimit = intVal(activity.get("join_limit"));
			int usedCount = intVal(activity.get("usedCount"));
			if (joinLimit > 0 && usedCount > 0 && usedCount >= joinLimit) {
				continue;
			}
			int useBound = intVal(activity.get("use_bound"));
			String marketingType = stringVal(activity.get("marketing_type"));
			for (Map<String, Object> cart : cartData) {
				if ("package".equals(stringVal(cart.get("activity_type")))) {
					continue;
				}
				if (Boolean.TRUE.equals(cart.get("is_last_price"))) {
					continue;
				}
				long cartShopId = longVal(cart.get("shop_id"));
				@SuppressWarnings("unchecked")
				List<String> shopIds =
						activity.get("shop_ids") instanceof List<?> l
								? l.stream().map(String::valueOf).toList()
								: List.of();
				if (cartShopId > 0L
						&& !shopIds.isEmpty()
						&& !shopIds.contains("all")
						&& !shopIds.contains(String.valueOf(cartShopId))) {
					continue;
				}
				if ("platform".equals(productModel)) {
					long activitySourceId = longVal(activity.get("source_id"));
					if (cartShopId != activitySourceId) {
						continue;
					}
				}
				boolean boundOk = false;
				if (useBound == 0 && relItems.isEmpty()) {
					boundOk = true;
				} else if (useBound > 0) {
					Object rel = relItems.get(String.valueOf(longVal(cart.get("item_id"))));
					if (rel instanceof Map<?, ?> rm && !rm.isEmpty()) {
						boundOk = true;
					} else if (rel instanceof List<?> rl && !rl.isEmpty()) {
						boundOk = true;
					}
				}
				if (!boundOk) {
					continue;
				}
				if ("full_gift".equals(marketingType)) {
					appendIdList(cart, "full_gift_id", activityId);
				} else if ("plus_price_buy".equals(marketingType) && !"pc".equals(userDevice)) {
					appendIdList(cart, "plus_buy_id", activityId);
				} else {
					appendPromotion(cart, activityCopy);
					cart.put("activity_id", activityId);
				}
			}
		}
	}

	private Map<String, Object> computeTotals(long companyId, long userId, List<Map<String, Object>> cartData) {
		applyLimitedTimeSaleDiscountDesc(cartData);
		Map<Long, Map<String, Object>> promotionDatas = new LinkedHashMap<>();
		Map<Long, UsedPromotion> usedPromotions = new LinkedHashMap<>();
		Map<Long, List<Long>> usedFullGift = new LinkedHashMap<>();
		Map<Long, List<Long>> usedPlusBuy = new LinkedHashMap<>();

		for (Map<String, Object> value : cartData) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> promotions =
					value.get("promotions") instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
			for (Map<String, Object> v : promotions) {
				long mid = longVal(v.get("marketing_id"));
				if (mid > 0L) {
					promotionDatas.put(mid, v);
				}
			}
			long activityId = longVal(value.get("activity_id"));
			if (activityId > 0L && !Boolean.TRUE.equals(value.get("is_last_price"))) {
				if (promotionDatas.containsKey(activityId)) {
					usedPromotions.computeIfAbsent(activityId, k -> new UsedPromotion()).cartIds.add(longVal(value.get("cart_id")));
				}
			}
			@SuppressWarnings("unchecked")
			List<Long> fullGiftIds = value.get("full_gift_id") instanceof List<?> l ? castLongList(l) : List.of();
			if (!fullGiftIds.isEmpty() && !Boolean.TRUE.equals(value.get("is_last_price"))) {
				for (Long gid : fullGiftIds) {
					usedFullGift.computeIfAbsent(gid, k -> new ArrayList<>()).add(longVal(value.get("cart_id")));
				}
			}
			@SuppressWarnings("unchecked")
			List<Long> plusBuyIds = value.get("plus_buy_id") instanceof List<?> l ? castLongList(l) : List.of();
			if (!plusBuyIds.isEmpty() && !Boolean.TRUE.equals(value.get("is_last_price"))) {
				for (Long pid : plusBuyIds) {
					usedPlusBuy.computeIfAbsent(pid, k -> new ArrayList<>()).add(longVal(value.get("cart_id")));
				}
			}
		}

		List<Map<String, Object>> usedActivity = new ArrayList<>();
		List<Long> usedActivityIds = new ArrayList<>();
		Map<Long, Map<String, Object>> activityGroupingById = new LinkedHashMap<>();

		for (Map.Entry<Long, UsedPromotion> en : usedPromotions.entrySet()) {
			long activityId = en.getKey();
			Map<String, Object> promo = promotionDatas.get(activityId);
			if (promo == null) {
				continue;
			}
			String marketingType = stringVal(promo.get("marketing_type"));
			String marketName = stringVal(promo.get("marketing_name"));
			UsedPromotion up = en.getValue();
			Map<String, Object> grouping = activityGroupingById.computeIfAbsent(activityId, k -> new LinkedHashMap<>());
			grouping.put("activity_name", marketName);
			grouping.put("activity_id", activityId);
			grouping.put("activity_tag", promo.get("promotion_tag"));
			grouping.put("condition_rules", promo.get("condition_rules"));
			grouping.put("activity_type", marketingType);
			grouping.put("cart_ids", new ArrayList<>(up.cartIds));
			if ("full_minus".equals(marketingType) || "full_discount".equals(marketingType)) {
				long discountPrice =
						applyFullDiscount(
								cartData, companyId, userId, up.cartIds, promo);
				if (discountPrice > 0L) {
					usedActivity.add(Map.of("activity_id", activityId, "activity_name", marketName));
					usedActivityIds.add(activityId);
				}
				grouping.put("discount_fee", Long.toString(discountPrice));
			}
		}

		List<Map<String, Object>> giftActivity = new ArrayList<>();
		Map<Long, Map<String, Object>> byCartId = indexByCartId(cartData);
		for (Map.Entry<Long, List<Long>> giftEntry : usedFullGift.entrySet()) {
			long marketingId = giftEntry.getKey();
			List<Long> validCartIds = giftEntry.getValue();
			long cartTotalPrice = 0L;
			long cartTotalNum = 0L;
			List<Long> usedItemIds = new ArrayList<>();
			for (Long cartId : validCartIds) {
				Map<String, Object> row = byCartId.get(cartId);
				if (row == null || !truthyChecked(row.get("is_checked"))) {
					continue;
				}
				cartTotalPrice += linePayFen(row);
				cartTotalNum += intVal(row.get("num"));
				long itemId = longVal(row.get("item_id"));
				if (itemId > 0L) {
					usedItemIds.add(itemId);
				}
			}
			Map<String, Object> cartParams = new LinkedHashMap<>();
			cartParams.put("total_price", cartTotalPrice);
			cartParams.put("total_num", cartTotalNum);
			Map<String, Object> giftData =
					marketingActivityApplyRulesService.applyActivityRules(
							companyId, marketingId, userId, cartParams);
			long activityId = longVal(giftData.get("activity_id"));
			Object giftsRaw = giftData.get("gifts");
			boolean hasGifts = giftsRaw instanceof List<?> gl && !gl.isEmpty();
			if (activityId > 0L && hasGifts) {
				Map<String, Object> entry = new LinkedHashMap<>(giftData);
				entry.put("activity_item_ids", usedItemIds);
				giftActivity.add(entry);
			}
		}

		List<Map<String, Object>> plusBuyActivity = new ArrayList<>();
		for (Map.Entry<Long, List<Long>> plusEntry : usedPlusBuy.entrySet()) {
			long marketingId = plusEntry.getKey();
			List<Long> validCartIds = plusEntry.getValue();
			long cartTotalPrice = 0L;
			long cartTotalNum = 0L;
			List<Long> usedItemIds = new ArrayList<>();
			for (Long cartId : validCartIds) {
				Map<String, Object> row = byCartId.get(cartId);
				if (row == null || !truthyChecked(row.get("is_checked"))) {
					continue;
				}
				cartTotalPrice += linePayFen(row);
				cartTotalNum += intVal(row.get("num"));
				long itemId = longVal(row.get("item_id"));
				if (itemId > 0L) {
					usedItemIds.add(itemId);
				}
			}
			Map<String, Object> cartParams = new LinkedHashMap<>();
			cartParams.put("total_price", cartTotalPrice);
			cartParams.put("total_num", cartTotalNum);
			Map<String, Object> plusData =
					marketingActivityApplyRulesService.applyActivityRules(
							companyId, marketingId, userId, cartParams);
			long activityId = longVal(plusData.get("activity_id"));
			Object plusItemsRaw = plusData.get("plus_buy_items");
			boolean hasPlusItems = plusItemsRaw instanceof Map<?, ?> m && !m.isEmpty();
			if (activityId > 0L && hasPlusItems) {
				Map<String, Object> entry = new LinkedHashMap<>(plusData);
				entry.put("activity_item_ids", usedItemIds);
				@SuppressWarnings("unchecked")
				Map<String, Object> pluscart = (Map<String, Object>) plusItemsRaw;
				String redisKey = PlusBuyCartRedisKeys.redisKey(companyId, userId, marketingId);
				String checkedRaw = stringRedisTemplate.opsForValue().get(redisKey);
				long checkedId = longVal(checkedRaw);
				if (checkedId > 0L) {
					Object checkedPlus = pluscart.get(String.valueOf(checkedId));
					if (checkedPlus instanceof Map<?, ?> checkedMap) {
						@SuppressWarnings("unchecked")
						Map<String, Object> plusItem = (Map<String, Object>) checkedMap;
						entry.put("plus_item", plusItem);
						for (Long cartId : validCartIds) {
							Map<String, Object> row = byCartId.get(cartId);
							if (row != null) {
								row.put("marketing_type", "plus_price_buy");
							}
						}
					}
				}
				plusBuyActivity.add(entry);
			}
		}

		long itemTotalFee = 0L;
		long totalDiscountFee = 0L;
		int itemNum = 0;
		int cartNum = 0;
		for (Map<String, Object> cart : cartData) {
			if (!truthyChecked(cart.get("is_checked"))) {
				continue;
			}
			itemTotalFee += packageAwareLineItemFee(cart);
			itemNum += intVal(cart.get("num"));
			cartNum += 1;
			totalDiscountFee += longVal(cart.get("discount_fee"));
		}
		long totalPayFee = totalDiscountFee >= itemTotalFee ? 0L : itemTotalFee - totalDiscountFee;

		Map<String, Object> totals = new LinkedHashMap<>();
		totals.put("item_fee", itemTotalFee);
		totals.put("discount_fee", totalDiscountFee);
		totals.put("total_fee", totalPayFee);
		totals.put("cart_total_num", itemNum);
		totals.put("cart_total_count", cartNum);
		totals.put("cart_total_price", itemTotalFee);
		totals.put("used_activity", usedActivity);
		totals.put("used_activity_ids", usedActivityIds);
		totals.put("activity_grouping", new ArrayList<>(activityGroupingById.values()));
		totals.put("gift_activity", giftActivity);
		totals.put("plus_buy_activity", plusBuyActivity);
		return totals;
	}

	private long applyFullDiscount(
			List<Map<String, Object>> cartData,
			long companyId,
			long userId,
			List<Long> validCartIds,
			Map<String, Object> activityDetail) {
		long totalPrice = 0L;
		long totalNum = 0L;
		Map<Long, Map<String, Object>> byCartId = indexByCartId(cartData);
		for (Long cartId : validCartIds) {
			Map<String, Object> row = byCartId.get(cartId);
			if (row == null || !truthyChecked(row.get("is_checked"))) {
				continue;
			}
			totalPrice += linePayFen(row);
			totalNum += intVal(row.get("num"));
		}
		Map<String, Object> cartParams = new LinkedHashMap<>();
		cartParams.put("total_price", totalPrice);
		cartParams.put("total_num", totalNum);
		long marketingId = longVal(activityDetail.get("marketing_id"));
		Map<String, Object> discount =
				marketingActivityApplyRulesService.applyActivityRules(companyId, marketingId, userId, cartParams);
		long discountFee = longVal(discount.get("discount_fee"));
		if (discountFee <= 0L) {
			return 0L;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> discountDesc =
				discount.get("discount_desc") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
		List<Long> checkedIds = new ArrayList<>();
		for (Long cartId : validCartIds) {
			Map<String, Object> row = byCartId.get(cartId);
			if (row != null && truthyChecked(row.get("is_checked"))) {
				checkedIds.add(cartId);
			}
		}
		if (checkedIds.isEmpty()) {
			return 0L;
		}
		List<Long> shareFees = allocateDiscount(discountFee, checkedIds, totalPrice, byCartId);
		for (int i = 0; i < checkedIds.size(); i++) {
			long cartId = checkedIds.get(i);
			long share = shareFees.get(i);
			Map<String, Object> row = byCartId.get(cartId);
			if (row == null) {
				continue;
			}
			long existingDisc = longVal(row.get("discount_fee"));
			row.put("discount_fee", existingDisc + share);
			long pay = linePayFen(row) - share;
			row.put("total_fee", Long.toString(Math.max(0L, pay)));
			row.put("activity_type", stringVal(activityDetail.get("marketing_type")));
			row.put("activity_id", marketingId);
			Map<String, Object> desc = new LinkedHashMap<>(discountDesc);
			desc.put("discount_fee", Long.toString(share));
			appendActivityInfo(row, desc);
		}
		return discountFee;
	}

	private static List<Long> allocateDiscount(
			long discountFee, List<Long> cartIds, long totalPrice, Map<Long, Map<String, Object>> byCartId) {
		List<Long> shares = new ArrayList<>();
		long allocated = 0L;
		for (int i = 0; i < cartIds.size(); i++) {
			long cartId = cartIds.get(i);
			Map<String, Object> row = byCartId.get(cartId);
			long linePay = row == null ? 0L : linePayFen(row);
			if (i == cartIds.size() - 1 && !shares.isEmpty()) {
				shares.add(discountFee - allocated);
			} else if (cartIds.size() == 1) {
				shares.add(discountFee);
			} else {
				BigDecimal percent =
						totalPrice > 0L
								? BigDecimal.valueOf(linePay)
										.divide(BigDecimal.valueOf(totalPrice), 5, RoundingMode.HALF_UP)
								: BigDecimal.ZERO;
				long share = percent.multiply(BigDecimal.valueOf(discountFee)).setScale(0, RoundingMode.HALF_UP).longValue();
				shares.add(share);
				allocated += share;
			}
		}
		return shares;
	}

	private static void applyLimitedTimeSaleDiscountDesc(List<Map<String, Object>> cartData) {
		for (Map<String, Object> cartinfo : cartData) {
			long discountFee = longVal(cartinfo.get("discount_fee"));
			long totalFee = longVal(cartinfo.get("total_fee"));
			if (totalFee == 0L) {
				totalFee = longVal(cartinfo.get("price")) * intVal(cartinfo.get("num"));
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> activityInfo =
					cartinfo.get("activity_info") instanceof List<?> l
							? (List<Map<String, Object>>) l
							: new ArrayList<>();
			if (cartinfo.get("limitedTimeSaleAct") instanceof Map<?, ?> actRaw
					&& cartinfo.get("activity_price") != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> act = (Map<String, Object>) actRaw;
				long activityPrice = longVal(cartinfo.get("activity_price"));
				long discountFeeL =
						longVal(cartinfo.get("price")) * intVal(cartinfo.get("num"))
								- activityPrice * intVal(cartinfo.get("num"));
				discountFee += discountFeeL;
				totalFee = activityPrice * intVal(cartinfo.get("num"));
				Map<String, Object> activityInfoRow = new LinkedHashMap<>();
				activityInfoRow.put("type", "limited_time_sale");
				activityInfoRow.put("id", act.get("activity_id"));
				activityInfoRow.put("rule", act.get("promotion_tag"));
				activityInfoRow.put("info", act.get("marketing_name"));
				activityInfoRow.put("discount_fee", Long.toString(discountFeeL));
				activityInfo.add(activityInfoRow);
			}
			cartinfo.put("discount_fee", discountFee);
			cartinfo.put("total_fee", Long.toString(totalFee));
			cartinfo.put("activity_info", activityInfo);
		}
	}

	private static Map<Long, Map<String, Object>> indexByCartId(List<Map<String, Object>> cartData) {
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> row : cartData) {
			long cid = longVal(row.get("cart_id"));
			out.put(cid, row);
		}
		return out;
	}

	private static long linePayFen(Map<String, Object> row) {
		if ("package".equals(stringVal(row.get("activity_type"))) && row.get("packages") instanceof List<?> pkgs && !pkgs.isEmpty()) {
			return WxappH5PackageCartSupport.packageLinePayFen(row);
		}
		long tf = longVal(row.get("total_fee"));
		if (tf > 0L) {
			return tf;
		}
		return longVal(row.get("price")) * intVal(row.get("num"));
	}

	private static long packageAwareLineItemFee(Map<String, Object> cart) {
		long main = longVal(cart.get("price")) * intVal(cart.get("num"));
		if (!"package".equals(stringVal(cart.get("activity_type")))) {
			return main;
		}
		Object raw = cart.get("packages");
		if (!(raw instanceof List<?> pkgs)) {
			return main;
		}
		long sum = main;
		for (Object o : pkgs) {
			if (!(o instanceof Map<?, ?> pkg)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> child = (Map<String, Object>) pkg;
			sum += longVal(child.get("price")) * intVal(child.get("num"));
		}
		return sum;
	}

	private static void appendPromotion(Map<String, Object> cart, Map<String, Object> activity) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> promotions =
				cart.get("promotions") instanceof List<?> l ? (List<Map<String, Object>>) l : new ArrayList<>();
		if (!(cart.get("promotions") instanceof List<?>)) {
			cart.put("promotions", promotions);
		}
		promotions.add(activity);
	}

	private static void appendIdList(Map<String, Object> cart, String key, long id) {
		@SuppressWarnings("unchecked")
		List<Long> ids = cart.get(key) instanceof List<?> l ? castLongList(l) : new ArrayList<>();
		if (!(cart.get(key) instanceof List<?>)) {
			cart.put(key, ids);
		}
		ids.add(id);
	}

	@SuppressWarnings("unchecked")
	private static void appendActivityInfo(Map<String, Object> cart, Map<String, Object> desc) {
		Object existing = cart.get("activity_info");
		List<Map<String, Object>> activityInfo;
		if (existing instanceof List<?> l) {
			if (l instanceof ArrayList) {
				activityInfo = (List<Map<String, Object>>) l;
			} else {
				activityInfo = new ArrayList<>((List<Map<String, Object>>) l);
				cart.put("activity_info", activityInfo);
			}
		} else {
			activityInfo = new ArrayList<>();
			cart.put("activity_info", activityInfo);
		}
		activityInfo.add(desc);
	}

	private static List<Long> castLongList(List<?> raw) {
		List<Long> out = new ArrayList<>();
		for (Object o : raw) {
			long v = longVal(o);
			if (v > 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private static Map<String, Object> emptyTotals() {
		Map<String, Object> totals = new LinkedHashMap<>();
		totals.put("item_fee", 0L);
		totals.put("discount_fee", 0L);
		totals.put("total_fee", 0L);
		totals.put("cart_total_num", 0);
		totals.put("cart_total_count", 0);
		totals.put("cart_total_price", 0L);
		totals.put("used_activity", List.of());
		totals.put("used_activity_ids", List.of());
		totals.put("activity_grouping", List.of());
		totals.put("gift_activity", List.of());
		totals.put("plus_buy_activity", List.of());
		return totals;
	}

	private static boolean truthyChecked(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static long longVal(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o) {
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

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static final class UsedPromotion {
		private final List<Long> cartIds = new ArrayList<>();
	}
}
