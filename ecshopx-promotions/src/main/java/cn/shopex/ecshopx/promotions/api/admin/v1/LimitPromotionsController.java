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
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.LimitPromotionCancelService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionCreateService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionLimitItemDeleteService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionLimitItemUpdateService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionLimitItemsListService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionUpdateService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionsInfoService;
import cn.shopex.ecshopx.promotions.service.LimitPromotionsListService;
import cn.shopex.ecshopx.promotions.service.LimitSaleItemUploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1LimitPromotions")
@RequestMapping("/api/v1/promotions")
public class LimitPromotionsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final LimitPromotionCreateService limitPromotionCreateService;
	private final LimitPromotionUpdateService limitPromotionUpdateService;
	private final LimitSaleItemUploadService limitSaleItemUploadService;
	private final LimitPromotionLimitItemUpdateService limitPromotionLimitItemUpdateService;
	private final LimitPromotionLimitItemDeleteService limitPromotionLimitItemDeleteService;
	private final LimitPromotionsListService limitPromotionsListService;
	private final LimitPromotionsInfoService limitPromotionsInfoService;
	private final LimitPromotionLimitItemsListService limitPromotionLimitItemsListService;
	private final LimitPromotionCancelService limitPromotionCancelService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public LimitPromotionsController(
			LimitPromotionCreateService limitPromotionCreateService,
			LimitPromotionUpdateService limitPromotionUpdateService,
			LimitSaleItemUploadService limitSaleItemUploadService,
			LimitPromotionLimitItemUpdateService limitPromotionLimitItemUpdateService,
			LimitPromotionLimitItemDeleteService limitPromotionLimitItemDeleteService,
			LimitPromotionsListService limitPromotionsListService,
			LimitPromotionsInfoService limitPromotionsInfoService,
			LimitPromotionLimitItemsListService limitPromotionLimitItemsListService,
			LimitPromotionCancelService limitPromotionCancelService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.limitPromotionCreateService = limitPromotionCreateService;
		this.limitPromotionUpdateService = limitPromotionUpdateService;
		this.limitSaleItemUploadService = limitSaleItemUploadService;
		this.limitPromotionLimitItemUpdateService = limitPromotionLimitItemUpdateService;
		this.limitPromotionLimitItemDeleteService = limitPromotionLimitItemDeleteService;
		this.limitPromotionsListService = limitPromotionsListService;
		this.limitPromotionsInfoService = limitPromotionsInfoService;
		this.limitPromotionLimitItemsListService = limitPromotionLimitItemsListService;
		this.limitPromotionCancelService = limitPromotionCancelService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.limit.list.get")
	@GetMapping(value = "/limit", name = "限购列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "distributor_id", defaultValue = "0") String distributorIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String sourceType = readOperatorTypeFromOperatorJwtMap(jwt);
		long sourceIdFromQuery = parseLeadingLongQueryParam(distributorIdRaw, 0L);
		if (sourceIdFromQuery == 0L) {
			sourceIdFromQuery = readSourceIdFromOperatorJwtMap(jwt);
		}
		int page = parseLeadingIntQueryParam(pageRaw, 1);
		int pageSize = parseLeadingIntQueryParam(pageSizeRaw, 20);
		String langTag = RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				limitPromotionsListService.lists(
						companyId, sourceType, sourceIdFromQuery, status, page, pageSize, langTag);
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

	@Activated(routeAlias = "promotions.limit.error_desc.get")
	@GetMapping(
			value = "/limit_error_desc",
			name = "限购错误下载",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> exportErrorDesc(
			HttpServletRequest request,
			@RequestParam(value = "limit_id", required = false) String limitIdRaw,
			@RequestParam(value = "file_name", required = false) String fileNameRaw) {
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (fileNameRaw == null || !StringUtils.hasText(fileNameRaw.trim())) {
			throw new ResourceException("文件名称不能为空！");
		}
		String fileName = fileNameRaw.trim();
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		byte[] xlsxBytes = limitSaleItemUploadService.exportErrorDesc(companyId, limitIdRaw, fileName, loc);
		Map<String, String> dataPayload = new LinkedHashMap<>();
		dataPayload.put("name", fileName + ".xlsx");
		dataPayload.put(
				"file",
				"data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,"
						+ Base64.getEncoder().encodeToString(xlsxBytes));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", dataPayload);
		return ResponseEntity.ok(body);
	}

	@Activated(routeAlias = "promotions.limit.info.get")
	@GetMapping(value = "/limit/{limitId}", name = "限购详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> info(
			HttpServletRequest request, @PathVariable("limitId") String limitId) {
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long parsedLimitId;
		try {
			if (!StringUtils.hasText(limitId == null ? "" : limitId.trim())) {
				throw new NumberFormatException();
			}
			parsedLimitId = Long.parseLong(limitId.trim());
			if (parsedLimitId <= 0L) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.limit.no_update_data_found", null, loc));
		}
		String langTag = RequestLangTag.current(langueProperties);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		return ResponseEntity.ok(
				ApiResult.ok(limitPromotionsInfoService.info(companyId, parsedLimitId, langTag)));
	}

	@Activated(routeAlias = "promotions.limit.create")
	@PostMapping(value = "/limit", name = "创建限购")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(jwt));
		merged.put("source_id", readSourceIdFromOperatorJwtMap(jwt));
		merged.put("source_type", readOperatorTypeFromOperatorJwtMap(jwt));
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = limitPromotionCreateService.createLimitPromotion(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.limit.update")
	@PutMapping(
			value = "/limit/{limitId}",
			name = "更新限购",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			@PathVariable("limitId") String limitId,
			@FlexibleBody Map<String, Object> body,
			HttpServletRequest request) {
		Map<String, Object> merged = mergeInput(request, body);
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long parsedLimitId;
		try {
			if (!StringUtils.hasText(limitId == null ? "" : limitId.trim())) {
				throw new NumberFormatException();
			}
			parsedLimitId = Long.parseLong(limitId.trim());
			if (parsedLimitId <= 0L) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.limit.no_update_data_found", null, loc));
		}
		merged.put("limit_id", parsedLimitId);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(jwt));
		merged.put("source_id", readSourceIdFromOperatorJwtMap(jwt));
		merged.put("source_type", readOperatorTypeFromOperatorJwtMap(jwt));
		Object lt = merged.get("limit_type");
		if (lt == null || !StringUtils.hasText(Objects.toString(lt, "").trim())) {
			merged.put("limit_type", "global");
		}
		merged.put("total_count", parseIntDefault(merged.get("total_count"), 0));
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag = StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = limitPromotionUpdateService.updateLimitPromotion(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.limit_items.upload")
	@PostMapping(value = "/limit_items/upload", name = "上传限购商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadLimitItems(HttpServletRequest request) {
		MultipartFile file = extractOptionalMultipartFile(request, "file");
		Map<String, Object> data = limitSaleItemUploadService.uploadLimitItems(file);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.limit_items.list.get")
	@GetMapping(value = "/limit_items/{limitId}", name = "限购商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLimitItems(
			HttpServletRequest request,
			@PathVariable("limitId") String limitId,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw) {
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long parsedLimitId = LeadingNumberParser.parseAsLong(limitId != null ? limitId : "");
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		int page = parseLeadingIntQueryParam(pageRaw, 1);
		int pageSize = parseLeadingIntQueryParam(pageSizeRaw, 20);
		return ResponseEntity.ok(
				ApiResult.ok(
						limitPromotionLimitItemsListService.getLimitItems(
								companyId, parsedLimitId, itemBn, page, pageSize, loc)));
	}

	@Activated(routeAlias = "promotions.limit_item.delete")
	@DeleteMapping(
			value = "/limit_items/{limitId}",
			name = "删除限购商品",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<Boolean>>> deleteLimitItem(
			@PathVariable("limitId") String limitId,
			@FlexibleBody(required = false) Map<String, Object> body,
			HttpServletRequest request) {
		Map<String, Object> merged = mergeInput(request, body == null ? Map.of() : body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		merged.put("company_id", companyId);
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long itemId = parseDeleteLimitItemPositiveId(merged, "item_id", loc, "promotions.limit.delete_limit_item_product_id_error");
		long distributorId =
				parseDeleteLimitItemPositiveId(
						merged, "distributor_id", loc, "promotions.limit.delete_limit_item_store_id_error");
		limitPromotionLimitItemDeleteService.deleteLimitItem(companyId, distributorId, itemId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(List.of(true)));
	}

	@Activated(routeAlias = "promotions.limit_item.update")
	@PutMapping(
			value = "/limit_items/{limitId}",
			name = "更新限购数量",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateLimitItem(
			@PathVariable("limitId") String limitId,
			@FlexibleBody Map<String, Object> body,
			HttpServletRequest request) {
		Map<String, Object> merged = mergeInput(request, body);
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long parsedLimitId;
		try {
			if (!StringUtils.hasText(limitId == null ? "" : limitId.trim())) {
				throw new NumberFormatException();
			}
			parsedLimitId = Long.parseLong(limitId.trim());
			if (parsedLimitId <= 0L) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.limit.no_update_data_found", null, loc));
		}
		merged.put("limit_id", parsedLimitId);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request)));
		Map<String, Object> row = limitPromotionLimitItemUpdateService.updateLimitItem(merged);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.limit_items.save")
	@PostMapping(value = "/limit_items_save", name = "保存限购商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<String>>> saveLimitItems(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request)));
		merged.put("use_bound", "normal");
		List<String> ok = limitSaleItemUploadService.saveLimitItems(merged);
		return ResponseEntity.ok(ApiResult.ok(ok));
	}

	@Activated(routeAlias = "promotions.limit.cancel")
	@DeleteMapping(value = "/limit/cancel/{limitId}", name = "取消限购")
	public ResponseEntity<ApiResult<Map<String, Object>>> cancel(
			HttpServletRequest request, @PathVariable("limitId") String limitId) {
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long parsedLimitId;
		try {
			if (!StringUtils.hasText(limitId == null ? "" : limitId.trim())) {
				throw new NumberFormatException();
			}
			parsedLimitId = Long.parseLong(limitId.trim());
			if (parsedLimitId <= 0L) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.cancel_failed", null, loc));
		}
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Map<String, Object> data = limitPromotionCancelService.cancel(parsedLimitId, companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseIntDefault(Object v, int dft) {
		if (v == null) {
			return dft;
		}
		try {
			if (v instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dft;
		}
	}

	private static MultipartFile extractOptionalMultipartFile(HttpServletRequest request, String partName) {
		if (request instanceof MultipartHttpServletRequest mp) {
			return mp.getFile(partName);
		}
		return null;
	}

	private long parseDeleteLimitItemPositiveId(
			Map<String, Object> merged, String fieldKey, Locale loc, String messageKey) {
		if (merged.get(fieldKey) == null
				|| !StringUtils.hasText(Objects.toString(merged.get(fieldKey), "").trim())) {
			throw new ResourceException(messageSource.getMessage(messageKey, null, loc));
		}
		Object v = merged.get(fieldKey);
		try {
			long n = v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v).trim());
			if (n < 1L) {
				throw new ResourceException(messageSource.getMessage(messageKey, null, loc));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new ResourceException(messageSource.getMessage(messageKey, null, loc));
		}
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
