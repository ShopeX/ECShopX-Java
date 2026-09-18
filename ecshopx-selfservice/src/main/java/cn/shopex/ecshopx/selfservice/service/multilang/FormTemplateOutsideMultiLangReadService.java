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

package cn.shopex.ecshopx.selfservice.service.multilang;

import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangWriteService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FormTemplateOutsideMultiLangReadService {

	private static final Logger log = LoggerFactory.getLogger(FormTemplateOutsideMultiLangReadService.class);

	private static final String TABLE_MODULE = "selfservice_form_template";

	private final JdbcTemplate jdbcTemplate;

	public FormTemplateOutsideMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<String> loadTemNameOverride(long companyId, long templateId, String requestLangTag) {
		if (templateId <= 0L) {
			return Optional.empty();
		}
		String suffix = null;
		String langTable = null;
		try {
			suffix = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
			langTable = "outside_item_multi_lang_mod_lang_" + suffix;
			String sql = "SELECT attribute_value FROM `"
					+ langTable
					+ "` WHERE company_id = ? AND table_name = ? AND data_id = ? AND field = 'tem_name' LIMIT 1";
			String v;
			try {
				v = jdbcTemplate.queryForObject(sql, String.class, companyId, TABLE_MODULE, templateId);
			} catch (EmptyResultDataAccessException ex) {
				return Optional.empty();
			}
			if (!StringUtils.hasText(v)) {
				return Optional.empty();
			}
			return Optional.of(v.trim());
		} catch (Exception e) {
			log.debug(
					"loadTemNameOverride failed companyId={} templateId={} requestLangTag={} suffix={} langTable={} msg={}",
					companyId,
					templateId,
					requestLangTag != null ? requestLangTag : "",
					suffix != null ? suffix : "",
					langTable != null ? langTable : "",
					e.getMessage(),
					e);
			return Optional.empty();
		}
	}

	public void applyListLangOverrides(long companyId, List<Map<String, Object>> rows, String requestLangTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		Set<Long> seen = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Long id = resolveRowId(row.get("id"));
			if (id != null && seen.add(id)) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String suffix = null;
		String langTable = null;
		try {
			suffix = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
			langTable = "outside_item_multi_lang_mod_lang_" + suffix;
			String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));
			String sql = "SELECT data_id, field, attribute_value FROM `"
					+ langTable
					+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND field IN ('tem_name','content','header_link_title','header_bg_pic','header_title') AND data_id IN ("
					+ inPlaceholders
					+ ") ORDER BY id ASC";
			List<Object> args = new ArrayList<>();
			args.add(companyId);
			args.add(TABLE_MODULE);
			args.add(TABLE_MODULE);
			args.addAll(ids);
			Map<Long, Map<String, String>> byDataId = jdbcTemplate.query(
					sql,
					rs -> {
						Map<Long, Map<String, String>> acc = new HashMap<>();
						while (rs.next()) {
							long dataId = rs.getLong("data_id");
							String field = rs.getString("field");
							String value = rs.getString("attribute_value");
							acc.computeIfAbsent(dataId, k -> new HashMap<>()).put(field, value);
						}
						return acc;
					},
					args.toArray());
			if (byDataId == null || byDataId.isEmpty()) {
				return;
			}
			for (Map<String, Object> row : rows) {
				Long dataId = resolveRowId(row.get("id"));
				if (dataId == null) {
					continue;
				}
				Map<String, String> langFields = byDataId.get(dataId);
				if (langFields == null) {
					continue;
				}
				for (Map.Entry<String, String> e : langFields.entrySet()) {
					String field = e.getKey();
					String attr = e.getValue();
					if (!StringUtils.hasText(attr)) {
						continue;
					}
					if ("content".equals(field)) {
						row.put("content", attr);
					} else {
						row.put(field, attr.trim());
					}
				}
			}
		} catch (Exception e) {
			log.debug(
					"applyListLangOverrides failed companyId={} requestLangTag={} suffix={} langTable={} msg={}",
					companyId,
					requestLangTag != null ? requestLangTag : "",
					suffix != null ? suffix : "",
					langTable != null ? langTable : "",
					e.getMessage(),
					e);
		}
	}

	private static Long resolveRowId(Object idObj) {
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		if (idObj instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
