package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
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
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Admin full cancel: re-apply after shop rejected cancel")
class AdminNormalOrderFullCancelServiceRejectedCancelReapplyTest {

	private static final long ORDER_ID = 5353021000065097L;

	private record Fixture(
			AdminNormalOrderFullCancelService service,
			CancelOrdersMapper cancelOrdersMapper,
			AftersalesRefundService aftersalesRefundService) {}

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CancelOrders.class);
	}

	@Test
	@DisplayName("PAYED order with rejected cancel (FAILS): re-apply updates existing cancel row and creates refund")
	void payedRejectedCancel_reapplyWithoutRepeatCancel_succeeds() {
		Fixture fixture = buildServiceWithMocks(true);
		TransactionTemplate tx = new TransactionTemplate(mockTxManager());

		Map<String, Object> result =
				tx.execute(
						st ->
								fixture.service()
										.execute(
												1L,
												"shop",
												0L,
												0L,
												100L,
												"13800000000",
												ORDER_ID,
												"reason",
												Map.of("other_reason", "x")));

		assertThat(result).isNotNull();
		assertThat(String.valueOf(result.get("order_id"))).isEqualTo(String.valueOf(ORDER_ID));
		assertThat(result.get("refund_status")).isEqualTo("WAIT_CHECK");
		verify(fixture.cancelOrdersMapper(), never()).insert(any(CancelOrders.class));
		verify(fixture.cancelOrdersMapper(), times(1)).update(any(), any());
		verify(fixture.aftersalesRefundService(), times(1)).createRefund(any());
	}

	@Test
	@DisplayName("Existing non-rejected cancel row: re-apply throws when repeat_cancel=false")
	void existingPendingCancel_reapplyWithoutRepeatCancel_throws() {
		Fixture fixture = buildServiceWithMocks(false);

		assertThatThrownBy(
						() ->
								fixture.service()
										.execute(
												1L,
												"shop",
												0L,
												0L,
												100L,
												"13800000000",
												ORDER_ID,
												"reason",
												Map.of("other_reason", "x")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("不能重复取消订单");
	}

	private Fixture buildServiceWithMocks(boolean rejectedExisting) {
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
		order.setOrderId(ORDER_ID);
		order.setCompanyId(1L);
		order.setUserId(100L);
		order.setCancelStatus(rejectedExisting ? "FAILS" : "NO_APPLY_CANCEL");
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

		CancelOrders existing = new CancelOrders();
		existing.setCancelId(99L);
		existing.setOrderId(ORDER_ID);
		existing.setCompanyId(1L);
		existing.setUserId(100L);
		existing.setSupplierId(0L);
		if (rejectedExisting) {
			existing.setProgress(4);
			existing.setRefundStatus("SHOP_CHECK_FAILS");
			existing.setShopRejectReason("拒绝原因");
		} else {
			existing.setProgress(0);
			existing.setRefundStatus("WAIT_CHECK");
		}
		when(cancelOrdersMapper.selectOne(any())).thenReturn(existing);

		CancelOrders reloaded = new CancelOrders();
		reloaded.setCancelId(99L);
		reloaded.setOrderId(ORDER_ID);
		reloaded.setCompanyId(1L);
		reloaded.setUserId(100L);
		reloaded.setSupplierId(0L);
		reloaded.setProgress(0);
		reloaded.setRefundStatus("WAIT_CHECK");
		reloaded.setCancelFrom("shop");
		reloaded.setCancelReason("reason");
		when(cancelOrdersMapper.selectById(99L)).thenReturn(reloaded);
		when(cancelOrdersMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "tid-1");
		trade.put("pay_type", "wxpay");
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0f);
		trade.put("cur_fee_symbol", "￥");
		trade.put("pay_fee", 10000);
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(1L, ORDER_ID)).thenReturn(Optional.of(trade));

		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							OrderAssociations a = new OrderAssociations();
							a.setCompanyId(1L);
							a.setOrderId(ORDER_ID);
							a.setUserId(100L);
							a.setOrderClass("normal_groups");
							a.setOrderStatus("PAYED");
							return a;
						});

		PlatformSelfSubCancelSupport platformSelfSubCancelSupport = mock(PlatformSelfSubCancelSupport.class);
		when(platformSelfSubCancelSupport.countReadyPlatformSelfSubCancelRefunds(1L, ORDER_ID)).thenReturn(0L);
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		when(aftersalesRefundService.countReadySupplierSubCancelRefunds(1L, ORDER_ID)).thenReturn(0L);

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

		return new Fixture(svc, cancelOrdersMapper, aftersalesRefundService);
	}

	private PlatformTransactionManager mockTxManager() {
		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any())).thenReturn(new SimpleTransactionStatus(true));
		return txMgr;
	}
}
