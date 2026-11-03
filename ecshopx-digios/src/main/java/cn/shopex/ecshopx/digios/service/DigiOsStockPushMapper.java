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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 对齐 PHP {@code DigiOsStockPushMapper}。 */
public final class DigiOsStockPushMapper {

	private static final TypeReference<List<Map<String, Object>>> SKU_LIST_TYPE = new TypeReference<>() {};

	private DigiOsStockPushMapper() {}

	public static List<Map<String, Object>> parseAndValidate(Object skuListRaw, int maxRows, ObjectMapper objectMapper) {
		List<Map<String, Object>> rows = parseRows(skuListRaw, objectMapper);
		if (rows.isEmpty()) {
			throw new ResourceException("库存数据为空");
		}
		if (rows.size() > maxRows) {
			throw new ResourceException("sku_list 单次最多 " + maxRows + " 条");
		}
		List<Map<String, Object>> normalized = new ArrayList<>(rows.size());
		for (Map<String, Object> row : rows) {
			if (row == null || row.isEmpty()) {
				throw new ResourceException("sku_list 必填且格式正确");
			}
			normalized.add(validateRow(row));
		}
		return normalized;
	}

	private static List<Map<String, Object>> parseRows(Object skuListRaw, ObjectMapper objectMapper) {
		if (skuListRaw == null) {
			throw new ResourceException("sku_list 必填且格式正确");
		}
		if (skuListRaw instanceof List<?> list) {
			return objectMapper.convertValue(list, SKU_LIST_TYPE);
		}
		if (skuListRaw instanceof String raw) {
			if (!StringUtils.hasText(raw)) {
				throw new ResourceException("sku_list 必填且格式正确");
			}
			try {
				Object parsed = objectMapper.readValue(raw.trim(), Object.class);
				if (!(parsed instanceof List<?>)) {
					throw new ResourceException("sku_list 必填且格式正确");
				}
				return objectMapper.convertValue(parsed, SKU_LIST_TYPE);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				throw new ResourceException("sku_list 必填且格式正确");
			}
		}
		throw new ResourceException("sku_list 必填且格式正确");
	}

	private static Map<String, Object> validateRow(Map<String, Object> row) {
		String skuId = stringField(row, "sku_id");
		if (!StringUtils.hasText(skuId)) {
			throw new ResourceException("sku_list 必填且格式正确");
		}
		if (!row.containsKey("stock")) {
			throw new ResourceException("stock 必须为 0-999999999 的整数");
		}
		int stock = parseStockStrict(row.get("stock"));
		if (isEmptyShopCode(row.get("shop_code"))) {
			throw new ResourceException("shop_code 必填");
		}
		String shopCode = String.valueOf(row.get("shop_code")).trim();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("sku_id", skuId);
		out.put("stock", stock);
		out.put("shop_code", shopCode);
		return out;
	}

	private static String stringField(Map<String, Object> row, String key) {
		Object raw = row.get(key);
		if (raw == null) {
			return null;
		}
		return String.valueOf(raw).trim();
	}

	static int parseStockStrict(Object stock) {
		if (stock == null) {
			throw new ResourceException("stock 必须为 0-999999999 的整数");
		}
		int value;
		if (stock instanceof Number n) {
			if (n.doubleValue() != n.longValue()) {
				throw new ResourceException("stock 必须为 0-999999999 的整数");
			}
			value = n.intValue();
		} else {
			String s = String.valueOf(stock).trim();
			if (!isStrictIntString(s)) {
				throw new ResourceException("stock 必须为 0-999999999 的整数");
			}
			try {
				value = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("stock 必须为 0-999999999 的整数");
			}
		}
		if (value < 0 || value > 999_999_999) {
			throw new ResourceException("stock 必须为 0-999999999 的整数");
		}
		return value;
	}

	private static boolean isStrictIntString(String s) {
		if (s.isEmpty()) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEmptyShopCode(Object shopCode) {
		if (shopCode == null) {
			return true;
		}
		if (shopCode instanceof Number n && n.intValue() == 0) {
			return true;
		}
		String s = String.valueOf(shopCode).trim();
		return s.isEmpty() || "0".equals(s);
	}
}
