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

package cn.shopex.ecshopx.wechat.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Component
public class WeappSettingOutsideLangParamsLoader {

	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String TABLE_WECHAT_WEAPP_SETTING = "wechat_weapp_setting";

	private static final String FIELD_PARAMS = "params";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public WeappSettingOutsideLangParamsLoader(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public Map<Long, String> findParamsByLocale(long companyId, Collection<Long> weappSettingIds, String localeTag) {
		if (weappSettingIds == null || weappSettingIds.isEmpty()) {
			return Map.of();
		}
		String lang = localeTag != null ? localeTag.trim() : "";
		if (!StringUtils.hasText(lang)) {
			return Map.of();
		}
		List<Long> ids = new ArrayList<>(weappSettingIds);
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(lang);
		String sql = "SELECT data_id, attribute_value FROM "
				+ table
				+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
				+ "AND data_id IN (:ids)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_WECHAT_WEAPP_SETTING);
		p.addValue("field", FIELD_PARAMS);
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

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
