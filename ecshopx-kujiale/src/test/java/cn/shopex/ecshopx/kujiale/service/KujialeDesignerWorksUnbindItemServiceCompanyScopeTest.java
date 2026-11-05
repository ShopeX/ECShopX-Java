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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksItemRel;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksItemRelMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ECX-10894：外租户商品的酷家乐设计绑定不得被解绑删除。 */
class KujialeDesignerWorksUnbindItemServiceCompanyScopeTest {

	private ItemsRepository itemsRepository;
	private KujialeDesignerWorksItemRelMapper relMapper;
	private KujialeDesignerWorksUnbindItemService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), KujialeDesignerWorksItemRel.class);
	}

	@BeforeEach
	void setUp() {
		itemsRepository = mock(ItemsRepository.class);
		relMapper = mock(KujialeDesignerWorksItemRelMapper.class);
		service = new KujialeDesignerWorksUnbindItemService(itemsRepository, relMapper);
	}

	@Test
	void unbind_recordsForeignItemAsMissingAndDoesNotDelete() {
		when(relMapper.selectOne(any())).thenReturn(rel(9L));
		when(itemsRepository.findByItemId(7L)).thenReturn(item(7L, 200L));

		Map<String, Object> result = service.unbind(body(7L, "design-1"), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of("第1条数据的绑定关系不存在"));
		assertThat(result.get("success_count")).isEqualTo(0);
		assertThat(result.get("fail_count")).isEqualTo(1);
		verify(relMapper, never()).deleteById(eq(9L));
	}

	@Test
	void unbind_deletesItemThatBelongsToCallerCompany() {
		when(relMapper.selectOne(any())).thenReturn(rel(9L));
		when(itemsRepository.findByItemId(7L)).thenReturn(item(7L, 100L));

		Map<String, Object> result = service.unbind(body(7L, "design-1"), 100L);

		assertThat(result.get("errors")).isNull();
		assertThat(result.get("success_count")).isEqualTo(1);
		assertThat(result.get("fail_count")).isEqualTo(0);
		verify(relMapper).deleteById(eq(9L));
	}

	@Test
	void unbind_recordsMissingItemAsMissingAndDoesNotDelete() {
		when(relMapper.selectOne(any())).thenReturn(rel(9L));
		when(itemsRepository.findByItemId(7L)).thenReturn(null);

		Map<String, Object> result = service.unbind(body(7L, "design-1"), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of("第1条数据的绑定关系不存在"));
		assertThat(result.get("success_count")).isEqualTo(0);
		verify(relMapper, never()).deleteById(eq(9L));
	}

	@Test
	void unbind_recordsNullCompanyItemAsMissingAndDoesNotDelete() {
		when(relMapper.selectOne(any())).thenReturn(rel(9L));
		when(itemsRepository.findByItemId(7L)).thenReturn(item(7L, null));

		Map<String, Object> result = service.unbind(body(7L, "design-1"), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of("第1条数据的绑定关系不存在"));
		verify(relMapper, never()).deleteById(eq(9L));
	}

	@Test
	void unbind_keepsOwnRowWhenBatchContainsForeignItem() {
		when(relMapper.selectOne(any())).thenReturn(rel(9L), rel(10L));
		when(itemsRepository.findByItemId(7L)).thenReturn(item(7L, 200L));
		when(itemsRepository.findByItemId(8L)).thenReturn(item(8L, 100L));

		Map<String, Object> result = service.unbind(batch(row(7L, "design-1"), row(8L, "design-2")), 100L);

		assertThat(result.get("errors")).isEqualTo(List.of("第1条数据的绑定关系不存在"));
		assertThat(result.get("success_count")).isEqualTo(1);
		assertThat(result.get("fail_count")).isEqualTo(1);
		verify(relMapper, never()).deleteById(eq(9L));
		verify(relMapper).deleteById(eq(10L));
	}

	private static KujialeDesignerWorksItemRel rel(long id) {
		KujialeDesignerWorksItemRel rel = new KujialeDesignerWorksItemRel();
		rel.setId(id);
		return rel;
	}

	private static Items item(long itemId, Long companyId) {
		Items item = new Items();
		item.setItemId(itemId);
		item.setCompanyId(companyId);
		return item;
	}

	private static JsonNode body(long itemId, String designId) {
		return batch(row(itemId, designId));
	}

	private static JsonNode row(long itemId, String designId) {
		return JsonNodeFactory.instance.objectNode().put("item_id", itemId).put("design_id", designId);
	}

	private static JsonNode batch(JsonNode... rows) {
		var body = JsonNodeFactory.instance.arrayNode();
		for (JsonNode row : rows) {
			body.add(row);
		}
		return body;
	}
}
