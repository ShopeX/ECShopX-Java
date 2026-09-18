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

package cn.shopex.ecshopx.shopmenuborder.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class ShopMenuOutsideLangReadService {

	private static final String TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";
	private static final String TABLE_SHOP_MENU = "shop_menu";
	private static final String FIELD_NAME = "name";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ShopMenuOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Batch-load translated {@code name} for shop menu rows from the per-locale outside_item multi-lang mod table.
	 *
	 * @return map {@code data_id -> attribute_value} for non-empty values only; empty map if the table is missing or the
	 *         query fails
	 */
	public Map<Long, String> findShopMenuNamesByLocale(long companyId, Collection<Long> dataIds, String localeTag) {
		if (dataIds == null || dataIds.isEmpty()) {
			return Map.of();
		}
		String suffix = normalizeLangTableSuffix(localeTag);
		String table = TABLE_PREFIX + suffix;
		String sql =
				"SELECT data_id, attribute_value FROM "
						+ table
						+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
						+ "AND data_id IN (:ids)";
		List<Long> ids = new ArrayList<>(dataIds);
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_SHOP_MENU);
		p.addValue("field", FIELD_NAME);
		p.addValue("ids", ids);
		Map<Long, String> out = new LinkedHashMap<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(av)) {
							return;
						}
						out.putIfAbsent(dataId, av);
					});
		} catch (DataAccessException ignored) {
			return Map.of();
		}
		return out;
	}

	/** Hyphens removed from locale tag; if the result is not alphanumeric, falls back to {@code zhCN}. */
	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
