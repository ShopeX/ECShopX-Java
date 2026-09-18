package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
class AdminOrderPassRefundServiceRejectCancelPostCommitDispatchTest {

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
	void setUp() {
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
	}

	@Test
	@DisplayName(
			"reject cancel audit: afterCommit invokes ThirdPartyTradeRefundCancelSaasErpDispatchPublisher then "
					+ "WdtErpTradeCancelDispatchPublisher; no SaasErpRefundSpringEvent; no WdtErpTradeCancelSpringEvent")
	void rejectCancel_afterCommit_invokesTradeRefundCancelSaasErpPublisherThenWdtDispatchPublisherInOrder() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setRefundBn(800L);
		refund.setSupplierId(0L);
		refund.setOrderId(200L);
		refund.setCompanyId(1L);
		refund.setDistributorId(42L);

		when(aftersalesRefundService.updateRefundByConfirmFilter(anyLong(), anyLong(), any(), any(), anyMap()))
				.thenReturn(1);
		when(cancelOrdersMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		NormalOrders norm = new NormalOrders();
		norm.setCompanyId(1L);
		norm.setOrderId(200L);
		norm.setOrderStatus("PAYED");
		when(normalOrdersMapper.selectOne(any())).thenReturn(norm);

		CancelOrders outRow = new CancelOrders();
		outRow.setCompanyId(1L);
		outRow.setOrderId(200L);
		outRow.setCancelId(601L);
		when(cancelOrdersMapper.selectOne(any())).thenReturn(outRow);

		Map<String, Object> refundFilter = new LinkedHashMap<>();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 1L);
		params.put("order_id", 200L);
		params.put("operator_type", "shop");
		params.put("operator_id", 1L);
		params.put("shop_reject_reason", "busy");

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> svc.rejectCancelAuditAfterRefundReady(refundFilter, refund, params));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasCaptor = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> wdtCaptor = ArgumentCaptor.forClass(Map.class);
		InOrder inOrder = inOrder(thirdPartyTradeRefundCancelSaasErpDispatchPublisher, wdtErpTradeCancelDispatchPublisher);
		inOrder.verify(thirdPartyTradeRefundCancelSaasErpDispatchPublisher).publish(saasCaptor.capture());
		inOrder.verify(wdtErpTradeCancelDispatchPublisher).publish(wdtCaptor.capture());

		Map<String, Object> saasPayload = saasCaptor.getValue();
		assertEquals(1L, saasPayload.get("company_id"));
		assertEquals(200L, saasPayload.get("order_id"));
		assertEquals(800L, saasPayload.get("refund_bn"));
		assertEquals("REFUSE", saasPayload.get("refund_status"));

		Map<String, Object> wdtPayload = wdtCaptor.getValue();
		assertEquals(1L, wdtPayload.get("company_id"));
		assertEquals(200L, wdtPayload.get("order_id"));
		assertEquals(42L, wdtPayload.get("distributor_id"));
		assertFalse(wdtPayload.containsKey("action"));

		verify(applicationEventPublisher, never()).publishEvent(any());
		verify(jushuitanTradeCancelDispatchPublisher, never()).publish(anyMap());
	}

	@Test
	void rejectCancel_afterCommit_neverPublishesSaasErpRefundSpringEvent() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setRefundBn(800L);
		refund.setSupplierId(0L);
		refund.setOrderId(200L);
		refund.setCompanyId(1L);
		refund.setDistributorId(42L);

		when(aftersalesRefundService.updateRefundByConfirmFilter(anyLong(), anyLong(), any(), any(), anyMap()))
				.thenReturn(1);
		when(cancelOrdersMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		NormalOrders norm = new NormalOrders();
		norm.setCompanyId(1L);
		norm.setOrderId(200L);
		norm.setOrderStatus("PAYED");
		when(normalOrdersMapper.selectOne(any())).thenReturn(norm);

		CancelOrders outRow = new CancelOrders();
		outRow.setCompanyId(1L);
		outRow.setOrderId(200L);
		outRow.setCancelId(601L);
		when(cancelOrdersMapper.selectOne(any())).thenReturn(outRow);

		Map<String, Object> refundFilter = new LinkedHashMap<>();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 1L);
		params.put("order_id", 200L);
		params.put("operator_type", "shop");
		params.put("operator_id", 1L);
		params.put("shop_reject_reason", "busy");

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> svc.rejectCancelAuditAfterRefundReady(refundFilter, refund, params));

		verify(applicationEventPublisher, never()).publishEvent(any());
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
