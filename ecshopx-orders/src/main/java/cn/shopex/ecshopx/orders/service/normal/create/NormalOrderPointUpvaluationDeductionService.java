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
import cn.shopex.ecshopx.point.service.PointMemberPointToMoneyService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderPointUpvaluationDeductionService {

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberMoneyToPointService pointMemberMoneyToPointService;
	private final PointMemberPointToMoneyService pointMemberPointToMoneyService;

	public NormalOrderPointUpvaluationDeductionService(
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberMoneyToPointService pointMemberMoneyToPointService,
			PointMemberPointToMoneyService pointMemberPointToMoneyService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberMoneyToPointService = pointMemberMoneyToPointService;
		this.pointMemberPointToMoneyService = pointMemberPointToMoneyService;
	}

	public Map<String, Object> getUpTotalMaxPointDeduction(
			long companyId, Map<String, Object> orderData, long memberPoint, Map<String, Object> pointUpvaluation) {
		sortItemsByLineAmountDesc(orderData);
		int upvaluation = intVal(pointUpvaluation.get("upvaluation"), 0);
		long userUppoint = memberPoint * (upvaluation - 1L);
		long maxBasePoint = pointMemberPointToMoneyService.orderMaxMoneyToPoint(companyId, longVal(orderData.get("total_fee"), 0L));
		long orderMaxUpoint =
				bcInt(
						bcMul(
								bcDiv(BigDecimal.valueOf(maxBasePoint), BigDecimal.valueOf(upvaluation), 5),
								BigDecimal.valueOf(upvaluation - 1L)));
		long orderMaxPoint = bcLong(bcDiv(BigDecimal.valueOf(orderMaxUpoint), BigDecimal.valueOf(upvaluation - 1L), 0));
		orderMaxPoint = Math.min(orderMaxPoint, memberPoint);
		orderMaxUpoint = orderMaxPoint * (upvaluation - 1L);
		long userMaxUppoint = Math.min(userUppoint, orderMaxUpoint);
		long diffMax =
				longVal(pointUpvaluation.get("max_up_point"), 0L) * (upvaluation - 1L)
						- longVal(pointUpvaluation.get("uppoints"), 0L);
		if (userMaxUppoint <= diffMax) {
			long maxCanuseUppoint = Math.min(userMaxUppoint, diffMax);
			return allMaxUpPointDeduction(
					companyId, orderData, maxBasePoint, maxCanuseUppoint, orderMaxPoint, upvaluation);
		}
		userMaxUppoint = Math.min(diffMax, userMaxUppoint);
		long maxCanuseUppoint = Math.min(userMaxUppoint, diffMax);
		long maxCanusePointBase = bcLong(bcDiv(BigDecimal.valueOf(maxCanuseUppoint), BigDecimal.valueOf(upvaluation - 1L), 0));
		maxCanuseUppoint = maxCanusePointBase * (upvaluation - 1L);
		orderMaxPoint = maxBasePoint - maxCanuseUppoint;
		orderMaxPoint = Math.min(orderMaxPoint, memberPoint);
		return maxSomeUpPointDeduction(
				companyId, orderData, maxBasePoint, maxCanuseUppoint, orderMaxPoint, upvaluation);
	}

	@SuppressWarnings("unchecked")
	public void getUpTotalUsePointDeduction(
			long companyId,
			Map<String, Object> orderData,
			long memberPoint,
			Map<String, Object> pointUpvaluation,
			Map<String, Object> pointRule) {
		int pointUse = intVal(orderData.get("point_use"), 0);
		if (pointUse <= 0) {
			return;
		}
		if (memberPoint < pointUse) {
			throw new ResourceException(pointLabel(pointRule) + "不足");
		}
		sortItemsByLineAmountDesc(orderData);
		int upvaluation = intVal(pointUpvaluation.get("upvaluation"), 0);
		long userUppoint = (long) pointUse * (upvaluation - 1L);
		long maxBasePoint = pointMemberPointToMoneyService.orderMaxMoneyToPoint(companyId, longVal(orderData.get("total_fee"), 0L));
		long orderMaxUpoint =
				bcInt(
						bcMul(
								bcDiv(BigDecimal.valueOf(maxBasePoint), BigDecimal.valueOf(upvaluation), 5),
								BigDecimal.valueOf(upvaluation - 1L)));
		long orderMaxPoint = bcLong(bcDiv(BigDecimal.valueOf(orderMaxUpoint), BigDecimal.valueOf(upvaluation - 1L), 0));
		orderMaxPoint = Math.min(Math.min(orderMaxPoint, memberPoint), pointUse);
		orderMaxUpoint = orderMaxPoint * (upvaluation - 1L);
		long userMaxUppoint = Math.min(userUppoint, orderMaxUpoint);
		long diffMax =
				longVal(pointUpvaluation.get("max_up_point"), 0L) * (upvaluation - 1L)
						- longVal(pointUpvaluation.get("uppoints"), 0L);
		long maxCanuseUppoint = Math.min(userMaxUppoint, diffMax);
		if (userMaxUppoint <= diffMax) {
			allUseUpPointDeduction(companyId, orderData, maxBasePoint, maxCanuseUppoint, orderMaxPoint, upvaluation, pointRule);
		} else {
			userMaxUppoint = Math.min(diffMax, userMaxUppoint);
			maxCanuseUppoint = Math.min(userMaxUppoint, diffMax);
			long maxCanusePointBase = bcLong(bcDiv(BigDecimal.valueOf(maxCanuseUppoint), BigDecimal.valueOf(upvaluation - 1L), 0));
			maxCanuseUppoint = maxCanusePointBase * (upvaluation - 1L);
			orderMaxPoint = maxBasePoint - maxCanuseUppoint;
			orderMaxPoint = Math.min(Math.min(orderMaxPoint, memberPoint), pointUse);
			useSomeUpPointDeduction(companyId, orderData, maxBasePoint, maxCanuseUppoint, orderMaxPoint, upvaluation, pointRule);
		}
		int realUsePoint = intVal(orderData.get("real_use_point"), 0);
		if (realUsePoint > pointUse) {
			throw new ResourceException("超出本单可用" + pointLabel(pointRule) + "抵扣");
		}
		orderData.put("point", orderData.get("real_use_point"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> allMaxUpPointDeduction(
			long companyId,
			Map<String, Object> orderData,
			long maxBasePoint,
			long maxCanuseUppoint,
			long orderMaxPoint,
			int upvaluation) {
		long itemMaxCanuseUppoint = maxCanuseUppoint;
		long itemTotalFee = longVal(orderData.get("total_fee"), 0L);
		long totalSub = 0L;
		long useLimit = 0L;
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("limit_point", orderMaxPoint);
		result.put("max_point", useLimit);
		result.put("max_money", totalSub);
		result.put("max_uppoint", maxCanuseUppoint);

		Map<String, Object> pointRule = pointMemberRuleReadService.getPointRule(companyId);
		boolean canDeductFreight = phpTruthyCanDeductFreight(pointRule.get("can_deduct_freight"));
		long freightFee = longVal(orderData.get("freight_fee"), 0L);
		if (canDeductFreight && freightFee > 0L) {
			Map<String, Object> freightResult = freightUpPointDeduction(companyId, freightFee, upvaluation, orderMaxPoint);
			totalSub += longVal(freightResult.get("freight_point_fee"), 0L);
			useLimit += longVal(freightResult.get("freight_canuse_point"), 0L);
			itemMaxCanuseUppoint -= longVal(freightResult.get("freight_uppoints"), 0L);
			itemTotalFee -= longVal(freightResult.get("freight_point_fee"), 0L);
		}
		if (itemMaxCanuseUppoint <= 0L) {
			result.put("max_point", useLimit);
			result.put("max_money", totalSub);
			return result;
		}
		List<Map<String, Object>> items = itemsList(orderData);
		long itemTmpUppoint = 0L;
		for (int k = 0; k < items.size(); k++) {
			Map<String, Object> itemInfo = items.get(k);
			long shareUppoints;
			if (k < items.size() - 1) {
				BigDecimal proportion =
						itemTotalFee > 0L
								? bcDiv(BigDecimal.valueOf(longVal(itemInfo.get("total_fee"), 0L)), BigDecimal.valueOf(itemTotalFee), 5)
								: BigDecimal.ZERO;
				shareUppoints = Math.round(bcDouble(bcMul(proportion, BigDecimal.valueOf(itemMaxCanuseUppoint), 5)));
				long remaining = itemMaxCanuseUppoint - itemTmpUppoint;
				if (shareUppoints > remaining) {
					shareUppoints = remaining;
				}
			} else {
				shareUppoints = itemMaxCanuseUppoint - itemTmpUppoint;
				if (shareUppoints < 0L) {
					shareUppoints = 0L;
				}
			}
			long sharePoints = bcLong(bcDiv(BigDecimal.valueOf(shareUppoints), BigDecimal.valueOf(upvaluation - 1L), 0));
			shareUppoints = sharePoints * (upvaluation - 1L);
			long shareBasePoint = sharePoints * upvaluation;
			int pointFee = pointMemberPointToMoneyService.pointToMoney(companyId, shareBasePoint);
			itemInfo.put("point_fee", pointFee);
			itemInfo.put("share_points", sharePoints);
			itemInfo.put("point", sharePoints);
			itemInfo.put("share_uppoints", shareUppoints);
			itemTmpUppoint += shareUppoints;
			totalSub += pointFee;
			useLimit += sharePoints;
		}
		result.put("max_point", useLimit);
		result.put("max_money", totalSub);
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> maxSomeUpPointDeduction(
			long companyId,
			Map<String, Object> orderData,
			long maxBasePoint,
			long maxCanuseUppoint,
			long orderMaxPoint,
			int upvaluation) {
		long itemMaxCanuseUppoint = maxCanuseUppoint;
		long itemTotalFee = longVal(orderData.get("total_fee"), 0L);
		long itemTotalBasePoint = maxBasePoint;
		long totalSub = 0L;
		long useLimit = 0L;
		long tmpUppoint = 0L;
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("limit_point", orderMaxPoint);
		result.put("max_point", useLimit);
		result.put("max_money", totalSub);
		result.put("max_uppoint", maxCanuseUppoint);

		Map<String, Object> pointRule = pointMemberRuleReadService.getPointRule(companyId);
		boolean canDeductFreight = phpTruthyCanDeductFreight(pointRule.get("can_deduct_freight"));
		long freightFee = longVal(orderData.get("freight_fee"), 0L);
		if (canDeductFreight && freightFee > 0L) {
			Map<String, Object> freightResult =
					freightSomeUpPointDeduction(
							companyId, freightFee, upvaluation, maxCanuseUppoint, maxBasePoint, orderMaxPoint);
			totalSub += longVal(freightResult.get("freight_point_fee"), 0L);
			useLimit += longVal(freightResult.get("freight_canuse_point"), 0L);
			itemMaxCanuseUppoint -= longVal(freightResult.get("freight_uppoints"), 0L);
			itemTotalFee -= longVal(freightResult.get("freight_point_fee"), 0L);
			itemTotalBasePoint -= longVal(freightResult.get("freight_total_base_point"), 0L);
			if (itemMaxCanuseUppoint <= 0L) {
				maxCanuseUppoint = longVal(freightResult.get("freight_uppoints"), 0L);
			}
			tmpUppoint += longVal(freightResult.get("freight_uppoints"), 0L);
		}
		if (itemTotalBasePoint <= 0L) {
			result.put("max_point", useLimit);
			result.put("max_money", totalSub);
			result.put("max_uppoint", maxCanuseUppoint);
			return result;
		}
		List<Map<String, Object>> items = itemsList(orderData);
		long tmpBasePoint = 0L;
		for (int k = 0; k < items.size(); k++) {
			Map<String, Object> itemInfo = items.get(k);
			long sharePoints;
			long shareUppoints;
			if (k < items.size() - 1) {
				BigDecimal proportion =
						itemTotalFee > 0L
								? bcDiv(BigDecimal.valueOf(longVal(itemInfo.get("total_fee"), 0L)), BigDecimal.valueOf(itemTotalFee), 5)
								: BigDecimal.ZERO;
				BigDecimal orderPointProportion =
						maxBasePoint > 0L
								? bcDiv(BigDecimal.valueOf(orderMaxPoint), BigDecimal.valueOf(maxBasePoint), 5)
								: BigDecimal.ZERO;
				sharePoints = bcLong(bcMul(bcMul(BigDecimal.valueOf(itemTotalBasePoint), proportion), orderPointProportion));
				long shareBasePoint = bcLong(bcMul(BigDecimal.valueOf(itemTotalBasePoint), proportion));
				shareUppoints = shareBasePoint - sharePoints;
				long remaining = maxCanuseUppoint - tmpUppoint;
				if (shareUppoints > remaining) {
					shareUppoints = remaining;
				}
			} else {
				long shareBasePoint = itemTotalBasePoint - tmpBasePoint;
				sharePoints = orderMaxPoint - useLimit;
				shareUppoints = maxCanuseUppoint - tmpUppoint;
				if (shareUppoints < 0L) {
					shareUppoints = 0L;
				}
			}
			long sharePointUp = bcLong(bcDiv(BigDecimal.valueOf(shareUppoints), BigDecimal.valueOf(upvaluation - 1L), 0));
			long sharePointBase = sharePoints - sharePointUp;
			shareUppoints = sharePointUp * (upvaluation - 1L);
			long shareBasePoint = sharePointUp * upvaluation + sharePointBase;
			int pointFee = pointMemberPointToMoneyService.pointToMoney(companyId, shareBasePoint);
			itemInfo.put("point_fee", pointFee);
			itemInfo.put("share_points", sharePoints);
			itemInfo.put("point", sharePoints);
			itemInfo.put("share_uppoints", shareUppoints);
			tmpBasePoint += shareBasePoint;
			totalSub += pointFee;
			useLimit += sharePoints;
			tmpUppoint += shareUppoints;
		}
		result.put("max_point", Math.min(orderMaxPoint, useLimit));
		result.put("max_money", totalSub);
		result.put("max_uppoint", tmpUppoint);
		return result;
	}

	@SuppressWarnings("unchecked")
	private void allUseUpPointDeduction(
			long companyId,
			Map<String, Object> orderData,
			long maxBasePoint,
			long maxCanuseUppoint,
			long orderMaxPoint,
			int upvaluation,
			Map<String, Object> pointRule) {
		long itemMaxCanuseUppoint = maxCanuseUppoint;
		long itemTotalFee = longVal(orderData.get("total_fee"), 0L);
		long tmpPoints = 0L;
		long tmpUppoint = 0L;
		long tmpSharePointUp = 0L;
		long totalSub = 0L;
		Map<String, Object> freightResult = emptyFreightResult();
		boolean canDeductFreight = phpTruthyCanDeductFreight(pointRule.get("can_deduct_freight"));
		long freightFee = longVal(orderData.get("freight_fee"), 0L);
		if (canDeductFreight && freightFee > 0L) {
			freightResult = freightUpPointDeduction(companyId, freightFee, upvaluation, orderMaxPoint);
			tmpPoints += longVal(freightResult.get("freight_canuse_point"), 0L);
			tmpUppoint += longVal(freightResult.get("freight_uppoints"), 0L);
			tmpSharePointUp += longVal(freightResult.get("freight_canuse_point_up"), 0L);
			totalSub += longVal(freightResult.get("freight_point_fee"), 0L);
			itemMaxCanuseUppoint -= longVal(freightResult.get("freight_uppoints"), 0L);
			itemTotalFee -= longVal(freightResult.get("freight_point_fee"), 0L);
			orderData.put("freight_point", longVal(freightResult.get("freight_canuse_point"), 0L));
			orderData.put("freight_point_fee", longVal(freightResult.get("freight_point_fee"), 0L));
			orderData.put("freight_uppoints", longVal(freightResult.get("freight_uppoints"), 0L));
		}
		if (itemMaxCanuseUppoint <= 0L) {
			orderData.put("before_point_deduction_total_fee", longVal(orderData.get("total_fee"), 0L));
			orderData.put("total_fee", longVal(orderData.get("total_fee"), 0L) - totalSub);
			orderData.put("point_fee", totalSub);
			orderData.put("real_use_point", longVal(freightResult.get("freight_canuse_point"), 0L));
			orderData.put("uppoint_use", tmpUppoint);
			orderData.put("point_up_use", tmpSharePointUp);
			orderData.put("point_use", intVal(orderData.get("point_use"), 0));
			return;
		}
		List<Map<String, Object>> items = itemsList(orderData);
		for (int k = 0; k < items.size(); k++) {
			Map<String, Object> itemInfo = items.get(k);
			long shareUppoints;
			if (k < items.size() - 1) {
				BigDecimal proportion =
						itemTotalFee > 0L
								? bcDiv(BigDecimal.valueOf(longVal(itemInfo.get("total_fee"), 0L)), BigDecimal.valueOf(itemTotalFee), 5)
								: BigDecimal.ZERO;
				shareUppoints = Math.round(bcDouble(bcMul(proportion, BigDecimal.valueOf(itemMaxCanuseUppoint), 5)));
				long remaining = maxCanuseUppoint - tmpUppoint;
				if (shareUppoints > remaining) {
					shareUppoints = remaining;
				}
			} else {
				shareUppoints = maxCanuseUppoint - tmpUppoint;
				if (shareUppoints < 0L) {
					shareUppoints = 0L;
				}
			}
			long sharePoints = bcLong(bcDiv(BigDecimal.valueOf(shareUppoints), BigDecimal.valueOf(upvaluation - 1L), 0));
			shareUppoints = sharePoints * (upvaluation - 1L);
			long shareBasePoint = sharePoints * upvaluation;
			int pointFee = pointMemberPointToMoneyService.pointToMoney(companyId, shareBasePoint);
			long itemTf = longVal(itemInfo.get("total_fee"), 0L);
			itemInfo.put("total_fee", Math.max(0L, itemTf - pointFee));
			itemInfo.put("point_fee", pointFee);
			itemInfo.put("share_points", String.valueOf(sharePoints));
			itemInfo.put("point", String.valueOf(sharePoints));
			itemInfo.put("share_uppoints", shareUppoints);
			tmpUppoint += shareUppoints;
			totalSub += pointFee;
			tmpPoints += sharePoints;
			tmpSharePointUp += sharePoints;
		}
		orderData.put("before_point_deduction_total_fee", longVal(orderData.get("total_fee"), 0L));
		orderData.put("total_fee", longVal(orderData.get("total_fee"), 0L) - totalSub);
		orderData.put("point_fee", totalSub);
		orderData.put("real_use_point", tmpPoints);
		orderData.put("uppoint_use", tmpUppoint);
		orderData.put("point_up_use", tmpSharePointUp);
		orderData.put("point_use", intVal(orderData.get("point_use"), 0));
	}

	@SuppressWarnings("unchecked")
	private void useSomeUpPointDeduction(
			long companyId,
			Map<String, Object> orderData,
			long maxBasePoint,
			long maxCanuseUppoint,
			long orderMaxPoint,
			int upvaluation,
			Map<String, Object> pointRule) {
		long itemMaxCanuseUppoint = maxCanuseUppoint;
		long itemTotalFee = longVal(orderData.get("total_fee"), 0L);
		long itemTotalBasePoint = maxBasePoint;
		long itemMaxPoint = orderMaxPoint;
		long tmpPoints = 0L;
		long tmpUppoint = 0L;
		long tmpSharePointUp = 0L;
		long totalSub = 0L;
		Map<String, Object> freightResult = emptyFreightResult();
		boolean canDeductFreight = phpTruthyCanDeductFreight(pointRule.get("can_deduct_freight"));
		long freightFee = longVal(orderData.get("freight_fee"), 0L);
		if (canDeductFreight && freightFee > 0L) {
			freightResult =
					freightSomeUpPointDeduction(
							companyId, freightFee, upvaluation, maxCanuseUppoint, maxBasePoint, orderMaxPoint);
			totalSub += longVal(freightResult.get("freight_point_fee"), 0L);
			itemMaxCanuseUppoint -= longVal(freightResult.get("freight_uppoints"), 0L);
			itemTotalFee -= longVal(freightResult.get("freight_point_fee"), 0L);
			itemTotalBasePoint -= longVal(freightResult.get("freight_total_base_point"), 0L);
			if (itemMaxCanuseUppoint <= 0L) {
				maxCanuseUppoint = longVal(freightResult.get("freight_uppoints"), 0L);
			}
			itemMaxPoint -= longVal(freightResult.get("freight_canuse_point"), 0L);
			tmpPoints = longVal(freightResult.get("freight_canuse_point"), 0L);
			tmpUppoint += longVal(freightResult.get("freight_uppoints"), 0L);
			tmpSharePointUp += longVal(freightResult.get("freight_canuse_point_up"), 0L);
			orderData.put("freight_point", longVal(freightResult.get("freight_canuse_point"), 0L));
			orderData.put("freight_point_fee", longVal(freightResult.get("freight_point_fee"), 0L));
			orderData.put("freight_uppoints", longVal(freightResult.get("freight_uppoints"), 0L));
		}
		if (itemTotalBasePoint <= 0L || itemMaxPoint <= 0L) {
			orderData.put("before_point_deduction_total_fee", longVal(orderData.get("total_fee"), 0L));
			orderData.put("total_fee", longVal(orderData.get("total_fee"), 0L) - totalSub);
			orderData.put("point_fee", totalSub);
			orderData.put("real_use_point", longVal(freightResult.get("freight_canuse_point"), 0L));
			orderData.put("uppoint_use", tmpUppoint);
			orderData.put("point_up_use", tmpSharePointUp);
			orderData.put("point_use", intVal(orderData.get("point_use"), 0));
			return;
		}
		List<Map<String, Object>> items = itemsList(orderData);
		long tmpBasePoint = 0L;
		for (int k = 0; k < items.size(); k++) {
			Map<String, Object> itemInfo = items.get(k);
			long sharePoints;
			long shareUppoints;
			if (k < items.size() - 1) {
				BigDecimal proportion =
						itemTotalFee > 0L
								? bcDiv(BigDecimal.valueOf(longVal(itemInfo.get("total_fee"), 0L)), BigDecimal.valueOf(itemTotalFee), 5)
								: BigDecimal.ZERO;
				BigDecimal orderPointProportion =
						maxBasePoint > 0L
								? bcDiv(BigDecimal.valueOf(orderMaxPoint), BigDecimal.valueOf(maxBasePoint), 5)
								: BigDecimal.ZERO;
				sharePoints = bcLong(bcMul(bcMul(BigDecimal.valueOf(itemTotalBasePoint), proportion), orderPointProportion));
				long shareBasePoint = bcLong(bcMul(BigDecimal.valueOf(itemTotalBasePoint), proportion));
				shareUppoints = shareBasePoint - sharePoints;
				long remaining = maxCanuseUppoint - tmpUppoint;
				if (shareUppoints > remaining) {
					shareUppoints = remaining;
				}
			} else {
				long shareBasePoint = itemTotalBasePoint - tmpBasePoint;
				sharePoints = orderMaxPoint - tmpPoints;
				shareUppoints = maxCanuseUppoint - tmpUppoint;
				if (shareUppoints < 0L) {
					shareUppoints = 0L;
				}
			}
			long sharePointUp = bcLong(bcDiv(BigDecimal.valueOf(shareUppoints), BigDecimal.valueOf(upvaluation - 1L), 0));
			long sharePointBase = sharePoints - sharePointUp;
			shareUppoints = sharePointUp * (upvaluation - 1L);
			long shareBasePoint = sharePointUp * upvaluation + sharePointBase;
			int pointFee = pointMemberPointToMoneyService.pointToMoney(companyId, shareBasePoint);
			long itemTf = longVal(itemInfo.get("total_fee"), 0L);
			itemInfo.put("total_fee", Math.max(0L, itemTf - pointFee));
			if ("point".equals(String.valueOf(orderData.getOrDefault("pay_type", "")))) {
				itemInfo.put("total_fee", 0L);
			}
			itemInfo.put("point_fee", pointFee);
			itemInfo.put("share_points", String.valueOf(sharePoints));
			itemInfo.put("point", String.valueOf(sharePoints));
			itemInfo.put("share_uppoints", shareUppoints);
			itemInfo.put("share_point_up", sharePointUp);
			tmpBasePoint += shareBasePoint;
			totalSub += pointFee;
			tmpPoints += sharePoints;
			tmpUppoint += shareUppoints;
			tmpSharePointUp += sharePointUp;
		}
		orderData.put("before_point_deduction_total_fee", longVal(orderData.get("total_fee"), 0L));
		orderData.put("total_fee", longVal(orderData.get("total_fee"), 0L) - totalSub);
		orderData.put("point_fee", totalSub);
		orderData.put("real_use_point", tmpPoints);
		orderData.put("uppoint_use", tmpUppoint);
		orderData.put("point_up_use", tmpSharePointUp);
		orderData.put("point_use", intVal(orderData.get("point_use"), 0));
	}

	private Map<String, Object> freightUpPointDeduction(
			long companyId, long freightFee, int upvaluation, long orderMaxPoint) {
		long freightBasePoint = pointMemberMoneyToPointService.moneyToPoint(companyId, freightFee);
		long freightUppoints =
				bcLong(
						bcMul(
								bcDiv(BigDecimal.valueOf(freightBasePoint), BigDecimal.valueOf(upvaluation), 5),
								BigDecimal.valueOf(upvaluation - 1L)));
		long freightCanusePoint = bcLong(bcDiv(BigDecimal.valueOf(freightUppoints), BigDecimal.valueOf(upvaluation - 1L), 0));
		freightCanusePoint = Math.min(freightCanusePoint, orderMaxPoint);
		freightUppoints = freightCanusePoint * (upvaluation - 1L);
		long freightBasePointConverted = freightCanusePoint * upvaluation;
		int freightPointFee = pointMemberPointToMoneyService.pointToMoney(companyId, freightBasePointConverted);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("freight_uppoints", freightUppoints);
		result.put("freight_point_fee", freightPointFee);
		result.put("freight_canuse_point", freightCanusePoint);
		result.put("freight_canuse_point_up", freightCanusePoint);
		return result;
	}

	private Map<String, Object> freightSomeUpPointDeduction(
			long companyId,
			long freightFee,
			int upvaluation,
			long maxCanuseUppoint,
			long maxBasePoint,
			long orderMaxPoint) {
		long freightBasePoint = pointMemberMoneyToPointService.moneyToPoint(companyId, freightFee);
		long freightCanusePoint;
		long freightUppoints;
		if (freightBasePoint > maxBasePoint) {
			freightBasePoint = maxBasePoint;
			freightCanusePoint = freightBasePoint - maxCanuseUppoint;
		} else {
			BigDecimal ratio =
					maxBasePoint > 0L
							? bcDiv(BigDecimal.valueOf(orderMaxPoint), BigDecimal.valueOf(maxBasePoint), 5)
							: BigDecimal.ZERO;
			freightCanusePoint = bcLong(bcMul(BigDecimal.valueOf(freightBasePoint), ratio));
		}
		if (freightCanusePoint <= 0L && freightBasePoint > maxBasePoint) {
			freightCanusePoint = orderMaxPoint;
			freightUppoints = maxCanuseUppoint;
		} else {
			freightUppoints = freightBasePoint - freightCanusePoint;
		}
		if (freightCanusePoint == 0L) {
			return emptyFreightSomeResult();
		}
		long freightCanusePointUp = bcLong(bcDiv(BigDecimal.valueOf(freightUppoints), BigDecimal.valueOf(upvaluation - 1L), 0));
		long freightCanusePointBase = freightCanusePoint - freightCanusePointUp;
		freightUppoints = freightCanusePointUp * (upvaluation - 1L);
		long freightTotalBasePoint = freightCanusePointUp * upvaluation + freightCanusePointBase;
		int freightPointFee = pointMemberPointToMoneyService.pointToMoney(companyId, freightTotalBasePoint);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("freight_uppoints", freightUppoints);
		result.put("freight_point_fee", freightPointFee);
		result.put("freight_canuse_point", freightCanusePoint);
		result.put("freight_canuse_point_up", freightCanusePointUp);
		result.put("freight_total_base_point", freightTotalBasePoint);
		return result;
	}

	private static Map<String, Object> emptyFreightResult() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("freight_uppoints", 0L);
		m.put("freight_point_fee", 0L);
		m.put("freight_canuse_point", 0L);
		m.put("freight_canuse_point_up", 0L);
		return m;
	}

	private static Map<String, Object> emptyFreightSomeResult() {
		Map<String, Object> m = emptyFreightResult();
		m.put("freight_total_base_point", 0L);
		return m;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> itemsList(Map<String, Object> orderData) {
		Object raw = orderData.get("items");
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				items.add((Map<String, Object>) m);
			}
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	private static void sortItemsByLineAmountDesc(Map<String, Object> orderData) {
		List<Map<String, Object>> items = itemsList(orderData);
		items.sort(
				Comparator.comparingLong(
								(Map<String, Object> item) ->
										longVal(item.get("price"), 0L) * longVal(item.get("num"), 0L))
						.reversed());
		orderData.put("items", items);
	}

	private static BigDecimal bcDiv(BigDecimal a, BigDecimal b, int scale) {
		if (b.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return a.divide(b, scale, RoundingMode.DOWN);
	}

	private static BigDecimal bcMul(BigDecimal a, BigDecimal b) {
		return a.multiply(b);
	}

	private static BigDecimal bcMul(BigDecimal a, BigDecimal b, int scale) {
		return a.multiply(b).setScale(scale, RoundingMode.HALF_UP);
	}

	private static long bcLong(BigDecimal v) {
		return v.setScale(0, RoundingMode.DOWN).longValue();
	}

	private static int bcInt(BigDecimal v) {
		return v.setScale(0, RoundingMode.DOWN).intValue();
	}

	private static double bcDouble(BigDecimal v) {
		return v.doubleValue();
	}

	private static String pointLabel(Map<String, Object> pointRule) {
		Object nameObj = pointRule.get("name");
		if (nameObj != null && StringUtils.hasText(String.valueOf(nameObj).trim())) {
			return String.valueOf(nameObj).trim();
		}
		return "积分";
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
