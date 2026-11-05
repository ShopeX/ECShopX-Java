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

package cn.shopex.ecshopx.orders.service.epidemic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.mapper.OrderEpidemicRegisterMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEpidemicRegisterFrontDelServiceScopeTest {

	@Mock
	private OrderEpidemicRegisterMapper orderEpidemicRegisterMapper;

	private OrderEpidemicRegisterFrontDelService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), OrderEpidemicRegister.class);
	}

	@BeforeEach
	void setUp() {
		service = new OrderEpidemicRegisterFrontDelService(orderEpidemicRegisterMapper);
	}

	@Test
	void del_rejectsForeignOwner_beforeWriting() {
		when(orderEpidemicRegisterMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertThatThrownBy(() -> service.delEpidemicRegister("42", 100L, 7L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("未查询到更新数据");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<OrderEpidemicRegister>> captor =
				ArgumentCaptor.forClass(Wrapper.class);
		verify(orderEpidemicRegisterMapper).selectOne(captor.capture());
		String sql = captor.getValue().getSqlSegment();
		assertThat(sql).containsIgnoringCase("company_id");
		assertThat(sql).containsIgnoringCase("user_id");
		verify(orderEpidemicRegisterMapper, never()).update(any(), any(Wrapper.class));
		verify(orderEpidemicRegisterMapper, never()).updateById(any(OrderEpidemicRegister.class));
	}

	@Test
	void del_allowsOwnCompanyUser_andScopesWrite() {
		OrderEpidemicRegister existing = new OrderEpidemicRegister();
		existing.setId(42L);
		existing.setCompanyId(100);
		existing.setUserId(7L);
		existing.setIsUse(1);
		when(orderEpidemicRegisterMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
		when(orderEpidemicRegisterMapper.update(any(), any(Wrapper.class))).thenReturn(1);

		service.delEpidemicRegister("42", 100L, 7L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<OrderEpidemicRegister>> updateCaptor =
				ArgumentCaptor.forClass(Wrapper.class);
		verify(orderEpidemicRegisterMapper).update(any(), updateCaptor.capture());
		String sql = updateCaptor.getValue().getSqlSegment();
		assertThat(sql).containsIgnoringCase("company_id");
		assertThat(sql).containsIgnoringCase("user_id");
		verify(orderEpidemicRegisterMapper, never()).updateById(any(OrderEpidemicRegister.class));
	}
}
