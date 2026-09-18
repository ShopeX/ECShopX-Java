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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class PromotionActivityMultiLangWriteService {

	private static final String TABLE_AND_MODULE = "promotions_point_upvaluation";
	private static final List<String> FIELDS = List.of("title");
	private static final Pattern SAFE_LANG_SUFFIX = Pattern.compile("^[a-zA-Z0-9]+$");

	private final JdbcTemplate jdbcTemplate;

	public PromotionActivityMultiLangWriteService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void addTitleForNewActivity(
			long activityId, long companyId, Map<String, Object> requestData, String requestLangTag) {
		if (activityId <= 0) {
			return;
		}
		String lang = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		String suffix = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		if (!SAFE_LANG_SUFFIX.matcher(suffix).matches()) {
			suffix = "zhCN";
		}
		String table = "item_multi_lang_mod_lang_" + suffix;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : FIELDS) {
			String value = "";
			if (requestData != null && requestData.containsKey(field) && requestData.get(field) != null) {
				value = String.valueOf(requestData.get(field));
			}
			int n = jdbcTemplate.update(
					"UPDATE `"
							+ table
							+ "` SET attribute_value = ?, updated = ? WHERE table_name = ? AND `field` = ? AND data_id = ?",
					value,
					now,
					TABLE_AND_MODULE,
					field,
					activityId);
			if (n == 0) {
				jdbcTemplate.update(
						"INSERT INTO `"
								+ table
								+ "` (`field`, attribute_value, table_name, module_name, data_id, company_id, created, updated) VALUES (?,?,?,?,?,?,?,?)",
						field,
						value,
						TABLE_AND_MODULE,
						TABLE_AND_MODULE,
						activityId,
						companyId,
						now,
						now);
			}
		}
	}
}
