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

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderConfirmCancelService {

	private static final Set<String> ABSTRACT_ORDER_TYPES = new HashSet<>();

	static {
		ABSTRACT_ORDER_TYPES.add("service");
		ABSTRACT_ORDER_TYPES.add("bargain");
		ABSTRACT_ORDER_TYPES.add("normal_bargain");
		ABSTRACT_ORDER_TYPES.add("normal");
		ABSTRACT_ORDER_TYPES.add("service_groups");
		ABSTRACT_ORDER_TYPES.add("groups");
		ABSTRACT_ORDER_TYPES.add("normal_groups");
		ABSTRACT_ORDER_TYPES.add("normal_seckill");
		ABSTRACT_ORDER_TYPES.add("service_seckill");
		ABSTRACT_ORDER_TYPES.add("normal_drug");
		ABSTRACT_ORDER_TYPES.add("normal_shopguide");
		ABSTRACT_ORDER_TYPES.add("normal_excard");
		ABSTRACT_ORDER_TYPES.add("normal_community");
		ABSTRACT_ORDER_TYPES.add("normal_shopadmin");
		ABSTRACT_ORDER_TYPES.add("normal_employee_purchase");
	}

	private final ApplicationContext applicationContext;
	private final AftersalesRefundService aftersalesRefundService;
	private final AdminOrderPassRefundService adminOrderPassRefundService;

	public AdminOrderConfirmCancelService(
			ApplicationContext applicationContext,
			AftersalesRefundService aftersalesRefundService,
			AdminOrderPassRefundService adminOrderPassRefundService) {
		this.applicationContext = applicationContext;
		this.aftersalesRefundService = aftersalesRefundService;
		this.adminOrderPassRefundService = adminOrderPassRefundService;
	}

	public Map<String, Object> confirmOrderCancel(
			long companyId,
			String operatorType,
			long operatorId,
			String orderIdFromPath,
			Map<String, Object> mergedRequestParams) {
		if (orderIdFromPath == null || orderIdFromPath.trim().isEmpty()) {
			throw new BadRequestException("订单号必填");
		}
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdFromPath.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("订单号格式错误");
		}
		Map<String, Object> merged = mergedRequestParams == null ? Map.of() : mergedRequestParams;
		String checkCancel = normalizeCheckCancel(merged.get("check_cancel"));
		if (checkCancel.isEmpty()) {
			throw new BadRequestException("是否同意必填");
		}
		if (!"0".equals(checkCancel) && !"1".equals(checkCancel)) {
			throw new BadRequestException("是否同意取值无效");
		}
		String shopRejectReason = merged.get("shop_reject_reason") == null ? "" : str(merged.get("shop_reject_reason"));
		if ("0".equals(checkCancel) && shopRejectReason.isEmpty()) {
			throw new BadRequestException("拒绝退款时原因必填");
		}
		String orderTypeRaw = merged.get("order_type") == null ? "normal" : str(merged.get("order_type"));
		String orderType = orderTypeRaw.isEmpty() ? "normal" : orderTypeRaw;
		long supplierId = Objects.equals("supplier", safeOperatorType(operatorType)) ? operatorId : 0L;

		Map<String, Object> params = new LinkedHashMap<>(merged);
		params.put("company_id", companyId);
		params.put("order_id", orderIdNum);
		params.put("supplier_id", supplierId);
		params.put("check_cancel", checkCancel);
		params.put("shop_reject_reason", shopRejectReason);
		params.put("order_type", orderType);
		params.put("operator_type", operatorType == null ? "" : operatorType.trim());
		params.put("operator_id", operatorId);

		if ("normal_pointsmall".equalsIgnoreCase(orderType)) {
			return confirmPointsmallBranch(companyId, orderIdNum, merged, checkCancel, shopRejectReason, params);
		}
		if ("supplier".equals(safeOperatorType(operatorType))) {
			throw new ResourceException("售前取消退款需由平台或店铺审核");
		}
		if ("supplier_order".equalsIgnoreCase(orderType) || "membercard".equalsIgnoreCase(orderType)) {
			throw new ResourceException("无此类型订单！");
		}
		if (!ABSTRACT_ORDER_TYPES.contains(orderType.toLowerCase())) {
			throw new ResourceException("无此类型订单！");
		}
		return confirmAbstractBranch(companyId, orderIdNum, supplierId, merged, checkCancel, params);
	}

	private Map<String, Object> confirmAbstractBranch(
			long companyId,
			long orderIdNum,
			long supplierId,
			Map<String, Object> merged,
			String checkCancel,
			Map<String, Object> params) {
		AftersalesRefund refund =
				aftersalesRefundService.findSingleForConfirmCancel(
						companyId,
						orderIdNum,
						supplierId,
						merged.get("refund_bn"),
						OrdersConfirmCancelRefundStatuses.REFUND_STATUSES_FOR_ABSTRACT_CANCEL_LOOKUP);
		if (refund == null) {
			throw new ResourceException("未找到可处理的退款单");
		}
		Map<String, Object> refundFilter = new LinkedHashMap<>();
		refundFilter.put("company_id", companyId);
		refundFilter.put("order_id", orderIdNum);
		refundFilter.put("supplier_id", supplierId);
		refundFilter.put("refund_bn", merged.get("refund_bn"));
		if ("1".equals(checkCancel)) {
			return adminOrderPassRefundService.passRefund(refundFilter, refund, params);
		}
		return adminOrderPassRefundService.rejectCancelAuditAfterRefundReady(refundFilter, refund, params);
	}

	private Map<String, Object> confirmPointsmallBranch(
			long companyId,
			long orderIdNum,
			Map<String, Object> merged,
			String checkCancel,
			String shopRejectReason,
			Map<String, Object> params) {
		List<AftersalesRefund> list =
				aftersalesRefundService.listForPointsmallConfirmCancel(
						companyId,
						orderIdNum,
						merged.get("refund_bn"),
						OrdersConfirmCancelRefundStatuses.REFUND_STATUSES_FOR_ABSTRACT_CANCEL_LOOKUP);
		if (list == null || list.isEmpty()) {
			throw new ResourceException("没有查到退款单，无法同意取消订单");
		}
		Map<String, Object> lastResult = null;
		AdminOrderPassRefundService proxy = applicationContext.getBean(AdminOrderPassRefundService.class);
		for (AftersalesRefund refund : list) {
			lastResult = proxy.transactionalPointsmallOne(refund, checkCancel, shopRejectReason, params);
		}
		return lastResult != null ? lastResult : Collections.emptyMap();
	}

	private static String normalizeCheckCancel(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Boolean b) {
			return b ? "1" : "0";
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty()) {
			return "";
		}
		if ("true".equalsIgnoreCase(t)) {
			return "1";
		}
		if ("false".equalsIgnoreCase(t)) {
			return "0";
		}
		return t;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String safeOperatorType(String operatorType) {
		return operatorType == null ? "" : operatorType.trim();
	}
}
