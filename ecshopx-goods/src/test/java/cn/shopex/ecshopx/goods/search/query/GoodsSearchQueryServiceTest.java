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

package cn.shopex.ecshopx.goods.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.goods.search.price.SearchEffectivePriceCalculator;
import cn.shopex.ecshopx.goods.search.price.SearchMemberTier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoodsSearchQueryServiceTest {

	@Test
	void fromWxappParams_mapsPriceAndKeywords() {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("keywords", "牛奶");
		params.put("price|gte", 1000);
		params.put("price|lte", 5000);
		params.put("distributor_id", 9L);
		params.put("approve_status", List.of("onsale"));
		params.put("audit_status", "approved");
		SearchMemberTier tier = new SearchMemberTier("normal", 1L, 100, "默认");
		GoodsSearchQuery q = GoodsSearchQuery.fromWxappParams(1L, params, 2, 10, tier);
		assertThat(q.keywords()).isEqualTo("牛奶");
		assertThat(q.priceMinFen()).isEqualTo(1000);
		assertThat(q.priceMaxFen()).isEqualTo(5000);
		assertThat(q.distributorId()).isEqualTo(9L);
		assertThat(q.page()).isEqualTo(2);
	}

	@Test
	void esQueryBuilder_keywordModeAndHardFilters() {
		SearchMemberTier tier = new SearchMemberTier("normal", 1L, 100, "默认");
		GoodsSearchQuery q = new GoodsSearchQuery(
				10L, 0L, "ab", List.of(), List.of("100"), List.of("onsale"), "approved", null, null, "", 1, 10, tier, 0L);
		assertThat(GoodsSearchEsQueryBuilder.isKeywordMode(q)).isTrue();
		assertThat(GoodsSearchEsQueryBuilder.hardFilterCount(q)).isGreaterThanOrEqualTo(4);
	}

	@Test
	void sourceParser_andCalculator_priceFilterConsistency() {
		Map<String, Object> src = new LinkedHashMap<>();
		src.put("company_id", 1L);
		src.put("distributor_id", 0L);
		src.put("default_item_id", 100L);
		src.put("sale_price", 2000L);
		src.put("member_prices", Map.of("grade", Map.of("2", 1500), "vipGrade", Map.of()));
		src.put("skus", List.of());
		src.put("activities", List.of());
		GoodsSearchDocument doc = GoodsSearchSourceParser.parse(src);
		SearchMemberTier tier = new SearchMemberTier("normal", 2L, 100, "银卡");
		int eff = new SearchEffectivePriceCalculator().effectivePriceFen(doc, tier, 1_700_000_000L);
		assertThat(eff).isEqualTo(1500);
		GoodsSearchQuery q = new GoodsSearchQuery(
				1L, 0L, null, List.of(), List.of(), List.of("onsale"), "approved", 1400, 1600, "", 1, 10, tier, 0L);
		assertThat(q.hasPriceFilter()).isTrue();
		assertThat(eff).isBetween(q.priceMinFen(), q.priceMaxFen());
	}
}
