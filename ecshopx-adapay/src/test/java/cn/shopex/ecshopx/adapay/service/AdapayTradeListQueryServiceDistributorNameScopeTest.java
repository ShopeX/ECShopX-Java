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

package cn.shopex.ecshopx.adapay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.adapay.mapper.AdapayTradeExportQueryMapper;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

/** ECX-10908：AdaPay 交易列表的 distributor_name 筛选不得覆盖店铺端 JWT 范围。 */
@ExtendWith(MockitoExtension.class)
class AdapayTradeListQueryServiceDistributorNameScopeTest {

	@Mock
	private AdapayTradeExportQueryMapper adapayTradeExportQueryMapper;

	@Mock
	private AdapayTradeDivFeeBatchService adapayTradeDivFeeBatchService;

	@Mock
	private DistributorListQueryService distributorListQueryService;

	@Mock
	private AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper;

	private AdapayTradeListQueryService service;

	@BeforeEach
	void setUp() {
		service =
				new AdapayTradeListQueryService(
						adapayTradeExportQueryMapper,
						adapayTradeDivFeeBatchService,
						distributorListQueryService,
						adapayDealerOperatorResolveHelper);
	}

	@Test
	void distributorOperator_withDistributorName_isRejectedWithoutQuery() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 11L);
		jwt.put("operator_id", 22L);
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("distributor_name", "东");

		assertThatThrownBy(() -> service.getTradelist(jwt, request))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("无权筛选其他店铺");

		verify(adapayTradeExportQueryMapper, never()).selectGroupedAll(any(), anyLong());
	}

	@Test
	void adminOperator_withDistributorName_returnsListResult() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 11L);
		jwt.put("operator_id", 22L);
		jwt.put("operator_type", "admin");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("distributor_name", "东");

		when(distributorListQueryService.listDistributorIdsByCompanyAndNameContains(11L, "东"))
				.thenReturn(List.of(100L, 200L));
		when(adapayTradeExportQueryMapper.selectGroupedAll(any(), anyLong()))
				.thenReturn(List.of());

		Map<String, Object> out = service.getTradelist(jwt, request);

		assertThat(out.get("list")).isInstanceOf(List.class);
		assertThat(out.get("total_count")).isEqualTo(0);
		verify(adapayTradeExportQueryMapper).selectGroupedAll(any(), anyLong());
	}
}
