package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePassphraseService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseVerifiedRedisService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePurchaseActivityDataServiceSoftFailTest {

	@Mock private ActivitiesMapper activitiesMapper;
	@Mock private EmployeesMapper employeesMapper;
	@Mock private RelativesMapper relativesMapper;
	@Mock private MemberActivityAggregateMapper memberActivityAggregateMapper;
	@Mock private ActivityPassphraseService activityPassphraseService;
	@Mock private PassphraseVerifiedRedisService passphraseVerifiedRedisService;
	@Mock private EmployeePassphraseService employeePassphraseService;
	@Mock private EnterpriseConfigService enterpriseConfigService;

	private EmployeePurchaseActivityDataService service;

	@BeforeEach
	void setUp() {
		service =
				new EmployeePurchaseActivityDataService(
						activitiesMapper,
						employeesMapper,
						relativesMapper,
						memberActivityAggregateMapper,
						activityPassphraseService,
						passphraseVerifiedRedisService,
						employeePassphraseService,
						enterpriseConfigService);
	}

	@Test
	@DisplayName("口令名额满：软失败返回 purchase_mode，额度置 0，写 extraTips")
	void buildActivityData_quotaFull_softFailsWithExtraTipsAndZeroFees() {
		Activities activity = prepaidActivity(150L, "101");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		doThrow(new ResourceException("该企业在本活动下的参与名额已满"))
				.when(employeePassphraseService)
				.ensurePassphraseEmployeeFromVerifiedActivity(
						eq(activity), eq(1L), eq(150L), eq(101L), eq(9L), eq("13800000000"));
		when(activityPassphraseService.getPassphraseClientSummary(activity, 1L, 150L, 101L))
				.thenReturn(
						Map.of(
								"is_passphrase_enabled",
								1,
								"passphrase_participate_quota",
								10,
								"passphrase_limitfee",
								10000));
		when(passphraseVerifiedRedisService.isVerified(1L, 150L, 101L, 9L)).thenReturn(true);
		when(employeesMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(relativesMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		Map<String, Object> data =
				service.buildActivityData(1L, 9L, 150L, 101L, "13800000000");

		assertEquals(PurchaseModeSupport.PREPAID_POINT, data.get("purchase_mode"));
		assertEquals("预充点数", data.get("purchase_mode_desc"));
		assertEquals(0, data.get("limit_fee"));
		assertEquals(0L, ((Number) data.get("aggregate_fee")).longValue());
		assertEquals(0, data.get("left_fee"));
		assertEquals(0, data.get("is_employee"));
		assertEquals("该企业在本活动下的参与名额已满", data.get("extraTips"));
		verify(activityPassphraseService, never()).isPassphraseEnabled(any());
		verify(enterpriseConfigService, never())
				.requirePerCapitaLimitfee(anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("ensure 成功：不写 extraTips，走正常算费")
	void buildActivityData_ensureOk_computesFeesWithoutExtraTips() {
		Activities activity = prepaidActivity(150L, "101");
		when(activitiesMapper.selectOne(any(Wrapper.class))).thenReturn(activity);
		when(activityPassphraseService.isPassphraseEnabled(activity)).thenReturn(false);
		when(employeesMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(relativesMapper.selectOne(any(Wrapper.class))).thenReturn(null);
		when(activityPassphraseService.getPassphraseClientSummary(activity, 1L, 150L, 101L))
				.thenReturn(Map.of("is_passphrase_enabled", 0));
		when(passphraseVerifiedRedisService.isVerified(1L, 150L, 101L, 9L)).thenReturn(false);

		Map<String, Object> data = service.buildActivityData(1L, 9L, 150L, 101L, "");

		assertEquals(PurchaseModeSupport.PREPAID_POINT, data.get("purchase_mode"));
		assertEquals(0, data.get("limit_fee"));
		assertEquals(0L, ((Number) data.get("aggregate_fee")).longValue());
		assertEquals(0, data.get("left_fee"));
		assertFalse(data.containsKey("extraTips"));
		assertNull(data.get("extraTips"));
	}

	private static Activities prepaidActivity(long id, String enterpriseIdCsv) {
		Activities a = new Activities();
		a.setId(id);
		a.setCompanyId(1L);
		a.setEnterpriseId(enterpriseIdCsv);
		a.setName("act");
		a.setTitle("title");
		a.setPurchaseMode(PurchaseModeSupport.PREPAID_POINT);
		a.setIfRelativeJoin(false);
		a.setInviteLimit(0);
		return a;
	}
}
