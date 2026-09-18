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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.supplier.service.SupplierInfoService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("supplierFrontV1Supplier")
@RequestMapping("/api/v1/h5app/wxapp/supplier")
public class SupplierController {

	private final SupplierInfoService supplierInfoService;

	public SupplierController(SupplierInfoService supplierInfoService) {
		this.supplierInfoService = supplierInfoService;
	}

	@GetMapping(value = "/get_supplier_info", name = "供应商信息")
	public ResponseEntity<Map<String, Object>> getSupplierInfo(
			HttpServletRequest request,
			@RequestParam(value = "supplier_id", required = false) String supplierIdRaw) {
		long companyId = resolveH5CompanyId(request);
		long operatorId = parseSupplierIdAsOperatorId(supplierIdRaw);
		LinkedHashMap<String, Object> payload = supplierInfoService.getWxappSupplierPublicInfo(companyId, operatorId);
		return ResponseEntity.ok(Map.of("data", payload));
	}

	private static long parseSupplierIdAsOperatorId(String supplierIdRaw) {
		if (supplierIdRaw == null || supplierIdRaw.trim().isEmpty()) {
			throw new ResourceException("供应商ID错误");
		}
		String t = supplierIdRaw.trim();
		long v;
		try {
			v = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("供应商ID错误");
		}
		if (v <= 0L) {
			throw new ResourceException("供应商ID错误");
		}
		return v;
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

	private static long resolveH5CompanyId(HttpServletRequest request) {
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
}
