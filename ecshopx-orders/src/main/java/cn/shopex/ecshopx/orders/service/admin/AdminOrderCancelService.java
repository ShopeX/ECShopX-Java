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
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderCancelService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final OrderCancelReasonConfig orderCancelReasonConfig;
	private final AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;
	private final AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;
	private final PlatformSelfSubCancelSupport platformSelfSubCancelSupport;

	public AdminOrderCancelService(
			OrderAssociationsMapper orderAssociationsMapper,
			SupplierOrderMapper supplierOrderMapper,
			OrderCancelReasonConfig orderCancelReasonConfig,
			AdminNormalOrderFullCancelService adminNormalOrderFullCancelService,
			AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService,
			PlatformSelfSubCancelSupport platformSelfSubCancelSupport) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.orderCancelReasonConfig = orderCancelReasonConfig;
		this.adminNormalOrderFullCancelService = adminNormalOrderFullCancelService;
		this.adminNormalOrderPartialCancelService = adminNormalOrderPartialCancelService;
		this.platformSelfSubCancelSupport = platformSelfSubCancelSupport;
	}

	/**
	 * @apiNote When {@code delivery_status} is {@code PENDING}, delegates to {@link AdminNormalOrderFullCancelService}
	 * with {@code cancelFrom} {@code "shop"} (same service as wxapp whole-order cancel); a copy of the merged request
	 * map is used so {@code cancel_from} is set to {@code "shop"} for downstream order-process-log payloads. Jushuitan
	 * trade-cancel Bus dispatch is performed only inside that service, not here. Non-{@code PENDING} uses
	 * {@link AdminNormalOrderPartialCancelService} and does not carry this event path.
	 */
	public Map<String, Object> cancelOrder(
			long companyId,
			String operatorType,
			long operatorId,
			String orderIdFromPath,
			Map<String, Object> mergedRequestParams) {
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdFromPath == null ? "" : orderIdFromPath.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("订单号格式错误");
		}
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("订单号为" + orderIdNum + "的订单不存在");
		}
		if (!"normal".equalsIgnoreCase(safe(assoc.getOrderType()))) {
			throw new ResourceException("实体类订单才能取消订单！");
		}
		int reasonKey = parseCancelReasonKey(mergedRequestParams.get("cancel_reason"));
		String reasonText = orderCancelReasonConfig.reasonTextForKey(reasonKey);

		long userId = assoc.getUserId() == null ? 0L : assoc.getUserId();
		String mobile = assoc.getMobile() == null ? "" : assoc.getMobile();
		long supplierId = 0L;
		if ("supplier".equalsIgnoreCase(safe(operatorType))) {
			supplierId = operatorId;
		}

		String delivery = resolveCancelDeliveryStatus(companyId, orderIdNum, supplierId, assoc);
		if ("PENDING".equalsIgnoreCase(delivery)) {
			LinkedHashMap<String, Object> fullCancelParams = new LinkedHashMap<>(mergedRequestParams);
			fullCancelParams.put("cancel_from", "shop");
			if (supplierId <= 0L
					&& platformSelfSubCancelSupport.isShopRemainingFullCancelScope(
							companyId, orderIdNum, safe(assoc.getDeliveryStatus()))) {
				fullCancelParams.put(PlatformSelfSubCancelSupport.SHOP_REMAINING_FULL_CANCEL_PARAM, true);
			} else if (supplierId <= 0L
					&& platformSelfSubCancelSupport.isScope(
							companyId, orderIdNum, safe(assoc.getDeliveryStatus()))) {
				fullCancelParams.put(PlatformSelfSubCancelSupport.PLATFORM_SELF_SUB_CANCEL_PARAM, true);
			}
			return adminNormalOrderFullCancelService.execute(
					companyId,
					operatorType,
					operatorId,
					supplierId,
					userId,
					mobile,
					orderIdNum,
					reasonText,
					fullCancelParams,
					"shop");
		}
		return adminNormalOrderPartialCancelService.execute(
				companyId, operatorType, operatorId, supplierId, userId, orderIdNum, reasonText);
	}

	/**
	 * 供应商取消未发货子单：按 supplier_order.delivery_status 判断，不能因主单已部分发货而走入部分取消售后。
	 */
	private String resolveCancelDeliveryStatus(
			long companyId, long orderId, long supplierId, OrderAssociations assoc) {
		if (supplierId <= 0L) {
			if (platformSelfSubCancelSupport.isShopRemainingFullCancelScope(
					companyId, orderId, safe(assoc.getDeliveryStatus()))) {
				return "PENDING";
			}
			if (platformSelfSubCancelSupport.isScope(companyId, orderId, safe(assoc.getDeliveryStatus()))) {
				return "PENDING";
			}
			return safe(assoc.getDeliveryStatus());
		}
		if (supplierId > Integer.MAX_VALUE) {
			throw new BadRequestException("供应商ID无效");
		}
		SupplierOrder supplierOrder =
				supplierOrderMapper.selectOne(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderId)
								.eq(SupplierOrder::getSupplierId, (int) supplierId)
								.last("LIMIT 1"));
		if (supplierOrder == null) {
			throw new ResourceException("供应商订单不存在");
		}
		return safe(supplierOrder.getDeliveryStatus());
	}

	private static int parseCancelReasonKey(Object raw) {
		if (raw == null) {
			throw new BadRequestException("取消原因无效");
		}
		try {
			int k = Integer.parseInt(String.valueOf(raw).trim());
			if (k < 1 || k > 12) {
				throw new BadRequestException("取消原因无效");
			}
			return k;
		} catch (NumberFormatException e) {
			throw new BadRequestException("取消原因无效");
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
