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

package cn.shopex.ecshopx.orders.service.normal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OrderDiscountInfoSupport {

	private OrderDiscountInfoSupport() {}

	public static List<Map<String, Object>> parseDiscountInfoJson(String json, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			Object parsed = objectMapper.readValue(json.trim(), Object.class);
			return entriesFromParsed(parsed);
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	public static List<Map<String, Object>> parseDiscountInfoRaw(Object raw, ObjectMapper objectMapper) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> || raw instanceof Map<?, ?>) {
			return entriesFromParsed(raw);
		}
		if (raw instanceof String s) {
			return parseDiscountInfoJson(s, objectMapper);
		}
		return List.of();
	}

	public static int computeItemCnyFee(Float feeRate, Integer totalFee) {
		double rate = feeRate == null ? 0.0d : feeRate.doubleValue();
		int total = totalFee == null ? 0 : totalFee;
		BigDecimal fr = BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP);
		return fr.multiply(BigDecimal.valueOf(total)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	public static Map<String, Object> findCouponEntryFromDiscountInfo(Object raw) {
		for (Map<String, Object> entry : entriesFromParsed(raw)) {
			String type = String.valueOf(entry.getOrDefault("type", "")).trim();
			if ("coupon_discount".equals(type) || "cash_discount".equals(type)) {
				return entry;
			}
		}
		return null;
	}

	public static Map<String, Object> copyCouponDiscountDesc(Object couponInfo) {
		if (!(couponInfo instanceof Map<?, ?> source)) {
			return null;
		}
		Map<String, Object> desc = toStringKeyMap(source);
		String type = String.valueOf(desc.getOrDefault("type", "")).trim();
		if (type.isEmpty()) {
			desc.put("type", "coupon_discount");
		}
		return desc;
	}

	public static List<Map<String, Object>> buildItemDiscountInfo(
			String discountInfoJson,
			String couponDiscountDesc,
			String memberDiscountDesc,
			ObjectMapper objectMapper) {
		if (StringUtils.hasText(discountInfoJson)) {
			return parseDiscountInfoJson(discountInfoJson, objectMapper);
		}
		List<Map<String, Object>> list = new ArrayList<>();
		if (StringUtils.hasText(couponDiscountDesc)) {
			try {
				Map<String, Object> cm = objectMapper.readValue(couponDiscountDesc, new TypeReference<>() {});
				if (cm != null) {
					cm.put("type", "coupon_discount");
					list.add(cm);
				}
			} catch (Exception ignored) {
				// skip
			}
		}
		if (StringUtils.hasText(memberDiscountDesc)) {
			try {
				Map<String, Object> mm = objectMapper.readValue(memberDiscountDesc, new TypeReference<>() {});
				if (mm != null) {
					mm.put("type", "member_discount");
					list.add(mm);
				}
			} catch (Exception ignored) {
				// skip
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> entriesFromParsed(Object parsed) {
		List<Map<String, Object>> list = new ArrayList<>();
		if (parsed instanceof List<?> l) {
			for (Object el : l) {
				if (el instanceof Map<?, ?> m) {
					list.add(toStringKeyMap(m));
				}
			}
			return list;
		}
		if (parsed instanceof Map<?, ?> map) {
			for (Object v : map.values()) {
				if (v instanceof Map<?, ?> m) {
					list.add(toStringKeyMap(m));
				}
			}
		}
		return list;
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> source) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : source.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}
}
