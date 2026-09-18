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
import cn.shopex.ecshopx.point.service.PointMemberMoneyToPointService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderPointDeductionApplyService {

	private final PointMemberMoneyToPointService pointMemberMoneyToPointService;

	public NormalOrderPointDeductionApplyService(PointMemberMoneyToPointService pointMemberMoneyToPointService) {
		this.pointMemberMoneyToPointService = pointMemberMoneyToPointService;
	}

	@SuppressWarnings("unchecked")
	public void apply(
			Map<String, Object> orderData,
			int totalPointUse,
			long companyId,
			long memberPoint,
			Map<String, Object> pointRule) {
		if (totalPointUse <= 0) {
			return;
		}
		long orderTotalFen = longVal(orderData.get("total_fee"), 0L);
		if (orderTotalFen <= 0L) {
			throw new ResourceException(pointLabel(pointRule) + "抵扣不适用当前订单金额");
		}
		if (memberPoint < (long) totalPointUse) {
			throw new ResourceException(pointLabel(pointRule) + "不足");
		}
		if (!phpMoneyOutLimit(pointRule, totalPointUse, orderTotalFen)) {
			throw new ResourceException("超出本单可用" + pointLabel(pointRule) + "抵扣");
		}

		Object itemsRaw = orderData.get("items");
		if (!(itemsRaw instanceof List<?> rawList) || rawList.isEmpty()) {
			return;
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Object o : rawList) {
			if (o instanceof Map<?, ?> m) {
				items.add((Map<String, Object>) m);
			}
		}
		if (items.isEmpty()) {
			return;
		}

		items.sort((a, b) -> Long.compare(lineAmountFen(b), lineAmountFen(a)));

		long freightFeeFen = longVal(orderData.get("freight_fee"), 0L);
		long lastMoneyForIntegral = orderTotalFen - freightFeeFen;

		int totalFeeByPointFee = phpPointToMoneyFen(pointRule, totalPointUse);
		if (totalFeeByPointFee > orderTotalFen) {
			totalFeeByPointFee = (int) orderTotalFen;
		}
		long itemFeeByPointFee =
				totalFeeByPointFee >= lastMoneyForIntegral ? lastMoneyForIntegral : totalFeeByPointFee;

		long itemPointUse = pointMemberMoneyToPointService.moneyToPoint(companyId, itemFeeByPointFee);
		if (itemPointUse >= (long) totalPointUse) {
			itemPointUse = totalPointUse;
		}

		boolean canDeductFreight = phpTruthyCanDeductFreight(pointRule.get("can_deduct_freight"));
		long pointFreightUse = (long) totalPointUse - itemPointUse;
		long pointFreightFee = (long) totalFeeByPointFee - itemFeeByPointFee;
		if (canDeductFreight) {
			orderData.put("freight_point", pointFreightUse);
			orderData.put("freight_point_fee", pointFreightFee);
			long ff = freightFeeFen - pointFreightFee;
			orderData.put("freight_fee", Math.max(0L, ff));
		}

		long totalItemFee = 0L;
		for (Map<String, Object> item : items) {
			totalItemFee += longVal(item.get("total_fee"), 0L);
		}

		int remainingPoints = (int) itemPointUse;
		long remainingFee = itemFeeByPointFee;
		BigDecimal totalItemFeeBd =
				totalItemFee > 0L ? BigDecimal.valueOf(totalItemFee) : BigDecimal.ZERO;

		for (int k = 0; k < items.size(); k++) {
			Map<String, Object> itemInfo = items.get(k);
			long itemTf = longVal(itemInfo.get("total_fee"), 0L);
			BigDecimal proportion =
					totalItemFee > 0L
							? BigDecimal.valueOf(itemTf).divide(totalItemFeeBd, 6, RoundingMode.HALF_UP)
							: BigDecimal.ZERO;

			if (k == items.size() - 1) {
				itemInfo.put("share_points", String.valueOf(remainingPoints));
				itemInfo.put("point_fee", remainingFee);
				remainingPoints = 0;
				remainingFee = 0L;
			} else {
				int sharePoints =
						BigDecimal.valueOf(itemPointUse)
								.multiply(proportion)
								.setScale(6, RoundingMode.HALF_UP)
								.setScale(0, RoundingMode.CEILING)
								.intValue();
				long shareFee =
						BigDecimal.valueOf(itemFeeByPointFee)
								.multiply(proportion)
								.setScale(6, RoundingMode.HALF_UP)
								.setScale(0, RoundingMode.CEILING)
								.longValue();

				if (sharePoints > remainingPoints) {
					sharePoints = remainingPoints;
				}
				if (shareFee > remainingFee) {
					shareFee = remainingFee;
				}
				if (shareFee > itemTf) {
					shareFee = itemTf;
				}

				itemInfo.put("share_points", String.valueOf(sharePoints));
				itemInfo.put("point_fee", shareFee);

				remainingPoints -= sharePoints;
				remainingFee -= shareFee;
			}
		}

		for (Map<String, Object> itemInfo : items) {
			int sharePts = intVal(itemInfo.get("share_points"), 0);
			long ptFee = longVal(itemInfo.get("point_fee"), 0L);
			long itemTf = longVal(itemInfo.get("total_fee"), 0L);
			itemInfo.put("point", String.valueOf(sharePts));
			if (itemTf > ptFee) {
				itemInfo.put("total_fee", itemTf - ptFee);
			} else {
				itemInfo.put("point_fee", itemTf);
				itemInfo.put("total_fee", 0L);
			}
		}

		long beforeDeductionSnap = orderTotalFen;
		orderData.put("before_point_deduction_total_fee", beforeDeductionSnap);
		long newOrderTotal = beforeDeductionSnap - (long) totalFeeByPointFee;
		if (newOrderTotal < 0L) {
			newOrderTotal = 0L;
		}
		orderData.put("total_fee", newOrderTotal);
		orderData.put("point_fee", totalFeeByPointFee);
		orderData.put("point_fee_item", itemFeeByPointFee);
		orderData.put("real_use_point", String.valueOf(totalPointUse));
		orderData.put("real_use_point_ziti", String.valueOf(itemPointUse));
		orderData.put("point", String.valueOf(totalPointUse));
		orderData.put("point_use", totalPointUse);
	}

	private static String pointLabel(Map<String, Object> pointRule) {
		Object nameObj = pointRule.get("name");
		if (nameObj != null && StringUtils.hasText(String.valueOf(nameObj).trim())) {
			return String.valueOf(nameObj).trim();
		}
		return "积分";
	}

	private static long lineAmountFen(Map<String, Object> item) {
		return longVal(item.get("price"), 0L) * longVal(item.get("num"), 0L);
	}

	private static int phpPointToMoneyFen(Map<String, Object> pointRule, int points) {
		if (points <= 0) {
			return 0;
		}
		if (!redisFlagTrue(pointRule.get("isOpenDeductPoint"))) {
			return 0;
		}
		Object dpRaw = pointRule.get("deduct_point");
		BigDecimal deductPoint;
		try {
			deductPoint = new BigDecimal(String.valueOf(dpRaw == null ? "0" : dpRaw).trim());
		} catch (NumberFormatException e) {
			deductPoint = BigDecimal.ZERO;
		}
		if (deductPoint.compareTo(BigDecimal.ZERO) <= 0) {
			return 0;
		}
		return BigDecimal.valueOf(points)
				.divide(deductPoint, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(2, RoundingMode.HALF_UP)
				.intValue();
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

	private static boolean phpTruthyCanDeductFreight(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
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
