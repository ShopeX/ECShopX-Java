package cn.shopex.ecshopx.shuyun.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOfflineBenefitMapper;
import cn.shopex.ecshopx.shuyun.service.openplatform.OpenPlatformConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InboundSignedCallbackPreparerTokenModeTest {

	@Mock
	private OpenPlatformConfigService openPlatformConfigService;

	@Mock
	private ShuyunOfflineBenefitMapper offlineBenefitMapper;

	@Mock
	private CallbackSignatureVerifier signatureVerifier;

	@Mock
	private InboundSignedCallbackReplayGuard replayGuard;

	@Mock
	private HttpServletRequest request;

	private ShuyunOpenPlatformProperties properties;
	private InboundSignedCallbackPreparer preparer;

	@BeforeEach
	void setUp() {
		properties = new ShuyunOpenPlatformProperties();
		properties.setCallbackIdentitySecret("identity-secret");
		properties.setCallbackSignIncludeBody(true);
		properties.setCallbackReplaySkewSeconds(300);
		properties.setCallbackReplayTtlSeconds(600);
		preparer =
				new InboundSignedCallbackPreparer(
						openPlatformConfigService,
						offlineBenefitMapper,
						signatureVerifier,
						replayGuard,
						properties,
						new ObjectMapper());
	}

	@Test
	void prepare_token_missingSecret_returns403() {
		properties.setCallbackIdentitySecret("");
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(enabledConfig(9L));
		when(request.getParameter("appId")).thenReturn(null);

		InboundPrepareResult result =
				preparer.prepare(
						request,
						"[{\"appId\":\"app1\",\"authValue\":\"av1\"}]",
						InboundSignedPrepareMode.TOKEN);

		assertThat(result).isInstanceOf(InboundPrepareResult.Err.class);
		var err = (InboundPrepareResult.Err) result;
		assertThat(err.response().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(err.response().getBody())
				.containsEntry("code", 403)
				.containsEntry("msg", "CALLBACK_IDENTITY_SECRET_NOT_CONFIGURED");
		verify(signatureVerifier, never())
				.verifyHttpCallback(anyString(), eq(request), anyString(), anyString(), eq(true));
	}

	@Test
	void prepare_token_invalidSign_returns403AndSkipsReplay() {
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(enabledConfig(9L));
		when(request.getParameter("appId")).thenReturn(null);
		when(request.getParameter("sign")).thenReturn("bad");
		when(signatureVerifier.verifyHttpCallback(
						eq("identity-secret"), eq(request), eq("bad"), anyString(), eq(true)))
				.thenReturn(false);

		InboundPrepareResult result =
				preparer.prepare(
						request,
						"[{\"appId\":\"app1\",\"authValue\":\"av1\"}]",
						InboundSignedPrepareMode.TOKEN);

		assertThat(result).isInstanceOf(InboundPrepareResult.Err.class);
		var err = (InboundPrepareResult.Err) result;
		assertThat(err.response().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(err.response().getBody()).containsEntry("msg", "INVALID_SIGN");
		verify(replayGuard, never()).assertFreshAndOnce(anyString(), anyString(), anyInt(), anyInt());
	}

	@Test
	void prepare_token_arrayBody_validSign_returnsOk() {
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(enabledConfig(9L));
		when(request.getParameter("appId")).thenReturn(null);
		when(request.getParameter("sign")).thenReturn("good");
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(signatureVerifier.verifyHttpCallback(
						eq("identity-secret"), eq(request), eq("good"), anyString(), eq(true)))
				.thenReturn(true);
		when(replayGuard.assertFreshAndOnce("good", "1710000000000", 300, 600)).thenReturn(true);

		InboundPrepareResult result =
				preparer.prepare(
						request,
						"[{\"appId\":\"app1\",\"authValue\":\"av1\",\"accessToken\":\"tok\"}]",
						InboundSignedPrepareMode.TOKEN);

		assertThat(result).isInstanceOf(InboundPrepareResult.Ok.class);
		assertThat(((InboundPrepareResult.Ok) result).prepared().companyId()).isEqualTo(9L);
	}

	@Test
	void prepare_token_emptyArray_validSign_returnsOkWithoutApp() {
		when(request.getParameter("appId")).thenReturn(null);
		when(request.getParameter("sign")).thenReturn("good");
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(signatureVerifier.verifyHttpCallback(
						eq("identity-secret"), eq(request), eq("good"), anyString(), eq(true)))
				.thenReturn(true);
		when(replayGuard.assertFreshAndOnce("good", "1710000000000", 300, 600)).thenReturn(true);

		InboundPrepareResult result =
				preparer.prepare(request, "[]", InboundSignedPrepareMode.TOKEN);

		assertThat(result).isInstanceOf(InboundPrepareResult.Ok.class);
		assertThat(((InboundPrepareResult.Ok) result).prepared().companyId()).isEqualTo(0L);
	}

	private static CompanyShuyunOpenPlatformConfig enabledConfig(long companyId) {
		CompanyShuyunOpenPlatformConfig config = new CompanyShuyunOpenPlatformConfig();
		config.setCompanyId(companyId);
		config.setIsEnabled(1);
		config.setAppId("app1");
		return config;
	}
}
