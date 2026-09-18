package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
class TradeSyncDispatchServiceTest {

	@Mock
	private OpenPlatformConfigService openPlatformConfigService;

	@Mock
	private OpenPlatformDispatchDedupeService dedupeService;

	@Mock
	private TradeSyncService tradeSyncService;

	@Mock
	private JdbcTemplate jdbcTemplate;

	private TradeSyncDispatchService service;

	@BeforeEach
	void setUp() {
		service = new TradeSyncDispatchService(openPlatformConfigService, dedupeService, tradeSyncService, jdbcTemplate);
	}

	@Test
	void skipsWhenNotEligible() {
		when(openPlatformConfigService.findByCompanyId(1L)).thenReturn(null);
		service.dispatchPaySuccess(1L, "O1");
		verify(tradeSyncService, never()).syncOrder(anyLong(), anyString());
	}

	@Test
	void dispatchesWhenEligibleAndDedupeAcquired() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(9L)).thenReturn(cfg);
		when(dedupeService.tryAcquire(anyString(), eq(600))).thenReturn(true);
		service.dispatchPaySuccess(9L, "OID");
		verify(tradeSyncService).syncOrder(9L, "OID");
	}

	@Test
	void skipsWhenDeduped() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(9L)).thenReturn(cfg);
		when(dedupeService.tryAcquire(anyString(), anyInt())).thenReturn(false);
		service.dispatchPaySuccess(9L, "OID");
		verify(tradeSyncService, never()).syncOrder(eq(9L), anyString());
	}

	@Test
	void cancelRequiresPayed() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(9L)).thenReturn(cfg);
		when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<ResultSetExtractor<String>>any(), eq(9L), eq("OID")))
				.thenReturn("NOTPAY");
		service.dispatchOrderCancel(9L, "OID");
		verify(tradeSyncService, never()).syncOrder(anyLong(), anyString());
		verify(dedupeService, never()).tryAcquire(anyString(), anyInt());
	}

	@Test
	void cancelWhenPayedDispatches() {
		CompanyShuyunOpenPlatformConfig cfg = eligibleConfig();
		when(openPlatformConfigService.findByCompanyId(9L)).thenReturn(cfg);
		when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<ResultSetExtractor<String>>any(), eq(9L), eq("OID")))
				.thenReturn("PAYED");
		when(dedupeService.tryAcquire(anyString(), eq(30))).thenReturn(true);
		service.dispatchOrderCancel(9L, "OID");
		verify(tradeSyncService).syncOrder(9L, "OID");
	}

	@Test
	void orderTradeSourceResolver() {
		OrderTradeSourceResolver r =
				new OrderTradeSourceResolver(new cn.shopex.ecshopx.shuyun.config.ShuyunOpenPlatformProperties());
		assertEquals("11", r.resolve(1, "o", "normal"));
		assertNull(r.resolve(1, "o", "unknown_class"));
	}

	private static CompanyShuyunOpenPlatformConfig eligibleConfig() {
		CompanyShuyunOpenPlatformConfig cfg = new CompanyShuyunOpenPlatformConfig();
		cfg.setCompanyId(9L);
		cfg.setAppId("app");
		cfg.setAppSecret("secret");
		cfg.setAuthValue("auth");
		cfg.setAccessToken("token");
		cfg.setIsEnabled(1);
		cfg.setIsOverDue("0");
		return cfg;
	}
}
