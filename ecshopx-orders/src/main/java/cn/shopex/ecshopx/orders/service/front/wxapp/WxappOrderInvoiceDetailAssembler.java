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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceApiRowSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderInvoiceDetailAssembler {

	private final OrderInvoiceMapper orderInvoiceMapper;

	public WxappOrderInvoiceDetailAssembler(OrderInvoiceMapper orderInvoiceMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
	}

	public List<Map<String, Object>> listInvoicesForWxappDetail(long companyId, long orderId) {
		String oid = String.valueOf(orderId);
		List<OrderInvoice> rows =
				orderInvoiceMapper.selectList(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getOrderId, oid)
								.in(OrderInvoice::getInvoiceStatus, "pending", "in_process", "success", "failed")
								.orderByDesc(OrderInvoice::getId));
		List<Map<String, Object>> out = new ArrayList<>();
		for (OrderInvoice inv : rows) {
			LinkedHashMap<String, Object> m = OrderInvoiceApiRowSupport.toColumnNamesData(inv);
			m.put("invoice_items", new ArrayList<Map<String, Object>>());
			m.put("regionauth_name", "");
			m.put("user_mobile", "");
			m.put("user_card_code", "");
			m.put("distributor_name", "");
			m.put("order_holder", "");
			m.put("invoice_info", new LinkedHashMap<String, Object>());
			out.add(m);
		}
		return out;
	}

	public boolean hasShippingFeeInvoice(long companyId, long orderId) {
		return orderInvoiceMapper.countShippingFeeInvoiceLine(companyId, String.valueOf(orderId)) > 0L;
	}

	/**
	 * 订单下处于待开/处理中/成功/失败等可展示状态的发票记录，{@link OrderInvoice#getInvoiceAmount} 字段（分）求和。
	 */
	public int sumInvoiceAmountCents(long companyId, long orderId) {
		String oid = String.valueOf(orderId);
		List<OrderInvoice> rows =
				orderInvoiceMapper.selectList(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getOrderId, oid)
								.in(OrderInvoice::getInvoiceStatus, "pending", "in_process", "success", "failed"));
		int sum = 0;
		for (OrderInvoice inv : rows) {
			if (inv.getInvoiceAmount() != null) {
				sum += inv.getInvoiceAmount();
			}
		}
		return sum;
	}
}
