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
 * Loads live SKU spec rows for order detail line items. Implemented in {@code ecshopx-goods} to avoid a Maven cycle
 * ({@code ecshopx-goods} already depends on {@code ecshopx-orders}).
 */
public interface AdminOrderDetailItemsSkuSpecPort {

	void applyRealSkuSpec(long companyId, List<Map<String, Object>> orderItems);
}
