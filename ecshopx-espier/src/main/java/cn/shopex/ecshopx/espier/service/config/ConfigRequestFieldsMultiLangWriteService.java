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

package cn.shopex.ecshopx.espier.service.config;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * 向 {@code item_multi_lang_mod_lang_*} 分表写入配置请求字段的多语言属性（与读侧字段集合一致）。
 */
@Service
public class ConfigRequestFieldsMultiLangWriteService {

	private static final String TABLE_NAME = "config_request_fields";
	private static final List<String> LANG_FIELDS =
			List.of("label", "validate_condition", "alert_required_message");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ConfigRequestFieldsMultiLangWriteService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * 在主表插入成功后写入默认语言（zhCN）分表。
	 *
	 * @param dataId 主表主键
	 * @param createDataSnapshot 须含 {@code company_id} 及多语言字段当前值
	 */
	public void syncAfterInsert(int dataId, Map<String, Object> createDataSnapshot) {
		if (dataId <= 0 || createDataSnapshot == null) {
			return;
		}
		String suffix = normalizeLangSuffix("zh-CN");
		String shard = "item_multi_lang_mod_lang_" + suffix;
		int companyId = intFromSnapshot(createDataSnapshot.get("company_id"));
		if (companyId <= 0) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		String sql = "INSERT INTO `"
				+ shard
				+ "` (`field`, attribute_value, table_name, module_name, data_id, company_id, created, updated)"
				+ " VALUES (:field, :attributeValue, :tableName, :moduleName, :dataId, :companyId, :created, :updated)";
		for (String field : LANG_FIELDS) {
			String value = "";
			if (createDataSnapshot.containsKey(field) && createDataSnapshot.get(field) != null) {
				value = String.valueOf(createDataSnapshot.get(field));
			}
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("field", field);
			p.addValue("attributeValue", value);
			p.addValue("tableName", TABLE_NAME);
			p.addValue("moduleName", TABLE_NAME);
			p.addValue("dataId", dataId);
			p.addValue("companyId", companyId);
			p.addValue("created", now);
			p.addValue("updated", now);
			namedParameterJdbcTemplate.update(sql, p);
		}
	}

	public void syncAfterUpdate(int dataId, int companyId, Map<String, Object> langSnapshot, String requestLangTag) {
		if (dataId <= 0 || companyId <= 0 || langSnapshot == null) {
			return;
		}
		String suffix = normalizeLangSuffix(requestLangTag);
		String shard = "item_multi_lang_mod_lang_" + suffix;
		int now = (int) (System.currentTimeMillis() / 1000L);

		String updateSql = "UPDATE `"
				+ shard
				+ "` SET `attribute_value` = :attributeValue, `updated` = :updated WHERE `table_name` = :tableName"
				+ " AND `module_name` = :moduleName AND `data_id` = :dataId AND `field` = :field AND `company_id` = :companyId";

		String insertSql = "INSERT INTO `"
				+ shard
				+ "` (`field`, attribute_value, table_name, module_name, data_id, company_id, created, updated)"
				+ " VALUES (:field, :attributeValue, :tableName, :moduleName, :dataId, :companyId, :created, :updated)";

		for (String field : LANG_FIELDS) {
			Object rawVal = langSnapshot.get(field);
			String attributeValue = rawVal == null ? "" : String.valueOf(rawVal);

			MapSqlParameterSource up = new MapSqlParameterSource();
			up.addValue("attributeValue", attributeValue);
			up.addValue("updated", now);
			up.addValue("tableName", TABLE_NAME);
			up.addValue("moduleName", TABLE_NAME);
			up.addValue("dataId", dataId);
			up.addValue("field", field);
			up.addValue("companyId", companyId);

			int affected = namedParameterJdbcTemplate.update(updateSql, up);
			if (affected == 0) {
				MapSqlParameterSource ip = new MapSqlParameterSource();
				ip.addValue("field", field);
				ip.addValue("attributeValue", attributeValue);
				ip.addValue("tableName", TABLE_NAME);
				ip.addValue("moduleName", TABLE_NAME);
				ip.addValue("dataId", dataId);
				ip.addValue("companyId", companyId);
				ip.addValue("created", now);
				ip.addValue("updated", now);
				namedParameterJdbcTemplate.update(insertSql, ip);
			}
		}
	}

	private static int intFromSnapshot(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String normalizeLangSuffix(String lang) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
	}
}
