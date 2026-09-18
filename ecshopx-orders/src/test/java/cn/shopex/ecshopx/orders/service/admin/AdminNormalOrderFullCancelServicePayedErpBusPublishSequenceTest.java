package cn.shopex.ecshopx.orders.service.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeCancelDispatchPublisher;
import cn.shopex.ecshopx.common.event.SaasErpRefundSpringEvent;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryFormalCancelOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.event.WdtErpTradeCancelSpringEvent;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminNormalOrderFullCancelServicePayedErpBusPublishSequenceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CancelOrders.class);
	}

	@Mock
	NormalOrdersMapper normalOrdersMapper;
	@Mock
	SupplierOrderMapper supplierOrderMapper;
	@Mock
	CancelOrdersMapper cancelOrdersMapper;
	@Mock
	OrderAssociationsMapper orderAssociationsMapper;
	@Mock
	NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	@Mock
	TradeCancelSettingRedisService tradeCancelSettingRedisService;
	@Mock
	OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	@Mock
	AftersalesRefundService aftersalesRefundService;
	@Mock
	PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	@Mock
	OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService;
	@Mock
	DadaLocalDeliveryFormalCancelOrderPort dadaLocalDeliveryFormalCancelOrderPort;
	@Mock
	ApplicationEventPublisher applicationEventPublisher;
	@Mock
	OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock
	PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	@Mock
	JushuitanTradeCancelDispatchPublisher jushuitanTradeCancelDispatchPublisher;
	@Mock
	WdtErpTradeCancelDispatchPublisher wdtErpTradeCancelDispatchPublisher;
	@Mock
	NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher;

	@Mock
	TradeRefundDispatchPublisher tradeRefundDispatchPublisher;

	@Mock
	ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	@Mock
	NormalOrderFullCancelItemStoreRestoreService normalOrderFullCancelItemStoreRestoreService;

	@Mock
	NormalOrderStatusUpdateService normalOrderStatusUpdateService;

	@Mock
	PlatformSelfSubCancelSupport platformSelfSubCancelSupport;

	@Mock
	NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService;

	@InjectMocks
	AdminNormalOrderFullCancelService svc;

	@Test
	@DisplayName(
			"admin OrderController#cancelOrder shares AdminNormalOrderFullCancelService with wxapp entry-01; "
					+ "payed cancel publishes Jushuitan then WDT bus then Spring events then trade-refund Bus before normal-order-cancel Bus")
	void payedCancelWithCancelId_publishesJushuitanThenWdtBusThenSpringEventInOrder() {
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
		when(aftersalesRefundService.countReadySupplierSubCancelRefunds(1L, 200L)).thenReturn(0L);

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

		Map<String, Object> res =
				svc.execute(1L, "shop", 0L, 0L, 100L, "13800000000", 200L, "buyer reason", Map.of("other_reason", "x"));

		assertNotNull(res);

		InOrder inOrder =
				inOrder(
						jushuitanTradeCancelDispatchPublisher,
						wdtErpTradeCancelDispatchPublisher,
						applicationEventPublisher,
						tradeRefundDispatchPublisher,
						normalOrderCancelDispatchPublisher);
		inOrder.verify(jushuitanTradeCancelDispatchPublisher, times(1)).publish(anyMap());
		inOrder.verify(wdtErpTradeCancelDispatchPublisher, times(1)).publish(anyMap());
		inOrder.verify(applicationEventPublisher, times(1)).publishEvent(isA(WdtErpTradeCancelSpringEvent.class));
		inOrder.verify(applicationEventPublisher, times(1)).publishEvent(isA(SaasErpRefundSpringEvent.class));
		inOrder.verify(tradeRefundDispatchPublisher, times(1)).publish(anyMap());
		inOrder.verify(normalOrderCancelDispatchPublisher, times(1)).publish(anyMap());
		verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(anyMap());
	}
}
