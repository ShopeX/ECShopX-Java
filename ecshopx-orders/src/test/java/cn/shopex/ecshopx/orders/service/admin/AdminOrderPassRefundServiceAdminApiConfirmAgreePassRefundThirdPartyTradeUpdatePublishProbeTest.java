package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
 * Probes admin confirm-cancel agree path (event-208 / entry-01): {@link AdminOrderPassRefundService#passRefund}
 * publishes {@link ThirdPartyTradeUpdateDispatchPublisher#publish} once on afterCommit with association-shaped
 * payload (aligned with {@code AdminNormalOrderFullCancelService#buildThirdPartyTradeUpdatePayload}).
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderPassRefundServiceAdminApiConfirmAgreePassRefundThirdPartyTradeUpdatePublishProbeTest {

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
	}

	@Test
	@DisplayName(
			"passRefund (non-PAYED): afterCommit invokes ThirdPartyTradeUpdateDispatchPublisher once with CANCEL association payload")
	void adminApiConfirmAgreePassRefund_afterCommit_invokesThirdPartyTradeUpdatePublishOnce_withAssociationShapedPayload() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setRefundBn(800L);
		refund.setSupplierId(0L);
		refund.setOrderId(200L);
		refund.setCompanyId(1L);

		when(aftersalesRefundService.updateRefundByConfirmFilter(anyLong(), anyLong(), any(), any(), anyMap()))
				.thenReturn(1);
		when(cancelOrdersMapper.update(any(), any())).thenReturn(1);

		NormalOrders norm = new NormalOrders();
		norm.setCompanyId(1L);
		norm.setOrderId(200L);
		norm.setOrderStatus("WAIT_PAY");
		norm.setSupplierId(0);
		norm.setUserId(10L);
		norm.setPointUse(0);
		norm.setPayType("wxpay");
		norm.setReceiptType("store");
		when(normalOrdersMapper.selectOne(any())).thenReturn(norm);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(200L);
		assoc.setUserId(10L);
		assoc.setOrderClass("normal");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		CancelOrders outRow = new CancelOrders();
		outRow.setCompanyId(1L);
		outRow.setOrderId(200L);
		outRow.setCancelId(601L);
		when(cancelOrdersMapper.selectOne(any())).thenReturn(outRow);

		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(orderProfitMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", 9001L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(eq(1L), eq(200L))).thenReturn(Optional.of(trade));

		Map<String, Object> refundFilter = new LinkedHashMap<>();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 1L);
		params.put("order_id", 200L);
		params.put("check_cancel", "1");
		params.put("order_type", "normal");
		params.put("operator_type", "shop");
		params.put("operator_id", 7L);
		params.put("refund_bn", "800");

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> svc.passRefund(refundFilter, refund, params));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeUpdateDispatchPublisher).publish(payloadCaptor.capture());
		Map<String, Object> published = payloadCaptor.getValue();
		assertEquals(1L, published.get("company_id"));
		assertEquals("200", published.get("order_id"));
		assertEquals(10L, published.get("user_id"));
		assertEquals("normal", published.get("order_class"));
		assertEquals("CANCEL", published.get("order_status"));
		assertEquals("9001", published.get("trade_id"));
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
