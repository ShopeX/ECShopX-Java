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

package cn.shopex.ecshopx.kaquan.service.order.normal;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import cn.shopex.ecshopx.kaquan.service.discount.AdminUserCardListFacadeService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountCardMatchedAmount;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountCardMatchedAmountService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.CartItemMoneyRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderCheckoutCouponFacade {

	private static final Logger log = LoggerFactory.getLogger(NormalOrderCheckoutCouponFacade.class);

	private final AdminUserCardListFacadeService adminUserCardListFacadeService;
	private final UserDiscountCardMatchedAmountService userDiscountCardMatchedAmountService;

	public NormalOrderCheckoutCouponFacade(
			AdminUserCardListFacadeService adminUserCardListFacadeService,
			UserDiscountCardMatchedAmountService userDiscountCardMatchedAmountService) {
		this.adminUserCardListFacadeService = adminUserCardListFacadeService;
		this.userDiscountCardMatchedAmountService = userDiscountCardMatchedAmountService;
	}

	public void applyOptimalCouponAndSilentDeduction(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		int notUse = intVal(pr.get("not_use_coupon"), 0);
		if (notUse != 0) {
			return;
		}
		long userId = longVal(pr.get("user_id"), 0L);
		if (userId <= 0L) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long companyId = longVal(od.get("company_id"), longVal(pr.get("company_id"), 0L));
		long distributorId = longVal(od.get("distributor_id"), longVal(pr.get("distributor_id"), 0L));
		long operatorId = longVal(pr.get("operator_id"), 0L);
		long totalBefore = longVal(od.get("total_fee"), 0L);
		if (totalBefore <= 0L) {
			return;
		}
		long freightFen = longVal(od.get("freight_fee"), 0L);
		long goodsTotalForCoupon = Math.max(0L, totalBefore - freightFen);

		Map<Long, CartItemMoneyRow> cartMoney = buildCartMoneyRows(od.get("items"));
		String explicitCode = stringVal(pr.get("coupon_discount")).trim();
		boolean explicitRequested = StringUtils.hasText(explicitCode) && !"0".equals(explicitCode);
		Map<String, Object> chosen = null;
		if (explicitRequested) {
			chosen =
					findCardRowByCode(
							companyId,
							operatorId,
							userId,
							distributorId,
							explicitCode,
							goodsTotalForCoupon,
							cartMoney,
							pr);
		} else {
			chosen =
					pickBestValidCard(
							companyId, operatorId, userId, distributorId, goodsTotalForCoupon, cartMoney, pr);
		}
		if (chosen == null) {
			if (explicitRequested) {
				pr.put("coupon_discount", "0");
			}
			return;
		}
		String code = explicitRequested ? explicitCode : stringVal(firstKey(chosen, "code")).trim();
		if (!StringUtils.hasText(code)) {
			return;
		}
		try {
			boolean applied =
					applyDeductionToOrderData(
							od, chosen, totalBefore, explicitRequested ? explicitCode : null, companyId, cartMoney);
			if (applied) {
				pr.put("coupon_discount", code);
			} else if (explicitRequested) {
				pr.put("coupon_discount", "0");
			}
		} catch (RuntimeException e) {
			log.debug("checkout coupon deduction skipped: {}", e.toString());
			if (explicitRequested) {
				pr.put("coupon_discount", "0");
			}
		}
	}

	private Map<String, Object> findCardRowByCode(
			long companyId,
			long operatorId,
			long userId,
			long distributorId,
			String code,
			long totalBefore,
			Map<Long, CartItemMoneyRow> cartMoney,
			Map<String, Object> pr) {
		Map<String, Object> lists =
				adminUserCardListFacadeService.buildList(
						companyId, operatorId, userId, distributorId, code, "", String.valueOf(totalBefore), true, 1, 50,
						couponLineItemsInsteadOfOperatorCart(pr, cartMoney));
		Object listObj = lists.get("list");
		if (!(listObj instanceof List<?> raw)) {
			return null;
		}
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> m)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> row = (Map<String, Object>) m;
			if (!isCardUsable(row)) {
				continue;
			}
			return row;
		}
		return null;
	}

	private Map<String, Object> pickBestValidCard(
			long companyId,
			long operatorId,
			long userId,
			long distributorId,
			long totalBefore,
			Map<Long, CartItemMoneyRow> cartMoney,
			Map<String, Object> pr) {
		Map<String, Object> lists =
				adminUserCardListFacadeService.buildList(
						companyId, operatorId, userId, distributorId, "", "", String.valueOf(totalBefore), true, 1, 50,
						couponLineItemsInsteadOfOperatorCart(pr, cartMoney));
		Object listObj = lists.get("list");
		if (!(listObj instanceof List<?> raw)) {
			return null;
		}
		Map<String, Object> best = null;
		long bestSaving = -1L;
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> m)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> row = (Map<String, Object>) m;
			if (!isCardUsable(row)) {
				continue;
			}
			long saving = estimateSavingFen(row, totalBefore, cartMoney, companyId);
			if (saving > bestSaving) {
				bestSaving = saving;
				best = row;
			}
		}
		return bestSaving > 0L ? best : null;
	}

	/**
	 * H5 / wxapp checkout fills {@code orderData.items} from {@code orders_cart} (or other non-operator sources) while
	 * {@code companys_operator_cart} may be empty. When we already have priced lines, do not reload the operator cart
	 * for coupon filtering (that path uses submit semantics and throws if empty).
	 */
	private static Map<Long, CartItemMoneyRow> couponLineItemsInsteadOfOperatorCart(
			Map<String, Object> pr,
			Map<Long, CartItemMoneyRow> cartMoney) {
		if (!Boolean.TRUE.equals(pr.get("is_online_order"))) {
			return null;
		}
		if (cartMoney.isEmpty()) {
			return null;
		}
		return cartMoney;
	}

	private static boolean isCardUsable(Map<String, Object> row) {
		if (Boolean.FALSE.equals(row.get("valid"))) {
			return false;
		}
		Object coupon = row.get("coupon");
		if (coupon instanceof Map<?, ?> cm && Boolean.FALSE.equals(cm.get("valid"))) {
			return false;
		}
		return true;
	}

	private long estimateSavingFen(Map<String, Object> row, long orderTotalFen, Map<Long, CartItemMoneyRow> cartMoney, long companyId) {
		String cardType = stringVal(firstKey(row, "card_type"));
		int leastCost = intVal(firstKey(row, "least_cost"));
		long matched = matchedAmountForCard(row, cartMoney, orderTotalFen, companyId);
		if (leastCost > 0 && matched < leastCost) {
			return 0L;
		}
		if (matched <= 0L && !cartMoney.isEmpty()) {
			return 0L;
		}
		long basis = cartMoney.isEmpty() ? orderTotalFen : matched;
		if (basis <= 0L) {
			return 0L;
		}
		if ("cash".equals(cardType)) {
			int reduce = intVal(firstKey(row, "reduce_cost"));
			return Math.min(Math.max(reduce, 0), basis);
		}
		if ("discount".equals(cardType)) {
			int disc = intVal(firstKey(row, "discount"));
			if (disc <= 0 || disc >= 100) {
				return 0L;
			}
			return basis * disc / 100L;
		}
		return 0L;
	}

	private long matchedAmountForCard(
			Map<String, Object> row, Map<Long, CartItemMoneyRow> cartMoney, long orderTotalFen, long companyId) {
		if (cartMoney.isEmpty()) {
			return orderTotalFen;
		}
		Map<Long, Long> fees = new LinkedHashMap<>();
		for (Map.Entry<Long, CartItemMoneyRow> e : cartMoney.entrySet()) {
			if (e.getKey() == null) {
				continue;
			}
			CartItemMoneyRow r = e.getValue();
			fees.put(e.getKey(), r == null ? 0L : r.getTotalFeeFen());
		}
		int useBound = UserDiscountCardMatchedAmount.useBoundOf(row);
		Object rel = UserDiscountCardMatchedAmount.relItemIdsOf(row);
		Map<Long, CouponCartItemScope> scopes = userDiscountCardMatchedAmountService.loadScopes(companyId, fees.keySet());
		return UserDiscountCardMatchedAmount.feeFen(useBound, rel, fees, scopes);
	}

	private boolean applyDeductionToOrderData(
			Map<String, Object> od,
			Map<String, Object> cardRow,
			long totalBefore,
			String explicitCouponCode,
			long companyId,
			Map<Long, CartItemMoneyRow> cartMoney) {
		String cardType = stringVal(firstKey(cardRow, "card_type"));
		long freightFen = longVal(od.get("freight_fee"), 0L);
		long goodsTotal = Math.max(0L, totalBefore - freightFen);
		long matched = matchedAmountForCard(cardRow, cartMoney, goodsTotal, companyId);
		int leastCost = intVal(firstKey(cardRow, "least_cost"));
		if (leastCost > 0 && matched < leastCost) {
			return false;
		}
		Set<Long> matchedItemIds = matchedItemIdSet(cardRow, cartMoney, companyId);
		long deduct;
		Map<String, Object> orderCouponDesc;
		if ("discount".equals(cardType)) {
			DiscountApplyResult applied = applyDiscountCouponToLines(od, cardRow, explicitCouponCode, matchedItemIds);
			if (applied.deductFen() <= 0L) {
				return false;
			}
			deduct = applied.deductFen();
			orderCouponDesc = applied.orderDiscountDesc();
		} else if ("cash".equals(cardType)) {
			int reduce = intVal(firstKey(cardRow, "reduce_cost"));
			if (reduce <= 0) {
				return false;
			}
			if (matched <= 0L) {
				return false;
			}
			if (reduce >= goodsTotal && goodsTotal > 0L) {
				return false;
			}
			deduct = Math.min(reduce, matched);
			if (deduct <= 0L) {
				return false;
			}
			orderCouponDesc = buildCashOrderCouponDesc(cardRow, deduct, explicitCouponCode);
			applyCashCouponToLines(od, cardRow, deduct, matched, explicitCouponCode, matchedItemIds);
		} else {
			return false;
		}
		long newTotal = Math.max(0L, totalBefore - deduct);
		od.put("total_fee", newTotal);
		int add = (int) Math.min(deduct, (long) Integer.MAX_VALUE);
		od.put("coupon_discount", intVal(od.get("coupon_discount"), 0) + add);
		od.put("discount_fee", intVal(od.get("discount_fee"), 0) + add);
		od.put("coupon_info", orderCouponDesc);
		appendOrderDiscountInfo(od, orderCouponDesc);
		return true;
	}

	private Set<Long> matchedItemIdSet(Map<String, Object> cardRow, Map<Long, CartItemMoneyRow> cartMoney, long companyId) {
		if (cartMoney == null || cartMoney.isEmpty()) {
			return Set.of();
		}
		int useBound = UserDiscountCardMatchedAmount.useBoundOf(cardRow);
		Object rel = UserDiscountCardMatchedAmount.relItemIdsOf(cardRow);
		Map<Long, CouponCartItemScope> scopes =
				userDiscountCardMatchedAmountService.loadScopes(companyId, cartMoney.keySet());
		return new HashSet<>(UserDiscountCardMatchedAmount.itemIds(useBound, rel, cartMoney.keySet(), scopes));
	}

	private record DiscountApplyResult(long deductFen, Map<String, Object> orderDiscountDesc) {}

	@SuppressWarnings("unchecked")
	private static DiscountApplyResult applyDiscountCouponToLines(
			Map<String, Object> od,
			Map<String, Object> cardRow,
			String explicitCouponCode,
			Set<Long> matchedItemIds) {
		int disc = intVal(firstKey(cardRow, "discount"));
		if (disc <= 0 || disc >= 100) {
			return new DiscountApplyResult(0L, Map.of());
		}
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList)) {
			return new DiscountApplyResult(0L, Map.of());
		}
		List<Map<String, Object>> eligible = new ArrayList<>();
		long orderTotalFee = 0L;
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> lineRaw)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) lineRaw;
			if ("gift".equals(stringVal(line.get("order_item_type")))) {
				continue;
			}
			if (Boolean.FALSE.equals(line.get("coupon_valid"))) {
				continue;
			}
			if (!matchedItemIds.contains(longVal(line.get("item_id"), 0L))) {
				continue;
			}
			long payFee = longVal(line.get("total_fee"), 0L);
			if (payFee < 100L) {
				continue;
			}
			eligible.add(line);
			orderTotalFee += payFee;
		}
		if (eligible.isEmpty() || orderTotalFee <= 0L) {
			return new DiscountApplyResult(0L, Map.of());
		}

		long basisFee = orderTotalFee;
		int mostCost = intVal(firstKey(cardRow, "most_cost"));
		if (mostCost > 0 && basisFee > mostCost) {
			basisFee = mostCost;
		}
		BigDecimal rate = BigDecimal.valueOf(disc).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
		long totalDiscountFee =
				BigDecimal.valueOf(basisFee).multiply(rate).setScale(0, RoundingMode.DOWN).longValue();
		if (totalDiscountFee <= 0L || totalDiscountFee >= basisFee) {
			return new DiscountApplyResult(0L, Map.of());
		}

		int leastCost = intVal(firstKey(cardRow, "least_cost"));
		String rule = "折扣券满" + (leastCost / 100) + "元减" + disc + "%优惠";
		eligible.sort(Comparator.comparingLong(line -> longVal(line.get("total_fee"), 0L)));

		long discountFeePlus = 0L;
		int orderItemCount = eligible.size();
		long couponDiscount = 0L;
		for (int key = 0; key < orderItemCount; key++) {
			Map<String, Object> line = eligible.get(key);
			long payFee = longVal(line.get("total_fee"), 0L);
			long lineDiscount;
			if (key == orderItemCount - 1) {
				lineDiscount = totalDiscountFee - discountFeePlus;
			} else if (orderItemCount == 1) {
				lineDiscount = totalDiscountFee;
			} else {
				BigDecimal percent = bcdivRound4(payFee, orderTotalFee);
				lineDiscount = roundBcmul2(totalDiscountFee, percent);
				discountFeePlus += lineDiscount;
			}
			if (lineDiscount <= 0L) {
				continue;
			}
			line.put("total_fee", (int) Math.min(payFee - lineDiscount, Integer.MAX_VALUE));
			line.put("discount_fee", intVal(line.get("discount_fee"), 0) + (int) Math.min(lineDiscount, Integer.MAX_VALUE));
			line.put("coupon_discount", String.valueOf(lineDiscount));
			line.put("coupon_valid", true);
			appendLineDiscountInfo(
					line,
					buildCouponDiscountInfoEntry(cardRow, explicitCouponCode, lineDiscount, rule, "coupon_discount"));
			couponDiscount += lineDiscount;
		}
		if (couponDiscount <= 0L) {
			return new DiscountApplyResult(0L, Map.of());
		}
		Map<String, Object> orderDesc =
				buildCouponDiscountInfoEntry(cardRow, explicitCouponCode, couponDiscount, rule, "coupon_discount");
		return new DiscountApplyResult(couponDiscount, orderDesc);
	}

	private static BigDecimal bcdivRound4(long numerator, long denominator) {
		if (denominator <= 0L) {
			return BigDecimal.ZERO;
		}
		BigDecimal ratio =
				BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 6, RoundingMode.DOWN);
		return ratio.setScale(4, RoundingMode.HALF_UP);
	}

	private static long roundBcmul2(long discountFee, BigDecimal percent) {
		return BigDecimal.valueOf(discountFee)
				.multiply(percent)
				.setScale(2, RoundingMode.DOWN)
				.setScale(0, RoundingMode.HALF_UP)
				.longValue();
	}

	@SuppressWarnings("unchecked")
	private static void applyCashCouponToLines(
			Map<String, Object> od,
			Map<String, Object> cardRow,
			long deduct,
			long goodsTotal,
			String explicitCouponCode,
			Set<Long> matchedItemIds) {
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList) || goodsTotal <= 0L) {
			return;
		}
		List<Map<String, Object>> eligible = new ArrayList<>();
		long eligibleTotal = 0L;
		for (Object o : itemList) {
			if (!(o instanceof Map<?, ?> lineRaw)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) lineRaw;
			if ("gift".equals(stringVal(line.get("order_item_type")))) {
				continue;
			}
			if (!matchedItemIds.contains(longVal(line.get("item_id"), 0L))) {
				continue;
			}
			long payFee = longVal(line.get("total_fee"), 0L);
			if (payFee < 100L) {
				continue;
			}
			eligible.add(line);
			eligibleTotal += payFee;
		}
		if (eligible.isEmpty()) {
			return;
		}
		int leastCost = intVal(firstKey(cardRow, "least_cost"));
		int reduce = intVal(firstKey(cardRow, "reduce_cost"));
		String rule = "代金券满" + (leastCost / 100) + "元减" + (reduce / 100) + "元";
		long remaining = deduct;
		for (int i = 0; i < eligible.size(); i++) {
			Map<String, Object> line = eligible.get(i);
			long payFee = longVal(line.get("total_fee"), 0L);
			long lineDeduct;
			if (i == eligible.size() - 1) {
				lineDeduct = remaining;
			} else {
				lineDeduct = Math.round((double) deduct * payFee / eligibleTotal);
				remaining -= lineDeduct;
			}
			if (lineDeduct <= 0L) {
				continue;
			}
			line.put("total_fee", (int) Math.min(payFee - lineDeduct, Integer.MAX_VALUE));
			line.put("discount_fee", intVal(line.get("discount_fee"), 0) + (int) Math.min(lineDeduct, Integer.MAX_VALUE));
			line.put("coupon_discount", String.valueOf(lineDeduct));
			appendLineDiscountInfo(
					line,
					buildCouponDiscountInfoEntry(cardRow, explicitCouponCode, lineDeduct, rule, "cash_discount"));
		}
	}

	private static String couponCodeForDesc(Map<String, Object> cardRow, String explicitCouponCode) {
		if (StringUtils.hasText(explicitCouponCode)) {
			return explicitCouponCode.trim();
		}
		return stringVal(firstKey(cardRow, "code"));
	}

	private static Map<String, Object> buildCashOrderCouponDesc(
			Map<String, Object> cardRow, long deduct, String explicitCouponCode) {
		int leastCost = intVal(firstKey(cardRow, "least_cost"));
		int reduce = intVal(firstKey(cardRow, "reduce_cost"));
		return buildCouponDiscountInfoEntry(
				cardRow,
				explicitCouponCode,
				deduct,
				"代金券满" + (leastCost / 100) + "元减" + (reduce / 100) + "元",
				"cash_discount");
	}

	private static Map<String, Object> buildCouponDiscountInfoEntry(
			Map<String, Object> cardRow,
			String explicitCouponCode,
			long discountFeeFen,
			String rule,
			String type) {
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("id", stringVal(firstKey(cardRow, "card_id")));
		entry.put("coupon_code", couponCodeForDesc(cardRow, explicitCouponCode));
		entry.put("info", stringVal(firstKey(cardRow, "title")));
		entry.put("dm_card_code", stringVal(firstKey(cardRow, "dm_card_code")));
		entry.put("type", type);
		entry.put("rule", rule);
		entry.put("discount_fee", String.valueOf(discountFeeFen));
		return entry;
	}

	@SuppressWarnings("unchecked")
	private static void appendLineDiscountInfo(Map<String, Object> line, Map<String, Object> entry) {
		Object raw = line.get("discount_info");
		List<Map<String, Object>> list;
		if (raw instanceof List<?> existing) {
			list = new ArrayList<>();
			for (Object el : existing) {
				if (el instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		} else {
			list = new ArrayList<>();
		}
		list.add(entry);
		line.put("discount_info", list);
	}

	@SuppressWarnings("unchecked")
	private static void appendOrderDiscountInfo(Map<String, Object> od, Map<String, Object> entry) {
		Object raw = od.get("discount_info");
		List<Map<String, Object>> list;
		if (raw instanceof List<?> existing) {
			list = new ArrayList<>();
			for (Object el : existing) {
				if (el instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		} else if (raw instanceof Map<?, ?> map) {
			list = new ArrayList<>();
			for (Object v : map.values()) {
				if (v instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		} else {
			list = new ArrayList<>();
		}
		list.add(new LinkedHashMap<>(entry));
		od.put("discount_info", list);
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, CartItemMoneyRow> buildCartMoneyRows(Object itemsRaw) {
		Map<Long, CartItemMoneyRow> out = new LinkedHashMap<>();
		if (!(itemsRaw instanceof List<?> list)) {
			return out;
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> line)) {
				continue;
			}
			long itemId = longVal(line.get("item_id"), 0L);
			if (itemId <= 0L) {
				continue;
			}
			long num = longVal(line.get("num"), 1L);
			long lineTotal = longVal(line.get("total_fee"), 0L);
			if (lineTotal <= 0L) {
				lineTotal = longVal(line.get("item_fee"), 0L);
			}
			out.put(itemId, new CartItemMoneyRow(num, lineTotal));
		}
		return out;
	}

	private static Object firstKey(Map<String, Object> m, String key) {
		if (m.containsKey(key)) {
			return m.get(key);
		}
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v) {
		return intVal(v, 0);
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
