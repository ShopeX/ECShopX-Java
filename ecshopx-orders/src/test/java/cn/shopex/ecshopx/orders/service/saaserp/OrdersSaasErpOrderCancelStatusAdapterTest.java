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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersSaasErpOrderCancelStatusAdapterTest {

	private static final long companyId = 10L;
	private static final long orderId = 9L;

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	OrdersSaasErpOrderCancelStatusAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
	}

	@BeforeEach
	void setUp() {
		adapter = new OrdersSaasErpOrderCancelStatusAdapter(normalOrdersMapper);
	}

	@Test
	void findCancelStatus_withPositiveCompanyId_queriesOrderAndCompany() {
		NormalOrders row = new NormalOrders();
		row.setOrderId(orderId);
		row.setCompanyId(companyId);
		row.setCancelStatus("SUCCESS");
		when(normalOrdersMapper.selectOne(any())).thenReturn(row);

		Optional<String> found = adapter.findCancelStatus(companyId, String.valueOf(orderId));

		assertThat(found).contains("SUCCESS");
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper).selectOne(captor.capture());
		String sql = captor.getValue().getCustomSqlSegment();
		assertThat(sql).contains("order_id");
		assertThat(sql).contains("company_id");
	}

	@Test
	void findCancelStatus_withZeroCompanyId_omitsCompanyCondition() {
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);

		Optional<String> found = adapter.findCancelStatus(0L, String.valueOf(orderId));

		assertThat(found).isEmpty();
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper).selectOne(captor.capture());
		String sql = captor.getValue().getCustomSqlSegment();
		assertThat(sql).contains("order_id");
		assertThat(sql).doesNotContain("company_id");
	}

	@Test
	void findCancelStatus_whenRowMissing_returnsEmpty() {
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);

		Optional<String> found = adapter.findCancelStatus(companyId, String.valueOf(orderId));

		assertThat(found).isEmpty();
	}
}
