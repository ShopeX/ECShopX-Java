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
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.mapper.SearchIndexSourceMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisSearchIndexSourceLoaderStoreOwnedTest {

	@Mock private SearchIndexSourceMapper searchIndexSourceMapper;
	@Mock private ItemsMapper itemsMapper;
	@Mock private DistributorItemsMapper distributorItemsMapper;
	@Mock private SearchIndexMemberActivityRepository memberActivityRepository;
	@Mock private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock private ItemsTagsRepository itemsTagsRepository;

	@Test
	void fetchOne_storeOwnedWithoutShelfRow_stillMaterializes() {
		MybatisSearchIndexSourceLoader loader =
				new MybatisSearchIndexSourceLoader(
						searchIndexSourceMapper,
						itemsMapper,
						distributorItemsMapper,
						memberActivityRepository,
						itemsRelTagsRepository,
						itemsTagsRepository);

		Items anchor = new Items();
		anchor.setItemId(8145L);
		anchor.setCompanyId(141L);
		anchor.setDistributorId(3361);
		anchor.setDefaultItemId(8145L);
		anchor.setItemName("JM001 滑雪板双板");
		anchor.setApproveStatus("onsale");
		anchor.setAuditStatus("approved");
		anchor.setPrice(100);
		anchor.setStore(9995);
		anchor.setSales(5);

		when(itemsMapper.selectOne(any())).thenReturn(anchor);
		when(itemsMapper.selectList(any())).thenReturn(List.of(anchor));
		when(distributorItemsMapper.selectList(any())).thenReturn(List.of());
		when(memberActivityRepository.loadMemberPrices(141L, 8145L, List.of(8145L)))
				.thenReturn(new SearchIndexMemberPriceMaterializer.Buckets(java.util.Map.of(), java.util.Map.of()));
		when(memberActivityRepository.loadActivities(141L, 3361L, List.of(8145L))).thenReturn(List.of());
		when(itemsRelTagsRepository.listRowsByCompanyIdAndItemIdIn(141L, List.of(8145L))).thenReturn(List.of());

		Optional<SearchIndexSource> doc = loader.fetchOne(141L, 3361L, 8145L);

		assertThat(doc).isPresent();
		assertThat(doc.get().distributorId()).isEqualTo(3361L);
		assertThat(doc.get().defaultItemId()).isEqualTo(8145L);
		assertThat(doc.get().skus()).hasSize(1);
		assertThat(doc.get().skus().get(0).store()).isEqualTo(9995L);
		assertThat(doc.get().sales()).isEqualTo(5L);
	}
}
