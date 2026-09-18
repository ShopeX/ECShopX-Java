package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.normal.MemberConsumptionOnNormalOrderFinishService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderBrokerageOnFinishService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.OrderPickupSmsRedisVerifyService;
import cn.shopex.ecshopx.orders.service.normal.OrdersRelChinaumspayDivisionWriteService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenapiThirdApiV2OrderWriteoffServiceThirdPartyTradeUpdatePublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private PickupcodeSettingRedisService pickupcodeSettingRedisService;

	@Mock
	private OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;

	@Mock
	private MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService;

	@Mock
	private OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;

	@Mock
	private WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private AdminNormalOrderDetailService adminNormalOrderDetailService;

	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	private DispatchFacade dispatchFacade;

	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	private OpenapiThirdApiV2OrderWriteoffService openapiThirdApiV2OrderWriteoffService;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		NormalOrderZitiWriteoffService normalOrderZitiWriteoffService =
				new NormalOrderZitiWriteoffService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						tradeMapper,
						normalOrderBrokerageOnFinishService,
						memberConsumptionOnNormalOrderFinishService,
						ordersRelChinaumspayDivisionWriteService,
						wxOrderShippingDispatchPublisher,
						orderValiditySettingRedisReadService,
						orderProcessLogPublishPort,
						adminNormalOrderDetailService,
						thirdPartyTradeUpdateDispatchPublisher);
		openapiThirdApiV2OrderWriteoffService =
				new OpenapiThirdApiV2OrderWriteoffService(
						orderAssociationsMapper,
						pickupcodeSettingRedisService,
						orderPickupSmsRedisVerifyService,
						normalOrdersMapper,
						normalOrderZitiWriteoffService);
	}

	@DisplayName(
			"OpenAPI ecx.order.writeoff：存在 tradeId 时经 OpenapiThirdApiV2OrderWriteoffService→核销核心触发一次 ThirdPartyTradeUpdateDispatchPublisher#publish")
	@Test
	void executeOpenapiWriteoff_whenTradeIdPresent_publishesThirdPartyTradeUpdateOnce() {
		long companyId = 12L;
		long orderId = 70001L;

		stubOpenapiOrchestration(companyId, orderId);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setNum(2);
		line.setId(null);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId))
				.thenReturn(Collections.emptyMap());
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);

		Trade trade = new Trade();
		trade.setTradeId("T-WX-1");
		trade.setWxaAppid("wxapp1");
		when(tradeMapper.selectList(any())).thenReturn(List.of(trade));

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("company_id", companyId);
		orderInfo.put("order_id", String.valueOf(orderId));
		orderInfo.put("order_class", "normal");
		orderInfo.put("user_id", 501L);
		Map<String, Object> bundle = new LinkedHashMap<>();
		bundle.put("orderInfo", orderInfo);
		when(adminNormalOrderDetailService.buildOrderBundle(companyId, String.valueOf(orderId), false))
				.thenReturn(bundle);

		openapiThirdApiV2OrderWriteoffService.executeOpenapiWriteoff(companyId, String.valueOf(orderId), null);

		verify(adminNormalOrderDetailService, times(1))
				.buildOrderBundle(eq(companyId), eq(String.valueOf(orderId)), eq(false));
		verify(thirdPartyTradeUpdateDispatchPublisher, times(1))
				.publish(
						argThat(
								m ->
										m != null
												&& Long.valueOf(companyId).equals(m.get("company_id"))
												&& String.valueOf(orderId).equals(m.get("order_id"))
												&& "normal".equals(m.get("order_class"))
												&& Long.valueOf(501L).equals(m.get("user_id"))));
	}

	@DisplayName("OpenAPI ecx.order.writeoff：无 tradeId 时不得调用 ThirdPartyTradeUpdateDispatchPublisher#publish")
	@Test
	void executeOpenapiWriteoff_whenTradeIdAbsent_neverPublishesThirdPartyTradeUpdate() {
		long companyId = 12L;
		long orderId = 70001L;

		stubOpenapiOrchestration(companyId, orderId);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setNum(2);
		line.setId(null);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId))
				.thenReturn(Collections.emptyMap());
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);
		when(tradeMapper.selectList(any())).thenReturn(Collections.emptyList());

		openapiThirdApiV2OrderWriteoffService.executeOpenapiWriteoff(companyId, String.valueOf(orderId), null);

		verify(adminNormalOrderDetailService, never())
				.buildOrderBundle(anyLong(), anyString(), anyBoolean());
		verify(thirdPartyTradeUpdateDispatchPublisher, never()).publish(any());
	}

	private void stubOpenapiOrchestration(long companyId, long orderId) {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderId);
		assoc.setOrderType("normal");
		assoc.setOrderClass("normal");

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(pickupcodeSettingRedisService.handle(eq(companyId), eq(null)))
				.thenReturn(Map.of("pickupcode_status", false));

		NormalOrders normal = openapiEligibleOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);
	}

	private static NormalOrders openapiEligibleOrder(long companyId, long orderId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setReceiptType("ziti");
		order.setOrderStatus("PAYED");
		order.setZitiStatus("PENDING");
		order.setPayStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setPayType("wxpay");
		order.setDistributorId(0L);
		order.setUserId(1L);
		order.setMobile("13800000000");
		return order;
	}
}
