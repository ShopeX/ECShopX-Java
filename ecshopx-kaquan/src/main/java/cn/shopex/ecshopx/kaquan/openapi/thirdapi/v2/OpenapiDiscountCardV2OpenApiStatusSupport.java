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

import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import java.util.Map;

public final class OpenapiDiscountCardV2OpenApiStatusSupport {

	private OpenapiDiscountCardV2OpenApiStatusSupport() {}

	public static String mapFromEntity(UserDiscount row) {
		int dbStatus = row.getStatus() == null ? 0 : row.getStatus();
		Integer endDate = row.getEndDate();
		return mapFromDbStatusAndEndDate(dbStatus, endDate);
	}

	public static String mapFromDbRow(Map<String, Object> row) {
		int dbStatus = parseIntFlexible(row.get("status"));
		Integer endDate = parseEndDate(row.get("end_date"));
		return mapFromDbStatusAndEndDate(dbStatus, endDate);
	}

	private static String mapFromDbStatusAndEndDate(int dbStatus, Integer endDate) {
		int mapped;
		if (dbStatus == 4 || dbStatus == 10 || dbStatus == 1) {
			mapped = 1;
			long nowSec = System.currentTimeMillis() / 1000L;
			if (endDate != null && endDate > 0 && endDate < nowSec) {
				mapped = 5;
			}
		} else if (dbStatus == 6) {
			mapped = 5;
		} else {
			mapped = dbStatus;
		}
		return switch (mapped) {
			case 1 -> "unused";
			case 2 -> "redeemed";
			case 5 -> "expired";
			default -> "";
		};
	}

	private static Integer parseEndDate(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			return number.intValue();
		}
		try {
			String text = String.valueOf(raw).trim();
			if (text.isEmpty()) {
				return null;
			}
			if (text.contains(".")) {
				return (int) Double.parseDouble(text);
			}
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parseIntFlexible(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number number) {
			return number.intValue();
		}
		try {
			String text = String.valueOf(raw).trim();
			if (text.isEmpty()) {
				return 0;
			}
			if (text.contains(".")) {
				return (int) Double.parseDouble(text);
			}
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
