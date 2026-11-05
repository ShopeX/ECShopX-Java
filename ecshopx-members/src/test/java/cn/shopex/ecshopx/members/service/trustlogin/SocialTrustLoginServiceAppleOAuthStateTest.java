package cn.shopex.ecshopx.members.service.trustlogin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SocialTrustLoginServiceAppleOAuthStateTest {

	private static final String ALLOWED = "https://h5.example.com";

	@Mock
	private StringRedisTemplate sharedStringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private SocialTrustLoginService service;

	@BeforeEach
	void setUp() {
		service = new SocialTrustLoginService(sharedStringRedisTemplate, new ObjectMapper());
		ReflectionTestUtils.setField(service, "commonH5BaseUrl", ALLOWED);
		lenient().when(sharedStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	void resolveH5Host_evilOrigin_fallsBackToConfiguredBase() {
		assertThat(service.resolveH5Host("https://evil.example")).isEqualTo(ALLOWED);
	}

	@Test
	void resolveH5Host_matchingOrigin_returnsConfiguredBase() {
		assertThat(service.resolveH5Host(ALLOWED + "/")).isEqualTo(ALLOWED);
	}

	@Test
	void encodeAppleOAuthState_isOpaque_notEmbeddedHostJson() {
		String state = service.encodeAppleOAuthState(ALLOWED);
		assertThat(state).isNotBlank();
		byte[] raw = Base64.getUrlDecoder().decode(padUrl(state));
		assertThat(raw).hasSize(32);
		assertThat(new String(raw, StandardCharsets.ISO_8859_1)).doesNotContain("h5_host");
	}

	@Test
	void rememberAppleOAuthH5Host_rejectsNonAllowedHost() {
		service.rememberAppleOAuthH5Host("state-1", "https://evil.example");
		verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
	}

	@Test
	void rememberAndResolve_usesRedisOnly_ignoresQueryAndForgedState() {
		String state = service.encodeAppleOAuthState(ALLOWED);
		ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
		service.rememberAppleOAuthH5Host(state, ALLOWED);
		verify(valueOperations).set(keyCap.capture(), eq(ALLOWED), any(Duration.class));

		lenient().when(valueOperations.getAndDelete(anyString())).thenReturn(null);
		when(valueOperations.getAndDelete(keyCap.getValue())).thenReturn(ALLOWED);

		String forged =
				Base64.getUrlEncoder()
						.withoutPadding()
						.encodeToString("{\"h5_host\":\"https://evil.example\"}".getBytes(StandardCharsets.UTF_8));

		assertThat(service.resolveAppleCallbackH5Host("https://evil.example", forged)).isEmpty();
		assertThat(service.resolveAppleCallbackH5Host("https://evil.example", state)).isEqualTo(ALLOWED);
	}

	@Test
	void getAuthorizeUrl_apple_bindsAllowedHostToOpaqueState() {
		Map<String, Object> cfg = Map.of("app_id", "apple.client");
		String url =
				service.getAuthorizeUrl(
						"apple", cfg, "https://api.example.com/callback", "https://evil.example");

		assertThat(url).contains("state=");
		assertThat(url).doesNotContain("evil.example");
		verify(valueOperations).set(anyString(), eq(ALLOWED), any(Duration.class));
	}

	private static String padUrl(String state) {
		int p = state.length() % 4;
		if (p == 0) {
			return state;
		}
		return state + "====".substring(p);
	}
}
