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

/** 店品键分页游标：与 {@code ORDER BY default_item_id, distributor_id} 一致。 */
public final class SearchIndexStoreItemKeyOrder {

	private SearchIndexStoreItemKeyOrder() {}

	/**
	 * 游标 {@code (afterDefaultItemId, afterDistributorId)} 之后的第一页条件。
	 *
	 * @param afterDefaultItemId 首页传 0
	 * @param afterDistributorId 与上一页最后一条的 {@code distributor_id} 成对使用，首页传 0
	 */
	public static boolean isAfter(
			long defaultItemId, long distributorId, long afterDefaultItemId, long afterDistributorId) {
		return defaultItemId > afterDefaultItemId
				|| (defaultItemId == afterDefaultItemId && distributorId > afterDistributorId);
	}
}
