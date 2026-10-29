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
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersSaasErpDeliveryOrderAdapterTest {

	private static final long rowCompanyId = 11L;
	private static final long orderId = 9001L;

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	@Mock
	NormalOrdersItemsMapper normalOrdersItemsMapper;

	OrdersSaasErpDeliveryOrderAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
	}

	@BeforeEach
	void setUp() {
		adapter = new OrdersSaasErpDeliveryOrderAdapter(normalOrdersMapper, normalOrdersItemsMapper);
	}

	@Test
	void load_withZeroCallbackCompanyId_stillReturnsOrderCompanyId() {
		NormalOrders tradeInfo = new NormalOrders();
		tradeInfo.setOrderId(orderId);
		tradeInfo.setCompanyId(rowCompanyId);
		tradeInfo.setDeliveryStatus("PENDING");
		when(normalOrdersMapper.selectOne(any())).thenReturn(tradeInfo);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());

		Optional<Map<String, Object>> loaded = adapter.load(0L, String.valueOf(orderId));

		assertThat(loaded).isPresent();
		assertThat(loaded.get().get("company_id")).isEqualTo(rowCompanyId);
	}
}
