package cn.shopex.ecshopx.companys.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.dto.EmployeeAccountManagementListQuery;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.mapper.SupplierOperatorReadMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeManagementListServiceTest {

	@Mock
	private OperatorsQueryService operatorsQueryService;
	@Mock
	private OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;
	@Mock
	private EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	@Mock
	private DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	@Mock
	private SupplierOperatorReadMapper supplierOperatorReadMapper;
	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;
	@Mock
	private OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService;

	@Test
	@DisplayName("店铺管理员列表返回绑定店铺的店铺号")
	void distributorListIncludesShopCode() {
		EmployeeManagementListService service =
				new EmployeeManagementListService(
						operatorsQueryService,
						operatorDeliveryStaffRoleDataService,
						employeeSelfDeliveryStaffMapper,
						distributionDistributorSelfReadMapper,
						supplierOperatorReadMapper,
						sensitiveFieldEncryptor,
						operatorAccountOutsideLangReadService,
						new ObjectMapper());

		Operators operator = new Operators();
		operator.setOperatorId(9L);
		operator.setCompanyId(1001L);
		operator.setMobile("13800138000");
		operator.setUsername("店长");
		operator.setDistributorIds("[{\"distributor_id\":\"88\",\"name\":\"一号店\"}]");

		EmployeeAccountManagementListQuery query = new EmployeeAccountManagementListQuery();
		query.setOperatorType("distributor");

		when(operatorsQueryService.countAccountManagementList(any())).thenReturn(1L);
		when(operatorsQueryService.pageAccountManagementList(any(), anyInt(), anyInt(), anyList()))
				.thenReturn(List.of(operator));
		when(operatorDeliveryStaffRoleDataService.roleDataRowsForOperators(eq(1001L), anyList()))
				.thenReturn(Map.of());
		when(distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(
						eq(1001L), anyList()))
				.thenReturn(List.of(Map.of("distributor_id", 88L, "name", "一号店", "shop_code", "S001")));

		Map<String, Object> result =
				service.getListData(
						Map.of("company_id", 1001L, "operator_id", 2002L, "operator_type", "admin"),
						query,
						0,
						"zh-CN");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("list");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> distributors =
				(List<Map<String, Object>>) rows.get(0).get("distributor_ids");
		assertThat(distributors.get(0).get("shop_code")).isEqualTo("S001");
	}

	@Test
	@DisplayName("店铺号限制账号列表到所属店铺")
	void shopCodeRestrictsAccountListToItsDistributor() {
		EmployeeManagementListService service =
				new EmployeeManagementListService(
						operatorsQueryService,
						operatorDeliveryStaffRoleDataService,
						employeeSelfDeliveryStaffMapper,
						distributionDistributorSelfReadMapper,
						supplierOperatorReadMapper,
						sensitiveFieldEncryptor,
						operatorAccountOutsideLangReadService,
						new ObjectMapper());
		EmployeeAccountManagementListQuery query = new EmployeeAccountManagementListQuery();
		query.setOperatorType("distributor");
		query.setShopCode("JM001");

		when(distributionDistributorSelfReadMapper.selectShopByCompanyAndShopCode(1001L, "JM001"))
				.thenReturn(Map.of("distributor_id", 3361L));
		when(operatorsQueryService.countAccountManagementList(any())).thenReturn(0L);

		service.getListData(
				Map.of("company_id", 1001L, "operator_id", 2002L, "operator_type", "admin"),
				query,
				0,
				"zh-CN");

		ArgumentCaptor<AccountManagementOperatorsFilter> filter =
				ArgumentCaptor.forClass(AccountManagementOperatorsFilter.class);
		verify(operatorsQueryService).countAccountManagementList(filter.capture());
		assertThat(filter.getValue().getDistributorIds()).containsExactly(3361L);
	}

	@Test
	@DisplayName("未知店铺号不返回账号")
	void unknownShopCodeReturnsNoAccounts() {
		EmployeeManagementListService service =
				new EmployeeManagementListService(
						operatorsQueryService,
						operatorDeliveryStaffRoleDataService,
						employeeSelfDeliveryStaffMapper,
						distributionDistributorSelfReadMapper,
						supplierOperatorReadMapper,
						sensitiveFieldEncryptor,
						operatorAccountOutsideLangReadService,
						new ObjectMapper());
		EmployeeAccountManagementListQuery query = new EmployeeAccountManagementListQuery();
		query.setShopCode("UNKNOWN");

		when(operatorsQueryService.countAccountManagementList(any())).thenReturn(0L);

		service.getListData(
				Map.of("company_id", 1001L, "operator_id", 2002L, "operator_type", "admin"),
				query,
				0,
				"zh-CN");

		ArgumentCaptor<AccountManagementOperatorsFilter> filter =
				ArgumentCaptor.forClass(AccountManagementOperatorsFilter.class);
		verify(operatorsQueryService).countAccountManagementList(filter.capture());
		assertThat(filter.getValue().getOperatorIds()).containsExactly(-1L);
	}
}
