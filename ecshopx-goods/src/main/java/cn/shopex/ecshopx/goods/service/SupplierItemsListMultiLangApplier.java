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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierItemsListMultiLangApplier {

	private static final String TABLE_SUPPLIER_ITEMS = "supplier_items";

	private final ItemLangShardJdbc itemLangShardJdbc;

	public SupplierItemsListMultiLangApplier(ItemLangShardJdbc itemLangShardJdbc) {
		this.itemLangShardJdbc = itemLangShardJdbc;
	}

	public void applyToRows(long companyId, String countryCode, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
		String langAlt = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);

		Set<Long> ids = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			Object i = r.get("item_id");
			if (i instanceof Number n) {
				ids.add(n.longValue());
			}
		}
		if (ids.isEmpty()) {
			return;
		}

		Map<String, Map<Long, String>> fieldMaps = new HashMap<>();
		for (String field : List.of("item_name", "brief", "intro")) {
			fieldMaps.put(field, loadFieldMap(companyId, lang, langAlt, field, ids));
		}

		for (Map<String, Object> r : rows) {
			Object i = r.get("item_id");
			if (!(i instanceof Number n)) {
				continue;
			}
			long itemId = n.longValue();
			overlay(r, "item_name", fieldMaps.get("item_name").get(itemId));
			overlay(r, "brief", fieldMaps.get("brief").get(itemId));
			overlay(r, "intro", fieldMaps.get("intro").get(itemId));
		}
	}

	private Map<Long, String> loadFieldMap(long companyId, String lang, String langAlt, String field, Set<Long> ids) {
		Map<Long, String> exact = new HashMap<>();
		Map<Long, String> other = new HashMap<>();
		for (Map<String, Object> row : itemLangShardJdbc.listFieldRows(
				companyId, lang, TABLE_SUPPLIER_ITEMS, TABLE_SUPPLIER_ITEMS, ids, List.of(field))) {
			Long dataId = toLong(row.get("data_id"));
			String val = toText(row.get("attribute_value"));
			if (dataId != null && StringUtils.hasText(val)) {
				exact.put(dataId, val);
			}
		}
		if (!langAlt.equals(OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang))) {
			for (Map<String, Object> row : itemLangShardJdbc.listFieldRows(
					companyId, langAlt, TABLE_SUPPLIER_ITEMS, TABLE_SUPPLIER_ITEMS, ids, List.of(field))) {
				Long dataId = toLong(row.get("data_id"));
				String val = toText(row.get("attribute_value"));
				if (dataId != null && StringUtils.hasText(val)) {
					other.putIfAbsent(dataId, val);
				}
			}
		}
		Map<Long, String> out = new HashMap<>(other);
		out.putAll(exact);
		return out;
	}

	private static Long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return null;
	}

	private static String toText(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private static void overlay(Map<String, Object> row, String key, String value) {
		if (value != null && StringUtils.hasText(value)) {
			row.put(key, value);
		}
	}
}
