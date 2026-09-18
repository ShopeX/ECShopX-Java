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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenapiDiscountCardV2ListFormatSupport {

	private OpenapiDiscountCardV2ListFormatSupport() {}

	public static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	public static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}

	public static String bcdivCentsToYuanString(Object cents) {
		return BigDecimal.valueOf(parseLongFlexible(cents))
				.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long parseLongFlexible(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number number) {
			return number.longValue();
		}
		try {
			String text = String.valueOf(raw).trim();
			if (text.isEmpty()) {
				return 0L;
			}
			if (text.contains(".")) {
				return (long) Double.parseDouble(text);
			}
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
