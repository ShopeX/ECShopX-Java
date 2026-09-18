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

package cn.shopex.ecshopx.shuyun.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.shuyun.service.openplatform.ShopSyncDispatchService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DistributorUpdateShuyunShopSyncDispatchListener implements DispatchListener {

	private final ShopSyncDispatchService shopSyncDispatchService;

	public DistributorUpdateShuyunShopSyncDispatchListener(ShopSyncDispatchService shopSyncDispatchService) {
		this.shopSyncDispatchService = shopSyncDispatchService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		Map<String, Object> entities = entitiesOf(payload);
		long companyId = toLong(first(entities, payload, "company_id", "companyId"));
		long distributorId = toLong(first(entities, payload, "distributor_id", "distributorId"));
		shopSyncDispatchService.dispatchIfAuthAllows(companyId, distributorId);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> entitiesOf(Map<String, Object> payload) {
		if (payload == null) {
			return Map.of();
		}
		Object raw = payload.get("entities");
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Map.of();
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
