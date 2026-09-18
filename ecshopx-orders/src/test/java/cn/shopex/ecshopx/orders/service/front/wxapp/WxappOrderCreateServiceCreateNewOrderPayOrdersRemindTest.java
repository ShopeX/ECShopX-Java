package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.distribution.DistributorWhiteListCheckUserValidPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterValidateService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterWxappAfterOrderService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappOrderCreateServiceCreateNewOrderPayOrdersRemindTest {

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
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Captor
	private ArgumentCaptor<Map<String, Object>> orderDataCaptor;

	private WxappOrderCreateService service;

	@BeforeEach
	void setUp() {
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
	void createNewOrder_publishesPayOrdersRemind_onSuccessPath() {
		long companyId = 400L;
		long userId = 500L;

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_type", "normal");
		merged.put("pay_type", "wxpay");

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

		service.createNewOrder(httpServletRequest, merged, h5Auth);

		verify(sendPayOrdersRemindJobDispatchPublisher, times(1)).publish(orderDataCaptor.capture());
		Map<String, Object> orderData = orderDataCaptor.getValue();
		assertEquals(9001L, orderData.get("order_id"));
		assertEquals(companyId, orderData.get("company_id"));
		assertEquals(userId, orderData.get("user_id"));
		assertEquals("wxatest", orderData.get("wxa_appid"));
		assertEquals("open-test", orderData.get("open_id"));
		assertEquals(199, orderData.get("total_fee"));
	}
}
