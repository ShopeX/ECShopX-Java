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
import java.util.Map;

/** 读取实体订单明细行（用于取消/售后计算）。 */
public interface OrderNormalOrderItemsReadPort {

	List<Map<String, Object>> listItems(long companyId, long orderId);

	List<Map<String, Object>> listServiceOrderItems(long companyId, long orderId);
}
