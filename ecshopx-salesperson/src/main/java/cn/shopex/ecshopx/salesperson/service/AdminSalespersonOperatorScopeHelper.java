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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminSalespersonOperatorScopeHelper {

	public void assertShopIdsAllowed(Map<String, Object> operatorJwt, List<Long> shopIds) {
		if (shopIds.isEmpty()) {
			return;
		}
		Object raw = operatorJwt.get("shop_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		List<Long> allowed = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> m) {
				Long id = readShopIdFromClaimEntry(m);
				if (id != null) {
					allowed.add(id);
				}
			}
		}
		if (allowed.isEmpty()) {
			return;
		}
		Set<Long> allowSet = new HashSet<>(allowed);
		for (Long id : shopIds) {
			if (!allowSet.contains(id)) {
				throw new ForbiddenException("门店权限不足");
			}
		}
	}

	public void assertDistributorIdsAllowed(Map<String, Object> operatorJwt, List<Long> distributorIds) {
		if (distributorIds.isEmpty()) {
			return;
		}
		Object raw = operatorJwt.get("distributor_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		List<Long> allowed = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> m) {
				Long id = readShopIdFromClaimEntry(m);
				if (id != null) {
					allowed.add(id);
				}
			}
		}
		if (allowed.isEmpty()) {
			return;
		}
		Set<Long> allowSet = new HashSet<>(allowed);
		for (Long id : distributorIds) {
			if (!allowSet.contains(id)) {
				throw new ForbiddenException("店铺权限不足");
			}
		}
	}

	private static Long readShopIdFromClaimEntry(Map<?, ?> m) {
		Object v = m.get("shop_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
