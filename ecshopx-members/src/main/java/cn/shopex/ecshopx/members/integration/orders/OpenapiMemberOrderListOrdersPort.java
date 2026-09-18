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

package cn.shopex.ecshopx.members.integration.orders;

import java.util.Map;

public interface OpenapiMemberOrderListOrdersPort {

	/**
	 * 对齐 PHP AbstractNormalOrder::getOrderItemLists。
	 * orderFilter 须已含 company_id、user_id、order_type=normal、order_status|notin、可选 order_class。
	 */
	Map<String, Object> queryOrderItemLists(Map<String, Object> orderFilter, int page, int pageSize);

	/**
	 * 对齐 PHP NormalOrdersRepository::getTotalAmountByUserId。
	 * 返回 { userId -> sum(total_fee) }；无记录时 emptyMap。
	 */
	Map<Object, Long> sumTotalFeeByUserId(Map<String, Object> orderFilter);
}
