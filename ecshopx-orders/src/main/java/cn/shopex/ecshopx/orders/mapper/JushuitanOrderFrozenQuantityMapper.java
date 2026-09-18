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

package cn.shopex.ecshopx.orders.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JushuitanOrderFrozenQuantityMapper {

	/**
	 * 未支付且未过自动取消时间的订单行数量之和。
	 */
	@Select("SELECT COALESCE(SUM(i.num), 0) FROM orders_normal_orders_items i "
			+ "LEFT JOIN orders_normal_orders o ON i.order_id = o.order_id "
			+ "WHERE i.company_id = #{companyId} AND i.item_id = #{itemId} "
			+ "AND o.order_status = 'NOTPAY' AND o.auto_cancel_time > #{nowEpochSeconds}")
	Long sumFrozenNumForNotPayOrdersAfterCancelDeadline(
			@Param("companyId") long companyId,
			@Param("itemId") long itemId,
			@Param("nowEpochSeconds") long nowEpochSeconds);
}
