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

package cn.shopex.ecshopx.promotions.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.promotions.service.give.CouponGiveErrorLogListService;
import cn.shopex.ecshopx.promotions.service.give.CouponGiveLogListService;
import cn.shopex.ecshopx.promotions.service.give.PromotionActivityGiveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
		notFound = false)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1GivePromotions")
@RequestMapping("/api/v1/promotions")
public class GivePromotionsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final String SOURCE_FROM_ADMIN = "商城后台发放";

	private final PromotionActivityGiveService promotionActivityGiveService;
	private final CouponGiveLogListService couponGiveLogListService;
	private final CouponGiveErrorLogListService couponGiveErrorLogListService;

	public GivePromotionsController(
			PromotionActivityGiveService promotionActivityGiveService,
			CouponGiveLogListService couponGiveLogListService,
			CouponGiveErrorLogListService couponGiveErrorLogListService) {
		this.promotionActivityGiveService = promotionActivityGiveService;
		this.couponGiveLogListService = couponGiveLogListService;
		this.couponGiveErrorLogListService = couponGiveErrorLogListService;
	}

	@Activated(routeAlias = "promotions.give.create")
	@PostMapping(value = "/activity/give", name = "后台发券")
	public ResponseEntity<ApiResult<Boolean>> give(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long distributorId = parseDistributorId(jwt);
		String operatorType = Objects.toString(jwt.get("operator_type"), "").trim();
		String username = Objects.toString(jwt.get("username"), "");
		String mobile = Objects.toString(jwt.get("mobile"), "");
		String sender;
		if ("staff".equalsIgnoreCase(operatorType)) {
			sender = "员工-" + username + "-" + mobile;
		} else {
			sender = username;
		}
		List<Long> userIds = PromotionActivityGiveService.parseRequiredLongArray(merged.get("userids"), "请选择用户");
		List<Long> couponCardIds =
				PromotionActivityGiveService.parseRequiredLongArray(merged.get("couponsids"), "请选择优惠券");
		promotionActivityGiveService.give(companyId, distributorId, sender, userIds, couponCardIds, SOURCE_FROM_ADMIN);
		return ResponseEntity.ok(ApiResult.ok(Boolean.TRUE));
	}

	@Activated(routeAlias = "promotions.give.list")
	@GetMapping(value = "/activity/give", name = "发券日志")
	public ResponseEntity<ApiResult<Map<String, Object>>> getGiveLog(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long distributorIdFromJwt = parseDistributorId(jwt);
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);
		Map<String, Object> data =
				couponGiveLogListService.getGiveLog(companyId, distributorIdFromJwt, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "promotions.give.info")
	@GetMapping(value = "/activity/give/{id}", name = "发券失败记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> getGiveErrorLog(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		long giveId = LeadingNumberParser.parseAsLong(id);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);
		Map<String, Object> data = couponGiveErrorLogListService.getGiveErrorLog(giveId, companyId, page, pageSize);
		if (isDatapassBlocked(request)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("list");
			if (rows != null) {
				for (Map<String, Object> row : rows) {
					Object mob = row.get("mobile");
					if (mob != null && !"null".equals(Objects.toString(mob, ""))) {
						row.put("mobile", DataMasking.maskMobile(String.valueOf(mob)));
					}
					Object user = row.get("username");
					if (user != null && !"null".equals(Objects.toString(user, ""))) {
						row.put("username", DataMasking.maskTruename(String.valueOf(user)));
					}
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								input.put(k, v[0]);
							}
						});
		if (body != null) {
			input.putAll(body);
		}
		return input;
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static long parseDistributorId(Map<String, Object> jwt) {
		Object v = jwt.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = v.toString().trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parsePositivePagingInt(String raw, int defaultVal) {
		try {
			String s = raw == null ? "" : raw.trim();
			String digits = LeadingNumberParser.parseAsString(s);
			int v = Integer.parseInt(digits);
			return Math.max(1, v);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	private static boolean isDatapassBlocked(HttpServletRequest request) {
		return resolveDatapassBlock(request) == 1;
	}

	private static int resolveDatapassBlock(HttpServletRequest request) {
		if (truthyDatapassToken(request.getHeader("X-Datapass-Block"))) {
			return 1;
		}
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Boolean b && b) {
			return 1;
		}
		if (attr != null && truthyDatapassToken(attr.toString())) {
			return 1;
		}
		return truthyDatapassToken(request.getParameter("x-datapass-block")) ? 1 : 0;
	}

	private static boolean truthyDatapassToken(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}
}

