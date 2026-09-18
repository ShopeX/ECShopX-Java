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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelV2FailException;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderConfirmCancelService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderConfirmCancelService {

	private static final Map<String, String> RESOURCE_MSG_MAP =
			Map.ofEntries(
					Map.entry("未找到可处理的退款单", "没有查到退款单，无法同意取消订单"),
					Map.entry("退款单非待审核状态，不可拒审", "退款单状态不是待审核状态，无法拒绝"),
					Map.entry("该退款单已退款成功", "退款单状态 已退款，无法继续操作"),
					Map.entry("该退款单已拒绝", "退款单状态 已驳回，无法继续操作"),
					Map.entry("该退款单已审核通过", "退款单状态 已审核通过，无法继续操作"),
					Map.entry("该退款单已撤销", "退款单状态 已撤销，无法继续操作"),
					Map.entry("该退款单处理中", "退款单状态 已发起退款等待到账，无法继续操作"),
					Map.entry("该退款单状态异常", "退款单状态 退款异常，无法继续操作"),
					Map.entry("退款单状态不允许审核通过", "退款单状态 已退款，无法继续操作"));

	private static final Map<String, String> BAD_REQUEST_MSG_MAP =
			Map.of("订单号格式错误", "没有查到退款单，无法同意取消订单");

	private final AdminOrderConfirmCancelService adminOrderConfirmCancelService;

	public OpenapiThirdApiV2OrderConfirmCancelService(
			AdminOrderConfirmCancelService adminOrderConfirmCancelService) {
		this.adminOrderConfirmCancelService = adminOrderConfirmCancelService;
	}

	public void executeOpenapiConfirmCancel(
			long companyId,
			String orderIdRaw,
			String cancelHandleRaw,
			String rejectReasonRaw) {
		validateParams(orderIdRaw, cancelHandleRaw, rejectReasonRaw);

		String cancelHandle = trimToEmpty(cancelHandleRaw);
		String rejectReason = rejectReasonRaw == null ? "" : rejectReasonRaw.trim();

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("check_cancel", cancelHandle);
		merged.put("shop_reject_reason", rejectReason);
		merged.put("order_type", "normal");

		try {
			adminOrderConfirmCancelService.confirmOrderCancel(
					companyId, "openapi", 0L, trimToEmpty(orderIdRaw), merged);
		} catch (OpenapiOrderCancelV2FailException e) {
			throw e;
		} catch (ResourceException e) {
			throw handleError(mapResourceMessage(e.getMessage()));
		} catch (BadRequestException e) {
			throw handleError(mapBadRequestMessage(e.getMessage()));
		} catch (Exception e) {
			throw handleError(e.getMessage() == null ? "" : e.getMessage());
		}
	}

	private void validateParams(String orderIdRaw, String cancelHandleRaw, String rejectReasonRaw) {
		if (!StringUtils.hasText(trimToEmpty(orderIdRaw))) {
			throw missingParams("订单编号必填");
		}
		String cancelHandle = trimToEmpty(cancelHandleRaw);
		if (!StringUtils.hasText(cancelHandle)) {
			throw missingParams("是否同意必填");
		}
		if (!cancelHandle.matches("^[01]$")) {
			throw missingParams("是否同意必填");
		}
		if ("0".equals(cancelHandle) && !StringUtils.hasText(trimToEmpty(rejectReasonRaw))) {
			throw missingParams("拒绝退款时原因必填");
		}
	}

	private static String mapResourceMessage(String adminMsg) {
		if (adminMsg == null) {
			return "";
		}
		return RESOURCE_MSG_MAP.getOrDefault(adminMsg, adminMsg);
	}

	private static String mapBadRequestMessage(String adminMsg) {
		if (adminMsg == null) {
			return "";
		}
		return BAD_REQUEST_MSG_MAP.getOrDefault(adminMsg, adminMsg);
	}

	private static OpenapiOrderCancelV2FailException missingParams(String message) {
		return new OpenapiOrderCancelV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiOrderCancelV2FailException handleError(String message) {
		return new OpenapiOrderCancelV2FailException(OpenapiErrorCode.ORDER_HANDLE_ERROR, message);
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}
}
