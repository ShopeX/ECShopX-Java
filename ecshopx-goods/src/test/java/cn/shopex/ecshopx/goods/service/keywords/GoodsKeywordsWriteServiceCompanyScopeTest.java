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

package cn.shopex.ecshopx.goods.service.keywords;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Keywords;
import cn.shopex.ecshopx.goods.mapper.KeywordsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10889：外租户关键词 ID 不得被覆盖接管。 */
class GoodsKeywordsWriteServiceCompanyScopeTest {

	private KeywordsMapper keywordsMapper;
	private GoodsKeywordsWriteService service;

	@BeforeEach
	void setUp() {
		keywordsMapper = mock(KeywordsMapper.class);
		service = new GoodsKeywordsWriteService(keywordsMapper);
	}

	@Test
	void addKeywords_rejectsIdOwnedByAnotherCompany() {
		Keywords existing = new Keywords();
		existing.setId(7L);
		existing.setCompanyId(200L);
		existing.setContent("old");
		when(keywordsMapper.selectById(any())).thenReturn(existing);

		assertThatThrownBy(() -> service.addKeywords(100L, 7L, 0L, "new"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("记录不存在");

		verify(keywordsMapper, never()).updateById(any(Keywords.class));
		verify(keywordsMapper, never()).updateById(anyCollection());
		assertThat(existing.getCompanyId()).isEqualTo(200L);
		assertThat(existing.getContent()).isEqualTo("old");
	}

	@Test
	void addKeywords_updatesOwnCompanyRowWithoutChangingCompanyId() {
		Keywords existing = new Keywords();
		existing.setId(7L);
		existing.setCompanyId(100L);
		existing.setContent("old");
		when(keywordsMapper.selectById(any())).thenReturn(existing);
		when(keywordsMapper.updateById(any(Keywords.class))).thenReturn(1);

		service.addKeywords(100L, 7L, 3L, "new");

		ArgumentCaptor<Keywords> cap = ArgumentCaptor.forClass(Keywords.class);
		verify(keywordsMapper).updateById(cap.capture());
		assertThat(cap.getValue().getCompanyId()).isEqualTo(100L);
		assertThat(cap.getValue().getContent()).isEqualTo("new");
		assertThat(cap.getValue().getDistributorId()).isEqualTo(3L);
	}

	@Test
	void addKeywords_insertsWhenIdIsAbsent() {
		service.addKeywords(100L, null, 0L, "fresh");

		ArgumentCaptor<Keywords> cap = ArgumentCaptor.forClass(Keywords.class);
		verify(keywordsMapper).insert(cap.capture());
		assertThat(cap.getValue().getCompanyId()).isEqualTo(100L);
		assertThat(cap.getValue().getContent()).isEqualTo("fresh");
		verify(keywordsMapper, never()).updateById(any(Keywords.class));
	}
}
