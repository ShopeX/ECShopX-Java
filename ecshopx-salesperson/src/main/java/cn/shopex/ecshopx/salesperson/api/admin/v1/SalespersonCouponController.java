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

package cn.shopex.ecshopx.salesperson.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.salesperson.service.SalespersonCouponCreateService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCouponDeleteService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCouponListService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCouponCreateService.CouponItem;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("salespersonAdminV1SalespersonCoupon")
@RequestMapping("/api/v1/salesperson/coupon")
public class SalespersonCouponController {

	private final SalespersonCouponCreateService salespersonCouponCreateService;
	private final SalespersonCouponListService salespersonCouponListService;
	private final SalespersonCouponDeleteService salespersonCouponDeleteService;

	public SalespersonCouponController(
			SalespersonCouponCreateService salespersonCouponCreateService,
			SalespersonCouponListService salespersonCouponListService,
			SalespersonCouponDeleteService salespersonCouponDeleteService) {
		this.salespersonCouponCreateService = salespersonCouponCreateService;
		this.salespersonCouponListService = salespersonCouponListService;
		this.salespersonCouponDeleteService = salespersonCouponDeleteService;
	}

	@Activated(routeAlias = "salesperson.coupon.list")
	@GetMapping(name = "获取导购优惠券列表")
	public ResponseEntity<Map<String, Object>> lists(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "page_size", required = false) String pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Map<String, Object> inner = salespersonCouponListService.lists(companyId, page, pageSize);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	@Activated(routeAlias = "salesperson.coupon.create")
	@PostMapping(name = "添加导购可发放优惠券")
	public ResponseEntity<Map<String, Object>> create(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		String limitCycle = optionalString(merged.get("limit_cycle"));
		String grantPerUserTotal = optionalString(merged.get("grant_per_user_total"));
		String grantTotal = optionalString(merged.get("grant_total"));

		if (!StringUtils.hasText(limitCycle)) {
			throw new BadRequestException("限制周期必填", 422);
		}
		if (!StringUtils.hasText(grantPerUserTotal)) {
			throw new BadRequestException("grant_per_user_total 不能为空", 422);
		}
		if (!StringUtils.hasText(grantTotal)) {
			throw new BadRequestException("grant_total 不能为空", 422);
		}

		List<CouponItem> items = parseCoupons(merged.get("coupons"));
		salespersonCouponCreateService.create(companyId, limitCycle, grantPerUserTotal, grantTotal, items);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	private static ResponseEntity<?> dingo422(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	@Activated(routeAlias = "salesperson.coupon.delete")
	@DeleteMapping(value = "/{id}", name = "删除导购优惠券")
	public ResponseEntity<Map<String, Object>> delete(HttpServletRequest request, @PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		String trimmed = id == null ? "" : id.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("id 不能为空", 422);
		}
		long relId;
		try {
			relId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
		}

		salespersonCouponDeleteService.delete(companyId, relId);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	private static String optionalString(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private List<CouponItem> parseCoupons(Object c) {
		if (c == null) {
			return List.of();
		}
		if (!(c instanceof List<?> list)) {
			throw new BadRequestException("coupons 必须为数组", 422);
		}
		List<CouponItem> out = new ArrayList<>(list.size());
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> map)) {
				throw new BadRequestException("coupons 必须为对象数组", 422);
			}
			Object idObj = map.get("coupon_id");
			if (idObj == null || !StringUtils.hasText(String.valueOf(idObj).trim())) {
				throw new BadRequestException("优惠券id必填", 422);
			}
			long couponId;
			try {
				couponId = idObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(idObj).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("coupon_id 格式错误", 422);
			}
			long sendNum = parseSendNum(map.get("send_num"));
			out.add(new CouponItem(couponId, sendNum));
		}
		return out;
	}

	private static long parseSendNum(Object sendNumObj) {
		if (sendNumObj == null) {
			return 1L;
		}
		if (sendNumObj instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				return 1L;
			}
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("send_num 格式错误", 422);
			}
		}
		if (sendNumObj instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(sendNumObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("send_num 格式错误", 422);
		}
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
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
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}
}
