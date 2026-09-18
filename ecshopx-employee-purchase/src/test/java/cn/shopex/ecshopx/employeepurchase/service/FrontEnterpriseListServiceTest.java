package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrontEnterpriseListServiceTest {

	@Mock private EnterprisesMapper enterprisesMapper;
	@Mock private EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;
	@Mock private EmployeesMapper employeesMapper;
	@Mock private RelativesMapper relativesMapper;
	@Mock private ActivityEnterprisesMapper activityEnterprisesMapper;
	@Mock private DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	@Mock private DistributorSelfMetaService distributorSelfMetaService;
	@Mock private DistributorInfoResolveService distributorInfoResolveService;

	private FrontEnterpriseListService service;

	@BeforeEach
	void setUp() {
		service =
				new FrontEnterpriseListService(
						enterprisesMapper,
						enterpriseEmailBoxMapper,
						employeesMapper,
						relativesMapper,
						activityEnterprisesMapper,
						distributorEmployeeListLookupService,
						distributorSelfMetaService,
						distributorInfoResolveService);
	}

	@Test
	@DisplayName("listUserEnterprises 排除后台禁用的企业")
	void listUserEnterprises_excludesDisabledEnterprises() {
		long companyId = 141L;
		long userId = 1001L;

		Employees employee = new Employees();
		employee.setCompanyId(companyId);
		employee.setUserId(userId);
		employee.setEnterpriseId(10L);
		employee.setMobile("13800138000");
		employee.setDisabled(false);
		when(employeesMapper.selectList(any())).thenReturn(List.of(employee));
		when(relativesMapper.selectList(any())).thenReturn(List.of());

		Enterprises enabled = new Enterprises();
		enabled.setId(10L);
		enabled.setCompanyId(companyId);
		enabled.setName("启用企业");
		enabled.setEnterpriseSn("EN001");
		enabled.setAuthType("mobile");
		enabled.setDisabled(false);
		when(enterprisesMapper.selectList(any())).thenReturn(List.of(enabled));

		List<Map<String, Object>> result =
				service.listUserEnterprises(companyId, userId, null, 0L, 0L);

		assertEquals(1, result.size());
		assertEquals(10L, ((Number) result.get(0).get("enterprise_id")).longValue());
		assertEquals("启用企业", result.get(0).get("name"));
	}

	@Test
	@DisplayName("listUserEnterprises 企业被禁用时返回空列表")
	void listUserEnterprises_returnsEmptyWhenEnterpriseDisabled() {
		long companyId = 141L;
		long userId = 1001L;

		Employees employee = new Employees();
		employee.setCompanyId(companyId);
		employee.setUserId(userId);
		employee.setEnterpriseId(20L);
		employee.setMobile("13800138000");
		employee.setDisabled(false);
		when(employeesMapper.selectList(any())).thenReturn(List.of(employee));
		when(relativesMapper.selectList(any())).thenReturn(List.of());
		when(enterprisesMapper.selectList(any())).thenReturn(List.of());

		List<Map<String, Object>> result =
				service.listUserEnterprises(companyId, userId, null, 0L, 0L);

		assertTrue(result.isEmpty());
	}
}
