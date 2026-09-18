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
public class BargainPromotionMultiLangReadService {

	private static final String TABLE_NAME = "promotions_bargain";
	private static final Pattern SAFE_LANG_SUFFIX = Pattern.compile("^[a-zA-Z0-9]+$");

	private final JdbcTemplate jdbcTemplate;

	public BargainPromotionMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void applyTitleAndAdPic(List<Map<String, Object>> rows, String langTag) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Object bid = row.get("bargain_id");
			if (bid instanceof Number n) {
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

		Map<Long, Map<String, String>> idToFields = new HashMap<>();
		List<Long> idList = new ArrayList<>(idSet);
		final int chunkSize = 500;
		for (int i = 0; i < idList.size(); i += chunkSize) {
			int end = Math.min(i + chunkSize, idList.size());
			List<Long> chunk = idList.subList(i, end);
			String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
			String sql =
					"SELECT data_id, field, attribute_value FROM `"
							+ langTable
							+ "` WHERE table_name = ? AND `field` IN ('title','ad_pic') AND data_id IN ("
							+ placeholders
							+ ")";
			List<Object> params = new ArrayList<>(chunk.size() + 1);
			params.add(TABLE_NAME);
			params.addAll(chunk);
			ResultSetExtractor<Void> extractor =
					rs -> {
						while (rs.next()) {
							long dataId = rs.getLong("data_id");
							String field = rs.getString("field");
							String val = rs.getString("attribute_value");
							if (val != null && !val.isEmpty()) {
								idToFields.computeIfAbsent(dataId, k -> new HashMap<>()).put(field, val);
							}
						}
						return null;
					};
			jdbcTemplate.query(sql, extractor, params.toArray());
		}

		for (Map<String, Object> row : rows) {
			Object bid = row.get("bargain_id");
			if (!(bid instanceof Number n)) {
				continue;
			}
			long id = n.longValue();
			Map<String, String> fm = idToFields.get(id);
			if (fm == null) {
				continue;
			}
			for (Map.Entry<String, String> e : fm.entrySet()) {
				String v = e.getValue();
				if (v != null && !v.isEmpty()) {
					row.put(e.getKey(), v);
				}
			}
		}
	}
}
