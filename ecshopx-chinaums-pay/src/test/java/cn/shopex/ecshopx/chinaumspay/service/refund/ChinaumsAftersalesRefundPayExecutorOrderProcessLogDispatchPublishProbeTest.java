package cn.shopex.ecshopx.chinaumspay.service.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: Chinaums refund executor publishEvent probe")
class ChinaumsAftersalesRefundPayExecutorOrderProcessLogDispatchPublishProbeTest {

	@Test
	void execute_successBranch_invokesPublishEventOnce_withUnionPaySuccessDetail() throws Exception {
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.get(org.mockito.ArgumentMatchers.anyString()))
				.thenReturn("{\"mid\":\"m1\",\"tid\":\"t1\"}");

		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		ChinaumsAftersalesRefundPayExecutor executor =
				new ChinaumsAftersalesRefundPayExecutor(redis, new ObjectMapper(), orderProcessLogPublishPort);
		executor.bindGatewayForProbe("app-id", "app-key", "http://127.0.0.1:9");

		AftersalesRefundPaymentContext ctx = refundContext(88001L, 77001L);

		HttpClient httpClient = mock(HttpClient.class);
		@SuppressWarnings("unchecked")
		HttpResponse<String> httpResponse = mock(HttpResponse.class);
		when(httpResponse.body()).thenReturn("{\"status\":\"SUCCESS\",\"refund_id\":\"rid-cn-1\"}");
		doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

		try (MockedStatic<HttpClient> http = mockStatic(HttpClient.class)) {
			http.when(HttpClient::newHttpClient).thenReturn(httpClient);
			Map<String, Object> payRes = executor.execute(ctx);
			assertEquals("SUCCESS", String.valueOf(payRes.get("status")));
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(77001L, ((Number) payload.get("order_id")).longValue());
		assertEquals(88001L, ((Number) payload.get("company_id")).longValue());
		assertEquals("system", payload.get("operator_type"));
		assertEquals("订单退款", payload.get("remarks"));
		assertEquals("订单号：77001，订单退款成功（银联支付渠道）", payload.get("detail"));
	}

	@Test
	void execute_failBranch_invokesPublishEventOnce_withUnionPayFailDetail() throws Exception {
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(redis.opsForValue()).thenReturn(vo);
		when(vo.get(org.mockito.ArgumentMatchers.anyString()))
				.thenReturn("{\"mid\":\"m1\",\"tid\":\"t1\"}");

		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		ChinaumsAftersalesRefundPayExecutor executor =
				new ChinaumsAftersalesRefundPayExecutor(redis, new ObjectMapper(), orderProcessLogPublishPort);
		executor.bindGatewayForProbe("app-id", "app-key", "http://127.0.0.1:9");

		AftersalesRefundPaymentContext ctx = refundContext(88002L, 77002L);

		HttpClient httpClient = mock(HttpClient.class);
		@SuppressWarnings("unchecked")
		HttpResponse<String> httpResponse = mock(HttpResponse.class);
		when(httpResponse.body()).thenReturn("{\"errCode\":\"BUSY\",\"errMsg\":\"网关拒绝\"}");
		doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

		try (MockedStatic<HttpClient> http = mockStatic(HttpClient.class)) {
			http.when(HttpClient::newHttpClient).thenReturn(httpClient);
			Map<String, Object> payRes = executor.execute(ctx);
			assertEquals("FAIL", String.valueOf(payRes.get("status")));
		}

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("订单号：77002，订单退款失败（银联支付渠道），失败原因：网关拒绝", payload.get("detail"));
	}

	private static AftersalesRefundPaymentContext refundContext(long companyId, long orderId) {
		return new AftersalesRefundPaymentContext(
				companyId,
				"",
				33001L,
				orderId,
				1L,
				0L,
				0L,
				0L,
				0L,
				"chinaums",
				"wx_lite",
				"trade-cn-1",
				200,
				100,
				"",
				"pay_txn_cn_1",
				false,
				"",
				"",
				"",
				0);
	}
}
