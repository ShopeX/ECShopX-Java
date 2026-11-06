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

package cn.shopex.ecshopx.bspay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.bspay.mapper.BspayTradeExportQueryMapper;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

/** ECX-10941：BsPay 交易列表的店名筛选不得覆盖门店 JWT 范围。 */
@ExtendWith(MockitoExtension.class)
class BspayTradeListQueryServiceDistributorNameScopeTest {

	@Mock
	private BspayTradeExportQueryMapper bspayTradeExportQueryMapper;

	@Mock
	private BspayTradeDivFeeBatchService bspayTradeDivFeeBatchService;

	@Mock
	private DistributorListQueryService distributorListQueryService;

	@Mock
	private BsPayOperatorResolveService bsPayOperatorResolveService;

	private BspayTradeListQueryService service;

	@BeforeEach
	void setUp() {
		service = new BspayTradeListQueryService(
				bspayTradeExportQueryMapper,
				bspayTradeDivFeeBatchService,
				distributorListQueryService,
				bsPayOperatorResolveService);
	}

	@Test
	void distributorOperator_withDistributorName_isRejectedWithoutLookupOrTradeQuery() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 11L);
		jwt.put("operator_id", 22L);
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("distributor_name", "其他门店");

		assertThatThrownBy(() -> service.getTradelist(jwt, request))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("无权筛选其他店铺");

		verify(distributorListQueryService, never())
				.listDistributorIdsByCompanyAndNameContains(anyLong(), anyString());
		verify(bspayTradeExportQueryMapper, never()).selectGroupedAll(any(), anyLong());
		verify(bspayTradeExportQueryMapper, never()).selectGroupedPage(any(), anyLong(), anyLong(), anyInt());
	}

	@Test
	void distributorOperator_withoutDistributorName_queriesJwtStore() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 11L);
		jwt.put("operator_id", 22L);
		jwt.put("operator_type", "distributor");
		jwt.put("distributor_id", 100L);
		when(bspayTradeExportQueryMapper.selectGroupedAll(any(), anyLong())).thenReturn(List.of());

		service.getTradelist(jwt, new MockHttpServletRequest());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> filterCaptor = ArgumentCaptor.forClass(Map.class);
		verify(bspayTradeExportQueryMapper).selectGroupedAll(filterCaptor.capture(), anyLong());
		assertThat(filterCaptor.getValue().get("distributor_id")).isEqualTo(100L);
		verify(distributorListQueryService, never())
				.listDistributorIdsByCompanyAndNameContains(anyLong(), anyString());
	}

	@Test
	void adminOperator_withDistributorName_queriesMatchedStores() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 11L);
		jwt.put("operator_id", 22L);
		jwt.put("operator_type", "admin");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("distributor_name", "东");
		when(distributorListQueryService.listDistributorIdsByCompanyAndNameContains(11L, "东"))
				.thenReturn(List.of(100L, 200L));
		when(bspayTradeExportQueryMapper.selectGroupedAll(any(), anyLong())).thenReturn(List.of());

		service.getTradelist(jwt, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> filterCaptor = ArgumentCaptor.forClass(Map.class);
		verify(bspayTradeExportQueryMapper).selectGroupedAll(filterCaptor.capture(), anyLong());
		assertThat(filterCaptor.getValue().get("distributor_id")).isEqualTo(List.of(100L, 200L));
	}
}
