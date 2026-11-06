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
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Keywords;
import cn.shopex.ecshopx.goods.mapper.KeywordsMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10947：关键词详情只返回当前公司的记录。 */
@ExtendWith(MockitoExtension.class)
class GoodsKeywordsQueryServiceCompanyScopeTest {

	@Mock
	private KeywordsMapper keywordsMapper;

	private GoodsKeywordsQueryService service;

	@BeforeEach
	void setUp() {
		service = new GoodsKeywordsQueryService(keywordsMapper);
	}

	private static Keywords keyword(long id, Long companyId, String content) {
		Keywords row = new Keywords();
		row.setId(id);
		row.setCompanyId(companyId);
		row.setContent(content);
		return row;
	}

	@Test
	void foreignCompany_returnsNull() {
		when(keywordsMapper.selectById(77L)).thenReturn(keyword(77L, 99L, "他司关键词"));

		assertThat(service.getInfoById(11L, 77L)).isNull();
	}

	@Test
	void missingCompany_returnsNull() {
		when(keywordsMapper.selectById(77L)).thenReturn(keyword(77L, null, "无公司关键词"));

		assertThat(service.getInfoById(11L, 77L)).isNull();
	}

	@Test
	void ownCompany_returnsContent() {
		when(keywordsMapper.selectById(77L)).thenReturn(keyword(77L, 11L, "本司关键词"));

		Map<String, Object> row = service.getInfoById(11L, 77L);

		assertThat(row).isNotNull();
		assertThat(row.get("content")).isEqualTo("本司关键词");
		assertThat(row.get("company_id")).isEqualTo(11L);
	}
}
