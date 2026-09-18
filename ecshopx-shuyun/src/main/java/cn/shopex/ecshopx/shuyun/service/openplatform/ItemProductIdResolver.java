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

package cn.shopex.ecshopx.shuyun.service.openplatform;

/** 数云 product_id：优先 goods_id → default_item_id → item_id。 */
public final class ItemProductIdResolver {

	private ItemProductIdResolver() {}

	public static String resolve(long goodsId, long defaultItemId, long fallbackItemId) {
		if (goodsId > 0) {
			return String.valueOf(goodsId);
		}
		if (defaultItemId > 0) {
			return String.valueOf(defaultItemId);
		}
		if (fallbackItemId > 0) {
			return String.valueOf(fallbackItemId);
		}
		return "";
	}

	public static String resolveFromItemRow(Long goodsId, Long defaultItemId, Long itemId) {
		return resolve(
				goodsId == null ? 0L : goodsId,
				defaultItemId == null ? 0L : defaultItemId,
				itemId == null ? 0L : itemId);
	}
}
