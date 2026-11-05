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

package cn.shopex.ecshopx.employeepurchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityItemInsertMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10910：添加员工购活动商品必须限定当前租户的活动。 */
@ExtendWith(MockitoExtension.class)
class EmployeePurchaseActivityItemWriteServiceCompanyScopeTest {

	@Mock
	private ActivitiesMapper activitiesMapper;

	@Mock
	private ItemsMapper itemsMapper;

	@Mock
	private EmployeePurchaseActivityItemInsertMapper employeePurchaseActivityItemInsertMapper;

	@Mock
	private EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService;

	private EmployeePurchaseActivityItemWriteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Activities.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Items.class);
	}

	@BeforeEach
	void setUp() {
		service =
				new EmployeePurchaseActivityItemWriteService(
						activitiesMapper,
						itemsMapper,
						employeePurchaseActivityItemInsertMapper,
						employeePurchaseActivityItemsCategoryRedisService);
	}

	@Test
	void writeOneBatch_queriesActivityWithCompanyScope() {
		// 他租户活动存在（按 id 能查到），但带 companyId 条件的查询不应返回它
		when(activitiesMapper.selectOne(any())).thenReturn(null);

		assertThatThrownBy(() -> service.writeOneBatch(77L, 11L, List.of(101L), 0L, null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("活动不存在");

		// 关键断言：活动查询 SQL 必须绑定 company_id
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<Activities>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(activitiesMapper).selectOne(captor.capture());
		assertThat(captor.getValue().getSqlSegment()).containsIgnoringCase("company_id");
	}

	@Test
	void writeOneBatch_foreignCompanyActivity_isRejectedWithoutInsert() {
		// 模拟仅按 id 查询时的他租户活动——正确实现（id+company 双条件）不会返回它
		when(activitiesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							Wrapper<Activities> w = inv.getArgument(0);
							if (w.getSqlSegment() != null
									&& w.getSqlSegment().toLowerCase().contains("company_id")) {
								return null;
							}
							Activities foreign = new Activities();
							foreign.setId(77L);
							foreign.setCompanyId(99L);
							return foreign;
						});

		assertThatThrownBy(() -> service.writeOneBatch(77L, 11L, List.of(101L), 0L, null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("活动不存在");

		verify(employeePurchaseActivityItemInsertMapper, never())
				.insertIgnoreActivityItems(anyList());
		verify(employeePurchaseActivityItemInsertMapper, never())
				.insertIgnoreActivityGoods(anyList());
		verify(employeePurchaseActivityItemsCategoryRedisService, never())
				.store(anyLong(), anyLong(), anyLong());
	}

	@Test
	void writeOneBatch_ownCompanyActivity_insertsItemsAndGoods() {
		Activities activity = new Activities();
		activity.setId(77L);
		activity.setCompanyId(11L);
		when(activitiesMapper.selectOne(any())).thenReturn(activity);

		Items item = new Items();
		item.setItemId(101L);
		item.setGoodsId(201L);
		item.setPrice(9900);
		item.setStore(50);
		when(itemsMapper.selectList(any())).thenReturn(List.of(item));

		service.writeOneBatch(77L, 11L, List.of(101L), 0L, null);

		verify(employeePurchaseActivityItemInsertMapper).insertIgnoreActivityItems(anyList());
		verify(employeePurchaseActivityItemInsertMapper).insertIgnoreActivityGoods(anyList());
		verify(employeePurchaseActivityItemsCategoryRedisService).store(11L, 77L, 0L);
	}
}
