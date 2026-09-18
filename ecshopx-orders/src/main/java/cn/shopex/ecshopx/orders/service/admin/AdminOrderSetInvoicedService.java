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
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderSetInvoicedService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;

	public AdminOrderSetInvoicedService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
	}

	public Map<String, Object> setInvoiced(
			long companyId, String operatorType, long operatorId, Map<String, Object> mergedInput) {
		Map<String, Object> in = mergedInput == null ? Map.of() : mergedInput;

		if (!in.containsKey("order_id")) {
			throw new BadRequestException("订单号不能为空");
		}
		Object orderIdObj = in.get("order_id");
		if (orderIdObj == null) {
			throw new BadRequestException("订单号不能为空");
		}
		String orderIdRaw = String.valueOf(orderIdObj).trim();
		if (orderIdRaw.isEmpty() || "0".equals(orderIdRaw)) {
			throw new BadRequestException("订单号不能为空");
		}

		if (!in.containsKey("status")) {
			throw new BadRequestException("状态不能为空");
		}
		Object statusVal = in.get("status");
		if (statusVal == null) {
			throw new BadRequestException("状态不能为空");
		}
		if (String.valueOf(statusVal).trim().isEmpty()) {
			throw new BadRequestException("状态不能为空");
		}

		boolean invoicedFlag = invoicedFlagFromStatus(statusVal);

		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdRaw);
		} catch (NumberFormatException e) {
			throw new ResourceException("无效的订单号");
		}

		if (operatorType != null && "supplier".equals(operatorType.trim())) {
			return setInvoicedSupplier(companyId, operatorId, orderIdNum, invoicedFlag);
		}
		return setInvoicedNonSupplier(companyId, orderIdNum, invoicedFlag);
	}

	private Map<String, Object> setInvoicedSupplier(
			long companyId, long operatorId, long orderIdNum, boolean invoicedFlag) {
		SupplierOrder row =
				supplierOrderMapper.selectOne(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getSupplierId, operatorId)
								.eq(SupplierOrder::getOrderId, orderIdNum));
		if (row == null) {
			throw new ResourceException("此订单无发票信息");
		}
		if (!hasInvoicePayload(row.getInvoice())) {
			throw new ResourceException("此订单无发票信息");
		}
		LambdaUpdateWrapper<SupplierOrder> uw =
				new LambdaUpdateWrapper<SupplierOrder>()
						.eq(SupplierOrder::getCompanyId, companyId)
						.eq(SupplierOrder::getSupplierId, operatorId)
						.eq(SupplierOrder::getOrderId, orderIdNum)
						.set(SupplierOrder::getIsInvoiced, invoicedFlag);
		supplierOrderMapper.update(null, uw);
		return Map.of("success", Boolean.TRUE);
	}

	private Map<String, Object> setInvoicedNonSupplier(
			long companyId, long orderIdNum, boolean invoicedFlag) {
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
		if ("membercard".equals(raw)) {
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
		LambdaUpdateWrapper<NormalOrders> ou =
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderIdNum)
						.set(NormalOrders::getIsInvoiced, invoicedFlag);
		int n = normalOrdersMapper.update(null, ou);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return Map.of("success", Boolean.TRUE);
	}

	private static boolean invoicedFlagFromStatus(Object statusVal) {
		String s = String.valueOf(statusVal).trim();
		return "true".equals(s) || "1".equals(s);
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
