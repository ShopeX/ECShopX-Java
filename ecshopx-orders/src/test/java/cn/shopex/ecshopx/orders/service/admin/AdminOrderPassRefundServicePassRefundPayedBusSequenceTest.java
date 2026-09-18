package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeRefundCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReAddOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.event.WdtErpTradeCancelSpringEvent;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderFullCancelItemStoreRestoreService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderStatusUpdateService;

@ExtendWith(MockitoExtension.class)
class AdminOrderPassRefundServicePassRefundPayedBusSequenceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CancelOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderProfit.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersRelDada.class);
	}

	@Mock
	ApplicationContext applicationContext;
	@Mock
	AftersalesRefundService aftersalesRefundService;
	@Mock
	CancelOrdersMapper cancelOrdersMapper;
	@Mock
	NormalOrdersMapper normalOrdersMapper;
	@Mock
	OrderAssociationsMapper orderAssociationsMapper;
	@Mock
	SupplierOrderMapper supplierOrderMapper;
	@Mock
	NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	@Mock
	OrderProfitMapper orderProfitMapper;
	@Mock
	OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	@Mock
	OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService;
	@Mock
	OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock
	AdminOrderBrokerageCancelOnRefundPassService adminOrderBrokerageCancelOnRefundPassService;
	@Mock
	PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	@Mock
	DadaLocalDeliveryReAddOrderPort dadaLocalDeliveryReAddOrderPort;
	@Mock
	AdminOrderEmployeePurchaseRestoreOnRefundPassService adminOrderEmployeePurchaseRestoreOnRefundPassService;
	@Mock
	ApplicationEventPublisher applicationEventPublisher;
	@Mock
	JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher;
	@Mock
	WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher;
	@Mock
	ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;
	@Mock
	ThirdPartyTradeRefundCancelSaasErpDispatchPublisher thirdPartyTradeRefundCancelSaasErpDispatchPublisher;

	AdminOrderPassRefundService svc;

	@BeforeEach
	void wireSelfBean() {
		svc =
				new AdminOrderPassRefundService(
						applicationContext,
						aftersalesRefundService,
						cancelOrdersMapper,
						normalOrdersMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						normalOrdersRelDadaMapper,
						orderProfitMapper,
						orderSuccessTradeReadPort,
						orderInvoiceCancelOnOrderCancelService,
						orderProcessLogPublishPort,
						adminOrderBrokerageCancelOnRefundPassService,
						pointMemberCancelOrderReturnPointsService,
						dadaLocalDeliveryReAddOrderPort,
						adminOrderEmployeePurchaseRestoreOnRefundPassService,
						applicationEventPublisher,
						jushuitanTradeCancelDispatchPublisher,
						wdtErpTradeCancelDispatchPublisher,
						thirdPartyTradeUpdateDispatchPublisher,
						thirdPartyTradeRefundCancelSaasErpDispatchPublisher,
						mock(NormalOrderFullCancelItemStoreRestoreService.class),
						new NormalOrderStatusUpdateService(normalOrdersMapper, org.mockito.Mockito.mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class), supplierOrderMapper, orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
						mock(PartialDeliveryFulfillmentReconcileService.class),
				mock(NormalOrderCancelDiscountRestoreService.class));
		when(applicationContext.getBean(AdminOrderPassRefundService.class)).thenReturn(svc);
		lenient().when(orderSuccessTradeReadPort.primarySuccessTrade(anyLong(), anyLong()))
				.thenReturn(Optional.empty());
		lenient().when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		lenient().when(aftersalesRefundService.countReadySupplierSubCancelRefunds(anyLong(), anyLong()))
				.thenReturn(0L);
		lenient().when(aftersalesRefundService.hasApprovedFullOrderCancelRefund(anyLong(), anyLong()))
				.thenReturn(false);
	}

	@Test
	@DisplayName(
			"admin confirm cancel agrees refund: passRefund PAYED publishes Jushuitan bus, WDT bus, then WdtErpTradeCancelSpringEvent in order")
	void passRefundPayed_publishesJushuitanThenWdtBusThenWdtSpringEventInOrder() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setRefundBn(900L);
		refund.setSupplierId(0L);
		refund.setOrderId(200L);
		refund.setCompanyId(1L);

		when(aftersalesRefundService.updateRefundByConfirmFilter(anyLong(), anyLong(), any(), any(), anyMap()))
				.thenReturn(1);
		when(cancelOrdersMapper.update(any(), any())).thenReturn(1);

		NormalOrders norm = new NormalOrders();
		norm.setCompanyId(1L);
		norm.setOrderId(200L);
		norm.setOrderStatus("PAYED");
		norm.setSupplierId(0);
		norm.setDistributorId(77L);
		norm.setUserId(10L);
		norm.setPointUse(0);
		norm.setPayType("wxpay");
		norm.setReceiptType("store");
		when(normalOrdersMapper.selectOne(any())).thenReturn(norm);

		CancelOrders cancelSnap = new CancelOrders();
		cancelSnap.setCompanyId(1L);
		cancelSnap.setOrderId(200L);
		cancelSnap.setCancelId(501L);
		cancelSnap.setCancelReason("buyer asked");
		when(cancelOrdersMapper.selectOne(any())).thenReturn(cancelSnap);

		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(orderProfitMapper.update(any(), any())).thenReturn(1);

		OrderAssociations assocRow = new OrderAssociations();
		assocRow.setCompanyId(1L);
		assocRow.setOrderId(200L);
		assocRow.setUserId(10L);
		assocRow.setOrderClass("normal");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assocRow);

		Map<String, Object> refundFilter = new LinkedHashMap<>();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 1L);
		params.put("order_id", 200L);
		params.put("operator_type", "shop");
		params.put("operator_id", 1L);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(
				status ->
						svc.passRefund(refundFilter, refund, params));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder inOrder =
				inOrder(jushuitanTradeCancelDispatchPublisher, wdtErpTradeCancelDispatchPublisher, applicationEventPublisher);
		inOrder.verify(jushuitanTradeCancelDispatchPublisher).publish(payloadCaptor.capture());
		inOrder.verify(wdtErpTradeCancelDispatchPublisher).publish(payloadCaptor.capture());
		inOrder.verify(applicationEventPublisher).publishEvent(any(WdtErpTradeCancelSpringEvent.class));
		assertEquals(2, payloadCaptor.getAllValues().size());
		assertSame(payloadCaptor.getAllValues().get(0), payloadCaptor.getAllValues().get(1));
		assertEquals("pass_refund", payloadCaptor.getAllValues().get(0).get("action"));
		assertEquals(1L, payloadCaptor.getAllValues().get(0).get("company_id"));
		assertEquals(200L, payloadCaptor.getAllValues().get(0).get("order_id"));
		assertEquals(77L, payloadCaptor.getAllValues().get(0).get("distributor_id"));
		assertEquals(501L, payloadCaptor.getAllValues().get(0).get("cancel_id"));
		assertEquals("buyer asked", payloadCaptor.getAllValues().get(0).get("cancel_reason"));

		verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(anyMap());
	}

	private static PlatformTransactionManager syncFiringTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition)
					throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new IllegalStateException("nested tx not expected in unit test");
				}
				TransactionSynchronizationManager.initSynchronization();
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				try {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization s :
								new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())) {
							s.afterCommit();
						}
					}
				} finally {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.clearSynchronization();
					}
				}
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					TransactionSynchronizationManager.clearSynchronization();
				}
			}
		};
	}
}
