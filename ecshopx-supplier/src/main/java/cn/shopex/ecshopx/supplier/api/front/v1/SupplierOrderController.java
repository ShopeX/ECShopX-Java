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

package cn.shopex.ecshopx.supplier.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.supplier.service.front.SupplierBuyerOrderPayStatusService;
import cn.shopex.ecshopx.supplier.service.front.SupplierOrderOfflinePayInfoService;
import cn.shopex.ecshopx.supplier.service.front.dto.OfflinePayInfoEntry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("supplierFrontV1SupplierOrder")
@RequestMapping("/api/v1/h5app/wxapp")
public class SupplierOrderController {

	private final SupplierBuyerOrderPayStatusService supplierBuyerOrderPayStatusService;
	private final SupplierOrderOfflinePayInfoService supplierOrderOfflinePayInfoService;

	public SupplierOrderController(
			SupplierBuyerOrderPayStatusService supplierBuyerOrderPayStatusService,
			SupplierOrderOfflinePayInfoService supplierOrderOfflinePayInfoService) {
		this.supplierBuyerOrderPayStatusService = supplierBuyerOrderPayStatusService;
		this.supplierOrderOfflinePayInfoService = supplierOrderOfflinePayInfoService;
	}

	@GetMapping(value = "/order/get_offline_pay_info", name = "线下支付信息")
	public ResponseEntity<List<OfflinePayInfoEntry>> getOfflinePayInfo(
			HttpServletRequest request, @RequestParam(value = "order_id", required = false) String orderId) {
		long companyId = resolveCompanyIdForSetOrderPayStatus(request);
		long userId = resolveUserIdForSetOrderPayStatus(request);
		return ResponseEntity.ok(supplierOrderOfflinePayInfoService.getOfflinePayInfo(companyId, userId, orderId));
	}

	@PostMapping(value = "/supplier/set_order_pay_status", name = "已转账")
	public ResponseEntity<ApiResult<Map<String, Object>>> setOrderPayStatus(
			HttpServletRequest request,
			@RequestParam(value = "order_id", required = false) String orderIdQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = resolveCompanyIdForSetOrderPayStatus(request);
		long userId = resolveUserIdForSetOrderPayStatus(request);
		String orderIdRaw = mergeOrderIdRaw(orderIdQuery, body);
		Map<String, Object> data = supplierBuyerOrderPayStatusService.setOrderPayStatus(companyId, userId, orderIdRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String mergeOrderIdRaw(String orderIdQuery, Map<String, Object> body) {
		if (body != null && body.containsKey("order_id")) {
			return normalizeOrderIdFromBodyValue(body.get("order_id"));
		}
		return orderIdQuery;
	}

	private static String normalizeOrderIdFromBodyValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof List<?> list) {
			for (Object el : list) {
				if (el == null) {
					continue;
				}
				String s = scalarOrderIdToString(el);
				if (s != null && !s.isEmpty()) {
					return s;
				}
			}
			return null;
		}
		return scalarOrderIdToString(value);
	}

	private static String scalarOrderIdToString(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		if (o instanceof String s) {
			return s.trim();
		}
		return String.valueOf(o).trim();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMapOrEmpty(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseRequiredPositiveLongClaim(Map<String, Object> claims, String key) {
		Object v = claims.get(key);
		if (v == null) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			if (x <= 0) {
				throw new UnauthorizedException("Unable to authenticate user.");
			}
			return x;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		try {
			long x = Long.parseLong(s);
			if (x <= 0) {
				throw new UnauthorizedException("Unable to authenticate user.");
			}
			return x;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
	}

	private static long resolveCompanyIdForSetOrderPayStatus(HttpServletRequest request) {
		Object attr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (attr instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				return v;
			}
		} else if (attr instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				try {
					long v = Long.parseLong(t);
					if (v > 0) {
						return v;
					}
				} catch (NumberFormatException ignored) {
					// fall through to claims
				}
			}
		}
		Map<String, Object> claims = readH5AuthClaimsMapOrEmpty(request);
		if (claims.isEmpty()) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return parseRequiredPositiveLongClaim(claims, "company_id");
	}

	private static long resolveUserIdForSetOrderPayStatus(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMapOrEmpty(request);
		if (claims.isEmpty()) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return parseRequiredPositiveLongClaim(claims, "user_id");
	}
}
