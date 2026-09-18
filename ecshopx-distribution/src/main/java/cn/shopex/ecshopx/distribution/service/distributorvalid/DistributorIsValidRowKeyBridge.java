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

package cn.shopex.ecshopx.distribution.service.distributorvalid;

import java.util.Map;

final class DistributorIsValidRowKeyBridge {

	private DistributorIsValidRowKeyBridge() {
	}

	static void ensureSnakeCaseCoreKeys(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return;
		}
		alias(row, "distributorId", "distributor_id");
		alias(row, "companyId", "company_id");
		alias(row, "distributorSelf", "distributor_self");
		alias(row, "isDefault", "is_default");
		alias(row, "isValid", "is_valid");
		alias(row, "isZiti", "is_ziti");
		alias(row, "isDelivery", "is_delivery");
		alias(row, "isDada", "is_dada");
		alias(row, "openDivided", "open_divided");
	}

	private static void alias(Map<String, Object> row, String camel, String snake) {
		if (!row.containsKey(snake) && row.containsKey(camel)) {
			row.put(snake, row.get(camel));
		}
	}
}
