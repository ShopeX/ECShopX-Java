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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.distribution.service.ShopScreenAdvertisementCreateService;
import cn.shopex.ecshopx.distribution.service.ShopScreenAdvertisementDeleteService;
import cn.shopex.ecshopx.distribution.service.ShopScreenAdvertisementListService;
import cn.shopex.ecshopx.distribution.service.ShopScreenAdvertisementUpdateService;
import cn.shopex.ecshopx.distribution.service.ShopScreenSliderGetService;
import cn.shopex.ecshopx.distribution.service.ShopScreenSliderSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
		notFound = true)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1ShopScreen")
@RequestMapping("/api/v1/shopScreen")
public class ShopScreenController {

	private final ShopScreenAdvertisementCreateService shopScreenAdvertisementCreateService;

	private final ShopScreenAdvertisementUpdateService shopScreenAdvertisementUpdateService;

	private final ShopScreenAdvertisementListService shopScreenAdvertisementListService;

	private final ShopScreenAdvertisementDeleteService shopScreenAdvertisementDeleteService;

	private final ShopScreenSliderSaveService shopScreenSliderSaveService;

	private final ShopScreenSliderGetService shopScreenSliderGetService;

	private final MessageSource messageSource;

	public ShopScreenController(
			ShopScreenAdvertisementCreateService shopScreenAdvertisementCreateService,
			ShopScreenAdvertisementUpdateService shopScreenAdvertisementUpdateService,
			ShopScreenAdvertisementListService shopScreenAdvertisementListService,
			ShopScreenAdvertisementDeleteService shopScreenAdvertisementDeleteService,
			ShopScreenSliderSaveService shopScreenSliderSaveService,
			ShopScreenSliderGetService shopScreenSliderGetService,
			MessageSource messageSource) {
		this.shopScreenAdvertisementCreateService = shopScreenAdvertisementCreateService;
		this.shopScreenAdvertisementUpdateService = shopScreenAdvertisementUpdateService;
		this.shopScreenAdvertisementListService = shopScreenAdvertisementListService;
		this.shopScreenAdvertisementDeleteService = shopScreenAdvertisementDeleteService;
		this.shopScreenSliderSaveService = shopScreenSliderSaveService;
		this.shopScreenSliderGetService = shopScreenSliderGetService;
		this.messageSource = messageSource;
	}

	/**
	 * DINGO Resource 默认响应不含 {@code errors}；携带 {@link ResourceException#getFieldErrors()} 时在此写出
	 * {@code message}、{@code errors}、{@code status_code} 信封（HTTP 200），与 {@link
	 * cn.shopex.ecshopx.companys.api.front.v1.ShopsController} 一致。
	 */
	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<?> handleResourceException(ResourceException ex) {
		Map<String, List<String>> fe = ex.getFieldErrors();
		if (fe != null && !fe.isEmpty()) {
			int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
			return dingoValidationEnvelope(ex.getMessage(), fe, statusCode);
		}
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		if (ex.getEmbeddedBusinessCode() != null) {
			data.put("code", ex.getEmbeddedBusinessCode());
		}
		data.put("status_code", statusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoValidationEnvelope(
			String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		data.put("status_code", statusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@PostMapping(value = "/advertisement", name = "添加广告")
	public ResponseEntity<ApiResult<Map<String, Object>>> addAdvertisement(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeBasicConfig(request, body);
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> data = shopScreenAdvertisementCreateService.addAdvertisement(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private Map<String, Object> mergeInputLikeBasicConfig(HttpServletRequest request, Map<String, Object> body) {
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

	@DeleteMapping(value = "/advertisement/{id}", name = "删除广告")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteAdvertisement(
			HttpServletRequest request, @PathVariable("id") String idRaw) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		String trimmed = idRaw == null ? "" : idRaw.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("广告 id 无效");
		}
		long advertisementId;
		try {
			advertisementId = Long.parseLong(trimmed);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("广告 id 无效");
		}

		shopScreenAdvertisementDeleteService.deleteAdvertisement(companyId, advertisementId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@PutMapping(value = "/advertisement", name = "排序发布撤回")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateAdvertisement(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Locale locale = LocaleContextHolder.getLocale();
		Map<String, Object> merged = mergeInputLikeBasicConfig(request, body);
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		Object raw = merged.get("inputdata");
		if (raw == null || !(raw instanceof List<?> list)) {
			throw new BadRequestException(
					messageSource.getMessage("distribution.shop_screen.advertisement.inputdata_bad_format", null, locale));
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> mm)) {
				throw new BadRequestException(
						messageSource.getMessage(
								"distribution.shop_screen.advertisement.inputdata_bad_format", null, locale));
			}
			items.add(toStringKeyMap(mm));
		}
		if (list.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
		}
		shopScreenAdvertisementUpdateService.updateAdvertisement(companyId, items);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@GetMapping(value = "/advertisement", name = "开屏广告列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAdvertisement(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		long distributorId = parseQueryLongDefaultZero(distributorIdRaw);
		int page = parseQueryIntWithDefault(pageRaw, 1, 1);
		int pageSize = parseQueryIntWithDefault(pageSizeRaw, 20, 1);
		Map<String, Object> data =
				shopScreenAdvertisementListService.getAdvertisement(companyId, distributorId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseQueryLongDefaultZero(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static int parseQueryIntWithDefault(String raw, int defaultValue, int minimumPositive) {
		if (raw == null) {
			return defaultValue;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(t);
			if (v < minimumPositive) {
				return defaultValue;
			}
			return v;
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	@PostMapping(value = "/slider", name = "保存轮播")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveSlider(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeBasicConfig(request, body);
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> data = shopScreenSliderSaveService.saveSlider(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/slider", name = "轮播图")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSlider(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		Locale locale = LocaleContextHolder.getLocale();
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		if (distributorIdRaw == null || !StringUtils.hasText(distributorIdRaw.trim())) {
			fieldErrors.put(
					"distributor_id",
					List.of(
							messageSource.getMessage(
									"distribution.shop_screen.validation.distributor_id_required", null, locale)));
		} else {
			String t = distributorIdRaw.trim();
			if (!t.matches("^-?\\d+$")) {
				fieldErrors.put(
						"distributor_id",
						List.of(
								messageSource.getMessage(
										"distribution.shop_screen.validation.distributor_id_integer", null, locale)));
			} else {
				try {
					long v = Long.parseLong(t);
					if (v < 0L) {
						fieldErrors.put(
								"distributor_id",
								List.of(
										messageSource.getMessage(
												"distribution.shop_screen.validation.distributor_id_min", null, locale)));
					}
				} catch (NumberFormatException ex) {
					fieldErrors.put(
							"distributor_id",
							List.of(
									messageSource.getMessage(
											"distribution.shop_screen.validation.distributor_id_integer", null, locale)));
				}
			}
		}
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage("distribution.shop_screen.params_error", null, locale), fieldErrors);
		}
		long distributorId = Long.parseLong(distributorIdRaw.trim());
		Map<String, Object> data = shopScreenSliderGetService.getSlider(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
