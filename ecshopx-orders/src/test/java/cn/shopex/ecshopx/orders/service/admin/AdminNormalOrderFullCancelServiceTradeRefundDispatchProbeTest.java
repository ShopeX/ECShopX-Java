package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
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
import cn.shopex.ecshopx.orders.service.admin.NormalOrderFullCancelItemStoreRestoreService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderStatusUpdateService;
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
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin full cancel: trade refund Bus publish after commit")
class AdminNormalOrderFullCancelServiceTradeRefundDispatchProbeTest {

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
	void notPay_shopCancel_afterCommit_invokesTradeRefundPublisherOnce() {
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
		order.setOrderStatus("NOTPAY");
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
		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(88L);
							return 1;
						});
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
							a.setOrderStatus("CANCEL");
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
				new NormalOrderStatusUpdateService(normalOrdersMapper, org.mockito.Mockito.mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class), supplierOrderMapper, orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		tx.executeWithoutResult(
				st ->
						svc.execute(
								1L, "shop", 0L, 0L, 100L, "13800000000", 200L, "", Map.of()));

		verify(jushuitanTradeCancelDispatchPublisher, never()).publish(anyMap());
		verify(wdtErpTradeCancelDispatchPublisher, never()).publish(anyMap());

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		InOrder publishOrder =
				inOrder(thirdPartyTradeUpdateDispatchPublisher, tradeRefundDispatchPublisher, normalOrderCancelDispatchPublisher);
		publishOrder.verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(anyMap());
		publishOrder.verify(tradeRefundDispatchPublisher, times(1)).publish(captor.capture());
		publishOrder.verify(normalOrderCancelDispatchPublisher, times(1)).publish(anyMap());
		Map<String, Object> published = captor.getValue();
		assertThat(published.get("cancel_from")).isEqualTo("shop");
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo("200");
		assertThat(String.valueOf(published.get("company_id"))).isEqualTo("1");
		assertThat(published).doesNotContainKey("action");
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName(
			"Admin shop full cancel PAYED+PENDING: after commit publishes trade refund once with cancel row keys plus"
					+ " preorder refund_bn / refund_status")
	void payed_shopCancel_afterCommit_payloadContainsCancelOrderAction() {
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
		order.setDistributorId(0L);
		order.setSupplierId(0);
		order.setTotalFee("10000");
		order.setPoint(0);
		order.setPayType("wxpay");
		order.setFreightFee(0);
		order.setFreightType("normal");

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(null);
		when(tradeCancelSettingRedisService.getCancelSetting(1L)).thenReturn(Map.of("repeat_cancel", false));
		when(cancelOrdersMapper.selectOne(any())).thenReturn(null);
		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(55L);
							return 1;
						});

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "tid-1");
		trade.put("pay_type", "wxpay");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0f);
		trade.put("cur_fee_symbol", "￥");
		trade.put("pay_fee", 10000);
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, 200L)).thenReturn(Optional.of(trade));

		AftersalesRefund preorderRefund = new AftersalesRefund();
		preorderRefund.setRefundBn(7_777_777L);
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
							a.setOrderClass("normal_groups");
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
				new NormalOrderStatusUpdateService(normalOrdersMapper, org.mockito.Mockito.mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class), supplierOrderMapper, orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		tx.executeWithoutResult(
				st ->
						svc.execute(1L, "shop", 0L, 0L, 100L, "13800000000", 200L, "reason", Map.of("other_reason", "x")));

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		InOrder afterCommitOrder =
				inOrder(thirdPartyTradeUpdateDispatchPublisher, tradeRefundDispatchPublisher, normalOrderCancelDispatchPublisher);
		afterCommitOrder.verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(anyMap());
		afterCommitOrder.verify(tradeRefundDispatchPublisher).publish(captor.capture());
		afterCommitOrder.verify(normalOrderCancelDispatchPublisher, times(1)).publish(anyMap());
		Map<String, Object> published = captor.getValue();
		assertThat(published.get("action")).isEqualTo("cancel_order");
		assertThat(published.get("cancel_from")).isEqualTo("shop");
		assertThat(published.get("cancel_id")).isNotNull();
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo("200");
		assertThat(String.valueOf(published.get("company_id"))).isEqualTo("1");
		assertThat(published.get("refund_bn")).isEqualTo(7_777_777L);
		assertThat(published.get("refund_status")).isEqualTo("READY");
		verify(aftersalesRefundService, times(1))
				.findSingleForConfirmCancel(eq(1L), eq(200L), eq(0L), isNull(), eq(List.of("READY")));
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName(
			"Wxapp buyer full cancel PAYED+PENDING (operator_type=buyer): after commit publishes trade refund once;"
					+ " payload cancel_from=buyer (distinct from admin shop-cancel probes)")
	void wxappBuyerPendingPayed_afterCommit_invokesTradeRefundPublisherOnce_withBuyerCancelFromInPayload() {
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
		order.setDistributorId(0L);
		order.setSupplierId(0);
		order.setTotalFee("10000");
		order.setPoint(0);
		order.setPayType("wxpay");
		order.setFreightFee(0);
		order.setFreightType("normal");

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(null);
		when(tradeCancelSettingRedisService.getCancelSetting(1L)).thenReturn(Map.of("repeat_cancel", false));
		when(cancelOrdersMapper.selectOne(any())).thenReturn(null);
		when(cancelOrdersMapper.insert(any(CancelOrders.class)))
				.thenAnswer(
						inv -> {
							CancelOrders c = inv.getArgument(0);
							c.setCancelId(56L);
							return 1;
						});

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "tid-wxapp-1");
		trade.put("pay_type", "wxpay");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0f);
		trade.put("cur_fee_symbol", "￥");
		trade.put("pay_fee", 10000);
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, 200L)).thenReturn(Optional.of(trade));

		AftersalesRefund preorderRefund = new AftersalesRefund();
		preorderRefund.setRefundBn(8_888_888L);
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
				new NormalOrderStatusUpdateService(normalOrdersMapper, org.mockito.Mockito.mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class), supplierOrderMapper, orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));

		TransactionTemplate tx = new TransactionTemplate(txMgr);
		tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		tx.executeWithoutResult(
				st ->
						svc.execute(
								1L,
								"buyer",
								0L,
								0L,
								100L,
								"13800000000",
								200L,
								"reason",
								Map.of("other_reason", "x"),
								"buyer"));

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		InOrder afterCommitOrder =
				inOrder(thirdPartyTradeUpdateDispatchPublisher, tradeRefundDispatchPublisher, normalOrderCancelDispatchPublisher);
		afterCommitOrder.verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(anyMap());
		afterCommitOrder.verify(tradeRefundDispatchPublisher, times(1)).publish(captor.capture());
		afterCommitOrder.verify(normalOrderCancelDispatchPublisher, times(1)).publish(anyMap());
		Map<String, Object> published = captor.getValue();
		assertThat(published.get("action")).isEqualTo("cancel_order");
		assertThat(published.get("cancel_from")).isEqualTo("buyer");
		assertThat(published.get("cancel_id")).isNotNull();
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo("200");
		assertThat(String.valueOf(published.get("company_id"))).isEqualTo("1");
		assertThat(published.get("refund_bn")).isEqualTo(8_888_888L);
		assertThat(published.get("refund_status")).isEqualTo("READY");
		verify(aftersalesRefundService, times(1))
				.findSingleForConfirmCancel(eq(1L), eq(200L), eq(0L), isNull(), eq(List.of("READY")));
	}
}
