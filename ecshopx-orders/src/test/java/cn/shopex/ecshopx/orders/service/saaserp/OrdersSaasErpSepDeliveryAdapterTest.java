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

package cn.shopex.ecshopx.orders.service.saaserp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersSaasErpSepDeliveryAdapterTest {

	private static final long companyId = 11L;
	private static final long orderId = 9001L;

	@Mock
	OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;

	OrdersSaasErpSepDeliveryAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
	}

	@BeforeEach
	void setUp() {
		adapter =
				new OrdersSaasErpSepDeliveryAdapter(
						orderAssociationsMapper, adminNormalOrderDeliveryCoreService);
	}

	@Test
	void deliver_callsDeliveryNormalPhysicalOnceAndReturnsTrue() {
		Map<String, Object> sepParams = new LinkedHashMap<>();
		sepParams.put("company_id", companyId);
		sepParams.put("order_id", orderId);
		sepParams.put("delivery_type", "sep");

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderId);
		when(orderAssociationsMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(assoc);

		Object result = adapter.deliver(sepParams);

		assertThat(result).isEqualTo(Boolean.TRUE);
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(adminNormalOrderDeliveryCoreService)
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assoc), eq("normal"));
		assertThat(paramsCaptor.getValue()).isSameAs(sepParams);
	}

	@Test
	void deliver_propagatesResourceExceptionFromCore() {
		Map<String, Object> sepParams = new LinkedHashMap<>();
		sepParams.put("company_id", companyId);
		sepParams.put("order_id", orderId);

		OrderAssociations assoc = new OrderAssociations();
		when(orderAssociationsMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(assoc);

		ResourceException ex = new ResourceException("发货失败");
		doThrow(ex)
				.when(adminNormalOrderDeliveryCoreService)
				.deliveryNormalPhysical(sepParams, assoc, "normal");

		assertThatThrownBy(() -> adapter.deliver(sepParams)).isSameAs(ex);
	}
}
