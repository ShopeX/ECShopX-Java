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

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * 写入 shop_menu 名称到 {@code outside_item_multi_lang_mod_lang_*}（对齐 PHP {@code MultiLangItem('other')} /
 * {@code CommonLangModService::updateLangData/saveLang}）。
 */
@Service
public class ShopMenuOutsideLangWriteService {

	private static final String TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";
	private static final String TABLE_SHOP_MENU = "shop_menu";
	private static final String MODULE_SHOP_MENU = "shop_menu";
	private static final String FIELD_NAME = "name";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ShopMenuOutsideLangWriteService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Upsert 当前请求语种下的菜单名称。
	 *
	 * @param companyId  border 菜单为 {@code 0}
	 * @param dataId     shopmenu_id
	 * @param name       名称；{@code null} 按空串写入
	 * @param requestLang 如 {@code zh-CN}
	 */
	public void upsertName(int companyId, long dataId, String name, String requestLang) {
		if (dataId <= 0L) {
			return;
		}
		String langTag = StringUtils.hasText(requestLang) ? requestLang.trim() : "zh-CN";
		String suffix = normalizeLangTableSuffix(langTag);
		String shard = TABLE_PREFIX + suffix;
		String attributeValue = name != null ? name : "";
		int now = (int) (System.currentTimeMillis() / 1000L);

		String updateSql =
				"UPDATE `"
						+ shard
						+ "` SET `attribute_value` = :attributeValue, `updated` = :updated, `lang` = :lang"
						+ " WHERE `company_id` = :companyId AND `table_name` = :tableName"
						+ " AND `module_name` = :moduleName AND `data_id` = :dataId AND `field` = :field";

		MapSqlParameterSource up = new MapSqlParameterSource();
		up.addValue("attributeValue", attributeValue);
		up.addValue("updated", now);
		up.addValue("lang", langTag);
		up.addValue("companyId", companyId);
		up.addValue("tableName", TABLE_SHOP_MENU);
		up.addValue("moduleName", MODULE_SHOP_MENU);
		up.addValue("dataId", dataId);
		up.addValue("field", FIELD_NAME);

		int affected = namedParameterJdbcTemplate.update(updateSql, up);
		if (affected > 0) {
			return;
		}

		String insertSql =
				"INSERT INTO `"
						+ shard
						+ "` (`field`, attribute_value, table_name, module_name, data_id, company_id, lang, created, updated)"
						+ " VALUES (:field, :attributeValue, :tableName, :moduleName, :dataId, :companyId, :lang, :created, :updated)";
		MapSqlParameterSource ip = new MapSqlParameterSource();
		ip.addValue("field", FIELD_NAME);
		ip.addValue("attributeValue", attributeValue);
		ip.addValue("tableName", TABLE_SHOP_MENU);
		ip.addValue("moduleName", MODULE_SHOP_MENU);
		ip.addValue("dataId", dataId);
		ip.addValue("companyId", companyId);
		ip.addValue("lang", langTag);
		ip.addValue("created", now);
		ip.addValue("updated", now);
		namedParameterJdbcTemplate.update(insertSql, ip);
	}

	static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}

