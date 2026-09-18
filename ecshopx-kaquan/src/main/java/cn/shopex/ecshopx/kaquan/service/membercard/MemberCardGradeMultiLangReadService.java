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

package cn.shopex.ecshopx.kaquan.service.membercard;

import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberCardGradeMultiLangReadService {

	private static final String TABLE = "membercard_grade";
	private static final String TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private final JdbcTemplate jdbcTemplate;

	public MemberCardGradeMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Overlay grade display fields from {@code outside_item_multi_lang_mod_lang_*}; {@code lang} matches the
	 * write-side default language when blank.
	 */
	public void applyOverlays(long companyId, List<Map<String, Object>> gradeRows, String lang) {
		if (gradeRows == null || gradeRows.isEmpty()) {
			return;
		}
		List<Long> dataIds = new ArrayList<>();
		for (Map<String, Object> row : gradeRows) {
			Object gid = row.get("grade_id");
			if (gid instanceof Number n) {
				dataIds.add(n.longValue());
			}
		}
		if (dataIds.isEmpty()) {
			return;
		}
		List<Long> distinctIds = dataIds.stream().distinct().collect(Collectors.toList());
		String effectiveLang = StringUtils.hasText(lang) ? lang.trim() : "zh-CN";
		String shard = TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(effectiveLang);
		String inClause = distinctIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql =
				"SELECT data_id, `field`, attribute_value FROM `"
						+ shard
						+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND data_id IN ("
						+ inClause
						+ ") AND `field` IN ('grade_name','description','background_pic_url','grade_background')";
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		args.add(TABLE);
		args.add(TABLE);
		args.addAll(distinctIds);
		List<Map<String, Object>> rows;
		try {
			rows = jdbcTemplate.queryForList(sql, args.toArray());
		} catch (DataAccessException ignored) {
			return;
		}
		Map<Long, Map<String, String>> byDataId = new HashMap<>();
		for (Map<String, Object> r : rows) {
			Object did = r.get("data_id");
			if (!(did instanceof Number)) {
				continue;
			}
			long dataId = ((Number) did).longValue();
			Object fieldObj = r.get("field");
			if (fieldObj == null) {
				continue;
			}
			String field = String.valueOf(fieldObj);
			Object av = r.get("attribute_value");
			if (av == null) {
				continue;
			}
			byDataId.computeIfAbsent(dataId, k -> new HashMap<>()).put(field, String.valueOf(av));
		}
		for (Map<String, Object> gradeRow : gradeRows) {
			Object gid = gradeRow.get("grade_id");
			if (!(gid instanceof Number)) {
				continue;
			}
			long gradeId = ((Number) gid).longValue();
			Map<String, String> fields = byDataId.get(gradeId);
			if (fields == null) {
				continue;
			}
			for (Map.Entry<String, String> e : fields.entrySet()) {
				gradeRow.put(e.getKey(), e.getValue());
			}
		}
	}
}
