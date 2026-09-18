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

package cn.shopex.ecshopx.orders.service.serviceorder;

import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.SubOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.event.OrderProcessLogSpringEvent;
import cn.shopex.ecshopx.orders.event.SaasErpOrderSyncSpringEvent;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.SubOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.normal.MemberConsumptionOnNormalOrderFinishService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderBrokerageOnFinishService;
import cn.shopex.ecshopx.orders.service.normal.OrdersRelChinaumspayDivisionWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class ServiceOrderZitiWriteoffService {

	private static final Logger log = LoggerFactory.getLogger(ServiceOrderZitiWriteoffService.class);

	private final ServiceOrdersMapper serviceOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SubOrdersMapper subOrdersMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;
	private final MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService;
	private final OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	public ServiceOrderZitiWriteoffService(
			ServiceOrdersMapper serviceOrdersMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			SubOrdersMapper subOrdersMapper,
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService,
			MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService,
			OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService,
			ApplicationEventPublisher applicationEventPublisher,
			WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher) {
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.subOrdersMapper = subOrdersMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.normalOrderBrokerageOnFinishService = normalOrderBrokerageOnFinishService;
		this.memberConsumptionOnNormalOrderFinishService = memberConsumptionOnNormalOrderFinishService;
		this.ordersRelChinaumspayDivisionWriteService = ordersRelChinaumspayDivisionWriteService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.wxOrderShippingDispatchPublisher = wxOrderShippingDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> orderZitiWriteoffForAdmin(
			long companyId,
			long orderId,
			long operatorId,
			boolean pickupcodeStatus,
			String pickupcodeForLog) {
		ServiceOrders svc =
				serviceOrdersMapper.selectOne(
						new LambdaQueryWrapper<ServiceOrders>()
								.eq(ServiceOrders::getCompanyId, companyId)
								.eq(ServiceOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (svc == null) {
			throw new ResourceException("此订单不存在！");
		}

		int leftAftersalesNum = sumSubOrderNumsForAftersalesPolicy(companyId, orderId);

		int now = (int) (System.currentTimeMillis() / 1000L);

		LambdaUpdateWrapper<ServiceOrders> suw = new LambdaUpdateWrapper<>();
		suw.eq(ServiceOrders::getCompanyId, companyId)
				.eq(ServiceOrders::getOrderId, orderId)
				.set(ServiceOrders::getOrderStatus, "DONE")
				.set(ServiceOrders::getUpdateTime, now);
		int svcUpdated = serviceOrdersMapper.update(null, suw);
		if (svcUpdated == 0) {
			throw new ResourceException("订单状态已变更，无法核销");
		}

		LambdaUpdateWrapper<OrderAssociations> aw = new LambdaUpdateWrapper<>();
		aw.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderId)
				.set(OrderAssociations::getOrderStatus, "DONE")
				.set(OrderAssociations::getDeliveryStatus, "DONE")
				.set(OrderAssociations::getCancelStatus, "NO_APPLY_CANCEL")
				.set(OrderAssociations::getDeliveryTime, now)
				.set(OrderAssociations::getEndTime, (long) now)
				.set(OrderAssociations::getUpdateTime, now);
		int assocUpdated = orderAssociationsMapper.update(null, aw);
		if (assocUpdated == 0) {
			throw new ResourceException("订单状态已变更，无法核销");
		}

		NormalOrders snap =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (snap == null) {
			snap = new NormalOrders();
			snap.setOrderId(orderId);
			snap.setCompanyId(companyId);
			snap.setUserId(svc.getUserId());
		}

		normalOrderBrokerageOnFinishService.orderFinishBrokerage(companyId, orderId, snap);
		memberConsumptionOnNormalOrderFinishService.updateMemberConsumptionIfNotPointPay(companyId, snap);

		String pickupLog = pickupcodeForLog == null ? "" : pickupcodeForLog;
		String detail = "订单号：" + orderId + "，订单核销";
		if (pickupcodeStatus && StringUtils.hasText(pickupLog)) {
			detail += "，核销号：" + pickupLog;
		}
		Map<String, Object> processLog = new LinkedHashMap<>();
		processLog.put("order_id", orderId);
		processLog.put("company_id", companyId);
		processLog.put("operator_type", "admin");
		processLog.put("is_show", true);
		processLog.put("operator_id", operatorId);
		processLog.put("remarks", "订单核销");
		processLog.put("detail", detail);
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("order_id", orderId);
		logParams.put("company_id", companyId);
		logParams.put("pickupcode_status", pickupcodeStatus);
		logParams.put("pickupcode", pickupLog);
		logParams.put("operator_type", "admin");
		logParams.put("operator_id", operatorId);
		processLog.put("params", logParams);
		applicationEventPublisher.publishEvent(new OrderProcessLogSpringEvent(this, processLog));

		if ("chinaums".equalsIgnoreCase(safeTrim(snap.getPayType()))
				&& snap.getDistributorId() != null
				&& snap.getDistributorId() > 0L) {
			ordersRelChinaumspayDivisionWriteService.addRelChinaumsPayDivision(companyId, snap);
		}

		Trade trade = findPrimarySuccessTrade(companyId, orderId);
		String tradeId = trade != null ? trade.getTradeId() : null;

		Map<String, Object> shippingPayload = new LinkedHashMap<>();
		shippingPayload.put("company_id", companyId);
		shippingPayload.put("order_id", orderId);
		shippingPayload.put("trade_id", tradeId);
		shippingPayload.put("user_id", snap.getUserId());
		shippingPayload.put("receipt_type", "ziti");
		shippingPayload.put("delivery_type", "batch");
		shippingPayload.put("is_all_delivered", Boolean.TRUE);
		shippingPayload.put("delivery_corp", "");
		shippingPayload.put("delivery_code", "");
		shippingPayload.put("delivery_items", Collections.emptyList());
		if (trade != null && StringUtils.hasText(trade.getWxaAppid())) {
			shippingPayload.put("wxa_appid", trade.getWxaAppid());
		}
		scheduleWxOrderShippingDispatchAfterCommit(shippingPayload);

		if (StringUtils.hasText(tradeId)) {
			Map<String, Object> erpPayload = new LinkedHashMap<>();
			erpPayload.put("company_id", companyId);
			erpPayload.put("order_id", orderId);
			erpPayload.put("trade_id", tradeId);
			applicationEventPublisher.publishEvent(new SaasErpOrderSyncSpringEvent(this, erpPayload));
		} else {
			log.debug("skip erp/trade sync: no success trade for orderId={} companyId={}", orderId, companyId);
		}

		OrderAssociations reloaded =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (reloaded == null) {
			throw new ResourceException("此订单不存在！");
		}
		return associationRowToMap(reloaded, leftAftersalesNum);
	}

	private void scheduleWxOrderShippingDispatchAfterCommit(Map<String, Object> shippingPayload) {
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						wxOrderShippingDispatchPublisher.publish(shippingPayload);
					}
				});
	}

	private int sumSubOrderNumsForAftersalesPolicy(long companyId, long orderId) {
		List<SubOrders> subLines =
				subOrdersMapper.selectList(
						new LambdaQueryWrapper<SubOrders>()
								.eq(SubOrders::getCompanyId, companyId)
								.eq(SubOrders::getOrderId, orderId));
		if (subLines.isEmpty()) {
			return 0;
		}
		int sum = 0;
		for (SubOrders line : subLines) {
			if (line != null && line.getNum() != null) {
				sum += line.getNum().intValue();
			}
		}
		return sum;
	}

	private Trade findPrimarySuccessTrade(long companyId, long orderId) {
		List<Trade> list =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getTradeState, "SUCCESS")
								.last("ORDER BY time_start DESC LIMIT 1"));
		return list.isEmpty() ? null : list.get(0);
	}

	private static String safeTrim(String s) {
		return s == null ? "" : s.trim();
	}

	private static Map<String, Object> associationRowToMap(OrderAssociations a, int leftAftersalesNum) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", a.getOrderId());
		m.put("authorizer_appid", a.getAuthorizerAppid());
		m.put("wxa_appid", a.getWxaAppid());
		m.put("title", a.getTitle());
		m.put("total_fee", a.getTotalFee());
		m.put("company_id", a.getCompanyId());
		m.put("shop_id", a.getShopId());
		m.put("store_name", a.getStoreName());
		m.put("user_id", a.getUserId());
		m.put("promoter_user_id", a.getPromoterUserId());
		m.put("promoter_shop_id", a.getPromoterShopId());
		m.put("salesman_id", a.getSalesmanId());
		m.put("source_id", a.getSourceId());
		m.put("monitor_id", a.getMonitorId());
		m.put("mobile", a.getMobile());
		m.put("order_class", a.getOrderClass());
		m.put("order_type", a.getOrderType());
		m.put("order_status", a.getOrderStatus());
		m.put("create_time", a.getCreateTime());
		m.put("update_time", a.getUpdateTime());
		m.put("is_distribution", a.getIsDistribution());
		m.put("total_rebate", a.getTotalRebate());
		m.put("delivery_corp", a.getDeliveryCorp());
		m.put("delivery_code", a.getDeliveryCode());
		m.put("member_discount", a.getMemberDiscount());
		m.put("coupon_discount", a.getCouponDiscount());
		m.put("coupon_discount_desc", List.of());
		m.put("member_discount_desc", List.of());
		m.put("delivery_status", a.getDeliveryStatus());
		m.put("delivery_time", a.getDeliveryTime());
		m.put("cancel_status", a.getCancelStatus());
		m.put("end_time", a.getEndTime());
		m.put("fee_type", a.getFeeType());
		m.put("fee_rate", a.getFeeRate());
		m.put("fee_symbol", a.getFeeSymbol());
		m.put("left_aftersales_num", leftAftersalesNum);
		return m;
	}
}
