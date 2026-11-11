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
				.sales(31L)
				.listedAt(1_650_000_000L)
				.build();
		GoodsSearchDocument doc = new SearchIndexMapper().toDocument(src);
		Map<String, Object> es = GoodsSearchIndexRepository.toEsSource(doc);

		assertThat(doc.sales()).isEqualTo(31L);
		assertThat(doc.listedAt()).isEqualTo(1_650_000_000L);
		assertThat(es.get("sales")).isEqualTo(31L);
		assertThat(es.get("listed_at")).isEqualTo(1_650_000_000L);
		assertThat(es.keySet()).contains("sales", "listed_at");
	}

	@Test
	void toEsSource_writesPinyinFieldsFromItemName() {
		GoodsSearchDocument doc = new GoodsSearchDocument(
				"1_2_99",
				1L,
				2L,
				99L,
				"山岳户外双肩包",
				"轻便户外",
				"BN",
				"BC",
				List.of("登山"),
				"onsale",
				"approved",
				List.of(),
				100L,
				100L,
				0L,
				1L,
				new GoodsSearchDocument.MemberPrices(Map.of(), Map.of()),
				List.of(),
				List.of());
		Map<String, Object> es = GoodsSearchIndexRepository.toEsSource(doc);
		assertThat(es.get("item_name_pinyin")).isEqualTo("shanyuehuwaishuangjianbao");
		assertThat(es.get("item_name_py")).isEqualTo("syhwsjb");
		assertThat(es.get("brief_pinyin")).isEqualTo("qingbianhuwai");
		assertThat(es.get("tags_py")).isEqualTo("ds");
	}
}
