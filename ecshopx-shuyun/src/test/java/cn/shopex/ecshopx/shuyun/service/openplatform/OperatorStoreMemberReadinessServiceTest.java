package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OperatorStoreMemberReadinessServiceTest {

	@Mock
	private OpenPlatformConfigService openPlatformConfigService;

	@Mock
	private MemberRegisterService memberRegisterService;

	@Mock
	private JdbcTemplate jdbcTemplate;

	private OperatorStoreMemberReadinessService service;

	@BeforeEach
	void setUp() {
		service =
				new OperatorStoreMemberReadinessService(
						openPlatformConfigService, memberRegisterService, jdbcTemplate);
	}

	@Test
	void skipsWhenOpenPlatformDisabled() {
		when(openPlatformConfigService.findByCompanyId(1L)).thenReturn(null);

		Map<String, Object> result = service.ensureOfflineMemberAtStore(1L, 10L, 88L);

		assertFalse((Boolean) result.get("synced"));
		assertTrue((Boolean) result.get("skipped"));
		verify(memberRegisterService, never())
				.registerOfflineAfterCreate(anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void skipsWhenAlreadyRegisteredWithOpenPlatform() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(1L)).thenReturn(cfg);
		when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(88L)))
				.thenReturn(
						List.of(
								Map.of(
										"user_id",
										88,
										"company_id",
										1L,
										"mobile",
										"13800138000",
										"offline_reg_distributor",
										5)));

		Map<String, Object> result = service.ensureOfflineMemberAtStore(1L, 10L, 88L);

		assertTrue((Boolean) result.get("skipped"));
		verify(memberRegisterService, never())
				.registerOfflineAfterCreate(anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void syncsWhenEligibleMemberNeedsRegister() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(1L)).thenReturn(cfg);
		when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(88L)))
				.thenReturn(
						List.of(
								Map.of(
										"user_id",
										88,
										"company_id",
										1L,
										"mobile",
										"13800138000",
										"offline_reg_distributor",
										0,
										"shuyun_open_online_wxapp_sync_at",
										0)));
		when(memberRegisterService.registerOfflineAfterCreate(1L, 88L, 10L, "13800138000"))
				.thenReturn(true);

		Map<String, Object> result = service.ensureOfflineMemberAtStore(1L, 10L, 88L);

		assertTrue((Boolean) result.get("synced"));
		assertFalse((Boolean) result.get("skipped"));
	}

	@Test
	void throwsWhenMemberMissing() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(1L)).thenReturn(cfg);
		when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(88L))).thenReturn(List.of());

		assertThrows(ResourceException.class, () -> service.ensureOfflineMemberAtStore(1L, 10L, 88L));
	}

	@Test
	void throwsWhenUserIdMissing() {
		assertThrows(ResourceException.class, () -> service.ensureOfflineMemberAtStore(1L, 10L, 0L));
	}

	private static CompanyShuyunOpenPlatformConfig eligibleConfig() {
		CompanyShuyunOpenPlatformConfig cfg = new CompanyShuyunOpenPlatformConfig();
		cfg.setIsEnabled(1);
		cfg.setAppId("app");
		cfg.setAppSecret("secret");
		cfg.setAuthValue("auth");
		cfg.setAccessToken("token");
		cfg.setIsOverDue("0");
		return cfg;
	}
}
