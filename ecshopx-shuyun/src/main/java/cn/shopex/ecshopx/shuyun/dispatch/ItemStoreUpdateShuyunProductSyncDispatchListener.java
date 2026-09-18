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
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformProductSyncPort;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** A-PROD-02：店铺库存变更后同步商品至数云。 */
@Component
public class ItemStoreUpdateShuyunProductSyncDispatchListener implements DispatchListener {

	private final ShuyunOpenPlatformProductSyncPort productSyncPort;
	private final JdbcTemplate jdbcTemplate;

	public ItemStoreUpdateShuyunProductSyncDispatchListener(
			ShuyunOpenPlatformProductSyncPort productSyncPort, JdbcTemplate jdbcTemplate) {
		this.productSyncPort = productSyncPort;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		long distributorId = toLong(payload.get("distributor_id"));
		long itemId = toLong(payload.get("item_id"));
		if (distributorId < 1 || itemId < 1) {
			return;
		}
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT company_id, default_item_id FROM items WHERE item_id=? LIMIT 1
						""",
						itemId);
		if (rows.isEmpty()) {
			return;
		}
		Map<String, Object> item = rows.get(0);
		long companyId = toLong(item.get("company_id"));
		long defaultItemId = toLong(item.get("default_item_id"));
		if (defaultItemId < 1) {
			defaultItemId = itemId;
		}
		productSyncPort.dispatchIfAuthAllows(companyId, distributorId, defaultItemId);
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
