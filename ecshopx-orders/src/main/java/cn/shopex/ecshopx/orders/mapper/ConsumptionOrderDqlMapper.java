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

import cn.shopex.ecshopx.orders.domain.dto.OrderIdRefundSumRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsumptionOrderDqlMapper {

	/** analysis §3 2.2：有「未完结」售后行（NOT IN 2,3,4）的订单 id。 */
	List<Long> selectHaveAftersalesOrderIds(@Param("timeSec") long timeSec);

	/** analysis §3 3.1 */
	List<Long> selectProcessedAftersalesOrderIds(@Param("orderIds") List<Long> orderIds);

	/** analysis §3 3.2 */
	List<OrderIdRefundSumRow> selectRefundSumsExcludingPoint(
			@Param("orderIds") List<Long> orderIds);
}
