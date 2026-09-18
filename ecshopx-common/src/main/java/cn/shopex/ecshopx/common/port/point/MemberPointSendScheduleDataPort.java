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

package cn.shopex.ecshopx.common.port.point;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会员订单送积分批处理在订单/售后域的数据访问（实现位于 ecshopx-orders，避免 point↔orders 循环依赖）。
 */
public interface MemberPointSendScheduleDataPort {

	long countSendPointPendingOrders();

	List<MemberPointScheduleOrderRow> listSendPointPendingOrders(int offset, int pageSize);

	/**
	 * 存在未完成售后（状态非 3/4）的订单号。
	 */
	Set<Long> orderIdsWithOpenAftersales(List<Long> orderIds);

	List<MemberPointScheduleItemRow> listLineItemsForOrder(long orderId);

	/** 退款成功记录上 return_point 之和（积分数）。 */
	int sumReturnPointForSuccessfulRefunds(long orderId);

	/** 将 send_point 置 1。 */
	void markOrderSendPointDone(long orderId);

	/** 商品可获积分：item_id -> 单件积分数。 */
	Map<Long, Long> mapItemPointAccess(long companyId, List<Long> itemIds);
}
