package cn.shopex.ecshopx.shuyun.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class CallbackSignatureVerifierTest {

	private final CallbackSignatureVerifier verifier = new CallbackSignatureVerifier();

	@Test
	void verifyHttpCallback_withSyRequestTime() {
		String secret = "identity-secret";
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameterMap()).thenReturn(Map.of("appId", new String[] {"app1"}));
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(request.getHeader("Sy-Request-Time")).thenReturn(null);
		Map<String, String> params = new LinkedHashMap<>();
		params.put("SY-Request-Time", "1710000000000");
		params.put("appId", "app1");
		String sign = verifier.signParams(secret, params);
		assertTrue(verifier.verifyHttpCallback(secret, request, sign, "", false));
		assertFalse(verifier.verifyHttpCallback(secret, request, "deadbeef", "", false));
	}

	@Test
	void verifyHttpCallback_whenIncludeBody_rejectsTamperedBody() {
		String secret = "identity-secret";
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameterMap()).thenReturn(Map.of("appId", new String[] {"app1"}));
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(request.getHeader("Sy-Request-Time")).thenReturn(null);

		String body = "{\"appId\":\"app1\",\"grade\":\"VIP\"}";
		String sign = signWithBody(secret, Map.of("appId", "app1", "SY-Request-Time", "1710000000000"), body);

		assertTrue(verifier.verifyHttpCallback(secret, request, sign, body, true));
		assertFalse(
				verifier.verifyHttpCallback(
						secret, request, sign, "{\"appId\":\"app1\",\"grade\":\"ATTACK\"}", true));
	}

	@Test
	void verifyHttpCallback_whenIncludeBodyFalse_ignoresBodyTamper() {
		String secret = "identity-secret";
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameterMap()).thenReturn(Map.of("appId", new String[] {"app1"}));
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(request.getHeader("Sy-Request-Time")).thenReturn(null);

		Map<String, String> params = new TreeMap<>();
		params.put("SY-Request-Time", "1710000000000");
		params.put("appId", "app1");
		String sign = verifier.signParams(secret, params);

		assertTrue(
				verifier.verifyHttpCallback(
						secret, request, sign, "{\"grade\":\"VIP\"}", false));
		assertTrue(
				verifier.verifyHttpCallback(
						secret, request, sign, "{\"grade\":\"ATTACK\"}", false));
	}

	@Test
	void gatewaySign_sortedConcat() {
		String sign =
				verifier.signParams(
						"sec",
						Map.of("Gateway-Request-Time", "1", "a", "2"));
		// TreeMap ASCII: Gateway-Request-Time before a
		assertEquals(CallbackSignatureVerifier.md5Hex("secGateway-Request-Time1a2sec"), sign);
	}

	@Test
	void emptyParams_fail() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameterMap()).thenReturn(Collections.emptyMap());
		when(request.getHeader("SY-Request-Time")).thenReturn(null);
		when(request.getHeader("Sy-Request-Time")).thenReturn(null);
		when(request.getParameter("callBackTime")).thenReturn(null);
		assertFalse(verifier.verifyHttpCallback("s", request, "abc", "", false));
	}

	private static String signWithBody(String secret, Map<String, String> params, String rawBody) {
		Map<String, String> sorted = new TreeMap<>(params);
		StringBuilder sb = new StringBuilder(secret);
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			sb.append(e.getKey()).append(e.getValue() == null ? "" : e.getValue());
		}
		sb.append(rawBody == null ? "" : rawBody);
		sb.append(secret);
		return CallbackSignatureVerifier.md5Hex(sb.toString());
	}
}
