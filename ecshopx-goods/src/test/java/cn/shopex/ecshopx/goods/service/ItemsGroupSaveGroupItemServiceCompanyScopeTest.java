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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsGroup;
import cn.shopex.ecshopx.goods.domain.ItemsGroupRelItem;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupRelItemMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10892：外租户 group_id 不得改写分组商品。 */
class ItemsGroupSaveGroupItemServiceCompanyScopeTest {

	private ItemsGroupMapper groupMapper;
	private ItemsGroupRelItemMapper relItemMapper;
	private ItemsGroupSaveGroupItemService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ItemsGroupRelItem.class);
	}

	@BeforeEach
	void setUp() {
		groupMapper = mock(ItemsGroupMapper.class);
		relItemMapper = mock(ItemsGroupRelItemMapper.class);
		service = new ItemsGroupSaveGroupItemService(groupMapper, relItemMapper);
	}

	@Test
	void saveGroupItem_rejectsForeignGroupIdWithoutWritingRelations() {
		ItemsGroup existing = group(7L, 200L);
		when(groupMapper.selectById(any())).thenReturn(existing);

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("group_id", 7L);
		input.put("regionauth_id", 1L);
		input.put("rel_goods_ids", List.of(11L));

		assertThatThrownBy(() -> service.saveGroupItem(100L, input))
				.isInstanceOf(ResourceException.class)
				.hasMessage("group_id错误");

		verify(relItemMapper, never()).selectList(any());
		verify(relItemMapper, never()).delete(any());
		verify(relItemMapper, never()).insert(any(ItemsGroupRelItem.class));
	}

	@Test
	void saveGroupItem_syncsGoodsForOwnCompanyGroup() {
		when(groupMapper.selectById(any())).thenReturn(group(7L, 100L));
		when(relItemMapper.selectList(any())).thenReturn(List.of());

		service.saveGroupItem(100L, input());

		ArgumentCaptor<ItemsGroupRelItem> cap = ArgumentCaptor.forClass(ItemsGroupRelItem.class);
		verify(relItemMapper).insert(cap.capture());
		assertThat(cap.getValue().getGroupId()).isEqualTo(7L);
		assertThat(cap.getValue().getGoodsId()).isEqualTo(11L);
		assertThat(cap.getValue().getCompanyId()).isEqualTo(100L);
	}

	private static ItemsGroup group(long id, long companyId) {
		ItemsGroup existing = new ItemsGroup();
		existing.setId(id);
		existing.setCompanyId(companyId);
		return existing;
	}

	private static Map<String, Object> input() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("group_id", 7L);
		input.put("regionauth_id", 1L);
		input.put("rel_goods_ids", List.of(11L));
		return input;
	}
}
