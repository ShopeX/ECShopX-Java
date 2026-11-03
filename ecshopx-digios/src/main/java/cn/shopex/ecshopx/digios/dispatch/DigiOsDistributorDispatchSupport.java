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

package cn.shopex.ecshopx.digios.dispatch;

import java.util.Map;

public final class DigiOsDistributorDispatchSupport {

	private DigiOsDistributorDispatchSupport() {}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> entitiesOf(Map<String, Object> payload) {
		if (payload == null) {
			return Map.of();
		}
		Object raw = payload.get("entities");
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Map.of();
	}

	public static long companyIdOf(Map<String, Object> payload) {
		Map<String, Object> entities = entitiesOf(payload);
		return toLong(first(entities, payload, "company_id", "companyId"));
	}

	static long distributorIdOf(Map<String, Object> payload) {
		Map<String, Object> entities = entitiesOf(payload);
		return toLong(first(entities, payload, "distributor_id", "distributorId"));
	}

	private static Object first(Map<String, Object> primary, Map<String, Object> fallback, String... keys) {
		for (String k : keys) {
			if (primary != null && primary.containsKey(k) && primary.get(k) != null) {
				return primary.get(k);
			}
		}
		for (String k : keys) {
			if (fallback != null && fallback.containsKey(k) && fallback.get(k) != null) {
				return fallback.get(k);
			}
		}
		return null;
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			return 0L;
		}
	}
}
