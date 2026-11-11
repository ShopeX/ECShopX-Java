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

package cn.shopex.ecshopx.goods.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.SearchIndexSourceMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisSearchIndexSourceLoaderFetchPageTest {

	@Mock private SearchIndexSourceMapper searchIndexSourceMapper;
	@Mock private ItemsMapper itemsMapper;
	@Mock private DistributorItemsMapper distributorItemsMapper;
	@Mock private SearchIndexMemberActivityRepository memberActivityRepository;
	@Mock private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock private ItemsTagsRepository itemsTagsRepository;

	@Test
	void fetchPageAdvancesPastFullyOrphanKeyPage() {
		MybatisSearchIndexSourceLoader loader =
				new MybatisSearchIndexSourceLoader(
						searchIndexSourceMapper,
						itemsMapper,
						distributorItemsMapper,
						memberActivityRepository,
						itemsRelTagsRepository,
						itemsTagsRepository);

		SearchIndexStoreItemKey orphanA = new SearchIndexStoreItemKey(20L, 16L);
		SearchIndexStoreItemKey orphanB = new SearchIndexStoreItemKey(21L, 16L);
		when(searchIndexSourceMapper.selectStoreItemKeys(eq(38L), isNull(), eq(0L), eq(0L), eq(2)))
				.thenReturn(List.of(orphanA, orphanB));
		when(searchIndexSourceMapper.selectStoreItemKeys(eq(38L), isNull(), eq(16L), eq(21L), eq(2)))
				.thenReturn(List.of());
		when(itemsMapper.selectOne(any())).thenReturn(null);

		List<SearchIndexSource> page = loader.fetchPage(38L, null, 0L, 0L, 2);

		assertThat(page).isEmpty();
		verify(searchIndexSourceMapper).selectStoreItemKeys(eq(38L), isNull(), eq(0L), eq(0L), eq(2));
		verify(searchIndexSourceMapper).selectStoreItemKeys(eq(38L), isNull(), eq(16L), eq(21L), eq(2));
	}
}
