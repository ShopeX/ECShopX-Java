package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.mapper.CompanyShuyunOpenPlatformConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenCallbackServiceTest {

	@Mock
	private CompanyShuyunOpenPlatformConfigMapper configMapper;

	@Mock
	private OpenPlatformConfigService openPlatformConfigService;

	private TokenCallbackService service;

	@BeforeEach
	void setUp() {
		ShuyunOpenPlatformProperties properties = new ShuyunOpenPlatformProperties();
		service =
				new TokenCallbackService(
						configMapper, openPlatformConfigService, properties, new ObjectMapper());
	}

	@Test
	void invalidBody() {
		Map<String, Object> r = service.handle("{");
		assertEquals(400, r.get("code"));
		assertEquals("INVALID_BODY", r.get("msg"));
	}

	@Test
	void emptyArraySuccess() {
		Map<String, Object> r = service.handle("[]");
		assertEquals(200, r.get("code"));
		assertEquals("SUCCESS", r.get("msg"));
	}

	@Test
	void updatesTokenByAuthValue() {
		CompanyShuyunOpenPlatformConfig row = new CompanyShuyunOpenPlatformConfig();
		row.setId(3L);
		row.setCompanyId(9L);
		row.setAppId("app1");
		row.setAuthValue("av1");
		when(openPlatformConfigService.findByAppId("app1")).thenReturn(row);
		when(openPlatformConfigService.findByAuthValue("av1")).thenReturn(row);

		String body =
				"[{\"appId\":\"app1\",\"authValue\":\"av1\",\"accessToken\":\"tok\",\"isOverDue\":\"0\"}]";
		Map<String, Object> r = service.handle(body);
		assertEquals(200, r.get("code"));
		assertEquals("tok", row.getAccessToken());
		assertEquals("0", row.getIsOverDue());
		verify(configMapper).updateById(org.mockito.ArgumentMatchers.<CompanyShuyunOpenPlatformConfig>any());
	}

	@Test
	void noAppConfig() {
		when(openPlatformConfigService.findByAppId("appx")).thenReturn(null);
		String body =
				"[{\"appId\":\"appx\",\"authValue\":\"av1\",\"accessToken\":\"tok\",\"isOverDue\":\"0\"}]";
		Map<String, Object> r = service.handle(body);
		assertEquals(403, r.get("code"));
		assertEquals("NO_APP_CONFIG", r.get("msg"));
	}
}
