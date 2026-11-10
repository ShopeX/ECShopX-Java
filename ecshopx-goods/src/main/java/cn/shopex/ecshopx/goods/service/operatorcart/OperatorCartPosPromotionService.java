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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonDistributorCartFormatAndTotalService;
import cn.shopex.ecshopx.goods.service.cart.wxapp.WxappH5DistributorCartPromotionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * POS 购物车促销应用（ECX-10719）：复用小程序促销计算（WxappH5DistributorCartPromotionService），
 * 自动应用满减、满折与限时特惠，并把限时特惠聚合补进 used_activity / used_activity_ids /
 * activity_grouping。仅 POS 链路调用，小程序与导购端产出保持不变。
 */
@Service
public class OperatorCartPosPromotionService {

	private static final String TAG_LIMITED_TIME_SALE = "限时优惠";

	private final WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService;
	private final SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService;

	public OperatorCartPosPromotionService(
			WxappH5DistributorCartPromotionService wxappH5DistributorCartPromotionService,
			SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService) {
		this.wxappH5DistributorCartPromotionService = wxappH5DistributorCartPromotionService;
		this.salespersonDistributorCartFormatAndTotalService = salespersonDistributorCartFormatAndTotalService;
	}

	/**
	 * POS 购物车自动应用促销入口（ECX-10719）：对每个 shop 桶的行调用小程序促销计算
	 * （满减、满折自动算价，限时特惠改价；userDevice 传 "pos"，避开 "pc" 跳过限时特惠的分支），
	 * 聚合限时特惠补进 totals 三个展示字段后合并回桶。仅影响本 cartData，不落库。
	 */
	public void applyPosPromotions(long companyId, long userId, Map<String, Object> cartData) {
		if (cartData == null) {
			return;
		}
		if (!(cartData.get("valid_cart") instanceof List<?> shopBlocks) || shopBlocks.isEmpty()) {
			return;
		}
		for (Object blockObj : shopBlocks) {
			if (!(blockObj instanceof Map<?, ?> blockRaw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> shop = (Map<String, Object>) blockRaw;
			if (!(shop.get("list") instanceof List<?> listRaw) || listRaw.isEmpty()) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> lines = (List<Map<String, Object>>) listRaw;
			Map<String, Object> totals =
					wxappH5DistributorCartPromotionService.apply(companyId, userId, lines, "pos");
			appendLimitedTimeSaleTotals(totals, lines);
			salespersonDistributorCartFormatAndTotalService.mergePromotionTotals(List.of(shop), totals);
		}
	}

	/**
	 * 行级 {@code limitedTimeSaleAct} 按 activity_id（seckill_id）聚合并追加进 totals：
	 * {@code cart_ids} 为全部命中行（对齐小程序满减 grouping 口径），{@code discount_fee}
	 * 仅累计勾选行（对齐店铺 discount_fee 口径），优惠总额大于 0 时才写入 used_activity。
	 * totals 内的三个列表字段兼容不可变 List（emptyTotals 产出）。
	 */
	public void appendLimitedTimeSaleTotals(Map<String, Object> totals, List<Map<String, Object>> cartLines) {
		if (totals == null || cartLines == null || cartLines.isEmpty()) {
			return;
		}
		Map<Long, LimitedSaleAggregation> byActivityId = new LinkedHashMap<>();
		for (Map<String, Object> line : cartLines) {
			if (!(line.get("limitedTimeSaleAct") instanceof Map<?, ?> actRaw)) {
				continue;
			}
			long activityId = longVal(actRaw.get("activity_id"));
			if (activityId <= 0L) {
				continue;
			}
			LimitedSaleAggregation agg =
					byActivityId.computeIfAbsent(activityId, k -> new LimitedSaleAggregation(actRaw));
			agg.cartIds.add(longVal(line.get("cart_id")));
			if (truthyChecked(line.get("is_checked"))) {
				agg.discountFee += limitedSaleLineDiscountFee(line);
			}
		}
		if (byActivityId.isEmpty()) {
			return;
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> usedActivity = mutableList(totals.get("used_activity"));
		totals.put("used_activity", usedActivity);
		@SuppressWarnings("unchecked")
		List<Long> usedActivityIds = mutableLongList(totals.get("used_activity_ids"));
		totals.put("used_activity_ids", usedActivityIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> activityGrouping = mutableList(totals.get("activity_grouping"));
		totals.put("activity_grouping", activityGrouping);

		for (LimitedSaleAggregation agg : byActivityId.values()) {
			if (agg.discountFee > 0L) {
				Map<String, Object> used = new LinkedHashMap<>();
				used.put("activity_id", agg.activityId);
				used.put("activity_name", agg.activityName);
				usedActivity.add(used);
				usedActivityIds.add(agg.activityId);
			}
			Map<String, Object> grouping = new LinkedHashMap<>();
			grouping.put("activity_id", agg.activityId);
			grouping.put("activity_name", agg.activityName);
			grouping.put("activity_tag", agg.activityTag);
			grouping.put("activity_type", "limited_time_sale");
			grouping.put("cart_ids", new ArrayList<>(agg.cartIds));
			grouping.put("discount_fee", Long.toString(agg.discountFee));
			activityGrouping.add(grouping);
		}
	}

	private static long limitedSaleLineDiscountFee(Map<String, Object> line) {
		if (line.get("activity_info") instanceof List<?> infos) {
			for (Object o : infos) {
				if (o instanceof Map<?, ?> info
						&& "limited_time_sale".equals(String.valueOf(info.get("type")))) {
					return longVal(info.get("discount_fee"));
				}
			}
		}
		long price = longVal(line.get("price"));
		long activityPrice = longVal(line.get("activity_price"));
		return Math.max(0L, price - activityPrice) * longVal(line.get("num"));
	}

	private static List<Map<String, Object>> mutableList(Object raw) {
		if (raw instanceof List<?> l) {
			List<Map<String, Object>> out = new ArrayList<>(l.size());
			for (Object o : l) {
				if (o instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					Map<String, Object> row = (Map<String, Object>) m;
					out.add(row);
				}
			}
			return out;
		}
		return new ArrayList<>();
	}

	private static List<Long> mutableLongList(Object raw) {
		if (raw instanceof List<?> l) {
			List<Long> out = new ArrayList<>(l.size());
			for (Object o : l) {
				long v = longVal(o);
				if (v > 0L) {
					out.add(v);
				}
			}
			return out;
		}
		return new ArrayList<>();
	}

	private static boolean truthyChecked(Object v) {
		if (v == null || Boolean.FALSE.equals(v)) {
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

	private static final class LimitedSaleAggregation {
		private final long activityId;
		private final String activityName;
		private final String activityTag;
		private final List<Long> cartIds = new ArrayList<>();
		private long discountFee = 0L;

		private LimitedSaleAggregation(Map<?, ?> act) {
			this.activityId = longVal(act.get("activity_id"));
			Object name = act.get("marketing_name");
			this.activityName = name != null ? name.toString() : "";
			Object tag = act.get("promotion_tag");
			this.activityTag = tag != null && !tag.toString().isBlank()
					? tag.toString()
					: TAG_LIMITED_TIME_SALE;
		}
	}
}
