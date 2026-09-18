package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryFormalCancelOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderFullCancelItemStoreRestoreService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderStatusUpdateService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName(
		"EVENT_ORDER_PROCESS_LOG: drug reject PAYED+PENDING — publishEvent probe (shop OPL params, processDrugOrders)")
class AdminProcessDrugOrdersServiceDrugRejectShopPayedPendingOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CancelOrders.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void drugReject_pendingPayedPath_afterCommit_invokesPublishEventWithShopOplPayload() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		AdminNormalOrderFullCancelService fullCancel = buildServiceForPayedShopCancel(dispatchFacade);

		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		AdminNormalOrderPartialCancelService partialCancel = mock(AdminNormalOrderPartialCancelService.class);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(200L);
		assoc.setOrderType("normal_drug");
		assoc.setDeliveryStatus("PENDING");
		assoc.setUserId(100L);
		assoc.setMobile("13800000000");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		AdminProcessDrugOrdersService drugSvc =
				new AdminProcessDrugOrdersService(
						orderAssociationsMapper,
						normalOrdersMapper,
						supplierOrderMapper,
						fullCancel,
						partialCancel);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("status", "false");
		merged.put("reject_reason", "买/卖双方协商一致");

		LinkedHashMap<String, Object> expectedParams = new LinkedHashMap<>();
		expectedParams.put("status", "false");
		expectedParams.put("reject_reason", "买/卖双方协商一致");
		expectedParams.put("cancel_reason", "买/卖双方协商一致");
		expectedParams.put("cancel_from", "shop");
		expectedParams.put("company_id", 1L);
		expectedParams.put("user_id", 100L);
		expectedParams.put("order_id", "200");
		expectedParams.put("mobile", "13800000000");

		runDrugRejectInTx(drugSvc, merged);

		ArgumentCaptor<Map<String, Object>> payloadCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertThat(payload.get("order_id")).isEqualTo(200L);
		assertThat(payload.get("company_id")).isEqualTo(1L);
		assertThat(payload.get("supplier_id")).isEqualTo(0L);
		assertThat(payload.get("remarks")).isEqualTo("申请取消订单");
		assertThat(payload.get("detail"))
				.isEqualTo("订单号：200，后台管理员申请取消订单，需要进行退款操作");
		assertThat(payload.get("operator_type")).isEqualTo("system");
		assertThat(payload.get("operator_id")).isEqualTo(42L);
		assertThat(payload.get("is_show")).isEqualTo(Boolean.FALSE);
		assertThat(payload.get("params")).isEqualTo(expectedParams);
	}

	private static void runDrugRejectInTx(AdminProcessDrugOrdersService drugSvc, Map<String, Object> merged) {
		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		tx.executeWithoutResult(st -> drugSvc.processDrugOrders(1L, "system", 42L, "200", merged));
	}

	private static AdminNormalOrderFullCancelService buildServiceForPayedShopCancel(DispatchFacade dispatchFacade) {
		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		CancelOrdersMapper cancelOrdersMapper = mock(CancelOrdersMapper.class);
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		NormalOrdersRelDadaMapper normalOrdersRelDadaMapper = mock(NormalOrdersRelDadaMapper.class);
		TradeCancelSettingRedisService tradeCancelSettingRedisService = mock(TradeCancelSettingRedisService.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService =
				mock(PointMemberCancelOrderReturnPointsService.class);
		OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService =
				mock(OrderInvoiceCancelOnOrderCancelService.class);
		DadaLocalDeliveryFormalCancelOrderPort dadaLocalDeliveryFormalCancelOrderPort =
				mock(DadaLocalDeliveryFormalCancelOrderPort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService =
				mock(PointMemberMinusOrderUppointsService.class);
		JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher =
				mock(JushuitanTradeCancelDispatchPublisher.class);
		WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher =
				mock(WdtErpTradeCancelDispatchPublisher.class);
		NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher =
				mock(NormalOrderCancelDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);

		NormalOrders order = new NormalOrders();
		order.setOrderId(200L);
		order.setCompanyId(1L);
		order.setUserId(100L);
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setOrderStatus("PAYED");
		order.setDeliveryStatus("PENDING");
		order.setReceiptType("store");
		order.setType(0);
		order.setShopId(1L);
		order.setDistributorId(0L);
		order.setSupplierId(0);
		order.setTotalFee("10000");
		order.setPoint(0);
		order.setPayType("wxpay");
		order.setFreightFee(0);
		order.setFreightType("normal");
		order.setUppointUse(0);

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(null);
		when(supplierOrderMapper.selectList(any())).thenReturn(List.of());
		when(cancelOrdersMapper.selectOne(any())).thenReturn(null);
		when(tradeCancelSettingRedisService.getCancelSetting(1L))
				.thenReturn(new LinkedHashMap<>(Map.of("repeat_cancel", false)));

		LinkedHashMap<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "T1");
		trade.put("pay_type", "wxpay");
		trade.put("merchant_id", 1L);
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0);
		trade.put("cur_fee_symbol", "¥");
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, 200L)).thenReturn(Optional.of(trade));

		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(77L);
							return 1;
						});
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		return new AdminNormalOrderFullCancelService(
				normalOrdersMapper,
				supplierOrderMapper,
				cancelOrdersMapper,
				orderAssociationsMapper,
				normalOrdersRelDadaMapper,
				tradeCancelSettingRedisService,
				orderSuccessTradeReadPort,
				aftersalesRefundService,
				pointMemberCancelOrderReturnPointsService,
				orderInvoiceCancelOnOrderCancelService,
				dadaLocalDeliveryFormalCancelOrderPort,
				applicationEventPublisher,
				orderProcessLogPublishPort,
				pointMemberMinusOrderUppointsService,
				jushuitanTradeCancelDispatchPublisher,
				wdtErpTradeCancelDispatchPublisher,
				normalOrderCancelDispatchPublisher,
				tradeRefundDispatchPublisher,
				mock(ThirdPartyTradeUpdateDispatchPublisher.class),
				mock(NormalOrderFullCancelItemStoreRestoreService.class),
				new NormalOrderStatusUpdateService(normalOrdersMapper, org.mockito.Mockito.mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class), supplierOrderMapper, orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));
	}
}
