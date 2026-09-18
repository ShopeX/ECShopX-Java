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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SupplierItemDeleteOrderGuardMapper {

	@Select("<script>"
			+ "SELECT COUNT(*) FROM orders_normal_orders_items oi "
			+ "INNER JOIN orders_normal_orders o ON oi.order_id = o.order_id "
			+ "WHERE oi.company_id = #{companyId} AND o.supplier_id = #{supplierId} "
			+ "AND o.cancel_status != 'SUCCESS' "
			+ "AND (o.order_auto_close_aftersales_time &gt; #{nowSeconds} "
			+ "OR o.order_auto_close_aftersales_time IS NULL OR o.order_auto_close_aftersales_time = 0) "
			+ "AND oi.item_id IN "
			+ "<foreach collection='poolItemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
			+ "</script>")
	long countUnfinishedOrders(@Param("companyId") long companyId, @Param("supplierId") long supplierId,
			@Param("nowSeconds") int nowSeconds, @Param("poolItemIds") List<Long> poolItemIds);

	@Select("<script>"
			+ "SELECT COUNT(*) FROM aftersales_detail ad "
			+ "INNER JOIN aftersales a ON ad.aftersales_bn = a.aftersales_bn "
			+ "WHERE ad.company_id = #{companyId} AND a.supplier_id = #{supplierId} "
			+ "AND ad.aftersales_status IN (0, 1) "
			+ "AND ad.item_id IN "
			+ "<foreach collection='poolItemIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
			+ "</script>")
	long countUnfinishedAftersales(@Param("companyId") long companyId, @Param("supplierId") long supplierId,
			@Param("poolItemIds") List<Long> poolItemIds);
}
