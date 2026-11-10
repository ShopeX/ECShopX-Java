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

package cn.shopex.ecshopx.goods.search.index;

import java.util.List;
import java.util.Optional;

/**
 * 按公司（可选店铺）分页加载 {@link SearchIndexSource}，供全量重建与增量 upsert 复用。
 */
public interface SearchIndexSourceLoader {

	/**
	 * @param afterDefaultItemId 上一页最后一条的 {@code default_item_id}，首页传 0
	 * @param limit 本页最大条数
	 * @return 空列表表示扫描结束
	 */
	List<SearchIndexSource> fetchPage(long companyId, Long distributorIdOrNull, long afterDefaultItemId, int limit);

	/** 加载单条店品；不存在则 empty（增量 upsert 用）。 */
	default Optional<SearchIndexSource> fetchOne(long companyId, long distributorId, long defaultItemId) {
		List<SearchIndexSource> page = fetchPage(companyId, distributorId, defaultItemId - 1, 1);
		if (page.isEmpty() || page.get(0).defaultItemId() != defaultItemId) {
			return Optional.empty();
		}
		return Optional.of(page.get(0));
	}
}
