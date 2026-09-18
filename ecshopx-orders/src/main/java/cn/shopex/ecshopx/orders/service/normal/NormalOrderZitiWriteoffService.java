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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderZitiWriteoffService {

	private static final Logger log = LoggerFactory.getLogger(NormalOrderZitiWriteoffService.class);

	private static final ConcurrentHashMap<String, Object> OPENAPI_ZITI_WRITEOFF_MUTEX =
			new ConcurrentHashMap<>();

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final TradeMapper tradeMapper;
	private final NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;
	private final MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService;
	private final OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;
	private final WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	public NormalOrderZitiWriteoffService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			TradeMapper tradeMapper,
			NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService,
			MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService,
			OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService,
			WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.tradeMapper = tradeMapper;
		this.normalOrderBrokerageOnFinishService = normalOrderBrokerageOnFinishService;
		this.memberConsumptionOnNormalOrderFinishService = memberConsumptionOnNormalOrderFinishService;
		this.ordersRelChinaumspayDivisionWriteService = ordersRelChinaumspayDivisionWriteService;
		this.wxOrderShippingDispatchPublisher = wxOrderShippingDispatchPublisher;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.thirdPartyTradeUpdateDispatchPublisher = thirdPartyTradeUpdateDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void orderZitiWriteoffForChief(long companyId, long orderId, long chiefId) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("此订单不存在！");
		}
		validateChiefWriteoffContext(order, chiefId);
		applyZitiWriteoffTransaction(
				order,
				companyId,
				orderId,
				chiefId,
				"user",
				"自提核销",
				"订单号：" + orderId + "，团长自提核销成功",
				true,
				false,
				"");
	}

	@Transactional(rollbackFor = Exception.class)
	public void orderZitiWriteoffForBuyerOperator(long companyId, long orderId, long operatorUserId) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("此订单不存在！");
		}
		applyZitiWriteoffTransaction(
				order,
				companyId,
				orderId,
				operatorUserId,
				"user",
				"自提核销",
				"订单号：" + orderId + "，会员自提核销成功",
				true,
				false,
				"");
	}

	@Transactional(rollbackFor = Exception.class)
	public void orderZitiWriteoffForAdmin(long companyId, long orderId, long operatorId) {
		orderZitiWriteoffForAdmin(companyId, orderId, operatorId, false, "");
	}

	@Transactional(rollbackFor = Exception.class)
	public void orderZitiWriteoffForOpenapi(
			long companyId,
			long orderId,
			boolean pickupcodeStatus,
			String pickupcodeForLog) {
		String lockKey = companyId + ":" + orderId;
		Object mutex = OPENAPI_ZITI_WRITEOFF_MUTEX.computeIfAbsent(lockKey, k -> new Object());
		synchronized (mutex) {
			NormalOrders order =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (order == null) {
				throw new ResourceException("此订单不存在！");
			}
			validateOpenApiZitiWriteoffContext(order);
			String detail = "订单号: " + orderId + ", 已被核销. ";
			if (pickupcodeStatus && StringUtils.hasText(pickupcodeForLog)) {
				detail += "核销号: " + pickupcodeForLog;
			}
			String pickupLog = pickupcodeForLog == null ? "" : pickupcodeForLog;
			applyZitiWriteoffTransaction(
					order,
					companyId,
					orderId,
					0L,
					"openapi",
					"订单核销",
					detail,
					true,
					pickupcodeStatus,
					pickupLog);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void orderZitiWriteoffForAdmin(
			long companyId,
			long orderId,
			long operatorId,
			boolean pickupcodeStatus,
			String pickupcodeForLog) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("此订单不存在！");
		}
		String detail = "订单号: " + orderId + ", 已被核销. ";
		if (pickupcodeStatus && StringUtils.hasText(pickupcodeForLog)) {
			detail += "核销号: " + pickupcodeForLog;
		}
		String pickupLog = pickupcodeForLog == null ? "" : pickupcodeForLog;
		applyZitiWriteoffTransaction(
				order,
				companyId,
				orderId,
				operatorId,
				"admin",
				"订单核销",
				detail,
				false,
				pickupcodeStatus,
				pickupLog);
	}

	private void applyZitiWriteoffTransaction(
			NormalOrders order,
			long companyId,
			long orderId,
			long operatorIdForLog,
			String processLogOperatorType,
			String processLogRemarks,
			String processLogDetailLine,
			boolean requirePendingZiti,
			boolean pickupcodeStatusForLog,
			String pickupcodeForLog) {
		List<NormalOrdersItems> lines =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		int totalNum = 0;
		for (NormalOrdersItems line : lines) {
			int n = line.getNum() == null ? 0 : line.getNum();
			totalNum += n;
		}
		if (totalNum <= 0) {
			totalNum = 1;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> validity = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		Object v = validity == null ? null : validity.get("latest_aftersale_time");
		String settingStr;
		if (v == null) {
			settingStr = "";
		} else if (v instanceof String) {
			settingStr = ((String) v).trim();
		} else if (v instanceof Number) {
			settingStr = Long.toString(((Number) v).longValue());
		} else {
			settingStr = "";
		}
		long days = LeadingNumberParser.parseAsLong(settingStr);
		long autoCloseEpoch = (long) now + days * 86400L;

		LambdaUpdateWrapper<NormalOrders> uw = new LambdaUpdateWrapper<>();
		uw.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, orderId);
		if (requirePendingZiti) {
			uw.eq(NormalOrders::getZitiStatus, "PENDING");
		}
		uw.set(NormalOrders::getZitiStatus, "DONE")
				.set(NormalOrders::getOrderStatus, "DONE")
				.set(NormalOrders::getDeliveryStatus, "DONE")
				.set(NormalOrders::getDeliveryTime, now)
				.set(NormalOrders::getEndTime, (long) now)
				.set(NormalOrders::getUpdateTime, now)
				.set(NormalOrders::getLeftAftersalesNum, totalNum)
				.set(NormalOrders::getOrderAutoCloseAftersalesTime, (int) autoCloseEpoch);
		int mainUpdated = normalOrdersMapper.update(null, uw);
		if (mainUpdated == 0) {
			throw new ResourceException("订单状态已变更，无法核销");
		}

		LambdaUpdateWrapper<NormalOrdersItems> iw = new LambdaUpdateWrapper<>();
		iw.eq(NormalOrdersItems::getCompanyId, companyId)
				.eq(NormalOrdersItems::getOrderId, orderId)
				.set(NormalOrdersItems::getDeliveryStatus, "DONE")
				.set(NormalOrdersItems::getDeliveryTime, now)
				.set(NormalOrdersItems::getUpdateTime, now);
		normalOrdersItemsMapper.update(null, iw);

		if (autoCloseEpoch > 0) {
			for (NormalOrdersItems line : lines) {
				if (line == null || line.getId() == null) {
					continue;
				}
				LambdaUpdateWrapper<NormalOrdersItems> acw = new LambdaUpdateWrapper<>();
				acw.eq(NormalOrdersItems::getId, line.getId())
						.set(NormalOrdersItems::getAutoCloseAftersalesTime, (int) autoCloseEpoch);
				normalOrdersItemsMapper.update(null, acw);
			}
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
		orderAssociationsMapper.update(null, aw);

		normalOrderBrokerageOnFinishService.orderFinishBrokerage(companyId, orderId, order);
		memberConsumptionOnNormalOrderFinishService.updateMemberConsumptionIfNotPointPay(companyId, order);

		Map<String, Object> processLog = new LinkedHashMap<>();
		processLog.put("order_id", Long.toString(orderId));
		processLog.put("company_id", Long.toString(companyId));
		processLog.put("operator_type", processLogOperatorType);
		processLog.put("is_show", Boolean.FALSE);
		processLog.put("operator_id", operatorIdForLog);
		processLog.put("remarks", processLogRemarks);
		processLog.put("detail", processLogDetailLine);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", Long.toString(orderId));
		params.put("company_id", Long.toString(companyId));
		params.put("pickupcode_status", pickupcodeStatusForLog);
		params.put("pickupcode", pickupcodeForLog == null ? "" : pickupcodeForLog);
		params.put("operator_type", processLogOperatorType);
		params.put("operator_id", operatorIdForLog);
		processLog.put("params", params);
		orderProcessLogPublishPort.publish(processLog);

		if ("chinaums".equalsIgnoreCase(safeTrim(order.getPayType()))
				&& order.getDistributorId() != null
				&& order.getDistributorId() > 0L) {
			ordersRelChinaumspayDivisionWriteService.addRelChinaumsPayDivision(companyId, order);
		}

		Trade trade = findPrimarySuccessTrade(companyId, orderId);
		String tradeId = trade != null ? trade.getTradeId() : null;

		Map<String, Object> shippingPayload = new LinkedHashMap<>();
		shippingPayload.put("company_id", companyId);
		shippingPayload.put("order_id", orderId);
		shippingPayload.put("trade_id", tradeId);
		shippingPayload.put("user_id", order.getUserId());
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
			Map<String, Object> bundle =
					adminNormalOrderDetailService.buildOrderBundle(
							companyId, String.valueOf(orderId), false);
			Object orderInfoObj = bundle == null ? null : bundle.get("orderInfo");
			if (orderInfoObj instanceof Map<?, ?> rawOrderInfo) {
				@SuppressWarnings("unchecked")
				Map<String, Object> orderInfo = (Map<String, Object>) rawOrderInfo;
				thirdPartyTradeUpdateDispatchPublisher.publish(new LinkedHashMap<>(orderInfo));
			} else {
				log.debug(
						"skip trade update dispatch: orderInfo missing for orderId={} companyId={}",
						orderId,
						companyId);
			}
		} else {
			log.debug("skip erp/trade sync: no success trade for orderId={} companyId={}", orderId, companyId);
		}
	}

	private void validateOpenApiZitiWriteoffContext(NormalOrders order) {
		if (!"ziti".equalsIgnoreCase(safeTrim(order.getReceiptType()))) {
			throw new ResourceException("订单收货方式不允许核销");
		}
		if (!"PAYED".equalsIgnoreCase(safeTrim(order.getOrderStatus()))) {
			throw new ResourceException("订单状态不允许核销");
		}
		if (!"PENDING".equalsIgnoreCase(safeTrim(order.getZitiStatus()))) {
			throw new ResourceException("自提状态不允许核销");
		}
		if (!"PAYED".equalsIgnoreCase(safeTrim(order.getPayStatus()))) {
			throw new ResourceException("订单未支付，无法核销");
		}
		String cs = safeTrim(order.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equals(cs) && !"FAILS".equals(cs)) {
			throw new ResourceException("订单取消状态不允许核销");
		}
	}

	private void validateChiefWriteoffContext(NormalOrders order, long chiefId) {
		if (chiefId <= 0L) {
			throw new ResourceException("核销操作无效");
		}
		if (!"community".equals(safeTrim(order.getOrderClass()))) {
			throw new ResourceException("订单类型不允许核销");
		}
		if (!"PAYED".equals(safeTrim(order.getPayStatus()))) {
			throw new ResourceException("订单未支付，无法核销");
		}
		String cs = safeTrim(order.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equals(cs) && !"FAILS".equals(cs)) {
			throw new ResourceException("订单取消状态不允许核销");
		}
		if (!"PENDING".equals(safeTrim(order.getZitiStatus()))) {
			throw new ResourceException("自提状态不允许核销");
		}
		if (order.getActId() == null || order.getActId() <= 0L) {
			throw new ResourceException("活动信息缺失");
		}
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
}
