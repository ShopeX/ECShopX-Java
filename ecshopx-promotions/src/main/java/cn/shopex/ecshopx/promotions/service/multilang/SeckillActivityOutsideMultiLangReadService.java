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

package cn.shopex.ecshopx.promotions.service.multilang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeckillActivityOutsideMultiLangReadService {

	private static final String TABLE_NAME = "promotions_seckill_activity";
	private static final String MODULE_NAME = "promotions_seckill_activity";

	private final JdbcTemplate jdbcTemplate;

	public SeckillActivityOutsideMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void apply(long companyId, long seckillId, Map<String, Object> target, String requestLangTag) {
		String suffix = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String sql =
				"SELECT field, attribute_value FROM `outside_item_multi_lang_mod_lang_"
						+ suffix
						+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND data_id = ? "
						+ "AND field IN ('activity_name','ad_pic','description')";
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(sql, companyId, TABLE_NAME, MODULE_NAME, seckillId);
		for (Map<String, Object> row : rows) {
			Object fieldObj = row.get("field");
			Object valueObj = row.get("attribute_value");
			if (fieldObj == null || valueObj == null) {
				continue;
			}
			String field = fieldObj.toString();
			String attr = valueObj.toString().trim();
			if (StringUtils.hasText(attr)) {
				target.put(field, attr);
			}
		}
	}

	public void applyBatch(long companyId, List<Map<String, Object>> rows, String requestLangTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Long sid = parseSeckillIdFromRow(row);
			if (sid != null) {
				idSet.add(sid);
			}
		}
		if (idSet.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>(idSet);
		String suffix = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String inMarks = String.join(",", Collections.nCopies(ids.size(), "?"));
		String sql =
				"SELECT data_id, field, attribute_value FROM `outside_item_multi_lang_mod_lang_"
						+ suffix
						+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND data_id IN ("
						+ inMarks
						+ ") AND field IN ('activity_name','ad_pic','description')";
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		args.add(TABLE_NAME);
		args.add(MODULE_NAME);
		args.addAll(ids);
		List<Map<String, Object>> dbRows = jdbcTemplate.queryForList(sql, args.toArray());
		Map<Long, Map<String, String>> byDataId = new LinkedHashMap<>();
		for (Map<String, Object> dbRow : dbRows) {
			Object didObj = dbRow.get("data_id");
			if (didObj == null) {
				continue;
			}
			long dataId =
					didObj instanceof Number n ? n.longValue() : Long.parseLong(didObj.toString().trim());
			Object fieldObj = dbRow.get("field");
			Object valueObj = dbRow.get("attribute_value");
			if (fieldObj == null || valueObj == null) {
				continue;
			}
			String field = fieldObj.toString();
			String attr = valueObj.toString().trim();
			if (!StringUtils.hasText(attr)) {
				continue;
			}
			byDataId.computeIfAbsent(dataId, k -> new LinkedHashMap<>()).put(field, attr);
		}
		for (Map<String, Object> row : rows) {
			Long sid = parseSeckillIdFromRow(row);
			if (sid == null) {
				continue;
			}
			Map<String, String> lang = byDataId.get(sid);
			if (lang == null || lang.isEmpty()) {
				continue;
			}
			for (String f : List.of("activity_name", "ad_pic", "description")) {
				String v = lang.get(f);
				if (StringUtils.hasText(v)) {
					row.put(f, v);
				}
			}
		}
	}

	private static Long parseSeckillIdFromRow(Map<String, Object> row) {
		Object v = row.get("seckill_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long l = n.longValue();
			return l > 0L ? l : null;
		}
		try {
			long l = Long.parseLong(v.toString().trim());
			return l > 0L ? l : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
