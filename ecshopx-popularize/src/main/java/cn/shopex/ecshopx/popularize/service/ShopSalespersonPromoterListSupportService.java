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

package cn.shopex.ecshopx.popularize.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service("shopSalespersonPromoterListSupportService")
public class ShopSalespersonPromoterListSupportService {

	private static final String SQL_LIST_USER_IDS_FOR_GATE_NO_SHOP =
			"SELECT ranked.user_id FROM ("
					+ "SELECT user_id, created, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created DESC) AS rn "
					+ "FROM shop_salesperson WHERE company_id = :companyId"
					+ ") ranked WHERE ranked.rn = 1 ORDER BY ranked.created DESC LIMIT 1000";

	private static final String SQL_LIST_USER_IDS_FOR_GATE_WITH_SHOP =
			"SELECT ranked.user_id FROM ("
					+ "SELECT user_id, created, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created DESC) AS rn "
					+ "FROM shop_salesperson WHERE company_id = :companyId AND shop_id IN (:shopIds)"
					+ ") ranked WHERE ranked.rn = 1 ORDER BY ranked.created DESC LIMIT 1000";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ShopSalespersonPromoterListSupportService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public List<Long> listUserIdsForSalespersonGate(long companyId, List<Long> shopIds) {
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		String sql = SQL_LIST_USER_IDS_FOR_GATE_NO_SHOP;
		if (shopIds != null && !shopIds.isEmpty()) {
			List<String> shopIdStrs = new ArrayList<>(shopIds.size());
			for (Long id : shopIds) {
				if (id == null) {
					continue;
				}
				shopIdStrs.add(Long.toString(id.longValue()));
			}
			if (!shopIdStrs.isEmpty()) {
				sql = SQL_LIST_USER_IDS_FOR_GATE_WITH_SHOP;
				p.addValue("shopIds", shopIdStrs);
			}
		}
		List<Long> raw =
				namedParameterJdbcTemplate.query(
						sql,
						p,
						(rs, rowNum) -> {
							Object v = rs.getObject("user_id");
							if (v == null) {
								return null;
							}
							if (v instanceof Number n) {
								return n.longValue();
							}
							try {
								return Long.parseLong(String.valueOf(v).trim());
							} catch (NumberFormatException e) {
								return null;
							}
						});
		List<Long> out = new ArrayList<>();
		if (raw == null) {
			return out;
		}
		for (Long uid : raw) {
			if (uid == null || uid <= 0L) {
				continue;
			}
			out.add(uid);
		}
		return out;
	}

	public Map<Long, String> mapUserIdToSalespersonName(
			long companyId, List<Long> userIds, List<Long> optionalShopIdsForMerchant) {
		LinkedHashMap<Long, String> out = new LinkedHashMap<>();
		if (userIds == null || userIds.isEmpty()) {
			return out;
		}
		List<Long> positive = new ArrayList<>();
		for (Long uid : userIds) {
			if (uid != null && uid > 0L) {
				positive.add(uid);
			}
		}
		if (positive.isEmpty()) {
			return out;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT user_id, name FROM shop_salesperson WHERE company_id = :companyId ");
		sql.append("AND user_id IN (:userIds) ");
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("userIds", positive);
		if (optionalShopIdsForMerchant != null && !optionalShopIdsForMerchant.isEmpty()) {
			List<String> shopIdStrs = new ArrayList<>();
			for (Long id : optionalShopIdsForMerchant) {
				if (id == null) {
					continue;
				}
				shopIdStrs.add(Long.toString(id.longValue()));
			}
			if (!shopIdStrs.isEmpty()) {
				sql.append("AND shop_id IN (:shopIds) ");
				p.addValue("shopIds", shopIdStrs);
			}
		}
		sql.append("ORDER BY created_time ASC");
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql.toString(), p);
		for (Map<String, Object> row : rows) {
			Object uidO = row.get("user_id");
			if (uidO == null) {
				continue;
			}
			long uid =
					uidO instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uidO).trim());
			if (uid <= 0L) {
				continue;
			}
			Object nameO = row.get("name");
			out.put(uid, nameO != null ? String.valueOf(nameO) : "");
		}
		return out;
	}
}
