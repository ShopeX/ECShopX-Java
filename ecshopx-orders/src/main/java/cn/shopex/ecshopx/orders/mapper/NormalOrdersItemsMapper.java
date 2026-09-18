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

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NormalOrdersItemsMapper extends BaseMapper<NormalOrdersItems> {

	/**
	 * 未支付且未过自动取消时间的订单行数量之和，与主单 {@code order_status=NOTPAY} 且 {@code auto_cancel_time} 未过期 对齐。
	 */
	@Select("SELECT COALESCE(SUM(i.num), 0) FROM orders_normal_orders_items i "
			+ "LEFT JOIN orders_normal_orders o ON i.order_id = o.order_id "
			+ "WHERE i.company_id = #{companyId} AND i.item_id = #{itemId} "
			+ "AND o.order_status = 'NOTPAY' AND o.auto_cancel_time > #{nowEpochSeconds}")
	Long sumUnpaidHoldingNumForItem(
			@Param("companyId") long companyId,
			@Param("itemId") long itemId,
			@Param("nowEpochSeconds") long nowEpochSeconds);

	@Select("SELECT id, user_id, item_id, item_bn, item_name, pic, num, price, item_fee, item_spec_desc, "
			+ "order_item_type FROM orders_normal_orders_items WHERE order_id = #{orderId} LIMIT 1")
	List<Map<String, Object>> selectFirstItemByOrderId(@Param("orderId") long orderId);

	@Select("SELECT item_name, item_id, item_bn, price, total_fee, num, item_spec_desc, pic "
			+ "FROM orders_normal_orders_items "
			+ "WHERE company_id = #{companyId} AND order_id = #{orderId}")
	List<Map<String, Object>> selectItemsByCompanyAndOrderId(
			@Param("companyId") long companyId,
			@Param("orderId") long orderId);
}
