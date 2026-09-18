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

package cn.shopex.ecshopx.common.port.order;

import java.util.List;

/**
 * 普通实体订单：自动关子单售后（联表筛选 + 更新子单）端口；实现位于 {@code ecshopx-orders}，避免 aftersales 直接依赖 orders 形成环。
 */
public interface NormalOrderAutoCloseAftersalesCronPort {

	long countPendingItems(int nowEpochSec);

	List<PendingAutoCloseOrderItemRow> listPendingItems(int nowEpochSec, int page, int pageSize);

	/** 将子单 {@code aftersales_status} 置为 CLOSED，返回影响行数（0/1）。 */
	int updateItemAftersalesClosed(long itemId);
}
