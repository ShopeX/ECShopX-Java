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

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import cn.shopex.ecshopx.goods.search.synonym.SearchSynonymSetNames;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 商品搜索索引 mapping 与 alias 管理。
 *
 * <p>{@code activities[]} 仅收录秒杀/限时特惠/拼团改价活动（见 ES 搜索设计 §5.3）。
 */
@Service
public class GoodsSearchMappingService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final ElasticsearchClient client;

	private final SearchEsProperties properties;

	public GoodsSearchMappingService(ElasticsearchClient client, SearchEsProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	/** 返回索引 {@code mappings} 段 JSON 形状（单测契约）。 */
	public Map<String, Object> buildMappingsPayload() {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("properties", buildProperties());
		return root;
	}

	public void createIndex(String physicalIndex) throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("settings", buildIndexSettings());
		body.put("mappings", buildMappingsPayload());
		String json = toJson(body);
		client.indices().create(b -> b.index(physicalIndex).withJson(new StringReader(json)));
	}

	public void putAlias(String physicalIndex) throws IOException {
		String aliasName = GoodsSearchIndexNames.alias(properties);
		client.indices().putAlias(a -> a.index(physicalIndex).name(aliasName));
	}

	/**
	 * 原子切换 alias：从已绑定该 alias 的物理索引移除，再挂到 {@code physicalIndex}。
	 */
	public void switchAlias(String physicalIndex) throws IOException {
		String aliasName = GoodsSearchIndexNames.alias(properties);
		List<String> bound = new ArrayList<>();
		if (client.indices().existsAlias(e -> e.name(aliasName)).value()) {
			bound.addAll(client.indices().getAlias(g -> g.name(aliasName)).result().keySet());
		}
		client.indices().updateAliases(u -> {
			for (String index : bound) {
				if (!physicalIndex.equals(index)) {
					u.actions(a -> a.remove(r -> r.index(index).alias(aliasName)));
				}
			}
			if (!bound.contains(physicalIndex)) {
				u.actions(a -> a.add(add -> add.index(physicalIndex).alias(aliasName)));
			}
			return u;
		});
	}

	private Map<String, Object> buildIndexSettings() {
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("number_of_shards", 1);
		settings.put("number_of_replicas", 0);
		return settings;
	}

	/**
	 * ES 8 可热更新的 synonym filter 定义，引用 {@link SearchSynonymSetNames} 推送的同义词集。
	 *
	 * <p>共享索引下按公司切换 analyzer 尚未接入查询路径；重建索引时可按 companyId 选用本定义。
	 */
	public static Map<String, Object> buildUpdatableSynonymFilter(long companyId) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("type", "synonym");
		filter.put("synonyms_set", SearchSynonymSetNames.synonymSetId(companyId));
		filter.put("updateable", true);
		return filter;
	}

	private Map<String, Object> buildProperties() {
		Map<String, Object> props = new LinkedHashMap<>();
		props.put("company_id", scalar("long"));
		props.put("distributor_id", scalar("long"));
		props.put("default_item_id", scalar("long"));
		props.put("item_name", ikText());
		props.put("brief", ikText());
		props.put("item_bn", scalar("keyword"));
		props.put("barcode", scalar("keyword"));
		props.put("sale_price", scalar("long"));
		props.put("market_price", scalar("long"));
		props.put("sales_30d", scalar("long"));
		props.put("member_prices", memberPrices());
		props.put("skus", nestedSkus());
		props.put("activities", nestedActivities());
		props.put("has_stock", scalar("boolean"));
		props.put("listed_at", scalar("long"));
		props.put("tags", ikText());
		props.put("category_ids", scalar("long"));
		props.put("approve_status", scalar("keyword"));
		props.put("audit_status", scalar("keyword"));
		return props;
	}

	private static Map<String, Object> scalar(String type) {
		return Map.of("type", type);
	}

	private static Map<String, Object> ikText() {
		Map<String, Object> field = new LinkedHashMap<>();
		field.put("type", "text");
		field.put("analyzer", "ik_max_word");
		field.put("search_analyzer", "ik_smart");
		return field;
	}

	private static Map<String, Object> memberPrices() {
		Map<String, Object> grade = Map.of("type", "flattened");
		Map<String, Object> vipGrade = Map.of("type", "flattened");
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("grade", grade);
		inner.put("vipGrade", vipGrade);
		Map<String, Object> object = new LinkedHashMap<>();
		object.put("type", "object");
		object.put("properties", inner);
		return object;
	}

	private static Map<String, Object> nestedSkus() {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("sku_id", scalar("long"));
		inner.put("store", scalar("long"));
		inner.put("approve_status", scalar("keyword"));
		return nested(inner);
	}

	private static Map<String, Object> nestedActivities() {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("type", scalar("keyword"));
		inner.put("activity_id", scalar("long"));
		inner.put("activity_price", scalar("long"));
		inner.put("start_at", scalar("long"));
		inner.put("end_at", scalar("long"));
		inner.put("priority", scalar("integer"));
		return nested(inner);
	}

	private static Map<String, Object> nested(Map<String, Object> properties) {
		Map<String, Object> nested = new LinkedHashMap<>();
		nested.put("type", "nested");
		nested.put("properties", properties);
		return nested;
	}

	private static String toJson(Map<String, Object> body) {
		try {
			return JSON.writeValueAsString(body);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("serialize index body", e);
		}
	}
}
