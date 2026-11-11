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

package cn.shopex.ecshopx.goods.search.rebuild;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexRepository;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchMappingService;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSource;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSourceLoader;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SearchRebuildServiceTest {

	private static final long FIXED_EPOCH_MS = 1_700_000_000_000L;

	@Test
	void rebuildCreatesIndexBulksAndSwitchesAlias() throws Exception {
		GoodsSearchMappingService mappingService = mock(GoodsSearchMappingService.class);
		GoodsSearchIndexRepository indexRepository = mock(GoodsSearchIndexRepository.class);
		SearchIndexSourceLoader sourceLoader = mock(SearchIndexSourceLoader.class);

		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1)
				.distributorId(9)
				.defaultItemId(100)
				.hqPriceFen(2000)
				.marketPriceFen(2500)
				.build();

		when(sourceLoader.fetchPage(eq(1L), isNull(), eq(0L), eq(0L), anyInt())).thenReturn(List.of(src));
		when(sourceLoader.fetchPage(eq(1L), isNull(), eq(100L), eq(9L), anyInt())).thenReturn(List.of());

		SearchRebuildService service =
				new SearchRebuildService(mappingService, indexRepository, sourceLoader, () -> FIXED_EPOCH_MS);

		SearchRebuildService.RebuildResult result = service.rebuild(1L, null);

		String expectedIndex = GoodsSearchIndexNames.physicalIndex(FIXED_EPOCH_MS);
		assertThat(result.physicalIndex()).isEqualTo(expectedIndex);
		assertThat(result.docs()).isEqualTo(1);

		InOrder inOrder = inOrder(mappingService, indexRepository);
		inOrder.verify(mappingService).createIndex(expectedIndex);
		inOrder.verify(indexRepository).bulkIndex(eq(expectedIndex), anyList());
		inOrder.verify(indexRepository).switchAlias(expectedIndex);
		verify(mappingService, never()).putAlias(anyString());

		verify(sourceLoader).fetchPage(1L, null, 0L, 0L, SearchRebuildService.BULK_BATCH_SIZE);
		verify(sourceLoader).fetchPage(1L, null, 100L, 9L, SearchRebuildService.BULK_BATCH_SIZE);
	}

	@Test
	void reindexCompanyIntoAliasEnsuresAliasAndBulksWithoutSwitch() throws Exception {
		GoodsSearchMappingService mappingService = mock(GoodsSearchMappingService.class);
		GoodsSearchIndexRepository indexRepository = mock(GoodsSearchIndexRepository.class);
		SearchIndexSourceLoader sourceLoader = mock(SearchIndexSourceLoader.class);

		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1)
				.distributorId(9)
				.defaultItemId(100)
				.hqPriceFen(2000)
				.marketPriceFen(2500)
				.build();

		when(mappingService.ensureWritableAlias()).thenReturn("goods_store_item");
		when(sourceLoader.fetchPage(eq(1L), isNull(), eq(0L), eq(0L), anyInt())).thenReturn(List.of(src));
		when(sourceLoader.fetchPage(eq(1L), isNull(), eq(100L), eq(9L), anyInt())).thenReturn(List.of());

		SearchRebuildService service =
				new SearchRebuildService(mappingService, indexRepository, sourceLoader, () -> FIXED_EPOCH_MS);

		SearchRebuildService.RebuildResult result = service.reindexCompanyIntoAlias(1L, null);

		assertThat(result.physicalIndex()).isEqualTo("goods_store_item");
		assertThat(result.docs()).isEqualTo(1);
		verify(mappingService).ensureWritableAlias();
		verify(indexRepository).bulkIndex(eq("goods_store_item"), anyList());
		verify(indexRepository, never()).switchAlias(anyString());
		verify(mappingService, never()).createIndex(anyString());
	}

	@Test
	void rebuildAbortsBeforeAliasSwitchWhenBulkFails() throws Exception {
		GoodsSearchMappingService mappingService = mock(GoodsSearchMappingService.class);
		GoodsSearchIndexRepository indexRepository = mock(GoodsSearchIndexRepository.class);
		SearchIndexSourceLoader sourceLoader = mock(SearchIndexSourceLoader.class);

		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1)
				.distributorId(9)
				.defaultItemId(100)
				.hqPriceFen(2000)
				.marketPriceFen(2500)
				.build();

		when(sourceLoader.fetchPage(eq(1L), isNull(), eq(0L), eq(0L), anyInt())).thenReturn(List.of(src));
		doThrow(new IOException("Elasticsearch bulk index failed: one or more items were rejected"))
				.when(indexRepository)
				.bulkIndex(anyString(), anyList());

		SearchRebuildService service =
				new SearchRebuildService(mappingService, indexRepository, sourceLoader, () -> FIXED_EPOCH_MS);

		assertThatThrownBy(() -> service.rebuild(1L, null)).isInstanceOf(IOException.class);

		verify(indexRepository, never()).switchAlias(anyString());
		verify(mappingService, never()).putAlias(anyString());
	}
}
