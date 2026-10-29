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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.distribution.service.DistributorPlatformStoreBuySupport;
import java.util.List;
import java.util.Map;

/** Aligns with PHP {@code ItemsService::appendPlatformStoreForOnsaleSkuList}. */
public final class OnsalePlatformStoreSupport {

	private OnsalePlatformStoreSupport() {
	}

	public static List<Map<String, Object>> appendPlatformStoreForOnsaleSkuList(
			List<Map<String, Object>> list, boolean injectPlatformStore) {
		if (!injectPlatformStore || list == null || list.isEmpty()) {
			return list;
		}
		for (Map<String, Object> row : list) {
			row.put("platform_store", resolvePlatformStoreForSkuRow(row));
		}
		return list;
	}

	public static int resolvePlatformStoreForSkuRow(Map<String, Object> row) {
		if (row == null) {
			return 0;
		}
		if (row.containsKey("logistics_store")) {
			return Math.max(0, intVal(row.get("logistics_store")));
		}
		if (DistributorPlatformStoreBuySupport.isSkuHeadquartersShipment(row)) {
			return Math.max(0, intVal(row.get("store")));
		}
		if (row.containsKey("distributor_store")) {
			return 0;
		}
		return Math.max(0, intVal(row.get("store")));
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
