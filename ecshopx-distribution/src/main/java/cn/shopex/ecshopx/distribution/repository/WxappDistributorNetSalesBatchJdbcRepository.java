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

package cn.shopex.ecshopx.distribution.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WxappDistributorNetSalesBatchJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public WxappDistributorNetSalesBatchJdbcRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Map<Long, Long> sumDoneOrderItemQtyByDistributorIds(long companyId, List<Long> distributorIds, long nowEpochSecond) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Map.of();
		}
		String sql =
				"""
				SELECT o.distributor_id AS distributor_id, COALESCE(SUM(i.num), 0) AS sales_count
				FROM orders_normal_orders o
				LEFT JOIN orders_normal_orders_items i ON o.order_id = i.order_id
				WHERE o.company_id = :companyId
				AND o.distributor_id IN (:ids)
				AND o.order_status = 'DONE'
				AND o.order_auto_close_aftersales_time < :nowTs
				GROUP BY o.distributor_id
				""";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("ids", distributorIds);
		p.addValue("nowTs", nowEpochSecond);
		Map<Long, Long> out = new HashMap<>();
		for (Map<String, Object> row : jdbc.queryForList(sql, p)) {
			out.put(((Number) row.get("distributor_id")).longValue(), ((Number) row.get("sales_count")).longValue());
		}
		return out;
	}

	public Map<Long, Long> sumDoneAftersalesItemQtyByDistributorIds(long companyId, List<Long> distributorIds, long nowEpochSecond) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Map.of();
		}
		String sql =
				"""
				SELECT a.distributor_id AS distributor_id, COALESCE(SUM(ad.num), 0) AS sales_count
				FROM aftersales a
				LEFT JOIN aftersales_detail ad ON a.aftersales_bn = ad.aftersales_bn
				LEFT JOIN orders_normal_orders o2 ON a.order_id = o2.order_id
				WHERE a.company_id = :companyId
				AND a.distributor_id IN (:ids)
				AND a.aftersales_status = 2
				AND o2.order_status = 'DONE'
				AND o2.order_auto_close_aftersales_time < :nowTs
				GROUP BY a.distributor_id
				""";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("ids", distributorIds);
		p.addValue("nowTs", nowEpochSecond);
		Map<Long, Long> out = new HashMap<>();
		for (Map<String, Object> row : jdbc.queryForList(sql, p)) {
			out.put(((Number) row.get("distributor_id")).longValue(), ((Number) row.get("sales_count")).longValue());
		}
		return out;
	}
}
