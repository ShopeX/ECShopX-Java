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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchIndexMapperTest {

	@Test
	void storePriceOverridesHqPrice() {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100)
				.hqPriceFen(2000).distributorPriceFen(1800)
				.marketPriceFen(2500)
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		assertThat(doc.salePrice()).isEqualTo(1800);
		assertThat(doc.id()).isEqualTo("1_9_100");
	}

	@Test
	void activitiesOnlySeckillLimitedGroup_noTargeted() {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100)
				.hqPriceFen(2000)
				.activities(List.of(
						new SearchIndexSource.Activity("targeted", 10L, 800, 1, 9999999999L, 1),
						new SearchIndexSource.Activity("seckill", 11L, 990, 1, 9999999999L, 1),
						new SearchIndexSource.Activity("group", 12L, 1200, 1, 9999999999L, 2)))
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		assertThat(doc.activities()).extracting(GoodsSearchDocument.Activity::type)
				.containsExactly("seckill", "group");
	}

	@Test
	void activitiesDefaultPriorityWhenNull() {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100)
				.hqPriceFen(2000)
				.activities(List.of(
						new SearchIndexSource.Activity("limited_time_sale", 1L, 100, 1, 9999999999L, null),
						new SearchIndexSource.Activity("seckill", 2L, 100, 1, 9999999999L, null),
						new SearchIndexSource.Activity("group", 3L, 100, 1, 9999999999L, null)))
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		assertThat(doc.activities()).extracting(GoodsSearchDocument.Activity::priority)
				.containsExactly(1, 1, 2);
	}

	@Test
	void hasStock_trueWhenOnsaleOrOnlyShowAndStorePositive() {
		SearchIndexSource onsale = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100).hqPriceFen(2000)
				.skus(List.of(new SearchIndexSource.Sku(1L, 3L, "onsale")))
				.build();
		SearchIndexSource onlyShow = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100).hqPriceFen(2000)
				.skus(List.of(new SearchIndexSource.Sku(2L, 1L, "only_show")))
				.build();
		assertThat(new SearchIndexMapper().toDocument(onsale).hasStock()).isTrue();
		assertThat(new SearchIndexMapper().toDocument(onlyShow).hasStock()).isTrue();
	}

	@Test
	void hasStock_falseWhenStoreZeroOrNonSellableApproveStatus() {
		SearchIndexSource zeroStore = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100).hqPriceFen(2000)
				.skus(List.of(new SearchIndexSource.Sku(1L, 0L, "onsale")))
				.build();
		SearchIndexSource nonSellableDespiteStock = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100).hqPriceFen(2000)
				.skus(List.of(new SearchIndexSource.Sku(2L, 99L, "instock")))
				.build();
		assertThat(new SearchIndexMapper().toDocument(zeroStore).hasStock()).isFalse();
		assertThat(new SearchIndexMapper().toDocument(nonSellableDespiteStock).hasStock()).isFalse();
	}

	@Test
	void rankFieldsPassThroughFromSource() {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100).hqPriceFen(2000)
				.sales30d(420L)
				.listedAt(1_700_000_000L)
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		assertThat(doc.sales30d()).isEqualTo(420L);
		assertThat(doc.listedAt()).isEqualTo(1_700_000_000L);
	}

	@Test
	void memberPricesKeepCustomBucketsOnly() {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1).distributorId(9).defaultItemId(100).hqPriceFen(2000)
				.memberPriceGrade(Map.of(3L, 1500L))
				.memberPriceVipGrade(Map.of(8L, 1400L))
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		assertThat(doc.memberPrices().grade()).containsEntry(3L, 1500L);
		assertThat(doc.memberPrices().vipGrade()).containsEntry(8L, 1400L);
	}
}
