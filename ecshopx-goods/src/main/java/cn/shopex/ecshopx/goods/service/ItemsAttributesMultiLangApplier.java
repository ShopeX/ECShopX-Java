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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import cn.shopex.ecshopx.goods.support.ItemLangShardJdbc;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsAttributesMultiLangApplier {

	private static final String TABLE_ATTR = "items_attributes";
	private static final String TABLE_VALUE = "items_attribute_values";

	private final ItemLangShardJdbc itemLangShardJdbc;

	public ItemsAttributesMultiLangApplier(ItemLangShardJdbc itemLangShardJdbc) {
		this.itemLangShardJdbc = itemLangShardJdbc;
	}

	public void afterAttributeInsert(long companyId, long attributeId, String attributeName, String attributeMemo, String countryCode) {
		upsertField(companyId, TABLE_ATTR, attributeId, "attribute_name", attributeName, countryCode);
		upsertField(companyId, TABLE_ATTR, attributeId, "attribute_memo", attributeMemo, countryCode);
	}

	public void afterAttributeUpdate(long companyId, long attributeId, String attributeName, String attributeMemo, String countryCode) {
		upsertField(companyId, TABLE_ATTR, attributeId, "attribute_name", attributeName, countryCode);
		upsertField(companyId, TABLE_ATTR, attributeId, "attribute_memo", attributeMemo, countryCode);
	}

	public void afterAttributeValueInsert(long companyId, long attributeValueId, String attributeValue, String countryCode) {
		upsertField(companyId, TABLE_VALUE, attributeValueId, "attribute_value", attributeValue, countryCode);
	}

	public void afterAttributeValueUpdate(long companyId, long attributeValueId, String attributeValue, String countryCode) {
		upsertField(companyId, TABLE_VALUE, attributeValueId, "attribute_value", attributeValue, countryCode);
	}

	/**
	 * 列表读路径：批量覆盖 {@code items_attributes} 的 {@code attribute_name} / {@code attribute_memo}（与 {@link ItemsCategoryMultiLangApplier} 语言合并策略一致）。
	 */
	public void applyListLangForAttributes(long companyId, String countryCode, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> ids = new HashSet<>();
		for (Map<String, Object> row : rows) {
			Long id = toLong(row.get("attribute_id"));
			if (id != null && id != 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
		String langAlt = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		Map<String, String> exact = new HashMap<>();
		Map<String, String> other = new HashMap<>();
		collectFieldMaps(companyId, lang, TABLE_ATTR, ids, List.of("attribute_name", "attribute_memo"), exact);
		if (!langAlt.equals(OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang))) {
			collectFieldMapsIfAbsent(companyId, langAlt, TABLE_ATTR, ids, List.of("attribute_name", "attribute_memo"), other);
		}
		for (Map<String, Object> row : rows) {
			Long id = toLong(row.get("attribute_id"));
			if (id == null || id == 0L) {
				continue;
			}
			overlay(row, "attribute_name", exact, other, id);
			overlay(row, "attribute_memo", exact, other, id);
		}
	}

	/**
	 * 列表读路径：批量覆盖 {@code items_attribute_values} 的 {@code attribute_value}。
	 */
	public void applyListLangForAttributeValues(long companyId, String countryCode, List<Map<String, Object>> valueRows) {
		if (valueRows == null || valueRows.isEmpty()) {
			return;
		}
		Set<Long> ids = new HashSet<>();
		for (Map<String, Object> row : valueRows) {
			Long id = toLong(row.get("attribute_value_id"));
			if (id != null && id != 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
		String langAlt = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		Map<Long, String> exact = new HashMap<>();
		Map<Long, String> other = new HashMap<>();
		for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(
				companyId, lang, TABLE_VALUE, TABLE_VALUE, ids, List.of("attribute_value"))) {
			String val = toText(m.get("attribute_value"));
			Long dataId = toLong(m.get("data_id"));
			if (dataId == null || !StringUtils.hasText(val)) {
				continue;
			}
			exact.put(dataId, val);
		}
		if (!langAlt.equals(OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang))) {
			for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(
					companyId, langAlt, TABLE_VALUE, TABLE_VALUE, ids, List.of("attribute_value"))) {
				String val = toText(m.get("attribute_value"));
				Long dataId = toLong(m.get("data_id"));
				if (dataId == null || !StringUtils.hasText(val)) {
					continue;
				}
				other.putIfAbsent(dataId, val);
			}
		}
		for (Map<String, Object> row : valueRows) {
			Long id = toLong(row.get("attribute_value_id"));
			String tr = exact.get(id);
			if (tr == null) {
				tr = other.get(id);
			}
			if (tr != null && !tr.isEmpty()) {
				row.put("attribute_value", tr);
			}
		}
	}

	private void collectFieldMaps(
			long companyId, String localeTag, String tableName, Set<Long> ids, List<String> fields, Map<String, String> target) {
		for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(companyId, localeTag, tableName, tableName, ids, fields)) {
			String val = toText(m.get("attribute_value"));
			Long dataId = toLong(m.get("data_id"));
			String field = toText(m.get("field"));
			if (dataId == null || !StringUtils.hasText(val) || !StringUtils.hasText(field)) {
				continue;
			}
			target.put(dataId + "|" + field, val);
		}
	}

	private void collectFieldMapsIfAbsent(
			long companyId, String localeTag, String tableName, Set<Long> ids, List<String> fields, Map<String, String> target) {
		for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(companyId, localeTag, tableName, tableName, ids, fields)) {
			String val = toText(m.get("attribute_value"));
			Long dataId = toLong(m.get("data_id"));
			String field = toText(m.get("field"));
			if (dataId == null || !StringUtils.hasText(val) || !StringUtils.hasText(field)) {
				continue;
			}
			target.putIfAbsent(dataId + "|" + field, val);
		}
	}

	private static void overlay(
			Map<String, Object> row, String field, Map<String, String> exact, Map<String, String> other, long id) {
		String key = id + "|" + field;
		String tr = exact.get(key);
		if (tr == null) {
			tr = other.get(key);
		}
		if (tr != null && !tr.isEmpty()) {
			row.put(field, tr);
		}
	}

	private static Long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v != null) {
			try {
				return Long.parseLong(v.toString());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static String toText(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private void upsertField(long companyId, String tableName, long dataId, String field, String value, String langCode) {
		String lang = StringUtils.hasText(langCode) ? langCode.trim() : "zh-CN";
		itemLangShardJdbc.upsert(companyId, dataId, lang, tableName, tableName, field, value);
	}
}
