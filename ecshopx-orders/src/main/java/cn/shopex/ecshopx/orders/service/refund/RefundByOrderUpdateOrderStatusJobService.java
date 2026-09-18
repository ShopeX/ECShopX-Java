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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class RefundByOrderUpdateOrderStatusJobService {

	private static final Logger log = LoggerFactory.getLogger(RefundByOrderUpdateOrderStatusJobService.class);

	private static final String AUTO_CANCEL_REASON = "拼团自动取消";

	private final TradeMapper tradeMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final ServiceOrdersMapper serviceOrdersMapper;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;
	private final AftersalesRefundService aftersalesRefundService;
	private final OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;
	private final TransactionTemplate transactionTemplate;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	public RefundByOrderUpdateOrderStatusJobService(
			TradeMapper tradeMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			ServiceOrdersMapper serviceOrdersMapper,
			CancelOrdersMapper cancelOrdersMapper,
			AdminNormalOrderFullCancelService adminNormalOrderFullCancelService,
			AftersalesRefundService aftersalesRefundService,
			OrderSuccessTradeReadPort orderSuccessTradeReadPort,
			TradeCancelSettingRedisService tradeCancelSettingRedisService,
			TransactionTemplate transactionTemplate,
			ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher) {
		this.tradeMapper = tradeMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.adminNormalOrderFullCancelService = adminNormalOrderFullCancelService;
		this.aftersalesRefundService = aftersalesRefundService;
		this.orderSuccessTradeReadPort = orderSuccessTradeReadPort;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
		this.transactionTemplate = transactionTemplate;
		this.thirdPartyTradeUpdateDispatchPublisher = thirdPartyTradeUpdateDispatchPublisher;
	}

	public void execute(long orderId, long companyId, String orderType) {
		Trade trade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 1"));
		if (trade == null) {
			log.debug(
					"refund-by-order-update-order-status skip: no SUCCESS trade orderId={} companyId={}",
					orderId,
					companyId);
			return;
		}

		String normalizedType = orderType == null ? "" : orderType.trim().toLowerCase(Locale.ROOT);
		if ("service_groups".equals(normalizedType)) {
			transactionTemplate.executeWithoutResult(status -> cancelPayedServiceGroupOrder(companyId, orderId));
			return;
		}
		if ("normal_groups".equals(normalizedType)) {
			cancelNormalGroupsPaidOrder(companyId, orderId);
			return;
		}

		log.debug(
				"refund-by-order-update-order-status skip: unknown order_type={} orderId={} companyId={}",
				orderType,
				orderId,
				companyId);
	}

	private void cancelNormalGroupsPaidOrder(long companyId, long orderId) {
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			log.debug("refund-by-order-update-order-status skip: no association orderId={}", orderId);
			return;
		}

		long userId = assoc.getUserId() == null ? 0L : assoc.getUserId();
		String mobile = assoc.getMobile() == null ? "" : assoc.getMobile();
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("other_reason", AUTO_CANCEL_REASON);
		adminNormalOrderFullCancelService.execute(
				companyId, "shop", 0L, 0L, userId, mobile, orderId, "", params, "system");
	}

	private void cancelPayedServiceGroupOrder(long companyId, long orderId) {
		ServiceOrders so =
				serviceOrdersMapper.selectOne(
						new LambdaQueryWrapper<ServiceOrders>()
								.eq(ServiceOrders::getCompanyId, companyId)
								.eq(ServiceOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (so == null || !"PAYED".equalsIgnoreCase(safe(so.getOrderStatus()))) {
			log.debug(
					"refund-by-order-update-order-status skip: no PAYED service order orderId={} companyId={}",
					orderId,
					companyId);
			return;
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			log.debug("refund-by-order-update-order-status skip: no association orderId={}", orderId);
			return;
		}

		if (!"PENDING".equalsIgnoreCase(safe(assoc.getDeliveryStatus()))) {
			log.debug("refund-by-order-update-order-status skip: delivery not PENDING orderId={}", orderId);
			return;
		}

		String cancelStatus = safe(assoc.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equalsIgnoreCase(cancelStatus) && !"FAILS".equalsIgnoreCase(cancelStatus)) {
			log.debug("refund-by-order-update-order-status skip: cancel already in progress orderId={}", orderId);
			return;
		}

		long userId = so.getUserId() == null ? 0L : so.getUserId();
		Optional<Map<String, Object>> tradeOpt = orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId);
		if (tradeOpt.isEmpty()) {
			throw new ResourceException("支付信息未找到！");
		}
		Map<String, Object> trade = tradeOpt.get();
		String tradeId = String.valueOf(trade.get("trade_id"));
		String tradePayType = safe(String.valueOf(trade.get("pay_type")));

		Map<String, Object> setting = tradeCancelSettingRedisService.getCancelSetting(companyId);
		boolean repeat = Boolean.TRUE.equals(setting.get("repeat_cancel"));

		CancelOrders existing =
				cancelOrdersMapper.selectOne(
						new LambdaQueryWrapper<CancelOrders>()
								.eq(CancelOrders::getCompanyId, companyId)
								.eq(CancelOrders::getOrderId, orderId)
								.eq(CancelOrders::getUserId, userId)
								.last("LIMIT 1"));

		int now = (int) (System.currentTimeMillis() / 1000L);
		if (existing != null) {
			if (!repeat && !isRejectedCancelOrder(existing)) {
				throw new ResourceException("不能重复取消订单");
			}
			LambdaUpdateWrapper<CancelOrders> uw = new LambdaUpdateWrapper<>();
			uw.eq(CancelOrders::getCancelId, existing.getCancelId())
					.set(CancelOrders::getProgress, 0)
					.set(CancelOrders::getCancelReason, AUTO_CANCEL_REASON)
					.set(CancelOrders::getCancelFrom, "system")
					.set(CancelOrders::getRefundStatus, "WAIT_CHECK")
					.set(CancelOrders::getShopRejectReason, null)
					.set(CancelOrders::getUpdateTime, now);
			cancelOrdersMapper.update(null, uw);
			CancelOrders reloaded = cancelOrdersMapper.selectById(existing.getCancelId());
			if (reloaded == null) {
				throw new ResourceException("订单取消失败！");
			}
		} else {
			CancelOrders row = new CancelOrders();
			row.setOrderId(orderId);
			row.setCompanyId(companyId);
			row.setSupplierId(0L);
			row.setShopId(so.getShopId() == null ? 0L : so.getShopId());
			row.setUserId(userId);
			row.setDistributorId(0L);
			row.setOrderType("service");
			row.setTotalFee(parseMoneyLong(so.getTotalFee()));
			row.setPoint(0);
			row.setPayType(tradePayType);
			row.setProgress(0);
			row.setCancelFrom("system");
			row.setCancelReason(AUTO_CANCEL_REASON);
			row.setRefundStatus("WAIT_CHECK");
			row.setCreateTime(now);
			row.setUpdateTime(now);
			cancelOrdersMapper.insert(row);
			if (row.getCancelId() == null || row.getCancelId() <= 0) {
				throw new ResourceException("订单取消失败！");
			}
		}

		int totalFee = parseMoneyInt(so.getTotalFee());
		int refundFee = Math.max(0, totalFee);
		String refundChannel = "offline_pay".equalsIgnoreCase(tradePayType) ? "offline" : "original";
		long shopId = so.getShopId() == null ? 0L : so.getShopId();
		long distributorId = 0L;

		Map<String, Object> p = new LinkedHashMap<>();
		p.put("company_id", companyId);
		p.put("user_id", userId);
		p.put("order_id", orderId);
		p.put("trade_id", tradeId);
		p.put("shop_id", shopId);
		p.put("distributor_id", distributorId);
		p.put("merchant_id", longVal(trade.get("merchant_id")));
		p.put("supplier_id", 0L);
		p.put("refund_fee", refundFee);
		p.put("refund_point", 0);
		p.put("return_freight", 1);
		p.put("freight", 0);
		p.put("freight_type", "cash");
		p.put("pay_type", tradePayType);
		p.put("refund_type", "1");
		p.put("refund_channel", refundChannel);
		p.put("refund_status", "READY");
		p.put("currency", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("fee_type")));
		p.put("cur_fee_type", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("cur_fee_type")));
		p.put("cur_fee_rate", trade.get("cur_fee_rate"));
		p.put("cur_fee_symbol", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("cur_fee_symbol")));
		p.put(
				"cur_pay_fee",
				"point".equalsIgnoreCase(tradePayType)
						? "0"
						: String.valueOf((int) Math.round(refundFee * doubleVal(trade.get("cur_fee_rate")))));

		aftersalesRefundService.createRefund(p);

		serviceOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<ServiceOrders>()
						.eq(ServiceOrders::getCompanyId, companyId)
						.eq(ServiceOrders::getOrderId, orderId)
						.set(ServiceOrders::getOrderStatus, "CANCEL")
						.set(ServiceOrders::getUpdateTime, now));

		orderAssociationsMapper.update(
				null,
				new LambdaUpdateWrapper<OrderAssociations>()
						.eq(OrderAssociations::getCompanyId, companyId)
						.eq(OrderAssociations::getOrderId, orderId)
						.set(OrderAssociations::getOrderStatus, "CANCEL")
						.set(OrderAssociations::getCancelStatus, "SUCCESS")
						.set(OrderAssociations::getUpdateTime, now));

		OrderAssociations reloadedAssoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		Map<String, Object> tradeUpdatePayload =
				buildThirdPartyTradeUpdatePayloadForServiceGroupCancel(companyId, orderId, reloadedAssoc, trade);
		if (tradeUpdatePayload != null && !tradeUpdatePayload.isEmpty()) {
			scheduleThirdPartyTradeUpdateDispatchAfterCommit(tradeUpdatePayload);
		}
	}

	private Map<String, Object> buildThirdPartyTradeUpdatePayloadForServiceGroupCancel(
			long companyId, long orderId, OrderAssociations assocRow, Map<String, Object> trade) {
		if (assocRow == null) {
			return null;
		}
		LinkedHashMap<String, Object> erp = new LinkedHashMap<>();
		erp.put("company_id", companyId);
		erp.put("order_id", String.valueOf(orderId));
		erp.put("user_id", assocRow.getUserId() == null ? 0L : assocRow.getUserId());
		String orderClass = safe(assocRow.getOrderClass());
		if (!StringUtils.hasText(orderClass)) {
			orderClass = "service_groups";
		}
		erp.put("order_class", orderClass);
		if (trade != null && trade.get("trade_id") != null) {
			erp.put("trade_id", String.valueOf(trade.get("trade_id")));
		}
		erp.put("order_status", "CANCEL");
		return erp;
	}

	private void scheduleThirdPartyTradeUpdateDispatchAfterCommit(Map<String, Object> payload) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					thirdPartyTradeUpdateDispatchPublisher.publish(payload);
				}
			});
		} else {
			thirdPartyTradeUpdateDispatchPublisher.publish(payload);
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static boolean isRejectedCancelOrder(CancelOrders existing) {
		if (existing == null) {
			return false;
		}
		if ("SHOP_CHECK_FAILS".equalsIgnoreCase(safe(existing.getRefundStatus()))) {
			return true;
		}
		return existing.getProgress() != null && existing.getProgress() == 4;
	}

	private static long parseMoneyLong(String raw) {
		return parseMoneyInt(raw);
	}

	private static int parseMoneyInt(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static double doubleVal(Object o) {
		if (o == null) {
			return 1.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 1.0;
		}
	}
}
