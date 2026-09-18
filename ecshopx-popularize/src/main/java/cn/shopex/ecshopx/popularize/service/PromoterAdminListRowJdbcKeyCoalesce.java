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

package cn.shopex.ecshopx.popularize.service;

import java.util.Map;

/**
 * Normalizes {@code popularize_promoter} JDBC row keys to legacy snake_case names when MyBatis
 * underscore-to-camelCase mapping produced camelCase-only entries.
 */
final class PromoterAdminListRowJdbcKeyCoalesce {

	private PromoterAdminListRowJdbcKeyCoalesce() {
	}

	static void coalesceJdbcPromoterKeysToSnakeCase(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return;
		}
		coalesce(row, "company_id", "companyId");
		coalesce(row, "user_id", "userId");
		coalesce(row, "identity_id", "identityId");
		coalesce(row, "is_subordinates", "isSubordinates");
		coalesce(row, "shop_name", "shopName");
		coalesce(row, "alipay_name", "alipayName");
		coalesce(row, "shop_pic", "shopPic");
		coalesce(row, "alipay_account", "alipayAccount");
		coalesce(row, "grade_level", "gradeLevel");
		coalesce(row, "is_promoter", "isPromoter");
		coalesce(row, "shop_status", "shopStatus");
		coalesce(row, "is_buy", "isBuy");
		coalesce(row, "promoter_name", "promoterName");
		coalesce(row, "regions_id", "regionsId");
	}

	private static void coalesce(Map<String, Object> row, String snake, String camel) {
		if (!row.containsKey(camel)) {
			return;
		}
		if (!row.containsKey(snake)) {
			row.put(snake, row.get(camel));
		}
		row.remove(camel);
	}
}
