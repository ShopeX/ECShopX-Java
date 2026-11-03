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

package cn.shopex.ecshopx.distribution.service;

import java.util.Map;

/**
 * 店铺账号请求里 {@code distributor_id=0} 或未传时，与 {@code GET /distributor/items} 列表 Gate 一致：用 JWT 当前店补全。
 */
public final class DistributorShopOperatorIdResolver {

	private DistributorShopOperatorIdResolver() {
	}

	public static void applyJwtDistributorWhenQueryUnsetOrZero(
			Map<String, Object> merged, String operatorType, Map<String, Object> operatorJwt) {
		if (merged == null || !"distributor".equalsIgnoreCase(operatorType != null ? operatorType : "")) {
			return;
		}
		long target = merged.get("distributor_id") != null ? toLong(merged.get("distributor_id")) : 0L;
		if (target > 0L) {
			return;
		}
		if (operatorJwt == null) {
			return;
		}
		long selected = toLong(operatorJwt.get("distributor_id"));
		if (selected > 0L) {
			merged.put("distributor_id", selected);
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}
}
