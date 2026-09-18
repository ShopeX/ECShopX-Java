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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelV2FailException;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderFullCancelService;
import cn.shopex.ecshopx.orders.service.admin.OrderCancelReasonConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderCancelService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderCancelReasonConfig orderCancelReasonConfig;
	private final AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;

	public OpenapiThirdApiV2OrderCancelService(
			OrderAssociationsMapper orderAssociationsMapper,
			OrderCancelReasonConfig orderCancelReasonConfig,
			AdminNormalOrderFullCancelService adminNormalOrderFullCancelService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderCancelReasonConfig = orderCancelReasonConfig;
		this.adminNormalOrderFullCancelService = adminNormalOrderFullCancelService;
	}

	public void executeOpenapiCancel(
			long companyId,
			String orderIdRaw,
			String cancelReasonIdRaw,
			String cancelReasonRaw) {
		validateParams(orderIdRaw, cancelReasonIdRaw, cancelReasonRaw);

		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw.trim());
		} catch (NumberFormatException e) {
			throw orderNotFound();
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw orderNotFound();
		}

		if (!"normal".equalsIgnoreCase(trimToEmpty(assoc.getOrderType()))) {
			throw handleError("实体类订单才能取消订单");
		}

		int reasonId;
		try {
			reasonId = Integer.parseInt(cancelReasonIdRaw.trim());
		} catch (NumberFormatException e) {
			throw handleError("请填写正确的取消原因ID");
		}
		if (!orderCancelReasonConfig.hasReasonKey(reasonId)) {
			throw handleError("请填写正确的取消原因ID");
		}

		String configText =
				reasonId != 12 ? orderCancelReasonConfig.reasonTextForKey(reasonId) : "";
		String otherReason = cancelReasonRaw == null ? "" : cancelReasonRaw.trim();
		String effectiveReasonText = StringUtils.hasText(otherReason) ? otherReason : configText;

		long userId = assoc.getUserId() == null ? 0L : assoc.getUserId();
		String mobile = assoc.getMobile() == null ? "" : assoc.getMobile().trim();

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("order_id", orderId);
		merged.put("cancel_from", "shop");
		merged.put("cancel_reason", configText);
		merged.put("other_reason", otherReason);
		merged.put("user_id", userId);
		merged.put("mobile", mobile);
		merged.put("operator_type", "openapi");
		merged.put("operator_id", 0L);

		try {
			adminNormalOrderFullCancelService.execute(
					companyId,
					"openapi",
					0L,
					0L,
					userId,
					mobile,
					orderId,
					effectiveReasonText,
					merged,
					"shop");
		} catch (OpenapiOrderCancelV2FailException e) {
			throw e;
		} catch (ResourceException e) {
			throw handleError(e.getMessage());
		} catch (Exception e) {
			throw handleError(e.getMessage());
		}
	}

	private void validateParams(String orderIdRaw, String cancelReasonIdRaw, String cancelReasonRaw) {
		if (!StringUtils.hasText(trimToEmpty(orderIdRaw))) {
			throw missingParams("请填写订单编号");
		}
		if (!StringUtils.hasText(trimToEmpty(cancelReasonIdRaw))) {
			throw missingParams("请填写取消原因ID");
		}
		try {
			int parsedId = Integer.parseInt(cancelReasonIdRaw.trim());
			if (parsedId == 12 && !StringUtils.hasText(trimToEmpty(cancelReasonRaw))) {
				throw missingParams("请填写取消原因描述");
			}
		} catch (NumberFormatException ignored) {
			// parse 失败留给 hasReasonKey → E5310
		}
		if (cancelReasonRaw != null && cancelReasonRaw.trim().length() > 255) {
			throw missingParams("请填写取消原因描述");
		}
	}

	private static OpenapiOrderCancelV2FailException missingParams(String message) {
		return new OpenapiOrderCancelV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiOrderCancelV2FailException orderNotFound() {
		return new OpenapiOrderCancelV2FailException(
				OpenapiErrorCode.ORDER_NOT_FOUND, "订单相应的明细不存在");
	}

	private static OpenapiOrderCancelV2FailException handleError(String message) {
		return new OpenapiOrderCancelV2FailException(OpenapiErrorCode.ORDER_HANDLE_ERROR, message);
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}
}
