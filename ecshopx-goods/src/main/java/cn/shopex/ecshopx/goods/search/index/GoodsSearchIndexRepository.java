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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** 商品搜索索引文档读写（物理索引名由调用方传入）。 */
@Repository
public class GoodsSearchIndexRepository {

	private final ElasticsearchClient client;

	private final GoodsSearchMappingService mappingService;

	public GoodsSearchIndexRepository(ElasticsearchClient client, GoodsSearchMappingService mappingService) {
		this.client = client;
		this.mappingService = mappingService;
	}

	public void bulkIndex(String physicalIndex, List<GoodsSearchDocument> documents) throws IOException {
		if (documents.isEmpty()) {
			return;
		}
		List<BulkOperation> ops = new ArrayList<>(documents.size());
		for (GoodsSearchDocument doc : documents) {
			ops.add(BulkOperation.of(b -> b.index(i -> i.index(physicalIndex)
					.id(doc.id())
					.document(JsonData.of(toEsSource(doc))))));
		}
		BulkResponse response = client.bulk(b -> b.operations(ops));
		assertBulkSucceeded(response);
	}

	static void assertBulkSucceeded(BulkResponse response) throws IOException {
		if (response.errors()) {
			throw new IOException("Elasticsearch bulk index failed: one or more items were rejected");
		}
	}

	public void delete(String physicalIndex, String documentId) throws IOException {
		client.delete(d -> d.index(physicalIndex).id(documentId));
	}

	/** 将读写 alias 切换到 {@code physicalIndex}（蓝绿重建收尾）。 */
	public void switchAlias(String physicalIndex) throws IOException {
		mappingService.switchAlias(physicalIndex);
	}

	static Map<String, Object> toEsSource(GoodsSearchDocument doc) {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("company_id", doc.companyId());
		root.put("distributor_id", doc.distributorId());
		root.put("default_item_id", doc.defaultItemId());
		root.put("item_name", doc.itemName());
		root.put("brief", doc.brief());
		root.put("item_bn", doc.itemBn());
		root.put("barcode", doc.barcode());
		root.put("tags", doc.tags());
		root.put("approve_status", doc.approveStatus());
		root.put("audit_status", doc.auditStatus());
		root.put("category_ids", doc.categoryIds());
		root.put("sale_price", doc.salePrice());
		root.put("market_price", doc.marketPrice());
		root.put("sales_30d", doc.sales30d());
		root.put("listed_at", doc.listedAt());
		root.put("has_stock", doc.hasStock());
		root.put("member_prices", toMemberPrices(doc.memberPrices()));
		root.put("skus", toSkus(doc.skus()));
		root.put("activities", toActivities(doc.activities()));
		return root;
	}

	private static Map<String, Object> toMemberPrices(GoodsSearchDocument.MemberPrices memberPrices) {
		Map<String, Object> mp = new LinkedHashMap<>();
		mp.put("grade", memberPrices.grade());
		mp.put("vipGrade", memberPrices.vipGrade());
		return mp;
	}

	private static List<Map<String, Object>> toSkus(List<GoodsSearchDocument.Sku> skus) {
		List<Map<String, Object>> out = new ArrayList<>(skus.size());
		for (GoodsSearchDocument.Sku sku : skus) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("sku_id", sku.skuId());
			row.put("store", sku.store());
			row.put("approve_status", sku.approveStatus());
			out.add(row);
		}
		return out;
	}

	private static List<Map<String, Object>> toActivities(List<GoodsSearchDocument.Activity> activities) {
		List<Map<String, Object>> out = new ArrayList<>(activities.size());
		for (GoodsSearchDocument.Activity activity : activities) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("type", activity.type());
			row.put("activity_id", activity.activityId());
			row.put("activity_price", activity.activityPrice());
			row.put("start_at", activity.startAt());
			row.put("end_at", activity.endAt());
			row.put("priority", activity.priority());
			out.add(row);
		}
		return out;
	}
}
