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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Persists {@code members_tags} multi-language fields into local
 * {@code outside_item_multi_lang_mod_lang_*}.
 */
@Service
public class MemberTagsMultiLangWriteService {

	private static final String TABLE_MODULE = "members_tags";
	private static final String FIELD = "tag_name";
	private static final String TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";
	private static final List<String> LANG_SUFFIXES = List.of("zhCN", "enCN", "arSA");

	private final JdbcTemplate jdbcTemplate;

	public MemberTagsMultiLangWriteService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void afterTagUpdate(long tagId, long companyId, Map<String, Object> params, String requestLangTag) {
		if (tagId <= 0 || params == null || !params.containsKey(FIELD)) {
			return;
		}
		Object raw = params.get(FIELD);
		String value = raw == null ? "" : String.valueOf(raw).trim();
		String lang = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		upsertField(tagId, companyId, lang, FIELD, value);
	}

	public void afterTagCreate(long tagId, long companyId, Map<String, Object> params, String requestLangTag) {
		if (tagId <= 0) {
			return;
		}
		String lang = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		String value = "";
		if (params != null && params.containsKey(FIELD) && params.get(FIELD) != null) {
			value = String.valueOf(params.get(FIELD)).trim();
		}
		String table = TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		int now = (int) (System.currentTimeMillis() / 1000L);
		try {
			jdbcTemplate.update(
					"INSERT INTO `"
							+ table
							+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
							+ "VALUES (?,?,?,?,?,?,?,?)",
					companyId,
					FIELD,
					value,
					TABLE_MODULE,
					TABLE_MODULE,
					tagId,
					now,
					now);
		} catch (DataAccessException ignored) {
			upsertField(tagId, companyId, lang, FIELD, value);
		}
	}

	public void deleteMultiLangRowsForMembersTags(long dataId) {
		if (dataId <= 0L) {
			return;
		}
		for (String suffix : LANG_SUFFIXES) {
			String table = TABLE_PREFIX + suffix;
			try {
				jdbcTemplate.update(
						"DELETE FROM `"
								+ table
								+ "` WHERE table_name = ? AND module_name = ? AND data_id = ?",
						TABLE_MODULE,
						TABLE_MODULE,
						dataId);
			} catch (DataAccessException ignored) {
			}
		}
	}

	private void upsertField(long tagId, long companyId, String langTag, String field, String value) {
		String table = TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
		int now = (int) (System.currentTimeMillis() / 1000L);
		int updated =
				jdbcTemplate.update(
						"UPDATE `"
								+ table
								+ "` SET attribute_value = ?, updated = ?, company_id = ? "
								+ "WHERE table_name = ? AND module_name = ? AND data_id = ? AND field = ?",
						value,
						now,
						companyId,
						TABLE_MODULE,
						TABLE_MODULE,
						tagId,
						field);
		if (updated == 0) {
			jdbcTemplate.update(
					"INSERT INTO `"
							+ table
							+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
							+ "VALUES (?,?,?,?,?,?,?,?)",
					companyId,
					field,
					value,
					TABLE_MODULE,
					TABLE_MODULE,
					tagId,
					now,
					now);
		}
	}
}
