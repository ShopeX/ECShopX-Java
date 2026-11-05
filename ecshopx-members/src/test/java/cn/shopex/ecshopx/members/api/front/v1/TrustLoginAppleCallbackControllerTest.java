package cn.shopex.ecshopx.members.api.front.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.trustlogin.SocialTrustLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TrustLoginAppleCallbackControllerTest {

	@Mock
	private SocialTrustLoginService socialTrustLoginService;

	@Mock
	private HttpServletRequest request;

	private TrustLoginAppleCallbackController controller;

	@BeforeEach
	void setUp() {
		controller = new TrustLoginAppleCallbackController(socialTrustLoginService);
	}

	@Test
	void callback_forgedHost_doesNotRedirectCodeToEvil() {
		when(socialTrustLoginService.resolveAppleCallbackH5Host(eq("https://evil.example"), eq("forged")))
				.thenReturn("");

		ResponseEntity<String> resp =
				controller.appleOAuthCallback(
						request, "https://evil.example", "forged", "auth-code", null, null);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resp.getHeaders().getLocation()).isNull();
	}

	@Test
	void callback_validBinding_redirectsCodeToAllowedLanding() {
		when(socialTrustLoginService.resolveAppleCallbackH5Host(isNull(), eq("ok-state")))
				.thenReturn("https://h5.example.com");
		when(socialTrustLoginService.buildAppleH5LandingUrl(
						eq("https://h5.example.com"), eq("auth-code"), any()))
				.thenReturn("https://h5.example.com/subpages/auth/auth-social-loading?code=auth-code");

		ResponseEntity<String> resp =
				controller.appleOAuthCallback(request, null, "ok-state", "auth-code", null, null);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(resp.getHeaders().getLocation())
				.hasToString("https://h5.example.com/subpages/auth/auth-social-loading?code=auth-code");
		verify(socialTrustLoginService)
				.buildAppleH5LandingUrl(eq("https://h5.example.com"), eq("auth-code"), any());
	}
}
