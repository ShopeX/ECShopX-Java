package cn.shopex.ecshopx.thirdparty.service.prism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrismOAuthServicePasswordGrantTest {

	@Mock
	private PrismCoreHttpClient prismCoreHttpClient;

	private PrismOAuthService service;

	@BeforeEach
	void setUp() {
		service = new PrismOAuthService(prismCoreHttpClient, new ObjectMapper());
	}

	@Test
	void exchangePasswordGrant_retriesThenExchangesCode() {
		when(prismCoreHttpClient.isConfigured()).thenReturn(true);
		when(prismCoreHttpClient.postSignedForm(eq("/oauth/token"), any()))
				.thenAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							LinkedHashMap<String, Object> form = inv.getArgument(1);
							if ("passwordv2".equals(form.get("grant_type"))) {
								return "{\"code\":\"abc-code\"}";
							}
							return "{\"access_token\":\"at\",\"expires_in\":3600,\"refresh_token\":\"rt\","
									+ "\"refresh_expires\":7200,\"data\":{\"passport_uid\":\"pu\",\"eid\":\"e1\","
									+ "\"shopexid\":\"user@shopex.test\"}}";
						});

		PrismOAuthTokenResult token = service.exchangePasswordGrant("u", "p");
		assertThat(token.accessToken()).isEqualTo("at");
		assertThat(token.data()).containsEntry("passport_uid", "pu").containsEntry("shopexid", "user@shopex.test");
	}

	@Test
	void exchangePasswordGrant_throwsAfterThreeErrors() {
		when(prismCoreHttpClient.isConfigured()).thenReturn(true);
		when(prismCoreHttpClient.postSignedForm(eq("/oauth/token"), any()))
				.thenReturn("{\"error\":\"invalid_grant\"}");

		assertThatThrownBy(() -> service.exchangePasswordGrant("u", "p"))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("用户名或者密码不正确");
	}
}
