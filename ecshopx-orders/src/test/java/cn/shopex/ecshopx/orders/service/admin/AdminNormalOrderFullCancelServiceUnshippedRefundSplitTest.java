package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
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
@DisplayName("Admin full cancel: unshipped multi-supplier refund split")
class AdminNormalOrderFullCancelServiceUnshippedRefundSplitTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CancelOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	@DisplayName("整单取消含多供应商：只 insert 一张 cancel 且 createRefund 一次 supplier_id=0")
	@SuppressWarnings("unchecked")
	void fullCancel_multiSupplier_createsSingleRefund() {
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
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService =
				mock(PointMemberMinusOrderUppointsService.class);
		JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher =
				mock(JushuitanTradeCancelDispatchPublisher.class);
		WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher =
				mock(WdtErpTradeCancelDispatchPublisher.class);
		NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher =
				mock(NormalOrderCancelDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);

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
		order.setDistributorId(10L);
		order.setSupplierId(0);
		order.setTotalFee("15000");
		order.setPoint(0);
		order.setPayType("wxpay");
		order.setFreightFee(500);
		order.setFreightType("normal");
		order.setOrderType("normal");

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(null);
		when(tradeCancelSettingRedisService.getCancelSetting(1L)).thenReturn(Map.of("repeat_cancel", false));
		when(cancelOrdersMapper.selectOne(any())).thenReturn(null);
		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(99L);
							return 1;
						});

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "tid-multi");
		trade.put("pay_type", "wxpay");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0f);
		trade.put("cur_fee_symbol", "￥");
		trade.put("pay_fee", 15000);
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, 200L)).thenReturn(Optional.of(trade));

		AftersalesRefund preorderRefund = new AftersalesRefund();
		preorderRefund.setRefundBn(9_999_999L);
		preorderRefund.setRefundStatus("READY");
		when(aftersalesRefundService.findSingleForConfirmCancel(
						eq(1L), eq(200L), eq(0L), isNull(), eq(List.of("READY"))))
				.thenReturn(preorderRefund);

		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							OrderAssociations a = new OrderAssociations();
							a.setCompanyId(1L);
							a.setOrderId(200L);
							a.setUserId(100L);
							a.setOrderClass("normal");
							a.setOrderStatus("PAYED");
							return a;
						});

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

		AdminNormalOrderFullCancelService svc =
				new AdminNormalOrderFullCancelService(
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
						thirdPartyTradeUpdateDispatchPublisher,
						mock(NormalOrderFullCancelItemStoreRestoreService.class),
						new NormalOrderStatusUpdateService(
								normalOrdersMapper,
								mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class),
								supplierOrderMapper,
								orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		tx.executeWithoutResult(
				st ->
						svc.execute(
								1L,
								"shop",
								0L,
								0L,
								100L,
								"13800000000",
								200L,
								"reason",
								Map.of(),
								"shop"));

		ArgumentCaptor<CancelOrders> cancelCaptor = ArgumentCaptor.forClass(CancelOrders.class);
		verify(cancelOrdersMapper, times(1)).insert(cancelCaptor.capture());
		CancelOrders insertedCancel = cancelCaptor.getValue();
		assertThat(insertedCancel.getSupplierId()).isEqualTo(0L);
		assertThat(insertedCancel.getTotalFee()).isEqualTo(15000L);

		ArgumentCaptor<Map<String, Object>> refundCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(aftersalesRefundService, times(1)).createRefund(refundCaptor.capture());
		Map<String, Object> refundParams = refundCaptor.getValue();
		assertThat(refundParams.get("supplier_id")).isEqualTo(0L);
		assertThat(refundParams.get("refund_fee")).isEqualTo(14500);
		assertThat(refundParams.get("freight")).isEqualTo(500);
		assertThat(refundParams.get("return_freight")).isEqualTo(1);

		verify(supplierOrderMapper, never()).selectList(any());
		verify(supplierOrderMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("店铺整单取消含供应商子单：supplier_order.cancel_status 进入 WAIT_PROCESS")
	@SuppressWarnings("unchecked")
	void fullCancel_withSupplierRows_updatesSupplierCancelStatusToWaitProcess() {
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
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService =
				mock(PointMemberMinusOrderUppointsService.class);
		JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher =
				mock(JushuitanTradeCancelDispatchPublisher.class);
		WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher =
				mock(WdtErpTradeCancelDispatchPublisher.class);
		NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher =
				mock(NormalOrderCancelDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);

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
		order.setDistributorId(10L);
		order.setSupplierId(0);
		order.setTotalFee("15000");
		order.setPoint(0);
		order.setPayType("wxpay");
		order.setFreightFee(500);
		order.setFreightType("normal");
		order.setOrderType("normal");

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(null);
		when(tradeCancelSettingRedisService.getCancelSetting(1L)).thenReturn(Map.of("repeat_cancel", false));
		when(cancelOrdersMapper.selectOne(any())).thenReturn(null);
		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(99L);
							return 1;
						});
		PlatformSelfSubCancelSupport platformSelfSubCancelSupport = mock(PlatformSelfSubCancelSupport.class);
		when(aftersalesRefundService.countReadySupplierSubCancelRefunds(1L, 200L)).thenReturn(0L);
		when(platformSelfSubCancelSupport.countReadyPlatformSelfSubCancelRefunds(1L, 200L)).thenReturn(0L);
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L, 2L);
		when(supplierOrderMapper.update(any(), any())).thenReturn(2);

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "tid-multi");
		trade.put("pay_type", "wxpay");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0f);
		trade.put("cur_fee_symbol", "￥");
		trade.put("pay_fee", 15000);
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, 200L)).thenReturn(Optional.of(trade));

		AftersalesRefund preorderRefund = new AftersalesRefund();
		preorderRefund.setRefundBn(9_999_999L);
		preorderRefund.setRefundStatus("READY");
		when(aftersalesRefundService.findSingleForConfirmCancel(
						eq(1L), eq(200L), eq(0L), isNull(), eq(List.of("READY"))))
				.thenReturn(preorderRefund);

		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							OrderAssociations a = new OrderAssociations();
							a.setCompanyId(1L);
							a.setOrderId(200L);
							a.setUserId(100L);
							a.setOrderClass("normal");
							a.setOrderStatus("PAYED");
							return a;
						});

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

		AdminNormalOrderFullCancelService svc =
				new AdminNormalOrderFullCancelService(
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
						thirdPartyTradeUpdateDispatchPublisher,
						mock(NormalOrderFullCancelItemStoreRestoreService.class),
						new NormalOrderStatusUpdateService(
								normalOrdersMapper,
								mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class),
								supplierOrderMapper,
								orderAssociationsMapper),
						platformSelfSubCancelSupport,
				mock(NormalOrderCancelDiscountRestoreService.class));

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		tx.executeWithoutResult(
				st ->
						svc.execute(
								1L,
								"shop",
								0L,
								0L,
								100L,
								"13800000000",
								200L,
								"reason",
								Map.of(),
								"shop"));

		verify(supplierOrderMapper, times(1)).update(any(), any());
	}

	@Test
	@DisplayName("供应商自取消：createRefund 一次且 supplier_id=自己")
	@SuppressWarnings("unchecked")
	void supplierSelfCancel_createsOnlyOwnRefund() {
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
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService =
				mock(PointMemberMinusOrderUppointsService.class);
		JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher =
				mock(JushuitanTradeCancelDispatchPublisher.class);
		WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher =
				mock(WdtErpTradeCancelDispatchPublisher.class);
		NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher =
				mock(NormalOrderCancelDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);

		long supplierId = 101L;
		SupplierOrder supplierOrder = new SupplierOrder();
		supplierOrder.setSupplierId(101);
		supplierOrder.setOrderId(200L);
		supplierOrder.setCompanyId(1L);
		supplierOrder.setUserId(100L);
		supplierOrder.setCancelStatus("NO_APPLY_CANCEL");
		supplierOrder.setOrderStatus("PAYED");
		supplierOrder.setDeliveryStatus("PENDING");
		supplierOrder.setReceiptType("store");
		supplierOrder.setShopId(1L);
		supplierOrder.setDistributorId(10L);
		supplierOrder.setTotalFee("8000");
		supplierOrder.setFreightFee(200);
		supplierOrder.setPoint(0);
		supplierOrder.setPayType("wxpay");
		supplierOrder.setFreightType("normal");
		supplierOrder.setOrderType("normal");

		when(supplierOrderMapper.selectOne(any())).thenReturn(supplierOrder);
		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(null);
		when(tradeCancelSettingRedisService.getCancelSetting(1L)).thenReturn(Map.of("repeat_cancel", false));
		when(cancelOrdersMapper.selectOne(any())).thenReturn(null);
		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(88L);
							return 1;
						});

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "tid-sup");
		trade.put("pay_type", "wxpay");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0f);
		trade.put("cur_fee_symbol", "￥");
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, 200L)).thenReturn(Optional.of(trade));

		AftersalesRefund preorderRefund = new AftersalesRefund();
		preorderRefund.setRefundBn(8_888_888L);
		preorderRefund.setRefundStatus("READY");
		preorderRefund.setSupplierId(supplierId);
		when(aftersalesRefundService.findSingleForConfirmCancel(
						eq(1L), eq(200L), eq(supplierId), isNull(), eq(List.of("READY"))))
				.thenReturn(preorderRefund);

		when(supplierOrderMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							OrderAssociations a = new OrderAssociations();
							a.setCompanyId(1L);
							a.setOrderId(200L);
							a.setUserId(100L);
							a.setOrderClass("normal");
							a.setOrderStatus("PAYED");
							return a;
						});

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

		AdminNormalOrderFullCancelService svc =
				new AdminNormalOrderFullCancelService(
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
						thirdPartyTradeUpdateDispatchPublisher,
						mock(NormalOrderFullCancelItemStoreRestoreService.class),
						new NormalOrderStatusUpdateService(
								normalOrdersMapper,
								mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class),
								supplierOrderMapper,
								orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		tx.executeWithoutResult(
				st ->
						svc.execute(
								1L,
								"supplier",
								supplierId,
								supplierId,
								100L,
								"13800000000",
								200L,
								"reason",
								Map.of(),
								"shop"));

		ArgumentCaptor<Map<String, Object>> refundCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(aftersalesRefundService, times(1)).createRefund(refundCaptor.capture());
		Map<String, Object> refundParams = refundCaptor.getValue();
		assertThat(refundParams.get("supplier_id")).isEqualTo(supplierId);
		assertThat(refundParams.get("refund_fee")).isEqualTo(7800);
		assertThat(refundParams.get("freight")).isEqualTo(200);

		verify(supplierOrderMapper, never()).selectList(any());
	}
}
