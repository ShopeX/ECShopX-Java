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

package cn.shopex.ecshopx.companys.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 自配送员进行中订单条数（用于禁用前校验），查询 {@code orders_normal_orders}。
 */
@Mapper
public interface OperatorSelfDeliveryOrderGuardMapper {

	@Select(
			"""
			SELECT COUNT(1) FROM orders_normal_orders
			WHERE company_id = #{companyId}
			  AND self_delivery_operator_id = #{operatorId}
			  AND self_delivery_status IN ('RECEIVEORDER', 'PACKAGED', 'DELIVERING')
			""")
	long countInProgressSelfDeliveryOrders(
			@Param("companyId") long companyId, @Param("operatorId") long operatorId);
}
