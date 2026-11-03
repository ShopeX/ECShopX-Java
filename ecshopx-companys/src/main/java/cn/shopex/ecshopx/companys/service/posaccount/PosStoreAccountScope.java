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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.service.employee.EmployeeOrchestrationSupport;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PosStoreAccountScope {

	private PosStoreAccountScope() {}

	static boolean isAdmin(Map<String, Object> jwt) {
		return "admin".equals(EmployeeOrchestrationSupport.strClaim(jwt, "operator_type", "operatorType"));
	}

	static long companyId(Map<String, Object> jwt) {
		return EmployeeOrchestrationSupport.requireLongClaim(jwt, "company_id", "companyId");
	}

	static Set<Long> allowedShopIds(Map<String, Object> jwt) {
		return parseIds(jwt.get("distributor_ids"));
	}

	static Set<Long> requireReadableShops(Map<String, Object> jwt) {
		if (isAdmin(jwt)) {
			return Set.of();
		}
		Set<Long> allowed = allowedShopIds(jwt);
		if (allowed.isEmpty()) {
			throw new ForbiddenException("无访问权限");
		}
		return allowed;
	}

	static void assertShopAllowed(Map<String, Object> jwt, Long shopId) {
		if (isAdmin(jwt)) {
			return;
		}
		if (shopId == null || shopId <= 0 || !allowedShopIds(jwt).contains(shopId)) {
			throw new ForbiddenException("无访问权限");
		}
	}

	static Set<Long> parseIds(Object raw) {
		Set<Long> out = new HashSet<>();
		if (raw == null) {
			return out;
		}
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				addId(out, o);
			}
			return out;
		}
		addId(out, raw);
		return out;
	}

	private static void addId(Set<Long> out, Object o) {
		if (o == null) {
			return;
		}
		if (o instanceof Number n) {
			if (n.longValue() > 0) {
				out.add(n.longValue());
			}
			return;
		}
		if (o instanceof Map<?, ?> m) {
			Object nested = m.get("distributor_id");
			if (nested == null) {
				nested = m.get("id");
			}
			addId(out, nested);
			return;
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return;
		}
		try {
			long v = Long.parseLong(s);
			if (v > 0) {
				out.add(v);
			}
		} catch (NumberFormatException ignored) {
		}
	}
}
