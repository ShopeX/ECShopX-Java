/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.companys.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.dto.EmployeeCreateRequest;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.EmployeeService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeCreateOrchestrationServiceMobileTest {

	@Mock
	private MemberOperatorContextService memberOperatorContextService;
	@Mock
	private OperatorsQueryService operatorsQueryService;
	@Mock
	private OperatorsMapper operatorsMapper;
	@Mock
	private DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	@Mock
	private EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	@Mock
	private EmployeeService employeeService;

	private EmployeeCreateOrchestrationService service;

	@BeforeEach
	void setUp() {
		service = new EmployeeCreateOrchestrationService(
				memberOperatorContextService,
				operatorsQueryService,
				operatorsMapper,
				distributionDistributorSelfReadMapper,
				employeeSelfDeliveryStaffMapper,
				employeeService);
	}

	@Test
	@DisplayName("supplier + EG 本地号放行")
	void supplierAcceptsEgLocal() {
		when(employeeService.createOperatorStaff(any())).thenReturn(Map.of("ok", true));

		Map<String, Object> out = service.execute(supplierBody("01012345678"), jwt());

		assertThat(out.get("ok")).isEqualTo(true);
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(employeeService).createOperatorStaff(captor.capture());
		assertThat(captor.getValue().get("mobile")).isEqualTo("01012345678");
	}

	@Test
	@DisplayName("supplier + CN 放行")
	void supplierAcceptsCn() {
		when(employeeService.createOperatorStaff(any())).thenReturn(Map.of("ok", true));

		service.execute(supplierBody("13800138000"), jwt());

		verify(employeeService).createOperatorStaff(any());
	}

	@Test
	@DisplayName("staff + EG 仍拒绝")
	void staffRejectsEg() {
		EmployeeCreateRequest body = baseBody("01012345678");
		body.setOperatorType("staff");

		assertThatThrownBy(() -> service.execute(body, jwt()))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("请填写正确的手机号");

		verify(employeeService, never()).createOperatorStaff(any());
	}

	@Test
	@DisplayName("supplier + 非法号拒绝")
	void supplierRejectsInvalid() {
		assertThatThrownBy(() -> service.execute(supplierBody("123"), jwt()))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("请填写正确的手机号");

		verify(employeeService, never()).createOperatorStaff(any());
	}

	private static EmployeeCreateRequest supplierBody(String mobile) {
		EmployeeCreateRequest body = baseBody(mobile);
		body.setOperatorType("supplier");
		body.setLoginName("SUP001");
		return body;
	}

	private static EmployeeCreateRequest baseBody(String mobile) {
		EmployeeCreateRequest body = new EmployeeCreateRequest();
		body.setMobile(mobile);
		body.setPassword("pass1234");
		return body;
	}

	private static Map<String, Object> jwt() {
		return Map.of(
				"company_id", 1001L,
				"operator_id", 2002L,
				"operator_type", "admin");
	}
}
