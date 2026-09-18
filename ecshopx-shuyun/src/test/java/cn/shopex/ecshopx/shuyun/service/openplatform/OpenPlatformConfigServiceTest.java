package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.mapper.CompanyShuyunOpenPlatformConfigMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenPlatformConfigServiceTest {

	@Mock
	private CompanyShuyunOpenPlatformConfigMapper configMapper;

	private ShuyunOpenPlatformProperties properties;
	private OpenPlatformConfigService service;

	@BeforeEach
	void setUp() {
		properties = new ShuyunOpenPlatformProperties();
		properties.setAuthValue("auth-from-env");
		service = new OpenPlatformConfigService(configMapper, properties);
	}

	@Test
	void maskSecret_rules() {
		assertEquals("", OpenPlatformConfigService.maskSecret(null));
		assertEquals("****", OpenPlatformConfigService.maskSecret("ab"));
		assertEquals("****cdef", OpenPlatformConfigService.maskSecret("abcdefghcdef"));
	}

	@Test
	void getAdminView_noRow_returnsDefaultShell() {
		when(configMapper.selectOne(any())).thenReturn(null);
		Map<String, Object> view = service.getAdminView(9L);
		assertEquals(9L, view.get("company_id"));
		assertEquals("OFFLINE", view.get("plat_code"));
		assertEquals("", view.get("app_id"));
		assertEquals("", view.get("app_secret_masked"));
		assertEquals("", view.get("access_token"));
		assertEquals(false, view.get("is_enabled"));
		assertEquals("", view.get("is_over_due"));
	}

	@Test
	void bootstrapCreatesRowWithOfflinePlatCode() {
		when(configMapper.selectOne(any())).thenReturn(null);
		service.saveFromAdmin(9L, Map.of("app_id", "app1", "app_secret", "secret12345"));
		ArgumentCaptor<CompanyShuyunOpenPlatformConfig> cap =
				ArgumentCaptor.forClass(CompanyShuyunOpenPlatformConfig.class);
		verify(configMapper).insert(cap.capture());
		CompanyShuyunOpenPlatformConfig row = cap.getValue();
		assertEquals(9L, row.getCompanyId());
		assertEquals("OFFLINE", row.getPlatCode());
		assertEquals(0, row.getIsEnabled());
		assertEquals("app1", row.getAppId());
		assertEquals("auth-from-env", row.getAuthValue());
	}

	@Test
	void enableWithoutToken_throws() {
		CompanyShuyunOpenPlatformConfig row = new CompanyShuyunOpenPlatformConfig();
		row.setId(1L);
		row.setCompanyId(9L);
		row.setAppId("a");
		row.setAppSecret("s");
		row.setIsEnabled(0);
		when(configMapper.selectOne(any())).thenReturn(row);
		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() -> service.saveFromAdmin(9L, Map.of("is_enabled", true)));
		assertEquals("须先获取有效 access_token 后再开启数云同步。", ex.getMessage());
		verify(configMapper, never()).updateById(org.mockito.ArgumentMatchers.<CompanyShuyunOpenPlatformConfig>any());
	}
}
