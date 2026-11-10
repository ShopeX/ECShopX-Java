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

class GoodsSearchDocumentMaterializationTest {

	@Test
	void sourceToDocumentToEsSource_carriesRankFields() {
		SearchIndexSource src = SearchIndexSource.builder()
				.companyId(1)
				.distributorId(2)
				.defaultItemId(99)
				.hqPriceFen(500)
				.sales30d(31L)
				.listedAt(1_650_000_000L)
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		Map<String, Object> es = GoodsSearchIndexRepository.toEsSource(doc);

		assertThat(doc.sales30d()).isEqualTo(31L);
		assertThat(doc.listedAt()).isEqualTo(1_650_000_000L);
		assertThat(es.get("sales_30d")).isEqualTo(31L);
		assertThat(es.get("listed_at")).isEqualTo(1_650_000_000L);
		assertThat(es.keySet()).contains("sales_30d", "listed_at");
	}
}
