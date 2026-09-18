package cn.shopex.ecshopx.wechat.service.wxa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxaOrderShippingApiClientTest {

	private static final String GET_ORDER_URL = "wxa/sec/order/get_order";
	private static final String UPLOAD_URL = "wxa/sec/order/upload_shipping_info";

	@Mock
	private WxJavaMaRuntime runtime;

	@Mock
	private WxMaService maService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private WxaOrderShippingApiClient client;

	@BeforeEach
	void setUp() {
		client = new WxaOrderShippingApiClient(runtime, objectMapper);
	}

	@Test
	void getOrder_postsTransactionIdAndParsesErrcode() throws WxErrorException {
		when(runtime.ma("wxApp123")).thenReturn(maService);
		when(maService.post(eq(GET_ORDER_URL), org.mockito.ArgumentMatchers.<Map<String, Object>>any()))
				.thenAnswer(invocation -> {
			Map<String, Object> body = invocation.getArgument(1);
			assertEquals("txn-abc", body.get("transaction_id"));
			return "{\"errcode\":0,\"order\":{\"order_state\":1}}";
		});

		Map<String, Object> result = client.getOrder("wxApp123", "txn-abc");

		assertEquals(0, result.get("errcode"));
		assertInstanceOf(Map.class, result.get("order"));
		Map<?, ?> order = (Map<?, ?>) result.get("order");
		assertEquals(1, order.get("order_state"));
		verify(runtime).ma("wxApp123");
		verify(maService).post(eq(GET_ORDER_URL), org.mockito.ArgumentMatchers.<Map<String, Object>>any());
	}

	@Test
	void getOrder_wxErrorException_returnsErrcodeMap() throws WxErrorException {
		when(runtime.ma("wxApp123")).thenReturn(maService);
		WxError wxError = WxError.builder().errorCode(10060012).errorMsg("system error").build();
		when(maService.post(eq(GET_ORDER_URL), org.mockito.ArgumentMatchers.<Map<String, Object>>any()))
				.thenThrow(new WxErrorException(wxError));

		Map<String, Object> result = client.getOrder("wxApp123", "txn-abc");

		assertEquals(10060012, result.get("errcode"));
		assertEquals("system error", result.get("errmsg"));
	}

	@Test
	void getOrder_nonJsonResponse_returnsSyntheticErrcodeMap() throws WxErrorException {
		when(runtime.ma("wxApp123")).thenReturn(maService);
		when(maService.post(eq(GET_ORDER_URL), org.mockito.ArgumentMatchers.<Map<String, Object>>any()))
				.thenReturn("not-json");

		Map<String, Object> result = client.getOrder("wxApp123", "txn-abc");

		assertNotEquals(0, result.get("errcode"));
		assertEquals(-999, result.get("errcode"));
		assertEquals("invalid wechat response", result.get("errmsg"));
	}

	@Test
	void uploadShippingInfo_postsParamsAndParsesErrcode() throws WxErrorException {
		when(runtime.ma("wxApp456")).thenReturn(maService);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_key", Map.of("order_number_type", 2));
		when(maService.post(eq(UPLOAD_URL), eq(params)))
				.thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

		Map<String, Object> result = client.uploadShippingInfo("wxApp456", params);

		assertEquals(0, result.get("errcode"));
		assertEquals("ok", result.get("errmsg"));
		verify(maService).post(UPLOAD_URL, params);
	}
}
