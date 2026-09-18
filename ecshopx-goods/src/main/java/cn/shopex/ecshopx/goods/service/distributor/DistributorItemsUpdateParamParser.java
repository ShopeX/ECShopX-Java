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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.service.distributor.dto.DistributorItemsUpdateColumnPatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

public final class DistributorItemsUpdateParamParser {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private DistributorItemsUpdateParamParser() {}

	public record Parsed(
			List<Long> distributorIds,
			Optional<Long> singleGoodsId,
			Optional<List<Long>> multiGoodsIds,
			Optional<Long> itemIdFromRequest,
			boolean isDefaultKeyPresent,
			boolean isCanSaleKeyPresent,
			boolean isCanSaleNormalized,
			boolean isTotalStoreKeyPresent,
			boolean isTotalStoreNormalized,
			boolean storeKeyPresent,
			Optional<Long> store,
			boolean priceKeyPresent,
			Optional<Long> price) {}

	public static Parsed parse(Map<String, Object> merged) {
		if (merged == null) {
			throw new BadRequestException("请先选择商品");
		}
		List<Long> distributorIds = parseDistributorIds(merged.get("distributor_id"));
		GoodsIdParse goods = parseGoodsIdValue(merged.get("goods_id"));
		Optional<Long> itemId = parseOptionalLongKey(merged, "item_id");
		boolean isDefaultKey = merged.containsKey("is_default");
		boolean isCanSaleKey = merged.containsKey("is_can_sale");
		boolean isTotalStoreKey = merged.containsKey("is_total_store");
		boolean storeKey = merged.containsKey("store");
		boolean priceKey = merged.containsKey("price");
		return new Parsed(
				distributorIds,
				goods.singleId,
				goods.multiIds,
				itemId,
				isDefaultKey,
				isCanSaleKey,
				isCanSaleKey ? normalizeBool(merged.get("is_can_sale")) : false,
				isTotalStoreKey,
				isTotalStoreKey ? normalizeBool(merged.get("is_total_store")) : false,
				storeKey,
				storeKey ? Optional.of(parseLongRequired(merged.get("store"), "store")) : Optional.empty(),
				priceKey,
				priceKey ? Optional.of(parseLongRequired(merged.get("price"), "price")) : Optional.empty());
	}

	public static DistributorItemsUpdateColumnPatch toColumnPatch(Parsed p) {
		Optional<Boolean> isCanSale = p.isCanSaleKeyPresent ? Optional.of(p.isCanSaleNormalized) : Optional.empty();
		Optional<Boolean> isTotalStore = p.isTotalStoreKeyPresent ? Optional.of(p.isTotalStoreNormalized) : Optional.empty();
		Optional<Long> store = p.storeKeyPresent ? p.store : Optional.empty();
		Optional<Long> price = p.priceKeyPresent ? p.price : Optional.empty();
		return new DistributorItemsUpdateColumnPatch(isCanSale, isTotalStore, store, price);
	}

	private record GoodsIdParse(Optional<Long> singleId, Optional<List<Long>> multiIds) {}

	private static GoodsIdParse parseGoodsIdValue(Object raw) {
		if (raw == null) {
			return new GoodsIdParse(Optional.empty(), Optional.empty());
		}
		if (raw instanceof List<?> list) {
			List<Long> ids = new ArrayList<>();
			for (Object o : list) {
				Long v = toLongOrNull(o);
				if (v != null) {
					ids.add(v);
				}
			}
			if (ids.isEmpty()) {
				return new GoodsIdParse(Optional.empty(), Optional.empty());
			}
			return new GoodsIdParse(Optional.empty(), Optional.of(List.copyOf(ids)));
		}
		if (raw instanceof Number n) {
			return new GoodsIdParse(Optional.of(n.longValue()), Optional.empty());
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return new GoodsIdParse(Optional.empty(), Optional.empty());
			}
			if (looksLikeJsonArray(t)) {
				try {
					List<Long> ids = MAPPER.readValue(t, new TypeReference<List<Long>>() {});
					if (ids == null || ids.isEmpty()) {
						return new GoodsIdParse(Optional.empty(), Optional.empty());
					}
					return new GoodsIdParse(Optional.empty(), Optional.of(List.copyOf(ids)));
				} catch (Exception e) {
					throw new BadRequestException("参数错误");
				}
			}
			try {
				long v = Long.parseLong(t);
				return new GoodsIdParse(Optional.of(v), Optional.empty());
			} catch (NumberFormatException e) {
				try {
					List<Long> ids = MAPPER.readValue(t, new TypeReference<List<Long>>() {});
					if (ids == null || ids.isEmpty()) {
						return new GoodsIdParse(Optional.empty(), Optional.empty());
					}
					return new GoodsIdParse(Optional.empty(), Optional.of(List.copyOf(ids)));
				} catch (Exception e2) {
					throw new BadRequestException("参数错误");
				}
			}
		}
		return new GoodsIdParse(Optional.empty(), Optional.empty());
	}

	private static boolean looksLikeJsonArray(String s) {
		return s.startsWith("[");
	}

	private static List<Long> parseDistributorIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				Long v = toLongOrNull(o);
				if (v != null) {
					out.add(v);
				}
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<Long> out = new ArrayList<>();
			for (Object o : arr) {
				Long v = toLongOrNull(o);
				if (v != null) {
					out.add(v);
				}
			}
			return out;
		}
		Long one = toLongOrNull(raw);
		if (one == null) {
			return List.of();
		}
		return List.of(one);
	}

	private static Optional<Long> parseOptionalLongKey(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return Optional.empty();
		}
		Object raw = merged.get(key);
		if (raw == null) {
			return Optional.empty();
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			return Optional.empty();
		}
		Long v = toLongOrNull(raw);
		return v != null ? Optional.of(v) : Optional.empty();
	}

	private static Long toLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseLongRequired(Object raw, String field) {
		Long v = toLongOrNull(raw);
		if (v == null) {
			throw new BadRequestException("参数错误");
		}
		return v;
	}

	private static boolean normalizeBool(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof String s) {
			return "true".equalsIgnoreCase(s.trim());
		}
		return false;
	}
}
