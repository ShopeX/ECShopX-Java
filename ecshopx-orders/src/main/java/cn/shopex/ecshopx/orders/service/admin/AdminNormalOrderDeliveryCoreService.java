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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDeliveryItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.event.SaasErpOrderSyncSpringEvent;
import cn.shopex.ecshopx.orders.integration.notify.OrderDeliverySuccessNotifyPort;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Core physical delivery flow shared by multiple HTTP controllers. Third-party trade updates are
 * scheduled for dispatch only after the surrounding transaction commits, giving every caller the
 * same post-commit scheduling behavior.
 */
@Service
public class AdminNormalOrderDeliveryCoreService {

	private static final String KUAIDI_REDIS_PREFIX = "kuaidiTypeOpenConfig:";
	private static final Pattern POSITIVE_INT = Pattern.compile("^[1-9][0-9]*$");
	private static final DateTimeFormatter DELIVERY_NO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final OrdersDeliveryItemsMapper ordersDeliveryItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final CompanyRelLogisticsMapper companyRelLogisticsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final OrderDeliverySuccessNotifyPort orderDeliverySuccessNotifyPort;
	private final TradeMapper tradeMapper;
	private final ObjectMapper objectMapper;
	private final WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;
	private final NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher;

	public AdminNormalOrderDeliveryCoreService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrdersDeliveryMapper ordersDeliveryMapper,
			OrdersDeliveryItemsMapper ordersDeliveryItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			CompanyRelLogisticsMapper companyRelLogisticsMapper,
			SupplierOrderMapper supplierOrderMapper,
			StringRedisTemplate stringRedisTemplate,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			ApplicationEventPublisher applicationEventPublisher,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			OrderDeliverySuccessNotifyPort orderDeliverySuccessNotifyPort,
			TradeMapper tradeMapper,
			ObjectMapper objectMapper,
			WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher,
			ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher,
			NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.ordersDeliveryItemsMapper = ordersDeliveryItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.companyRelLogisticsMapper = companyRelLogisticsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.orderDeliverySuccessNotifyPort = orderDeliverySuccessNotifyPort;
		this.tradeMapper = tradeMapper;
		this.objectMapper = objectMapper;
		this.wxOrderShippingDispatchPublisher = wxOrderShippingDispatchPublisher;
		this.thirdPartyTradeUpdateDispatchPublisher = thirdPartyTradeUpdateDispatchPublisher;
		this.normalOrderDeliveryDispatchPublisher = normalOrderDeliveryDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deliveryNormalPhysical(
			Map<String, Object> params, OrderAssociations assoc, @SuppressWarnings("unused") String effectiveOrderType) {
		long companyId = parseLongRequired(params.get("company_id"), "company_id");
		long orderId = parseLongRequired(params.get("order_id"), "order_id");
		int supplierId = parseIntDefault(params.get("supplier_id"), 0);

		NormalOrders normalProbe =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (normalProbe == null) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		if ("merchant".equalsIgnoreCase(safe(normalProbe.getReceiptType()))) {
			String dc = stringParam(params, "delivery_code");
			if (!StringUtils.hasText(dc)) {
				params.put("delivery_code", nextDeliveryNo(companyId, nullToZero(normalProbe.getDistributorId())));
			}
		}

		requireParam(params, "delivery_type", "缺少类型");
		requireParam(params, "delivery_corp", "缺少快递公司");
		requireParam(params, "delivery_code", "缺少物流单号");
		String deliveryType = stringParam(params, "delivery_type");
		if ("sep".equalsIgnoreCase(deliveryType)) {
			requireParam(params, "sepInfo", "缺少拆单发货信息");
		}

		String deliveryCorp = stringParam(params, "delivery_corp");
		String deliveryCode = stringParam(params, "delivery_code");

		SupplierOrder supplierRow = null;
		NormalOrders orderRowForState = normalProbe;
		if (supplierId > 0) {
			supplierRow =
					supplierOrderMapper.selectOne(
							new LambdaQueryWrapper<SupplierOrder>()
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getSupplierId, supplierId)
									.eq(SupplierOrder::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (supplierRow == null) {
				throw new ResourceException("订单号为" + orderId + "的订单不存在");
			}
			validateOrderState(
					safe(supplierRow.getOrderStatus()),
					safe(supplierRow.getCancelStatus()),
					safe(supplierRow.getDeliveryStatus()),
					orderId);
			if ("merchant".equalsIgnoreCase(safe(supplierRow.getReceiptType()))) {
				validateMerchantSelfParams(params);
			}
		} else {
			validateOrderState(
					safe(orderRowForState.getOrderStatus()),
					safe(orderRowForState.getCancelStatus()),
					safe(orderRowForState.getDeliveryStatus()),
					orderId);
			if ("merchant".equalsIgnoreCase(safe(orderRowForState.getReceiptType()))) {
				validateMerchantSelfParams(params);
			}
		}

		List<Map<String, Object>> sepParsed = null;
		if ("sep".equalsIgnoreCase(deliveryType)) {
			sepParsed = parseSepInfo(params, orderId);
		}

		DeliveryBuildResult built =
				buildDeliveryPayload(
						params, companyId, orderId, supplierId, deliveryType, deliveryCorp, supplierRow, orderRowForState, sepParsed);

		String corpSource = readKuaidiOpenType(companyId);
		String corpDisplayName = getDeliveryCorpName(companyId, deliveryCorp, supplierId);
		int now = (int) (System.currentTimeMillis() / 1000L);

		OrdersDelivery delivery = new OrdersDelivery();
		delivery.setCompanyId(companyId);
		delivery.setSupplierId(supplierId);
		delivery.setOrderId(orderId);
		delivery.setUserId(built.userId);
		delivery.setDeliveryCorpName(corpDisplayName);
		delivery.setDeliveryCorp(deliveryCorp);
		delivery.setDeliveryCode(deliveryCode);
		delivery.setDeliveryCorpSource(corpSource);
		delivery.setReceiverMobile(built.receiverMobile);
		delivery.setPackageType(deliveryType.toLowerCase());
		delivery.setSelfDeliveryOperatorId(parseLongParam(params.get("self_delivery_operator_id"), 0L));
		delivery.setDeliveryRemark(stringParam(params, "delivery_remark"));
		delivery.setDeliveryPics(deliveryPicsSerialized(params, objectMapper));
		delivery.setDeliveryTime(now);
		delivery.setCreated(now);
		delivery.setUpdated(now);
		ordersDeliveryMapper.insert(delivery);
		Long deliveryId = delivery.getOrdersDeliveryId();
		if (deliveryId == null || deliveryId <= 0L) {
			throw new ResourceException("创建发货单失败");
		}

		for (OrdersDeliveryItems di : built.deliveryItems) {
			di.setOrdersDeliveryId(deliveryId);
			ordersDeliveryItemsMapper.insert(di);
		}

		if (!built.fullyShippedItemIds.isEmpty()) {
			LambdaUpdateWrapper<NormalOrdersItems> iu = new LambdaUpdateWrapper<>();
			iu.in(NormalOrdersItems::getId, built.fullyShippedItemIds)
					.set(NormalOrdersItems::getDeliveryStatus, "DONE")
					.set(NormalOrdersItems::getDeliveryTime, now)
					.set(NormalOrdersItems::getUpdateTime, now);
			normalOrdersItemsMapper.update(null, iu);
		}
		for (DeliveryItemNum n : built.itemNumUpdates) {
			LambdaUpdateWrapper<NormalOrdersItems> iu = new LambdaUpdateWrapper<>();
			iu.eq(NormalOrdersItems::getId, n.itemId()).set(NormalOrdersItems::getDeliveryItemNum, n.newTotal());
			normalOrdersItemsMapper.update(null, iu);
		}

		boolean hasPending = hasPendingItems(companyId, orderId);
		Map<String, Object> validity = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		int finishDays = intFromSetting(validity.get("order_finish_time"), 7);
		long finishSeconds = finishDays * 24L * 3600L;

		NormalOrders orderBeforeUpdate =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (orderBeforeUpdate == null) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		int leftBase =
				orderBeforeUpdate.getLeftAftersalesNum() == null ? 0 : orderBeforeUpdate.getLeftAftersalesNum();

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getOrderId, orderId).eq(NormalOrders::getCompanyId, companyId);
		ou.set(NormalOrders::getLeftAftersalesNum, leftBase + built.canAftersalesNum);
		if (hasPending) {
			ou.set(NormalOrders::getDeliveryStatus, "PARTAIL");
		} else {
			ou.set(NormalOrders::getDeliveryCorpSource, corpSource)
					.set(NormalOrders::getDeliveryStatus, "DONE")
					.set(NormalOrders::getDeliveryTime, now)
					.set(NormalOrders::getAutoFinishTime, String.valueOf(now + finishSeconds))
					.set(NormalOrders::getOrderStatus, "WAIT_BUYER_CONFIRM");
			if ("merchant".equalsIgnoreCase(safe(orderBeforeUpdate.getReceiptType()))) {
				ou.set(
						NormalOrders::getSelfDeliveryOperatorId,
						parseLongParam(params.get("self_delivery_operator_id"), 0L));
				String sds = stringParam(params, "self_delivery_status");
				if (StringUtils.hasText(sds)) {
					ou.set(NormalOrders::getSelfDeliveryStatus, sds);
				}
			}
		}
		ou.set(NormalOrders::getDeliveryCorp, deliveryCorp).set(NormalOrders::getDeliveryCode, deliveryCode);
		ou.set(NormalOrders::getUpdateTime, now);
		normalOrdersMapper.update(null, ou);

		if (supplierId > 0) {
			updateSupplierShipStatus(orderId, supplierId);
		}

		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getOrderId, orderId).eq(OrderAssociations::getCompanyId, companyId);
		if (hasPending) {
			au.set(OrderAssociations::getDeliveryStatus, "PARTAIL");
		} else {
			au.set(OrderAssociations::getDeliveryStatus, "DONE")
					.set(OrderAssociations::getDeliveryTime, now)
					.set(OrderAssociations::getOrderStatus, "WAIT_BUYER_CONFIRM");
		}
		au.set(OrderAssociations::getUpdateTime, now);
		int assocUpd = orderAssociationsMapper.update(null, au);
		if (assocUpd == 0) {
			throw new ResourceException("订单关联信息不存在");
		}

		boolean isAllDelivered = !hasPending;
		publishProcessLog(params, companyId, orderId, supplierId);

		publishWxShipping(
				companyId,
				orderId,
				deliveryCode,
				deliveryType,
				isAllDelivered,
				deliveryCorp,
				deliveryCode,
				built.deliveryItemsForNotice);

		Map<String, Object> normalDeliveryPayload = new LinkedHashMap<>();
		normalDeliveryPayload.put("order_id", orderId);
		normalDeliveryPayload.put("company_id", companyId);
		scheduleNormalOrderDeliveryDispatchAfterCommit(normalDeliveryPayload);

		sendDeliveryNotices(params, companyId, orderId, deliveryType, built.deliveryItemsForNotice, delivery);

		Trade trade = findPrimarySuccessTrade(companyId, orderId);
		String tradeId = trade != null ? trade.getTradeId() : null;
		scheduleSaasErpAfterCommit(companyId, orderId, tradeId);
		scheduleThirdPartyTradeUpdateDispatchAfterCommit(companyId, orderId);
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

	private void scheduleSaasErpAfterCommit(long companyId, long orderId, String tradeId) {
		if (!StringUtils.hasText(tradeId)) {
			return;
		}
		final Object source = this;
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						Map<String, Object> erpPayload = new LinkedHashMap<>();
						erpPayload.put("company_id", companyId);
						erpPayload.put("order_id", orderId);
						erpPayload.put("trade_id", tradeId);
						applicationEventPublisher.publishEvent(new SaasErpOrderSyncSpringEvent(source, erpPayload));
					}
				});
	}

	private void scheduleNormalOrderDeliveryDispatchAfterCommit(Map<String, Object> normalDeliveryPayload) {
		Runnable publish = () -> normalOrderDeliveryDispatchPublisher.publish(normalDeliveryPayload);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publish.run();
						}
					});
		} else {
			publish.run();
		}
	}

	private void scheduleThirdPartyTradeUpdateDispatchAfterCommit(long companyId, long orderId) {
		Runnable publishAfterRead =
				() -> {
					Map<String, Object> payload = buildThirdPartyTradeUpdateEntitiesPayload(companyId, orderId);
					if (payload == null || payload.isEmpty()) {
						return;
					}
					thirdPartyTradeUpdateDispatchPublisher.publish(payload);
				};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publishAfterRead.run();
						}
					});
		} else {
			publishAfterRead.run();
		}
	}

	private Map<String, Object> buildThirdPartyTradeUpdateEntitiesPayload(long companyId, long orderId) {
		OrderAssociations assocRow =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assocRow == null) {
			return null;
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", assocRow.getCompanyId() != null ? assocRow.getCompanyId() : companyId);
		payload.put("order_id", String.valueOf(assocRow.getOrderId() != null ? assocRow.getOrderId() : orderId));
		payload.put("user_id", assocRow.getUserId() == null ? 0L : assocRow.getUserId());
		String orderClass = safe(assocRow.getOrderClass());
		if (!StringUtils.hasText(orderClass)) {
			orderClass = "normal";
		}
		payload.put("order_class", orderClass);
		appendOrderAssociationScalars(payload, assocRow);
		return payload;
	}

	private static void appendOrderAssociationScalars(LinkedHashMap<String, Object> payload, OrderAssociations row) {
		putIfNotNull(payload, "authorizer_appid", row.getAuthorizerAppid());
		putIfNotNull(payload, "wxa_appid", row.getWxaAppid());
		putIfNotNull(payload, "title", row.getTitle());
		putIfNotNull(payload, "total_fee", row.getTotalFee());
		putIfNotNull(payload, "shop_id", row.getShopId());
		putIfNotNull(payload, "store_name", row.getStoreName());
		putIfNotNull(payload, "promoter_user_id", row.getPromoterUserId());
		putIfNotNull(payload, "promoter_shop_id", row.getPromoterShopId());
		putIfNotNull(payload, "source_id", row.getSourceId());
		putIfNotNull(payload, "monitor_id", row.getMonitorId());
		putIfNotNull(payload, "mobile", row.getMobile());
		putIfNotNull(payload, "order_type", row.getOrderType());
		putIfNotNull(payload, "order_status", row.getOrderStatus());
		putIfNotNull(payload, "is_distribution", row.getIsDistribution());
		putIfNotNull(payload, "total_rebate", row.getTotalRebate());
		putIfNotNull(payload, "delivery_corp", row.getDeliveryCorp());
		putIfNotNull(payload, "delivery_code", row.getDeliveryCode());
		putIfNotNull(payload, "delivery_time", row.getDeliveryTime());
		putIfNotNull(payload, "member_discount", row.getMemberDiscount());
		putIfNotNull(payload, "coupon_discount", row.getCouponDiscount());
		putIfNotNull(payload, "coupon_discount_desc", row.getCouponDiscountDesc());
		putIfNotNull(payload, "member_discount_desc", row.getMemberDiscountDesc());
		putIfNotNull(payload, "delivery_status", row.getDeliveryStatus());
		putIfNotNull(payload, "cancel_status", row.getCancelStatus());
		putIfNotNull(payload, "create_time", row.getCreateTime());
		putIfNotNull(payload, "end_time", row.getEndTime());
		putIfNotNull(payload, "update_time", row.getUpdateTime());
		putIfNotNull(payload, "fee_type", row.getFeeType());
		putIfNotNull(payload, "fee_rate", row.getFeeRate());
		putIfNotNull(payload, "fee_symbol", row.getFeeSymbol());
		putIfNotNull(payload, "salesman_id", row.getSalesmanId());
	}

	private static void putIfNotNull(LinkedHashMap<String, Object> payload, String key, Object value) {
		if (value == null) {
			return;
		}
		if (value instanceof String s && !StringUtils.hasText(s)) {
			return;
		}
		payload.put(key, value);
	}

	private void sendDeliveryNotices(
			Map<String, Object> params,
			long companyId,
			long orderId,
			String packageType,
			List<Map<String, Object>> itemRows,
			OrdersDelivery delivery) {
		Map<String, Object> base = new LinkedHashMap<>();
		base.put("company_id", companyId);
		base.put("order_id", orderId);
		base.put("user_id", params.get("user_id"));
		base.put("delivery_corp", delivery.getDeliveryCorp());
		base.put("delivery_code", delivery.getDeliveryCode());
		if (StringUtils.hasText(stringParam(params, "logi_name"))) {
			base.put("logi_name", stringParam(params, "logi_name"));
		}
		base.put("delivery_corp_source", delivery.getDeliveryCorpSource());
		if ("sep".equalsIgnoreCase(packageType)) {
			for (Map<String, Object> item : itemRows) {
				Map<String, Object> send = new LinkedHashMap<>(base);
				send.put("item_name", item.get("item_name"));
				orderDeliverySuccessNotifyPort.sendDeliverySuccessNotice(send);
			}
		} else {
			orderDeliverySuccessNotifyPort.sendDeliverySuccessNotice(base);
		}
	}

	private void publishWxShipping(
			long companyId,
			long orderId,
			String deliveryCodeRaw,
			String packageType,
			boolean isAllDelivered,
			String deliveryCorp,
			String deliveryCode,
			List<Map<String, Object>> itemRows) {
		Trade trade = findPrimarySuccessTrade(companyId, orderId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("trade_id", trade != null ? trade.getTradeId() : null);
		NormalOrders n =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (n != null) {
			payload.put("user_id", n.getUserId());
		}
		if (trade != null && StringUtils.hasText(trade.getWxaAppid())) {
			payload.put("wxa_appid", trade.getWxaAppid());
		}
		String receiptType;
		if (n != null && "merchant".equalsIgnoreCase(safe(n.getReceiptType()))) {
			receiptType = "dada";
		} else {
			receiptType = "dada".equalsIgnoreCase(deliveryCodeRaw) ? "dada" : "logistics";
		}
		payload.put("receipt_type", receiptType);
		String deliveryTypeNorm =
				packageType == null ? "" : packageType.trim().toLowerCase(Locale.ROOT);
		payload.put("delivery_type", deliveryTypeNorm);
		payload.put("is_all_delivered", isAllDelivered);
		payload.put("delivery_corp", deliveryCorp);
		payload.put("delivery_code", deliveryCode);
		payload.put("delivery_items", itemRows);
		scheduleWxOrderShippingDispatchAfterCommit(payload);
	}

	private void publishProcessLog(Map<String, Object> params, long companyId, long orderId, int supplierId) {
		Map<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderId);
		log.put("company_id", companyId);
		log.put("supplier_id", supplierId);
		log.put("operator_type", params.getOrDefault("operator_type", "system"));
		log.put("operator_id", parseLongParam(params.get("operator_id"), 0L));
		log.put("is_show", true);
		log.put("remarks", "订单发货");
		log.put("detail", "订单号：" + orderId + "，订单发货");
		log.put("delivery_remark", stringParam(params, "delivery_remark"));
		log.put("pics", params.get("delivery_pics"));
		log.put("params", new LinkedHashMap<>(params));
		orderProcessLogPublishPort.publish(log);
	}

	private void updateSupplierShipStatus(long orderId, int supplierId) {
		LambdaQueryWrapper<NormalOrdersItems> qw = new LambdaQueryWrapper<>();
		qw.eq(NormalOrdersItems::getOrderId, orderId).eq(NormalOrdersItems::getSupplierId, supplierId);
		List<NormalOrdersItems> lines = normalOrdersItemsMapper.selectList(qw);
		String deliveryStatus = "DONE";
		for (NormalOrdersItems v : lines) {
			int num = v.getNum() == null ? 0 : v.getNum();
			int sent = v.getDeliveryItemNum() == null ? 0 : v.getDeliveryItemNum();
			int cancelled = v.getCancelItemNum() == null ? 0 : v.getCancelItemNum();
			if (sent + cancelled < num) {
				deliveryStatus = "PARTAIL";
				break;
			}
		}
		LambdaUpdateWrapper<SupplierOrder> su = new LambdaUpdateWrapper<>();
		su.eq(SupplierOrder::getOrderId, orderId).eq(SupplierOrder::getSupplierId, supplierId);
		su.set(SupplierOrder::getDeliveryStatus, deliveryStatus);
		if ("DONE".equals(deliveryStatus)) {
			su.set(SupplierOrder::getOrderStatus, "WAIT_BUYER_CONFIRM");
		}
		supplierOrderMapper.update(null, su);
	}

	private boolean hasPendingItems(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getCompanyId, companyId));
		return PartialDeliveryFulfillmentReconcileService.hasPendingShippableItems(items);
	}

	private DeliveryBuildResult buildDeliveryPayload(
			Map<String, Object> params,
			long companyId,
			long orderId,
			int supplierId,
			String deliveryType,
			String deliveryCorp,
			SupplierOrder supplierRow,
			NormalOrders normalRow,
			List<Map<String, Object>> sepParsed) {
		long userId;
		String receiverMobile;
		if (supplierRow != null) {
			userId = supplierRow.getUserId() == null ? 0L : supplierRow.getUserId();
			receiverMobile = firstNonBlank(supplierRow.getReceiverMobile(), supplierRow.getMobile());
		} else {
			userId = normalRow.getUserId() == null ? 0L : normalRow.getUserId();
			receiverMobile = firstNonBlank(normalRow.getReceiverMobile(), normalRow.getMobile());
		}
		params.put("user_id", userId);

		List<OrdersDeliveryItems> deliveryItems = new ArrayList<>();
		List<Long> fullyShipped = new ArrayList<>();
		List<DeliveryItemNum> itemNums = new ArrayList<>();
		List<Map<String, Object>> noticeRows = new ArrayList<>();
		int canAftersales = 0;
		int now = (int) (System.currentTimeMillis() / 1000L);

		if ("batch".equalsIgnoreCase(deliveryType)) {
			LambdaQueryWrapper<NormalOrdersItems> qw = new LambdaQueryWrapper<>();
			qw.eq(NormalOrdersItems::getCompanyId, companyId).eq(NormalOrdersItems::getOrderId, orderId);
			if (supplierId > 0) {
				qw.eq(NormalOrdersItems::getSupplierId, supplierId);
			} else {
				// 平台/店铺整单发货与拆单一致：不得发供应商商品行
				qw.and(
						w -> w.isNull(NormalOrdersItems::getSupplierId).or().eq(NormalOrdersItems::getSupplierId, 0));
			}
			List<NormalOrdersItems> lines = normalOrdersItemsMapper.selectList(qw);
			if (lines.isEmpty() && supplierId <= 0) {
				LambdaQueryWrapper<NormalOrdersItems> anyQw = new LambdaQueryWrapper<>();
				anyQw.eq(NormalOrdersItems::getCompanyId, companyId).eq(NormalOrdersItems::getOrderId, orderId);
				if (normalOrdersItemsMapper.selectCount(anyQw) > 0) {
					throw new ResourceException("供应商商品请由供应商发货");
				}
			}
			for (NormalOrdersItems it : lines) {
				OrdersDeliveryItems di = baseDeliveryItem(companyId, orderId, supplierId, now, it);
				deliveryItems.add(di);
				fullyShipped.add(it.getId());
				itemNums.add(new DeliveryItemNum(it.getId(), it.getNum() == null ? 0 : it.getNum()));
				canAftersales += Math.max(it.getNum() == null ? 0 : it.getNum(), 0);
				noticeRows.add(itemToNoticeMap(it));
			}
		} else {
			for (Map<String, Object> val : sepParsed) {
				Object deliveryNumObj = val.get("delivery_num");
				if (deliveryNumObj == null || !StringUtils.hasText(String.valueOf(deliveryNumObj).trim())) {
					throw new ResourceException(
							"订单号为" + orderId + "的订单,发货商品数量格式错误：" + val);
				}
				String numStr = String.valueOf(deliveryNumObj).trim();
				if (!POSITIVE_INT.matcher(numStr).matches()) {
					throw new ResourceException("订单号为" + orderId + "的订单,发货商品数量错误:" + numStr);
				}
				int num = Integer.parseInt(numStr);
				long orderItemsId = parseLongRequired(val.get("id"), "id");
				NormalOrdersItems info =
						normalOrdersItemsMapper.selectOne(
								new LambdaQueryWrapper<NormalOrdersItems>()
										.eq(NormalOrdersItems::getId, orderItemsId)
										.last("LIMIT 1"));
				if (info == null) {
					throw new ResourceException("订单号为" + orderId + "的订单,发货商品不正确");
				}
				if ("DONE".equalsIgnoreCase(safe(info.getDeliveryStatus()))) {
					throw new ResourceException("订单号为" + orderId + "的订单,发货商品已发货");
				}
				// 平台/店铺发货不得包含供应商商品行（应由供应商后台发货）
				int lineSupplierId = info.getSupplierId() == null ? 0 : info.getSupplierId();
				if (supplierId <= 0 && lineSupplierId > 0) {
					throw new ResourceException("供应商商品请由供应商发货");
				}
				if (supplierId > 0 && lineSupplierId != supplierId) {
					throw new ResourceException("订单号为" + orderId + "的订单,发货商品不正确");
				}
				int orderNum = info.getNum() == null ? 0 : info.getNum();
				int sent = info.getDeliveryItemNum() == null ? 0 : info.getDeliveryItemNum();
				int remain = orderNum - sent;
				if (num > remain) {
					throw new ResourceException(
							"订单号为" + orderId + "的订单,发货商品数量" + num + "大于购买数量" + remain);
				}
				OrdersDeliveryItems di = new OrdersDeliveryItems();
				di.setCompanyId(companyId);
				di.setOrderId(orderId);
				di.setOrderItemsId(orderItemsId);
				di.setGoodsId(info.getGoodsId() == null ? 0L : info.getGoodsId());
				di.setItemId(info.getItemId() == null ? 0L : info.getItemId());
				di.setNum(num);
				di.setItemName(info.getItemName());
				di.setPic(info.getPic());
				di.setCreated(now);
				di.setUpdated(now);
				deliveryItems.add(di);
				if (orderNum - (sent + num) == 0) {
					fullyShipped.add(orderItemsId);
				}
				itemNums.add(new DeliveryItemNum(orderItemsId, sent + num));
				canAftersales += num;
				noticeRows.add(itemToNoticeMap(di, info));
			}
		}
		return new DeliveryBuildResult(
				userId,
				receiverMobile,
				deliveryItems,
				fullyShipped,
				itemNums,
				canAftersales,
				noticeRows);
	}

	private static Map<String, Object> itemToNoticeMap(NormalOrdersItems it) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_name", it.getItemName());
		m.put("num", it.getNum());
		return m;
	}

	private static Map<String, Object> itemToNoticeMap(OrdersDeliveryItems di, NormalOrdersItems info) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_name", di.getItemName() != null ? di.getItemName() : info.getItemName());
		m.put("num", di.getNum());
		return m;
	}

	private static OrdersDeliveryItems baseDeliveryItem(
			long companyId, long orderId, int supplierId, int now, NormalOrdersItems it) {
		OrdersDeliveryItems di = new OrdersDeliveryItems();
		di.setCompanyId(companyId);
		di.setOrderId(orderId);
		di.setOrderItemsId(it.getId());
		di.setGoodsId(it.getGoodsId() == null ? 0L : it.getGoodsId());
		di.setItemId(it.getItemId() == null ? 0L : it.getItemId());
		di.setNum(it.getNum() == null ? 0 : it.getNum());
		di.setItemName(it.getItemName());
		di.setPic(it.getPic());
		di.setCreated(now);
		di.setUpdated(now);
		return di;
	}

	private List<Map<String, Object>> parseSepInfo(Map<String, Object> params, long orderId) {
		Object raw = params.get("sepInfo");
		if (raw == null) {
			throw new ResourceException("拆单信息不正确");
		}
		String json = String.valueOf(raw).trim();
		if (!StringUtils.hasText(json)) {
			throw new ResourceException("拆单信息不正确");
		}
		try {
			List<Map<String, Object>> list =
					objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
			if (list == null || list.isEmpty()) {
				throw new ResourceException("拆单信息不正确");
			}
			return list;
		} catch (Exception e) {
			throw new ResourceException("拆单信息不正确");
		}
	}

	private void validateMerchantSelfParams(Map<String, Object> params) {
		if (!StringUtils.hasText(stringParam(params, "self_delivery_status"))) {
			throw new ResourceException("自配送订单发货必须选择配送状态");
		}
		long op = parseLongParam(params.get("self_delivery_operator_id"), 0L);
		if (op <= 0L) {
			throw new ResourceException("自配送订单发货必须选择配送员");
		}
	}

	private static void validateOrderState(String orderStatus, String cancelStatus, String deliveryStatus, long orderId) {
		if ("NOTPAY".equals(orderStatus)) {
			throw new ResourceException("订单未支付，不能发货");
		}
		if ("CANCEL".equals(orderStatus)) {
			throw new ResourceException("订单已取消，不能发货");
		}
		if ("WAIT_PROCESS".equals(cancelStatus) || "REFUND_PROCESS".equals(cancelStatus)) {
			throw new ResourceException("订单有退款待处理，不能发货");
		}
		if ("DONE".equals(deliveryStatus)) {
			throw new ResourceException("订单已发货，不能重复发货");
		}
	}

	private String getDeliveryCorpName(long companyId, String deliveryCorp, int supplierId) {
		String openType = readKuaidiOpenType(companyId);
		LambdaQueryWrapper<CompanyRelLogistics> qw = new LambdaQueryWrapper<>();
		qw.eq(CompanyRelLogistics::getCompanyId, (int) companyId);
		long sup = supplierId <= 0 ? 0L : supplierId;
		qw.eq(CompanyRelLogistics::getSupplierId, sup);
		if ("kuaidi100".equalsIgnoreCase(openType) && StringUtils.hasText(deliveryCorp)) {
			qw.apply("LOWER(kuaidi_code) = LOWER({0})", deliveryCorp.trim());
		} else {
			qw.eq(CompanyRelLogistics::getCorpCode, deliveryCorp);
		}
		qw.last("LIMIT 1");
		CompanyRelLogistics row = companyRelLogisticsMapper.selectOne(qw);
		if (row != null && StringUtils.hasText(row.getCorpName())) {
			return row.getCorpName().trim();
		}
		return "其他";
	}

	private String readKuaidiOpenType(long companyId) {
		String key = KUAIDI_REDIS_PREFIX + sha1Hex(String.valueOf(companyId));
		try {
			String v = stringRedisTemplate.opsForValue().get(key);
			return StringUtils.hasText(v) ? v.trim() : "";
		} catch (DataAccessException e) {
			return "";
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private String nextDeliveryNo(long companyId, long distributorId) {
		String date = LocalDate.now(ZoneId.systemDefault()).format(DELIVERY_NO_DATE);
		String key = "DeliveryNo:" + date + ":" + companyId + ":" + distributorId;
		Long n = stringRedisTemplate.opsForValue().increment(key);
		if (n == null) {
			n = 1L;
		}
		return date + "#_" + n;
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

	private static void requireParam(Map<String, Object> params, String key, String message) {
		Object v = params.get(key);
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			throw new ResourceException(message);
		}
	}

	private static String stringParam(Map<String, Object> params, String key) {
		Object v = params.get(key);
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String deliveryPicsSerialized(Map<String, Object> params, ObjectMapper mapper) {
		Object v = params.get("delivery_pics");
		if (v == null) {
			return "[]";
		}
		if (v instanceof String s) {
			return StringUtils.hasText(s) ? s : "[]";
		}
		try {
			return mapper.writeValueAsString(v);
		} catch (Exception e) {
			return "[]";
		}
	}

	private static long parseLongRequired(Object v, String label) {
		if (v == null) {
			throw new ResourceException("缺少订单id");
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("缺少订单id");
		}
	}

	private static long parseLongParam(Object v, long dflt) {
		if (v == null) {
			return dflt;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static int parseIntDefault(Object v, int dflt) {
		if (v == null) {
			return dflt;
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static int intFromSetting(Object v, int dflt) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v == null) {
			return dflt;
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private static long nullToZero(Long v) {
		return v == null ? 0L : v;
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a.trim();
		}
		if (StringUtils.hasText(b)) {
			return b.trim();
		}
		return "";
	}

	private record DeliveryItemNum(long itemId, int newTotal) {}

	private record DeliveryBuildResult(
			long userId,
			String receiverMobile,
			List<OrdersDeliveryItems> deliveryItems,
			List<Long> fullyShippedItemIds,
			List<DeliveryItemNum> itemNumUpdates,
			int canAftersalesNum,
			List<Map<String, Object>> deliveryItemsForNotice) {}
}
