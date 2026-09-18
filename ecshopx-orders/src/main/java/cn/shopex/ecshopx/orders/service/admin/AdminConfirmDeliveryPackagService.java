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
public class AdminConfirmDeliveryPackagService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public AdminConfirmDeliveryPackagService(
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			NormalOrdersMapper normalOrdersMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void confirmDeliveryPackag(
			long companyId, String operatorType, long operatorId, String orderIdRaw) {
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

		String sds = row.getSelfDeliveryStatus();
		if ("DELIVERING".equals(sds) || "DONE".equals(sds)) {
			throw new ResourceException("当前状态不可以确认打包");
		}

		String operatorTypeNorm =
				operatorType == null || operatorType.trim().isEmpty() ? "system" : operatorType.trim();

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", orderIdNum);
		params.put("company_id", companyId);
		params.put("operator_id", operatorId);
		params.put("operator_type", operatorTypeNorm);
		params.put("self_delivery_status", "PACKAGED");

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<NormalOrders> u = new LambdaUpdateWrapper<>();
		u.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderIdNum)
				.set(NormalOrders::getSelfDeliveryStatus, "PACKAGED")
				.set(NormalOrders::getUpdateTime, now);
		int n = normalOrdersMapper.update(null, u);
		if (n != 1) {
			throw new ResourceException("订单不存在");
		}

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderIdNum);
		log.put("company_id", companyId);
		log.put("operator_type", operatorTypeNorm);
		log.put("operator_id", operatorId);
		log.put("is_show", Boolean.TRUE);
		log.put("remarks", "商家已打包");
		log.put("detail", "订单号：" + orderIdNum + "，已打包");
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
