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

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoodsSearchMappingServiceTest {

	@Test
	void mappingsContainNestedSkusAndActivitiesAndIkOnItemName() {
		GoodsSearchMappingService svc = new GoodsSearchMappingService(null, new SearchEsProperties());
		Map<String, Object> m = svc.buildMappingsPayload();
		@SuppressWarnings("unchecked")
		Map<String, Object> props = (Map<String, Object>) m.get("properties");
		assertThat(props).containsKeys(
				"company_id", "distributor_id", "default_item_id",
				"item_name", "item_name_pinyin", "item_name_py",
				"brief_pinyin", "tags_pinyin",
				"sale_price", "sort", "member_prices", "skus", "activities", "has_stock");
		@SuppressWarnings("unchecked")
		Map<String, Object> namePy = (Map<String, Object>) props.get("item_name_pinyin");
		assertThat(namePy.get("analyzer")).isEqualTo("goods_pinyin_index");
		assertThat(namePy.get("search_analyzer")).isEqualTo("goods_pinyin_search");
		@SuppressWarnings("unchecked")
		Map<String, Object> skus = (Map<String, Object>) props.get("skus");
		assertThat(skus.get("type")).isEqualTo("nested");
		@SuppressWarnings("unchecked")
		Map<String, Object> acts = (Map<String, Object>) props.get("activities");
		assertThat(acts.get("type")).isEqualTo("nested");
		assertThat(GoodsSearchMappingService.buildPinyinAnalysis()).containsKeys("filter", "analyzer");
		@SuppressWarnings("unchecked")
		Map<String, Object> itemName = (Map<String, Object>) props.get("item_name");
		assertThat(itemName.get("analyzer")).isEqualTo("ik_max_word");
		assertThat(itemName.get("search_analyzer")).isEqualTo("ik_smart");
		@SuppressWarnings("unchecked")
		Map<String, Object> nameFields = (Map<String, Object>) itemName.get("fields");
		@SuppressWarnings("unchecked")
		Map<String, Object> nameNgram = (Map<String, Object>) nameFields.get("ngram");
		assertThat(nameNgram.get("analyzer")).isEqualTo("goods_cjk_ngram");
		Map<String, Object> analysis = GoodsSearchMappingService.buildAnalysis();
		assertThat(analysis).containsKeys("filter", "analyzer", "tokenizer");
		@SuppressWarnings("unchecked")
		Map<String, Object> analyzers = (Map<String, Object>) analysis.get("analyzer");
		assertThat(analyzers).containsKeys("goods_pinyin_index", "goods_pinyin_search", "goods_cjk_ngram");
	}
}
