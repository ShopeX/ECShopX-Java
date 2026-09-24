package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.distribution.DistributorWhiteListCheckUserValidPort;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutEmployeePurchaseCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseFormatPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsNormalCheckoutPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterValidateService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterWxappAfterOrderService;
import cn.shopex.ecshopx.orders.service.front.wxapp.strategy.WxappNormalOrderCreateStrategy;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
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
class WxappOrderCreateServiceCreateNewOrderNormalOrderAddDispatchPublishProbeTest {

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
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private OrderCreateNeedParamsPort orderCreateNeedParamsPort;

	@Mock
	private OrderCheckoutCartPort orderCheckoutCartPort;

	@Mock
	private OrderCheckoutEmployeePurchaseCartPort orderCheckoutEmployeePurchaseCartPort;

	@Mock
	private OrderCreateEmployeePurchaseFormatPort orderCreateEmployeePurchaseFormatPort;

	@Mock
	private OrderCreateItemCheckPort orderCreateItemCheckPort;

	@Mock
	private OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;

	@Mock
	private OrderCreateFormatDataPort orderCreateFormatDataPort;

	@Mock
	private OrderCreateGroupsNormalCheckoutPort orderCreateGroupsNormalCheckoutPort;

	@Mock
	private WxappNormalOrderTempInfoEnrichmentService wxappNormalOrderTempInfoEnrichmentService;

	@Mock
	private WxappNormalOrderCreateTransactionalRunner wxappNormalOrderCreateTransactionalRunner;

	@Mock
	private DispatchFacade dispatchFacade;

	@Mock
	private DispatchListener normalOrderAddFanOutListener;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private WxappOrderCreateService wxappOrderCreateService;

	@BeforeEach
	void setUp() {
		NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher =
				payload ->
						dispatchFacade.publishEvent(
								OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
								payload,
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault()));
		WxappNormalOrderCreateOrchestrator wxappNormalOrderCreateOrchestrator =
				new WxappNormalOrderCreateOrchestrator(
						orderCreateNeedParamsPort,
						orderCheckoutCartPort,
						orderCheckoutEmployeePurchaseCartPort,
						orderCreateEmployeePurchaseFormatPort,
						orderCreateItemCheckPort,
						orderCreateDistributorCheckPort,
						orderCreateFormatDataPort,
						orderCreateGroupsNormalCheckoutPort,
						wxappNormalOrderTempInfoEnrichmentService,
						wxappNormalOrderCreateTransactionalRunner,
						normalOrderAddDispatchPublisher,
						mock(cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService.class),
						mock(cn.shopex.ecshopx.common.goods.GoodsRecommendCheckoutMergePort.class));
		WxappPhysicalNormalOrderCreateSupport wxappPhysicalNormalOrderCreateSupport =
				new WxappPhysicalNormalOrderCreateSupport(wxappNormalOrderCreateOrchestrator);
		WxappNormalOrderCreateStrategy wxappNormalOrderCreateStrategy =
				new WxappNormalOrderCreateStrategy(wxappPhysicalNormalOrderCreateSupport);
		WxappOrderTypeRegistry wxappOrderTypeRegistry =
				new WxappOrderTypeRegistry(Map.of("wxappOrderTypeCreateNormal", wxappNormalOrderCreateStrategy));
		wxappOrderCreateService =
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
	@DisplayName(
			"EVENT_NORMAL_ORDER_ADD: wxapp order_new — createNewOrder delegates to normal path and publishes after transaction")
	void createNewOrder_whenOrderTypeNormal_thenPublishEventOnceWithThreeKeyPayload() {
		long companyId = 9L;
		long userId = 501L;
		long orderId = 1001L;
		String payType = "wxpay";

		Map<String, Object> mergedParams = new LinkedHashMap<>();
		mergedParams.put("order_type", "normal");
		mergedParams.put("pay_type", payType);
		mergedParams.put("receipt_type", "ziti");

		Map<String, Object> h5AuthClaims = new LinkedHashMap<>();
		h5AuthClaims.put("company_id", companyId);
		h5AuthClaims.put("user_id", userId);
		h5AuthClaims.put("wxapp_appid", "wx-probe");
		h5AuthClaims.put("open_id", "oid-probe");

		when(wxappOrderSourceFromResolveService.resolve(eq(companyId), eq(httpServletRequest), any()))
				.thenReturn("wxapp");
		when(wxappOrderClientIpResolveService.resolve(eq(httpServletRequest))).thenReturn("10.0.0.9");

		doAnswer(
						invocation -> {
							NormalOrderCreateParams p = invocation.getArgument(0);
							Map<String, Object> insert = p.getOrdersInsertResult();
							insert.put("company_id", companyId);
							insert.put("order_id", orderId);
							insert.put("pay_type", payType);
							insert.put("total_fee", 199);
							insert.put("title", "probe order");
							insert.put("shop_id", 1L);
							insert.put("distributor_id", 0L);
							insert.put("team_id", 2L);
							return null;
						})
				.when(wxappNormalOrderCreateTransactionalRunner)
				.runInTransaction(any(NormalOrderCreateParams.class), any(HttpServletRequest.class));

		CurrencyExchangeRate cur = new CurrencyExchangeRate();
		cur.setCurrency("CNY");
		cur.setSymbol("￥");
		cur.setRate(1.0);
		when(companyDefaultCurrencyService.getCur(companyId)).thenReturn(cur);
		when(memberAccountService.getMemberInfo(userId, companyId)).thenReturn(new LinkedHashMap<>(h5AuthClaims));

		wxappOrderCreateService.createNewOrder(httpServletRequest, mergedParams, h5AuthClaims);

		DispatchOptions expectedParent =
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault());
		verify(dispatchFacade, times(1))
				.publishEvent(eq(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD), payloadCaptor.capture(), eq(expectedParent));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(companyId, payload.get("company_id"));
		assertEquals(orderId, payload.get("order_id"));
		assertEquals(payType, payload.get("pay_type"));
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_ADD: wxapp order_new — createNewOrder published payload routes through DispatchConsumerRuntime")
	void createNewOrder_whenOrderTypeNormal_thenDispatchConsumerRuntimeConsumesFanOutListenerTask() {
		long companyId = 11L;
		long userId = 601L;
		long orderId = 2002L;
		String payType = "wxpay";

		Map<String, Object> mergedParams = new LinkedHashMap<>();
		mergedParams.put("order_type", "normal");
		mergedParams.put("pay_type", payType);
		mergedParams.put("receipt_type", "ziti");

		Map<String, Object> h5AuthClaims = new LinkedHashMap<>();
		h5AuthClaims.put("company_id", companyId);
		h5AuthClaims.put("user_id", userId);
		h5AuthClaims.put("wxapp_appid", "wx-probe");
		h5AuthClaims.put("open_id", "oid-probe");

		when(wxappOrderSourceFromResolveService.resolve(eq(companyId), eq(httpServletRequest), any()))
				.thenReturn("wxapp");
		when(wxappOrderClientIpResolveService.resolve(eq(httpServletRequest))).thenReturn("10.0.0.11");

		doAnswer(
						invocation -> {
							NormalOrderCreateParams p = invocation.getArgument(0);
							Map<String, Object> insert = p.getOrdersInsertResult();
							insert.put("company_id", companyId);
							insert.put("order_id", orderId);
							insert.put("pay_type", payType);
							insert.put("total_fee", 299);
							insert.put("title", "probe order consume");
							insert.put("shop_id", 3L);
							insert.put("distributor_id", 0L);
							insert.put("team_id", 4L);
							return null;
						})
				.when(wxappNormalOrderCreateTransactionalRunner)
				.runInTransaction(any(NormalOrderCreateParams.class), any(HttpServletRequest.class));

		CurrencyExchangeRate cur = new CurrencyExchangeRate();
		cur.setCurrency("CNY");
		cur.setSymbol("￥");
		cur.setRate(1.0);
		when(companyDefaultCurrencyService.getCur(companyId)).thenReturn(cur);
		when(memberAccountService.getMemberInfo(userId, companyId)).thenReturn(new LinkedHashMap<>(h5AuthClaims));

		wxappOrderCreateService.createNewOrder(httpServletRequest, mergedParams, h5AuthClaims);

		DispatchOptions expectedParent =
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault());
		verify(dispatchFacade, times(1))
				.publishEvent(eq(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD), payloadCaptor.capture(), eq(expectedParent));

		Map<String, Object> published = payloadCaptor.getValue();
		assertEquals(companyId, published.get("company_id"));
		assertEquals(orderId, published.get("order_id"));
		assertEquals(payType, published.get("pay_type"));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
				OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_ADD,
				ListenerDispatchOptions.async("default", null),
				normalOrderAddFanOutListener);
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_ADD,
						published,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-wxapp-order-new-normal-order-add",
						OrdersDispatchEventNames.LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_ADD);

		runtime.consume(message, 1);

		verify(normalOrderAddFanOutListener, times(1)).onEvent(eq(published));
	}
}
