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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * Ensures AdaPay withdraw-set writes stay within the operator JWT shop scope.
 */
public final class AdapayWithdrawSetDistributorScopeGuard {

	private AdapayWithdrawSetDistributorScopeGuard() {}

	public static void assertTargetAllowed(long targetDistributorId, Map<String, Object> jwtMap) {
		if (targetDistributorId <= 0L) {
			throw new ForbiddenException("店铺权限不足");
		}
		if (jwtMap == null || jwtMap.isEmpty()) {
			throw new ForbiddenException("店铺权限不足");
		}
		if (isAdmin(jwtMap.get("operator_type"))) {
			return;
		}
		Set<Long> allowed = collectAllowedDistributorIds(jwtMap);
		if (allowed.isEmpty()) {
			// HQ-style: empty distributor_ids and no JWT shop → any store in company (company check elsewhere).
			return;
		}
		if (!allowed.contains(targetDistributorId)) {
			throw new ForbiddenException("店铺权限不足");
		}
	}

	private static boolean isAdmin(Object operatorTypeRaw) {
		if (operatorTypeRaw == null) {
			return false;
		}
		return "admin".equalsIgnoreCase(String.valueOf(operatorTypeRaw).trim());
	}

	private static Set<Long> collectAllowedDistributorIds(Map<String, Object> jwtMap) {
		Set<Long> allowed = new HashSet<>();
		Long jwtShop = parsePositiveLong(jwtMap.get("distributor_id"));
		if (jwtShop != null) {
			allowed.add(jwtShop);
		}
		Object raw = jwtMap.get("distributor_ids");
		if (raw instanceof Iterable<?> iterable) {
			for (Object element : iterable) {
				Long id = extractIdFromDistributorIdsEntry(element);
				if (id != null) {
					allowed.add(id);
				}
			}
		}
		return allowed;
	}

	private static Long extractIdFromDistributorIdsEntry(Object element) {
		if (element == null) {
			return null;
		}
		if (element instanceof Number || element instanceof String) {
			return parsePositiveLong(element);
		}
		if (element instanceof Map<?, ?> m) {
			Long fromDistributorId = parsePositiveLong(m.get("distributor_id"));
			if (fromDistributorId != null) {
				return fromDistributorId;
			}
			return parsePositiveLong(m.get("shop_id"));
		}
		return parsePositiveLong(element);
	}

	private static Long parsePositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

