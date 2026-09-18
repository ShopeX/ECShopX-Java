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

package cn.shopex.ecshopx.promotions.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class MarketingActivityListMultiLangReadService {

	private static final Logger log = LoggerFactory.getLogger(MarketingActivityListMultiLangReadService.class);

	private static final String MODULE_TABLE = "promotions_marketing_activity";
	private static final Pattern SAFE_LANG_SUFFIX = Pattern.compile("^[a-zA-Z0-9]+$");

	private final JdbcTemplate jdbcTemplate;

	public MarketingActivityListMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void applyListTranslations(long companyId, List<Map<String, Object>> rows, String acceptLanguageHeader) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> dataIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			try {
				dataIds.add(Long.parseLong(String.valueOf(row.get("marketing_id"))));
			} catch (NumberFormatException ignored) {
				// skip row without numeric marketing_id
			}
		}
		if (dataIds.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(acceptLanguageHeader) ? acceptLanguageHeader.trim() : "zh-CN";
		String suffix = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		if (!SAFE_LANG_SUFFIX.matcher(suffix).matches()) {
			suffix = "zhCN";
		}
		String table = "item_multi_lang_mod_lang_" + suffix;
		String inClause = dataIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
		if (!StringUtils.hasText(inClause)) {
			return;
		}
		String sql =
				"SELECT data_id, `field`, attribute_value FROM `"
						+ table
						+ "` WHERE company_id = ? AND table_name = ? AND data_id IN ("
						+ inClause
						+ ") AND `field` IN ('title','description')";
		try {
			List<Map<String, Object>> dbRows =
					jdbcTemplate.query(
							sql,
							(rs, i) -> {
								Map<String, Object> m = new LinkedHashMap<>();
								m.put("data_id", rs.getLong("data_id"));
								m.put("field", rs.getString("field"));
								m.put("attribute_value", rs.getString("attribute_value"));
								return m;
							},
							companyId,
							MODULE_TABLE);
			Map<Long, Map<String, String>> translations = new LinkedHashMap<>();
			for (Map<String, Object> r : dbRows) {
				long dataId = ((Number) r.get("data_id")).longValue();
				String field = String.valueOf(r.get("field"));
				String val = r.get("attribute_value") == null ? "" : String.valueOf(r.get("attribute_value"));
				translations.computeIfAbsent(dataId, k -> new LinkedHashMap<>()).put(field, val);
			}
			for (Map<String, Object> row : rows) {
				long dataId;
				try {
					dataId = Long.parseLong(String.valueOf(row.get("marketing_id")));
				} catch (NumberFormatException e) {
					continue;
				}
				Map<String, String> tr = translations.get(dataId);
				if (tr == null) {
					continue;
				}
				String title = tr.get("title");
				if (StringUtils.hasText(title)) {
					row.put("marketing_name", title);
				}
				String desc = tr.get("description");
				if (StringUtils.hasText(desc)) {
					row.put("marketing_desc", desc);
				}
			}
		} catch (Exception e) {
			log.debug("marketing_activity_list_multilang_read_failed table={} msg={}", table, e.getMessage());
		}
	}
}
