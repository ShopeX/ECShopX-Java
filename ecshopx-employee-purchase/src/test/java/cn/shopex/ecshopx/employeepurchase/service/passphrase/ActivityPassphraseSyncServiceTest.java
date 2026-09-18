package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityPassphraseSyncServiceTest {

	@Mock private ActivitiesMapper activitiesMapper;
	@Mock private ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;
	@Mock private ActivityPassphraseService activityPassphraseService;
	@Mock private PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService;

	private ActivityPassphraseSyncService service;

	@BeforeEach
	void setUp() {
		service =
				new ActivityPassphraseSyncService(
						activitiesMapper,
						activityPassphraseEnterpriseMapper,
						activityPassphraseService,
						passphraseParticipateQuotaRedisService);
	}

	@Test
	@DisplayName("QA-09: sync=none 不改口令表")
	void updateSyncNone_doesNotTouchPassphraseTable() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("name", "only-name-change");

		service.applyOnUpdate(1L, 900001L, Set.of(900101L), params);

		verify(activityPassphraseEnterpriseMapper, never()).delete(any());
		verify(activityPassphraseEnterpriseMapper, never()).insert(any(ActivityPassphraseEnterprise.class));
	}

	@Test
	@DisplayName("QA-05: resolveUpdateSyncMode 关口令 → clear")
	void resolveSyncMode_clearWhenDisabled() {
		Map<String, Object> params = Map.of("is_passphrase_enabled", false);
		assertEquals(
				ActivityPassphraseSyncService.PassphraseSyncMode.CLEAR,
				service.resolveUpdateSyncMode(params));
	}

	@Test
	@DisplayName("resolveUpdateSyncMode: clear 优先于 replace")
	void resolveSyncMode_clearPriorityOverReplace() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("is_passphrase_enabled", 0);
		params.put("passphrase_enterprises", List.of());

		assertEquals(
				ActivityPassphraseSyncService.PassphraseSyncMode.CLEAR,
				service.resolveUpdateSyncMode(params));
	}

	@Test
	@DisplayName("parsePassphraseEnterprises: 支持前端 JSON 字符串")
	void parsePassphraseEnterprises_acceptsJsonString() {
		String json =
				"[{\"enterprise_id\":900101,\"participate_quota\":1,"
						+ "\"passphrase_limitfee\":1000,\"passphrase_code\":\"00000000\"}]";
		var items = ActivityPassphraseSyncService.parsePassphraseEnterprises(json);
		assertEquals(1, items.size());
		assertEquals(900101L, items.get(0).enterpriseId());
		assertEquals(1, items.get(0).participateQuota());
		assertEquals(1000, items.get(0).passphraseLimitfee());
		assertEquals("00000000", items.get(0).passphraseCode());
	}

	@Test
	@DisplayName("parsePassphraseEnterprises: 非法 JSON 字符串报错")
	void parsePassphraseEnterprises_invalidJsonString() {
		assertThrows(
				ResourceException.class,
				() -> ActivityPassphraseSyncService.parsePassphraseEnterprises("[invalid"));
	}

	@Test
	@DisplayName("resolveUpdateSyncMode: replace 当带 passphrase_enterprises")
	void resolveSyncMode_replaceWhenEnterprisesPresent() {
		Map<String, Object> params = Map.of("passphrase_enterprises", List.of(Map.of("enterprise_id", 1)));
		assertEquals(
				ActivityPassphraseSyncService.PassphraseSyncMode.REPLACE,
				service.resolveUpdateSyncMode(params));
	}
}
