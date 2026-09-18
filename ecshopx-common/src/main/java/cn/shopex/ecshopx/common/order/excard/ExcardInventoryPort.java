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

package cn.shopex.ecshopx.common.order.excard;

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;

/**
 * Inventory and line-item snapshot for exchange-card orders; implemented in the goods module to keep module boundaries clean.
 */
public interface ExcardInventoryPort {

	/**
	 * Atomically adjusts stock by {@code num} (negative increases stock, e.g. unlock; positive decreases).
	 *
	 * @return false if stock would go negative (change rolled back), true on success including DB store column sync
	 */
	boolean minusItemStore(long companyId, long itemId, int num, long distributorId, boolean isTotalStore);

	boolean minusItemStore(ItemInventoryLineContext ctx, int num);

	boolean resolveIsTotalStore(long companyId, long itemId, long distributorId);

	ExcardOrderItemSnapshot loadOrderItemSnapshot(long companyId, long itemId, long distributorId);
}
