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
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 构建 ES 查询体：硬过滤 + 可选关键词 multi_match。
 * {@code _score} 仅表示文本相关度；默认综合排序由 {@link GoodsSearchQueryService#applyEsSort}
 * 分层：有货 → 相关度 → 销量 → 上架时间。
 */
final class GoodsSearchEsQueryBuilder {

	private GoodsSearchEsQueryBuilder() {}

	@SuppressWarnings("unused") // properties 保留给调用方；综合排序已改为分层 sort，不再读 rank 权重
	static Query buildRootQuery(GoodsSearchQuery q, SearchEsProperties properties) {
		List<Query> filters = buildHardFilters(q);
		Query inner;
		if (StringUtils.hasText(q.keywords())) {
			// IK：整串（含同义词扩展「金 小金」）只打主字段，多字不走 ngram，避免「小金」拆出「小」误召。
			// ngram：仅对单字 token（如「金」）另开 multi_match，补「白金级」单字召回。
			Query ikText = QueryBuilders.multiMatch(m -> m
					.query(q.keywords())
					.fields("item_name^3", "brief^2", "tags^1")
					.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields));
			List<Query> textShoulds = new ArrayList<>();
			textShoulds.add(ikText);
			List<String> singleChars = listSingleCodePointTokens(q.keywords());
			if (!singleChars.isEmpty()) {
				String ngramQuery = String.join(" ", singleChars);
				textShoulds.add(QueryBuilders.multiMatch(m -> m
						.query(ngramQuery)
						.fields("item_name.ngram^0.8", "brief.ngram^0.5", "tags.ngram^0.4")
						.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)));
			}
			Query text = textShoulds.size() == 1
					? textShoulds.get(0)
					: QueryBuilders.bool(b -> b.should(textShoulds).minimumShouldMatch("1"));
			Query bn = QueryBuilders.prefix(p -> p.field("item_bn").value(q.keywords()));
			Query barcode = QueryBuilders.prefix(p -> p.field("barcode").value(q.keywords()));
			List<Query> shoulds = new ArrayList<>();
			shoulds.add(text);
			shoulds.add(bn);
			shoulds.add(barcode);
			String pinyinQ = GoodsSearchPinyinEncoder.normalizeQuery(q.keywords());
			if (pinyinQ.length() >= 2) {
				shoulds.add(QueryBuilders.multiMatch(m -> m
						.query(pinyinQ)
						.fields(
								"item_name_pinyin^2.5",
								"item_name_py^2",
								"brief_pinyin^1.5",
								"brief_py",
								"tags_pinyin",
								"tags_py")
						.type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)));
			}
			inner = QueryBuilders.bool(b -> b.should(shoulds).minimumShouldMatch("1"));
		} else {
			// 无关键词时文本分恒为 1，综合排序退化为：有货 → 后台 sort → 销量 → 上架时间
			inner = QueryBuilders.constantScore(cs -> cs.filter(QueryBuilders.matchAll(m -> m)).boost(1.0f));
		}
		return QueryBuilders.bool(b -> {
			b.must(inner);
			for (Query f : filters) {
				b.filter(f);
			}
			return b;
		});
	}

	/** 供单测断言生成的 query DSL。 */
	static String queryToJson(Query query) {
		JacksonJsonpMapper mapper = new JacksonJsonpMapper();
		StringWriter writer = new StringWriter();
		try (JsonGenerator generator = mapper.jsonProvider().createGenerator(writer)) {
			query.serialize(generator, mapper);
			generator.flush();
		}
		return writer.toString();
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

	/** 按空白拆词后，收集恰好一个 Unicode 码点的 token（供 ngram 专用查询）。 */
	static List<String> listSingleCodePointTokens(String keywords) {
		List<String> out = new ArrayList<>();
		if (!StringUtils.hasText(keywords)) {
			return out;
		}
		for (String token : keywords.trim().split("\\s+")) {
			if (isSingleCodePointToken(token)) {
				out.add(token);
			}
		}
		return out;
	}

	static boolean isSingleCodePointToken(String token) {
		if (!StringUtils.hasText(token)) {
			return false;
		}
		return token.codePointCount(0, token.length()) == 1;
	}
}
