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
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartAddService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartCheckStatusService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartCountService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartDataListService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartDeleteService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("employeepurchaseCartFrontV1")
@RequestMapping("/api/v1/h5app")
public class CartController {

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final EmployeePurchaseCartAddService employeePurchaseCartAddService;
	private final EmployeePurchaseCartCheckStatusService employeePurchaseCartCheckStatusService;
	private final EmployeePurchaseCartDataListService employeePurchaseCartDataListService;
	private final EmployeePurchaseCartCountService employeePurchaseCartCountService;
	private final EmployeePurchaseCartDeleteService employeePurchaseCartDeleteService;

	public CartController(
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			EmployeePurchaseCartAddService employeePurchaseCartAddService,
			EmployeePurchaseCartCheckStatusService employeePurchaseCartCheckStatusService,
			EmployeePurchaseCartDataListService employeePurchaseCartDataListService,
			EmployeePurchaseCartCountService employeePurchaseCartCountService,
			EmployeePurchaseCartDeleteService employeePurchaseCartDeleteService) {
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.employeePurchaseCartAddService = employeePurchaseCartAddService;
		this.employeePurchaseCartCheckStatusService = employeePurchaseCartCheckStatusService;
		this.employeePurchaseCartDataListService = employeePurchaseCartDataListService;
		this.employeePurchaseCartCountService = employeePurchaseCartCountService;
		this.employeePurchaseCartDeleteService = employeePurchaseCartDeleteService;
	}

	@PostMapping("/wxapp/employeepurchase/cart")
	public ResponseEntity<ApiResult<Object>> cartDataAdd(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);

		long distributorId = parseLongDefault(merged.get("distributor_id"), 0L);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", userId);
		filter.put("enterprise_id", merged.get("enterprise_id"));
		filter.put("activity_id", merged.get("activity_id"));
		Object itemRaw = merged.get("item_id");
		if (itemRaw != null) {
			filter.put("item_id", itemRaw);
		}
		filter.put("shop_id", distributorId);

		Optional<Long> validCartId = parseValidPositiveLong(merged.get("cart_id"));
		validCartId.ifPresent(cid -> filter.put("cart_id", cid));

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("cart_type", defaultString(merged.get("cart_type"), "cart"));
		params.put("shop_type", defaultString(merged.get("shop_type"), "distributor"));
		params.put("shop_id", distributorId);
		validCartId.ifPresent(cid -> params.put("cart_id", cid));
		params.put("num", parseNumForCart(merged.get("num")));
		params.put("is_checked", parseIsCheckedForCart(merged.get("is_checked")));
		boolean isAccumulate = parseIsAccumulate(merged.get("is_accumulate"));

		Object result =
				employeePurchaseCartAddService.addCartData(
						companyId, userId, filter, params, isAccumulate);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object raw = claims.get("company_id");
		if (raw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long v = Long.parseLong(raw.toString().trim());
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
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

	private static long parseLongDefault(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Optional<Long> parseValidPositiveLong(Object v) {
		if (v == null) {
			return Optional.empty();
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return x > 0L ? Optional.of(x) : Optional.empty();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return Optional.empty();
		}
		try {
			long x = Long.parseLong(s);
			return x > 0L ? Optional.of(x) : Optional.empty();
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static long requirePositiveLongResource(Object raw, String message) {
		if (raw == null) {
			throw new ResourceException(message);
		}
		if (raw instanceof Number n) {
			if (n.longValue() <= 0L) {
				throw new ResourceException(message);
			}
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(message);
		}
		try {
			long x = Long.parseLong(s);
			if (x <= 0L) {
				throw new ResourceException(message);
			}
			return x;
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
	}

	private static boolean parseIsCheckedForUpdateCartCheckStatus(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		if (s.isEmpty() || "0".equals(s) || "false".equals(s)) {
			return false;
		}
		return true;
	}

	private static String defaultString(Object raw, String def) {
		if (raw == null) {
			return def;
		}
		String s = raw.toString().trim();
		return s.isEmpty() ? def : s;
	}

	private static long parseNumForCart(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Boolean b) {
			return b ? 1L : 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			if (s.indexOf('.') >= 0) {
				return 0L;
			}
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Boolean parseIsCheckedForCart(Object v) {
		if (v == null) {
			return Boolean.TRUE;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		String lower = s.toLowerCase(Locale.ROOT);
		if ("false".equals(lower)
				|| "0".equals(lower)
				|| "no".equals(lower)
				|| "off".equals(lower)
				|| s.isEmpty()) {
			return Boolean.FALSE;
		}
		if ("true".equals(lower)
				|| "1".equals(lower)
				|| "yes".equals(lower)
				|| "on".equals(lower)) {
			return Boolean.TRUE;
		}
		return Boolean.TRUE;
	}

	private static boolean parseIsAccumulate(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n && n.longValue() == 0L) {
			return false;
		}
		String s = v.toString().trim();
		if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
			return false;
		}
		return true;
	}

	@PutMapping("/wxapp/employeepurchase/cart")
	public ResponseEntity<ApiResult<Object>> updateCartData(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", userId);
		filter.put("enterprise_id", merged.get("enterprise_id"));
		filter.put("activity_id", merged.get("activity_id"));

		parseValidPositiveLong(merged.get("cart_id")).ifPresent(cid -> filter.put("cart_id", cid));
		parseValidPositiveLong(merged.get("item_id")).ifPresent(iid -> filter.put("item_id", iid));

		if (merged.containsKey("shop_id")) {
			filter.put("shop_id", parseLongDefault(merged.get("shop_id"), 0L));
		}
		if (merged.containsKey("shop_type")) {
			filter.put("shop_type", merged.get("shop_type"));
		}

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("num", parseNumForCart(merged.get("num")));
		params.put("is_checked", parseIsCheckedForCart(merged.get("is_checked")));
		if (merged.containsKey("shop_id")) {
			params.put("shop_id", merged.get("shop_id"));
		}
		if (merged.containsKey("shop_type")) {
			params.put("shop_type", merged.get("shop_type"));
		}

		Map<String, Object> result =
				employeePurchaseCartAddService.updateCartData(companyId, userId, filter, params);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@PutMapping("/wxapp/employeepurchase/cart/checkstatus")
	public ResponseEntity<ApiResult<Object>> updateCartCheckStatus(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);

		long cartId =
				parseValidPositiveLong(merged.get("cart_id"))
						.orElseThrow(() -> new ResourceException("购物车ID必填"));
		long enterpriseId =
				requirePositiveLongResource(merged.get("enterprise_id"), "企业ID必填");
		long activityId =
				requirePositiveLongResource(merged.get("activity_id"), "活动ID必填");

		boolean isChecked = parseIsCheckedForUpdateCartCheckStatus(merged.get("is_checked"));
		int rows =
				employeePurchaseCartCheckStatusService.updateCartCheckStatus(
						companyId, userId, enterpriseId, activityId, cartId, isChecked);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", rows);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employeepurchase/cart")
	public ResponseEntity<ApiResult<Object>> getCartDataList(
			HttpServletRequest request,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long enterpriseId = requirePositiveLongResource(enterpriseIdRaw, "企业ID必填");
		long activityId = requirePositiveLongResource(activityIdRaw, "活动ID必填");
		Map<String, Object> result =
				employeePurchaseCartDataListService.getCartDataList(companyId, userId, enterpriseId, activityId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@GetMapping("/wxapp/employeepurchase/cartcount")
	public ResponseEntity<ApiResult<Object>> getCartItemCount(
			HttpServletRequest request,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long enterpriseId = requirePositiveLongResource(enterpriseIdRaw, "企业ID必填");
		long activityId = requirePositiveLongResource(activityIdRaw, "活动ID必填");
		Map<String, Object> data =
				employeePurchaseCartCountService.countCart(companyId, userId, enterpriseId, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DeleteMapping("/wxapp/employeepurchase/cart")
	public ResponseEntity<ApiResult<Object>> delCartData(
			HttpServletRequest request,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "cart_id", required = false) String cartIdRaw,
			@RequestParam(value = "item_id", required = false) String itemIdRaw) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long enterpriseId = requirePositiveLongResource(enterpriseIdRaw, "企业ID必填");
		long activityId = requirePositiveLongResource(activityIdRaw, "活动ID必填");
		Optional<Long> cartIdOpt = parseValidPositiveLong(cartIdRaw);
		Optional<Long> itemIdOpt = parseValidPositiveLong(itemIdRaw);

		employeePurchaseCartDeleteService.deleteByFilter(
				companyId, userId, enterpriseId, activityId, cartIdOpt, itemIdOpt);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
