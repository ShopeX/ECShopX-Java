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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemAddPushMarketingCenterProcessor {

	private static final String LOAD_ITEM_ROW =
			"SELECT item_bn, approve_status FROM items WHERE company_id = ? AND item_id = ? LIMIT 1";

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final JdbcTemplate jdbcTemplate;

	public ItemAddPushMarketingCenterProcessor(
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			JdbcTemplate jdbcTemplate) {
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		long companyId = readLong(payload.get("company_id"));
		long itemId = readLong(payload.get("item_id"));
		if (companyId <= 0L || itemId <= 0L) {
			return;
		}
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(LOAD_ITEM_ROW, companyId, itemId);
		if (rows.isEmpty()) {
			return;
		}
		Map<String, Object> row = rows.get(0);
		String itemBn = row.get("item_bn") == null ? "" : row.get("item_bn").toString();
		String approveStatus = row.get("approve_status") == null ? "" : row.get("approve_status").toString();

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("item_id", itemId);
		params.put("item_bn", itemBn);
		params.put("approve_status", approveStatus);
		marketingCenterOpenApiSignedFormClient.basicsItemProccess(companyId, params);
	}

	private static long readLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw != null && StringUtils.hasText(raw.toString())) {
			try {
				return Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
