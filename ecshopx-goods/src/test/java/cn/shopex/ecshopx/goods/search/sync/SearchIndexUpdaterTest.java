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

package cn.shopex.ecshopx.goods.search.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocumentId;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexRepository;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSource;
import cn.shopex.ecshopx.goods.search.index.SearchIndexSourceLoader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchIndexUpdaterTest {

	private static final long COMPANY_ID = 1L;
	private static final long DISTRIBUTOR_ID = 9L;
	private static final long DEFAULT_ITEM_ID = 100L;

	@Mock
	private SearchIndexSourceLoader sourceLoader;

	@Mock
	private GoodsSearchIndexRepository indexRepository;

	private SearchEsProperties properties;

	private SearchIndexUpdater updater;

	@BeforeEach
	void setUp() {
		properties = new SearchEsProperties();
		properties.setEnabled(true);
		properties.setAlias("goods_store_item");
		updater = new SearchIndexUpdater(properties, sourceLoader, indexRepository);
	}

	@Test
	void upsert_whenDisabled_isNoOp() throws IOException {
		properties.setEnabled(false);
		updater.upsert(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID);
		verify(sourceLoader, never()).fetchOne(anyLong(), anyLong(), anyLong());
		verify(indexRepository, never()).bulkIndex(any(), any());
	}

	@Test
	void upsert_loadsSource_mapsAndBulkIndexesOnAlias() throws IOException {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(COMPANY_ID)
				.distributorId(DISTRIBUTOR_ID)
				.defaultItemId(DEFAULT_ITEM_ID)
				.hqPriceFen(2000)
				.distributorPriceFen(1800)
				.marketPriceFen(2500)
				.skus(List.of(new SearchIndexSource.Sku(101L, 5L, "onsale")))
				.build();
		when(sourceLoader.fetchOne(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID)).thenReturn(Optional.of(src));

		updater.upsert(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<GoodsSearchDocument>> docsCaptor = ArgumentCaptor.forClass(List.class);
		verify(indexRepository)
				.bulkIndex(eq(GoodsSearchIndexNames.alias(properties)), docsCaptor.capture());
		List<GoodsSearchDocument> docs = docsCaptor.getValue();
		assertThat(docs).hasSize(1);
		assertThat(docs.get(0).id()).isEqualTo(GoodsSearchDocumentId.of(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID));
		assertThat(docs.get(0).salePrice()).isEqualTo(1800);
	}

	@Test
	void upsert_whenSourceMissing_deletesDocument() throws IOException {
		when(sourceLoader.fetchOne(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID)).thenReturn(Optional.empty());

		updater.upsert(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID);

		verify(indexRepository)
				.delete(
						eq(GoodsSearchIndexNames.alias(properties)),
						eq(GoodsSearchDocumentId.of(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID)));
		verify(indexRepository, never()).bulkIndex(any(), any());
	}

	@Test
	void delete_whenEnabled_invokesRepositoryDelete() throws IOException {
		updater.delete(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID);
		verify(indexRepository)
				.delete(
						eq(GoodsSearchIndexNames.alias(properties)),
						eq(GoodsSearchDocumentId.of(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID)));
	}

	@Test
	void upsert_whenIndexFails_retriesThenSwallows() throws IOException {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(COMPANY_ID)
				.distributorId(DISTRIBUTOR_ID)
				.defaultItemId(DEFAULT_ITEM_ID)
				.hqPriceFen(2000)
				.build();
		when(sourceLoader.fetchOne(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID)).thenReturn(Optional.of(src));
		doThrow(new IOException("es down"))
				.doThrow(new IOException("es down again"))
				.doNothing()
				.when(indexRepository)
				.bulkIndex(any(), any());

		assertThatCode(() -> updater.upsert(COMPANY_ID, DISTRIBUTOR_ID, DEFAULT_ITEM_ID))
				.doesNotThrowAnyException();
		verify(indexRepository, org.mockito.Mockito.times(3))
				.bulkIndex(eq(GoodsSearchIndexNames.alias(properties)), any());
	}
}
