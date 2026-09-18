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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceEndTimeOnOrderFinishService {

	private final OrderInvoiceMapper orderInvoiceMapper;

	public OrderInvoiceEndTimeOnOrderFinishService(OrderInvoiceMapper orderInvoiceMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
	}

	public void updateInvoiceEndTime(long companyId, long orderId, int endTimeSec, int closeAftersalesTimeSec) {
		String orderIdStr = String.valueOf(orderId);
		OrderInvoice row =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getOrderId, orderIdStr)
								.eq(OrderInvoice::getInvoiceStatus, "pending")
								.last("LIMIT 1"));
		if (row == null) {
			return;
		}
		LambdaUpdateWrapper<OrderInvoice> uw = new LambdaUpdateWrapper<>();
		uw.eq(OrderInvoice::getCompanyId, companyId)
				.eq(OrderInvoice::getOrderId, orderIdStr)
				.eq(OrderInvoice::getInvoiceStatus, "pending")
				.set(OrderInvoice::getEndTime, endTimeSec)
				.set(OrderInvoice::getCloseAftersalesTime, closeAftersalesTimeSec);
		orderInvoiceMapper.update(null, uw);
	}
}
