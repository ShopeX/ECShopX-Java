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
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeRefundCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReAddOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.orders.service.orderlog.AbstractAdminApiConfirmCancelRejectRefundReadyOrderProcessLogEntities;
import cn.shopex.ecshopx.orders.service.orderlog.PointsmallApiConfirmCancelAgreeOrderProcessLogEntities;
import cn.shopex.ecshopx.orders.service.orderlog.PointsmallApiConfirmCancelRejectOrderProcessLogEntities;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AdminOrderPassRefundService {

	private final ApplicationContext applicationContext;
	private final AftersalesRefundService aftersalesRefundService;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final OrderProfitMapper orderProfitMapper;
	private final OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	private final OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final AdminOrderBrokerageCancelOnRefundPassService adminOrderBrokerageCancelOnRefundPassService;
	private final PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	private final DadaLocalDeliveryReAddOrderPort dadaLocalDeliveryReAddOrderPort;
	private final AdminOrderEmployeePurchaseRestoreOnRefundPassService adminOrderEmployeePurchaseRestoreOnRefundPassService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher;
	private final WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;
	private final ThirdPartyTradeRefundCancelSaasErpDispatchPublisher thirdPartyTradeRefundCancelSaasErpDispatchPublisher;
	private final NormalOrderFullCancelItemStoreRestoreService normalOrderFullCancelItemStoreRestoreService;
	private final NormalOrderStatusUpdateService normalOrderStatusUpdateService;
	private final PlatformSelfSubCancelSupport platformSelfSubCancelSupport;
	private final PartialDeliveryFulfillmentReconcileService partialDeliveryFulfillmentReconcileService;
	private final NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService;

	public AdminOrderPassRefundService(
			ApplicationContext applicationContext,
			AftersalesRefundService aftersalesRefundService,
			CancelOrdersMapper cancelOrdersMapper,
			NormalOrdersMapper normalOrdersMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			OrderProfitMapper orderProfitMapper,
			OrderSuccessTradeReadPort orderSuccessTradeReadPort,
			OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			AdminOrderBrokerageCancelOnRefundPassService adminOrderBrokerageCancelOnRefundPassService,
			PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService,
			DadaLocalDeliveryReAddOrderPort dadaLocalDeliveryReAddOrderPort,
			AdminOrderEmployeePurchaseRestoreOnRefundPassService adminOrderEmployeePurchaseRestoreOnRefundPassService,
			ApplicationEventPublisher applicationEventPublisher,
			JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher,
			WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher,
			ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher,
			ThirdPartyTradeRefundCancelSaasErpDispatchPublisher thirdPartyTradeRefundCancelSaasErpDispatchPublisher,
			NormalOrderFullCancelItemStoreRestoreService normalOrderFullCancelItemStoreRestoreService,
			NormalOrderStatusUpdateService normalOrderStatusUpdateService,
			PlatformSelfSubCancelSupport platformSelfSubCancelSupport,
			PartialDeliveryFulfillmentReconcileService partialDeliveryFulfillmentReconcileService,
			NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService) {
		this.applicationContext = applicationContext;
		this.aftersalesRefundService = aftersalesRefundService;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.orderProfitMapper = orderProfitMapper;
		this.orderSuccessTradeReadPort = orderSuccessTradeReadPort;
		this.orderInvoiceCancelOnOrderCancelService = orderInvoiceCancelOnOrderCancelService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.adminOrderBrokerageCancelOnRefundPassService = adminOrderBrokerageCancelOnRefundPassService;
		this.pointMemberCancelOrderReturnPointsService = pointMemberCancelOrderReturnPointsService;
		this.dadaLocalDeliveryReAddOrderPort = dadaLocalDeliveryReAddOrderPort;
		this.adminOrderEmployeePurchaseRestoreOnRefundPassService = adminOrderEmployeePurchaseRestoreOnRefundPassService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.jushuitanTradeCancelDispatchPublisher = jushuitanTradeCancelDispatchPublisher;
		this.wdtErpTradeCancelDispatchPublisher = wdtErpTradeCancelDispatchPublisher;
		this.thirdPartyTradeUpdateDispatchPublisher = thirdPartyTradeUpdateDispatchPublisher;
		this.thirdPartyTradeRefundCancelSaasErpDispatchPublisher = thirdPartyTradeRefundCancelSaasErpDispatchPublisher;
		this.normalOrderFullCancelItemStoreRestoreService = normalOrderFullCancelItemStoreRestoreService;
		this.normalOrderStatusUpdateService = normalOrderStatusUpdateService;
		this.platformSelfSubCancelSupport = platformSelfSubCancelSupport;
		this.partialDeliveryFulfillmentReconcileService = partialDeliveryFulfillmentReconcileService;
		this.normalOrderCancelDiscountRestoreService = normalOrderCancelDiscountRestoreService;
	}

	public static Map<String, Object> cancelOrderRowToResponseMap(CancelOrders row) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (row == null) {
			return m;
		}
		if (row.getCancelId() != null) {
			m.put("cancel_id", row.getCancelId());
		}
		if (row.getOrderId() != null) {
			m.put("order_id", String.valueOf(row.getOrderId()));
		}
		if (row.getCompanyId() != null) {
			m.put("company_id", row.getCompanyId());
		}
		if (row.getSupplierId() != null) {
			m.put("supplier_id", row.getSupplierId());
		}
		m.put("shop_id", row.getShopId() == null ? 0L : row.getShopId());
		m.put("user_id", row.getUserId());
		m.put("distributor_id", row.getDistributorId() == null ? 0L : row.getDistributorId());
		m.put("order_type", row.getOrderType());
		m.put("total_fee", row.getTotalFee());
		m.put("progress", row.getProgress());
		m.put("cancel_from", row.getCancelFrom());
		m.put("cancel_reason", row.getCancelReason());
		m.put("shop_reject_reason", row.getShopRejectReason());
		m.put("refund_status", row.getRefundStatus());
		m.put("create_time", row.getCreateTime());
		m.put("update_time", row.getUpdateTime());
		m.put("fee_type", row.getFeeType());
		m.put("fee_rate", row.getFeeRate());
		m.put("fee_symbol", row.getFeeSymbol());
		m.put("point", row.getPoint());
		m.put("pay_type", row.getPayType());
		return m;
	}

	/**
	 * @apiNote On the {@code PAYED} branch, builds the ERP trade-cancel payload with {@code action=pass_refund}
	 *           and publishes it through {@link JushuitanTradeCancelDispatchPublisher} and
	 *           {@link WdtErpTradeCancelDispatchPublisher}.
	 */
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> passRefund(
			Map<String, Object> refundFilter,
			AftersalesRefund refund,
			Map<String, Object> params) {
		if (refund == null) {
			throw new ResourceException("退款单不存在");
		}
		long companyId = longVal(params.get("company_id"));
		long orderId = longVal(params.get("order_id"));
		ensureRefundReadyForPass(refund);
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long refundBn = refund.getRefundBn();
		Long supplierFilter =
				refund.getSupplierId() != null && refund.getSupplierId() > 0L ? refund.getSupplierId() : null;
		Map<String, Object> refundUpd = new LinkedHashMap<>();
		refundUpd.put("refund_status", "AUDIT_SUCCESS");
		refundUpd.put("update_time", now);
		int ur =
				aftersalesRefundService.updateRefundByConfirmFilter(
						companyId, orderId, refundBn, supplierFilter, refundUpd);
		if (ur <= 0) {
			throw new ResourceException("退款单更新失败");
		}
		long refundSupplierId = refund.getSupplierId() == null ? 0L : refund.getSupplierId();
		LambdaUpdateWrapper<CancelOrders> cu = new LambdaUpdateWrapper<>();
		cu.eq(CancelOrders::getCompanyId, companyId).eq(CancelOrders::getOrderId, orderId);
		if (refundSupplierId > 0L) {
			cu.eq(CancelOrders::getSupplierId, refundSupplierId);
		}
		cu.set(CancelOrders::getProgress, 2)
				.set(CancelOrders::getRefundStatus, "AUDIT_SUCCESS")
				.set(CancelOrders::getUpdateTime, now);
		cancelOrdersMapper.update(null, cu);

		NormalOrders norm = loadNormalOrderLine(companyId, orderId);
		if (norm == null) {
			throw new ResourceException("订单不存在");
		}
		String orderStatus = safe(norm.getOrderStatus());
		if ("PAYED".equalsIgnoreCase(orderStatus)) {
			Map<String, Object> erpPayload = new LinkedHashMap<>();
			erpPayload.put("company_id", companyId);
			erpPayload.put("order_id", orderId);
			erpPayload.put("action", "pass_refund");
			erpPayload.put("distributor_id", norm.getDistributorId() == null ? 0L : norm.getDistributorId());
			CancelOrders cancelSnap = loadCancelRow(companyId, orderId, refundSupplierId);
			if (cancelSnap != null && cancelSnap.getCancelId() != null) {
				erpPayload.put("cancel_id", cancelSnap.getCancelId());
			}
			if (cancelSnap != null && cancelSnap.getCancelReason() != null) {
				erpPayload.put("cancel_reason", cancelSnap.getCancelReason());
			}
			jushuitanTradeCancelDispatchPublisher.publish(erpPayload);
			wdtErpTradeCancelDispatchPublisher.publish(erpPayload);
			applicationEventPublisher.publishEvent(
					new cn.shopex.ecshopx.orders.event.WdtErpTradeCancelSpringEvent(this, erpPayload));
		}

		applicationContext
				.getBean(AdminOrderPassRefundService.class)
				.updateMainOrderCancelSuccess(companyId, orderId, refundSupplierId, refund);

		orderInvoiceCancelOnOrderCancelService.updateInvoiceStatusCancel(companyId, orderId, "cancel");

		orderProcessLogPublishPort.publish(buildPassRefundOrderProcessLogMap(orderId, companyId, params));

		adminOrderBrokerageCancelOnRefundPassService.closeBrokerageForCanceledOrder(companyId, orderId);

		int nowP = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderProfit> pu = new LambdaUpdateWrapper<>();
		pu.eq(OrderProfit::getCompanyId, companyId).eq(OrderProfit::getOrderId, orderId);
		pu.set(OrderProfit::getOrderProfitStatus, 0L).set(OrderProfit::getUpdated, nowP);
		orderProfitMapper.update(null, pu);

		Map<String, Object> od = new LinkedHashMap<>();
		od.put("order_id", orderId);
		od.put("company_id", companyId);
		od.put("user_id", norm.getUserId());
		od.put("point_use", norm.getPointUse() == null ? 0 : norm.getPointUse());
		od.put("pay_type", norm.getPayType());
		pointMemberCancelOrderReturnPointsService.cancelOrderReturnBackPoints(od);

		String receiptType = safe(norm.getReceiptType());
		if ("dada".equalsIgnoreCase(receiptType)) {
			NormalOrdersRelDada dadaRow =
					normalOrdersRelDadaMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersRelDada>()
									.eq(NormalOrdersRelDada::getCompanyId, companyId)
									.eq(NormalOrdersRelDada::getOrderId, orderId)
									.last("LIMIT 1"));
			if (dadaRow != null) {
				LambdaUpdateWrapper<NormalOrdersRelDada> du = new LambdaUpdateWrapper<>();
				du.eq(NormalOrdersRelDada::getCompanyId, companyId)
						.eq(NormalOrdersRelDada::getOrderId, orderId)
						.set(NormalOrdersRelDada::getDadaStatus, 5);
				normalOrdersRelDadaMapper.update(null, du);
			}
		}

		adminOrderEmployeePurchaseRestoreOnRefundPassService.restoreIfEmployeePurchaseOrder(companyId, orderId);

		scheduleSaasErpOrderSyncAfterCommit(companyId, orderId);
		scheduleThirdPartyTradeUpdateDispatchAfterCommit(companyId, orderId, refundSupplierId);

		CancelOrders outRow = loadCancelRow(companyId, orderId, refundSupplierId);
		if (outRow == null) {
			throw new ResourceException("取消单数据异常");
		}
		Map<String, Object> result = cancelOrderRowToResponseMap(outRow);
		result.put("action", "pass_refund");
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> rejectCancelAuditAfterRefundReady(
			Map<String, Object> refundFilter,
			AftersalesRefund refund,
			Map<String, Object> params) {
		if (refund == null) {
			throw new ResourceException("退款单不存在");
		}
		String rs = safe(refund.getRefundStatus());
		if (!"READY".equalsIgnoreCase(rs)) {
			throw new ResourceException("退款单非待审核状态，不可拒审");
		}
		long companyId = longVal(params.get("company_id"));
		long orderId = longVal(params.get("order_id"));
		String shopRejectReason = str(params.get("shop_reject_reason"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long refundBn = refund.getRefundBn();
		Long supplierFilter =
				refund.getSupplierId() != null && refund.getSupplierId() > 0L ? refund.getSupplierId() : null;
		Map<String, Object> refuseUpd = new LinkedHashMap<>();
		refuseUpd.put("refund_status", "REFUSE");
		refuseUpd.put("update_time", now);
		int ur =
				aftersalesRefundService.updateRefundByConfirmFilter(
						companyId, orderId, refundBn, supplierFilter, refuseUpd);
		if (ur <= 0) {
			throw new ResourceException("退款单更新失败");
		}
		long refundSupplierId = refund.getSupplierId() == null ? 0L : refund.getSupplierId();
		LambdaUpdateWrapper<CancelOrders> cu = new LambdaUpdateWrapper<>();
		cu.eq(CancelOrders::getCompanyId, companyId).eq(CancelOrders::getOrderId, orderId);
		if (refundSupplierId > 0L) {
			cu.eq(CancelOrders::getSupplierId, refundSupplierId);
		}
		cu.set(CancelOrders::getProgress, 4)
				.set(CancelOrders::getRefundStatus, "SHOP_CHECK_FAILS")
				.set(CancelOrders::getShopRejectReason, shopRejectReason)
				.set(CancelOrders::getUpdateTime, now);
		cancelOrdersMapper.update(null, cu);

		applicationContext
				.getBean(AdminOrderPassRefundService.class)
				.updateMainOrderCancelFails(companyId, orderId, refundSupplierId);

		orderProcessLogPublishPort.publish(
				AbstractAdminApiConfirmCancelRejectRefundReadyOrderProcessLogEntities.buildForRejectRefundReady(
						orderId,
						companyId,
						longVal(params.get("operator_id")),
						str(params.get("operator_type")),
						shopRejectReason,
						params));

		NormalOrders norm = loadNormalOrderLine(companyId, orderId);
		if (norm != null && "dada".equalsIgnoreCase(safe(norm.getReceiptType()))) {
			NormalOrdersRelDada dadaRow =
					normalOrdersRelDadaMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersRelDada>()
									.eq(NormalOrdersRelDada::getCompanyId, companyId)
									.eq(NormalOrdersRelDada::getOrderId, orderId)
									.last("LIMIT 1"));
			if (dadaRow != null && dadaRow.getDadaStatus() != null && dadaRow.getDadaStatus() == 5) {
				dadaLocalDeliveryReAddOrderPort.reAddOrder(companyId, orderId);
				LambdaUpdateWrapper<NormalOrdersRelDada> du = new LambdaUpdateWrapper<>();
				du.eq(NormalOrdersRelDada::getCompanyId, companyId)
						.eq(NormalOrdersRelDada::getOrderId, orderId)
						.set(NormalOrdersRelDada::getDadaStatus, 1);
				normalOrdersRelDadaMapper.update(null, du);
			}
		}

		CancelOrders outRow = loadCancelRow(companyId, orderId, refundSupplierId);
		if (outRow == null) {
			throw new ResourceException("取消单数据异常");
		}
		Map<String, Object> eventPayload = cancelOrderRowToResponseMap(outRow);
		registerRejectRefundPostCommitEvents(refund);
		return eventPayload;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public Map<String, Object> transactionalPointsmallOne(
			AftersalesRefund refund,
			String checkCancelNormalized,
			String shopRejectReason,
			Map<String, Object> params) {
		if (refund == null) {
			throw new ResourceException("退款单不存在");
		}
		long companyId = longVal(params.get("company_id"));
		long orderId = longVal(params.get("order_id"));
		long refundSupplierId = refund.getSupplierId() == null ? 0L : refund.getSupplierId();
		int now = (int) (System.currentTimeMillis() / 1000L);
		Long refundBn = refund.getRefundBn();
		Long supplierFilter =
				refund.getSupplierId() != null && refund.getSupplierId() > 0L ? refund.getSupplierId() : null;
		if ("1".equals(checkCancelNormalized)) {
			ensureRefundReadyForPass(refund);
			Map<String, Object> refundUpd = new LinkedHashMap<>();
			refundUpd.put("refund_status", "AUDIT_SUCCESS");
			refundUpd.put("update_time", now);
			int ur =
					aftersalesRefundService.updateRefundByConfirmFilter(
							companyId, orderId, refundBn, supplierFilter, refundUpd);
			if (ur <= 0) {
				throw new ResourceException("退款单更新失败");
			}
			LambdaUpdateWrapper<CancelOrders> cu = new LambdaUpdateWrapper<>();
			cu.eq(CancelOrders::getCompanyId, companyId).eq(CancelOrders::getOrderId, orderId);
			if (refundSupplierId > 0L) {
				cu.eq(CancelOrders::getSupplierId, refundSupplierId);
			}
			cu.set(CancelOrders::getProgress, 2)
					.set(CancelOrders::getRefundStatus, "AUDIT_SUCCESS")
					.set(CancelOrders::getUpdateTime, now);
			cancelOrdersMapper.update(null, cu);
			applicationContext
					.getBean(AdminOrderPassRefundService.class)
					.updateMainOrderCancelSuccess(companyId, orderId, refundSupplierId);
			Map<String, Object> log =
					PointsmallApiConfirmCancelAgreeOrderProcessLogEntities.buildForAdminAgreeRefund(
							orderId,
							companyId,
							longVal(params.get("operator_id")),
							str(params.get("operator_type")),
							params);
			orderProcessLogPublishPort.publish(log);
		} else {
			String rs = safe(refund.getRefundStatus());
			if (!"READY".equalsIgnoreCase(rs)) {
				throw new ResourceException("退款单非待审核状态，不可拒审");
			}
			Map<String, Object> refuseUpd = new LinkedHashMap<>();
			refuseUpd.put("refund_status", "REFUSE");
			refuseUpd.put("update_time", now);
			int ur =
					aftersalesRefundService.updateRefundByConfirmFilter(
							companyId, orderId, refundBn, supplierFilter, refuseUpd);
			if (ur <= 0) {
				throw new ResourceException("退款单更新失败");
			}
			LambdaUpdateWrapper<CancelOrders> cu = new LambdaUpdateWrapper<>();
			cu.eq(CancelOrders::getCompanyId, companyId).eq(CancelOrders::getOrderId, orderId);
			if (refundSupplierId > 0L) {
				cu.eq(CancelOrders::getSupplierId, refundSupplierId);
			}
			cu.set(CancelOrders::getProgress, 4)
					.set(CancelOrders::getRefundStatus, "SHOP_CHECK_FAILS")
					.set(CancelOrders::getShopRejectReason, shopRejectReason)
					.set(CancelOrders::getUpdateTime, now);
			cancelOrdersMapper.update(null, cu);
			applicationContext
					.getBean(AdminOrderPassRefundService.class)
					.updateMainOrderCancelFails(companyId, orderId, refundSupplierId);
			Map<String, Object> log =
					PointsmallApiConfirmCancelRejectOrderProcessLogEntities.buildForAdminRejectRefund(
							orderId,
							companyId,
							longVal(params.get("operator_id")),
							str(params.get("operator_type")),
							shopRejectReason,
							params);
			orderProcessLogPublishPort.publish(log);
		}
		CancelOrders outRow = loadCancelRow(companyId, orderId, refundSupplierId);
		if (outRow == null) {
			throw new ResourceException("取消单数据异常");
		}
		return cancelOrderRowToResponseMap(outRow);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateMainOrderCancelSuccess(long companyId, long orderId, long supplierScope) {
		updateMainOrderCancelSuccess(companyId, orderId, supplierScope, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateMainOrderCancelSuccess(
			long companyId, long orderId, long supplierScope, AftersalesRefund refund) {
		if (supplierScope == 0L
				&& refund != null
				&& platformSelfSubCancelSupport.isShopRemainingFullCancelRefund(companyId, orderId, refund)) {
			normalOrderFullCancelItemStoreRestoreService.restoreRemainingUnshippedItemsOnCancelSuccess(
					companyId, orderId);
			platformSelfSubCancelSupport.markRemainingUnshippedItemsCancelled(companyId, orderId);
			platformSelfSubCancelSupport.finalizeSupplierOrdersAfterRemainingCancelPass(companyId, orderId);
			reconcileMainOrderCancelStatus(companyId, orderId);
			partialDeliveryFulfillmentReconcileService.reconcileIfFullyProcessed(companyId, orderId);
			restoreDiscountIfMainCancelSuccess(companyId, orderId);
			return;
		}
		if (supplierScope == 0L
				&& refund != null
				&& platformSelfSubCancelSupport.isPlatformSelfSubCancelRefund(companyId, orderId, refund)) {
			platformSelfSubCancelSupport.markPlatformSelfItemsCancelled(companyId, orderId);
			normalOrderFullCancelItemStoreRestoreService.restorePlatformSelfItemsOnCancelSuccess(
					companyId, orderId);
			reconcileMainOrderCancelStatus(companyId, orderId);
			partialDeliveryFulfillmentReconcileService.reconcileIfFullyProcessed(companyId, orderId);
			restoreDiscountIfMainCancelSuccess(companyId, orderId);
			return;
		}
		boolean mainUpdated =
				normalOrderStatusUpdateService.applyStatusUpdate(
						companyId, orderId, supplierScope, "CANCEL", "SUCCESS");
		if (mainUpdated) {
			normalOrderFullCancelItemStoreRestoreService.restoreOnCancelSuccess(
					companyId, orderId, supplierScope);
			NormalOrders orderForDiscount =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (orderForDiscount != null) {
				normalOrderCancelDiscountRestoreService.restoreOnCancelSuccess(orderForDiscount);
			}
		}
		if (supplierScope > 0L) {
			platformSelfSubCancelSupport.markSupplierItemsCancelled(companyId, orderId, supplierScope);
		}
		reconcileMainOrderCancelStatus(companyId, orderId);
		partialDeliveryFulfillmentReconcileService.reconcileIfFullyProcessed(companyId, orderId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateMainOrderCancelFails(long companyId, long orderId, long supplierScope) {
		if (supplierScope > 0L) {
			LambdaUpdateWrapper<SupplierOrder> su = new LambdaUpdateWrapper<>();
			su.eq(SupplierOrder::getCompanyId, companyId)
					.eq(SupplierOrder::getOrderId, orderId)
					.eq(SupplierOrder::getSupplierId, (int) supplierScope)
					.set(SupplierOrder::getCancelStatus, "FAILS")
					.set(SupplierOrder::getUpdateTime, (int) (System.currentTimeMillis() / 1000L));
			supplierOrderMapper.update(null, su);
		} else {
			normalOrderStatusUpdateService.applyStatusUpdate(
					companyId, orderId, 0L, null, "FAILS");
		}
		reconcileMainOrderCancelStatus(companyId, orderId);
	}

	private void reconcileMainOrderCancelStatus(long companyId, long orderId) {
		boolean pendingSub =
				supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderId)
								.eq(SupplierOrder::getCancelStatus, "WAIT_PROCESS"))
						> 0;
		if (pendingSub
				|| aftersalesRefundService.countReadySupplierSubCancelRefunds(companyId, orderId) > 0
				|| platformSelfSubCancelSupport.countReadyPlatformSelfSubCancelRefunds(companyId, orderId) > 0) {
			return;
		}

		NormalOrders main = loadNormalOrderLine(companyId, orderId);
		if (main == null) {
			return;
		}
		String orderStatus = safe(main.getOrderStatus());
		String cancelStatus = safe(main.getCancelStatus());

		if (platformSelfSubCancelSupport.hasApprovedFullOrderCancelRefund(companyId, orderId)) {
			if (!("CANCEL".equalsIgnoreCase(orderStatus) && "SUCCESS".equalsIgnoreCase(cancelStatus))) {
				normalOrderStatusUpdateService.applyStatusUpdate(
						companyId, orderId, 0L, "CANCEL", "SUCCESS");
			}
			return;
		}

		if (normalOrderStatusUpdateService.hasRemainingActiveShipmentScope(companyId, orderId)) {
			if ("PAYED".equalsIgnoreCase(orderStatus) && "NO_APPLY_CANCEL".equalsIgnoreCase(cancelStatus)) {
				return;
			}
			normalOrderStatusUpdateService.revertMainOrderToPayedNoApplyCancel(companyId, orderId);
			return;
		}

		if ("CANCEL".equalsIgnoreCase(orderStatus) && "SUCCESS".equalsIgnoreCase(cancelStatus)) {
			return;
		}
		normalOrderStatusUpdateService.applyStatusUpdate(companyId, orderId, 0L, "CANCEL", "SUCCESS");
	}

	private Map<String, Object> buildThirdPartyTradeUpdatePayloadForPassRefund(
			long companyId, long orderId, @SuppressWarnings("unused") long refundSupplierId) {
		OrderAssociations assocRow =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
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
		if (tradeOpt.isPresent() && tradeOpt.get().get("trade_id") != null) {
			erp.put("trade_id", String.valueOf(tradeOpt.get().get("trade_id")));
		}
		erp.put("order_status", "CANCEL");
		return erp;
	}

	private void scheduleThirdPartyTradeUpdateDispatchAfterCommit(
			long companyId, long orderId, long refundSupplierId) {
		Map<String, Object> payload =
				buildThirdPartyTradeUpdatePayloadForPassRefund(companyId, orderId, refundSupplierId);
		if (payload == null || payload.isEmpty()) {
			return;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							thirdPartyTradeUpdateDispatchPublisher.publish(payload);
						}
					});
		} else {
			thirdPartyTradeUpdateDispatchPublisher.publish(payload);
		}
	}

	private void scheduleSaasErpOrderSyncAfterCommit(long companyId, long orderId) {
		final Object source = this;
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						Map<String, Object> erpPayload = new LinkedHashMap<>();
						erpPayload.put("company_id", companyId);
						erpPayload.put("order_id", orderId);
						orderSuccessTradeReadPort
								.primarySuccessTrade(companyId, orderId)
								.ifPresent(t -> erpPayload.put("trade_id", String.valueOf(t.get("trade_id"))));
						applicationEventPublisher.publishEvent(
								new cn.shopex.ecshopx.orders.event.SaasErpOrderSyncSpringEvent(source, erpPayload));
					}
				});
	}

	private void registerRejectRefundPostCommitEvents(AftersalesRefund refund) {
		final Map<String, Object> refundCancelPayload = buildTradeRefundCancelSaasErpPayload(refund);
		final Map<String, Object> wdtPayload = buildRejectPathWdtPayload(refund);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						thirdPartyTradeRefundCancelSaasErpDispatchPublisher.publish(refundCancelPayload);
						wdtErpTradeCancelDispatchPublisher.publish(wdtPayload);
					}
				});
	}

	private static Map<String, Object> buildTradeRefundCancelSaasErpPayload(AftersalesRefund refund) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (refund == null) {
			return m;
		}
		putIfNonNull(m, "refund_bn", refund.getRefundBn());
		putIfNonNull(m, "aftersales_bn", refund.getAftersalesBn());
		putIfNonNull(m, "order_id", refund.getOrderId());
		putIfHasText(m, "trade_id", refund.getTradeId());
		putIfNonNull(m, "company_id", refund.getCompanyId());
		putIfNonNull(m, "supplier_id", refund.getSupplierId());
		putIfNonNull(m, "user_id", refund.getUserId());
		putIfNonNull(m, "shop_id", refund.getShopId());
		putIfNonNull(m, "distributor_id", refund.getDistributorId());
		putIfHasText(m, "refund_type", refund.getRefundType());
		putIfHasText(m, "refund_channel", refund.getRefundChannel());
		putIfNonNull(m, "refund_fee", refund.getRefundFee());
		putIfNonNull(m, "refunded_fee", refund.getRefundedFee());
		putIfNonNull(m, "refund_point", refund.getRefundPoint());
		putIfNonNull(m, "refunded_point", refund.getRefundedPoint());
		putIfNonNull(m, "return_point", refund.getReturnPoint());
		putIfNonNull(m, "return_freight", refund.getReturnFreight());
		putIfHasText(m, "pay_type", refund.getPayType());
		putIfHasText(m, "currency", refund.getCurrency());
		putIfHasText(m, "refunds_memo", refund.getRefundsMemo());
		putIfNonNull(m, "refund_success_time", refund.getRefundSuccessTime());
		putIfHasText(m, "refund_id", refund.getRefundId());
		putIfNonNull(m, "create_time", refund.getCreateTime());
		putIfNonNull(m, "update_time", refund.getUpdateTime());
		putIfHasText(m, "cur_fee_type", refund.getCurFeeType());
		putIfNonNull(m, "cur_fee_rate", refund.getCurFeeRate());
		putIfHasText(m, "cur_fee_symbol", refund.getCurFeeSymbol());
		putIfHasText(m, "cur_pay_fee", refund.getCurPayFee());
		putIfHasText(m, "hf_order_id", refund.getHfOrderId());
		putIfNonNull(m, "merchant_id", refund.getMerchantId());
		putIfNonNull(m, "freight", refund.getFreight());
		putIfHasText(m, "freight_type", refund.getFreightType());
		m.put("refund_status", "REFUSE");
		return m;
	}

	private static void putIfNonNull(LinkedHashMap<String, Object> m, String key, Object value) {
		if (value != null) {
			m.put(key, value);
		}
	}

	private static void putIfHasText(LinkedHashMap<String, Object> m, String key, String value) {
		if (StringUtils.hasText(value)) {
			m.put(key, value);
		}
	}

	private static Map<String, Object> buildRejectPathWdtPayload(AftersalesRefund refund) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", refund.getCompanyId());
		m.put("order_id", refund.getOrderId());
		m.put("distributor_id", refund.getDistributorId() == null ? 0L : refund.getDistributorId());
		return m;
	}

	/**
	 * Order-process-log entities for the agree-refund path (cancel audit success), aligned with the legacy
	 * admin cancel-confirm payload shape for downstream persistence.
	 */
	private static Map<String, Object> buildPassRefundOrderProcessLogMap(
			long orderId, long companyId, Map<String, Object> params) {
		Map<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderId);
		log.put("company_id", companyId);
		String operatorType = str(params.get("operator_type"));
		if (operatorType.isEmpty()) {
			operatorType = "system";
		}
		log.put("operator_type", operatorType);
		long operatorId = longVal(params.get("operator_id"));
		log.put("operator_id", operatorId);
		String remarks = params.get("remarks") == null ? "订单退款" : str(params.get("remarks"));
		log.put("remarks", remarks);
		log.put("params", params);
		String detail =
				operatorId == 0L
						? "订单号：" + orderId + "，系统自动同意退款"
						: "订单号：" + orderId + "，后台管理员同意退款";
		log.put("detail", detail);
		log.put("is_show", Boolean.FALSE);
		return log;
	}

	private static void ensureRefundReadyForPass(AftersalesRefund refund) {
		String rs = safe(refund.getRefundStatus());
		if ("SUCCESS".equalsIgnoreCase(rs)) {
			throw new ResourceException("该退款单已退款成功");
		}
		if ("REFUSE".equalsIgnoreCase(rs)) {
			throw new ResourceException("该退款单已拒绝");
		}
		if ("AUDIT_SUCCESS".equalsIgnoreCase(rs)) {
			throw new ResourceException("该退款单已审核通过");
		}
		if ("CANCEL".equalsIgnoreCase(rs)) {
			throw new ResourceException("该退款单已撤销");
		}
		if ("PROCESSING".equalsIgnoreCase(rs)) {
			throw new ResourceException("该退款单处理中");
		}
		if ("CHANGE".equalsIgnoreCase(rs)) {
			throw new ResourceException("该退款单状态异常");
		}
		if (!"READY".equalsIgnoreCase(rs)) {
			throw new ResourceException("退款单状态不允许审核通过");
		}
	}

	private CancelOrders loadCancelRow(long companyId, long orderId, long refundSupplierId) {
		LambdaQueryWrapper<CancelOrders> q =
				new LambdaQueryWrapper<CancelOrders>()
						.eq(CancelOrders::getCompanyId, companyId)
						.eq(CancelOrders::getOrderId, orderId);
		if (refundSupplierId > 0L) {
			q.eq(CancelOrders::getSupplierId, refundSupplierId);
		}
		return cancelOrdersMapper.selectOne(q.last("LIMIT 1"));
	}

	private NormalOrders loadNormalOrderLine(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private void restoreDiscountIfMainCancelSuccess(long companyId, long orderId) {
		NormalOrders orderForDiscount = loadNormalOrderLine(companyId, orderId);
		if (orderForDiscount == null) {
			return;
		}
		if (!"CANCEL".equalsIgnoreCase(safe(orderForDiscount.getOrderStatus()))) {
			return;
		}
		if (!"SUCCESS".equalsIgnoreCase(safe(orderForDiscount.getCancelStatus()))) {
			return;
		}
		normalOrderCancelDiscountRestoreService.restoreOnCancelSuccess(orderForDiscount);
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
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
}
