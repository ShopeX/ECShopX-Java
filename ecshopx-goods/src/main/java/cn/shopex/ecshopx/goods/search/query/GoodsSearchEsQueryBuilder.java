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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 构建 ES 查询体：硬过滤 + 可选关键词 multi_match；默认排序走 function_score（has_stock 加权，完整权重 Task 8）。
 */
final class GoodsSearchEsQueryBuilder {

	private GoodsSearchEsQueryBuilder() {}

	static Query buildRootQuery(GoodsSearchQuery q, SearchEsProperties properties) {
		List<Query> filters = buildHardFilters(q);
		Query inner;
		if (StringUtils.hasText(q.keywords())) {
			Query text = QueryBuilders.multiMatch(m -> m
					.query(q.keywords())
					.fields("item_name^3", "brief^2", "tags")
					.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields));
			Query bn = QueryBuilders.prefix(p -> p.field("item_bn").value(q.keywords()));
			Query barcode = QueryBuilders.prefix(p -> p.field("barcode").value(q.keywords()));
			inner = QueryBuilders.bool(b -> b.should(text).should(bn).should(barcode).minimumShouldMatch("1"));
		} else {
			inner = QueryBuilders.matchAll(m -> m);
		}
		Query bool = QueryBuilders.bool(b -> {
			b.must(inner);
			for (Query f : filters) {
				b.filter(f);
			}
			return b;
		});
		double hasStockWeight = properties.getRank() != null ? properties.getRank().getHasStockWeight() : 10.0;
		return QueryBuilders.functionScore(fs -> fs
				.query(bool)
				.functions(FunctionScore.of(fn -> fn
						.filter(QueryBuilders.term(t -> t.field("has_stock").value(true)))
						.weight(hasStockWeight))));
	}

	static List<Query> buildHardFilters(GoodsSearchQuery q) {
		List<Query> filters = new ArrayList<>();
		filters.add(QueryBuilders.term(t -> t.field("company_id").value(q.companyId())));
		filters.add(QueryBuilders.term(t -> t.field("distributor_id").value(q.distributorId())));
		if (!q.approveStatuses().isEmpty()) {
			List<FieldValue> vals = q.approveStatuses().stream().map(FieldValue::of).toList();
			filters.add(QueryBuilders.terms(t -> t.field("approve_status").terms(tv -> tv.value(vals))));
		}
		if (StringUtils.hasText(q.auditStatus())) {
			filters.add(QueryBuilders.term(t -> t.field("audit_status").value(q.auditStatus())));
		}
		if (!q.defaultItemIdAllowList().isEmpty()) {
			if (q.defaultItemIdAllowList().size() == 1 && q.defaultItemIdAllowList().get(0) == -1L) {
				filters.add(QueryBuilders.term(t -> t.field("default_item_id").value(-1L)));
			} else {
				List<FieldValue> vals = q.defaultItemIdAllowList().stream().map(FieldValue::of).toList();
				filters.add(QueryBuilders.terms(t -> t.field("default_item_id").terms(tv -> tv.value(vals))));
			}
		}
		if (!q.itemCategoryKeys().isEmpty()) {
			List<Long> catIds = new ArrayList<>();
			for (String key : q.itemCategoryKeys()) {
				try {
					catIds.add(Long.parseLong(key.trim()));
				} catch (NumberFormatException ignored) {
					// skip non-numeric legacy keys
				}
			}
			if (!catIds.isEmpty()) {
				List<FieldValue> vals = catIds.stream().map(FieldValue::of).toList();
				filters.add(QueryBuilders.terms(t -> t.field("category_ids").terms(tv -> tv.value(vals))));
			}
		}
		return filters;
	}

	/** 供单测断言 filter 数量与关键词模式。 */
	static boolean isKeywordMode(GoodsSearchQuery q) {
		return StringUtils.hasText(q.keywords());
	}

	static int hardFilterCount(GoodsSearchQuery q) {
		return buildHardFilters(q).size();
	}
}
