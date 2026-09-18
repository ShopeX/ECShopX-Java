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

package cn.shopex.ecshopx.companys.service.employee;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * Loads per-locale attribute rows for {@code operators} from {@code outside_item_multi_lang_mod_lang_*},
 * matching the repository list enrichment used on the legacy side for {@code username} / {@code contact} /
 * {@code split_ledger_info}.
 */
@Service
public class OperatorAccountOutsideLangReadService {

	private static final String TABLE_NAME = "operators";

	private static final List<String> LANG_FIELDS =
			List.of("username", "contact", "split_ledger_info");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public OperatorAccountOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Merges translated values and {@code <field>_lang} maps into list rows for the request locale.
	 * Query shape aligns with {@code CommonLangModService::getListAddLang} (filters {@code company_id},
	 * {@code table_name}, {@code data_id}, {@code field} only).
	 */
	public void applyForAccountList(long companyId, String requestLangTag, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long oid = longOrNull(row.get("operator_id"));
			if (oid != null && oid > 0L) {
				ids.add(oid);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String langKey = displayLangKey(requestLangTag);
		String suffix = normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + suffix;

		Map<Long, Map<String, String>> byOp = new LinkedHashMap<>();
		loadFromOutsideTable(companyId, ids, outsideTable, byOp);

		for (Map<String, Object> row : rows) {
			Long oid = longOrNull(row.get("operator_id"));
			if (oid == null || oid <= 0L) {
				continue;
			}
			Map<String, String> perField = byOp.get(oid);
			if (perField == null || perField.isEmpty()) {
				continue;
			}
			for (String field : LANG_FIELDS) {
				String val = perField.get(field);
				if (!StringUtils.hasText(val)) {
					continue;
				}
				row.put(field, val);
				Map<String, Object> langMap = new LinkedHashMap<>();
				langMap.put(langKey, val);
				row.put(field + "_lang", langMap);
			}
		}
	}

	private void loadFromOutsideTable(
			long companyId, List<Long> ids, String outsideTable, Map<Long, Map<String, String>> byOp) {
		String sql =
				"SELECT data_id, `field`, attribute_value FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name "
						+ "AND data_id IN (:ids) AND `field` IN (:fields)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_NAME);
		p.addValue("ids", ids);
		p.addValue("fields", LANG_FIELDS);
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String field = rs.getString("field");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(field)) {
							return;
						}
						byOp.computeIfAbsent(dataId, k -> new LinkedHashMap<>())
								.put(field, av != null ? av : "");
					});
		} catch (DataAccessException ignored) {
			// Locale-specific table may be missing in some environments.
		}
	}

	private static String displayLangKey(String requestLangTag) {
		if (!StringUtils.hasText(requestLangTag)) {
			return "zh-CN";
		}
		return requestLangTag.trim();
	}

	static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

