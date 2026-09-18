package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MemberRegisterServiceTest {

	@Mock
	private OpenPlatformConfigService openPlatformConfigService;

	@Mock
	private ShuyunOpenGatewayClient gatewayClient;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private MemberEnhanceCardSyncService enhanceCardSyncService;

	private MemberRegisterService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberRegisterService(
						openPlatformConfigService,
						new ShuyunOpenPlatformProperties(),
						gatewayClient,
						enhanceCardSyncService,
						jdbcTemplate);
	}

	@Test
	void registerOfflinePostsExpectedPayload() {
		CompanyShuyunOpenPlatformConfig cfg = new CompanyShuyunOpenPlatformConfig();
		cfg.setAppId("a");
		cfg.setAppSecret("s");
		cfg.setAuthValue("v");
		cfg.setAccessToken("t");
		cfg.setIsEnabled(1);
		cfg.setIsOverDue("0");
		when(openPlatformConfigService.findByCompanyId(1L)).thenReturn(cfg);
		when(gatewayClient.postJson(anyLong(), anyString(), any(), anyString()))
				.thenReturn(JsonNodeFactory.instance.objectNode());
		when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

		assertTrue(service.registerOfflineAfterCreate(1L, 88L, 12L, "13800138000"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
		verify(gatewayClient)
				.postJson(eq(1L), eq(ShuyunOpenPlatformGatewayActions.MEMBER_REGISTER), body.capture(), eq("offline"));
		assertTrue("88".equals(String.valueOf(body.getValue().get("id"))));
		assertTrue("OFFLINE".equals(body.getValue().get("platCode")));
		assertTrue("12-off".equals(body.getValue().get("shopId")));
		assertTrue("13800138000".equals(body.getValue().get("mobile")));
	}
}
