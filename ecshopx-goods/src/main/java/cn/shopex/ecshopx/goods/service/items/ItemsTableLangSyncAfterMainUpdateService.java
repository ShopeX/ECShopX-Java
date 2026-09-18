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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.goods.support.ItemLangShardJdbc;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Sync multilang shard after non-full-item main-table patches (e.g. price/store).
 * Resolves lang via {@link RequestCountryCode} (body → query/Accept-Language), aligning PHP
 * {@code updateLangData}.
 */
@Service
public class ItemsTableLangSyncAfterMainUpdateService {

	private static final String TABLE_ITEMS = "items";

	private final ItemLangShardJdbc itemLangShardJdbc;
	private final LangueProperties langueProperties;

	public ItemsTableLangSyncAfterMainUpdateService(
			ItemLangShardJdbc itemLangShardJdbc, LangueProperties langueProperties) {
		this.itemLangShardJdbc = itemLangShardJdbc;
		this.langueProperties = langueProperties;
	}

	/**
	 * @param requestParams full merged request (may contain {@code country_code} and lang fields)
	 */
	public void afterItemsUpdateIfLangFieldsPresent(long companyId, long itemId, Map<String, Object> requestParams) {
		if (requestParams == null || requestParams.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Object> subset = new LinkedHashMap<>();
		for (String k : List.of("item_name", "brief", "intro")) {
			if (requestParams.containsKey(k) && requestParams.get(k) != null) {
				subset.put(k, requestParams.get(k));
			}
		}
		if (subset.isEmpty()) {
			return;
		}
		String lang = RequestCountryCode.resolve(langueProperties, requestParams);
		for (Map.Entry<String, Object> e : subset.entrySet()) {
			String field = e.getKey();
			String value = e.getValue() == null ? "" : e.getValue().toString();
			itemLangShardJdbc.upsert(companyId, itemId, lang, TABLE_ITEMS, TABLE_ITEMS, field, value);
		}
	}
}
