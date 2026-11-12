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

package cn.shopex.ecshopx.goods.search.synonym;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchSynonymImportRowServiceTest {

	@Mock
	private SearchSynonymService searchSynonymService;

	private SearchSynonymImportRowService service;

	@BeforeEach
	void setUp() {
		service = new SearchSynonymImportRowService(searchSynonymService);
	}

	@Test
	void applyRow_upsertsNormalizedWords() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("group_key", " brand ");
		row.put("word_1", "手机");
		row.put("word_2", " 手机 ");
		row.put("word_3", "移动电话");
		service.applyRow(10L, row);
		verify(searchSynonymService).upsertGroupWithoutEsReload(10L, "brand", List.of("手机", "移动电话"));
	}

	@Test
	void applyRow_blankGroupKey_throws() {
		assertThatThrownBy(() -> service.applyRow(10L, Map.of("word_1", "a")))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void applyRow_noWords_throws() {
		assertThatThrownBy(() -> service.applyRow(10L, Map.of("group_key", "g")))
				.isInstanceOf(BadRequestException.class);
	}
}
