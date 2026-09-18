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

package cn.shopex.ecshopx.wsugc.service.comment;

import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class CommentMultiLangDeleteService {

	private static final String TABLE_NAME = "wsugc_comment";
	private static final String MODULE_NAME = "wsugc_comment";

	/** Whitelisted per-locale outside tables only (suffix from {@link #normalizeLangTableSuffix}). */
	private static final List<String> WHITELIST_OUTSIDE_TABLES = buildWhitelistedOutsideTables();

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public CommentMultiLangDeleteService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	private static List<String> buildWhitelistedOutsideTables() {
		List<String> tables = new ArrayList<>();
		for (String lang : List.of("zh-CN", "en-CN", "ar-SA")) {
			tables.add("outside_item_multi_lang_mod_lang_" + normalizeLangTableSuffix(lang));
		}
		return List.copyOf(tables);
	}

	/** Same suffix rules as {@link CommentOutsideLangReadService} (mirrors its private normalizer). */
	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteMultiLangForComments(List<Long> commentIds) {
		if (commentIds == null || commentIds.isEmpty()) {
			return;
		}
		MapSqlParameterSource base = new MapSqlParameterSource();
		base.addValue("table_name", TABLE_NAME);
		base.addValue("module_name", MODULE_NAME);
		base.addValue("ids", commentIds);

		for (String outsideTable : WHITELIST_OUTSIDE_TABLES) {
			String sql =
					"DELETE FROM "
							+ outsideTable
							+ " WHERE table_name = :table_name AND module_name = :module_name AND data_id IN (:ids)";
			try {
				namedParameterJdbcTemplate.update(sql, base);
			} catch (DataAccessException ignored) {
				// Optional locale tables may be absent in some environments (align read-side tolerance).
			}
		}
	}
}
