package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseParticipateUserMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePassphraseServiceTest {

	private static final long COMPANY_ID = 1L;
	private static final long ENTERPRISE_ID = 10L;
	private static final long ACTIVITY_ID = 100L;
	private static final long USER_ID = 200L;
	private static final int ENTERPRISE_DISTRIBUTOR_ID = 285;

	@Mock
	private ActivityPassphraseService activityPassphraseService;

	@Mock
	private PassphraseVerifiedRedisService passphraseVerifiedRedisService;

	@Mock
	private PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService;

	@Mock
	private ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService;

	@Mock
	private ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper;

	@Mock
	private EmployeesMapper employeesMapper;

	@Mock
	private EnterprisesMapper enterprisesMapper;

	@Mock
	private RelativesMapper relativesMapper;

	@Mock
	private PointMemberDmPointMemberInfoReadPort pointMemberInfoReadPort;

	private EmployeePassphraseService service;

	@BeforeEach
	void setUp() {
		service =
				new EmployeePassphraseService(
						activityPassphraseService,
						passphraseVerifiedRedisService,
						passphraseParticipateQuotaRedisService,
						activityEnterpriseBehaviorLogService,
						activityEnterpriseParticipateUserMapper,
						employeesMapper,
						enterprisesMapper,
						relativesMapper,
						pointMemberInfoReadPort);
	}

	private Activities passphraseActivity() {
		Activities activity = new Activities();
		activity.setIsPassphraseEnabled(true);
		return activity;
	}

	private void stubVerifiedPassphrasePath(Activities activity) {
		when(activityPassphraseService.isPassphraseEnabled(activity)).thenReturn(true);
		when(passphraseVerifiedRedisService.isVerified(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID))
				.thenReturn(true);
		when(activityPassphraseService.supportsPassphraseBypassWhitelist(
						activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(true);
	}

	private void stubMember(String mobile, String email) {
		when(pointMemberInfoReadPort.getMemberInfo(USER_ID, COMPANY_ID))
				.thenReturn(Map.of("user_id", USER_ID, "mobile", mobile, "email", email));
	}

	@Test
	@DisplayName("口令自动建档：员工 distributor_id 取自企业来源店铺")
	void insertEmployeeUsesEnterpriseDistributorId() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		stubMember("13800138000", "");

		when(employeesMapper.selectOne(any())).thenReturn(null);
		when(relativesMapper.selectOne(any())).thenReturn(null);

		Enterprises enterprise = new Enterprises();
		enterprise.setId(ENTERPRISE_ID);
		enterprise.setCompanyId(COMPANY_ID);
		enterprise.setDistributorId(ENTERPRISE_DISTRIBUTOR_ID);
		when(enterprisesMapper.selectOne(any())).thenReturn(enterprise);

		when(activityEnterpriseParticipateUserMapper.selectCount(any())).thenReturn(0L);

		ActivityPassphraseEnterprise cfg = new ActivityPassphraseEnterprise();
		cfg.setParticipateQuota(10);
		when(activityPassphraseService.requireEnterpriseConfig(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(cfg);
		when(passphraseParticipateQuotaRedisService.tryConsumeSlot(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(PassphraseParticipateQuotaRedisService.TryConsumeResult.SUCCESS);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		ArgumentCaptor<Employees> captor = ArgumentCaptor.forClass(Employees.class);
		verify(employeesMapper).insert(captor.capture());
		assertEquals(ENTERPRISE_DISTRIBUTOR_ID, captor.getValue().getDistributorId());
		assertEquals("用户-8000", captor.getValue().getName());
		assertEquals("13800138000", captor.getValue().getMobile());
		assertEquals("13800138000", captor.getValue().getMemberMobile());
	}

	@Test
	@DisplayName("企业不存在时不建档")
	void skipInsertWhenEnterpriseMissing() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		stubMember("13800138000", "");

		when(employeesMapper.selectOne(any())).thenReturn(null);
		when(relativesMapper.selectOne(any())).thenReturn(null);
		when(enterprisesMapper.selectOne(any())).thenReturn(null);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		verify(employeesMapper, never()).insert(org.mockito.ArgumentMatchers.<Employees>any());
	}

	@Test
	@DisplayName("已是活动亲友时不口令自动建档")
	void skipWhenActiveRelative() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);

		when(relativesMapper.selectOne(any())).thenReturn(new Relatives());

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		verify(employeesMapper, never()).insert(org.mockito.ArgumentMatchers.<Employees>any());
		verify(enterprisesMapper, never()).selectOne(any());
		verify(pointMemberInfoReadPort, never()).getMemberInfo(anyLong(), anyLong());
	}

	@Test
	@DisplayName("口令新建员工后禁用同企业亲友关系")
	void disableRelativesAfterInsert() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		stubMember("13800138000", "");

		when(relativesMapper.selectOne(any())).thenReturn(null);
		when(employeesMapper.selectOne(any())).thenReturn(null);

		Enterprises enterprise = new Enterprises();
		enterprise.setDistributorId(ENTERPRISE_DISTRIBUTOR_ID);
		when(enterprisesMapper.selectOne(any())).thenReturn(enterprise);
		when(activityEnterpriseParticipateUserMapper.selectCount(any())).thenReturn(0L);

		ActivityPassphraseEnterprise cfg = new ActivityPassphraseEnterprise();
		cfg.setParticipateQuota(10);
		when(activityPassphraseService.requireEnterpriseConfig(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(cfg);
		when(passphraseParticipateQuotaRedisService.tryConsumeSlot(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(PassphraseParticipateQuotaRedisService.TryConsumeResult.SUCCESS);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		verify(relativesMapper).update(org.mockito.ArgumentMatchers.isNull(), any());
	}

	@Test
	@DisplayName("口令配置无效时不建档")
	void skipWhenPassphraseBypassNotSupported() {
		Activities activity = passphraseActivity();
		when(activityPassphraseService.isPassphraseEnabled(activity)).thenReturn(true);
		when(passphraseVerifiedRedisService.isVerified(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID))
				.thenReturn(true);
		when(activityPassphraseService.supportsPassphraseBypassWhitelist(
						activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(false);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		verify(pointMemberInfoReadPort, never()).getMemberInfo(anyLong(), anyLong());
		verify(employeesMapper, never()).insert(org.mockito.ArgumentMatchers.<Employees>any());
	}

	@Test
	@DisplayName("会员不存在时抛错")
	void throwWhenMemberMissing() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		when(relativesMapper.selectOne(any())).thenReturn(null);
		when(pointMemberInfoReadPort.getMemberInfo(USER_ID, COMPANY_ID)).thenReturn(Map.of());

		ResourceException ex =
				assertThrows(
						ResourceException.class,
						() ->
								service.ensurePassphraseEmployeeFromVerifiedActivity(
										activity,
										COMPANY_ID,
										ACTIVITY_ID,
										ENTERPRISE_ID,
										USER_ID,
										"13800138000"));
		assertEquals("会员信息不存在", ex.getMessage());
		verify(employeesMapper, never()).insert(org.mockito.ArgumentMatchers.<Employees>any());
	}

	@Test
	@DisplayName("建档姓名字段与 PHP 一致且写入会员邮箱")
	void insertEmployeeNameAndEmailFromMember() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		stubMember("13912345678", "user@example.com");

		when(relativesMapper.selectOne(any())).thenReturn(null);
		when(employeesMapper.selectOne(any())).thenReturn(null);

		Enterprises enterprise = new Enterprises();
		enterprise.setDistributorId(ENTERPRISE_DISTRIBUTOR_ID);
		when(enterprisesMapper.selectOne(any())).thenReturn(enterprise);
		when(activityEnterpriseParticipateUserMapper.selectCount(any())).thenReturn(0L);

		ActivityPassphraseEnterprise cfg = new ActivityPassphraseEnterprise();
		cfg.setParticipateQuota(10);
		when(activityPassphraseService.requireEnterpriseConfig(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(cfg);
		when(passphraseParticipateQuotaRedisService.tryConsumeSlot(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(PassphraseParticipateQuotaRedisService.TryConsumeResult.SUCCESS);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "");

		ArgumentCaptor<Employees> captor = ArgumentCaptor.forClass(Employees.class);
		verify(employeesMapper).insert(captor.capture());
		Employees row = captor.getValue();
		assertEquals("用户-5678", row.getName());
		assertEquals("13912345678", row.getMobile());
		assertEquals("13912345678", row.getMemberMobile());
		assertEquals("user@example.com", row.getEmail());
		verify(pointMemberInfoReadPort).getMemberInfo(eq(USER_ID), eq(COMPANY_ID));
	}

	@Test
	@DisplayName("已是员工仍占活动名额并记 bind，不 insert 员工")
	void existingEmployeeConsumesQuotaAndBindWithoutInsert() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		stubMember("13800138000", "");

		when(relativesMapper.selectOne(any())).thenReturn(null);

		Employees active = new Employees();
		active.setUserId(USER_ID);
		active.setDisabled(false);
		when(employeesMapper.selectOne(any())).thenReturn(active);

		Enterprises enterprise = new Enterprises();
		enterprise.setDistributorId(ENTERPRISE_DISTRIBUTOR_ID);
		when(enterprisesMapper.selectOne(any())).thenReturn(enterprise);
		when(activityEnterpriseParticipateUserMapper.selectCount(any())).thenReturn(0L);

		ActivityPassphraseEnterprise cfg = new ActivityPassphraseEnterprise();
		cfg.setParticipateQuota(10);
		when(activityPassphraseService.requireEnterpriseConfig(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(cfg);
		when(passphraseParticipateQuotaRedisService.tryConsumeSlot(
						COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID))
				.thenReturn(PassphraseParticipateQuotaRedisService.TryConsumeResult.SUCCESS);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		verify(employeesMapper, never()).insert(org.mockito.ArgumentMatchers.<Employees>any());
		verify(activityEnterpriseParticipateUserMapper)
				.insertIgnore(
						eq(COMPANY_ID), eq(ACTIVITY_ID), eq(ENTERPRISE_ID), eq(USER_ID), anyInt());
		verify(activityEnterpriseBehaviorLogService)
				.recordBind(
						eq(COMPANY_ID),
						eq(ACTIVITY_ID),
						eq(ENTERPRISE_ID),
						eq(USER_ID),
						eq(PassphraseConstants.BIND_CHANNEL_PASSPHRASE));
	}

	@Test
	@DisplayName("已是员工且已在 participate 表时不占名额不写 bind")
	void existingEmployeeAlreadyParticipatingSkipsQuotaAndBind() {
		Activities activity = passphraseActivity();
		stubVerifiedPassphrasePath(activity);
		stubMember("13800138000", "");

		when(relativesMapper.selectOne(any())).thenReturn(null);
		when(employeesMapper.selectOne(any())).thenReturn(new Employees());
		when(enterprisesMapper.selectOne(any())).thenReturn(new Enterprises());
		when(activityEnterpriseParticipateUserMapper.selectCount(any())).thenReturn(1L);

		service.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, COMPANY_ID, ACTIVITY_ID, ENTERPRISE_ID, USER_ID, "13800138000");

		verify(passphraseParticipateQuotaRedisService, never())
				.tryConsumeSlot(anyLong(), anyLong(), anyLong());
		verify(activityEnterpriseParticipateUserMapper, never())
				.insertIgnore(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
		verify(activityEnterpriseBehaviorLogService, never())
				.recordBind(anyLong(), anyLong(), anyLong(), anyLong(), any());
		verify(employeesMapper, never()).insert(org.mockito.ArgumentMatchers.<Employees>any());
	}
}
