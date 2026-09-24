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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.promotions.port.PointUpvaluationEligibleActivityReadPort;
import cn.shopex.ecshopx.point.service.PointMemberMoneyToPointService;
import cn.shopex.ecshopx.point.service.PointMemberPointToMoneyService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderCheckoutPointDeductService {

	private final PointUpvaluationEligibleActivityReadPort pointUpvaluationEligibleActivityReadPort;
	private final NormalOrderPointUpvaluationDeductionService normalOrderPointUpvaluationDeductionService;
	private final NormalOrderPointDeductionApplyService normalOrderPointDeductionApplyService;
	private final PointMemberMoneyToPointService pointMemberMoneyToPointService;
	private final PointMemberPointToMoneyService pointMemberPointToMoneyService;

	public NormalOrderCheckoutPointDeductService(
			PointUpvaluationEligibleActivityReadPort pointUpvaluationEligibleActivityReadPort,
			NormalOrderPointUpvaluationDeductionService normalOrderPointUpvaluationDeductionService,
			NormalOrderPointDeductionApplyService normalOrderPointDeductionApplyService,
			PointMemberMoneyToPointService pointMemberMoneyToPointService,
			PointMemberPointToMoneyService pointMemberPointToMoneyService) {
		this.pointUpvaluationEligibleActivityReadPort = pointUpvaluationEligibleActivityReadPort;
		this.normalOrderPointUpvaluationDeductionService = normalOrderPointUpvaluationDeductionService;
		this.normalOrderPointDeductionApplyService = normalOrderPointDeductionApplyService;
		this.pointMemberMoneyToPointService = pointMemberMoneyToPointService;
		this.pointMemberPointToMoneyService = pointMemberPointToMoneyService;
	}

	public void applyCheckoutPointDeduct(
			Map<String, Object> orderData,
			Map<String, Object> params,
			long companyId,
			long userId,
			Map<String, Object> pointRule,
			long memberPoint) {
		orderData.put("user_point", String.valueOf(memberPoint));
		boolean open =
				redisFlagTrue(pointRule.get("isOpenMemberPoint"))
						&& redisFlagTrue(pointRule.get("isOpenDeductPoint"));
		Optional<Map<String, Object>> activityOpt =
				pointUpvaluationEligibleActivityReadPort.getEligibleActivity(companyId, userId, "1");
		boolean hasUpvaluation =
				activityOpt.isPresent() && intVal(activityOpt.get().get("upvaluation"), 0) > 1;

		if (!open) {
			orderData.put("deduct_point_rule", deductRuleClosed());
			orderData.put("max_point", 0);
			orderData.put("limit_point", 0);
			orderData.put("max_point_ziti", 0);
			orderData.put("is_open_deduct_point", false);
			return;
		}

		long orderFenAfterFreight = longVal(orderData.get("total_fee"), 0L);
		if (hasUpvaluation) {
			Map<String, Object> act = activityOpt.get();
			Map<String, Object> pointUpvaluation = new LinkedHashMap<>();
			pointUpvaluation.put("upvaluation", act.get("upvaluation"));
			pointUpvaluation.put("uppoints", act.get("uppoints"));
			pointUpvaluation.put("max_up_point", String.valueOf(act.get("max_up_point")));
			orderData.put("pointupvaluation", pointUpvaluation);

			Map<String, Object> usePoint =
					normalOrderPointUpvaluationDeductionService.getUpTotalMaxPointDeduction(
							companyId, orderData, memberPoint, act);
			orderData.put("max_point", usePoint.get("max_point"));
			orderData.put("limit_point", usePoint.get("limit_point"));
			orderData.put("max_uppoint", usePoint.get("max_uppoint"));
			orderData.put("max_point_ziti", usePoint.get("max_point"));
			orderData.put("is_open_deduct_point", true);
			orderData.put(
					"deduct_point_rule",
					buildDeductPointRule(pointRule, orderFenAfterFreight, longVal(usePoint.get("max_money"), 0L)));
		} else {
			applyPointCapsFromOrderMaxPoint(orderData, pointRule, memberPoint, companyId);
		}

		// PHP: if pay_type==point with no amount, require full_amount then force max_point.
		// If the client already sent point_use (manual input), honor it instead of overwriting
		// to max_point — checkout may stamp pay_type=point from a previous full-deduct remaining=0.
		String payType = stringPayType(params, orderData);
		int pointUseRequested;
		if ("point".equals(payType)) {
			if (!isFullAmountDeductRule(orderData.get("deduct_point_rule"))) {
				throw new ResourceException("当前" + pointLabel(pointRule) + "不足以支付本次订单费用!");
			}
			int maxPoint = Math.max(0, intVal(orderData.get("max_point"), 0));
			int explicitUse =
					Math.max(
							0,
							intVal(
									params != null ? params.get("point_use") : null,
									intVal(orderData.get("point_use"), 0)));
			pointUseRequested = explicitUse > 0 ? Math.min(explicitUse, maxPoint) : maxPoint;
			orderData.put("point_use", pointUseRequested);
			if (params != null) {
				try {
					params.put("point_use", pointUseRequested);
				} catch (UnsupportedOperationException ignored) {
					// immutable params
				}
			}
		} else {
			pointUseRequested =
					Math.max(0, intVal(params.get("point_use"), intVal(orderData.get("point_use"), 0)));
		}
		if (pointUseRequested <= 0 || orderFenAfterFreight <= 0L) {
			return;
		}
		if (longVal(orderData.get("total_fee"), 0L) <= 0L) {
			throw new ResourceException("本单不能使用" + pointLabel(pointRule));
		}
		orderData.put("point_use", pointUseRequested);
		if (hasUpvaluation) {
			normalOrderPointUpvaluationDeductionService.getUpTotalUsePointDeduction(
					companyId, orderData, memberPoint, activityOpt.get(), pointRule);
		} else {
			normalOrderPointDeductionApplyService.apply(
					orderData, pointUseRequested, companyId, memberPoint, pointRule);
		}
		int realUse = intVal(orderData.get("real_use_point"), intVal(orderData.get("point"), 0));
		if (realUse <= 0) {
			throw new ResourceException("订单使用" + pointLabel(pointRule) + "不能低于一" + pointLabel(pointRule));
		}
		orderData.put("point", orderData.get("real_use_point"));
		rewritePayTypeToPointIfFullyPointDeducted(orderData, params);
		restoreCashPayTypeIfPartialPointDeduct(orderData, params);
	}

	/**
	 * 积分抵扣将应付现金打到 0 后，订单视为纯积分支付：落库 {@code pay_type=point}，
	 * 与后续交易单创建/退款 Job 分支对齐（避免仍写 offline_pay/wxpay 却走 0 元 localPay）。
	 */
	public static void rewritePayTypeToPointIfFullyPointDeducted(
			Map<String, Object> orderData, Map<String, Object> params) {
		if (orderData == null) {
			return;
		}
		long totalFee = longVal(orderData.get("total_fee"), 0L);
		int pointUse = intVal(orderData.get("point_use"), intVal(orderData.get("point"), 0));
		if (totalFee != 0L || pointUse <= 0) {
			return;
		}
		orderData.put("pay_type", "point");
		if (params != null) {
			try {
				params.put("pay_type", "point");
			} catch (UnsupportedOperationException ignored) {
				// immutable params map — orderData alone drives persistence
			}
		}
	}

	/**
	 * 积分未抵完应付现金时，不能再落成纯积分支付。收银台实际选中的现金方式在 {@code pay_channel}。
	 */
	static void restoreCashPayTypeIfPartialPointDeduct(
			Map<String, Object> orderData, Map<String, Object> params) {
		if (orderData == null) {
			return;
		}
		if (!"point".equals(stringPayType(params, orderData))) {
			return;
		}
		if (longVal(orderData.get("total_fee"), 0L) <= 0L) {
			return;
		}
		String cashPayType = "";
		if (params != null && params.get("pay_channel") != null) {
			cashPayType = String.valueOf(params.get("pay_channel")).trim();
		}
		if (!StringUtils.hasText(cashPayType) && orderData.get("pay_channel") != null) {
			cashPayType = String.valueOf(orderData.get("pay_channel")).trim();
		}
		if (!StringUtils.hasText(cashPayType) || "point".equals(cashPayType)) {
			return;
		}
		orderData.put("pay_type", cashPayType);
		if (params != null) {
			try {
				params.put("pay_type", cashPayType);
			} catch (UnsupportedOperationException ignored) {
				// immutable params map — orderData alone drives persistence
			}
		}
	}

	private void applyPointCapsFromOrderMaxPoint(
			Map<String, Object> od, Map<String, Object> pointRule, long memberPoint, long companyId) {
		long totalFeeFen = longVal(od.get("total_fee"), 0L);
		if (totalFeeFen <= 0L) {
			od.put("deduct_point_rule", deductRuleClosed());
			od.put("max_point", 0);
			od.put("limit_point", 0);
			od.put("max_point_ziti", 0);
			od.put("is_open_deduct_point", false);
			return;
		}
		BigDecimal deductPointRate = parsePositiveDecimal(pointRule.get("deduct_point"));
		if (deductPointRate.compareTo(BigDecimal.ZERO) <= 0) {
			od.put("deduct_point_rule", deductRuleClosed());
			od.put("max_point", 0);
			od.put("limit_point", 0);
			od.put("max_point_ziti", 0);
			od.put("is_open_deduct_point", false);
			return;
		}

		int limitPct = parsePercentInt(pointRule.get("deduct_proportion_limit"));
		BigDecimal proportion =
				BigDecimal.valueOf(limitPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
		long freightFeeFen = longVal(od.get("freight_fee"), 0L);
		long goodsPayFen = Math.max(0L, totalFeeFen - freightFeeFen);
		long goodsMaxMoneyFen = proportionMoneyFen(proportion, goodsPayFen);
		boolean canDeductFreight = phpTruthyCanDeductFreight(pointRule.get("can_deduct_freight"));

		// PHP orderMaxPoint: 自提仅商品部分；整单上限一次换算，避免分段 ceil 偏大
		long totalPointZiti = pointMemberMoneyToPointService.moneyToPoint(companyId, goodsMaxMoneyFen);
		long totalMaxMoneyFen =
				canDeductFreight ? proportionMoneyFen(proportion, totalFeeFen) : goodsMaxMoneyFen;
		long totalMoneyToPoint =
				pointMemberMoneyToPointService.moneyToPoint(companyId, totalMaxMoneyFen);

		long maxMoneyFen = totalMaxMoneyFen;
		long useLimit;
		if (memberPoint > totalMoneyToPoint) {
			useLimit = totalMoneyToPoint;
		} else {
			maxMoneyFen = pointMemberPointToMoneyService.pointToMoney(companyId, memberPoint);
			useLimit = pointMemberMoneyToPointService.moneyToPoint(companyId, maxMoneyFen);
		}
		useLimit = Math.max(0L, useLimit);
		while (useLimit > 0L && !phpMoneyOutLimit(pointRule, safeInt(useLimit), totalFeeFen)) {
			useLimit--;
		}

		// 自提上限按「货款可兑积分」再与会员余额取小，避免余额为 0 时仍返回货款换算值
		long zitiUseLimit;
		if (memberPoint > totalPointZiti) {
			zitiUseLimit = totalPointZiti;
		} else {
			long zitiMoneyFen = pointMemberPointToMoneyService.pointToMoney(companyId, memberPoint);
			zitiUseLimit = pointMemberMoneyToPointService.moneyToPoint(companyId, zitiMoneyFen);
		}
		zitiUseLimit = Math.max(0L, zitiUseLimit);

		int maxPoint = safeInt(useLimit);
		int limitPoint = safeInt(totalMoneyToPoint);
		int maxPointZiti = safeInt(zitiUseLimit);
		od.put("max_point", maxPoint);
		od.put("limit_point", limitPoint);
		od.put("max_point_ziti", maxPointZiti);
		od.put("is_open_deduct_point", true);
		od.put("deduct_point_rule", buildDeductPointRule(pointRule, totalFeeFen, maxMoneyFen));

		if (maxPoint > 0) {
			tryApplyMaxPointFromPointDeductionPreview(od, pointRule, memberPoint, companyId, maxPoint);
		}
	}

	private void tryApplyMaxPointFromPointDeductionPreview(
			Map<String, Object> od,
			Map<String, Object> pointRule,
			long memberPoint,
			long companyId,
			int provisionalMaxPoint) {
		Map<String, Object> temp = shallowCopyOrderDataForPointTry(od);
		temp.put("point_use", provisionalMaxPoint);
		try {
			normalOrderPointDeductionApplyService.apply(
					temp, provisionalMaxPoint, companyId, memberPoint, pointRule);
			int realUse = intVal(temp.get("real_use_point"), 0);
			if (realUse > 0) {
				od.put("max_point", realUse);
				int ziti = intVal(temp.get("real_use_point_ziti"), realUse);
				od.put("max_point_ziti", ziti);
			}
		} catch (ResourceException ignored) {
			// preview try: keep orderMaxPoint caps when deduction simulation fails
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> shallowCopyOrderDataForPointTry(Map<String, Object> source) {
		Map<String, Object> copy = new LinkedHashMap<>(source);
		Object itemsRaw = source.get("items");
		if (itemsRaw instanceof List<?> list) {
			List<Map<String, Object>> itemsCopy = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> row) {
					itemsCopy.add(new LinkedHashMap<>((Map<String, Object>) row));
				}
			}
			copy.put("items", itemsCopy);
		}
		return copy;
	}

	private static long proportionMoneyFen(BigDecimal proportion, long moneyFen) {
		return proportion.multiply(BigDecimal.valueOf(moneyFen)).longValue();
	}

	private static boolean phpMoneyOutLimit(Map<String, Object> pointRule, int point, long totalFeeFen) {
		int money = phpPointToMoneyFen(pointRule, point);
		int moneyMinus1 = point > 0 ? phpPointToMoneyFen(pointRule, point - 1) : 0;
		BigDecimal limitPct =
				BigDecimal.valueOf(parsePercentInt(pointRule.get("deduct_proportion_limit")))
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		BigDecimal limit =
				limitPct.multiply(BigDecimal.valueOf(totalFeeFen)).setScale(2, RoundingMode.HALF_UP);
		return !(limit.compareTo(BigDecimal.valueOf(money)) < 0
				&& limit.compareTo(BigDecimal.valueOf(moneyMinus1)) < 0);
	}

	private static int phpPointToMoneyFen(Map<String, Object> pointRule, int points) {
		if (points <= 0) {
			return 0;
		}
		if (!redisFlagTrue(pointRule.get("isOpenDeductPoint"))) {
			return 0;
		}
		BigDecimal deductPoint = parsePositiveDecimal(pointRule.get("deduct_point"));
		if (deductPoint.compareTo(BigDecimal.ZERO) <= 0) {
			return 0;
		}
		return BigDecimal.valueOf(points)
				.divide(deductPoint, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(2, RoundingMode.HALF_UP)
				.intValue();
	}

	private static boolean phpTruthyCanDeductFreight(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(raw).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static int safeInt(long v) {
		return (int) Math.min(Math.max(0L, v), Integer.MAX_VALUE);
	}

	private static Map<String, Object> buildDeductPointRule(
			Map<String, Object> pointRule, long orderTotalFeeFen, long maxMoneyFen) {
		Map<String, Object> dpr = new LinkedHashMap<>();
		dpr.put("deduct_proportion_limit", pointRule.get("deduct_proportion_limit"));
		dpr.put("deduct_point", pointRule.get("deduct_point"));
		dpr.put("full_amount", orderTotalFeeFen > 0L && orderTotalFeeFen == maxMoneyFen);
		return dpr;
	}

	private static Map<String, Object> deductRuleClosed() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("deduct_proportion_limit", 0);
		m.put("deduct_point", 0);
		m.put("full_amount", false);
		return m;
	}

	private static String pointLabel(Map<String, Object> pointRule) {
		Object nameObj = pointRule.get("name");
		if (nameObj != null && StringUtils.hasText(String.valueOf(nameObj).trim())) {
			return String.valueOf(nameObj).trim();
		}
		return "积分";
	}

	/** Prefer request params pay_type (PHP $params['pay_type']), fall back to orderData. */
	private static String stringPayType(Map<String, Object> params, Map<String, Object> orderData) {
		Object fromParams = params != null ? params.get("pay_type") : null;
		if (fromParams != null && StringUtils.hasText(String.valueOf(fromParams))) {
			return String.valueOf(fromParams).trim();
		}
		Object fromOrder = orderData != null ? orderData.get("pay_type") : null;
		return fromOrder == null ? "" : String.valueOf(fromOrder).trim();
	}

	@SuppressWarnings("unchecked")
	private static boolean isFullAmountDeductRule(Object deductPointRule) {
		if (!(deductPointRule instanceof Map<?, ?> raw)) {
			return false;
		}
		Object fa = ((Map<String, Object>) raw).get("full_amount");
		if (fa instanceof Boolean b) {
			return b;
		}
		if (fa instanceof Number n) {
			return n.intValue() != 0;
		}
		if (fa == null) {
			return false;
		}
		String s = String.valueOf(fa).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static boolean redisFlagTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(v).trim());
	}

	private static int parsePercentInt(Object raw) {
		if (raw == null) {
			return 100;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			return v < 1 ? 100 : Math.min(v, 100);
		} catch (NumberFormatException e) {
			return 100;
		}
	}

	private static BigDecimal parsePositiveDecimal(Object raw) {
		if (raw == null) {
			return BigDecimal.ONE;
		}
		try {
			BigDecimal bd = new BigDecimal(String.valueOf(raw).trim());
			return bd.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : bd;
		} catch (NumberFormatException e) {
			return BigDecimal.ONE;
		}
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
}
