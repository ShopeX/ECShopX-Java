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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.GoodsSearchSynonym;
import cn.shopex.ecshopx.goods.search.synonym.SearchSynonymService.SynonymGroupView;
import java.io.IOException;
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

	@Mock
	private SearchSynonymRepository repository;

	@Mock
	private SearchSynonymEsReloader reloader;

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

	@Test
	void expandDoesNotMatchArbitrarySubstring() {
		when(synonymService.list(COMPANY_A)).thenReturn(List.of(new SynonymGroupView(1L, "part", List.of("机"))));

		assertThat(expander.expand(COMPANY_A, "手机壳")).isEqualTo("手机壳");
	}

	@Test
	void expandMatchesWhitespaceSeparatedToken() {
		when(synonymService.list(COMPANY_A))
				.thenReturn(List.of(new SynonymGroupView(1L, "brand", List.of("手机", "移动电话"))));

		assertThat(expander.expand(COMPANY_A, "红色 手机 壳")).isEqualTo("红色 手机 壳 移动电话");
	}

	@Test
	void expandReusesCachedSynonymListForSameCompany() {
		SearchSynonymService service = new SearchSynonymService(repository, reloader, new SearchSynonymWordsCodec());
		SearchSynonymKeywordExpander cachedExpander = new SearchSynonymKeywordExpander(service);
		when(repository.listByCompanyId(COMPANY_A))
				.thenReturn(List.of(synonymRow(1L, COMPANY_A, "brand", "[\"手机\",\"移动电话\"]")));

		cachedExpander.expand(COMPANY_A, "手机");
		cachedExpander.expand(COMPANY_A, "手机");

		verify(repository, times(1)).listByCompanyId(COMPANY_A);
	}

	@Test
	void replaceGroupRefreshesExpansion() throws IOException {
		SearchSynonymService service = new SearchSynonymService(repository, reloader, new SearchSynonymWordsCodec());
		SearchSynonymKeywordExpander cachedExpander = new SearchSynonymKeywordExpander(service);

		GoodsSearchSynonym initial = synonymRow(1L, COMPANY_A, "brand", "[\"手机\",\"移动电话\"]");
		GoodsSearchSynonym updated = synonymRow(1L, COMPANY_A, "brand", "[\"智能手机\",\"手机\"]");
		when(repository.listByCompanyId(COMPANY_A))
				.thenReturn(List.of(initial))
				.thenReturn(List.of(updated))
				.thenReturn(List.of(updated));
		when(repository.replaceGroup(eq(COMPANY_A), eq("brand"), anyList())).thenReturn(updated);

		assertThat(cachedExpander.expand(COMPANY_A, "手机")).isEqualTo("手机 移动电话");

		service.replaceGroup(COMPANY_A, "brand", List.of("智能手机", "手机"));

		assertThat(cachedExpander.expand(COMPANY_A, "智能手机")).isEqualTo("智能手机 手机");
	}

	private static GoodsSearchSynonym synonymRow(long id, long companyId, String groupKey, String wordsJson) {
		GoodsSearchSynonym row = new GoodsSearchSynonym();
		row.setId(id);
		row.setCompanyId(companyId);
		row.setGroupKey(groupKey);
		row.setWords(wordsJson);
		row.setCreated(1);
		row.setUpdated(1);
		return row;
	}
}
