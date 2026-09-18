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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.SpecificCrowdDiscountCreateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1SpecificCrowdDiscount")
@RequestMapping("/api/v1/specific/crowd")
public class SpecificCrowdDiscountController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final SpecificCrowdDiscountCreateService specificCrowdDiscountCreateService;
	private final LangueProperties langueProperties;

	public SpecificCrowdDiscountController(
			SpecificCrowdDiscountCreateService specificCrowdDiscountCreateService,
			LangueProperties langueProperties) {
		this.specificCrowdDiscountCreateService = specificCrowdDiscountCreateService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "specific.crowd.discount.add")
	@PostMapping(value = "/discount", name = "创建定向促销")
	public ResponseEntity<ApiResult<Map<String, Object>>> createSpecificCrowdDiscount(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);

		String countryHeader = request.getHeader("country_code");
		String countryTrimmed = countryHeader == null ? "" : countryHeader.trim();
		String resolvedLang = RequestLangTag.current(langueProperties);
		String tag =
				StringUtils.hasText(countryTrimmed)
						? countryTrimmed.replace('_', '-')
						: (StringUtils.hasText(resolvedLang) ? resolvedLang : "zh-CN");
		Locale locale = Locale.forLanguageTag(tag);
		if (!StringUtils.hasText(locale.getLanguage())) {
			locale = Locale.SIMPLIFIED_CHINESE;
		}

		Map<String, Object> row = specificCrowdDiscountCreateService.createSpecificCrowdDiscount(merged, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
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

	@Activated(routeAlias = "specific.crowd.discount.update")
	@PutMapping(value = "/discount", name = "更新定向促销")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSpecificCrowdDiscount(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);

		String countryHeader = request.getHeader("country_code");
		String countryTrimmed = countryHeader == null ? "" : countryHeader.trim();
		String resolvedLang = RequestLangTag.current(langueProperties);
		String tag =
				StringUtils.hasText(countryTrimmed)
						? countryTrimmed.replace('_', '-')
						: (StringUtils.hasText(resolvedLang) ? resolvedLang : "zh-CN");
		Locale locale = Locale.forLanguageTag(tag);
		if (!StringUtils.hasText(locale.getLanguage())) {
			locale = Locale.SIMPLIFIED_CHINESE;
		}

		Map<String, Object> row = specificCrowdDiscountCreateService.updateSpecificCrowdDiscount(merged, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "specific.crowd.discount.list")
	@GetMapping(value = "/discountList", name = "定向促销列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSpecificCrowdDiscountList(
			HttpServletRequest request,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "specific_id", required = false) String specificId,
			@RequestParam(value = "page", required = false, defaultValue = "1") String page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "30") String pageSize) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> data =
				specificCrowdDiscountCreateService.getSpecificCrowdDiscountList(
						companyId, status, specificId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "specific.crowd.discount.info")
	@GetMapping(value = "/discountInfo", name = "定向促销详情")
	public ResponseEntity<ApiResult<Object>> getSpecificCrowdDiscountInfo(
			HttpServletRequest request, @RequestParam(value = "id", required = false) String id) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Object payload = specificCrowdDiscountCreateService.getSpecificCrowdDiscountInfo(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DataPass
	@Activated(routeAlias = "specific.crowd.discount.loglist")
	@GetMapping(value = "/discountLogList", name = "定向促销日志")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSpecificcrowddiscountLogList(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityId,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "page", required = false, defaultValue = "1") String page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "-1") String pageSize) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		boolean datapassBlocked = (resolveDatapassBlock(request) == 1);
		Map<String, Object> data =
				specificCrowdDiscountCreateService.getSpecificcrowddiscountLogList(
						companyId,
						activityId,
						mobile,
						orderId,
						timeStartBegin,
						timeStartEnd,
						page,
						pageSize,
						datapassBlocked);
		return ResponseEntity.ok(ApiResult.ok(data));
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
