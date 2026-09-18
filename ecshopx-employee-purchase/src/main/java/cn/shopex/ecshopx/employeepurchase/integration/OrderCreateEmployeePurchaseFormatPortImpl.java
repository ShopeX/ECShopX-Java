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

package cn.shopex.ecshopx.employeepurchase.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseFormatPort;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityItemsAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityDataService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartItemLimitService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePurchasePassphraseOrderService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateEmployeePurchaseFormatPortImpl implements OrderCreateEmployeePurchaseFormatPort {

	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final ActivityItemsMapper activityItemsMapper;
	private final MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper;
	private final EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService;

	public OrderCreateEmployeePurchaseFormatPortImpl(
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			ActivityItemsMapper activityItemsMapper,
			MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper,
			EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService) {
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.activityItemsMapper = activityItemsMapper;
		this.memberActivityItemsAggregateMapper = memberActivityItemsAggregateMapper;
		this.employeePurchasePassphraseOrderService = employeePurchasePassphraseOrderService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void applyEmployeePurchaseFormat(NormalOrderCreateParams p, boolean isCheck) {
		employeePurchasePassphraseOrderService.checkBeforeOrder(p, isCheck);
		Map<String, Object> pr = p.getParams();
		Map<String, Object> od = p.getOrderData();
		long companyId = longVal(pr.get("company_id"), 0L);
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L) {
			return;
		}

		Activities activity =
				employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(
						companyId, activityId, enterpriseId);

		boolean prepaid = PurchaseModeSupport.isPrepaidPoint(activity);
		od.put("purchase_mode", activity.getPurchaseMode());
		od.put("purchase_mode_desc", PurchaseModeSupport.desc(activity.getPurchaseMode()));
		if (prepaid) {
			if (employeePurchaseActivityDataService.isActiveRelative(
					companyId, enterpriseId, activityId, userId)) {
				throw new ResourceException("预充点数活动不支持亲友购");
			}
			od.put("pay_type", PurchaseModeSupport.PREPAID_POINT);
			od.put("freight_type", "point");
			pr.put("pay_type", PurchaseModeSupport.PREPAID_POINT);
		}

		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> rawList) || rawList.isEmpty()) {
			return;
		}

		List<Long> itemIds = new ArrayList<>();
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long itemId = longVal(((Map<String, Object>) row).get("item_id"), 0L);
			if (itemId > 0L) {
				itemIds.add(itemId);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}

		List<ActivityItems> activityItemRows =
				activityItemsMapper.selectList(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.in(ActivityItems::getItemId, itemIds));
		Map<Long, ActivityItems> activityItemByItemId = new LinkedHashMap<>();
		for (ActivityItems ai : activityItemRows) {
			if (ai.getItemId() != null) {
				activityItemByItemId.put(ai.getItemId(), ai);
			}
		}

		List<MemberActivityItemsAggregate> aggregateRows =
				memberActivityItemsAggregateMapper.selectList(
						Wrappers.<MemberActivityItemsAggregate>lambdaQuery()
								.eq(MemberActivityItemsAggregate::getCompanyId, companyId)
								.eq(MemberActivityItemsAggregate::getEnterpriseId, enterpriseId)
								.eq(MemberActivityItemsAggregate::getUserId, userId)
								.eq(MemberActivityItemsAggregate::getActivityId, activityId)
								.in(MemberActivityItemsAggregate::getItemId, itemIds));
		Map<Long, MemberActivityItemsAggregate> aggregateByItemId = new LinkedHashMap<>();
		for (MemberActivityItemsAggregate agg : aggregateRows) {
			if (agg.getItemId() != null) {
				aggregateByItemId.put(agg.getItemId(), agg);
			}
		}

		List<EmployeePurchaseItemLimitValidator.LimitLine> limitLines = new ArrayList<>();
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> rawMap)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) rawMap;
			long itemId = longVal(item.get("item_id"), 0L);
			ActivityItems activityItem = activityItemByItemId.get(itemId);
			if (activityItem == null || !EmployeePurchaseCartItemLimitService.isOnShelf(activityItem)) {
				if (isCheck) {
					throw new ResourceException("商品已下架");
				}
				od.put("extraTips", "商品已下架");
			}
			limitLines.add(
					new EmployeePurchaseItemLimitValidator.LimitLine(
							itemId, intVal(item.get("num"), 0), intVal(item.get("item_fee"), 0)));
		}
		try {
			EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
					toItemLimits(activityItemByItemId), toItemAggregates(aggregateByItemId), limitLines);
		} catch (ResourceException e) {
			applyLimitTip(od, e.getMessage(), isCheck);
		}

		Map<String, Object> aggregateFee =
				employeePurchaseActivityDataService.getAggregateFeeForInvitee(
						companyId, enterpriseId, activityId, userId);
		long leftFee = longVal(aggregateFee.get("left_fee"), 0L);
		od.put("left_fee", leftFee);

		int minimumAmount = activity.getMinimumAmount() == null ? 0 : activity.getMinimumAmount();
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> rawMap)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) rawMap;
			long itemId = longVal(item.get("item_id"), 0L);
			ActivityItems ai = activityItemByItemId.get(itemId);
			if (ai == null || ai.getActivityPrice() == null) {
				continue;
			}
			int actPrice = ai.getActivityPrice();
			int num = intVal(item.get("num"), 1);
			int correctItemFee = actPrice * num;
			item.put("price", actPrice);
			item.put("item_fee", correctItemFee);
			item.put("total_fee", correctItemFee);
		}
		long correctItemFeeSum = 0L;
		for (Object o : rawList) {
			if (o instanceof Map<?, ?> m) {
				correctItemFeeSum += intVal(((Map<String, Object>) m).get("item_fee"), 0);
			}
		}
		od.put("item_fee", correctItemFeeSum);
		od.put("total_fee", correctItemFeeSum);
		od.put("market_fee", String.valueOf(correctItemFeeSum));
		long itemFee = correctItemFeeSum;

		if (!prepaid) {
			if (leftFee < itemFee) {
				String tip =
						Boolean.TRUE.equals(activity.getIsPassphraseEnabled())
								? "超过个人口令通道额度"
								: "超过活动限额";
				applyLimitTip(od, tip, isCheck);
			}
		} else {
			// 运费可能尚未组装；先用当前可解析金额写入，运费后再 assertPrepaidPointQuotaAfterFreight 强制阻断
			int prepaidPayable = resolvePrepaidPayableFee(od);
			od.put("prepaid_payable_fee", prepaidPayable);
			if (leftFee < prepaidPayable) {
				throw new ResourceException("预充点数不足");
			}
		}

		if (minimumAmount > 0 && itemFee < minimumAmount) {
			String yuan =
					BigDecimal.valueOf(minimumAmount)
							.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
							.toPlainString();
			applyLimitTip(od, "订单商品定额不能少于" + yuan + "元", isCheck);
		}

		od.put("act_id", activityId);
		for (Object o : rawList) {
			if (o instanceof Map<?, ?> rawMap) {
				((Map<String, Object>) rawMap).put("act_id", activityId);
			}
		}
		od.put("items", rawList);
	}

	@Override
	public void assertPrepaidPointQuotaAfterFreight(NormalOrderCreateParams p, boolean isCheck) {
		Map<String, Object> pr = p.getParams();
		Map<String, Object> od = p.getOrderData();
		if (pr == null || od == null) {
			return;
		}
		if (!"normal_employee_purchase".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L) {
			return;
		}
		Activities activity =
				employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(
						companyId, activityId, enterpriseId);
		if (!PurchaseModeSupport.isPrepaidPoint(activity)) {
			return;
		}
		if (employeePurchaseActivityDataService.isActiveRelative(
				companyId, enterpriseId, activityId, userId)) {
			throw new ResourceException("预充点数活动不支持亲友购");
		}
		od.put("pay_type", PurchaseModeSupport.PREPAID_POINT);
		od.put("freight_type", "point");
		pr.put("pay_type", PurchaseModeSupport.PREPAID_POINT);
		int prepaidPayable = resolvePrepaidPayableFee(od);
		od.put("prepaid_payable_fee", prepaidPayable);
		od.put("purchase_mode", activity.getPurchaseMode());
		Map<String, Object> aggregateFee =
				employeePurchaseActivityDataService.getAggregateFeeForInvitee(
						companyId, enterpriseId, activityId, userId);
		long leftFee = longVal(aggregateFee.get("left_fee"), 0L);
		od.put("left_fee", leftFee);
		if (leftFee < prepaidPayable) {
			applyLimitTip(od, "预充点数不足", isCheck);
		}
	}

	/** prepaid_payable_fee = total_fee；否则 item_fee + freight_fee。 */
	private static int resolvePrepaidPayableFee(Map<String, Object> od) {
		long totalFee = longVal(od.get("total_fee"), 0L);
		if (totalFee > 0L) {
			return (int) Math.min(totalFee, Integer.MAX_VALUE);
		}
		long itemFee = longVal(od.get("item_fee"), 0L);
		long freightFee = longVal(od.get("freight_fee"), 0L);
		long sum = itemFee + freightFee;
		return (int) Math.min(Math.max(sum, 0L), Integer.MAX_VALUE);
	}

	private static void applyLimitTip(Map<String, Object> od, String tip, boolean isCheck) {
		if (isCheck) {
			throw new ResourceException(tip);
		}
		od.put("extraTips", tip);
	}

	private static Map<Long, EmployeePurchaseItemLimitValidator.ItemLimit> toItemLimits(
			Map<Long, ActivityItems> activityItemByItemId) {
		LinkedHashMap<Long, EmployeePurchaseItemLimitValidator.ItemLimit> out = new LinkedHashMap<>();
		for (Map.Entry<Long, ActivityItems> e : activityItemByItemId.entrySet()) {
			out.put(e.getKey(), EmployeePurchaseCartItemLimitService.toItemLimit(e.getValue()));
		}
		return out;
	}

	private static Map<Long, EmployeePurchaseItemLimitValidator.ItemAggregate> toItemAggregates(
			Map<Long, MemberActivityItemsAggregate> aggregateByItemId) {
		LinkedHashMap<Long, EmployeePurchaseItemLimitValidator.ItemAggregate> out = new LinkedHashMap<>();
		for (Map.Entry<Long, MemberActivityItemsAggregate> e : aggregateByItemId.entrySet()) {
			MemberActivityItemsAggregate agg = e.getValue();
			out.put(
					e.getKey(),
					new EmployeePurchaseItemLimitValidator.ItemAggregate(
							agg.getAggregateNum() == null ? 0 : agg.getAggregateNum(),
							agg.getAggregateFee() == null ? 0 : agg.getAggregateFee()));
		}
		return out;
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

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
