package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;

class SaasErpInboundResponseWriterTest {

	private static final String CACHE_CONTROL = "no-cache,no-store , must-revalidate";

	private OmsQueueLogWritePort logPort;
	private SaasErpInboundResponseWriter writer;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		logPort = Mockito.mock(OmsQueueLogWritePort.class);
		objectMapper = new ObjectMapper();
		writer = new SaasErpInboundResponseWriter(logPort, objectMapper);
	}

	@Test
	void writeSetsHeadersBodyAndUpdatesLog() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		long before = Instant.now().getEpochSecond();

		writer.write(response, SaasErpInboundResult.ok("操作成功", null), 15L);

		assertEquals(200, response.getStatus());
		assertEquals("text/html;charset=utf-8", response.getContentType());
		assertEquals(CACHE_CONTROL, response.getHeader("Cache-Control"));
		String expires = response.getHeader("Expires");
		assertNotNull(expires);
		ZonedDateTime expiresTime =
				ZonedDateTime.parse(expires, DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC));
		long secondsAhead = expiresTime.toEpochSecond() - Instant.now().getEpochSecond();
		assertTrue(secondsAhead >= 15 && secondsAhead <= 25, "Expires should be ~20s ahead, was " + secondsAhead);

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertEquals("success", body.get("result").asText());
		assertEquals(0, body.get("code").asInt());
		assertTrue(body.get("shopex_time").asLong() >= before);
		assertEquals("操作成功", body.get("msg").asText());
		assertTrue(body.get("info").isNull());

		ArgumentCaptor<String> resultJsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(logPort).updateResult(eq(15L), eq("success"), resultJsonCaptor.capture());
		JsonNode logged = objectMapper.readTree(resultJsonCaptor.getValue());
		assertEquals("success", logged.get("result").asText());
		assertEquals(0, logged.get("code").asInt());
	}

	@Test
	void writeSkipsLogUpdateWhenLogIdZero() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		writer.write(response, SaasErpInboundResult.fail("接口不存在"), 0L);

		assertEquals(200, response.getStatus());
		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertEquals("fail", body.get("result").asText());
		assertEquals("接口不存在", body.get("msg").asText());
		verify(logPort, never()).updateResult(anyLong(), anyString(), anyString());
	}

	@Test
	void writeStillReturnsBodyWhenUpdateResultThrows() throws Exception {
		doThrow(new RuntimeException("db down")).when(logPort).updateResult(anyLong(), eq("success"), anyString());
		MockHttpServletResponse response = new MockHttpServletResponse();

		writer.write(response, SaasErpInboundResult.ok("操作成功", null), 15L);

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertEquals("success", body.get("result").asText());
		assertEquals("操作成功", body.get("msg").asText());
		assertTrue(body.get("info").isNull());
	}
}
