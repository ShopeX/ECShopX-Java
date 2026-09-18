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

package cn.shopex.ecshopx.espier.service.offline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class OfflineBankAccountMultiLangReadService {

	private static final String TABLE_NAME = "offline_bank_account";
	private static final List<String> LANG_FIELDS = List.of("bank_account_name", "bank_name", "remark");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public OfflineBankAccountMultiLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void applyToRows(List<Map<String, Object>> rows, String requestLangTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object idObj = row.get("id");
			long id = 0L;
			if (idObj instanceof Number n) {
				id = n.longValue();
			} else if (idObj != null) {
				try {
					id = Long.parseLong(idObj.toString().trim());
				} catch (NumberFormatException e) {
					id = 0L;
				}
			}
			if (id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String suffix = normalizeLangSuffix(requestLangTag);
		String shard = "outside_item_multi_lang_mod_lang_" + suffix;
		String sql = "SELECT data_id, field, attribute_value FROM `" + shard + "` WHERE table_name = :tableName"
				+ " AND data_id IN (:ids) AND field IN ('bank_account_name','bank_name','remark')";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("tableName", TABLE_NAME);
		params.addValue("ids", ids);
		List<Map<String, Object>> langRows = namedParameterJdbcTemplate.queryForList(sql, params);
		Map<Long, Map<String, String>> byIdField = new HashMap<>();
		for (Map<String, Object> lr : langRows) {
			Object did = lr.get("data_id");
			if (did == null) {
				did = lr.get("DATA_ID");
			}
			Object f = lr.get("field");
			if (f == null) {
				f = lr.get("FIELD");
			}
			Object av = lr.get("attribute_value");
			if (av == null) {
				av = lr.get("ATTRIBUTE_VALUE");
			}
			if (did == null || f == null) {
				continue;
			}
			long dataId = did instanceof Number n ? n.longValue() : Long.parseLong(did.toString().trim());
			String field = f.toString();
			if (!LANG_FIELDS.contains(field)) {
				continue;
			}
			String val = av == null ? "" : av.toString();
			if (!StringUtils.hasText(val)) {
				continue;
			}
			byIdField.computeIfAbsent(dataId, k -> new HashMap<>()).put(field, val);
		}
		for (Map<String, Object> row : rows) {
			Object idObj = row.get("id");
			long rowId = 0L;
			if (idObj instanceof Number n) {
				rowId = n.longValue();
			} else if (idObj != null) {
				try {
					rowId = Long.parseLong(idObj.toString().trim());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			if (rowId <= 0L) {
				continue;
			}
			Map<String, String> attrs = byIdField.get(rowId);
			if (attrs == null || attrs.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, String> e : attrs.entrySet()) {
				row.put(e.getKey(), e.getValue());
			}
		}
	}

	private static String normalizeLangSuffix(String lang) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
	}
}
