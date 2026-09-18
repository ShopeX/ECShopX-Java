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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Admin order shipment API: merges JWT context into params and delegates physical delivery to
 * {@link AdminNormalOrderDeliveryCoreService}. Order-process log dispatch is published only inside that core
 * service via the shared {@code OrderProcessLogPublishPort}; this type does not perform a second publish.
 *
 * <p>Normal-order delivery sync ({@link cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames#EVENT_NORMAL_ORDER_DELIVERY})
 * is scheduled only inside {@link AdminNormalOrderDeliveryCoreService} after commit. Service-layer probe:
 * {@code AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest} — run
 * {@code mvn -pl ecshopx-orders test -Dtest=AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest}.
 */
@Service
public class AdminOrderDeliveryService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminSupplierDeliveryParamsAdjustService adminSupplierDeliveryParamsAdjustService;
	private final AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;
	private final OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;

	public AdminOrderDeliveryService(
			OrderAssociationsMapper orderAssociationsMapper,
			AdminSupplierDeliveryParamsAdjustService adminSupplierDeliveryParamsAdjustService,
			AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService,
			OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminSupplierDeliveryParamsAdjustService = adminSupplierDeliveryParamsAdjustService;
		this.adminDeliveryLogisticsNameService = adminDeliveryLogisticsNameService;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDeliveryCoreService = adminNormalOrderDeliveryCoreService;
		this.orderAssociationAssociationDataAssembler = orderAssociationAssociationDataAssembler;
	}

	public Map<String, Object> delivery(
			long companyId, String operatorType, long operatorId, Map<String, Object> params) {
		int supplierId = 0;
		if ("supplier".equals(operatorType == null ? null : operatorType.trim())) {
			if (operatorId > Integer.MAX_VALUE || operatorId < Integer.MIN_VALUE) {
				throw new ResourceException("供应商标识无效");
			}
			supplierId = (int) operatorId;
		}
		Map<String, Object> p = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
		p.put("company_id", companyId);
		p.put("operator_type", operatorType == null ? "" : operatorType.trim());
		p.put("operator_id", operatorId);
		p.put("supplier_id", supplierId);

		long orderId = parseOrderId(p.get("order_id"));
		if (orderId <= 0L) {
			throw new ResourceException("订单号缺少！");
		}
		p.put("order_id", orderId);

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}
		if (supplierId > 0) {
			adminSupplierDeliveryParamsAdjustService.adjustForSupplierIfNeeded(p, assoc, supplierId);
		}
		adminDeliveryLogisticsNameService.fillLogiName(p);

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if ("supplier_order".equals(effective)) {
			throw new ResourceException("当前订单类型不支持发货");
		}
		if ("service".equals(effective)
				|| (effective != null && effective.startsWith("service_"))
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return null;
		}
		if (!isNormalPhysicalFamily(effective)) {
			return null;
		}

		adminNormalOrderDeliveryCoreService.deliveryNormalPhysical(p, assoc, effective);

		OrderAssociations reloaded =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (reloaded == null) {
			throw new ResourceException("此订单不存在！");
		}
		return orderAssociationAssociationDataAssembler.toAssociationDataMap(reloaded);
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

	private static long parseOrderId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
