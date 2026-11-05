package cn.shopex.ecshopx.openapi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.openapi.OpenapiDeveloperCredentials;
import cn.shopex.ecshopx.common.openapi.OpenapiDeveloperLookupPort;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiSignSupport;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiRequestParamCollector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** ECX-10869：签名通过后重放拒绝返回 E4002。 */
@ExtendWith(MockitoExtension.class)
class OpenapiAuthInterceptorReplayTest {

	@Mock
	private OpenapiDeveloperLookupPort developerLookupPort;

	@Mock
	private OpenapiRequestParamCollector paramCollector;

	@Mock
	private OpenapiSignedRequestReplayGuard replayGuard;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
	private OpenapiAuthInterceptor interceptor;

	@BeforeEach
	void setUp() throws Exception {
		interceptor =
				new OpenapiAuthInterceptor(
						developerLookupPort, paramCollector, replayGuard, objectMapper);
		ReflectionTestUtils.setField(interceptor, "debug", 0);
		when(request.getRequestURI()).thenReturn("/api/openapi/ecx.member.getMemberInfo");
		lenient()
				.when(response.getOutputStream())
				.thenReturn(
						new ServletOutputStream() {
							@Override
							public boolean isReady() {
								return true;
							}

							@Override
							public void setWriteListener(WriteListener writeListener) {}

							@Override
							public void write(int b) {
								responseBody.write(b);
							}
						});
	}

	@Test
	void preHandle_whenReplayDetected_rejectsWithE4002() throws Exception {
		long nowSec = System.currentTimeMillis() / 1000L;
		String timestamp = String.valueOf(nowSec);
		String appKey = "demo-key";
		String secret = "demo-secret";
		Map<String, Object> params = baseParams(appKey, timestamp);
		String sign = OpenapiSignSupport.genSign(OpenapiRequestParamCollector.paramsForSign(params), secret);
		params.put("sign", sign);

		when(paramCollector.collect(request)).thenReturn(params);
		when(developerLookupPort.findByAppKey(appKey))
				.thenReturn(Optional.of(new OpenapiDeveloperCredentials(9L, secret)));
		when(replayGuard.assertOnce(eq(appKey), eq(sign), eq(timestamp), eq(600))).thenReturn(false);

		boolean ok = interceptor.preHandle(request, response, new Object());

		assertThat(ok).isFalse();
		JsonNode body = objectMapper.readTree(responseBody.toByteArray());
		assertThat(body.path("code").asText()).isEqualTo(OpenapiErrorCode.VALIDATION_TIMESTAMP_ERROR);
		assertThat(body.path("message").asText()).contains("重复");
	}

	@Test
	void preHandle_whenFirstSighting_allows() throws Exception {
		long nowSec = System.currentTimeMillis() / 1000L;
		String timestamp = String.valueOf(nowSec);
		String appKey = "demo-key";
		String secret = "demo-secret";
		Map<String, Object> params = baseParams(appKey, timestamp);
		String sign = OpenapiSignSupport.genSign(OpenapiRequestParamCollector.paramsForSign(params), secret);
		params.put("sign", sign);

		when(paramCollector.collect(request)).thenReturn(params);
		when(developerLookupPort.findByAppKey(appKey))
				.thenReturn(Optional.of(new OpenapiDeveloperCredentials(9L, secret)));
		when(replayGuard.assertOnce(eq(appKey), eq(sign), eq(timestamp), eq(600))).thenReturn(true);

		boolean ok = interceptor.preHandle(request, response, new Object());

		assertThat(ok).isTrue();
		verify(response, never()).getOutputStream();
	}

	private static Map<String, Object> baseParams(String appKey, String timestamp) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("version", "2.0");
		params.put("timestamp", timestamp);
		params.put("app_key", appKey);
		params.put("method", "ecx.member.getMemberInfo");
		return params;
	}
}
