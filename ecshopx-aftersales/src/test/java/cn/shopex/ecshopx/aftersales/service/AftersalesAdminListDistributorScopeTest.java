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

package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.AftersalesAdminListQuery;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorInfoReadPort;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10883：售后列表未授权标量 distributor_id 须收束到 JWT 门店，不得去掉门店条件。 */
class AftersalesAdminListDistributorScopeTest {

	private AftersalesMapper aftersalesMapper;
	private AftersalesAdminListService service;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		aftersalesMapper = mock(AftersalesMapper.class);
		when(aftersalesMapper.countAdminList(anyMap())).thenReturn(0L);
		service =
				new AftersalesAdminListService(
						aftersalesMapper,
						mock(AftersalesDetailMapper.class),
						mock(AftersalesRefundService.class),
						mock(MembersDeleteRecordMapper.class),
						mock(OperatorsMapper.class),
						mock(DistributorGetInfoSimpleByDistributorIdPort.class),
						mock(DistributorDefaultAftersalesAddressReadPort.class),
						mock(SupplierOperatorInfoReadPort.class),
						mock(OrderNormalOrderHeaderReadPort.class),
						mock(AftersalesAdminAppInfoBuilder.class));
		request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 100L);
		jwt.put("operator_type", "staff");
		jwt.put("operator_id", 1L);
		jwt.put(
				"distributor_ids",
				List.of(Map.of("distributor_id", "10"), Map.of("distributor_id", "20")));
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);
	}

	@Test
	void getAftersalesList_clampsUnauthorizedDistributorIdToJwtScope() {
		AftersalesAdminListQuery q = listQuery("99");

		service.getAftersalesList(q, request);

		assertThat(capturedDistributorIds()).containsExactlyInAnyOrder(10L, 20L);
	}

	@Test
	void getAftersalesList_keepsAuthorizedDistributorId() {
		AftersalesAdminListQuery q = listQuery("10");

		service.getAftersalesList(q, request);

		assertThat(capturedDistributorIds()).containsExactly(10L);
	}

	private static AftersalesAdminListQuery listQuery(String distributorId) {
		AftersalesAdminListQuery q = new AftersalesAdminListQuery();
		q.setPage(1);
		q.setPageSize(20);
		q.setDistributorId(distributorId);
		return q;
	}

	@SuppressWarnings("unchecked")
	private List<Long> capturedDistributorIds() {
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(aftersalesMapper).countAdminList(captor.capture());
		Object raw = captor.getValue().get("distributor_or_ids");
		assertThat(raw).isInstanceOf(List.class);
		return (List<Long>) raw;
	}
}
