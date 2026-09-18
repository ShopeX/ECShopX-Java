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

import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateCouponConsumePort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseEmptyCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreatePostCommitPort;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartDeleteDataService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatePostCommitPortImpl implements OrderCreatePostCommitPort {

	private final OrderCreateCouponConsumePort orderCreateCouponConsumePort;
	private final OrderCreateEmployeePurchaseEmptyCartPort orderCreateEmployeePurchaseEmptyCartPort;
	private final OrderDirectedCrowdDiscountPort orderDirectedCrowdDiscountPort;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final OperatorCartDeleteDataService operatorCartDeleteDataService;
	private final CartMapper cartMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final SendPayOrdersRemindJobDispatchPublisher sendPayOrdersRemindJobDispatchPublisher;
	private final MemberAccountService memberAccountService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderSaveOrderRelZitiService normalOrderSaveOrderRelZitiService;

	public OrderCreatePostCommitPortImpl(
			OrderCreateCouponConsumePort orderCreateCouponConsumePort,
			OrderCreateEmployeePurchaseEmptyCartPort orderCreateEmployeePurchaseEmptyCartPort,
			OrderDirectedCrowdDiscountPort orderDirectedCrowdDiscountPort,
			PointMemberAddPointService pointMemberAddPointService,
			OperatorCartDeleteDataService operatorCartDeleteDataService,
			CartMapper cartMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
			SendPayOrdersRemindJobDispatchPublisher sendPayOrdersRemindJobDispatchPublisher,
			MemberAccountService memberAccountService,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			NormalOrderSaveOrderRelZitiService normalOrderSaveOrderRelZitiService) {
		this.orderCreateCouponConsumePort = orderCreateCouponConsumePort;
		this.orderCreateEmployeePurchaseEmptyCartPort = orderCreateEmployeePurchaseEmptyCartPort;
		this.orderDirectedCrowdDiscountPort = orderDirectedCrowdDiscountPort;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.operatorCartDeleteDataService = operatorCartDeleteDataService;
		this.cartMapper = cartMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.sendPayOrdersRemindJobDispatchPublisher = sendPayOrdersRemindJobDispatchPublisher;
		this.memberAccountService = memberAccountService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.normalOrderSaveOrderRelZitiService = normalOrderSaveOrderRelZitiService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void runAfterOrderInsert(NormalOrderCreateParams p) {
		saveOrderRelZitiIfNeeded(p);
		orderCreateCouponConsumePort.consumeIfNeeded(p);
		orderDirectedCrowdDiscountPort.persistUserTotalDiscountIfNeeded(p);
		Map<String, Object> pr = p.getParams();
		long companyId = longVal(pr.get("company_id"), 0L);
		long operatorId = longVal(pr.get("operator_id"), 0L);
		Map<String, Object> od = p.getOrderData();
		long userId = longVal(od != null ? od.get("user_id") : 0L, 0L);
		int pointUse = intVal(od != null ? od.get("point_use") : 0, 0);
		String payType = od == null ? "" : String.valueOf(od.getOrDefault("pay_type", ""));
		String orderClassEarly = od == null ? "" : stringVal(od.get("order_class"));
		int orderPoint = intVal(od != null ? od.get("point") : 0, 0);
		// 积分商城（纯积分 / 混合）统一在创建时扣 order.point；失败抛错致整单回滚。
		// 与下方普通商城「积分抵扣现金」互斥，避免双扣。
		if ("pointsmall".equals(orderClassEarly)
				&& orderPoint > 0
				&& companyId > 0L
				&& userId > 0L) {
			pointMemberAddPointService.addPointForManualAdjustment(
					userId, companyId, orderPoint, false, "购物扣减积分");
		} else if (companyId > 0L && userId > 0L && pointUse > 0 && !"point".equals(payType)) {
			pointMemberAddPointService.addPointForManualAdjustment(
					userId, companyId, pointUse, false, "订单抵扣积分");
		}
		emptyCartIfSupported(p, companyId, operatorId, userId, od, pr);
		Map<String, Object> res = p.getOrdersInsertResult();

		Map<String, Object> processLog = new LinkedHashMap<>();
		Object rawOrderId = res.get("order_id");
		processLog.put("order_id", rawOrderId == null ? null : String.valueOf(rawOrderId));
		Object rawCompanyId = res.get("company_id");
		if (rawCompanyId == null && od != null) {
			rawCompanyId = od.get("company_id");
		}
		processLog.put("company_id", rawCompanyId == null ? null : String.valueOf(rawCompanyId));
		processLog.put("operator_type", "user");
		processLog.put("is_show", Boolean.TRUE);
		processLog.put("operator_id", userId);
		String orderClass = od == null ? "" : stringVal(od.get("order_class"));
		String remarks;
		String detailMsg;
		if ("excard".equals(orderClass)) {
			remarks = "订单核销";
			detailMsg = "订单核销成功";
		} else {
			remarks = "订单创建";
			detailMsg = "订单创建";
		}
		String orderIdStr = rawOrderId == null ? "" : String.valueOf(rawOrderId);
		processLog.put("remarks", remarks);
		processLog.put("detail", "订单号：" + orderIdStr + "，" + detailMsg);
		processLog.put("params", new LinkedHashMap<>(pr));
		orderProcessLogPublishPort.publish(processLog);

		publishPayOrdersRemindIfApplicable(companyId, userId, res, od, pr);
	}

	private void saveOrderRelZitiIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		Map<String, Object> od = p.getOrderData();
		Map<String, Object> res = p.getOrdersInsertResult();
		if (!"ziti".equals(stringVal(pr.get("receipt_type")))) {
			return;
		}
		String orderClass = od == null ? "" : stringVal(od.get("order_class"));
		if ("shopadmin".equals(orderClass) || "community".equals(orderClass)) {
			return;
		}
		LinkedHashMap<String, Object> zitiData = new LinkedHashMap<>();
		Object orderId = res != null && res.get("order_id") != null ? res.get("order_id") : (od == null ? null : od.get("order_id"));
		Object companyId =
				res != null && res.get("company_id") != null
						? res.get("company_id")
						: (od == null ? pr.get("company_id") : od.get("company_id"));
		zitiData.put("order_id", orderId);
		zitiData.put("company_id", companyId);
		zitiData.put("pickup_location", pr.get("pickup_location"));
		zitiData.put("pickup_date", pr.get("pickup_date"));
		zitiData.put("pickup_time", pr.get("pickup_time"));
		normalOrderSaveOrderRelZitiService.saveOrderRelZiti(zitiData);
	}

	/**
	 * Aligns with {@code WxappOrderCreateService#publishPayOrdersRemindIfApplicable}: same inner
	 * {@code orderData} keys passed to {@link SendPayOrdersRemindJobDispatchPublisher#publish(Map)}.
	 */
	private void publishPayOrdersRemindIfApplicable(
			long companyId,
			long userId,
			Map<String, Object> result,
			Map<String, Object> orderData,
			Map<String, Object> params) {
		Map<String, Object> authInfo = buildAuthInfoForRemind(orderData, userId, companyId);
		LinkedHashMap<String, Object> odRemind = new LinkedHashMap<>();
		odRemind.put("order_id", result.get("order_id"));
		odRemind.put("company_id", companyId);
		odRemind.put("user_id", userId);
		odRemind.put("wxa_appid", stringVal(authInfo.get("wxapp_appid")));
		odRemind.put("open_id", stringVal(authInfo.get("open_id")));
		odRemind.put("total_fee", result.get("total_fee"));
		odRemind.put("title", result.get("title"));
		odRemind.put("fee_symbol", result.get("fee_symbol"));
		odRemind.put("fee_rate", result.get("fee_rate"));
		if (result.get("create_time") != null) {
			odRemind.put("create_time", result.get("create_time"));
		}
		if (params.get("auto_cancel_time") != null) {
			odRemind.put("auto_cancel_time", params.get("auto_cancel_time"));
		} else if (result.get("auto_cancel_time") != null) {
			odRemind.put("auto_cancel_time", result.get("auto_cancel_time"));
		}
		sendPayOrdersRemindJobDispatchPublisher.publish(odRemind);
	}

	private void emptyCartIfSupported(
			NormalOrderCreateParams p,
			long companyId,
			long operatorId,
			long userId,
			Map<String, Object> od,
			Map<String, Object> pr) {
		Object itemsRaw = pr.get("items");
		if (!(itemsRaw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		String orderType = stringVal(pr.get("order_type"));
		if ("normal_employee_purchase".equals(orderType)) {
			orderCreateEmployeePurchaseEmptyCartPort.emptyCart(p, userId);
			return;
		}
		if ("normal_shopadmin".equals(orderType)) {
			emptyOperatorCart(companyId, operatorId, list);
			return;
		}
		String orderClass = od != null ? stringVal(od.get("order_class")) : "";
		if ("community".equals(orderClass) || "excard".equals(orderClass)) {
			return;
		}
		String cartType = stringVal(pr.get("cart_type"));
		if (cartType.isEmpty()) {
			cartType = "cart";
		}
		long cartUserId = userId > 0L ? userId : longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || cartUserId <= 0L) {
			return;
		}
		if ("fastbuy".equals(cartType)) {
			clearFastBuyCart(companyId, cartUserId);
			return;
		}
		List<Long> itemIds = collectItemIds(list);
		if (itemIds.isEmpty()) {
			return;
		}
		long shopId = longVal(pr.get("distributor_id"), 0L);
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, cartUserId)
				.eq(Cart::getShopId, shopId)
				.in(Cart::getItemId, itemIds);
		cartMapper.delete(w);
	}

	private void emptyOperatorCart(long companyId, long operatorId, List<?> list) {
		for (Object o : list) {
			if (o instanceof Map<?, ?> row) {
				long itemId = longVal(row.get("item_id"), 0L);
				if (itemId > 0L) {
					operatorCartDeleteDataService.delCartData(
							companyId, operatorId, null, Long.valueOf(itemId));
				}
			}
		}
	}

	private void clearFastBuyCart(long companyId, long userId) {
		String key = "fastbuy:" + DigestUtils.sha1Hex(String.valueOf(companyId) + userId);
		stringRedisTemplate.opsForValue().set(key, "[]", Duration.ofSeconds(600));
	}

	private static List<Long> collectItemIds(List<?> list) {
		List<Long> itemIds = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> row) {
				long itemId = longVal(row.get("item_id"), 0L);
				if (itemId > 0L) {
					itemIds.add(itemId);
				}
			}
		}
		return itemIds;
	}

	private Map<String, Object> buildAuthInfoForRemind(
			Map<String, Object> orderData, long userId, long companyId) {
		LinkedHashMap<String, Object> authInfo = new LinkedHashMap<>();
		if (orderData != null) {
			Object wxappAppid = orderData.get("wxapp_appid");
			if (wxappAppid == null) {
				wxappAppid = orderData.get("wxa_appid");
			}
			if (wxappAppid != null) {
				authInfo.put("wxapp_appid", wxappAppid);
			}
			if (orderData.get("open_id") != null) {
				authInfo.put("open_id", orderData.get("open_id"));
			}
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		for (Map.Entry<String, Object> e : member.entrySet()) {
			authInfo.putIfAbsent(e.getKey(), e.getValue());
		}
		return authInfo;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
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
}
