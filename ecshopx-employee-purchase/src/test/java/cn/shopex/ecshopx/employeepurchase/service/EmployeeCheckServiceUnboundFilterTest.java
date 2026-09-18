package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeFrontCheckQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeFrontCheckListRow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeCheckServiceUnboundFilterTest {

	@Mock private EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper;
	@Mock private EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService;
	@Mock private SensitiveFieldEncryptor sensitiveFieldEncryptor;
	@Mock private DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	@Mock private DistributorSelfMetaService distributorSelfMetaService;

	private EmployeeCheckService service;

	@BeforeEach
	void setUp() {
		service =
				new EmployeeCheckService(
						employeeFrontCheckQueryMapper,
						emailVcodeRedisService,
						sensitiveFieldEncryptor,
						distributorEmployeeListLookupService,
						distributorSelfMetaService);
	}

	@Test
	@DisplayName("PHP对齐：带 activity_id 的手机号 check 不限定 user_id=0")
	void mobileWithActivityId_doesNotRequireUnboundUser() {
		when(employeeFrontCheckQueryMapper.selectEnterpriseIdsByActivity(141L, 166L))
				.thenReturn(List.of(121L));
		stubMobileHit(141L, "mobile", List.of(121L), null, false);

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("auth_type", "mobile");
		params.put("activity_id", 166);
		params.put("enterprise_id", 121);
		params.put("mobile", "13800138000");

		Map<String, Object> result = service.doEmployeeCheck(141L, params);

		assertEquals(1L, result.get("total_count"));
		verify(employeeFrontCheckQueryMapper)
				.countEmployeesWithRel(
						eq(141L),
						eq("mobile"),
						eq(List.of(121L)),
						isNull(),
						isNull(),
						eq("enc-13800138000"),
						isNull(),
						isNull(),
						eq(false));
	}

	@Test
	@DisplayName("PHP对齐：手机号 check 即使未指定企业也不限定 user_id=0")
	void mobileWithoutEnterprise_doesNotRequireUnboundUser() {
		stubMobileHit(141L, "mobile", null, null, false);

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("auth_type", "mobile");
		params.put("mobile", "13800138000");

		service.doEmployeeCheck(141L, params);

		verify(employeeFrontCheckQueryMapper)
				.countEmployeesWithRel(
						eq(141L),
						eq("mobile"),
						isNull(),
						isNull(),
						isNull(),
						eq("enc-13800138000"),
						isNull(),
						isNull(),
						eq(false));
	}

	@Test
	@DisplayName("PHP对齐：账号+activity_id 不限定 user_id=0")
	void accountWithActivityId_doesNotRequireUnboundUser() {
		when(employeeFrontCheckQueryMapper.selectEnterpriseIdsByActivity(141L, 166L))
				.thenReturn(List.of(121L));
		stubAccountHit(141L, List.of(121L), null, false);

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("auth_type", "account");
		params.put("activity_id", 166);
		params.put("account", "emp01");
		params.put("auth_code", "pw");

		service.doEmployeeCheck(141L, params);

		verify(employeeFrontCheckQueryMapper)
				.countEmployeesWithRel(
						eq(141L),
						eq("account"),
						eq(List.of(121L)),
						isNull(),
						isNull(),
						isNull(),
						eq("emp01"),
						eq("pw"),
						eq(false));
	}

	@Test
	@DisplayName("PHP对齐：账号且未指定活动/企业时仍限定 user_id=0")
	void accountWithoutEnterprise_requiresUnboundUser() {
		stubAccountHit(141L, null, null, true);

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("auth_type", "account");
		params.put("account", "emp01");
		params.put("auth_code", "pw");

		service.doEmployeeCheck(141L, params);

		verify(employeeFrontCheckQueryMapper)
				.countEmployeesWithRel(
						eq(141L),
						eq("account"),
						isNull(),
						isNull(),
						isNull(),
						isNull(),
						eq("emp01"),
						eq("pw"),
						eq(true));
	}

	private void stubMobileHit(
			long companyId,
			String authType,
			List<Long> enterpriseIds,
			Long singleEnterpriseId,
			boolean requireUnbound) {
		when(sensitiveFieldEncryptor.encrypt("13800138000")).thenReturn("enc-13800138000");
		when(sensitiveFieldEncryptor.decrypt(any())).thenAnswer(inv -> inv.getArgument(0));
		EmployeeFrontCheckListRow row = boundEmployeeRow();
		when(employeeFrontCheckQueryMapper.countEmployeesWithRel(
						eq(companyId),
						eq(authType),
						eq(enterpriseIds),
						eq(singleEnterpriseId),
						isNull(),
						eq("enc-13800138000"),
						isNull(),
						isNull(),
						eq(requireUnbound)))
				.thenReturn(1L);
		when(employeeFrontCheckQueryMapper.selectEmployeesWithRel(
						eq(companyId),
						eq(authType),
						eq(enterpriseIds),
						eq(singleEnterpriseId),
						isNull(),
						eq("enc-13800138000"),
						isNull(),
						isNull(),
						eq(requireUnbound)))
				.thenReturn(List.of(row));
		stubDistributorNames();
	}

	private void stubAccountHit(
			long companyId, List<Long> enterpriseIds, Long singleEnterpriseId, boolean requireUnbound) {
		EmployeeFrontCheckListRow row = boundEmployeeRow();
		when(employeeFrontCheckQueryMapper.countEmployeesWithRel(
						eq(companyId),
						eq("account"),
						eq(enterpriseIds),
						eq(singleEnterpriseId),
						isNull(),
						isNull(),
						eq("emp01"),
						eq("pw"),
						eq(requireUnbound)))
				.thenReturn(1L);
		when(employeeFrontCheckQueryMapper.selectEmployeesWithRel(
						eq(companyId),
						eq("account"),
						eq(enterpriseIds),
						eq(singleEnterpriseId),
						isNull(),
						isNull(),
						eq("emp01"),
						eq("pw"),
						eq(requireUnbound)))
				.thenReturn(List.of(row));
		stubDistributorNames();
	}

	private void stubDistributorNames() {
		when(distributorEmployeeListLookupService.distributorIdToName(anyLong(), any(), anyInt()))
				.thenReturn(Map.of());
		when(distributorSelfMetaService.getDistributorSelfSimpleInfo(anyLong()))
				.thenReturn(Map.of("name", "self"));
	}

	private static EmployeeFrontCheckListRow boundEmployeeRow() {
		EmployeeFrontCheckListRow row = new EmployeeFrontCheckListRow();
		row.setId(9L);
		row.setCompanyId(141L);
		row.setDistributorId(0);
		row.setEnterpriseId(121L);
		row.setUserId(1185L);
		row.setMobile("enc-13800138000");
		row.setName("enc-name");
		row.setAuthType("mobile");
		return row;
	}
}
