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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.dto.RefundAdminListQuery;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesOfflineRefundMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10886：退款列表未授权 distributor_id 须收束到 JWT 门店，不得去掉门店条件。 */
class AftersalesRefundListDistributorScopeTest {

	private AftersalesRefundMapper refundMapper;
	private AftersalesRefundListService service;
	private HttpServletRequest request;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
	}

	@BeforeEach
	void setUp() {
		refundMapper = mock(AftersalesRefundMapper.class);
		when(refundMapper.selectCount(any())).thenReturn(0L);
		Page<AftersalesRefund> empty = new Page<>();
		empty.setRecords(List.of());
		when(refundMapper.selectPage(any(), any())).thenReturn(empty);
		service =
				new AftersalesRefundListService(
						refundMapper,
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
				List.of(Map.of("distributor_id", "10"), Map.of("distributor_id", "20")));
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);
	}

	@Test
	void getRefundList_clampsUnauthorizedDistributorIdToJwtScope() {
		RefundAdminListQuery q = new RefundAdminListQuery();
		q.setDistributorIdRaw("99");

		service.getRefundList(q, request);

		assertDistributorScopeIsJwtShops();
	}

	@Test
	void getRefundList_clampsEmptyDistributorIntersectionToJwtScope() {
		when(request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_IDS)).thenReturn(List.of(99L, 98L));
		RefundAdminListQuery q = new RefundAdminListQuery();

		service.getRefundList(q, request);

		assertDistributorScopeIsJwtShops();
	}

	@Test
	void getRefundList_keepsAuthorizedDistributorId() {
		RefundAdminListQuery q = new RefundAdminListQuery();
		q.setDistributorIdRaw("10");

		service.getRefundList(q, request);

		ArgumentCaptor<LambdaQueryWrapper<AftersalesRefund>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(refundMapper).selectCount(cap.capture());
		LambdaQueryWrapper<AftersalesRefund> wrapper = cap.getValue();
		assertThat(wrapper.getSqlSegment()).contains("distributor_id");
		assertThat(wrapper.getParamNameValuePairs().values()).contains(10L);
		assertThat(wrapper.getParamNameValuePairs().values()).doesNotContain(20L);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void assertDistributorScopeIsJwtShops() {
		ArgumentCaptor<LambdaQueryWrapper<AftersalesRefund>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(refundMapper).selectCount(cap.capture());
		LambdaQueryWrapper<AftersalesRefund> wrapper = cap.getValue();
		assertThat(wrapper.getSqlSegment()).contains("distributor_id");
		assertThat(wrapper.getParamNameValuePairs().values()).contains(10L, 20L);
	}
}
