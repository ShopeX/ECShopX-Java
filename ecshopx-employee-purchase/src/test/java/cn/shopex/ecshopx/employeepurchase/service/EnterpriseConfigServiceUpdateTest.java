package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseParticipateUserMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseConfigService.EnterpriseConfigInput;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseSyncService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseParticipateQuotaRedisService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnterpriseConfigServiceUpdateTest {

	@Mock private ActivityEnterprisesMapper activityEnterprisesMapper;
	@Mock private ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;
	@Mock private EnterprisesMapper enterprisesMapper;
	@Mock private ActivityPassphraseSyncService activityPassphraseSyncService;
	@Mock private ActivityEnterpriseParticipateUserMapper participateUserMapper;
	@Mock private PassphraseParticipateQuotaRedisService quotaRedisService;

	private EnterpriseConfigService service;

	@BeforeEach
	void setUp() {
		service =
				new EnterpriseConfigService(
						activityEnterprisesMapper,
						activityPassphraseEnterpriseMapper,
						enterprisesMapper,
						activityPassphraseSyncService,
						participateUserMapper,
						quotaRedisService);
	}

	@Test
	@DisplayName("全量提交时只改可参与名额：不报锁定错误")
	void assertConfigsUnchanged_allowsParticipateQuotaChange() {
		stubExistingEnterpriseAndPassphrase(500, 1, "1");
		List<Map<String, Object>> request =
				List.of(
						Map.of(
								"enterprise_id",
								129,
								"per_capita_limitfee",
								500,
								"participate_quota",
								2,
								"passphrase_code",
								"1"));

		assertDoesNotThrow(
				() ->
						service.assertConfigsUnchanged(
								141L, 157L, List.of(129L), request, true));
	}

	@Test
	@DisplayName("改人均额度仍锁定")
	void assertConfigsUnchanged_rejectsPerCapitaChange() {
		stubExistingEnterpriseAndPassphrase(500, 1, "1");
		List<Map<String, Object>> request =
				List.of(
						Map.of(
								"enterprise_id",
								129,
								"per_capita_limitfee",
								600,
								"participate_quota",
								1,
								"passphrase_code",
								"1"));

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								service.assertConfigsUnchanged(
										141L, 157L, List.of(129L), request, true));
		assertEquals("购买方式与企业额度配置创建后不可修改", ex.getMessage());
	}

	@Test
	@DisplayName("改口令码仍锁定")
	void assertConfigsUnchanged_rejectsPassphraseCodeChange() {
		stubExistingEnterpriseAndPassphrase(500, 1, "1");
		List<Map<String, Object>> request =
				List.of(
						Map.of(
								"enterprise_id",
								129,
								"per_capita_limitfee",
								500,
								"participate_quota",
								1,
								"passphrase_code",
								"CHANGED"));

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								service.assertConfigsUnchanged(
										141L, 157L, List.of(129L), request, true));
		assertEquals("购买方式与企业额度配置创建后不可修改", ex.getMessage());
	}

	@Test
	@DisplayName("改可参与名额：写库并按已占用同步 Redis 剩余")
	void applyParticipateQuotaOnUpdate_updatesDbAndRedisRemaining() {
		stubPassphraseRow(1, "1");
		when(participateUserMapper.selectCount(any())).thenReturn(1L);

		service.applyParticipateQuotaOnUpdate(
				141L,
				157L,
				List.of(new EnterpriseConfigInput(129L, 500, 3, "1")),
				true);

		verify(activityPassphraseEnterpriseMapper).updateById(any(ActivityPassphraseEnterprise.class));
		verify(quotaRedisService).syncRemainingQuota(141L, 157L, 129L, 2);
	}

	@Test
	@DisplayName("可参与名额未变：不写库不刷 Redis")
	void applyParticipateQuotaOnUpdate_skipsWhenUnchanged() {
		stubPassphraseRow(2, "1");

		service.applyParticipateQuotaOnUpdate(
				141L,
				157L,
				List.of(new EnterpriseConfigInput(129L, 500, 2, "1")),
				true);

		verify(activityPassphraseEnterpriseMapper, never())
				.updateById(any(ActivityPassphraseEnterprise.class));
		verify(quotaRedisService, never()).syncRemainingQuota(eq(141L), eq(157L), eq(129L), eq(2));
	}

	@Test
	@DisplayName("可参与名额不能小于已占用人数")
	void applyParticipateQuotaOnUpdate_rejectsQuotaBelowOccupied() {
		stubPassphraseRow(3, "1");
		when(participateUserMapper.selectCount(any())).thenReturn(2L);

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								service.applyParticipateQuotaOnUpdate(
										141L,
										157L,
										List.of(new EnterpriseConfigInput(129L, 500, 1, "1")),
										true));
		assertEquals("可参与名额不能小于已占用人数", ex.getMessage());
		verify(quotaRedisService, never()).syncRemainingQuota(any(Long.class), any(Long.class), any(Long.class), any(Integer.class));
	}

	private void stubExistingEnterpriseAndPassphrase(int fee, int quota, String code) {
		ActivityEnterprises ent = new ActivityEnterprises();
		ent.setEnterpriseId(129L);
		ent.setPerCapitaLimitfee(fee);
		when(activityEnterprisesMapper.selectList(any())).thenReturn(List.of(ent));

		ActivityPassphraseEnterprise pass = new ActivityPassphraseEnterprise();
		pass.setId(9L);
		pass.setEnterpriseId(129L);
		pass.setParticipateQuota(quota);
		pass.setPassphraseCode(code);
		when(activityPassphraseEnterpriseMapper.selectList(any())).thenReturn(List.of(pass));
	}

	private void stubPassphraseRow(int quota, String code) {
		ActivityPassphraseEnterprise pass = new ActivityPassphraseEnterprise();
		pass.setId(9L);
		pass.setEnterpriseId(129L);
		pass.setParticipateQuota(quota);
		pass.setPassphraseCode(code);
		when(activityPassphraseEnterpriseMapper.selectList(any())).thenReturn(List.of(pass));
	}
}
