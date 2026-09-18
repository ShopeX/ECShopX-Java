package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.distribution.DistributorWhiteListCheckUserValidPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterValidateService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterWxappAfterOrderService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: wxapp order create publishEvent probe")
class WxappOrderCreateServiceWxappOrderCreatedOrderProcessLogDispatchPublishProbeTest {

	@Mock
	private WxappOrderSourceFromResolveService wxappOrderSourceFromResolveService;

	@Mock
	private WxappOrderClientIpResolveService wxappOrderClientIpResolveService;

	@Mock
	private OrderEpidemicRegisterValidateService orderEpidemicRegisterValidateService;

	@Mock
	private OrderEpidemicRegisterWxappAfterOrderService orderEpidemicRegisterWxappAfterOrderService;

	@Mock
	private WxappOrderSalespersonResolveService wxappOrderSalespersonResolveService;

	@Mock
	private WxappOrderTypeRegistry wxappOrderTypeRegistry;

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;

	@Mock
	private DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	@Mock
	private CompanyDefaultCurrencyService companyDefaultCurrencyService;

	@Mock
	private DistributorWhiteListCheckUserValidPort distributorWhiteListCheckUserValidPort;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private SendPayOrdersRemindJobDispatchPublisher sendPayOrdersRemindJobDispatchPublisher;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;

	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	private WxappOrderCreateService service;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		service =
				new WxappOrderCreateService(
						wxappOrderSourceFromResolveService,
						wxappOrderClientIpResolveService,
						orderEpidemicRegisterValidateService,
						orderEpidemicRegisterWxappAfterOrderService,
						wxappOrderSalespersonResolveService,
						wxappOrderTypeRegistry,
						memberAccountService,
						ordersPaymentDoPaymentService,
						distributorGetInfoSimpleByDistributorIdPort,
						companyDefaultCurrencyService,
						distributorWhiteListCheckUserValidPort,
						objectMapper,
						sendPayOrdersRemindJobDispatchPublisher,
						orderProcessLogPublishPort);
	}

	@Test
	void createOrder_afterRegistryCreate_success_invokesPublishEventOnce_withNormalOrderCreatePayload() {
		long companyId = 9L;
		long userId = 501L;

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_type", "normal_drug");
		merged.put("pay_type", "wxpay");
		merged.put("probe_marker", "m1");

		Map<String, Object> sessionAuth = new LinkedHashMap<>();
		sessionAuth.put("company_id", companyId);
		sessionAuth.put("user_id", userId);
		sessionAuth.put("wxapp_appid", "wx-probe");
		sessionAuth.put("open_id", "oid-probe");

		when(wxappOrderSourceFromResolveService.resolve(eq(companyId), eq(httpServletRequest), any()))
				.thenReturn("wxapp");
		when(wxappOrderClientIpResolveService.resolve(httpServletRequest)).thenReturn("10.0.0.1");

		Map<String, Object> created = new LinkedHashMap<>();
		created.put("order_id", 1001L);
		created.put("total_fee", 10);
		created.put("title", "t");
		created.put("shop_id", 1L);
		created.put("distributor_id", 0L);
		created.put("team_id", 2L);
		created.put("create_time", "2026-05-09 10:00:00");
		created.put("prescription_status", 0);
		created.put("point", 0);
		created.put("discount_fee", 0);
		created.put("discount_info", Map.of());
		when(wxappOrderTypeRegistry.create(any(WxappOrderCreateContext.class))).thenReturn(created);

		CurrencyExchangeRate cur = new CurrencyExchangeRate();
		cur.setCurrency("CNY");
		cur.setSymbol("￥");
		cur.setRate(1.0);
		when(companyDefaultCurrencyService.getCur(companyId)).thenReturn(cur);
		when(memberAccountService.getMemberInfo(userId, companyId)).thenReturn(new LinkedHashMap<>(sessionAuth));

		LinkedHashMap<String, Object> expectedParams = WxappOrderParamMergeSupport.applyDefaultsAndAuth(merged, sessionAuth);
		expectedParams.put("source_from", "wxapp");
		expectedParams.put("client_ip", "10.0.0.1");
		expectedParams.putIfAbsent("salesman_id", 0L);

		service.createOrder(httpServletRequest, merged, sessionAuth);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("1001", payload.get("order_id"));
		assertEquals("9", payload.get("company_id"));
		assertEquals("user", payload.get("operator_type"));
		assertEquals(Boolean.TRUE, payload.get("is_show"));
		assertEquals(501L, payload.get("operator_id"));
		assertEquals("订单创建", payload.get("remarks"));
		assertEquals("订单号：1001，订单创建", payload.get("detail"));
		assertEquals(expectedParams, payload.get("params"));
	}

	@Test
	void createOrder_afterRegistryCreate_success_invokesPublishEventOnce_withExcardRemarksPayload() {
		long companyId = 7L;
		long userId = 88L;

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_type", "normal_drug");
		merged.put("pay_type", "wxpay");
		merged.put("order_class", "excard");

		Map<String, Object> sessionAuth = new LinkedHashMap<>();
		sessionAuth.put("company_id", companyId);
		sessionAuth.put("user_id", userId);
		sessionAuth.put("wxapp_appid", "wx-ex");
		sessionAuth.put("open_id", "oid-ex");

		when(wxappOrderSourceFromResolveService.resolve(eq(companyId), eq(httpServletRequest), any()))
				.thenReturn("wxapp");
		when(wxappOrderClientIpResolveService.resolve(httpServletRequest)).thenReturn("10.0.0.2");

		Map<String, Object> created = new LinkedHashMap<>();
		created.put("order_id", "ORD-EX-1");
		when(wxappOrderTypeRegistry.create(any(WxappOrderCreateContext.class))).thenReturn(created);

		CurrencyExchangeRate cur = new CurrencyExchangeRate();
		cur.setCurrency("CNY");
		cur.setSymbol("￥");
		cur.setRate(1.0);
		when(companyDefaultCurrencyService.getCur(companyId)).thenReturn(cur);
		when(memberAccountService.getMemberInfo(userId, companyId)).thenReturn(new LinkedHashMap<>(sessionAuth));

		service.createOrder(httpServletRequest, merged, sessionAuth);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("ORD-EX-1", payload.get("order_id"));
		assertEquals("7", payload.get("company_id"));
		assertEquals("订单核销", payload.get("remarks"));
		assertEquals("订单号：ORD-EX-1，订单核销成功", payload.get("detail"));
		@SuppressWarnings("unchecked")
		Map<String, Object> paramsCopy = (Map<String, Object>) payload.get("params");
		assertTrue(paramsCopy.containsKey("order_class"));
		assertEquals("excard", paramsCopy.get("order_class"));
	}

	@Test
	void createNewOrder_afterRegistryCreate_success_invokesPublishEventOnce_withOrderNewParamMergeAndSourceFromShape() {
		long companyId = 400L;
		long userId = 500L;

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_type", "normal");
		merged.put("pay_type", "wxpay");
		merged.put("probe_marker_order_new", "pn1");

		Map<String, Object> h5Auth = new LinkedHashMap<>();
		h5Auth.put("company_id", companyId);
		h5Auth.put("user_id", userId);
		h5Auth.put("wxapp_appid", "wxatest");
		h5Auth.put("open_id", "open-test");

		when(wxappOrderSourceFromResolveService.resolve(eq(companyId), eq(httpServletRequest), any()))
				.thenReturn("wxapp");

		Map<String, Object> created = new LinkedHashMap<>();
		created.put("order_id", 9001L);
		created.put("total_fee", 199);
		created.put("title", "unit order");
		created.put("shop_id", 1L);
		created.put("distributor_id", 0L);
		created.put("team_id", 7L);
		created.put("create_time", "2026-05-08 12:00:00");
		created.put("prescription_status", 0);
		created.put("point", 0);
		created.put("discount_fee", 0);
		created.put("discount_info", Map.of());
		when(wxappOrderTypeRegistry.create(any(WxappOrderCreateContext.class))).thenReturn(created);

		CurrencyExchangeRate cur = new CurrencyExchangeRate();
		cur.setCurrency("CNY");
		cur.setSymbol("￥");
		cur.setRate(1.0);
		when(companyDefaultCurrencyService.getCur(companyId)).thenReturn(cur);
		when(memberAccountService.getMemberInfo(userId, companyId)).thenReturn(new LinkedHashMap<>(h5Auth));

		LinkedHashMap<String, Object> expectedParams =
				WxappOrderParamMergeSupport.applyDefaultsAndAuthForOrderNew(merged, h5Auth);
		expectedParams.put("source_from", "wxapp");
		expectedParams.putIfAbsent("salesman_id", 0L);

		service.createNewOrder(httpServletRequest, merged, h5Auth);

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("9001", payload.get("order_id"));
		assertEquals("400", payload.get("company_id"));
		assertEquals("user", payload.get("operator_type"));
		assertEquals(Boolean.TRUE, payload.get("is_show"));
		assertEquals(500L, payload.get("operator_id"));
		assertEquals("订单创建", payload.get("remarks"));
		assertEquals("订单号：9001，订单创建", payload.get("detail"));
		assertEquals(expectedParams, payload.get("params"));
	}
}
