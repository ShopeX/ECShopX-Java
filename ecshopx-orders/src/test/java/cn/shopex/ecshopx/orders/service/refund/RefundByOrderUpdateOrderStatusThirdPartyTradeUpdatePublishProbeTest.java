package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.orders.service.admin.PlatformSelfSubCancelSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderFullCancelItemStoreRestoreService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderStatusUpdateService;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Asserts that the PAYED normal_groups system-cancel path through {@link AdminNormalOrderFullCancelService} issues
 * exactly one {@link ThirdPartyTradeUpdateDispatchPublisher#publish} (EVENT_TRADE_UPDATE bus) after association rows
 * are cancelled and re-read, so third-party visibility stays aligned with committed data.
 */
@ExtendWith(MockitoExtension.class)
class RefundByOrderUpdateOrderStatusThirdPartyTradeUpdatePublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CancelOrders.class);
	}

	@Test
	@DisplayName(
			"RefundByOrderUpdateOrderStatus normal_groups: PAYED system cancel triggers one ThirdPartyTradeUpdate publish")
	void normalGroupsCancelPath_afterFullCancelSuccess_invokesThirdPartyTradeUpdatePublishOnce() {
		ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);
		AdminNormalOrderFullCancelService svc =
				buildAdminNormalOrderFullCancelServiceForRefundByOrderProbe(thirdPartyTradeUpdateDispatchPublisher);

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("other_reason", "拼团自动取消");
		svc.execute(1L, "shop", 0L, 0L, 100L, "13800000000", 200L, "", params, "system");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(captor.capture());
		Map<String, Object> published = captor.getValue();
		assertThat(published.get("company_id")).isEqualTo(1L);
		assertThat(String.valueOf(published.get("order_id"))).isEqualTo("200");
		assertThat(published.get("user_id")).isEqualTo(100L);
		assertThat(published.get("order_class")).isEqualTo("normal_groups");
		assertThat(published.get("order_status")).isEqualTo("CANCEL");
		assertThat(String.valueOf(published.get("trade_id"))).isEqualTo("T1");
	}

	private static AdminNormalOrderFullCancelService buildAdminNormalOrderFullCancelServiceForRefundByOrderProbe(
			ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher) {
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
				thirdPartyTradeUpdateDispatchPublisher,
				mock(NormalOrderFullCancelItemStoreRestoreService.class),
				new NormalOrderStatusUpdateService(normalOrdersMapper, org.mockito.Mockito.mock(cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper.class), supplierOrderMapper, orderAssociationsMapper), mock(PlatformSelfSubCancelSupport.class),
				mock(NormalOrderCancelDiscountRestoreService.class));
	}
}
