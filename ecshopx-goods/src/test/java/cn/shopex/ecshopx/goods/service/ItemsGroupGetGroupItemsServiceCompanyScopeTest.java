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

package cn.shopex.ecshopx.goods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.ItemsGroupRelItem;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupRelItemMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10946：分组商品列表只返回当前公司的商品 ID。 */
@ExtendWith(MockitoExtension.class)
class ItemsGroupGetGroupItemsServiceCompanyScopeTest {

	@Mock
	private ItemsGroupRelItemMapper itemsGroupRelItemMapper;

	private ItemsGroupGetGroupItemsService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ItemsGroupRelItem.class);
	}

	@BeforeEach
	void setUp() {
		service = new ItemsGroupGetGroupItemsService(itemsGroupRelItemMapper);
	}

	private static ItemsGroupRelItem goods(long goodsId) {
		ItemsGroupRelItem row = new ItemsGroupRelItem();
		row.setGoodsId(goodsId);
		return row;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void stubPage(boolean returnRowWhenCompanyScoped) {
		when(itemsGroupRelItemMapper.selectPage(any(), any()))
				.thenAnswer(
						inv -> {
							Page<ItemsGroupRelItem> page = inv.getArgument(0);
							Wrapper<?> wrapper = inv.getArgument(1);
							String sql = wrapper.getSqlSegment() == null ? "" : wrapper.getSqlSegment();
							boolean scoped = sql.contains("company_id");
							if (scoped == returnRowWhenCompanyScoped) {
								page.setRecords(List.of(goods(88L)));
							} else {
								page.setRecords(List.of());
							}
							return page;
						});
	}

	@Test
	void foreignCompanyGroup_returnsNoGoodsIds() {
		stubPage(false);

		assertThat(service.listGoodsIdsByGroupId(11L, 5L, 1, 20)).isEmpty();
	}

	@Test
	void relationWithoutCompany_returnsNoGoodsIds() {
		stubPage(false);

		assertThat(service.listGoodsIdsByGroupId(11L, 5L, 1, 20)).isEmpty();
	}

	@Test
	void ownCompanyGroup_returnsGoodsId() {
		stubPage(true);

		assertThat(service.listGoodsIdsByGroupId(11L, 5L, 1, 20)).containsExactly(88L);
	}
}
