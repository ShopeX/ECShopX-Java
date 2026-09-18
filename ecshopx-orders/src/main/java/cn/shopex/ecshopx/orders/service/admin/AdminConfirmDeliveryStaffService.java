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
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminConfirmDeliveryStaffService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final AdminSelfDeliveryStaffFeeService adminSelfDeliveryStaffFeeService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public AdminConfirmDeliveryStaffService(
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			NormalOrdersMapper normalOrdersMapper,
			AdminSelfDeliveryStaffFeeService adminSelfDeliveryStaffFeeService,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.adminSelfDeliveryStaffFeeService = adminSelfDeliveryStaffFeeService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void confirmDeliveryStaff(
			long companyId,
			String operatorType,
			long operatorId,
			String orderIdRaw,
			long selfDeliveryOperatorId) {
		String trimmed = orderIdRaw == null ? "" : orderIdRaw.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("订单号必填");
		}
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("订单不存在");
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("订单不存在");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if ("supplier_order".equals(effective)) {
			throw new ResourceException("不支持该订单类型");
		}
		if ("service".equals(effective)
				|| (effective != null && effective.startsWith("service_"))
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			throw new ResourceException("不支持该订单类型");
		}
		if (!isNormalPhysicalFamily(effective)) {
			throw new ResourceException("不支持该订单类型");
		}

		NormalOrders row =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("订单号为" + orderIdRaw + "的订单不存在");
		}

		if (!"merchant".equals(row.getReceiptType())) {
			throw new ResourceException("普通快递和到店自提订单不能分配配送员");
		}

		String os = row.getOrderStatus();
		if ("NOTPAY".equals(os) || "CANCEL".equals(os) || "DONE".equals(os)) {
			throw new ResourceException("订单当前不可以分配配送员");
		}

		Long op = row.getSelfDeliveryOperatorId();
		if (op != null && op != 0L) {
			throw new ResourceException("订单已经分配配送员，不可重新分配");
		}

		NormalOrders freshRow =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (freshRow == null) {
			throw new ResourceException("订单不存在");
		}

		Long fop = freshRow.getSelfDeliveryOperatorId();
		if (fop != null && fop != 0L) {
			throw new ResourceException("订单已经分配配送员，不可重新分配");
		}

		int feeFen = adminSelfDeliveryStaffFeeService.computeFeeFen(companyId, freshRow, selfDeliveryOperatorId);

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<NormalOrders> u = new LambdaUpdateWrapper<>();
		u.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderIdNum)
				.set(NormalOrders::getSelfDeliveryOperatorId, selfDeliveryOperatorId)
				.set(NormalOrders::getSelfDeliveryFee, feeFen)
				.set(NormalOrders::getUpdateTime, now);
		if (!"DELIVERING".equals(freshRow.getSelfDeliveryStatus())) {
			u.set(NormalOrders::getSelfDeliveryStatus, "RECEIVEORDER");
		}
		int n = normalOrdersMapper.update(null, u);
		if (n != 1) {
			throw new ResourceException("订单不存在");
		}

		String operatorTypeTrimmed = operatorType == null ? "" : operatorType.trim();
		String operatorTypeResolved = operatorTypeTrimmed.isEmpty() ? "system" : operatorTypeTrimmed;

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", orderIdNum);
		params.put("company_id", companyId);
		params.put("operator_id", operatorId);
		params.put("operator_type", operatorTypeResolved);
		params.put("self_delivery_operator_id", selfDeliveryOperatorId);

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderIdNum);
		log.put("company_id", companyId);
		log.put("operator_type", operatorTypeResolved);
		log.put("operator_id", operatorId);
		log.put("is_show", Boolean.TRUE);
		log.put("remarks", "商家已接单");
		log.put("detail", "订单号：" + orderIdNum + "，接单分配配送员");
		log.put("params", new LinkedHashMap<>(params));
		orderProcessLogPublishPort.publish(log);
	}

	private static boolean isNormalPhysicalFamily(String effective) {
		if (effective == null || effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "normal_groups".equals(effective)
				|| "normal_drug".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_") && !"membercard".equals(effective);
	}
}
