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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderUpdateInvoiceNumberService {

	private static final Set<String> ALLOWED_ORDER_TYPES_FOR_INVOICE_NUMBER =
			Set.of(
					"service",
					"bargain",
					"normal_bargain",
					"normal",
					"service_groups",
					"groups",
					"normal_groups",
					"normal_seckill",
					"service_seckill",
					"normal_drug",
					"normal_shopguide",
					"normal_pointsmall",
					"normal_excard",
					"normal_community",
					"normal_shopadmin",
					"normal_employee_purchase");

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;

	public AdminOrderUpdateInvoiceNumberService(
			OrderAssociationsMapper orderAssociationsMapper, NormalOrdersMapper normalOrdersMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public Map<String, Object> updateInvoiceNumber(long companyId, Map<String, Object> mergedInput) {
		Map<String, Object> in = mergedInput == null ? Map.of() : mergedInput;

		if (!in.containsKey("order_id")) {
			throw new BadRequestException("订单号不能为空");
		}
		if (in.get("order_id") == null) {
			throw new BadRequestException("订单号不能为空");
		}
		String orderIdRaw = String.valueOf(in.get("order_id")).trim();
		if (orderIdRaw.isEmpty() || "0".equals(orderIdRaw)) {
			throw new BadRequestException("订单号不能为空");
		}

		if (!in.containsKey("invoice_number") || in.get("invoice_number") == null) {
			throw new BadRequestException("发票号不能为空");
		}
		String invoiceNumberRaw = String.valueOf(in.get("invoice_number")).trim();
		if (invoiceNumberRaw.isEmpty()) {
			throw new BadRequestException("发票号不能为空");
		}

		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdRaw);
		} catch (NumberFormatException e) {
			throw new ResourceException("无效的订单号");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum));
		if (assoc == null) {
			throw new ResourceException("无效的订单号");
		}

		String raw =
				assoc.getOrderType() == null ? "" : assoc.getOrderType().trim().toLowerCase(Locale.ROOT);
		if ("supplier_order".equals(raw) || "membercard".equals(raw)) {
			throw new ResourceException("无此类型订单！");
		}
		if (!ALLOWED_ORDER_TYPES_FOR_INVOICE_NUMBER.contains(raw)) {
			throw new ResourceException("无此类型订单！");
		}

		NormalOrders orderRow =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderIdNum));
		if (orderRow == null || !hasInvoicePayload(orderRow.getInvoice())) {
			throw new ResourceException("此订单无发票信息");
		}

		LambdaUpdateWrapper<NormalOrders> uw =
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderIdNum)
						.set(NormalOrders::getInvoiceNumber, invoiceNumberRaw);
		int n = normalOrdersMapper.update(null, uw);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return Map.of("success", Boolean.TRUE);
	}

	private static boolean hasInvoicePayload(String invoiceJson) {
		if (invoiceJson == null) {
			return false;
		}
		String t = invoiceJson.trim();
		if (t.isEmpty() || "[]".equals(t) || "{}".equals(t)) {
			return false;
		}
		return true;
	}
}
