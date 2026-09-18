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

package cn.shopex.ecshopx.supplier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.supplier.dto.admin.v1.SupplierOrderPaidConfirmRequest;
import cn.shopex.ecshopx.supplier.service.admin.SupplierOrderListService;
import cn.shopex.ecshopx.supplier.service.admin.SupplierOrderPaidConfirmService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@RestController("supplierAdminV1SupplierOrder")
@RequestMapping("/api/v1/supplier")
public class SupplierOrderController {

	private final SupplierOrderPaidConfirmService supplierOrderPaidConfirmService;
	private final SupplierOrderListService supplierOrderListService;

	public SupplierOrderController(
			SupplierOrderPaidConfirmService supplierOrderPaidConfirmService,
			SupplierOrderListService supplierOrderListService) {
		this.supplierOrderPaidConfirmService = supplierOrderPaidConfirmService;
		this.supplierOrderListService = supplierOrderListService;
	}

	@Activated(routeAlias = "order.supplier.get_order_list")
	@GetMapping(value = "/get_order_list", name = "供应商订单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderList(HttpServletRequest httpRequest) {
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		long supplierId = parsePositiveLongClaim(jwt, "operator_id", "operator_id 无效");
		if (supplierId > Integer.MAX_VALUE) {
			throw new BadRequestException("operator_id 无效");
		}
		Map<String, Object> data = supplierOrderListService.getOrderList(companyId, supplierId, httpRequest);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "order.supplier.order_paid_confirm")
	@PostMapping(value = "/order_paid_confirm", name = "确认付款")
	public ResponseEntity<ApiResult<Map<String, Object>>> orderPaidConfirm(
			HttpServletRequest httpRequest, @FlexibleBody SupplierOrderPaidConfirmRequest body) {
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		long supplierId = parsePositiveLongClaim(jwt, "operator_id", "operator_id 无效");
		if (supplierId > Integer.MAX_VALUE) {
			throw new BadRequestException("operator_id 无效");
		}
		long orderId = body == null || body.getOrderId() == null ? 0L : body.getOrderId();
		supplierOrderPaidConfirmService.orderPaidConfirm(companyId, supplierId, orderId);
		return ResponseEntity.ok(ApiResult.ok(new LinkedHashMap<>()));
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}
}
