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

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchIndexNames;
import cn.shopex.ecshopx.goods.search.price.SearchEffectivePriceCalculator;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 执行 ES 召回；生效价滤排在 Java 侧用 {@link SearchEffectivePriceCalculator} 与 ES 文档对齐（深分页时 total 可能为近似值）。
 */
@Service
public class GoodsSearchQueryService {

	private static final Logger log = LoggerFactory.getLogger(GoodsSearchQueryService.class);

	private static final int PRICE_RECALL_MULTIPLIER = 5;

	private final ElasticsearchClient client;
	private final SearchEsProperties properties;
	private final SearchEffectivePriceCalculator effectivePriceCalculator;

	public GoodsSearchQueryService(
			ElasticsearchClient client,
			SearchEsProperties properties,
			SearchEffectivePriceCalculator effectivePriceCalculator) {
		this.client = client;
		this.properties = properties;
		this.effectivePriceCalculator = effectivePriceCalculator;
	}

	public GoodsSearchPageResult search(GoodsSearchQuery query) {
		if (query.defaultItemIdAllowList().size() == 1 && query.defaultItemIdAllowList().get(0) == -1L) {
			return new GoodsSearchPageResult(0L, List.of(), false);
		}
		try {
			return doSearch(query);
		} catch (IOException e) {
			throw new GoodsSearchEsException("goods search elasticsearch failed", e);
		}
	}

	private GoodsSearchPageResult doSearch(GoodsSearchQuery query) throws IOException {
		boolean javaPriceOps = query.hasPriceFilter() || query.isPriceSort();
		int fetchSize;
		int from = 0;
		if (javaPriceOps) {
			fetchSize = Math.min(
					GoodsSearchQuery.MAX_WINDOW,
					Math.max(query.page() * query.pageSize() * PRICE_RECALL_MULTIPLIER, query.pageSize() * PRICE_RECALL_MULTIPLIER));
		} else {
			from = Math.max(0, (query.page() - 1) * query.pageSize());
			if (from + query.pageSize() > GoodsSearchQuery.MAX_WINDOW) {
				throw new GoodsSearchEsException("pagination window exceeds limit", null);
			}
			fetchSize = query.pageSize();
		}

		String index = GoodsSearchIndexNames.alias(properties);
		SearchRequest.Builder req = new SearchRequest.Builder()
				.index(index)
				.query(GoodsSearchEsQueryBuilder.buildRootQuery(query, properties))
				.from(from)
				.size(fetchSize)
				.trackTotalHits(t -> t.enabled(true));
		if (!javaPriceOps && !query.isPriceSort()) {
			req.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
		} else if (javaPriceOps) {
			req.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
		}

		SearchResponse<Map> response = client.search(req.build(), Map.class);
		long esTotal = response.hits().total() != null ? response.hits().total().value() : 0L;
		List<ScoredDoc> raw = new ArrayList<>();
		for (Hit<Map> hit : response.hits().hits()) {
			if (hit.source() == null) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> src = (Map<String, Object>) hit.source();
			var doc = GoodsSearchSourceParser.parse(src);
			int eff = effectivePriceCalculator.effectivePriceFen(doc, query.tier(), query.nowEpochSec());
			double score = hit.score() != null ? hit.score() : 0.0;
			raw.add(new ScoredDoc(doc.defaultItemId(), doc.distributorId(), eff, score, doc));
		}

		List<ScoredDoc> filtered = applyPriceFilter(raw, query);
		if (query.isPriceSort()) {
			Comparator<ScoredDoc> cmp =
					"price_asc".equals(query.goodsSort())
							? Comparator.comparingInt(ScoredDoc::effectivePriceFen).thenComparing(ScoredDoc::score, Comparator.reverseOrder())
							: Comparator.comparingInt(ScoredDoc::effectivePriceFen)
									.reversed()
									.thenComparing(ScoredDoc::score, Comparator.reverseOrder());
			filtered = filtered.stream().sorted(cmp).toList();
		}

		boolean approxTotal = javaPriceOps;
		long total;
		List<GoodsSearchHit> pageHits;
		if (javaPriceOps) {
			total = filtered.size();
			int off = (query.page() - 1) * query.pageSize();
			pageHits = filtered.stream().skip(off).limit(query.pageSize()).map(ScoredDoc::toHit).toList();
			if (off + query.pageSize() < filtered.size() || esTotal > filtered.size()) {
				approxTotal = true;
			}
		} else {
			total = esTotal;
			pageHits = filtered.stream().map(ScoredDoc::toHit).toList();
		}
		if (approxTotal) {
			log.debug(
					"goods search price filter/sort used Java post-processing; total={} esTotal={}",
					total,
					esTotal);
		}
		return new GoodsSearchPageResult(total, pageHits, approxTotal);
	}

	private static List<ScoredDoc> applyPriceFilter(List<ScoredDoc> raw, GoodsSearchQuery query) {
		if (!query.hasPriceFilter()) {
			return raw;
		}
		List<ScoredDoc> out = new ArrayList<>(raw.size());
		for (ScoredDoc d : raw) {
			if (query.priceMinFen() != null && d.effectivePriceFen() < query.priceMinFen()) {
				continue;
			}
			if (query.priceMaxFen() != null && d.effectivePriceFen() > query.priceMaxFen()) {
				continue;
			}
			out.add(d);
		}
		return out;
	}

	private record ScoredDoc(long defaultItemId, long distributorId, int effectivePriceFen, double score, cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument doc) {

		GoodsSearchHit toHit() {
			return new GoodsSearchHit(defaultItemId, distributorId, effectivePriceFen, score, doc);
		}
	}
}
