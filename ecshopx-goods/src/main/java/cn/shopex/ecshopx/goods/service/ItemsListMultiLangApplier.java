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
public class ItemsListMultiLangApplier {

	private static final String TABLE_ITEMS = "items";

	private final ItemLangShardJdbc itemLangShardJdbc;

	public ItemsListMultiLangApplier(ItemLangShardJdbc itemLangShardJdbc) {
		this.itemLangShardJdbc = itemLangShardJdbc;
	}

	/**
	 * 商品列表多语言覆盖（Accept-Language / countryCode）：精确语言优先，其次无连字符变体。
	 */
	public void applyToRows(long companyId, String countryCode, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
		String langAlt = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);

		Set<Long> ids = new LinkedHashSet<>();
		for (Map<String, Object> r : rows) {
			Long id = toLong(r.get("item_id"));
			if (id == null) {
				id = toLong(r.get("itemId"));
			}
			if (id != null && id > 0L) {
				ids.add(id);
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
			Long itemId = toLong(r.get("item_id"));
			if (itemId == null) {
				itemId = toLong(r.get("itemId"));
			}
			if (itemId == null) {
				continue;
			}
			overlay(r, "item_name", fieldMaps.get("item_name").get(itemId));
			overlay(r, "brief", fieldMaps.get("brief").get(itemId));
			overlay(r, "intro", fieldMaps.get("intro").get(itemId));
		}
	}

	/** 疫情登记等列表：与 {@link #applyToRows} 语义一致，避免两套实现漂移。 */
	public void applyListLangForItems(long companyId, String countryCode, List<Map<String, Object>> rows) {
		applyToRows(companyId, countryCode, rows);
	}

	private Map<Long, String> loadFieldMap(long companyId, String lang, String langAlt, String field, Set<Long> ids) {
		Map<Long, String> exact = new HashMap<>();
		Map<Long, String> other = new HashMap<>();
		for (Map<String, Object> row : itemLangShardJdbc.listFieldRows(
				companyId, lang, TABLE_ITEMS, TABLE_ITEMS, ids, List.of(field))) {
			putNonEmpty(exact, row);
		}
		if (!langAlt.equals(OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang))) {
			for (Map<String, Object> row : itemLangShardJdbc.listFieldRows(
					companyId, langAlt, TABLE_ITEMS, TABLE_ITEMS, ids, List.of(field))) {
				putNonEmptyIfAbsent(other, row);
			}
		}
		Map<Long, String> out = new HashMap<>(other);
		out.putAll(exact);
		return out;
	}

	private static void putNonEmpty(Map<Long, String> target, Map<String, Object> row) {
		Long dataId = toLong(row.get("data_id"));
		String val = toText(row.get("attribute_value"));
		if (dataId != null && StringUtils.hasText(val)) {
			target.put(dataId, val);
		}
	}

	private static void putNonEmptyIfAbsent(Map<Long, String> target, Map<String, Object> row) {
		Long dataId = toLong(row.get("data_id"));
		String val = toText(row.get("attribute_value"));
		if (dataId != null && StringUtils.hasText(val)) {
			target.putIfAbsent(dataId, val);
		}
	}

	private static Long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toText(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private static void overlay(Map<String, Object> row, String key, String value) {
		if (value != null && StringUtils.hasText(value)) {
			row.put(key, value);
			// Keep camelCase mirrors in sync (RowMapper sets them before overlay).
			if ("item_name".equals(key)) {
				row.put("itemName", value);
			}
		}
	}
}
