package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityDataService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassphraseBehaviorReportServiceTest {

	@Mock
	private ActivitiesMapper activitiesMapper;

	@Mock
	private EmployeePurchaseActivityDataService employeePurchaseActivityDataService;

	@Mock
	private ActivityPassphraseService activityPassphraseService;

	@Mock
	private ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService;

	@Mock
	private PassphraseVerifiedRedisService passphraseVerifiedRedisService;

	private PassphraseBehaviorReportService service;

	@BeforeEach
	void setUp() {
		service =
				new PassphraseBehaviorReportService(
						activitiesMapper,
						employeePurchaseActivityDataService,
						activityPassphraseService,
						activityEnterpriseBehaviorLogService,
						passphraseVerifiedRedisService);
	}

	@Test
	@DisplayName("QA-01: 正确口令 verified=true 且写 success 流水")
	void correctPassphraseVerifiedTrue() {
		stubActivity(150L);
		when(activityPassphraseService.isActivityEnterprisePassphraseMatch(
						any(), eq(1L), eq(150L), eq(101L), eq("abc12345")))
				.thenReturn(true);
		when(activityEnterpriseBehaviorLogService.recordPassphraseVerify(
						eq(1L), eq(150L), eq(101L), eq(1001L), eq(""), eq(true)))
				.thenReturn(99L);

		Map<String, Object> input = baseInput("passphrase_verify", "abc12345");
		input.put("user_id", 1001L);
		Map<String, Object> out = service.report(input);

		assertTrue((Boolean) out.get("verified"));
		assertEquals(99L, out.get("log_id"));
		verify(passphraseVerifiedRedisService)
				.markVerified(eq(1L), eq(150L), eq(101L), eq(1001L), anyLong());
	}

	@Test
	@DisplayName("QA-02: 错误口令 verified=false 写 fail 流水，不写 Redis")
	void wrongPassphraseVerifiedFalse() {
		stubActivity(150L);
		when(activityPassphraseService.isActivityEnterprisePassphraseMatch(any(), anyLong(), anyLong(), anyLong(), any()))
				.thenReturn(false);
		when(activityEnterpriseBehaviorLogService.recordPassphraseVerify(
						eq(1L), eq(150L), eq(101L), eq(1001L), eq(""), eq(false)))
				.thenReturn(88L);

		Map<String, Object> input = baseInput("passphrase_verify", "wrongCode");
		input.put("user_id", 1001L);
		Map<String, Object> out = service.report(input);

		assertFalse((Boolean) out.get("verified"));
		verify(passphraseVerifiedRedisService, never()).markVerified(anyLong(), anyLong(), anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("QA-03: 活动不存在抛错，不写口令流水")
	void activityNotFoundThrowsWithoutVerifyLog() {
		when(activitiesMapper.selectOne(any())).thenReturn(null);

		Map<String, Object> input = baseInput("passphrase_verify", "anyCode");
		assertThrows(ResourceException.class, () -> service.report(input));
		verify(activityEnterpriseBehaviorLogService, never())
				.recordPassphraseVerify(anyLong(), anyLong(), anyLong(), any(), any(), anyBoolean());
	}

	@Test
	@DisplayName("C1: 空口令抛错")
	void emptyPassphraseThrows() {
		stubActivity(150L);
		Map<String, Object> input = baseInput("passphrase_verify", "");
		assertThrows(ResourceException.class, () -> service.report(input));
	}

	private void stubActivity(long activityId) {
		Activities activity = new Activities();
		activity.setId(activityId);
		activity.setCompanyId(1L);
		activity.setEmployeeEndTime((int) (System.currentTimeMillis() / 1000L) + 86400);
		when(activitiesMapper.selectOne(any())).thenReturn(activity);
	}

	private static Map<String, Object> baseInput(String behaviorType, String code) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("behavior_type", behaviorType);
		m.put("company_id", 1L);
		m.put("activity_id", 150L);
		m.put("enterprise_id", 101L);
		m.put("passphrase_code", code);
		return m;
	}
}
