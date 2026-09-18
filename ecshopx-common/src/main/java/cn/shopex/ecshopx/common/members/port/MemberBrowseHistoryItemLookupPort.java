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

package cn.shopex.ecshopx.common.members.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** 浏览足迹场景下按主键读取商品行（只读）。 */
public interface MemberBrowseHistoryItemLookupPort {

	Optional<ItemRow> findByItemId(long itemId);

	/**
	 * 浏览足迹列表场景下按 {@code item_id} 批量加载商品行：仅使用主键 IN 查询，不按商户（company）过滤。
	 * 返回 Map 的 key 为 {@code item_id}；多语言字段覆盖与商品列表行查询一致。
	 */
	Map<Long, Map<String, Object>> listItemRowsForBrowseHistory(
			Collection<Long> itemIds,
			long companyIdForLang,
			String countryCodeOrLanguageTag);

	record ItemRow(long itemId, Long defaultItemId) {
		public long normalizedItemIdForFilter() {
			return defaultItemId != null ? defaultItemId : itemId;
		}
	}
}
