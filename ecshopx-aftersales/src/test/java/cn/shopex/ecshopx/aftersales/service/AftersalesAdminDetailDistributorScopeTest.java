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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailSalespersonLookupPort;
import cn.shopex.ecshopx.common.port.companys.OperatorInfoReadPort;
import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderServiceOrderDataReadPort;
import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorRowByOperatorIdsReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import jakarta.servlet.http.HttpServletRequest;
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

/** ECX-10936：管理端售后详情必须校验 JWT 门店范围。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersalesAdminDetailDistributorScopeTest {

	private AftersalesMapper aftersalesMapper;
	private AftersalesDetailMapper aftersalesDetailMapper;
	private JushuitanSettingReadPort jushuitanSettingReadPort;
	private AftersalesAdminDetailService service;
	private HttpServletRequest request;

	@BeforeAll
	static void initTables() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@BeforeEach
	void setUp() {
		aftersalesMapper = mock(AftersalesMapper.class);
		aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		OrderNormalOrderServiceOrderDataReadPort orderDataReadPort =
				mock(OrderNormalOrderServiceOrderDataReadPort.class);
		service = new AftersalesAdminDetailService(
				aftersalesMapper,
				aftersalesDetailMapper,
				mock(AftersalesRefundService.class),
				mock(DistributorDefaultAftersalesAddressReadPort.class),
				mock(AdminOrderDetailSalespersonLookupPort.class),
				orderDataReadPort,
				mock(OrderNormalOrderItemsReadPort.class),
				mock(DistributorGetInfoSimpleByDistributorIdPort.class),
				jushuitanSettingReadPort,
				mock(OperatorInfoReadPort.class),
				mock(SupplierOperatorRowByOperatorIdsReadPort.class),
				new AftersalesAdminAppInfoBuilder(orderDataReadPort),
				new AftersalesAdminDetailPhpParityApplier());
		request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(Map.of("company_id", 100L, "distributor_ids", List.of(Map.of("distributor_id", 10L))));
	}

	private void stubJwt(Map<String, Object> jwt) {
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);
	}

	private void stubAftersales(long distributorId) {
		Aftersales row = new Aftersales();
		row.setAftersalesBn(555001L);
		row.setCompanyId(100L);
		row.setDistributorId(distributorId);
		when(aftersalesMapper.selectOne(any())).thenReturn(row);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());
		when(jushuitanSettingReadPort.readJushuitanSetting(100L)).thenReturn(Map.of());
	}

	@Test
	void storeOperator_viewingForeignStoreAftersales_isRejectedBeforeLoadingDetails() {
		Aftersales row = new Aftersales();
		row.setAftersalesBn(555001L);
		row.setCompanyId(100L);
		row.setDistributorId(99L);
		when(aftersalesMapper.selectOne(any())).thenReturn(row);

		assertThatThrownBy(() -> service.getAftersalesDetail(100L, "555001", request))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("店铺权限不足");

		verify(aftersalesDetailMapper, never()).selectList(any());
	}

	@Test
	void storeOperator_viewingOwnStoreAftersales_returnsDetail() {
		stubAftersales(10L);

		Map<String, Object> result = service.getAftersalesDetail(100L, "555001", request);

		assertThat(result.get("distributor_id")).isEqualTo("10");
	}

	@Test
	void multiStoreOperator_viewingAuthorizedStoreAftersales_returnsDetail() {
		stubJwt(Map.of(
				"company_id", 100L,
				"distributor_id", 10L,
				"distributor_ids", List.of(Map.of("distributor_id", 20L))));
		stubAftersales(20L);

		Map<String, Object> result = service.getAftersalesDetail(100L, "555001", request);

		assertThat(result.get("distributor_id")).isEqualTo("20");
	}

	@Test
	void admin_viewingAnyStoreAftersales_returnsDetail() {
		stubJwt(Map.of("company_id", 100L, "operator_type", "admin"));
		stubAftersales(99L);

		Map<String, Object> result = service.getAftersalesDetail(100L, "555001", request);

		assertThat(result.get("distributor_id")).isEqualTo("99");
	}
}
