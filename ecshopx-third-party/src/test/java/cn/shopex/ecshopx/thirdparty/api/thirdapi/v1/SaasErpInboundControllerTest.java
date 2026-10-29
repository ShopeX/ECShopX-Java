package cn.shopex.ecshopx.thirdparty.api.thirdapi.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpAcVerifier;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpActDispatcher;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpCompanyResolver;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpInboundResponseWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SaasErpInboundControllerTest {

	private static final String TOKEN = "tok";

	private SaasErpAcVerifier verifier;
	private SaasErpCompanyResolver resolver;
	private SaasErpActDispatcher dispatcher;
	private SaasErpInboundResponseWriter responseWriter;
	private OmsQueueLogWritePort logPort;
	private ObjectMapper objectMapper;
	private SaasErpInboundController controller;

	@BeforeEach
	void setUp() {
		verifier = new SaasErpAcVerifier();
		resolver = mock(SaasErpCompanyResolver.class);
		when(resolver.resolve(any())).thenReturn(new SaasErpCompanyResolver.Resolved(1L, TOKEN));
		dispatcher = new SaasErpActDispatcher(List.of());
		logPort = mock(OmsQueueLogWritePort.class);
		objectMapper = new ObjectMapper();
		responseWriter = new SaasErpInboundResponseWriter(logPort, objectMapper);
		controller =
				new SaasErpInboundController(
						verifier, resolver, dispatcher, responseWriter, logPort, objectMapper);
	}

	@Test
	void unknownActWithValidSignReturnsInterfaceMissingWithoutLogInsert() throws Exception {
		Map<String, Object> body = signedParams(Map.of("act", "nope"));
		MockHttpServletRequest request = jsonRequest(body);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.saasErp(request, response);

		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals("text/html;charset=utf-8", response.getContentType());
		JsonNode written = objectMapper.readTree(response.getContentAsString());
		assertEquals("fail", written.get("result").asText());
		assertEquals("接口不存在", written.get("msg").asText());
		verify(logPort, never()).insertResponseFail(anyLong(), anyString(), anyString());
	}

	@Test
	void badAcReturnsSignErrorJsonWithoutLogInsert() throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("act", "nope");
		body.put("ac", "deadbeefdeadbeefdeadbeefdeadbeef");
		MockHttpServletRequest request = jsonRequest(body);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.saasErp(request, response);

		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
		assertNull(response.getHeader("Cache-Control"));

		JsonNode outer = objectMapper.readTree(response.getContentAsString());
		assertEquals("fail", outer.get("result").asText());
		assertEquals(0, outer.get("code").asInt());
		assertEquals("sign error", outer.get("msg").asText());
		JsonNode info = objectMapper.readTree(outer.get("info").asText());
		assertTrue(info.has("ac"));
		verify(logPort, never()).insertResponseFail(anyLong(), anyString(), anyString());
	}

	@Test
	void nonStringValueAfterValidSignReturns500WithoutFiveFieldBody() throws Exception {
		Map<String, Object> body = signedParams(Map.of("act", "nope", "qty", 42));
		MockHttpServletRequest request = jsonRequest(body);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.saasErp(request, response);

		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertTrue(response.getContentAsString().isEmpty() || !response.getContentAsString().contains("\"result\""));
		verify(logPort, never()).insertResponseFail(anyLong(), anyString(), anyString());
	}

	@Test
	void nullValueAfterValidSignReturns500WithoutFiveFieldBody() throws Exception {
		LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
		fields.put("act", "nope");
		fields.put("optional", null);
		Map<String, Object> body = signedParams(fields);
		assertTrue(verifier.matches(body, TOKEN));
		MockHttpServletRequest request = jsonRequest(body);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.saasErp(request, response);

		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertTrue(response.getContentAsString().isEmpty() || !response.getContentAsString().contains("\"result\""));
		verify(logPort, never()).insertResponseFail(anyLong(), anyString(), anyString());
	}

	@Test
	void validSignWithKnownActReturnsSuccessEnvelopeAndUpdatesLog() throws Exception {
		SaasErpActHandler handler = mock(SaasErpActHandler.class);
		when(handler.act()).thenReturn("update_order_status");
		when(handler.handle(any(), eq(1L)))
				.thenReturn(SaasErpInboundResult.ok("操作成功", Map.of("status", true)));
		when(logPort.insertResponseFail(eq(1L), eq("update_order_status"), anyString()))
				.thenReturn(42L);
		dispatcher = new SaasErpActDispatcher(List.of(handler));
		controller =
				new SaasErpInboundController(
						verifier, resolver, dispatcher, responseWriter, logPort, objectMapper);

		Map<String, Object> body =
				signedParams(Map.of("act", "update_order_status", "order_id", "1001"));
		MockHttpServletRequest request = jsonRequest(body);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.saasErp(request, response);

		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals("text/html;charset=utf-8", response.getContentType());
		String responseBody = response.getContentAsString();
		assertFalse(responseBody.contains("\"data\""));
		JsonNode written = objectMapper.readTree(responseBody);
		assertEquals("success", written.get("result").asText());
		assertEquals(0, written.get("code").asInt());
		assertEquals("操作成功", written.get("msg").asText());
		assertTrue(written.get("info").get("status").asBoolean());

		var order = inOrder(logPort, handler);
		order.verify(logPort).insertResponseFail(eq(1L), eq("update_order_status"), anyString());
		order.verify(handler).handle(any(), eq(1L));

		ArgumentCaptor<String> resultJsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(logPort).updateResult(eq(42L), eq("success"), resultJsonCaptor.capture());
		JsonNode logged = objectMapper.readTree(resultJsonCaptor.getValue());
		assertEquals("success", logged.get("result").asText());
		assertEquals("操作成功", logged.get("msg").asText());
	}

	@Test
	void insertResponseFailThrowsReturns500WithoutCallingHandler() throws Exception {
		SaasErpActHandler handler = mock(SaasErpActHandler.class);
		when(handler.act()).thenReturn("get_orders_info");
		dispatcher = new SaasErpActDispatcher(List.of(handler));
		doThrow(new RuntimeException("db down"))
				.when(logPort)
				.insertResponseFail(eq(1L), eq("get_orders_info"), anyString());
		controller =
				new SaasErpInboundController(
						verifier, resolver, dispatcher, responseWriter, logPort, objectMapper);

		Map<String, Object> body = signedParams(Map.of("act", "get_orders_info", "order_id", "1"));
		MockHttpServletRequest request = jsonRequest(body);
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.saasErp(request, response);

		assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		verify(handler, never()).handle(any(), anyLong());
	}

	private MockHttpServletRequest jsonRequest(Map<String, Object> body) throws Exception {
		byte[] bytes = objectMapper.writeValueAsBytes(body);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/thirdparty/saaserp");
		request.setContentType(MediaType.APPLICATION_JSON_VALUE);
		request.setContent(bytes);
		return request;
	}

	private Map<String, Object> signedParams(Map<String, Object> fields) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>(fields);
		params.put("ac", computeAc(params, TOKEN));
		return params;
	}

	private static String computeAc(Map<String, Object> params, String token) {
		Map<String, Object> copy = new LinkedHashMap<>(params);
		copy.remove("ac");
		List<String> keys = new ArrayList<>(copy.keySet());
		keys.sort(String::compareTo);
		StringBuilder concatenated = new StringBuilder();
		for (String key : keys) {
			Object value = copy.get(key);
			if (value == null) {
				continue;
			}
			if (value instanceof List || value instanceof Map) {
				concatenated.append("Array");
			} else {
				concatenated.append(String.valueOf(value));
			}
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] hash =
					digest.digest((concatenated.toString() + token).trim().getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(String.format(Locale.ROOT, "%02x", b));
			}
			return hex.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
