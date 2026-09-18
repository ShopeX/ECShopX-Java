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

import cn.shopex.ecshopx.orders.statement.generate.StatementDistributorOrderRow;
import cn.shopex.ecshopx.orders.statement.generate.StatementOrderItemNumRow;
import cn.shopex.ecshopx.orders.statement.generate.StatementRefundInfoRow;
import cn.shopex.ecshopx.orders.statement.generate.StatementSupplierOrderRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StatementGenerationQueryMapper {

	List<StatementDistributorOrderRow> selectDistributorOrdersPage(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("endTime") long endTime,
			@Param("offset") int offset,
			@Param("limit") int limit);

	List<StatementSupplierOrderRow> selectSupplierOrdersPage(
			@Param("companyId") long companyId,
			@Param("operatorId") long operatorId,
			@Param("endTime") long endTime,
			@Param("offset") int offset,
			@Param("limit") int limit);

	int sumItemCostForDistributorOrder(
			@Param("companyId") long companyId,
			@Param("orderId") long orderId);

	long sumAftersalesDetailNumForOrder(
			@Param("companyId") long companyId, @Param("orderId") long orderId);

	List<StatementOrderItemNumRow> sumOrderSupplierItemNums(
			@Param("companyId") long companyId,
			@Param("orderIds") List<Long> orderIds,
			@Param("operatorId") long operatorId);

	StatementRefundInfoRow selectSupplierRefundInfo(
			@Param("companyId") long companyId,
			@Param("orderId") long orderId,
			@Param("operatorId") long operatorId);
}
