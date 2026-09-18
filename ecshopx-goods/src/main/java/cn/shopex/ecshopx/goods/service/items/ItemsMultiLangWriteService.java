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
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns PHP {@code MultiLangService#addMultiLangByParams} / {@code updateLangData} for module {@code items}:
 * write {@code item_name}/{@code brief}/{@code intro} into {@code item_multi_lang_mod_lang_*}.
 */
@Service
public class ItemsMultiLangWriteService {

	private static final String TABLE_ITEMS = "items";
	private static final List<String> FIELDS = List.of("item_name", "brief", "intro");

	private final ItemLangShardJdbc itemLangShardJdbc;
	private final LangueProperties langueProperties;

	public ItemsMultiLangWriteService(ItemLangShardJdbc itemLangShardJdbc, LangueProperties langueProperties) {
		this.itemLangShardJdbc = itemLangShardJdbc;
		this.langueProperties = langueProperties;
	}

	/**
	 * Prefer {@code country_code} from merged request params (JSON body / form), then
	 * query / Accept-Language via {@link RequestCountryCode}.
	 */
	public String resolveLang(Map<String, Object> params) {
		return RequestCountryCode.resolve(langueProperties, params);
	}

	/** Create: write all multilang fields (empty string when absent), matching PHP addMultiLangByParams. */
	public void afterItemCreate(long companyId, long itemId, Map<String, Object> params, String requestLangTag) {
		if (itemId <= 0) {
			return;
		}
		String lang = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : langueProperties.getDefaultLang();
		for (String field : FIELDS) {
			String value = "";
			if (params != null && params.containsKey(field) && params.get(field) != null) {
				value = String.valueOf(params.get(field));
			}
			itemLangShardJdbc.upsert(companyId, itemId, lang, TABLE_ITEMS, TABLE_ITEMS, field, value);
		}
	}

	/** Update: only fields present in params, matching PHP updateLangData. */
	public void afterItemUpdate(long companyId, long itemId, Map<String, Object> params, String requestLangTag) {
		if (itemId <= 0 || params == null) {
			return;
		}
		String lang = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : langueProperties.getDefaultLang();
		for (String field : FIELDS) {
			if (!params.containsKey(field)) {
				continue;
			}
			String value = params.get(field) != null ? String.valueOf(params.get(field)) : "";
			itemLangShardJdbc.upsert(companyId, itemId, lang, TABLE_ITEMS, TABLE_ITEMS, field, value);
		}
	}
}
