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

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderInvoiceMapper extends BaseMapper<OrderInvoice> {

	@Select(
			"""
			SELECT COUNT(1) FROM orders_invoice_item i
			INNER JOIN orders_invoice inv ON inv.id = i.invoice_id
			WHERE i.company_id = #{companyId} AND i.order_id = #{orderIdStr}
			AND i.item_bn = 'shippingFeeLine888'
			AND inv.invoice_status IN ('in_process', 'success', 'inProgress')
			LIMIT 1
			""")
	long countShippingFeeInvoiceLine(@Param("companyId") long companyId, @Param("orderIdStr") String orderIdStr);
}
