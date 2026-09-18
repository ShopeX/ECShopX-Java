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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsBatchStoreUpdateService {

	private static final String STORE_VALIDATION_MSG = "库存为0-999999999的整数";

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemStoreRedisWriteService pointsmallItemStoreRedisWriteService;
	private final ObjectMapper objectMapper;

	public PointsmallItemsBatchStoreUpdateService(
			PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemStoreRedisWriteService pointsmallItemStoreRedisWriteService,
			ObjectMapper objectMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemStoreRedisWriteService = pointsmallItemStoreRedisWriteService;
		this.objectMapper = objectMapper;
	}

	public void updateFromMerged(long companyId, Map<String, Object> merged) {
		List<Map<String, Object>> rows = resolveItemsList(merged.get("items"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Map<String, Object> row : rows) {
			long itemIdOrDefaultId = parseRequiredPositiveItemId(row.get("item_id"));
			int store = parseRequiredStore(row.get("store"));
			boolean isDefaultSpu = isDefaultSpuFlag(row);
			if (isDefaultSpu) {
				applyDefaultSpuBranch(companyId, itemIdOrDefaultId, store, now);
			} else {
				applySingleItemBranch(itemIdOrDefaultId, store, now);
			}
		}
	}

	private void applyDefaultSpuBranch(long companyId, long defaultItemId, int store, int now) {
		LambdaUpdateWrapper<PointsmallItems> u = new LambdaUpdateWrapper<PointsmallItems>()
				.eq(PointsmallItems::getCompanyId, companyId)
				.eq(PointsmallItems::getDefaultItemId, defaultItemId)
				.set(PointsmallItems::getStore, store)
				.set(PointsmallItems::getUpdated, now);
		pointsmallItemsMapper.update(null, u);

		LambdaQueryWrapper<PointsmallItems> q = new LambdaQueryWrapper<PointsmallItems>()
				.eq(PointsmallItems::getCompanyId, companyId)
				.eq(PointsmallItems::getDefaultItemId, defaultItemId)
				.select(PointsmallItems::getItemId);
		List<PointsmallItems> skus = pointsmallItemsMapper.selectList(q);
		for (PointsmallItems sku : skus) {
			Long id = sku.getItemId();
			if (id != null) {
				pointsmallItemStoreRedisWriteService.save(id, store);
			}
		}
	}

	private void applySingleItemBranch(long itemId, int store, int now) {
		PointsmallItems entity = pointsmallItemsMapper.selectById(itemId);
		if (entity != null) {
			entity.setStore(store);
			entity.setUpdated(now);
			pointsmallItemsMapper.updateById(entity);
		}
		pointsmallItemStoreRedisWriteService.save(itemId, store);
	}

	private static boolean isDefaultSpuFlag(Map<String, Object> row) {
		if (!row.containsKey("is_default")) {
			return false;
		}
		Object raw = row.get("is_default");
		if (Boolean.TRUE.equals(raw)) {
			return true;
		}
		return "true".equalsIgnoreCase(String.valueOf(raw).trim());
	}

	private List<Map<String, Object>> resolveItemsList(Object raw) {
		if (raw == null) {
			throw new BadRequestException("未指定商品");
		}
		if (raw instanceof Boolean b && Boolean.FALSE.equals(b)) {
			throw new BadRequestException("未指定商品");
		}
		if (isNumericZero(raw)) {
			throw new BadRequestException("未指定商品");
		}

		Object current;
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("未指定商品");
			}
			try {
				current = objectMapper.readValue(t, Object.class);
			} catch (Exception e) {
				throw new BadRequestException("items 格式错误");
			}
		} else {
			current = raw;
		}

		if (current == null || (current instanceof Boolean b && Boolean.FALSE.equals(b)) || isNumericZero(current)) {
			throw new BadRequestException("未指定商品");
		}

		if (current instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("未指定商品");
			}
			if (!(current instanceof List<?> list)) {
				throw new BadRequestException("items 格式错误");
			}
			return normalizeItemsList(list);
		}

		throw new BadRequestException("items 格式错误");
	}

	private static boolean isNumericZero(Object o) {
		if (!(o instanceof Number n)) {
			return false;
		}
		if (n instanceof BigDecimal bd) {
			return bd.compareTo(BigDecimal.ZERO) == 0;
		}
		if (n instanceof Double d) {
			return d == 0.0d;
		}
		if (n instanceof Float f) {
			return f == 0.0f;
		}
		return n.longValue() == 0L;
	}

	private static List<Map<String, Object>> normalizeItemsList(List<?> list) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				throw new BadRequestException("商品id必填");
			}
			Map<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			out.add(row);
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
			throw new BadRequestException(STORE_VALIDATION_MSG);
		}
		if (v instanceof BigDecimal bd) {
			try {
				long lv = bd.longValueExact();
				if (lv < 0L || lv > 999_999_999L) {
					throw new BadRequestException(STORE_VALIDATION_MSG);
				}
				return (int) lv;
			} catch (ArithmeticException e) {
				throw new BadRequestException(STORE_VALIDATION_MSG);
			}
		}
		if (v instanceof Number n) {
			if (n instanceof Double d) {
				if (!Double.isFinite(d) || d < 0.0d || d > 999_999_999.0d || d != Math.rint(d)) {
					throw new BadRequestException(STORE_VALIDATION_MSG);
				}
				return (int) d.longValue();
			}
			if (n instanceof Float f) {
				if (!Float.isFinite(f) || f < 0.0f || f > 999_999_999.0f || f != (float) Math.rint(f)) {
					throw new BadRequestException(STORE_VALIDATION_MSG);
				}
				return (int) f.longValue();
			}
			long lv = n.longValue();
			if (lv < 0L || lv > 999_999_999L) {
				throw new BadRequestException(STORE_VALIDATION_MSG);
			}
			return (int) lv;
		}
		String s = v.toString().trim();
		try {
			long lv = Long.parseLong(s);
			if (lv < 0L || lv > 999_999_999L) {
				throw new BadRequestException(STORE_VALIDATION_MSG);
			}
			return (int) lv;
		} catch (NumberFormatException e) {
			throw new BadRequestException(STORE_VALIDATION_MSG);
		}
	}
}
