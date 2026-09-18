package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
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
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
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
@DisplayName("EVENT_ORDER_PROCESS_LOG: community activity system NOTPAY cancel — publishEvent probe")
class AdminNormalOrderFullCancelServiceCommunityActivitySystemNotpayCancelOrderProcessLogDispatchPublishProbeTest {

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
	void notPay_systemCancel_afterCommit_invokesPublishEventWithSystemShapedOplPayload() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		AdminNormalOrderFullCancelService svc = buildServiceForNotPayCancel(dispatchFacade);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("other_reason", "活动结束自动取消");

		runNotPayCancelInTx(svc, merged);

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
		assertThat(payload.get("remarks")).isEqualTo("取消订单");
		assertThat(payload.get("detail")).isEqualTo("订单号：200，来源：system");
		assertThat(payload.get("operator_type")).isEqualTo("system");
		assertThat(payload.get("operator_id")).isEqualTo(0L);
		assertThat(payload.get("is_show")).isEqualTo(Boolean.FALSE);
		assertThat(payload.get("params")).isEqualTo(new LinkedHashMap<>(merged));
	}

	@Test
	@SuppressWarnings("unchecked")
	void notPay_chiefCancel_afterCommit_invokesPublishEventWithChiefShapedOplPayload() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		AdminNormalOrderFullCancelService svc = buildServiceForNotPayCancel(dispatchFacade);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("other_reason", "成团失败");
		merged.put("chief_id", 777L);

		runNotPayCancelInTx(svc, merged);

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
		assertThat(payload.get("remarks")).isEqualTo("取消订单");
		assertThat(payload.get("detail")).isEqualTo("订单号：200，来源：chief");
		assertThat(payload.get("operator_type")).isEqualTo("chief");
		assertThat(payload.get("operator_id")).isEqualTo(777L);
		assertThat(payload.get("is_show")).isEqualTo(Boolean.FALSE);
		assertThat(payload.get("params")).isEqualTo(new LinkedHashMap<>(merged));
	}

	private static void runNotPayCancelInTx(AdminNormalOrderFullCancelService svc, Map<String, Object> merged) {
		String cancelFromForRecord = communityActivityNotPayCancelFromForProbe(merged);
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
								"活动结束自动取消",
								merged,
								cancelFromForRecord));
	}

	private static String communityActivityNotPayCancelFromForProbe(Map<String, Object> merged) {
		if (merged == null) {
			return "system";
		}
		Object cf = merged.get("cancel_from");
		if (cf != null) {
			String s = String.valueOf(cf).trim();
			if (!s.isEmpty()) {
				return s;
			}
		}
		Object chiefId = merged.get("chief_id");
		if (chiefId instanceof Number && ((Number) chiefId).longValue() > 0L) {
			return "chief";
		}
		return "system";
	}

	private static AdminNormalOrderFullCancelService buildServiceForNotPayCancel(DispatchFacade dispatchFacade) {
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
