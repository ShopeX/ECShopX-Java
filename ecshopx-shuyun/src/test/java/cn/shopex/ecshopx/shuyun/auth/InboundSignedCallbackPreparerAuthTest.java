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

@ExtendWith(MockitoExtension.class)
class InboundSignedCallbackPreparerAuthTest {

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
	void prepare_whenSignatureInvalid_doesNotTouchReplayGuard() {
		CompanyShuyunOpenPlatformConfig config = enabledConfig(100L);
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(config);
		when(request.getParameter("appId")).thenReturn("app1");
		when(request.getParameter("sign")).thenReturn("bad");
		when(signatureVerifier.verifyHttpCallback(
						eq("identity-secret"), eq(request), eq("bad"), anyString(), eq(true)))
				.thenReturn(false);

		InboundPrepareResult result =
				preparer.prepare(request, "{\"appId\":\"app1\"}", InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE);

		assertThat(result).isInstanceOf(InboundPrepareResult.Err.class);
		verify(replayGuard, never()).assertFreshAndOnce(anyString(), anyString(), anyInt(), anyInt());
	}

	@Test
	void prepare_whenReplayRejected_returnsError() {
		CompanyShuyunOpenPlatformConfig config = enabledConfig(100L);
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(config);
		when(request.getParameter("appId")).thenReturn("app1");
		when(request.getParameter("sign")).thenReturn("good");
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(signatureVerifier.verifyHttpCallback(
						eq("identity-secret"), eq(request), eq("good"), anyString(), eq(true)))
				.thenReturn(true);
		when(replayGuard.assertFreshAndOnce("good", "1710000000000", 300, 600)).thenReturn(false);

		InboundPrepareResult result =
				preparer.prepare(request, "{\"appId\":\"app1\"}", InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE);

		assertThat(result).isInstanceOf(InboundPrepareResult.Err.class);
		var body = ((InboundPrepareResult.Err) result).response().getBody();
		assertThat(body).containsEntry("code", "40306");
		assertThat(body).containsEntry("msg", "REPLAY_OR_STALE");
	}

	@Test
	void prepare_whenSignAndReplayOk_returnsOk() {
		CompanyShuyunOpenPlatformConfig config = enabledConfig(100L);
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(config);
		when(request.getParameter("appId")).thenReturn("app1");
		when(request.getParameter("sign")).thenReturn("good");
		when(request.getHeader("SY-Request-Time")).thenReturn("1710000000000");
		when(signatureVerifier.verifyHttpCallback(
						eq("identity-secret"), eq(request), eq("good"), anyString(), eq(true)))
				.thenReturn(true);
		when(replayGuard.assertFreshAndOnce("good", "1710000000000", 300, 600)).thenReturn(true);

		InboundPrepareResult result =
				preparer.prepare(request, "{\"appId\":\"app1\"}", InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE);

		assertThat(result).isInstanceOf(InboundPrepareResult.Ok.class);
		assertThat(((InboundPrepareResult.Ok) result).prepared().companyId()).isEqualTo(100L);
	}

	private static CompanyShuyunOpenPlatformConfig enabledConfig(long companyId) {
		CompanyShuyunOpenPlatformConfig config = new CompanyShuyunOpenPlatformConfig();
		config.setCompanyId(companyId);
		config.setIsEnabled(1);
		config.setAppId("app1");
		return config;
	}
}
