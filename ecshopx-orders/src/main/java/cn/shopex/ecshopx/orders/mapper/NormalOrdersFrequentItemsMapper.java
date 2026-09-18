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

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NormalOrdersFrequentItemsMapper {

	@Select("SELECT onoi.item_id AS item_id, COUNT(*) AS count, SUM(onoi.num) AS sum "
			+ "FROM orders_normal_orders ono "
			+ "LEFT JOIN orders_normal_orders_items onoi ON ono.order_id = onoi.order_id "
			+ "WHERE ono.company_id = #{companyId} "
			+ "AND ono.user_id = #{userId} "
			+ "AND onoi.company_id = #{companyId} "
			+ "AND onoi.user_id = #{userId} "
			+ "AND ono.pay_status = 'PAYED' "
			+ "AND ono.create_time >= #{startTime} "
			+ "GROUP BY onoi.item_id "
			+ "ORDER BY count DESC")
	List<Map<String, Object>> selectFrequentItemAggregates(
			@Param("companyId") long companyId,
			@Param("userId") Object userId,
			@Param("startTime") long startTime);
}
