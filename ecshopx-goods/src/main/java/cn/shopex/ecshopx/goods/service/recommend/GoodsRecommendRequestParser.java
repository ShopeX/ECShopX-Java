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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public final class GoodsRecommendRequestParser {

	private GoodsRecommendRequestParser() {}

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

	public record CheckoutAddDistributorIdResult(long distributorId, String errorCode) {

		public static CheckoutAddDistributorIdResult ok(long distributorId) {
			return new CheckoutAddDistributorIdResult(distributorId, null);
		}

		public static CheckoutAddDistributorIdResult err(String errorCode) {
			return new CheckoutAddDistributorIdResult(0L, errorCode);
		}

		public boolean isOk() {
			return errorCode == null;
		}
	}

	/**
	 * checkout-add 专用：standard 必须 {@code >0}；platform 可省略（视为 0），{@code >0} 报错。
	 */
	public static CheckoutAddDistributorIdResult resolveDistributorIdForCheckoutAdd(
			String productModel, Object raw) {
		if ("standard".equals(productModel)) {
			if (raw == null || raw.toString().isBlank()) {
				return CheckoutAddDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED);
			}
			long distributorId;
			try {
				distributorId = parseDistributorId(raw, true);
			} catch (IllegalArgumentException e) {
				return CheckoutAddDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED);
			}
			if (distributorId <= 0) {
				return CheckoutAddDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID);
			}
			return CheckoutAddDistributorIdResult.ok(distributorId);
		}
		long distributorId;
		try {
			distributorId = parseDistributorId(raw, false);
		} catch (IllegalArgumentException e) {
			return CheckoutAddDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID);
		}
		if (distributorId > 0) {
			return CheckoutAddDistributorIdResult.err(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID);
		}
		return CheckoutAddDistributorIdResult.ok(0L);
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
