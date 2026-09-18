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

import cn.shopex.ecshopx.goods.domain.Items;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemDeleteEventItemInfoMapper {

	private ItemDeleteEventItemInfoMapper() {
	}

	public static Map<String, Object> toItemInfoMap(Items row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", row.getItemId() == null ? null : row.getItemId());
		m.put("company_id", row.getCompanyId() == null ? null : row.getCompanyId());
		m.put("item_bn", row.getItemBn());
		putString(m, "item_name", row.getItemName());
		putString(m, "item_type", row.getItemType());
		putString(m, "approve_status", row.getApproveStatus());
		putString(m, "nospec", row.getNospec());
		putLong(m, "default_item_id", row.getDefaultItemId());
		putInt(m, "distributor_id", row.getDistributorId());
		putInt(m, "price", row.getPrice());
		putString(m, "barcode", row.getBarcode());
		return m;
	}

	private static void putLong(Map<String, Object> m, String key, Long v) {
		if (v != null) {
			m.put(key, v);
		}
	}

	private static void putInt(Map<String, Object> m, String key, Integer v) {
		if (v != null) {
			m.put(key, v.longValue());
		}
	}

	private static void putString(Map<String, Object> m, String key, String v) {
		if (v != null) {
			m.put(key, v);
		}
	}
}
