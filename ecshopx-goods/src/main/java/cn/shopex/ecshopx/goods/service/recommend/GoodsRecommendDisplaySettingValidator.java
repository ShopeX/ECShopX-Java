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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendDisplaySetting;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.util.StringUtils;

/** 四页展示设置校验（SSOT §4.2） */
public final class GoodsRecommendDisplaySettingValidator {

	private GoodsRecommendDisplaySettingValidator() {}

	public static void validate(Map<String, Object> input, MessageSource messageSource) {
		validatePage(input, messageSource, "detail", "detail_enabled", "detail_limit", "detail_sort");
		validatePage(input, messageSource, "cart", "cart_enabled", "cart_limit", "cart_sort");
		validatePage(input, messageSource, "checkout", "checkout_enabled", "checkout_limit", "checkout_sort");
		validatePage(
				input, messageSource, "order_detail", "order_detail_enabled", "order_detail_limit", "order_detail_sort");
	}

	private static void validatePage(
			Map<String, Object> input,
			MessageSource messageSource,
			String pageLabel,
			String enabledKey,
			String limitKey,
			String sortKey) {
		int enabled = readFlag(input, enabledKey, pageLabel + " 开关");
		if (hasLimitValue(input, limitKey)) {
			int limit = readLimit(input, limitKey, pageLabel + " 展示数量", messageSource);
			if (limit < 1 || limit > GoodsRecommendDisplayDefaults.MAX_DISPLAY_LIMIT) {
				throw invalid(messageSource, limitKey);
			}
		}
		if (enabled == 0) {
			return;
		}
		int limit = readLimit(input, limitKey, pageLabel + " 展示数量", messageSource);
		readSort(input, sortKey, pageLabel + " 排序", messageSource);
		if (limit < 1 || limit > GoodsRecommendDisplayDefaults.MAX_DISPLAY_LIMIT) {
			throw invalid(messageSource, limitKey);
		}
	}

	private static boolean hasLimitValue(Map<String, Object> input, String limitKey) {
		Object raw = input.get(limitKey);
		return raw != null && StringUtils.hasText(raw.toString());
	}

	private static int readFlag(Map<String, Object> input, String key, String label) {
		Object raw = input.get(key);
		if (raw == null) {
			throw new BadRequestException(label + " 缺失");
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v != 0 && v != 1) {
				throw new BadRequestException(label + " 无效");
			}
			return v;
		}
		String s = raw.toString().trim();
		if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
			return 1;
		}
		if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
			return 0;
		}
		throw new BadRequestException(label + " 无效");
	}

	private static int readLimit(
			Map<String, Object> input, String key, String label, MessageSource messageSource) {
		Object raw = input.get(key);
		if (raw == null || raw.toString().isBlank()) {
			throw invalid(messageSource, key);
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw invalid(messageSource, key);
		}
	}

	private static void readSort(
			Map<String, Object> input, String key, String label, MessageSource messageSource) {
		Object raw = input.get(key);
		if (raw == null || raw.toString().isBlank()) {
			throw invalid(messageSource, key);
		}
		String sort = raw.toString().trim();
		if (!GoodsRecommendDisplaySort.isValid(sort)) {
			throw invalid(messageSource, key);
		}
	}

	private static BadRequestException invalid(MessageSource messageSource, String field) {
		String message =
				GoodsRecommendErrorMessages.message(
						messageSource, GoodsRecommendErrorCodes.DISPLAY_SETTING_INVALID);
		LinkedHashMap<String, java.util.List<String>> errors = new LinkedHashMap<>();
		errors.put(field, java.util.List.of(GoodsRecommendErrorCodes.DISPLAY_SETTING_INVALID));
		BadRequestException ex = new BadRequestException(message, errors);
		return ex;
	}

	public static GoodsRecommendDisplaySetting applyInput(GoodsRecommendDisplaySetting row, Map<String, Object> input) {
		row.setDetailEnabled(readFlagQuiet(input, "detail_enabled", row.getDetailEnabled()));
		row.setCartEnabled(readFlagQuiet(input, "cart_enabled", row.getCartEnabled()));
		row.setCheckoutEnabled(readFlagQuiet(input, "checkout_enabled", row.getCheckoutEnabled()));
		row.setOrderDetailEnabled(readFlagQuiet(input, "order_detail_enabled", row.getOrderDetailEnabled()));

		row.setDetailLimit(readLimitQuiet(input, "detail_limit", row.getDetailLimit()));
		row.setCartLimit(readLimitQuiet(input, "cart_limit", row.getCartLimit()));
		row.setCheckoutLimit(readLimitQuiet(input, "checkout_limit", row.getCheckoutLimit()));
		row.setOrderDetailLimit(readLimitQuiet(input, "order_detail_limit", row.getOrderDetailLimit()));

		row.setDetailSort(readSortQuiet(input, "detail_sort", row.getDetailSort()));
		row.setCartSort(readSortQuiet(input, "cart_sort", row.getCartSort()));
		row.setCheckoutSort(readSortQuiet(input, "checkout_sort", row.getCheckoutSort()));
		row.setOrderDetailSort(readSortQuiet(input, "order_detail_sort", row.getOrderDetailSort()));
		return row;
	}

	private static int readFlagQuiet(Map<String, Object> input, String key, Integer fallback) {
		if (!input.containsKey(key)) {
			return fallback != null ? fallback : 0;
		}
		return readFlag(input, key, key);
	}

	private static int readLimitQuiet(Map<String, Object> input, String key, Integer fallback) {
		if (!input.containsKey(key) || input.get(key) == null || input.get(key).toString().isBlank()) {
			return fallback != null ? fallback : GoodsRecommendDisplayDefaults.DEFAULT_LIMIT;
		}
		return GoodsRecommendDisplayDefaults.clampLimit(
				Integer.parseInt(input.get(key).toString().trim()));
	}

	private static String readSortQuiet(Map<String, Object> input, String key, String fallback) {
		if (!input.containsKey(key) || input.get(key) == null || input.get(key).toString().isBlank()) {
			return fallback != null ? fallback : GoodsRecommendDisplayDefaults.DEFAULT_SORT;
		}
		return input.get(key).toString().trim();
	}
}
