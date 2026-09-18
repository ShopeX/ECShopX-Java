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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceCancelOnOrderCancelService {

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final NormalOrdersMapper normalOrdersMapper;

	public OrderInvoiceCancelOnOrderCancelService(
			OrderInvoiceMapper orderInvoiceMapper, NormalOrdersMapper normalOrdersMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public void updateInvoiceStatusCancel(long companyId, long orderId, String status) {
		OrderInvoice inv =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getOrderId, String.valueOf(orderId))
								.eq(OrderInvoice::getInvoiceStatus, "pending")
								.last("LIMIT 1"));
		if (inv == null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderInvoice> iu = new LambdaUpdateWrapper<>();
		iu.eq(OrderInvoice::getId, inv.getId())
				.set(OrderInvoice::getInvoiceStatus, status)
				.set(OrderInvoice::getUpdateTime, now);
		orderInvoiceMapper.update(null, iu);

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getInvoiceStatus, status);
		normalOrdersMapper.update(null, ou);
	}
}
