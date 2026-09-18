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

package cn.shopex.ecshopx.companys.service.employee;

import cn.shopex.ecshopx.companys.domain.Operators;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Persists {@code operators} lang-field mirrors into {@code outside_item_multi_lang_mod_lang_*}
 * so account reads that merge those rows stay aligned with the main table.
 */
@Service
public class OperatorAccountOutsideLangWriteService {

	private static final Logger log = LoggerFactory.getLogger(OperatorAccountOutsideLangWriteService.class);

	private static final String TABLE_AND_MODULE = "operators";

	private static final List<String> LANG_FIELDS = List.of("username", "contact", "split_ledger_info");

	private final JdbcTemplate jdbcTemplate;

	public OperatorAccountOutsideLangWriteService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * @param requestLangTag e.g. {@code zh-CN}; when blank, defaults to {@code zh-CN}
	 */
	public void syncOperatorsDisplayedLangFields(
			long companyId, long operatorId, Operators persisted, String requestLangTag) {
		if (persisted == null || operatorId <= 0L) {
			return;
		}
		String langTag = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		String suffix = OperatorAccountOutsideLangReadService.normalizeLangTableSuffix(langTag);
		String table = "outside_item_multi_lang_mod_lang_" + suffix;
		int now = (int) (System.currentTimeMillis() / 1000L);
		try {
			for (String field : LANG_FIELDS) {
				String attrStr = fieldValue(persisted, field);
				Integer exists =
						jdbcTemplate.queryForObject(
								"SELECT COUNT(1) FROM `"
										+ table
										+ "` WHERE company_id=? AND field=? AND table_name=? AND module_name=? AND data_id=? LIMIT 1",
								Integer.class,
								companyId,
								field,
								TABLE_AND_MODULE,
								TABLE_AND_MODULE,
								operatorId);
				int cnt = exists == null ? 0 : exists;
				if (cnt > 0) {
					jdbcTemplate.update(
							"UPDATE `"
									+ table
									+ "` SET attribute_value=?, updated=? WHERE company_id=? AND field=? AND table_name=? AND module_name=? AND data_id=?",
							attrStr,
							now,
							companyId,
							field,
							TABLE_AND_MODULE,
							TABLE_AND_MODULE,
							operatorId);
				} else {
					String sql =
							"INSERT INTO `"
									+ table
									+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
									+ "VALUES (?,?,?,?,?,?,?,?)";
					jdbcTemplate.update(
							sql,
							companyId,
							field,
							attrStr,
							TABLE_AND_MODULE,
							TABLE_AND_MODULE,
							operatorId,
							now,
							now);
				}
			}
		} catch (DataAccessException ex) {
			log.debug("operator outside lang sync skipped companyId={} operatorId={} table={}", companyId, operatorId, table, ex);
		}
	}

	private static String fieldValue(Operators persisted, String field) {
		return switch (field) {
			case "username" -> persisted.getUsername() != null ? persisted.getUsername() : "";
			case "contact" -> persisted.getContact() != null ? persisted.getContact() : "";
			case "split_ledger_info" ->
					persisted.getSplitLedgerInfo() != null ? persisted.getSplitLedgerInfo() : "";
			default -> "";
		};
	}
}
