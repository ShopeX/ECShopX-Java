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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsBatchStoreRequestParser {

	private final ObjectMapper objectMapper;

	public ItemsBatchStoreRequestParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<ItemStoreBatchRow> parseAndValidateRows(Object itemsRaw) {
		if (itemsRaw == null) {
			throw new BadRequestException("未指定商品");
		}
		List<Map<String, Object>> rowMaps;
		if (itemsRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("未指定商品");
			}
			try {
				rowMaps = objectMapper.readValue(t, new TypeReference<List<Map<String, Object>>>() {});
			} catch (Exception e) {
				throw new BadRequestException("items 格式错误");
			}
			if (rowMaps.isEmpty()) {
				throw new BadRequestException("未指定商品");
			}
		} else if (itemsRaw instanceof List<?> rawList) {
			if (rawList.isEmpty()) {
				throw new BadRequestException("未指定商品");
			}
			rowMaps = new ArrayList<>();
			for (Object el : rawList) {
				if (!(el instanceof Map<?, ?> m)) {
					throw new BadRequestException("items 格式错误");
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> cast = (Map<String, Object>) m;
				rowMaps.add(cast);
			}
		} else {
			throw new BadRequestException("items 格式错误");
		}
		List<ItemStoreBatchRow> out = new ArrayList<>(rowMaps.size());
		for (Map<String, Object> m : rowMaps) {
			long itemId = parseRequiredPositiveItemId(m.get("item_id"));
			int store = parseRequiredStore(m.get("store"));
			Object isDefaultRaw = m.get("is_default");
			boolean treatDefault = isDefaultRaw != null && "true".equals(String.valueOf(isDefaultRaw).trim());
			out.add(new ItemStoreBatchRow(itemId, store, treatDefault));
		}
		return out;
	}

	private static long parseRequiredPositiveItemId(Object v) {
		if (v == null) {
			throw new BadRequestException("商品id必填");
		}
		if (v instanceof BigDecimal bd) {
			try {
				long lv = bd.longValueExact();
				if (lv <= 0L) {
					throw new BadRequestException("商品id必填");
				}
				return lv;
			} catch (ArithmeticException e) {
				throw new BadRequestException("商品id必填");
			}
		}
		if (v instanceof Number n) {
			if (n instanceof Double d) {
				if (!Double.isFinite(d) || d < 1.0d || d > (double) Long.MAX_VALUE || d != Math.rint(d)) {
					throw new BadRequestException("商品id必填");
				}
				return d.longValue();
			}
			if (n instanceof Float f) {
				if (!Float.isFinite(f) || f < 1.0f || f > (float) Long.MAX_VALUE || f != (float) Math.rint(f)) {
					throw new BadRequestException("商品id必填");
				}
				return f.longValue();
			}
			long lv = n.longValue();
			if (lv <= 0L) {
				throw new BadRequestException("商品id必填");
			}
			return lv;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			throw new BadRequestException("商品id必填");
		}
		try {
			long lv = Long.parseLong(s);
			if (lv <= 0L) {
				throw new BadRequestException("商品id必填");
			}
			return lv;
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品id必填");
		}
	}

	private static int parseRequiredStore(Object v) {
		if (v == null) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
		if (v instanceof BigDecimal bd) {
			try {
				long lv = bd.longValueExact();
				if (lv < 0L || lv > 999_999_999L) {
					throw new BadRequestException("库存为0-999999999的整数");
				}
				return (int) lv;
			} catch (ArithmeticException e) {
				throw new BadRequestException("库存为0-999999999的整数");
			}
		}
		if (v instanceof Number n) {
			if (n instanceof Double d) {
				if (!Double.isFinite(d) || d < 0.0d || d > 999_999_999.0d || d != Math.rint(d)) {
					throw new BadRequestException("库存为0-999999999的整数");
				}
				return (int) d.longValue();
			}
			if (n instanceof Float f) {
				if (!Float.isFinite(f) || f < 0.0f || f > 999_999_999.0f || f != (float) Math.rint(f)) {
					throw new BadRequestException("库存为0-999999999的整数");
				}
				return (int) f.longValue();
			}
			long lv = n.longValue();
			if (lv < 0L || lv > 999_999_999L) {
				throw new BadRequestException("库存为0-999999999的整数");
			}
			return (int) lv;
		}
		String s = v.toString().trim();
		try {
			long lv = Long.parseLong(s);
			if (lv < 0L || lv > 999_999_999L) {
				throw new BadRequestException("库存为0-999999999的整数");
			}
			return (int) lv;
		} catch (NumberFormatException e) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
	}
}
