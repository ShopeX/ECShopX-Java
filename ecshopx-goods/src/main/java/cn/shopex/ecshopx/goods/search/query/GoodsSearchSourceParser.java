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

import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocument;
import cn.shopex.ecshopx.goods.search.index.GoodsSearchDocumentId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 将 ES {@code _source} Map 解析为 {@link GoodsSearchDocument}（算价所需字段）。 */
final class GoodsSearchSourceParser {

	private GoodsSearchSourceParser() {}

	static GoodsSearchDocument parse(Map<String, Object> src) {
		long companyId = longVal(src.get("company_id"));
		long distributorId = longVal(src.get("distributor_id"));
		long defaultItemId = longVal(src.get("default_item_id"));
		String id = GoodsSearchDocumentId.of(companyId, distributorId, defaultItemId);
		GoodsSearchDocument.MemberPrices mp = parseMemberPrices(src.get("member_prices"));
		return new GoodsSearchDocument(
				id,
				companyId,
				distributorId,
				defaultItemId,
				str(src.get("item_name")),
				str(src.get("brief")),
				str(src.get("item_bn")),
				str(src.get("barcode")),
				strList(src.get("tags")),
				str(src.get("approve_status")),
				str(src.get("audit_status")),
				longList(src.get("category_ids")),
				longVal(src.get("sale_price")),
				longVal(src.get("market_price")),
				mp,
				parseSkus(src.get("skus")),
				parseActivities(src.get("activities")));
	}

	@SuppressWarnings("unchecked")
	private static GoodsSearchDocument.MemberPrices parseMemberPrices(Object raw) {
		if (!(raw instanceof Map<?, ?> map)) {
			return new GoodsSearchDocument.MemberPrices(Map.of(), Map.of());
		}
		return new GoodsSearchDocument.MemberPrices(
				toLongLongMap((Map<String, Object>) map.get("grade")),
				toLongLongMap((Map<String, Object>) map.get("vipGrade")));
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, Long> toLongLongMap(Object bucket) {
		if (!(bucket instanceof Map<?, ?> map)) {
			return Map.of();
		}
		java.util.LinkedHashMap<Long, Long> out = new java.util.LinkedHashMap<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			try {
				long k = Long.parseLong(e.getKey().toString());
				long v = longVal(e.getValue());
				out.put(k, v);
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return Map.copyOf(out);
	}

	@SuppressWarnings("unchecked")
	private static List<GoodsSearchDocument.Sku> parseSkus(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<GoodsSearchDocument.Sku> skus = new ArrayList<>();
		for (Object row : list) {
			if (!(row instanceof Map<?, ?> m)) {
				continue;
			}
			skus.add(
					new GoodsSearchDocument.Sku(
							longVal(m.get("sku_id")),
							longVal(m.get("store")),
							str(m.get("approve_status"))));
		}
		return List.copyOf(skus);
	}

	@SuppressWarnings("unchecked")
	private static List<GoodsSearchDocument.Activity> parseActivities(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<GoodsSearchDocument.Activity> acts = new ArrayList<>();
		for (Object row : list) {
			if (!(row instanceof Map<?, ?> m)) {
				continue;
			}
			acts.add(
					new GoodsSearchDocument.Activity(
							str(m.get("type")),
							longVal(m.get("activity_id")),
							longVal(m.get("activity_price")),
							longVal(m.get("start_at")),
							longVal(m.get("end_at")),
							(int) longVal(m.get("priority"))));
		}
		return List.copyOf(acts);
	}

	private static String str(Object v) {
		return v != null ? v.toString() : "";
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static List<String> strList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object o : list) {
			if (o != null) {
				String s = o.toString().trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
		}
		return List.copyOf(out);
	}

	private static List<Long> longList(Object raw) {
		if (raw instanceof List<?> list) {
			return list.stream().map(GoodsSearchSourceParser::longVal).toList();
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		return List.of();
	}
}
