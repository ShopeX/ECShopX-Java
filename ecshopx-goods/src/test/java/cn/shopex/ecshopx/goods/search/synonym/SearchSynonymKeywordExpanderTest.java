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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.search.synonym.SearchSynonymService.SynonymGroupView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchSynonymKeywordExpanderTest {

	private static final long COMPANY_A = 10L;
	private static final long COMPANY_B = 20L;

	@Mock
	private SearchSynonymService synonymService;

	private SearchSynonymKeywordExpander expander;

	@BeforeEach
	void setUp() {
		expander = new SearchSynonymKeywordExpander(synonymService);
	}

	@Test
	void expandAddsPeerWordsWhenGroupMatches() {
		when(synonymService.list(COMPANY_A))
				.thenReturn(List.of(new SynonymGroupView(1L, "brand", List.of("手机", "移动电话"))));

		assertThat(expander.expand(COMPANY_A, "手机")).isEqualTo("手机 移动电话");
	}

	@Test
	void expandLeavesKeywordsUnchangedWhenNoMatch() {
		when(synonymService.list(COMPANY_A))
				.thenReturn(List.of(new SynonymGroupView(1L, "brand", List.of("手机", "移动电话"))));

		assertThat(expander.expand(COMPANY_A, "电脑")).isEqualTo("电脑");
	}

	@Test
	void companyGroupsDoNotLeakAcrossTenants() {
		when(synonymService.list(COMPANY_B)).thenReturn(List.of());

		assertThat(expander.expand(COMPANY_B, "手机")).isEqualTo("手机");

		verify(synonymService).list(COMPANY_B);
		verifyNoMoreInteractions(synonymService);
	}

	@Test
	void emptyOrMissingKeywordsUnchanged() {
		assertThat(expander.expand(COMPANY_A, null)).isNull();
		assertThat(expander.expand(COMPANY_A, "")).isEmpty();
		assertThat(expander.expand(COMPANY_A, "   ")).isEqualTo("   ");
	}
}
