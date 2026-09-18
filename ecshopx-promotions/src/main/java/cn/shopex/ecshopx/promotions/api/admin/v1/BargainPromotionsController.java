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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.BargainPromotionCreateService;
import cn.shopex.ecshopx.promotions.service.BargainPromotionsDeleteService;
import cn.shopex.ecshopx.promotions.service.BargainPromotionsListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
		notFound = true)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1BargainPromotions")
@RequestMapping("/api/v1/promotions/bargains")
public class BargainPromotionsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final BargainPromotionCreateService bargainPromotionCreateService;
	private final BargainPromotionsListService bargainPromotionsListService;
	private final BargainPromotionsDeleteService bargainPromotionsDeleteService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public BargainPromotionsController(
			BargainPromotionCreateService bargainPromotionCreateService,
			BargainPromotionsListService bargainPromotionsListService,
			BargainPromotionsDeleteService bargainPromotionsDeleteService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.bargainPromotionCreateService = bargainPromotionCreateService;
		this.bargainPromotionsListService = bargainPromotionsListService;
		this.bargainPromotionsDeleteService = bargainPromotionsDeleteService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "Promotions.bargains.add")
	@PostMapping(name = "创建助力")
	public ResponseEntity<ApiResult<Map<String, Object>>> createBargain(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = bargainPromotionCreateService.createBargain(merged, requestLangTag);
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

	@Activated(routeAlias = "Promotions.bargains.list")
	@GetMapping(name = "助力列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBargainList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		int p = page == null ? 1 : page;
		int s = pageSize == null ? 50 : pageSize;
		String itemNameContains = null;
		if (request.getParameterMap().containsKey("item_name")) {
			String in = request.getParameter("item_name");
			if (StringUtils.hasText(in)) {
				itemNameContains = in.trim();
			}
		}
		String titleContains = null;
		if (request.getParameterMap().containsKey("title")
				&& StringUtils.hasText(request.getParameter("item_name"))) {
			titleContains = String.valueOf(request.getParameter("title"));
		}
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				bargainPromotionsListService.getBargainList(
						companyId, itemNameContains, titleContains, p, s, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "Promotions.bargains.detail")
	@GetMapping(value = "/{bargain_id}", name = "助力详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBargainDetail(
			HttpServletRequest request, @PathVariable("bargain_id") String bargainId) {
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		String raw = bargainId == null ? "" : bargainId.trim();
		if (!StringUtils.hasText(raw)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_required", null, locale),
					Map.of("bargain_id", List.of("validation.required")));
		}
		long bargainIdLong;
		try {
			bargainIdLong = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_invalid", null, locale),
					Map.of("bargain_id", List.of("validation.invalid")));
		}
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String acceptLanguage = RequestLangTag.current(langueProperties);
		Map<String, Object> row =
				bargainPromotionsListService.getBargainDetail(companyId, bargainIdLong, acceptLanguage);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "Promotions.bargains.update")
	@PutMapping(value = "/{bargain_id}", name = "更新助力", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateBargain(
			@PathVariable("bargain_id") String bargainId,
			@FlexibleBody Map<String, Object> body,
			HttpServletRequest request) {
		Map<String, Object> merged = mergeInput(request, body);
		merged.put("bargain_id", bargainId);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = bargainPromotionCreateService.updateBargain(merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "Promotions.bargains.terminate")
	@PutMapping(value = "/termination/{bargain_id}", name = "终止助力")
	public ResponseEntity<ApiResult<Map<String, Object>>> terminateBargain(
			@PathVariable("bargain_id") String bargainId) {
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> row = bargainPromotionCreateService.terminateBargain(bargainId, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "Promotions.bargains.update")
	@DeleteMapping(value = "/{bargain_id}", name = "删除助力")
	public ResponseEntity<Void> deleteBargain(
			HttpServletRequest request, @PathVariable("bargain_id") String bargainId) {
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long bargainIdLong = parseBargainIdPathForDelete(bargainId, locale);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		bargainPromotionsDeleteService.deleteBargain(companyId, bargainIdLong, locale);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).build();
	}

	private long parseBargainIdPathForDelete(String bargainId, Locale locale) {
		String raw = bargainId == null ? "" : bargainId.trim();
		if (!StringUtils.hasText(raw)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_required", null, locale),
					Map.of("bargain_id", List.of("validation.required")));
		}
		long id;
		try {
			id = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_invalid", null, locale),
					Map.of("bargain_id", List.of("validation.invalid")));
		}
		if (id < 1L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.delete_bargain_error", null, locale),
					Map.of("bargain_id", List.of("validation.min.numeric")));
		}
		return id;
	}
}
