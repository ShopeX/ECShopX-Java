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

package cn.shopex.ecshopx.goods.service.recommend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public final class GoodsRecommendRequestParser {

	private static final ObjectMapper JSON = new ObjectMapper();

	private GoodsRecommendRequestParser() {}

	public record RecommendAddLine(long itemId, int num) {}

	/**
	 * 结算推荐加购：{@code recommend_item_id} 仅为 {@code [{item_id, num}, ...]}，
	 * 也接受现网 form-urlencoded 的 JSON 数组字符串。
	 * 同一 {@code item_id} 数量累加后再返回。{@code null} / 空数组 → 空列表；其他格式非法。
	 */
	public static List<RecommendAddLine> parseRecommendItemLines(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			String trimmed = s.trim();
			if (trimmed.isEmpty() || "[]".equals(trimmed)) {
				return List.of();
			}
			raw = parseJsonArray(trimmed);
		}
		if (!(raw instanceof Collection<?> collection)) {
			throw new IllegalArgumentException("recommend_item_id must be an array");
		}
		LinkedHashMap<Long, Integer> acc = new LinkedHashMap<>();
		for (Object element : collection) {
			if (element == null) {
				continue;
			}
			if (!(element instanceof Map<?, ?> map)) {
				throw new IllegalArgumentException("recommend_item_id element must be {item_id, num}");
			}
			if (!map.containsKey("item_id") || !map.containsKey("num")) {
				throw new IllegalArgumentException("recommend_item_id element must be {item_id, num}");
			}
			long itemId = parseLong(map.get("item_id"));
			int num = parseRequiredPositiveNum(map.get("num"));
			if (itemId <= 0) {
				throw new IllegalArgumentException("invalid item_id");
			}
			acc.merge(itemId, num, Integer::sum);
		}
		List<RecommendAddLine> out = new ArrayList<>();
		for (Map.Entry<Long, Integer> e : acc.entrySet()) {
			out.add(new RecommendAddLine(e.getKey(), e.getValue()));
		}
		return out;
	}

	private static int parseRequiredPositiveNum(Object raw) {
		if (raw == null || raw.toString().isBlank()) {
			throw new IllegalArgumentException("num required");
		}
		long n;
		if (raw instanceof Number number) {
			n = number.longValue();
		} else {
			try {
				n = Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid num");
			}
		}
		if (n < 1) {
			throw new IllegalArgumentException("num must be >= 1");
		}
		return (int) Math.min(n, Integer.MAX_VALUE);
	}

	public static List<Long> parseItemIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> collection) {
			for (Object element : collection) {
				out.add(parseLong(element));
			}
			return dedupe(out);
		}
		if (raw instanceof String s) {
			if (s.isBlank()) {
				return List.of();
			}
			for (String part : s.split(",")) {
				if (!part.isBlank()) {
					out.add(Long.parseLong(part.trim()));
				}
			}
			return dedupe(out);
		}
		out.add(parseLong(raw));
		return dedupe(out);
	}

	public static long parseDistributorId(Object raw, boolean required) {
		if (raw == null || raw.toString().isBlank()) {
			if (required) {
				throw new IllegalArgumentException("missing");
			}
			return -1L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(raw.toString().trim());
	}

	/**
	 * match 专用：上下文无效时不报错，由调用方返回空列表。
	 *
	 * <ul>
	 *   <li>standard：缺失 / 0 / 非法 → empty
	 *   <li>platform：缺失 / 0 → 0；{@code >0} → empty
	 * </ul>
	 */
	public static OptionalLong resolveDistributorIdForMatch(String productModel, Object raw) {
		long distributorId;
		try {
			distributorId = parseDistributorId(raw, false);
		} catch (IllegalArgumentException e) {
			return OptionalLong.empty();
		}
		if ("standard".equals(productModel)) {
			if (distributorId <= 0) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(distributorId);
		}
		if (distributorId > 0) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(0L);
	}

	public record RecommendMergeDistributorIdResult(long distributorId, String errorCode) {

		public static RecommendMergeDistributorIdResult ok(long distributorId) {
			return new RecommendMergeDistributorIdResult(distributorId, null);
		}

		public static RecommendMergeDistributorIdResult err(String errorCode) {
			return new RecommendMergeDistributorIdResult(0L, errorCode);
		}

		public boolean isOk() {
			return errorCode == null;
		}
	}

	/**
	 * 结算推荐加购：standard 必须 {@code >0}；platform 可省略（视为 0），{@code >0} 报错。
	 */
	public static RecommendMergeDistributorIdResult resolveDistributorIdForRecommendMerge(
			String productModel, Object raw) {
		if ("standard".equals(productModel)) {
			if (raw == null || raw.toString().isBlank()) {
				return RecommendMergeDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED);
			}
			long distributorId;
			try {
				distributorId = parseDistributorId(raw, true);
			} catch (IllegalArgumentException e) {
				return RecommendMergeDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED);
			}
			if (distributorId <= 0) {
				return RecommendMergeDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID);
			}
			return RecommendMergeDistributorIdResult.ok(distributorId);
		}
		long distributorId;
		try {
			distributorId = parseDistributorId(raw, false);
		} catch (IllegalArgumentException e) {
			return RecommendMergeDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID);
		}
		if (distributorId > 0) {
			return RecommendMergeDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID);
		}
		return RecommendMergeDistributorIdResult.ok(0L);
	}

	static Collection<?> parseJsonArray(String raw) {
		try {
			Object parsed = JSON.readValue(raw, Object.class);
			if (parsed instanceof Collection<?> collection) {
				return collection;
			}
		} catch (JsonProcessingException ignored) {
			// fall through
		}
		throw new IllegalArgumentException("recommend_item_id must be an array");
	}

	static long parseLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof Map<?, ?> map) {
			Object id = map.get("item_id");
			if (id == null) {
				id = map.get("itemId");
			}
			return parseLong(id);
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid item id");
		}
	}

	private static List<Long> dedupe(List<Long> ids) {
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (Long id : ids) {
			if (id != null && id > 0) {
				set.add(id);
			}
		}
		return new ArrayList<>(set);
	}
}
