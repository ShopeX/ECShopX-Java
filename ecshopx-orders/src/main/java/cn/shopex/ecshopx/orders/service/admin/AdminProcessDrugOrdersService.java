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
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProcessDrugOrdersService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;
	private final AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	public AdminProcessDrugOrdersService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			AdminNormalOrderFullCancelService adminNormalOrderFullCancelService,
			AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.adminNormalOrderFullCancelService = adminNormalOrderFullCancelService;
		this.adminNormalOrderPartialCancelService = adminNormalOrderPartialCancelService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void processDrugOrders(
			long companyId,
			String operatorType,
			long operatorId,
			String orderIdFromPath,
			Map<String, Object> mergedRequestParams) {
		String tid = orderIdFromPath == null ? "" : orderIdFromPath.trim();
		if (tid.isEmpty()) {
			throw new ResourceException("无效的订单号");
		}
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.apply("order_id = {0}", tid)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("无效的订单号");
		}
		long orderId = assoc.getOrderId() == null ? 0L : assoc.getOrderId();

		String orderType =
				assoc.getOrderType() == null ? "" : assoc.getOrderType().trim().toLowerCase(Locale.ROOT);
		assertDispatchableOrderType(orderType);

		if (isApproveStatus(mergedRequestParams)) {
			approveDrugPickup(companyId, assoc, orderId, mergedRequestParams);
		} else {
			LinkedHashMap<String, Object> mergedForReject = new LinkedHashMap<>();
			if (mergedRequestParams != null) {
				mergedForReject.putAll(mergedRequestParams);
			}
			if (!mergedForReject.containsKey("order_id") || mergedForReject.get("order_id") == null) {
				String pathOid = tid.isEmpty() ? String.valueOf(orderId) : tid;
				mergedForReject.put("order_id", pathOid);
			}
			rejectDrugPickup(companyId, operatorType, operatorId, assoc, orderId, mergedForReject);
		}
	}

	private static void assertDispatchableOrderType(String orderType) {
		switch (orderType) {
			case "service":
			case "supplier_order":
			case "membercard":
				throw new ResourceException("无此类型订单！");
			case "bargain":
			case "normal_bargain":
			case "normal":
			case "service_groups":
			case "groups":
			case "normal_groups":
			case "normal_seckill":
			case "service_seckill":
			case "normal_drug":
			case "normal_shopguide":
			case "normal_pointsmall":
			case "normal_excard":
			case "normal_community":
			case "normal_shopadmin":
			case "normal_employee_purchase":
				return;
			default:
				throw new ResourceException("无此类型订单！");
		}
	}

	private static boolean isApproveStatus(Map<String, Object> merged) {
		if (merged == null || !merged.containsKey("status")) {
			return true;
		}
		Object v = merged.get("status");
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		if (v instanceof String s) {
			return "true".equalsIgnoreCase(s.trim());
		}
		if (v instanceof Number) {
			return false;
		}
		return false;
	}

	private void approveDrugPickup(
			long companyId,
			OrderAssociations assoc,
			long orderId,
			Map<String, Object> mergedRequestParams) {
		String orderStatus = assoc.getOrderStatus();
		if ("CANCEL".equalsIgnoreCase(orderStatus == null ? "" : orderStatus.trim())) {
			throw new ResourceException("订单已被取消，不要审核");
		}

		String receiptTypeInput = firstString(mergedRequestParams, "receipt_type", "ziti");
		boolean writeZiti = "ziti".equalsIgnoreCase(receiptTypeInput);
		long shopIdVal = 0L;
		if (writeZiti) {
			Object rawShop = mergedRequestParams.get("shop_id");
			if (!isZitiShopIdTruthy(rawShop)) {
				throw new ResourceException("请选择自提门店");
			}
			String shopToken = String.valueOf(rawShop).trim();
			try {
				shopIdVal = Long.parseLong(shopToken);
			} catch (NumberFormatException e) {
				throw new ResourceException("请选择自提门店");
			}
			if (shopIdVal == 0L) {
				throw new ResourceException("请选择自提门店");
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000);

		LambdaUpdateWrapper<NormalOrders> nuw = new LambdaUpdateWrapper<>();
		nuw.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getZitiStatus, "APPROVE");
		if (writeZiti) {
			nuw.set(NormalOrders::getShopId, shopIdVal).set(NormalOrders::getReceiptType, "ziti");
		}
		int mainRows = normalOrdersMapper.update(null, nuw);
		if (mainRows == 0) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}

		if (writeZiti) {
			orderAssociationsMapper.update(
					null,
					new LambdaUpdateWrapper<OrderAssociations>()
							.eq(OrderAssociations::getCompanyId, companyId)
							.eq(OrderAssociations::getOrderId, orderId)
							.set(OrderAssociations::getShopId, shopIdVal)
							.set(OrderAssociations::getUpdateTime, now));
		}

		long supCount =
				supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderId));
		if (supCount > 0) {
			LambdaUpdateWrapper<SupplierOrder> suw = new LambdaUpdateWrapper<>();
			suw.eq(SupplierOrder::getCompanyId, companyId)
					.eq(SupplierOrder::getOrderId, orderId)
					.set(SupplierOrder::getZitiStatus, "APPROVE");
			if (writeZiti) {
				suw.set(SupplierOrder::getShopId, shopIdVal).set(SupplierOrder::getReceiptType, "ziti");
			}
			supplierOrderMapper.update(null, suw);
		}
	}

	private void rejectDrugPickup(
			long companyId,
			String operatorType,
			long operatorId,
			OrderAssociations assoc,
			long orderId,
			Map<String, Object> mergedRequestParams) {
		long userId = assoc.getUserId() == null ? 0L : assoc.getUserId();
		String mobile = assoc.getMobile() == null ? "" : assoc.getMobile();

		Object reject =
				mergedRequestParams == null ? null : mergedRequestParams.get("reject_reason");
		String reasonText = reject == null ? "" : String.valueOf(reject).trim();

		LinkedHashMap<String, Object> mergedCopy = new LinkedHashMap<>();
		if (mergedRequestParams != null) {
			mergedCopy.putAll(mergedRequestParams);
		}
		mergedCopy.put("cancel_reason", reasonText);
		mergedCopy.put("cancel_from", "shop");
		mergedCopy.putIfAbsent("company_id", companyId);
		mergedCopy.putIfAbsent("user_id", userId);
		if (!mergedCopy.containsKey("order_id") || mergedCopy.get("order_id") == null) {
			mergedCopy.put("order_id", String.valueOf(orderId));
		}
		if (!mergedCopy.containsKey("mobile") || mergedCopy.get("mobile") == null) {
			mergedCopy.put("mobile", mobile);
		}

		String delivery =
				assoc.getDeliveryStatus() == null ? "" : assoc.getDeliveryStatus().trim();
		if ("PENDING".equalsIgnoreCase(delivery)) {
			adminNormalOrderFullCancelService.execute(
					companyId,
					operatorType,
					operatorId,
					0L,
					userId,
					mobile,
					orderId,
					reasonText,
					mergedCopy);
		} else {
			adminNormalOrderPartialCancelService.execute(
					companyId, operatorType, operatorId, 0L, userId, orderId, reasonText);
		}
	}

	private static boolean isZitiShopIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.doubleValue() != 0.0 && !Double.isNaN(n.doubleValue());
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return false;
		}
		return !"0".equals(s);
	}

	private static String firstString(Map<String, Object> merged, String key, String defaultValue) {
		if (merged == null || !merged.containsKey(key) || merged.get(key) == null) {
			return defaultValue;
		}
		return String.valueOf(merged.get(key)).trim();
	}
}
