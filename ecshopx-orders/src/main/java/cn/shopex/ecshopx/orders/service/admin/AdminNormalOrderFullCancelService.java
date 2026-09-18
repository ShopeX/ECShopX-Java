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

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryFormalCancelOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AdminNormalOrderFullCancelService {

	static final String CANCEL_DISPATCH_SOURCE_ADMIN_FULL = "admin_normal_order_full_cancel";

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;
	private final OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	private final AftersalesRefundService aftersalesRefundService;
	private final PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	private final OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService;
	private final DadaLocalDeliveryFormalCancelOrderPort dadaLocalDeliveryFormalCancelOrderPort;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	private final JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher;
	private final WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher;
	private final NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher;
	private final TradeRefundDispatchPublisher tradeRefundDispatchPublisher;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;
	private final NormalOrderFullCancelItemStoreRestoreService normalOrderFullCancelItemStoreRestoreService;
	private final NormalOrderStatusUpdateService normalOrderStatusUpdateService;
	private final PlatformSelfSubCancelSupport platformSelfSubCancelSupport;
	private final NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService;

	public AdminNormalOrderFullCancelService(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			CancelOrdersMapper cancelOrdersMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			TradeCancelSettingRedisService tradeCancelSettingRedisService,
			OrderSuccessTradeReadPort orderSuccessTradeReadPort,
			AftersalesRefundService aftersalesRefundService,
			PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService,
			OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService,
			DadaLocalDeliveryFormalCancelOrderPort dadaLocalDeliveryFormalCancelOrderPort,
			ApplicationEventPublisher applicationEventPublisher,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService,
			JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher,
			WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher,
			NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher,
			@Qualifier("tradeRefundAsyncFanOut") TradeRefundDispatchPublisher tradeRefundDispatchPublisher,
			ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher,
			NormalOrderFullCancelItemStoreRestoreService normalOrderFullCancelItemStoreRestoreService,
			NormalOrderStatusUpdateService normalOrderStatusUpdateService,
			PlatformSelfSubCancelSupport platformSelfSubCancelSupport,
			NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
		this.orderSuccessTradeReadPort = orderSuccessTradeReadPort;
		this.aftersalesRefundService = aftersalesRefundService;
		this.pointMemberCancelOrderReturnPointsService = pointMemberCancelOrderReturnPointsService;
		this.orderInvoiceCancelOnOrderCancelService = orderInvoiceCancelOnOrderCancelService;
		this.dadaLocalDeliveryFormalCancelOrderPort = dadaLocalDeliveryFormalCancelOrderPort;
		this.applicationEventPublisher = applicationEventPublisher;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.pointMemberMinusOrderUppointsService = pointMemberMinusOrderUppointsService;
		this.jushuitanTradeCancelDispatchPublisher = jushuitanTradeCancelDispatchPublisher;
		this.wdtErpTradeCancelDispatchPublisher = wdtErpTradeCancelDispatchPublisher;
		this.normalOrderCancelDispatchPublisher = normalOrderCancelDispatchPublisher;
		this.tradeRefundDispatchPublisher = tradeRefundDispatchPublisher;
		this.thirdPartyTradeUpdateDispatchPublisher = thirdPartyTradeUpdateDispatchPublisher;
		this.normalOrderFullCancelItemStoreRestoreService = normalOrderFullCancelItemStoreRestoreService;
		this.normalOrderStatusUpdateService = normalOrderStatusUpdateService;
		this.platformSelfSubCancelSupport = platformSelfSubCancelSupport;
		this.normalOrderCancelDiscountRestoreService = normalOrderCancelDiscountRestoreService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> execute(
			long companyId,
			String operatorType,
			long operatorId,
			long supplierId,
			long userId,
			String mobile,
			long orderId,
			String reasonText,
			Map<String, Object> mergedRequestParams) {
		return execute(
				companyId,
				operatorType,
				operatorId,
				supplierId,
				userId,
				mobile,
				orderId,
				reasonText,
				mergedRequestParams,
				"shop");
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> execute(
			long companyId,
			String operatorType,
			long operatorId,
			long supplierId,
			long userId,
			String mobile,
			long orderId,
			String reasonText,
			Map<String, Object> mergedRequestParams,
			String cancelFromForRecord) {
		boolean supplierMode = supplierId > 0;
		boolean platformSelfSubCancel = false;
		boolean shopRemainingFullCancel = false;
		NormalOrders normalOrder = null;
		SupplierOrder supplierOrder = null;
		if (supplierMode) {
			supplierOrder =
					supplierOrderMapper.selectOne(
							new LambdaQueryWrapper<SupplierOrder>()
									.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getOrderId, orderId)
									.eq(SupplierOrder::getSupplierId, supplierId)
									.eq(SupplierOrder::getUserId, userId)
									.last("LIMIT 1"));
			if (supplierOrder == null) {
				throw new ResourceException("订单号为" + orderId + "的订单不存在");
			}
		} else {
			normalOrder =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.eq(NormalOrders::getUserId, userId)
									.last("LIMIT 1"));
			if (normalOrder == null) {
				throw new ResourceException("订单号为" + orderId + "的订单不存在");
			}
			platformSelfSubCancel =
					platformSelfSubCancelSupport.isScope(
							companyId, orderId, safe(normalOrder.getDeliveryStatus()));
			shopRemainingFullCancel =
					platformSelfSubCancelSupport.isShopRemainingFullCancelScope(
							companyId, orderId, safe(normalOrder.getDeliveryStatus()));
			if (mergedRequestParams != null
					&& Boolean.TRUE.equals(
							mergedRequestParams.get(PlatformSelfSubCancelSupport.SHOP_REMAINING_FULL_CANCEL_PARAM))) {
				shopRemainingFullCancel = true;
			}
			if (!platformSelfSubCancel
					&& mergedRequestParams != null
					&& Boolean.TRUE.equals(mergedRequestParams.get(PlatformSelfSubCancelSupport.PLATFORM_SELF_SUB_CANCEL_PARAM))) {
				platformSelfSubCancel = true;
			}
		}

		String cancelStatus =
				supplierMode
						? safe(supplierOrder.getCancelStatus())
						: safe(normalOrder.getCancelStatus());
		if (!"NO_APPLY_CANCEL".equalsIgnoreCase(cancelStatus) && !"FAILS".equalsIgnoreCase(cancelStatus)) {
			throw new ResourceException("订单已取消");
		}

		String receiptType = supplierMode ? safe(supplierOrder.getReceiptType()) : safe(normalOrder.getReceiptType());
		String orderStatus =
				supplierMode ? safe(supplierOrder.getOrderStatus()) : safe(normalOrder.getOrderStatus());
		String deliveryStatus =
				supplierMode
						? safe(supplierOrder.getDeliveryStatus())
						: safe(normalOrder.getDeliveryStatus());

		if (!"dada".equalsIgnoreCase(receiptType)) {
			if (!"NOTPAY".equalsIgnoreCase(orderStatus)
					&& !"REVIEW_PASS".equalsIgnoreCase(orderStatus)
					&& !"PAYED".equalsIgnoreCase(orderStatus)) {
				throw new ResourceException("订单状态已不能申请取消");
			}
			if (!"PENDING".equalsIgnoreCase(deliveryStatus)) {
				if (!platformSelfSubCancel && !shopRemainingFullCancel) {
					throw new ResourceException("已发货订单不能取消");
				}
				if (shopRemainingFullCancel
						&& !platformSelfSubCancelSupport.hasRemainingUnshippedItems(companyId, orderId)) {
					throw new ResourceException("已发货订单不能取消");
				}
				if (platformSelfSubCancel
						&& !shopRemainingFullCancel
						&& !platformSelfSubCancelSupport.hasUndeliveredPlatformSelfItems(companyId, orderId)) {
					throw new ResourceException("已发货订单不能取消");
				}
			}
		}

		if (!supplierMode) {
			Integer tp = normalOrder.getType();
			if (tp != null
					&& tp == 1
					&& "approved".equalsIgnoreCase(safe(normalOrder.getAuditStatus()))) {
				throw new ResourceException("订单已审核成功，不能取消");
			}
		}

		NormalOrdersRelDada dadaRow =
				normalOrdersRelDadaMapper.selectOne(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.eq(NormalOrdersRelDada::getOrderId, orderId)
								.last("LIMIT 1"));
		if ("dada".equalsIgnoreCase(receiptType)) {
			if (dadaRow == null || dadaRow.getDadaStatus() == null) {
				throw new ResourceException("骑手已取货，订单不能取消");
			}
			int ds = dadaRow.getDadaStatus();
			if (!List.of(0, 1, 2, 9, 100).contains(ds)) {
				throw new ResourceException("骑手已取货，订单不能取消");
			}
		}

		boolean notPay = "NOTPAY".equalsIgnoreCase(orderStatus);
		Map<String, Object> reqParams = mergedRequestParams == null ? Map.of() : mergedRequestParams;
		if (!notPay) {
			String other = reqParams.get("other_reason") == null
					? ""
					: String.valueOf(reqParams.get("other_reason")).trim();
			if (!StringUtils.hasText(reasonText) && !StringUtils.hasText(other)) {
				throw new ResourceException("取消原因必选，");
			}
		}

		String cancelFromNorm = normalizeCancelFrom(cancelFromForRecord);
		CancelOrders cancelRow;
		Map<String, Object> dadaExtra = new LinkedHashMap<>();
		if (notPay) {
			cancelRow =
					insertCancelForNotPay(
							companyId,
							supplierId,
							supplierMode,
							normalOrder,
							supplierOrder,
							reasonText,
							cancelFromNorm);
			closeNotPayOrder(companyId, orderId, supplierMode, supplierId, normalOrder);
			maybeScheduleThirdPartyTradeUpdateAfterOrderAssociationCancel(companyId, orderId, orderStatus);
			Map<String, Object> od = orderDataForPoints(supplierMode, normalOrder, supplierOrder, orderId);
			pointMemberCancelOrderReturnPointsService.cancelOrderReturnBackPoints(od);
			if (!supplierMode
					&& normalOrder != null
					&& normalOrder.getUppointUse() != null
					&& normalOrder.getUppointUse() > 0) {
				Map<String, Object> upData = orderDataForPoints(supplierMode, normalOrder, supplierOrder, orderId);
				upData.put("uppoint_use", normalOrder.getUppointUse());
				pointMemberMinusOrderUppointsService.minusOrderUppoints(upData);
			}
			dadaExtra.put("dada_cancel_from", "12");
		} else {
			if (!supplierMode) {
				ensureNoPendingSupplierSubCancel(companyId, orderId);
			}
			cancelRow =
					insertOrUpdateCancelForPayed(
							companyId,
							userId,
							supplierId,
							supplierMode,
							platformSelfSubCancel,
							shopRemainingFullCancel,
							normalOrder,
							supplierOrder,
							reasonText,
							cancelFromNorm);
			createPayedRefunds(
					companyId,
					userId,
					orderId,
					supplierMode,
					platformSelfSubCancel,
					shopRemainingFullCancel,
					normalOrder,
					supplierOrder,
					cancelRow);
			dadaCancelIfNeeded(
					companyId,
					orderId,
					receiptType,
					cancelRow,
					supplierMode,
					normalOrder,
					supplierOrder);
			dadaExtra.put("dada_cancel_from", "12");
			if (cancelRow.getCancelId() != null && cancelRow.getCancelId() > 0) {
				long supplierScope = supplierMode ? supplierId : 0L;
				normalOrderStatusUpdateService.applyStatusUpdate(
						companyId, orderId, supplierScope, null, "WAIT_PROCESS");
				maybeScheduleThirdPartyTradeUpdateAfterOrderAssociationCancel(companyId, orderId, orderStatus);
			}
		}

		if (cancelRow.getCancelId() != null && cancelRow.getCancelId() > 0) {
			if ("dada".equalsIgnoreCase(receiptType) && dadaRow != null) {
				LambdaUpdateWrapper<NormalOrdersRelDada> du = new LambdaUpdateWrapper<>();
				du.eq(NormalOrdersRelDada::getCompanyId, companyId)
						.eq(NormalOrdersRelDada::getOrderId, orderId)
						.set(NormalOrdersRelDada::getDadaStatus, 5)
						.set(NormalOrdersRelDada::getDadaCancelFrom, intVal(dadaExtra.get("dada_cancel_from")));
				normalOrdersRelDadaMapper.update(null, du);
			}
			if ("PAYED".equalsIgnoreCase(orderStatus)) {
				Map<String, Object> erpPayload = cancelToMap(cancelRow);
				erpPayload.put("action", "cancel_order");
				jushuitanTradeCancelDispatchPublisher.publish(erpPayload);
				wdtErpTradeCancelDispatchPublisher.publish(erpPayload);
				applicationEventPublisher.publishEvent(
						new cn.shopex.ecshopx.orders.event.WdtErpTradeCancelSpringEvent(this, erpPayload));
			}
			orderInvoiceCancelOnOrderCancelService.updateInvoiceStatusCancel(companyId, orderId, "cancel");

			publishOrderCancelProcessLog(
					companyId,
					orderId,
					userId,
					operatorType,
					operatorId,
					supplierId,
					cancelFromNorm,
					reqParams,
					notPay);
		}

		Map<String, Object> res = cancelToMap(cancelRow);
		applicationEventPublisher.publishEvent(
				new cn.shopex.ecshopx.common.event.SaasErpRefundSpringEvent(this, res));

		Map<String, Object> tradeRefundPayload = new LinkedHashMap<>(res);
		if ("PAYED".equalsIgnoreCase(orderStatus)
				&& cancelRow.getCancelId() != null
				&& cancelRow.getCancelId() > 0) {
			tradeRefundPayload.put("action", "cancel_order");
			mergeFreshPreorderRefundFieldsIntoPayload(
					companyId, orderId, supplierMode, supplierMode ? supplierId : 0L, tradeRefundPayload);
		}

		Map<String, Object> cancelDispatchPayload = new LinkedHashMap<>();
		cancelDispatchPayload.put("order_id", orderId);
		cancelDispatchPayload.put("company_id", companyId);
		cancelDispatchPayload.put("source", CANCEL_DISPATCH_SOURCE_ADMIN_FULL);
		// Register TradeRefund before NormalOrderCancel so afterCommit runs trade-refund dispatch first, then
		// normal-order-cancel dispatch (SaasErp refund event is published synchronously above).
		scheduleTradeRefundDispatchAfterCommit(tradeRefundPayload);
		scheduleNormalOrderCancelDispatchAfterCommit(cancelDispatchPayload);
		return res;
	}

	private void mergeFreshPreorderRefundFieldsIntoPayload(
			long companyId,
			long orderId,
			boolean supplierMode,
			long supplierFilterId,
			Map<String, Object> tradeRefundPayload) {
		long sid = supplierMode && supplierFilterId > 0L ? supplierFilterId : 0L;
		AftersalesRefund latest =
				aftersalesRefundService.findSingleForConfirmCancel(
						companyId, orderId, sid, null, List.of("READY"));
		if (latest == null) {
			return;
		}
		if (latest.getRefundBn() != null && latest.getRefundBn() > 0L) {
			tradeRefundPayload.put("refund_bn", latest.getRefundBn());
		}
		if (StringUtils.hasText(latest.getRefundStatus())) {
			tradeRefundPayload.put("refund_status", latest.getRefundStatus());
		}
		if (latest.getAftersalesBn() != null) {
			tradeRefundPayload.put("aftersales_bn", latest.getAftersalesBn());
		}
	}

	private void scheduleTradeRefundDispatchAfterCommit(Map<String, Object> payload) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					tradeRefundDispatchPublisher.publish(payload);
				}
			});
		} else {
			tradeRefundDispatchPublisher.publish(payload);
		}
	}

	private void scheduleNormalOrderCancelDispatchAfterCommit(Map<String, Object> payload) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					normalOrderCancelDispatchPublisher.publish(payload);
				}
			});
		} else {
			normalOrderCancelDispatchPublisher.publish(payload);
		}
	}

	/**
	 * When the pre-cancel order status is not {@code PART_PAYMENT}, schedules a third-party trade update dispatch
	 * after {@code order_associations} writes and a re-read: {@link ThirdPartyTradeUpdateDispatchPublisher} runs on
	 * {@code afterCommit} so external systems only observe updates once the transaction has committed.
	 */
	private void maybeScheduleThirdPartyTradeUpdateAfterOrderAssociationCancel(
			long companyId, long orderId, String preCancelOrderStatus) {
		if ("PART_PAYMENT".equalsIgnoreCase(safe(preCancelOrderStatus))) {
			return;
		}
		OrderAssociations assocRow =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		Map<String, Object> payload = buildThirdPartyTradeUpdatePayload(companyId, orderId, assocRow);
		if (payload == null || payload.isEmpty()) {
			return;
		}
		scheduleThirdPartyTradeUpdateDispatchAfterCommit(payload);
	}

	private Map<String, Object> buildThirdPartyTradeUpdatePayload(
			long companyId, long orderId, OrderAssociations assocRow) {
		if (assocRow == null) {
			return null;
		}
		LinkedHashMap<String, Object> erp = new LinkedHashMap<>();
		erp.put("company_id", companyId);
		erp.put("order_id", String.valueOf(orderId));
		erp.put("user_id", assocRow.getUserId() == null ? 0L : assocRow.getUserId());
		String orderClass = safe(assocRow.getOrderClass());
		if (!StringUtils.hasText(orderClass)) {
			orderClass = "normal";
		}
		erp.put("order_class", orderClass);
		Optional<Map<String, Object>> tradeOpt = orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId);
		if (tradeOpt.isPresent()) {
			erp.put("trade_id", String.valueOf(tradeOpt.get().get("trade_id")));
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

	private void dadaCancelIfNeeded(
			long companyId,
			long orderId,
			String receiptType,
			CancelOrders cancelRow,
			boolean supplierMode,
			NormalOrders normalOrder,
			SupplierOrder supplierOrder) {
		if (!"dada".equalsIgnoreCase(receiptType)) {
			return;
		}
		NormalOrdersRelDada dadaRow =
				normalOrdersRelDadaMapper.selectOne(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.eq(NormalOrdersRelDada::getOrderId, orderId)
								.last("LIMIT 1"));
		if (dadaRow == null || dadaRow.getDadaStatus() == null || dadaRow.getDadaStatus() != 1) {
			return;
		}
		if (!"shop".equalsIgnoreCase(cancelRow.getCancelFrom())
				&& !"buyer".equalsIgnoreCase(cancelRow.getCancelFrom())
				&& !"chief".equalsIgnoreCase(cancelRow.getCancelFrom())) {
			return;
		}
		String reason = cancelRow.getCancelReason() == null ? "" : cancelRow.getCancelReason();
		dadaLocalDeliveryFormalCancelOrderPort.formalCancel(companyId, orderId, reason);
	}

	private CancelOrders insertCancelForNotPay(
			long companyId,
			long supplierId,
			boolean supplierMode,
			NormalOrders normalOrder,
			SupplierOrder supplierOrder,
			String reasonText,
			String cancelFromForRecord) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		CancelOrders c = new CancelOrders();
		if (supplierMode) {
			c.setOrderId(supplierOrder.getOrderId());
			c.setCompanyId(supplierOrder.getCompanyId());
			c.setSupplierId(supplierId);
			c.setShopId(supplierOrder.getShopId() == null ? 0L : supplierOrder.getShopId());
			c.setUserId(supplierOrder.getUserId());
			c.setDistributorId(supplierOrder.getDistributorId() == null ? 0L : supplierOrder.getDistributorId());
			c.setOrderType(safe(supplierOrder.getOrderType()));
			c.setTotalFee(parseMoneyLong(supplierOrder.getTotalFee()));
			c.setPoint(supplierOrder.getPoint() == null ? 0 : supplierOrder.getPoint());
			c.setPayType(safe(supplierOrder.getPayType()));
		} else {
			c.setOrderId(normalOrder.getOrderId());
			c.setCompanyId(normalOrder.getCompanyId());
			c.setSupplierId(normalOrder.getSupplierId() == null ? 0L : normalOrder.getSupplierId());
			c.setShopId(normalOrder.getShopId() == null ? 0L : normalOrder.getShopId());
			c.setUserId(normalOrder.getUserId());
			c.setDistributorId(normalOrder.getDistributorId() == null ? 0L : normalOrder.getDistributorId());
			c.setOrderType(safe(normalOrder.getOrderType()));
			c.setTotalFee(parseMoneyLong(normalOrder.getTotalFee()));
			c.setPoint(normalOrder.getPoint() == null ? 0 : normalOrder.getPoint());
			c.setPayType(safe(normalOrder.getPayType()));
		}
		c.setProgress(3);
		c.setCancelFrom(cancelFromForRecord);
		c.setCancelReason(reasonText);
		c.setRefundStatus("SUCCESS");
		c.setCreateTime(now);
		c.setUpdateTime(now);
		cancelOrdersMapper.insert(c);
		if (c.getCancelId() == null || c.getCancelId() <= 0) {
			throw new ResourceException("订单取消失败！");
		}
		return c;
	}

	private void closeNotPayOrder(
			long companyId, long orderId, boolean supplierMode, long supplierId, NormalOrders normalOrder) {
		long supplierScope = supplierMode ? supplierId : 0L;
		boolean mainUpdated =
				normalOrderStatusUpdateService.applyStatusUpdate(
						companyId, orderId, supplierScope, "CANCEL", "SUCCESS");
		if (mainUpdated) {
			normalOrderFullCancelItemStoreRestoreService.restoreOnCancelSuccess(
					companyId, orderId, supplierScope);
			NormalOrders orderForDiscount = normalOrder;
			if (orderForDiscount == null) {
				orderForDiscount =
						normalOrdersMapper.selectOne(
								new LambdaQueryWrapper<NormalOrders>()
										.eq(NormalOrders::getCompanyId, companyId)
										.eq(NormalOrders::getOrderId, orderId)
										.last("LIMIT 1"));
			}
			if (orderForDiscount != null) {
				normalOrderCancelDiscountRestoreService.restoreOnCancelSuccess(orderForDiscount);
			}
		}
	}

	private Map<String, Object> orderDataForPoints(
			boolean supplierMode, NormalOrders normalOrder, SupplierOrder supplierOrder, long orderId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", orderId);
		if (supplierMode) {
			m.put("company_id", supplierOrder.getCompanyId());
			m.put("user_id", supplierOrder.getUserId());
			m.put("point_use", supplierOrder.getPointUse() == null ? 0 : supplierOrder.getPointUse());
			m.put("pay_type", supplierOrder.getPayType());
		} else {
			m.put("company_id", normalOrder.getCompanyId());
			m.put("user_id", normalOrder.getUserId());
			m.put("point_use", normalOrder.getPointUse() == null ? 0 : normalOrder.getPointUse());
			m.put("pay_type", normalOrder.getPayType());
		}
		return m;
	}

	private CancelOrders insertOrUpdateCancelForPayed(
			long companyId,
			long userId,
			long supplierId,
			boolean supplierMode,
			boolean platformSelfSubCancel,
			boolean shopRemainingFullCancel,
			NormalOrders normalOrder,
			SupplierOrder supplierOrder,
			String reasonText,
			String cancelFromForRecord) {
		LambdaQueryWrapper<CancelOrders> cq = new LambdaQueryWrapper<>();
		cq.eq(CancelOrders::getCompanyId, companyId)
				.eq(CancelOrders::getOrderId, supplierMode ? supplierOrder.getOrderId() : normalOrder.getOrderId())
				.eq(CancelOrders::getUserId, userId);
		if (supplierMode) {
			cq.eq(CancelOrders::getSupplierId, supplierId);
		} else {
			cq.eq(CancelOrders::getSupplierId, 0L);
		}
		CancelOrders existing = cancelOrdersMapper.selectOne(cq.last("LIMIT 1"));
		Map<String, Object> setting = tradeCancelSettingRedisService.getCancelSetting(companyId);
		boolean repeat = Boolean.TRUE.equals(setting.get("repeat_cancel"));

		int now = (int) (System.currentTimeMillis() / 1000L);
		CancelOrders data = new CancelOrders();
		PlatformSelfSubCancelSupport.PlatformSelfCancelAmounts platformAmounts = null;
		if (shopRemainingFullCancel) {
			platformAmounts =
					platformSelfSubCancelSupport.computeRemainingItemsCancelAmounts(
							companyId, normalOrder.getOrderId() == null ? 0L : normalOrder.getOrderId());
		} else if (platformSelfSubCancel) {
			platformAmounts =
					platformSelfSubCancelSupport.computeCancelAmounts(
							companyId, normalOrder.getOrderId() == null ? 0L : normalOrder.getOrderId());
		}
		if (supplierMode) {
			data.setOrderId(supplierOrder.getOrderId());
			data.setCompanyId(supplierOrder.getCompanyId());
			data.setSupplierId(supplierId);
			data.setShopId(supplierOrder.getShopId() == null ? 0L : supplierOrder.getShopId());
			data.setUserId(supplierOrder.getUserId());
			data.setDistributorId(supplierOrder.getDistributorId() == null ? 0L : supplierOrder.getDistributorId());
			data.setOrderType(safe(supplierOrder.getOrderType()));
			data.setTotalFee(parseMoneyLong(supplierOrder.getTotalFee()));
			data.setPoint(supplierOrder.getPoint() == null ? 0 : supplierOrder.getPoint());
			data.setPayType(safe(supplierOrder.getPayType()));
		} else {
			data.setOrderId(normalOrder.getOrderId());
			data.setCompanyId(normalOrder.getCompanyId());
			data.setSupplierId(0L);
			data.setShopId(normalOrder.getShopId() == null ? 0L : normalOrder.getShopId());
			data.setUserId(normalOrder.getUserId());
			data.setDistributorId(normalOrder.getDistributorId() == null ? 0L : normalOrder.getDistributorId());
			data.setOrderType(safe(normalOrder.getOrderType()));
			if (platformAmounts != null) {
				data.setTotalFee((long) platformAmounts.totalFee());
				data.setPoint(platformAmounts.refundPoint());
			} else {
				data.setTotalFee(parseMoneyLong(normalOrder.getTotalFee()));
				data.setPoint(normalOrder.getPoint() == null ? 0 : normalOrder.getPoint());
			}
			data.setPayType(safe(normalOrder.getPayType()));
		}
		data.setProgress(0);
		data.setCancelFrom(cancelFromForRecord);
		data.setCancelReason(reasonText);
		data.setRefundStatus("WAIT_CHECK");
		data.setCreateTime(now);
		data.setUpdateTime(now);

		if (existing != null) {
			if (!repeat && !isRejectedCancelOrder(existing)) {
				throw new ResourceException("不能重复取消订单");
			}
			LambdaUpdateWrapper<CancelOrders> uw = new LambdaUpdateWrapper<>();
			uw.eq(CancelOrders::getCancelId, existing.getCancelId())
					.set(CancelOrders::getProgress, 0)
					.set(CancelOrders::getCancelReason, reasonText)
					.set(CancelOrders::getCancelFrom, normalizeCancelFrom(cancelFromForRecord))
					.set(CancelOrders::getRefundStatus, "WAIT_CHECK")
					.set(CancelOrders::getShopRejectReason, null)
					.set(CancelOrders::getUpdateTime, now);
			cancelOrdersMapper.update(null, uw);
			CancelOrders reloaded = cancelOrdersMapper.selectById(existing.getCancelId());
			if (reloaded == null) {
				throw new ResourceException("订单取消失败！");
			}
			return reloaded;
		}

		CancelOrders row = new CancelOrders();
		row.setOrderId(data.getOrderId());
		row.setCompanyId(data.getCompanyId());
		row.setUserId(data.getUserId());
		row.setDistributorId(data.getDistributorId());
		row.setOrderType(data.getOrderType());
		row.setShopId(data.getShopId());
		row.setSupplierId(supplierMode ? supplierId : 0L);
		row.setTotalFee(data.getTotalFee());
		row.setPoint(data.getPoint());
		row.setPayType(data.getPayType());
		row.setProgress(0);
		row.setCancelFrom(cancelFromForRecord);
		row.setCancelReason(reasonText);
		row.setRefundStatus("WAIT_CHECK");
		row.setCreateTime(now);
		row.setUpdateTime(now);
		cancelOrdersMapper.insert(row);
		if (row.getCancelId() == null || row.getCancelId() <= 0) {
			throw new ResourceException("订单取消失败！");
		}
		return row;
	}

	private void createPayedRefunds(
			long companyId,
			long userId,
			long orderId,
			boolean supplierMode,
			boolean platformSelfSubCancel,
			boolean shopRemainingFullCancel,
			NormalOrders normalOrder,
			SupplierOrder supplierOrder,
			CancelOrders cancelRow) {
		Optional<Map<String, Object>> tradeOpt = orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId);
		if (tradeOpt.isEmpty()) {
			throw new ResourceException("支付信息未找到！");
		}
		Map<String, Object> trade = tradeOpt.get();
		String tradeId = String.valueOf(trade.get("trade_id"));
		String tradePayType = safe(String.valueOf(trade.get("pay_type")));

		long shopId =
				supplierMode
						? (supplierOrder.getShopId() == null ? 0L : supplierOrder.getShopId())
						: (normalOrder.getShopId() == null ? 0L : normalOrder.getShopId());
		long distributorId =
				supplierMode
						? (supplierOrder.getDistributorId() == null ? 0L : supplierOrder.getDistributorId())
						: (normalOrder.getDistributorId() == null ? 0L : normalOrder.getDistributorId());

		int totalFee;
		int freightFee;
		int refundFee;
		int refundPoint;
		if (shopRemainingFullCancel) {
			PlatformSelfSubCancelSupport.PlatformSelfCancelAmounts amounts =
					platformSelfSubCancelSupport.computeRemainingItemsCancelAmounts(companyId, orderId);
			totalFee = amounts.totalFee();
			freightFee = amounts.freightFee();
			refundFee = amounts.refundFee();
			refundPoint = amounts.refundPoint();
		} else if (platformSelfSubCancel) {
			PlatformSelfSubCancelSupport.PlatformSelfCancelAmounts amounts =
					platformSelfSubCancelSupport.computeCancelAmounts(companyId, orderId);
			totalFee = amounts.totalFee();
			freightFee = amounts.freightFee();
			refundFee = amounts.refundFee();
			refundPoint = amounts.refundPoint();
		} else {
			totalFee =
					supplierMode
							? parseMoneyInt(supplierOrder.getTotalFee())
							: parseMoneyInt(normalOrder.getTotalFee());
			freightFee =
					supplierMode
							? (supplierOrder.getFreightFee() == null ? 0 : supplierOrder.getFreightFee())
							: (normalOrder.getFreightFee() == null ? 0 : normalOrder.getFreightFee());
			refundFee = Math.max(0, totalFee - freightFee);
			refundPoint =
					supplierMode
							? (supplierOrder.getPoint() == null ? 0 : supplierOrder.getPoint())
							: (normalOrder.getPoint() == null ? 0 : normalOrder.getPoint());
		}
		String payType =
				supplierMode ? safe(supplierOrder.getPayType()) : safe(normalOrder.getPayType());
		String refundChannel = "offline_pay".equalsIgnoreCase(tradePayType) ? "offline" : "original";

		long refundSupplierId =
				supplierMode
						? (supplierOrder.getSupplierId() == null ? 0L : supplierOrder.getSupplierId())
						: 0L;
		String freightType =
				supplierMode ? safe(supplierOrder.getFreightType()) : safe(normalOrder.getFreightType());
		Map<String, Object> p = baseRefundParams(companyId, userId, orderId, trade, tradeId, shopId, distributorId);
		p.put("supplier_id", refundSupplierId);
		p.put("refund_fee", refundFee);
		p.put("refund_point", refundPoint);
		p.put("return_freight", shopRemainingFullCancel || platformSelfSubCancel ? 0 : 1);
		p.put("freight", freightFee);
		p.put("freight_type", freightType);
		p.put("pay_type", payType);
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
						? String.valueOf(refundPoint)
						: String.valueOf((int) Math.round(refundFee * doubleVal(trade.get("cur_fee_rate")))));
		aftersalesRefundService.createRefund(p);
	}

	private static Map<String, Object> baseRefundParams(
			long companyId,
			long userId,
			long orderId,
			Map<String, Object> trade,
			String tradeId,
			long shopId,
			long distributorId) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("company_id", companyId);
		p.put("user_id", userId);
		p.put("order_id", orderId);
		p.put("trade_id", tradeId);
		p.put("shop_id", shopId);
		p.put("distributor_id", distributorId);
		p.put("merchant_id", longVal(trade.get("merchant_id")));
		return p;
	}

	private static Map<String, Object> cancelToMap(CancelOrders c) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (c.getCancelId() != null) {
			m.put("cancel_id", String.valueOf(c.getCancelId()));
		}
		m.put("order_id", c.getOrderId() == null ? "" : String.valueOf(c.getOrderId()));
		m.put("company_id", c.getCompanyId() == null ? "" : String.valueOf(c.getCompanyId()));
		m.put("shop_id", c.getShopId() == null ? 0L : c.getShopId());
		m.put("user_id", c.getUserId() == null ? "" : String.valueOf(c.getUserId()));
		m.put("distributor_id", c.getDistributorId() == null ? "" : String.valueOf(c.getDistributorId()));
		m.put("order_type", c.getOrderType());
		m.put("total_fee", c.getTotalFee() == null ? "" : String.valueOf(c.getTotalFee()));
		m.put("progress", c.getProgress());
		m.put("cancel_from", c.getCancelFrom());
		m.put("cancel_reason", c.getCancelReason());
		m.put("shop_reject_reason", c.getShopRejectReason());
		m.put("refund_status", c.getRefundStatus());
		m.put("create_time", c.getCreateTime());
		m.put("update_time", c.getUpdateTime());
		m.put("fee_type", c.getFeeType());
		m.put(
				"fee_rate",
				c.getFeeRate() == null ? 0 : Math.round(c.getFeeRate()));
		m.put("fee_symbol", c.getFeeSymbol());
		m.put("point", c.getPoint());
		m.put("pay_type", c.getPayType());
		m.put("supplier_id", c.getSupplierId() == null ? 0L : c.getSupplierId());
		return m;
	}

	private static String normalizeCancelFrom(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "shop";
		}
		return raw.trim();
	}

	/** 商家拒绝退款后的取消单可再次申请，无需开启 repeat_cancel。 */
	private static boolean isRejectedCancelOrder(CancelOrders existing) {
		if (existing == null) {
			return false;
		}
		if ("SHOP_CHECK_FAILS".equalsIgnoreCase(safe(existing.getRefundStatus()))) {
			return true;
		}
		return existing.getProgress() != null && existing.getProgress() == 4;
	}

	private void publishOrderCancelProcessLog(
			long companyId,
			long orderId,
			long userId,
			String operatorType,
			long operatorId,
			long supplierId,
			String cancelFromNorm,
			Map<String, Object> mergedRequestParams,
			boolean notPay) {
		Map<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderId);
		log.put("company_id", companyId);
		log.put("supplier_id", supplierId);
		log.put("is_show", Boolean.FALSE);
		String logOpType = safe(operatorType);
		long logOpId = operatorId;
		if ("buyer".equalsIgnoreCase(cancelFromNorm)) {
			logOpType = "user";
			logOpId = userId;
		} else if ("chief".equalsIgnoreCase(cancelFromNorm)) {
			logOpType = "chief";
			logOpId = longVal(mergedRequestParams.get("chief_id"));
		} else if ("system".equalsIgnoreCase(cancelFromNorm)) {
			logOpType = "system";
			logOpId = 0L;
		} else if ("shop".equalsIgnoreCase(cancelFromNorm)) {
			logOpType = safe(operatorType);
			logOpId = operatorId;
		}
		if (!notPay && "shop".equalsIgnoreCase(cancelFromNorm)) {
			log.put("remarks", "申请取消订单");
			log.put("detail", "订单号：" + orderId + "，后台管理员申请取消订单，需要进行退款操作");
		} else if (!notPay && "buyer".equalsIgnoreCase(cancelFromNorm)) {
			log.put("remarks", "订单取消");
			log.put("detail", "订单号：" + orderId + "，用户申请取消订单，需要进行退款操作");
		} else if (!notPay && "chief".equalsIgnoreCase(cancelFromNorm)) {
			log.put("remarks", "订单取消");
			log.put("detail", "订单号：" + orderId + "，团长取消订单");
		} else {
			log.put("remarks", "取消订单");
			log.put("detail", "订单号：" + orderId + "，来源：" + cancelFromNorm);
		}
		log.put("operator_type", logOpType);
		log.put("operator_id", logOpId);
		log.put("params", new LinkedHashMap<>(mergedRequestParams));
		orderProcessLogPublishPort.publish(log);
	}

	private static long parseMoneyLong(Object totalFee) {
		if (totalFee == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(totalFee).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseMoneyInt(Object totalFee) {
		return (int) Math.min(Integer.MAX_VALUE, parseMoneyLong(totalFee));
	}

	private void ensureNoPendingSupplierSubCancel(long companyId, long orderId) {
		long waitProcess =
				supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderId)
								.eq(SupplierOrder::getCancelStatus, "WAIT_PROCESS"));
		if (waitProcess > 0) {
			throw new ResourceException("存在待审核的子单取消，请先处理后再整单取消");
		}
		if (aftersalesRefundService.countReadySupplierSubCancelRefunds(companyId, orderId) > 0) {
			throw new ResourceException("存在待审核的子单取消，请先处理后再整单取消");
		}
		if (platformSelfSubCancelSupport.countReadyPlatformSelfSubCancelRefunds(companyId, orderId) > 0) {
			throw new ResourceException("存在待审核的子单取消，请先处理后再整单取消");
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
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

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
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
