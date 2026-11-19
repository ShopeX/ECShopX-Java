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

package cn.shopex.ecshopx.goods.search.query;

import java.util.List;

/**
 * 前台商品列表原先在数据库回表时追加的硬过滤，改由 ES filter 承担。
 * {@code null} 表示该条件未启用。
 */
public record GoodsSearchListFilter(
		String itemType,
		Boolean gift,
		Boolean defaultItem,
		Long brandId,
		Integer itemCrossType,
		boolean excludeCrossBorder,
		String regionsId,
		Integer rebate,
		List<String> rebateTypes,
		List<Long> goodsIds,
		Boolean storeListable,
		boolean matchItemDistributorId,
		String storeStatus) {

	public GoodsSearchListFilter {
		rebateTypes = rebateTypes == null ? List.of() : List.copyOf(rebateTypes);
		goodsIds = goodsIds == null ? List.of() : List.copyOf(goodsIds);
	}

	public static GoodsSearchListFilter none() {
		return new GoodsSearchListFilter(
				null, null, null, null, null, false, null, null, List.of(), List.of(), null, false, null);
	}
}
