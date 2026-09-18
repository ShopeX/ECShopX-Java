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
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.PackagePromotionCancelService;
import cn.shopex.ecshopx.promotions.service.PackagePromotionCreateService;
import cn.shopex.ecshopx.promotions.service.PackagePromotionInfoService;
import cn.shopex.ecshopx.promotions.service.PackagePromotionsListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("promotionsAdminV1PackagePromotions")
@RequestMapping("/api/v1/promotions/package")
public class PackagePromotionsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PackagePromotionCreateService packagePromotionCreateService;
	private final PackagePromotionsListService packagePromotionsListService;
	private final PackagePromotionInfoService packagePromotionInfoService;
	private final PackagePromotionCancelService packagePromotionCancelService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public PackagePromotionsController(
			PackagePromotionCreateService packagePromotionCreateService,
			PackagePromotionsListService packagePromotionsListService,
			PackagePromotionInfoService packagePromotionInfoService,
			PackagePromotionCancelService packagePromotionCancelService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.packagePromotionCreateService = packagePromotionCreateService;
		this.packagePromotionsListService = packagePromotionsListService;
		this.packagePromotionInfoService = packagePromotionInfoService;
		this.packagePromotionCancelService = packagePromotionCancelService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.package.list.get")
	@GetMapping(name = "组合商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "distributor_id", defaultValue = "0") String distributorIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String operatorType = readOperatorTypeFromOperatorJwtMap(jwt);
		long distributorIdFromQuery = parseLeadingLongQueryParam(distributorIdRaw, 0L);
		if (distributorIdFromQuery == 0L) {
			distributorIdFromQuery = readSourceIdFromOperatorJwtMap(jwt);
		}
		int page = parseLeadingIntQueryParam(pageRaw, 1);
		int pageSize = parseLeadingIntQueryParam(pageSizeRaw, 10);
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				packagePromotionsListService.lists(
						companyId, operatorType, distributorIdFromQuery, status, page, pageSize, langTag);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static int parseLeadingIntQueryParam(String raw, int defaultWhenBlank) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultWhenBlank;
		}
		long v = LeadingNumberParser.parseAsLong(raw.trim());
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}

	private static long parseLeadingLongQueryParam(String raw, long defaultWhenBlank) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultWhenBlank;
		}
		return LeadingNumberParser.parseAsLong(raw.trim());
	}

	@Activated(routeAlias = "promotions.package.info.get")
	@GetMapping(value = "/{packageId}", name = "组合商品详情")
	public ResponseEntity<ApiResult<Object>> info(
			HttpServletRequest request, @PathVariable("packageId") String packageId) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Object body = packagePromotionInfoService.info(companyId, packageId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "promotions.package.create")
	@PostMapping(name = "创建组合商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(jwt));
		merged.put("source_id", readSourceIdFromOperatorJwtMap(jwt));
		merged.put("source_type", readOperatorTypeFromOperatorJwtMap(jwt));
		String countryCodeTrimmed = java.util.Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = packagePromotionCreateService.create(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.package.update")
	@PutMapping(value = "/{packageId}", name = "更新组合商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("packageId") String packageId,
			@FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(jwt));
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = packagePromotionCreateService.update(packageId, merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.package.cancel")
	@DeleteMapping(value = "/cancel/{packageId}", name = "取消组合商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> cancel(
			HttpServletRequest request, @PathVariable("packageId") String packageId) {
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long parsedPackageId;
		try {
			if (!StringUtils.hasText(packageId == null ? "" : packageId.trim())) {
				throw new NumberFormatException();
			}
			parsedPackageId = Long.parseLong(packageId.trim());
			if (parsedPackageId <= 0L) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.cancel_failed", null, loc));
		}
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Map<String, Object> data = packagePromotionCancelService.cancel(parsedPackageId, companyId);
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

	private static long readSourceIdFromOperatorJwtMap(Map<String, Object> ud) {
		Object v = ud.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String readOperatorTypeFromOperatorJwtMap(Map<String, Object> ud) {
		Object v = ud.get("operator_type");
		if (v == null) {
			return "admin";
		}
		String s = v.toString().trim();
		return StringUtils.hasText(s) ? s : "admin";
	}
}
