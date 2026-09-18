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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordOutsideMultiLangReadService {

	private static final Logger log =
			LoggerFactory.getLogger(RegistrationRecordOutsideMultiLangReadService.class);

	private static final String TABLE_AND_MODULE = "selfservice_registration_record";
	private static final List<String> FIELDS = List.of("reason", "remark");

	private final JdbcTemplate jdbcTemplate;

	public RegistrationRecordOutsideMultiLangReadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Applies translated registration-record text from the outside-item language table onto {@code row}
	 * for the configured fields.
	 * <p>
	 * Each value is resolved from the outside-item language table for the request language (and, when
	 * needed, the default {@code zhCN} table) by matching {@code table_name}, {@code field}, and
	 * {@code data_id} (the registration record id).
	 * {@code recordOwnerCompanyId} is reserved for diagnostics and structured logging correlation only;
	 * it is not used in the SQL filter.
	 *
	 * @param recordOwnerCompanyId company context for diagnostics/logging only; excluded from the lookup query
	 * @param recordId registration record id; used as {@code data_id} in the language-table lookup
	 * @param row mutable row map whose configured string fields may be overwritten when a translation exists
	 * @param requestLangTag requested language tag used to select the {@code outside_item_multi_lang_mod_lang_*} table
	 */
	public void applyRecordLangOverrides(
			long recordOwnerCompanyId, long recordId, Map<String, Object> row, String requestLangTag) {
		if (recordId <= 0L) {
			return;
		}
		String primarySuffix = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		List<String> suffixChain = new ArrayList<>(2);
		suffixChain.add(primarySuffix);
		if (!"zhCN".equals(primarySuffix)) {
			suffixChain.add("zhCN");
		}
		for (String field : FIELDS) {
			for (String suffix : suffixChain) {
				String lastNonEmpty = null;
				try {
					String langTable = "outside_item_multi_lang_mod_lang_" + suffix;
					String sql =
							"SELECT attribute_value FROM `"
									+ langTable
									+ "` WHERE table_name = ? AND field = ? AND data_id = ?";
					List<String> values =
							jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), TABLE_AND_MODULE, field, recordId);
					for (String v : values) {
						if (StringUtils.hasText(v)) {
							lastNonEmpty = v;
						}
					}
				} catch (Exception e) {
					log.debug(
							"applyRecordLangOverrides lookup skipped recordOwnerCompanyId={} recordId={} requestLangTag={} suffix={} field={} msg={}",
							recordOwnerCompanyId,
							recordId,
							requestLangTag != null ? requestLangTag : "",
							suffix,
							field,
							e.getMessage());
					continue;
				}
				if (lastNonEmpty != null) {
					row.put(field, lastNonEmpty);
					break;
				}
			}
		}
	}
}
