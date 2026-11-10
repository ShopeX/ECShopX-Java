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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.GoodsSearchSynonym;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchSynonymServiceTest {

	private static final long COMPANY_A = 10L;
	private static final long COMPANY_B = 20L;

	@Mock
	private SearchSynonymRepository repository;

	@Mock
	private SearchSynonymEsReloader reloader;

	private SearchSynonymService service;

	@BeforeEach
	void setUp() {
		service = new SearchSynonymService(repository, reloader, new SearchSynonymWordsCodec());
	}

	@Test
	void listReturnsOnlyGroupsForRequestedCompany() throws Exception {
		GoodsSearchSynonym aGroup = row(1L, COMPANY_A, "brand", "[\"手机\",\"移动电话\"]");
		when(repository.listByCompanyId(COMPANY_A)).thenReturn(List.of(aGroup));
		when(repository.listByCompanyId(COMPANY_B)).thenReturn(List.of());

		assertThat(service.list(COMPANY_A)).singleElement().satisfies(g -> {
			assertThat(g.id()).isEqualTo(1L);
			assertThat(g.groupKey()).isEqualTo("brand");
			assertThat(g.words()).containsExactly("手机", "移动电话");
		});
		assertThat(service.list(COMPANY_B)).isEmpty();

		verify(repository).listByCompanyId(COMPANY_A);
		verify(repository).listByCompanyId(COMPANY_B);
		verify(reloader, never()).reload(anyLong(), anyMap());
	}

	@Test
	void replaceGroupPersistsThenReloadsAllCompanyGroups() throws IOException {
		GoodsSearchSynonym stored = row(5L, COMPANY_A, "color", "[\"红色\",\"红\"]");
		when(repository.replaceGroup(eq(COMPANY_A), eq("color"), anyList())).thenReturn(stored);
		when(repository.listByCompanyId(COMPANY_A)).thenReturn(List.of(stored));

		SearchSynonymService.SynonymGroupView view =
				service.replaceGroup(COMPANY_A, "color", List.of("红色", "红"));

		assertThat(view.groupKey()).isEqualTo("color");
		assertThat(view.words()).containsExactly("红色", "红");

		ArgumentCaptor<Map<String, List<String>>> groupsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(reloader).reload(eq(COMPANY_A), groupsCaptor.capture());
		assertThat(groupsCaptor.getValue()).containsEntry("color", List.of("红色", "红"));
	}

	@Test
	void deleteGroupRemovesRowAndReloads() throws IOException {
		GoodsSearchSynonym remaining = row(2L, COMPANY_A, "size", "[\"大号\",\"L\"]");
		when(repository.listByCompanyId(COMPANY_A)).thenReturn(List.of(remaining));

		service.deleteGroup(COMPANY_A, 1L);

		InOrder order = inOrder(repository, reloader);
		order.verify(repository).deleteByCompanyAndId(COMPANY_A, 1L);
		order.verify(reloader).reload(eq(COMPANY_A), eq(Map.of("size", List.of("大号", "L"))));
	}

	@Test
	void listCachesByCompanyUntilMutated() throws IOException {
		GoodsSearchSynonym aGroup = row(1L, COMPANY_A, "brand", "[\"手机\",\"移动电话\"]");
		when(repository.listByCompanyId(COMPANY_A)).thenReturn(List.of(aGroup));
		when(repository.replaceGroup(eq(COMPANY_A), eq("brand"), anyList())).thenReturn(aGroup);

		service.list(COMPANY_A);
		service.list(COMPANY_A);
		verify(repository, times(1)).listByCompanyId(COMPANY_A);

		service.replaceGroup(COMPANY_A, "brand", List.of("手机", "移动电话"));

		service.list(COMPANY_A);
		verify(repository, times(3)).listByCompanyId(COMPANY_A);
	}

	@Test
	void reloadFailureAfterReplaceDoesNotRollbackDbWrite() throws IOException {
		GoodsSearchSynonym stored = row(7L, COMPANY_A, "sku", "[\"货号\",\"编码\"]");
		when(repository.replaceGroup(eq(COMPANY_A), eq("sku"), anyList())).thenReturn(stored);
		when(repository.listByCompanyId(COMPANY_A)).thenReturn(List.of(stored));
		doThrow(new SearchSynonymEsReloadException("reload failed", new IOException("es down")))
				.when(reloader)
				.reload(eq(COMPANY_A), any());

		assertThatThrownBy(() -> service.replaceGroup(COMPANY_A, "sku", List.of("货号", "编码")))
				.isInstanceOf(SearchSynonymEsReloadException.class);

		verify(repository).replaceGroup(eq(COMPANY_A), eq("sku"), eq(List.of("货号", "编码")));
	}

	private static GoodsSearchSynonym row(long id, long companyId, String groupKey, String wordsJson) {
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
