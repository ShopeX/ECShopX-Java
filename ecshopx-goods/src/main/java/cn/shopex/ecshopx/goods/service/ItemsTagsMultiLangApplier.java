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
public class ItemsTagsMultiLangApplier {

	private static final String TABLE_ITEMS_TAGS = "items_tags";

	private final ItemLangShardJdbc itemLangShardJdbc;

	public ItemsTagsMultiLangApplier(ItemLangShardJdbc itemLangShardJdbc) {
		this.itemLangShardJdbc = itemLangShardJdbc;
	}

	/**
	 * 列表结果行上覆盖 {@code tag_name}、{@code description}（仅当多语言值非空）。
	 */
	public void applyListLangForTags(long companyId, String countryCode, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> ids = new HashSet<>();
		for (Map<String, Object> row : rows) {
			Long id = toLong(row.get("tag_id"));
			if (id != null && id != 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
		String langAlt = OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang);
		for (String field : List.of("tag_name", "description")) {
			Map<Long, String> exact = new HashMap<>();
			Map<Long, String> other = new HashMap<>();
			for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(
					companyId, lang, TABLE_ITEMS_TAGS, TABLE_ITEMS_TAGS, ids, List.of(field))) {
				String val = toText(m.get("attribute_value"));
				Long dataId = toLong(m.get("data_id"));
				if (dataId == null || !StringUtils.hasText(val)) {
					continue;
				}
				exact.put(dataId, val);
			}
			if (!langAlt.equals(OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang))) {
				for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(
						companyId, langAlt, TABLE_ITEMS_TAGS, TABLE_ITEMS_TAGS, ids, List.of(field))) {
					String val = toText(m.get("attribute_value"));
					Long dataId = toLong(m.get("data_id"));
					if (dataId == null || !StringUtils.hasText(val)) {
						continue;
					}
					other.putIfAbsent(dataId, val);
				}
			}
			for (Map<String, Object> row : rows) {
				Long id = toLong(row.get("tag_id"));
				String tr = exact.get(id);
				if (tr == null) {
					tr = other.get(id);
				}
				if (tr != null && !tr.isEmpty()) {
					row.put(field, tr);
				}
			}
		}
	}

	private static Long toLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toText(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	public void afterTagInsert(long companyId, long tagId, String tagName, String description, String countryCode) {
		upsertField(companyId, tagId, "tag_name", tagName, countryCode);
		upsertField(companyId, tagId, "description", description, countryCode);
	}

	/**
	 * 仅当调用方已判定 input 含 tag_name 或 description 键且主表更新成功后调用；内部再按键分别 upsert。
	 */
	public void afterTagUpdate(long companyId, long tagId, Map<String, Object> input, String countryCode) {
		if (input.containsKey("tag_name")) {
			Object tn = input.get("tag_name");
			String tagName = tn == null ? "" : tn.toString().trim();
			upsertField(companyId, tagId, "tag_name", tagName, countryCode);
		}
		if (input.containsKey("description")) {
			Object d = input.get("description");
			String description = d == null ? null : d.toString();
			upsertField(companyId, tagId, "description", description, countryCode);
		}
	}

	private void upsertField(long companyId, long dataId, String field, String value, String langCode) {
		String lang = StringUtils.hasText(langCode) ? langCode.trim() : "zh-CN";
		itemLangShardJdbc.upsert(companyId, dataId, lang, TABLE_ITEMS_TAGS, TABLE_ITEMS_TAGS, field, value);
	}
}
