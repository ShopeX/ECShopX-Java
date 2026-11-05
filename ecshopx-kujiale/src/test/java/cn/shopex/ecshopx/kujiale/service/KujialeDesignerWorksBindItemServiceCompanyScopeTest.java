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

package cn.shopex.ecshopx.kujiale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.kujiale.api.admin.v1.request.KujialeDesignerWorksBindItemRequest;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksItemRel;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksItemRelMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10893：外租户商品不得写入酷家乐设计绑定。 */
class KujialeDesignerWorksBindItemServiceCompanyScopeTest {

	private ItemsRepository itemsRepository;
	private KujialeDesignerWorksItemRelMapper relMapper;
	private KujialeDesignerWorksBindItemService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), KujialeDesignerWorksItemRel.class);
	}

	@BeforeEach
	void setUp() {
		itemsRepository = mock(ItemsRepository.class);
		relMapper = mock(KujialeDesignerWorksItemRelMapper.class);
		service = new KujialeDesignerWorksBindItemService(itemsRepository, relMapper);
	}

	@Test
	void bind_recordsForeignItemAsMissingAndDoesNotInsert() {
		Items foreign = new Items();
		foreign.setItemId(7L);
		foreign.setCompanyId(200L);
		foreign.setGoodsBn("BN-7");
		when(itemsRepository.findByItemId(7L)).thenReturn(foreign);

		Map<String, Object> result = service.bind(request(7L, "design-1"), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of("商品不存在: 7"));
		assertThat(result.get("success_count")).isEqualTo(0);
		verify(relMapper, never()).insert(any(KujialeDesignerWorksItemRel.class));
	}

	@Test
	void bind_insertsItemThatBelongsToCallerCompany() {
		Items own = new Items();
		own.setItemId(7L);
		own.setCompanyId(100L);
		own.setGoodsBn("BN-7");
		when(itemsRepository.findByItemId(7L)).thenReturn(own);
		when(relMapper.selectOne(any())).thenReturn(null);

		Map<String, Object> result = service.bind(request(7L, "design-1"), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of());
		assertThat(result.get("success_count")).isEqualTo(1);
		ArgumentCaptor<KujialeDesignerWorksItemRel> inserted = ArgumentCaptor.forClass(KujialeDesignerWorksItemRel.class);
		verify(relMapper).insert(inserted.capture());
		assertThat(inserted.getValue().getItemId()).isEqualTo(7L);
		assertThat(inserted.getValue().getDesignId()).isEqualTo("design-1");
		assertThat(inserted.getValue().getGoodsBn()).isEqualTo("BN-7");
	}

	@Test
	void bind_recordsMissingItemWithoutInsert() {
		when(itemsRepository.findByItemId(8L)).thenReturn(null);

		Map<String, Object> result = service.bind(request(8L, "design-1"), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of("商品不存在: 8"));
		assertThat(result.get("success_count")).isEqualTo(0);
		verify(relMapper, never()).insert(any(KujialeDesignerWorksItemRel.class));
	}

	private static KujialeDesignerWorksBindItemRequest request(long itemId, String designId) {
		KujialeDesignerWorksBindItemRequest request = new KujialeDesignerWorksBindItemRequest();
		request.setItemId(JsonNodeFactory.instance.numberNode(itemId));
		request.setDesignId(JsonNodeFactory.instance.textNode(designId));
		return request;
	}
}
