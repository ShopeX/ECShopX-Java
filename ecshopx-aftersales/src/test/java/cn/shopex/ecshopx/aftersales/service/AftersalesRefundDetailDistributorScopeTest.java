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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesOfflineRefundMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** ECX-10922：退款单详情必须校验 JWT 门店范围（对齐 PHP AftersalesDistributorGate）。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersalesRefundDetailDistributorScopeTest {

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
		service =
				new AftersalesRefundListService(
						refundMapper,
						mock(AftersalesOfflineRefundMapper.class),
						mock(MembersUserIdByMobileLookupService.class),
						mock(DistributorGetInfoSimpleByDistributorIdPort.class));
		request = mock(HttpServletRequest.class);
	}

	private void stubJwt(Map<String, Object> jwt) {
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);
	}

	private static Map<String, Object> hqJwt() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", 100L);
		jwt.put("operator_type", "staff");
		jwt.put("operator_id", 1L);
		return jwt;
	}

	private static Map<String, Object> storeJwt() {
		Map<String, Object> jwt = hqJwt();
		jwt.put(
				"distributor_ids",
				List.of(Map.of("distributor_id", "10"), Map.of("distributor_id", "20")));
		return jwt;
	}

	private static AftersalesRefund refund(long distributorId) {
		AftersalesRefund r = new AftersalesRefund();
		r.setRefundBn(555001L);
		r.setCompanyId(100L);
		r.setDistributorId(distributorId);
		r.setRefundFee(9900);
		r.setRefundChannel("online");
		return r;
	}

	@Test
	void storeOperator_viewingForeignStoreRefund_isRejected() {
		stubJwt(storeJwt());
		when(refundMapper.selectOne(any())).thenReturn(refund(99L));

		assertThatThrownBy(() -> service.getRefundsDetail("555001", request))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权操作该售后单");
	}

	@Test
	void storeOperator_viewingOwnStoreRefund_returnsDetail() {
		stubJwt(storeJwt());
		when(refundMapper.selectOne(any())).thenReturn(refund(10L));

		Object out = service.getRefundsDetail("555001", request);

		assertThat(out).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> row = (Map<String, Object>) out;
		assertThat(row.get("distributor_id")).isEqualTo(10L);
	}

	@Test
	void hqOperator_viewingAnyRefund_returnsDetail() {
		stubJwt(hqJwt());
		when(refundMapper.selectOne(any())).thenReturn(refund(99L));

		Object out = service.getRefundsDetail("555001", request);

		assertThat(out).isInstanceOf(Map.class);
	}

	@Test
	void storeOperator_viewingSelfOperatedRefund_isRejected() {
		// 对齐 PHP gate：distributor_id=0（平台自营）不在 [10,20] 范围内也拒绝
		stubJwt(storeJwt());
		AftersalesRefund selfOperated = refund(0L);
		selfOperated.setDistributorId(null);
		when(refundMapper.selectOne(any())).thenReturn(selfOperated);

		assertThatThrownBy(() -> service.getRefundsDetail("555001", request))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权操作该售后单");
	}
}
