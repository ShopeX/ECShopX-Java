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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class PromotionActivityMultiLangReadService {

	private static final String TABLE_AND_MODULE = "promotions_point_upvaluation";
	private static final Pattern SAFE_LANG_SUFFIX = Pattern.compile("^[a-zA-Z0-9]+$");

	private final JdbcTemplate jdbcTemplate;

	public PromotionActivityMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void applyTitles(List<Map<String, Object>> rows, String langTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Object aid = row.get("activity_id");
			if (aid instanceof Number n) {
				long id = n.longValue();
				if (id > 0L) {
					idSet.add(id);
				}
			}
		}
		if (idSet.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(langTag) ? langTag.trim() : "zh-CN";
		String suffix = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		if (!SAFE_LANG_SUFFIX.matcher(suffix).matches()) {
			suffix = "zhCN";
		}
		String langTable = "outside_item_multi_lang_mod_lang_" + suffix;

		List<Long> idList = new ArrayList<>(idSet);
		Map<Long, String> idToTitle = new HashMap<>();
		final int chunkSize = 500;
		for (int i = 0; i < idList.size(); i += chunkSize) {
			int end = Math.min(i + chunkSize, idList.size());
			List<Long> chunk = idList.subList(i, end);
			String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
			String sql =
					"SELECT data_id, attribute_value FROM `"
							+ langTable
							+ "` WHERE table_name = ? AND `field` = 'title' AND data_id IN ("
							+ placeholders
							+ ")";
			List<Object> params = new ArrayList<>(chunk.size() + 1);
			params.add(TABLE_AND_MODULE);
			params.addAll(chunk);
			ResultSetExtractor<Void> extractor =
					rs -> {
						while (rs.next()) {
							long dataId = rs.getLong("data_id");
							String val = rs.getString("attribute_value");
							if (val != null && !val.isEmpty()) {
								idToTitle.put(dataId, val);
							}
						}
						return null;
					};
			jdbcTemplate.query(sql, extractor, params.toArray());
		}

		for (Map<String, Object> row : rows) {
			Object aid = row.get("activity_id");
			if (!(aid instanceof Number n)) {
				continue;
			}
			long id = n.longValue();
			String t = idToTitle.get(id);
			if (t != null && !t.isEmpty()) {
				row.put("title", t);
			}
		}
	}
}
