package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeRefundCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReAddOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
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

/**
 * Probes admin confirm-cancel reject READY refund path: {@link AdminOrderPassRefundService#rejectCancelAuditAfterRefundReady}
 * publishes order process log via {@link OrderProcessLogPublishPortImpl} and {@link DispatchFacade#publishEvent}.
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderPassRefundServiceAdminApiRejectRefundReadyOrderProcessLogDispatchPublishProbeTest {

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
	DispatchFacade dispatchFacade;
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

	OrderProcessLogPublishPort orderProcessLogPublishPort;
	AdminOrderPassRefundService svc;

	@BeforeEach
	void setUp() {
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
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
	}

	@Test
	@DisplayName("EVENT_ORDER_PROCESS_LOG: admin confirm-cancel reject READY refund — publishEvent probe")
	void adminApiRejectRefundReady_invokesPublishEventOnce_withRejectRefundOrderProcessLogPayloadShape() {
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
		params.put("check_cancel", "0");
		params.put("order_type", "normal");
		params.put("operator_type", "");
		params.put("operator_id", 7L);
		params.put("shop_reject_reason", "档期已满");

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> svc.rejectCancelAuditAfterRefundReady(refundFilter, refund, params));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade)
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> published = payloadCaptor.getValue();
		assertEquals(200L, published.get("order_id"));
		assertEquals(1L, published.get("company_id"));
		assertEquals("system", published.get("operator_type"));
		assertEquals(7L, published.get("operator_id"));
		assertEquals("订单退款", published.get("remarks"));
		assertEquals("订单号：200，后台管理员拒绝退款，拒绝原因：档期已满", published.get("detail"));
		assertEquals(Boolean.FALSE, published.get("is_show"));
		assertSame(params, published.get("params"));

		@SuppressWarnings("unchecked")
		Map<String, Object> nestedParams = (Map<String, Object>) published.get("params");
		assertNotNull(nestedParams);
		assertEquals("0", nestedParams.get("check_cancel"));
		assertEquals("normal", nestedParams.get("order_type"));
		assertEquals("档期已满", nestedParams.get("shop_reject_reason"));
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
