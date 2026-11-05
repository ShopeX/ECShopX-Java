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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.RefundLogExportQuery;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesOfflineRefundMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ECX-10879：退款导出未授权 distributor_id 须收束到 JWT 门店，不得去掉门店条件。 */
class AftersalesRefundLogExportDistributorScopeTest {

	private AftersalesRefundListService service;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		service =
				new AftersalesRefundListService(
						mock(AftersalesRefundMapper.class),
						mock(AftersalesOfflineRefundMapper.class),
						mock(MembersUserIdByMobileLookupService.class),
						mock(DistributorGetInfoSimpleByDistributorIdPort.class));
		request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 100L);
		jwt.put("operator_type", "staff");
		jwt.put("operator_id", 1L);
		jwt.put(
				"distributor_ids",
				List.of(
						Map.of("distributor_id", "10"),
						Map.of("distributor_id", "20")));
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);
	}

	@Test
	void buildRefundLogExportFilter_clampsUnauthorizedDistributorIdToJwtScope() {
		RefundLogExportQuery q = new RefundLogExportQuery();
		q.setDistributorIdRaw("99");

		LinkedHashMap<String, Object> filter = service.buildRefundLogExportFilter(q, request);

		@SuppressWarnings("unchecked")
		List<Long> scope = (List<Long>) filter.get("distributor_id");
		assertThat(scope).containsExactlyInAnyOrder(10L, 20L);
	}

	@Test
	void buildRefundLogExportFilter_keepsAuthorizedDistributorId() {
		RefundLogExportQuery q = new RefundLogExportQuery();
		q.setDistributorIdRaw("10");

		LinkedHashMap<String, Object> filter = service.buildRefundLogExportFilter(q, request);

		assertThat(filter.get("distributor_id")).isEqualTo(10L);
	}
}
