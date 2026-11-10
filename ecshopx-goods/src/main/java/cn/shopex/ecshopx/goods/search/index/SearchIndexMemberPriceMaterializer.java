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

import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** 将 {@code promotions_member_price} 列 JSON 物化为 ES {@code member_prices} 桶（仅定制价，分）。 */
public final class SearchIndexMemberPriceMaterializer {

	private SearchIndexMemberPriceMaterializer() {}

	public record Buckets(Map<Long, Long> grade, Map<Long, Long> vipGrade) {}

	public static Buckets fromStorageJson(ObjectMapper objectMapper, String rawJson) {
		JsonNode root = MemberPriceColumnCodec.parseRoot(objectMapper, rawJson);
		if (root == null || !root.isObject()) {
			return empty();
		}
		return new Buckets(parseBucket(root.get("grade")), parseBucket(root.get("vipGrade")));
	}

	private static Buckets empty() {
		return new Buckets(Map.of(), Map.of());
	}

	private static Map<Long, Long> parseBucket(JsonNode bucket) {
		if (bucket == null || !bucket.isObject()) {
			return Map.of();
		}
		Map<Long, Long> out = new HashMap<>();
		Iterator<Map.Entry<String, JsonNode>> it = bucket.fields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> en = it.next();
			long gradeId = parsePositiveLong(en.getKey());
			if (gradeId <= 0) {
				continue;
			}
			long fen = parsePriceFen(en.getValue());
			if (fen > 0) {
				out.put(gradeId, fen);
			}
		}
		return Map.copyOf(out);
	}

	private static long parsePositiveLong(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0 ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parsePriceFen(JsonNode node) {
		if (node == null || node.isNull()) {
			return 0L;
		}
		if (node.isIntegralNumber()) {
			return Math.max(0L, node.longValue());
		}
		if (node.isTextual()) {
			String t = node.asText().trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Math.max(0L, Long.parseLong(t));
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
