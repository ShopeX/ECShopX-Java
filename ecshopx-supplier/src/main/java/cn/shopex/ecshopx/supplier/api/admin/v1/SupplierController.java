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
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.supplier.dto.admin.v1.SupplierRegisterRequest;
import cn.shopex.ecshopx.supplier.service.SupplierCheckService;
import cn.shopex.ecshopx.supplier.service.SupplierInfoService;
import cn.shopex.ecshopx.supplier.service.SupplierListService;
import cn.shopex.ecshopx.supplier.service.SupplierRegisterService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("supplierAdminV1Supplier")
@RequestMapping("/api/v1/supplier")
public class SupplierController {

	private final SupplierRegisterService supplierRegisterService;
	private final SupplierCheckService supplierCheckService;
	private final SupplierInfoService supplierInfoService;
	private final SupplierListService supplierListService;
	private final LangueProperties langueProperties;

	public SupplierController(
			SupplierRegisterService supplierRegisterService,
			SupplierCheckService supplierCheckService,
			SupplierInfoService supplierInfoService,
			SupplierListService supplierListService,
			LangueProperties langueProperties) {
		this.supplierRegisterService = supplierRegisterService;
		this.supplierCheckService = supplierCheckService;
		this.supplierInfoService = supplierInfoService;
		this.supplierListService = supplierListService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "supplier.register")
	@PostMapping(value = "/register", name = "供应商入驻")
	public ResponseEntity<ApiResult<Map<String, Object>>> register(
			HttpServletRequest httpRequest, @FlexibleBody SupplierRegisterRequest body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		long operatorId = parsePositiveLongClaim(jwt, "operator_id", "operator_id 无效");
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data = supplierRegisterService.register(body, companyId, operatorId, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	@Activated(routeAlias = "supplier.get_supplier_info")
	@GetMapping(value = "/get_supplier_info", name = "查询供应商信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSupplierInfo(HttpServletRequest httpRequest) {
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		long operatorId = parsePositiveLongClaim(jwt, "operator_id", "operator_id 无效");
		Map<String, Object> data = supplierInfoService.getSupplierInfo(companyId, operatorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "supplier.get_supplier_list")
	@GetMapping(value = "/get_supplier_list", name = "查询供应商列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSupplierList(
			HttpServletRequest httpRequest,
			@RequestParam(name = "is_check", required = false) String isCheck,
			@RequestParam(name = "supplier_name", required = false) String supplierName,
			@RequestParam(name = "mobile", required = false) String mobile,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw) {
		Object raw = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		String supplierForService = null;
		if (supplierName != null) {
			String t = supplierName.trim();
			supplierForService = t.isEmpty() ? null : t;
		}
		String mobileForService = null;
		if (mobile != null) {
			String t = mobile.trim();
			mobileForService = t.isEmpty() ? null : t;
		}

		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data = supplierListService.getSupplierList(
				companyId, isCheck, supplierForService, mobileForService, pageRaw, pageSizeRaw, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "supplier.check_supplier")
	@PostMapping(value = "/check_supplier", name = "审核供应商")
	public ResponseEntity<ApiResult<Object>> checkSupplier(
			HttpServletRequest httpRequest, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawId = mergeInput(httpRequest, body, "id");
		Object rawIsCheck = mergeInput(httpRequest, body, "is_check");
		Object rawAuditRemark = mergeInput(httpRequest, body, "audit_remark");
		mergeInput(httpRequest, body, "wx_openid");

		long supplierId = parseLongDefaultZero(rawId);
		long isCheck = parseLongDefaultZero(rawIsCheck);
		String auditRemark = normalizeAuditRemark(rawAuditRemark);

		Object rawJwt = httpRequest.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		Object data = supplierCheckService.checkSupplier(companyId, supplierId, isCheck, auditRemark);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Object mergeInput(HttpServletRequest httpRequest, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key)) {
			return body.get(key);
		}
		return httpRequest.getParameter(key);
	}

	private static long parseLongDefaultZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException ex) {
				return 0L;
			}
		}
		String t = String.valueOf(raw).trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static String normalizeAuditRemark(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s.trim();
		}
		return String.valueOf(raw).trim();
	}
}
