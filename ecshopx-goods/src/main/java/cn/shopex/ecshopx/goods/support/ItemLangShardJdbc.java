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

package cn.shopex.ecshopx.goods.support;

import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ItemLangShardJdbc {
	private static final String PREFIX = "item_multi_lang_mod_lang_";
	private final NamedParameterJdbcTemplate jdbc;

	public ItemLangShardJdbc(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public static String shardTable(String localeTag) {
		return PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(localeTag);
	}

	public List<Map<String, Object>> listFieldRows(
			long companyId,
			String localeTag,
			String tableName,
			String moduleName,
			Collection<Long> dataIds,
			Collection<String> fields) {
		if (dataIds == null || dataIds.isEmpty() || fields == null || fields.isEmpty()) {
			return List.of();
		}
		String table = shardTable(localeTag);
		StringBuilder sql = new StringBuilder(
				"SELECT data_id, `field`, attribute_value FROM `" + table + "` WHERE company_id = :companyId"
						+ " AND table_name = :tableName AND data_id IN (:ids) AND `field` IN (:fields)");
		MapSqlParameterSource p = new MapSqlParameterSource()
				.addValue("companyId", companyId)
				.addValue("tableName", tableName)
				.addValue("ids", dataIds)
				.addValue("fields", fields);
		if (StringUtils.hasText(moduleName)) {
			sql.append(" AND module_name = :moduleName");
			p.addValue("moduleName", moduleName);
		}
		return jdbc.queryForList(sql.toString(), p);
	}

	/** Port-facing read without company filter (legacy {@code ItemMultiLangReadPort} shape). */
	public List<Map<String, Object>> listFieldRows(
			String localeTag,
			String tableName,
			String moduleName,
			Collection<Long> dataIds,
			Collection<String> fields) {
		if (dataIds == null || dataIds.isEmpty() || fields == null || fields.isEmpty()) {
			return List.of();
		}
		String table = shardTable(localeTag);
		StringBuilder sql = new StringBuilder(
				"SELECT data_id, `field`, attribute_value FROM `" + table + "` WHERE table_name = :tableName"
						+ " AND data_id IN (:ids) AND `field` IN (:fields)");
		MapSqlParameterSource p = new MapSqlParameterSource()
				.addValue("tableName", tableName)
				.addValue("ids", dataIds)
				.addValue("fields", fields);
		if (StringUtils.hasText(moduleName)) {
			sql.append(" AND module_name = :moduleName");
			p.addValue("moduleName", moduleName);
		}
		return jdbc.queryForList(sql.toString(), p);
	}

	public void upsert(
			long companyId,
			long dataId,
			String localeTag,
			String tableName,
			String moduleName,
			String field,
			String attributeValue) {
		if (dataId <= 0 || !StringUtils.hasText(field) || !StringUtils.hasText(tableName)) {
			return;
		}
		String table = shardTable(localeTag);
		int now = (int) (System.currentTimeMillis() / 1000L);
		String value = attributeValue == null ? "" : attributeValue;
		MapSqlParameterSource p = new MapSqlParameterSource()
				.addValue("companyId", companyId)
				.addValue("dataId", dataId)
				.addValue("tableName", tableName)
				.addValue("moduleName", moduleName == null ? "" : moduleName)
				.addValue("field", field)
				.addValue("value", value)
				.addValue("now", now);
		int updated = jdbc.update(
				"UPDATE `" + table + "` SET attribute_value = :value, updated = :now"
						+ " WHERE company_id = :companyId AND table_name = :tableName AND module_name = :moduleName"
						+ " AND data_id = :dataId AND `field` = :field",
				p);
		if (updated == 0) {
			jdbc.update(
					"INSERT INTO `" + table + "` (company_id, data_id, table_name, module_name, `field`, lang,"
							+ " attribute_value, created, updated) VALUES (:companyId,:dataId,:tableName,:moduleName,:field,:lang,:value,:now,:now)",
					p.addValue("lang", localeTag == null ? "" : localeTag));
		}
	}

	public void deleteByTableModuleDataIds(String tableName, String moduleName, Collection<Long> dataIds) {
		if (dataIds == null || dataIds.isEmpty() || !StringUtils.hasText(tableName)) {
			return;
		}
		for (String suffix : List.of("zhCN", "enCN", "arSA", "zhtw")) {
			String table = PREFIX + suffix;
			MapSqlParameterSource p = new MapSqlParameterSource()
					.addValue("tableName", tableName)
					.addValue("moduleName", moduleName == null ? "" : moduleName)
					.addValue("ids", dataIds);
			jdbc.update(
					"DELETE FROM `" + table + "` WHERE table_name = :tableName AND module_name = :moduleName AND data_id IN (:ids)",
					p);
		}
	}

	public List<Long> listDataIdsByFieldLike(
			long companyId, String localeTag, String tableName, String moduleName, String field, String keywordLike) {
		if (!StringUtils.hasText(keywordLike) || !StringUtils.hasText(tableName) || !StringUtils.hasText(field)) {
			return List.of();
		}
		String table = shardTable(localeTag);
		StringBuilder sql = new StringBuilder(
				"SELECT DISTINCT data_id FROM `" + table + "` WHERE company_id = :companyId"
						+ " AND table_name = :tableName AND `field` = :field AND attribute_value LIKE :kw");
		MapSqlParameterSource p = new MapSqlParameterSource()
				.addValue("companyId", companyId)
				.addValue("tableName", tableName)
				.addValue("field", field)
				.addValue("kw", keywordLike);
		if (StringUtils.hasText(moduleName)) {
			sql.append(" AND module_name = :moduleName");
			p.addValue("moduleName", moduleName);
		}
		return jdbc.query(sql.toString(), p, (rs, rowNum) -> rs.getLong("data_id"));
	}
}
