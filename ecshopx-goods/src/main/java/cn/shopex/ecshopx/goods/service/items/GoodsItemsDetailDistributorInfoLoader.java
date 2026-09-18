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

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GoodsItemsDetailDistributorInfoLoader {

	private static final List<String> FORMATTED_KEYS =
			List.of("store_address", "store_name", "phone", "selfDeliveryRule", "rate");

	private static final Set<String> REMOVE_KEYS =
			Set.of("merchant_name", "source_from", "show_mobile", "show_salesperson", "distributor_category_id");

	private GoodsItemsDetailDistributorInfoLoader() {
	}

	static Object load(AdminOrderDetailDistributionSupportPort port, long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return List.of();
		}
		Map<String, Object> info =
				new LinkedHashMap<>(port.getDistributorInfoSimple(companyId, String.valueOf(distributorId)));
		if (info.isEmpty()) {
			return List.of();
		}
		Map<String, Object> formatted = port.getDistributorInfoFormatted(companyId, distributorId);
		for (String key : FORMATTED_KEYS) {
			if (formatted.containsKey(key)) {
				info.put(key, formatted.get(key));
			}
		}
		for (String key : REMOVE_KEYS) {
			info.remove(key);
		}
		Object offline = info.get("offline_aftersales_distributor_id");
		if (offline instanceof String s && s.isEmpty()) {
			info.put("offline_aftersales_distributor_id", null);
		}
		return info;
	}
}
