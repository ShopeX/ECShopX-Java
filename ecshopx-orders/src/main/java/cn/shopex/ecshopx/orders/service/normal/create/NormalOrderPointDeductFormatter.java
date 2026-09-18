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
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderPointDeductFormatter {

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberBalanceReadService pointMemberBalanceReadService;
	private final NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService;

	public NormalOrderPointDeductFormatter(
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberBalanceReadService pointMemberBalanceReadService,
			NormalOrderCheckoutPointDeductService normalOrderCheckoutPointDeductService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
		this.normalOrderCheckoutPointDeductService = normalOrderCheckoutPointDeductService;
	}

	public void applyIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		if (isEmployeePurchaseOrder(p, od) || isPointsmallOrder(p, od)) {
			return;
		}
		// 积分商城不走现金单积分抵扣，仅校验会员积分是否覆盖订单所需积分
		if (isPointsmallOrder(p, od)) {
			applyPointsmallMemberPointCheck(od, p);
			return;
		}
		int pointUseRequested = intFrom(od.get("point_use"));
		if (pointUseRequested <= 0) {
			Object fromParams = p.getParams() != null ? p.getParams().get("point_use") : null;
			pointUseRequested = intFrom(fromParams);
		}
		// Align PHP OrderService::getPointDeduction: pay_type=point still runs checkout
		// deduct (force max_point when full_amount). Skipping here left total_fee unpaid.
		String payType = String.valueOf(od.getOrDefault("pay_type", ""));
		if (!"point".equals(payType) && p.getParams() != null && p.getParams().get("pay_type") != null) {
			payType = String.valueOf(p.getParams().get("pay_type"));
		}
		if (pointUseRequested <= 0 && !"point".equals(payType)) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		if (companyId <= 0L) {
			throw new ResourceException("企业信息缺失");
		}
		if (userId <= 0L) {
			throw new ResourceException("会员信息有误");
		}
		if (!pointMemberRuleReadService.getIsOpenPoint(companyId)) {
			Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
			Object nameObj = rule.get("name");
			String label =
					nameObj == null || !StringUtils.hasText(String.valueOf(nameObj))
							? "积分"
							: String.valueOf(nameObj).trim();
			throw new ResourceException(label + "抵扣未开启");
		}
		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
		long memberPoint = pointMemberBalanceReadService.getPointBalance(companyId, userId);
		Map<String, Object> params = p.getParams() != null ? p.getParams() : Map.of();
		if (params.get("point_use") == null) {
			od.put("point_use", pointUseRequested);
		}
		normalOrderCheckoutPointDeductService.applyCheckoutPointDeduct(
				od, params, companyId, userId, rule, memberPoint);
	}

	/**
	 * 积分商城下单：校验会员积分是否覆盖订单所需积分（create 路径 isCheck=true，不足则抛错）。
	 */
	private void applyPointsmallMemberPointCheck(Map<String, Object> od, NormalOrderCreateParams p) {
		Map<String, Object> params = p.getParams() != null ? p.getParams() : Map.of();
		long companyId = longVal(od.get("company_id"), longVal(params.get("company_id"), 0L));
		long userId = longVal(od.get("user_id"), longVal(params.get("user_id"), 0L));
		long orderPoint = longVal(od.get("point"), 0L);
		if (orderPoint <= 0L) {
			throw new ResourceException("订单使用积分不能低于一积分!");
		}
		if (companyId <= 0L || userId <= 0L) {
			throw new ResourceException("当前积分不足以支付本次订单费用!");
		}
		long memberPoint = pointMemberBalanceReadService.getPointBalance(companyId, userId);
		if (memberPoint < orderPoint) {
			throw new ResourceException("当前积分不足以支付本次订单费用!");
		}
	}

	private static boolean isPointsmallOrder(NormalOrderCreateParams p, Map<String, Object> od) {
		if ("pointsmall".equals(String.valueOf(od.getOrDefault("order_class", "")))) {
			return true;
		}
		Map<String, Object> params = p.getParams();
		return params != null
				&& "normal_pointsmall".equals(String.valueOf(params.getOrDefault("order_type", "")));
	}

	private static boolean isEmployeePurchaseOrder(NormalOrderCreateParams p, Map<String, Object> od) {
		if ("employee_purchase".equals(String.valueOf(od.getOrDefault("order_class", "")))) {
			return true;
		}
		Map<String, Object> params = p.getParams();
		return params != null
				&& "normal_employee_purchase".equals(String.valueOf(params.getOrDefault("order_type", "")));
	}

	private static int intFrom(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
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
