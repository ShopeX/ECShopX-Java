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

package cn.shopex.ecshopx.espier.service.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * 从 {@code item_multi_lang_mod_lang_*} 分表读取配置请求字段的多语言属性并合并到列表行。
 */
@Service
public class ConfigRequestFieldsMultiLangReadService {

	private static final String TABLE_NAME = "config_request_fields";
	private static final List<String> LANG_FIELDS =
			List.of("label", "validate_condition", "alert_required_message");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ConfigRequestFieldsMultiLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void applyToRows(List<Map<String, Object>> rows, String acceptLanguageHeader) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Integer> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Object idObj = row.get("id");
			if (idObj instanceof Number n) {
				int id = n.intValue();
				if (id > 0) {
					ids.add(id);
				}
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String suffix = normalizeLangSuffix(resolveAcceptLanguage(acceptLanguageHeader));
		String shard = "item_multi_lang_mod_lang_" + suffix;
		String sql = "SELECT data_id, field, attribute_value FROM `" + shard + "` WHERE table_name = :tableName"
				+ " AND data_id IN (:ids) AND field IN ('label','validate_condition','alert_required_message')";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("tableName", TABLE_NAME);
		params.addValue("ids", ids);
		List<Map<String, Object>> langRows = namedParameterJdbcTemplate.queryForList(sql, params);
		Map<Integer, Map<String, String>> byDataId = new HashMap<>();
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
			int dataId = did instanceof Number n ? n.intValue() : Integer.parseInt(did.toString().trim());
			String field = f.toString();
			if (!LANG_FIELDS.contains(field)) {
				continue;
			}
			String val = av == null ? "" : av.toString();
			if (!StringUtils.hasText(val)) {
				continue;
			}
			byDataId.computeIfAbsent(dataId, k -> new HashMap<>()).put(field, val);
		}
		for (Map<String, Object> row : rows) {
			Object idObj = row.get("id");
			if (!(idObj instanceof Number n)) {
				continue;
			}
			int id = n.intValue();
			Map<String, String> attrs = byDataId.get(id);
			if (attrs == null || attrs.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, String> e : attrs.entrySet()) {
				row.put(e.getKey(), e.getValue());
			}
		}
	}

	private static String resolveAcceptLanguage(String acceptLanguageHeader) {
		if (!StringUtils.hasText(acceptLanguageHeader)) {
			return "zh-CN";
		}
		String first = acceptLanguageHeader.split(",")[0].trim();
		int semi = first.indexOf(';');
		if (semi >= 0) {
			first = first.substring(0, semi).trim();
		}
		return StringUtils.hasText(first) ? first : "zh-CN";
	}

	private static String normalizeLangSuffix(String lang) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
	}
}
