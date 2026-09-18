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
public class ItemsCategoryMultiLangApplier {

	private static final String TABLE = "items_category";

	private final ItemLangShardJdbc itemLangShardJdbc;

	public ItemsCategoryMultiLangApplier(ItemLangShardJdbc itemLangShardJdbc) {
		this.itemLangShardJdbc = itemLangShardJdbc;
	}

	/**
	 * 在组树之前对平面行按语言覆盖 {@code category_name}。
	 */
	public void apply(long companyId, String countryCode, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> ids = new HashSet<>();
		for (Map<String, Object> row : rows) {
			Long id = ItemsCategoryTreeService.toLongKey(row.get("category_id"));
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
				companyId, lang, TABLE, TABLE, ids, List.of("category_name"))) {
			String val = toText(m.get("attribute_value"));
			Long dataId = toLong(m.get("data_id"));
			if (dataId == null || !StringUtils.hasText(val)) {
				continue;
			}
			exact.put(dataId, val);
		}
		if (!langAlt.equals(OutsideMultiLangTableSupport.normalizeLangTableSuffix(lang))) {
			for (Map<String, Object> m : itemLangShardJdbc.listFieldRows(
					companyId, langAlt, TABLE, TABLE, ids, List.of("category_name"))) {
				String val = toText(m.get("attribute_value"));
				Long dataId = toLong(m.get("data_id"));
				if (dataId == null || !StringUtils.hasText(val)) {
					continue;
				}
				other.putIfAbsent(dataId, val);
			}
		}
		for (Map<String, Object> row : rows) {
			Long id = ItemsCategoryTreeService.toLongKey(row.get("category_id"));
			String tr = exact.get(id);
			if (tr == null) {
				tr = other.get(id);
			}
			if (tr != null && !tr.isEmpty()) {
				row.put("category_name", tr);
			}
		}
	}

	/**
	 * 写入或更新当前语言下的 {@code category_name} 多语言行。
	 */
	public void upsertCategoryNameLang(long companyId, long dataId, String categoryName, String langCode) {
		if (categoryName == null) {
			return;
		}
		String lang = StringUtils.hasText(langCode) ? langCode.trim() : "zh-CN";
		itemLangShardJdbc.upsert(companyId, dataId, lang, TABLE, TABLE, "category_name", categoryName);
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
}
