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
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityOutsideMultiLangReadService {

	private static final Logger log =
			LoggerFactory.getLogger(RegistrationActivityOutsideMultiLangReadService.class);

	private static final String TABLE_MODULE = "selfservice_registration_activity";

	private final JdbcTemplate jdbcTemplate;

	public RegistrationActivityOutsideMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void applyActivityNameOverrides(long companyId, List<Map<String, Object>> rows, String requestLangTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		Set<Long> seen = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Long id = resolveActivityId(row.get("activity_id"));
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
			String sql = "SELECT data_id, attribute_value FROM `"
					+ langTable
					+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND field = 'activity_name' AND data_id IN ("
					+ inPlaceholders
					+ ")";
			List<Object> args = new ArrayList<>();
			args.add(companyId);
			args.add(TABLE_MODULE);
			args.add(TABLE_MODULE);
			args.addAll(ids);
			Map<Long, String> byDataId = jdbcTemplate.query(
					sql,
					rs -> {
						Map<Long, String> acc = new HashMap<>();
						while (rs.next()) {
							long dataId = rs.getLong("data_id");
							String value = rs.getString("attribute_value");
							acc.put(dataId, value);
						}
						return acc;
					},
					args.toArray());
			if (byDataId == null || byDataId.isEmpty()) {
				return;
			}
			for (Map<String, Object> row : rows) {
				Long dataId = resolveActivityId(row.get("activity_id"));
				if (dataId == null) {
					continue;
				}
				String attr = byDataId.get(dataId);
				if (!StringUtils.hasText(attr)) {
					continue;
				}
				row.put("activity_name", attr.trim());
			}
		} catch (Exception e) {
			log.debug(
					"applyActivityNameOverrides failed companyId={} requestLangTag={} suffix={} langTable={} msg={}",
					companyId,
					requestLangTag != null ? requestLangTag : "",
					suffix != null ? suffix : "",
					langTable != null ? langTable : "",
					e.getMessage(),
					e);
		}
	}

	private static final List<String> DETAIL_OUTSIDE_FIELDS = List.of(
			"activity_name",
			"place",
			"area",
			"address",
			"intro",
			"join_tips",
			"submit_form_tips",
			"content",
			"pics");

	/**
	 * 详情页 outside_item 多语言覆盖；表名后缀仅来自 {@link
	 * SeckillActivityOutsideMultiLangWriteService#normalizeLangTableSuffix(String)}。
	 */
	public void applyDetailOutsideLangOverrides(
			long activityOwnerCompanyId,
			long activityId,
			Map<String, Object> detailRow,
			String requestLangTag) {
		String suffix = null;
		String langTable = null;
		try {
			suffix = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
			langTable = "outside_item_multi_lang_mod_lang_" + suffix;
			for (String field : DETAIL_OUTSIDE_FIELDS) {
				String sql = "SELECT attribute_value FROM `"
						+ langTable
						+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND field = ? AND data_id = ? LIMIT 1";
				try {
					String value = jdbcTemplate.queryForObject(
							sql,
							String.class,
							activityOwnerCompanyId,
							TABLE_MODULE,
							TABLE_MODULE,
							field,
							activityId);
					if (StringUtils.hasText(value)) {
						detailRow.put(field, value);
					}
				} catch (EmptyResultDataAccessException ignored) {
					// no row for this field
				}
			}
		} catch (Exception e) {
			log.debug(
					"applyDetailOutsideLangOverrides failed activityOwnerCompanyId={} activityId={} requestLangTag={} suffix={} langTable={} msg={}",
					activityOwnerCompanyId,
					activityId,
					requestLangTag != null ? requestLangTag : "",
					suffix != null ? suffix : "",
					langTable != null ? langTable : "",
					e.getMessage(),
					e);
		}
	}

	private static Long resolveActivityId(Object idObj) {
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
