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
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10948：商品标签详情只返回当前公司的标签。 */
@ExtendWith(MockitoExtension.class)
class ItemsTagsQueryServiceCompanyScopeTest {

	@Mock
	private ItemsTagsRepository itemsTagsRepository;

	@Mock
	private ItemsRelTagsRepository itemsRelTagsRepository;

	@Mock
	private ItemsTagsMultiLangApplier itemsTagsMultiLangApplier;

	@Mock
	private DistributorMapper distributorMapper;

	private ItemsTagsQueryService service;

	@BeforeEach
	void setUp() {
		service =
				new ItemsTagsQueryService(
						itemsTagsRepository, itemsRelTagsRepository, itemsTagsMultiLangApplier, distributorMapper);
	}

	private static ItemsTags tag(long tagId, Long companyId, String tagName) {
		ItemsTags entity = new ItemsTags();
		entity.setTagId(tagId);
		entity.setCompanyId(companyId);
		entity.setTagName(tagName);
		return entity;
	}

	@Test
	void foreignCompany_returnsEmptyList() {
		when(itemsTagsRepository.selectById(77L)).thenReturn(tag(77L, 99L, "他司标签"));

		assertThat(service.getTagInfoByTagId(11L, 77L)).isEqualTo(List.of());
	}

	@Test
	void missingCompany_returnsEmptyList() {
		when(itemsTagsRepository.selectById(77L)).thenReturn(tag(77L, null, "无公司标签"));

		assertThat(service.getTagInfoByTagId(11L, 77L)).isEqualTo(List.of());
	}

	@Test
	void ownCompany_returnsTagName() {
		when(itemsTagsRepository.selectById(77L)).thenReturn(tag(77L, 11L, "本司标签"));

		Object out = service.getTagInfoByTagId(11L, 77L);

		assertThat(out).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) out).get("tag_name")).isEqualTo("本司标签");
		assertThat(((Map<?, ?>) out).get("company_id")).isEqualTo(11L);
	}
}
