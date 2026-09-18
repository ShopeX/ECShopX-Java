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

package cn.shopex.ecshopx.common.orders.port;

import java.util.List;
import java.util.Map;

/**
 * Enriches order detail line items with catalog SKU fields. Implemented in {@code ecshopx-goods} to avoid a Maven
 * cycle ({@code ecshopx-goods} already depends on {@code ecshopx-orders}).
 */
public interface AdminOrderDetailItemsCatalogPort {

	/** Sets {@code sale_price} from live {@code items.price} when a catalog row exists for the line {@code item_id}. */
	void applyCatalogSalePrice(long companyId, List<Map<String, Object>> orderItems);
}
