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

package cn.shopex.ecshopx.employeepurchase.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.employeepurchase.dto.front.UpdateEmployeePurchaseOrderReceiverRequest;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseOrderUpdateReceiverService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("employeepurchaseOrderFrontV1")
@RequestMapping("/api/v1/h5app")
public class OrderController {

	private static final Pattern ZIP_6 = Pattern.compile("^\\d{6}$");

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final EmployeePurchaseOrderUpdateReceiverService employeePurchaseOrderUpdateReceiverService;
	private final Validator validator;

	public OrderController(
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			EmployeePurchaseOrderUpdateReceiverService employeePurchaseOrderUpdateReceiverService,
			Validator validator) {
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.employeePurchaseOrderUpdateReceiverService = employeePurchaseOrderUpdateReceiverService;
		this.validator = validator;
	}

	@PutMapping("/wxapp/employeepurchase/order/receiver")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateOrderReceiver(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged = whitelistReceiverKeys(merged);

		Object zipRaw = merged.get("receiver_zip");
		String zipStr = zipRaw == null ? "" : zipRaw.toString().trim();
		if (!ZIP_6.matcher(zipStr).matches()) {
			merged.put("receiver_zip", "000000");
		}

		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);

		Long orderId = parseOrderId(merged.get("order_id"));

		UpdateEmployeePurchaseOrderReceiverRequest req = new UpdateEmployeePurchaseOrderReceiverRequest();
		req.setOrderId(orderId);
		req.setReceiverName(stringVal(merged.get("receiver_name")));
		req.setReceiverMobile(stringVal(merged.get("receiver_mobile")));
		req.setReceiverState(stringVal(merged.get("receiver_state")));
		req.setReceiverCity(stringVal(merged.get("receiver_city")));
		req.setReceiverDistrict(stringVal(merged.get("receiver_district")));
		req.setReceiverAddress(stringVal(merged.get("receiver_address")));
		req.setReceiverZip(stringVal(merged.get("receiver_zip")));

		Set<ConstraintViolation<UpdateEmployeePurchaseOrderReceiverRequest>> violations = validator.validate(req);
		if (!violations.isEmpty()) {
			throw new ResourceException(firstReceiverFieldViolationMessage(violations));
		}

		Map<String, Object> result =
				employeePurchaseOrderUpdateReceiverService.updateReceiver(companyId, userId, req);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static Map<String, Object> whitelistReceiverKeys(Map<String, Object> in) {
		String[] keys = {
			"order_id",
			"receiver_name",
			"receiver_mobile",
			"receiver_state",
			"receiver_city",
			"receiver_district",
			"receiver_address",
			"receiver_zip"
		};
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String k : keys) {
			if (in.containsKey(k)) {
				out.put(k, in.get(k));
			}
		}
		return out;
	}

	private static String stringVal(Object v) {
		return v == null ? null : v.toString();
	}

	private static Long parseOrderId(Object v) {
		if (v == null) {
			throw new ResourceException("请填写订单号");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException("请填写订单号");
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new ResourceException("请填写订单号");
			}
		}
		throw new ResourceException("请填写订单号");
	}

	private static String firstReceiverFieldViolationMessage(
			Set<ConstraintViolation<UpdateEmployeePurchaseOrderReceiverRequest>> violations) {
		List<String> order =
				List.of(
						"receiverName",
						"receiverMobile",
						"receiverState",
						"receiverCity",
						"receiverDistrict",
						"receiverAddress");
		return violations.stream()
				.min(
						Comparator.<ConstraintViolation<UpdateEmployeePurchaseOrderReceiverRequest>>comparingInt(
										cv -> {
											String path = cv.getPropertyPath().toString();
											int idx = order.indexOf(path);
											return idx >= 0 ? idx : Integer.MAX_VALUE;
										})
								.thenComparing(
										cv ->
												cv.getConstraintDescriptor()
														.getAnnotation()
														.annotationType()
														.getSimpleName()))
				.map(ConstraintViolation::getMessage)
				.orElse("参数错误");
	}

	private static Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase(Locale.ROOT).contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								m.put(k, v[0]);
							}
						});
		return m;
	}

	private static boolean isAccountDisabled(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object raw = claims.get("company_id");
		if (raw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long val = Long.parseLong(raw.toString().trim());
			if (val <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return val;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !authUserIdTruthy(claimUid)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long userId = parseLongFlexible(claimUid, 0L);
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return userId;
	}

	private static boolean authUserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
