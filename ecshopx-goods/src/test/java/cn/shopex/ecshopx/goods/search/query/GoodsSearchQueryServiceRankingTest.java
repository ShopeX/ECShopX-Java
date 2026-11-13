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

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.price.SearchMemberTier;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoodsSearchQueryServiceRankingTest {

	private static final SearchMemberTier TIER = new SearchMemberTier("normal", 1L, 100, "默认");

	@Test
	void defaultQuery_usesTextOrConstantScore_withoutFunctionScore() {
		SearchEsProperties props = new SearchEsProperties();
		GoodsSearchQuery q = baseQuery("", null, props);
		String dsl = GoodsSearchEsQueryBuilder.queryToJson(GoodsSearchEsQueryBuilder.buildRootQuery(q, props));
		assertThat(dsl).doesNotContain("function_score");
		assertThat(dsl).doesNotContain("field_value_factor");
		assertThat(dsl).contains("constant_score");
	}

	@Test
	void keywordMode_usesTextQuery_notConstantScoreOnly() {
		SearchEsProperties props = new SearchEsProperties();
		GoodsSearchQuery q = baseQuery("", "牛奶", props);
		String dsl = GoodsSearchEsQueryBuilder.queryToJson(GoodsSearchEsQueryBuilder.buildRootQuery(q, props));
		assertThat(dsl).contains("multi_match");
		assertThat(dsl).contains("item_name.ngram");
		assertThat(dsl).contains("brief.ngram");
		assertThat(dsl).doesNotContain("constant_score");
		assertThat(dsl).doesNotContain("function_score");
	}

	@Test
	void keywordMode_includesPinyinFields_forAsciiQuery() {
		SearchEsProperties props = new SearchEsProperties();
		GoodsSearchQuery q = baseQuery("", "shanyue", props);
		String dsl = GoodsSearchEsQueryBuilder.queryToJson(GoodsSearchEsQueryBuilder.buildRootQuery(q, props));
		assertThat(dsl).contains("item_name_pinyin");
		assertThat(dsl).contains("item_name_py");
		assertThat(dsl).contains("shanyue");
	}

	@Test
	void salesSort_esFieldSort_salesDesc() {
		GoodsSearchQuery q = baseQuery("sales", null, new SearchEsProperties());
		SearchRequest.Builder req = new SearchRequest.Builder();
		GoodsSearchQueryService.applyEsSort(req, q, false);
		String dsl = searchRequestToJson(req.build());
		assertThat(dsl).contains("\"sales\"");
		assertThat(dsl).contains("\"order\":\"desc\"");
		assertThat(dsl).doesNotContain("\"_score\"");
		assertThat(dsl).doesNotContain("has_stock");
	}

	@Test
	void newSort_esFieldSort_listedAtDesc() {
		GoodsSearchQuery q = baseQuery("new", null, new SearchEsProperties());
		SearchRequest.Builder req = new SearchRequest.Builder();
		GoodsSearchQueryService.applyEsSort(req, q, false);
		String dsl = searchRequestToJson(req.build());
		assertThat(dsl).contains("listed_at");
		assertThat(dsl).contains("\"order\":\"desc\"");
	}

	@Test
	void defaultCompositeSort_hasStockThenScoreThenSortThenSalesThenListedAt() {
		GoodsSearchQuery q = baseQuery("", null, new SearchEsProperties());
		SearchRequest.Builder req = new SearchRequest.Builder();
		GoodsSearchQueryService.applyEsSort(req, q, false);
		String dsl = searchRequestToJson(req.build());
		assertThat(dsl).contains("has_stock");
		assertThat(dsl).contains("_score");
		assertThat(dsl).contains("\"sales\"");
		assertThat(dsl).contains("listed_at");
		int hasStock = dsl.indexOf("has_stock");
		int score = dsl.indexOf("_score");
		// 请求顶层也有 "sort" 数组键；字段 sort 出现在 _score 之后
		int sortField = dsl.indexOf("\"sort\"", score + 1);
		int sales = dsl.indexOf("\"sales\"");
		int listedAt = dsl.indexOf("listed_at");
		assertThat(hasStock).isGreaterThanOrEqualTo(0);
		assertThat(score).isGreaterThan(hasStock);
		assertThat(sortField).isGreaterThan(score);
		assertThat(sales).isGreaterThan(sortField);
		assertThat(listedAt).isGreaterThan(sales);
	}

	@Test
	void priceSort_recallStillUsesHierarchicalSort() {
		GoodsSearchQuery q = baseQuery("price_asc", null, new SearchEsProperties());
		SearchRequest.Builder req = new SearchRequest.Builder();
		GoodsSearchQueryService.applyEsSort(req, q, true);
		String dsl = searchRequestToJson(req.build());
		assertThat(dsl).contains("has_stock");
		assertThat(dsl).contains("_score");
	}

	private static GoodsSearchQuery baseQuery(String goodsSort, String keywords, SearchEsProperties props) {
		return new GoodsSearchQuery(
				1L,
				0L,
				keywords,
				List.of(),
				List.of(),
				List.of("onsale"),
				"approved",
				null,
				null,
				goodsSort,
				1,
				10,
				TIER,
				0L);
	}

	private static String searchRequestToJson(SearchRequest request) {
		JacksonJsonpMapper mapper = new JacksonJsonpMapper();
		StringWriter writer = new StringWriter();
		try (JsonGenerator generator = mapper.jsonProvider().createGenerator(writer)) {
			request.serialize(generator, mapper);
			generator.flush();
		}
		return writer.toString();
	}
}
