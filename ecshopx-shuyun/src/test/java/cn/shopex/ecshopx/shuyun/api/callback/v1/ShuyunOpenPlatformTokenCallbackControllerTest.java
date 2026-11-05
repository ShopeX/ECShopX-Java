package cn.shopex.ecshopx.shuyun.api.callback.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.shuyun.auth.InboundPrepareResult;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedPrepareMode;
import cn.shopex.ecshopx.shuyun.auth.PreparedInboundCallback;
import cn.shopex.ecshopx.shuyun.service.openplatform.OpenPlatformConfigService;
import cn.shopex.ecshopx.shuyun.service.openplatform.TokenCallbackService;
import cn.shopex.ecshopx.shuyun.service.openplatform.TrafficAuditWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.DelegatingServletInputStream;

@ExtendWith(MockitoExtension.class)
class ShuyunOpenPlatformTokenCallbackControllerTest {

	@Mock
	private TokenCallbackService tokenCallbackService;

	@Mock
	private TrafficAuditWriter trafficAuditWriter;

	@Mock
	private OpenPlatformConfigService openPlatformConfigService;

	@Mock
	private InboundSignedCallbackPreparer preparer;

	@Mock
	private HttpServletRequest request;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ShuyunOpenPlatformTokenCallbackController controller;

	@BeforeEach
	void setUp() {
		controller =
				new ShuyunOpenPlatformTokenCallbackController(
						tokenCallbackService,
						trafficAuditWriter,
						openPlatformConfigService,
						objectMapper,
						preparer);
	}

	@Test
	void token_invalidSign_doesNotCallHandle() throws Exception {
		String body = "[{\"appId\":\"app1\",\"authValue\":\"av1\",\"accessToken\":\"tok\"}]";
		stubBody(body);
		Map<String, Object> errBody = new LinkedHashMap<>();
		errBody.put("code", 403);
		errBody.put("msg", "INVALID_SIGN");
		errBody.put("data", "");
		when(preparer.prepare(eq(request), eq(body), eq(InboundSignedPrepareMode.TOKEN)))
				.thenReturn(new InboundPrepareResult.Err(ResponseEntity.status(403).body(errBody)));

		ResponseEntity<Map<String, Object>> resp = controller.token(request);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(resp.getBody()).containsEntry("msg", "INVALID_SIGN");
		verify(tokenCallbackService, never()).handle(anyString());
	}

	@Test
	void token_validSign_callsHandle() throws Exception {
		String body = "[{\"appId\":\"app1\",\"authValue\":\"av1\",\"accessToken\":\"tok\"}]";
		stubBody(body);
		when(preparer.prepare(eq(request), eq(body), eq(InboundSignedPrepareMode.TOKEN)))
				.thenReturn(
						new InboundPrepareResult.Ok(
								new PreparedInboundCallback(9L, Map.of("appId", "app1"))));
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("code", 200);
		ok.put("msg", "SUCCESS");
		ok.put("data", "");
		when(tokenCallbackService.handle(body)).thenReturn(ok);
		when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());

		ResponseEntity<Map<String, Object>> resp = controller.token(request);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resp.getBody()).containsEntry("code", 200);
		verify(tokenCallbackService).handle(body);
	}

	private void stubBody(String body) throws Exception {
		when(request.getInputStream())
				.thenReturn(
						new DelegatingServletInputStream(
								new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))));
	}
}
