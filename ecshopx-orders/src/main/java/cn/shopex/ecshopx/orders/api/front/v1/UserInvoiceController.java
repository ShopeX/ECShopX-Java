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

package cn.shopex.ecshopx.orders.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.userinvoice.UserInvoiceCreateService;
import cn.shopex.ecshopx.orders.service.front.userinvoice.UserInvoiceProtocolService;
import cn.shopex.ecshopx.orders.service.front.userinvoice.UserInvoiceResendEmailService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceDetailService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceListService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceSettingService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@RestController("ordersFrontV1UserInvoice")
@RequestMapping("/api/v1/h5app/wxapp/order/invoice")
public class UserInvoiceController {

	private final UserInvoiceCreateService userInvoiceCreateService;
	private final UserInvoiceResendEmailService userInvoiceResendEmailService;
	private final OrderInvoiceSettingService orderInvoiceSettingService;
	private final OrderInvoiceUpdateService orderInvoiceUpdateService;
	private final OrderInvoiceDetailService orderInvoiceDetailService;
	private final OrderInvoiceListService orderInvoiceListService;
	private final UserInvoiceProtocolService userInvoiceProtocolService;

	public UserInvoiceController(
			UserInvoiceCreateService userInvoiceCreateService,
			UserInvoiceResendEmailService userInvoiceResendEmailService,
			OrderInvoiceSettingService orderInvoiceSettingService,
			OrderInvoiceUpdateService orderInvoiceUpdateService,
			OrderInvoiceDetailService orderInvoiceDetailService,
			OrderInvoiceListService orderInvoiceListService,
			UserInvoiceProtocolService userInvoiceProtocolService) {
		this.userInvoiceCreateService = userInvoiceCreateService;
		this.userInvoiceResendEmailService = userInvoiceResendEmailService;
		this.orderInvoiceSettingService = orderInvoiceSettingService;
		this.orderInvoiceUpdateService = orderInvoiceUpdateService;
		this.orderInvoiceDetailService = orderInvoiceDetailService;
		this.orderInvoiceListService = orderInvoiceListService;
		this.userInvoiceProtocolService = userInvoiceProtocolService;
	}

	@FrontAuth
	@PostMapping(
			value = "/apply",
			name = "申请发票",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> createInvoice(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object payload = userInvoiceCreateService.createInvoice(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontAuth
	@PostMapping(
			value = "/update",
			name = "更新发票申请",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> updateInvoice(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		AuthIds ids = readAuthIds(auth);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object payload = orderInvoiceUpdateService.updateInvoiceForFrontMember(ids.userId(), ids.companyId(), merged);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontAuth
	@GetMapping(value = "/list", name = "发票申请列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserInvoiceList(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		AuthIds ids = readAuthIds(auth);
		Map<String, Object> payload =
				orderInvoiceListService.getUserInvoiceList(ids.companyId(), ids.userId(), request);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontAuth
	@GetMapping(value = "/info/{id}", name = "发票申请详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserInvoiceDetail(
			HttpServletRequest request, @PathVariable("id") String id) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		AuthIds ids = readAuthIds(auth);
		Map<String, Object> payload = orderInvoiceDetailService.getUserInvoiceDetail(ids.companyId(), id);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontAuth
	@PostMapping(
			value = "/resend",
			name = "重发邮箱",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> resendInvoiceEmail(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		AuthIds ids = readAuthIds(auth);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, Object> serviceResult =
				userInvoiceResendEmailService.resendInvoiceEmail(ids.userId(), ids.companyId(), merged);
		return ResponseEntity.ok(ApiResult.ok(serviceResult));
	}

	private static Long parseLongOrNull(Object v) {
		if (v == null) {
			return null;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			String s = String.valueOf(v).trim();
			if (s.isEmpty()) {
				return null;
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private record AuthIds(long userId, long companyId) {}

	private static AuthIds readAuthIds(Map<String, Object> auth) {
		Long companyId = parseLongOrNull(auth.get("company_id"));
		Long userId = parseLongOrNull(auth.get("user_id"));
		if (companyId == null || userId == null) {
			throw new ResourceException("参数错误");
		}
		return new AuthIds(userId, companyId);
	}

	@FrontAuth
	@GetMapping(value = "/setting", name = "开票配置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getInvoiceSetting(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Long companyId = parseLongOrNull(auth.get("company_id"));
		if (companyId == null) {
			throw new ResourceException("参数错误");
		}
		Object payload = orderInvoiceSettingService.getInvoiceSetting(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontAuth
	@PostMapping(
			value = "/setting",
			name = "设置开票配置",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Boolean>> setInvoiceSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Long companyId = parseLongOrNull(auth.get("company_id"));
		if (companyId == null) {
			throw new ResourceException("参数错误");
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		orderInvoiceSettingService.setFrontMemberInvoiceSetting(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Boolean.TRUE));
	}

	@FrontNoAuth
	@GetMapping(value = "/protocol", name = "发票协议", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getInvoiceProtocol(HttpServletRequest request) {
		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("未授权");
		}
		Object payload = userInvoiceProtocolService.getInvoiceProtocol(companyId);
		if (payload instanceof Map<?, ?>) {
			return ResponseEntity.ok(ApiResult.ok(payload));
		}
		if (payload instanceof List<?>) {
			return ResponseEntity.ok(ApiResult.ok(payload));
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("protocol", List.of())));
	}

	private Map<String, Object> resolveFrontNoAuthClaims(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth instanceof Map<?, ?> authRaw) {
			return toStringKeyMap(authRaw);
		}
		rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawAuth instanceof Map<?, ?> authRaw2) {
			return toStringKeyMap(authRaw2);
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		auth.put("user_id", 0L);
		return auth;
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> raw) {
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
