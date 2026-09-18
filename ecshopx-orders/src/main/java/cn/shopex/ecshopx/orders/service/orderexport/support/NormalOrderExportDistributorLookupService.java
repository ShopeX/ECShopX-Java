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

package cn.shopex.ecshopx.orders.service.orderexport.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderExportDistributorLookupService {

	private static final String SQL = """
			SELECT distributor_id, name, shop_code
			FROM distribution_distributor
			WHERE company_id = :companyId AND distributor_id IN (:distributorIds)
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public NormalOrderExportDistributorLookupService(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Map<Long, StoreInfo> loadStores(long companyId, List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = distributorIds.stream().filter(id -> id != null && id > 0L).distinct().toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		MapSqlParameterSource p =
				new MapSqlParameterSource("companyId", companyId).addValue("distributorIds", ids);
		List<Map<String, Object>> rows = jdbc.queryForList(SQL, p);
		Map<Long, StoreInfo> out = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Long did = longObj(row.get("distributor_id"));
			if (did == null) {
				continue;
			}
			String name = row.get("name") == null ? "" : String.valueOf(row.get("name"));
			String shopCode = row.get("shop_code") == null ? "" : String.valueOf(row.get("shop_code"));
			out.put(did, new StoreInfo(nz(name), nz(shopCode)));
		}
		return out;
	}

	private static Long longObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String nz(String s) {
		return StringUtils.hasText(s) ? s : "";
	}

	public record StoreInfo(String name, String shopCode) {
		public static final StoreInfo EMPTY = new StoreInfo("", "");
	}
}
