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

package cn.shopex.ecshopx.theme.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import cn.shopex.ecshopx.theme.service.PagesTemplateFrontDetailService;
import cn.shopex.ecshopx.theme.service.PagesTemplateFrontShopDetailService;
import cn.shopex.ecshopx.theme.service.PagesTemplateSetGetInfoService;
import cn.shopex.ecshopx.theme.service.PagesTemplateWidgetItemsService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateFrontDetailQuery;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateFrontShopDetailQuery;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateWidgetItemsQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RequiredArgsConstructor
@RestController("themeFrontV1PagesTemplate")
@RequestMapping("/api/v1/h5app/wxapp/pagestemplate")
public class PagesTemplateController {

	private final PagesTemplateSetGetInfoService pagesTemplateSetGetInfoService;
	private final PagesTemplateFrontDetailService pagesTemplateFrontDetailService;
	private final PagesTemplateFrontShopDetailService pagesTemplateFrontShopDetailService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final LangueProperties langueProperties;
	private final TdkGivenSaveService tdkGivenSaveService;
	private final PagesTemplateWidgetItemsService pagesTemplateWidgetItemsService;

	@GetMapping(value = "/detail", name = "模板详情")
	public ApiResult<Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(name = "regionauth_id", required = false, defaultValue = "0") String regionauthIdParam,
			@RequestParam(name = "weapp_pages", required = false, defaultValue = "index") String weappPages,
			@RequestParam(name = "template_name", required = false) String templateName,
			@RequestParam(name = "version", required = false, defaultValue = "v1.0.2") String version,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageParam,
			@RequestParam(name = "page_size", required = false, defaultValue = "50") String pageSizeParam,
			@RequestParam(name = "weapp_setting_id", required = false) String weappSettingIdParam,
			@RequestParam(name = "goods_grid_tab_id", required = false) String goodsGridTabIdParam,
			@RequestParam(name = "pages_template_id", required = false, defaultValue = "0") String pagesTemplateIdParam,
			@RequestParam(name = "e_activity_id", required = false, defaultValue = "0") String eActivityIdParam) {
		long companyId = parseCompanyIdForValidator(request);
		if (!StringUtils.hasText(templateName)) {
			throw new BadRequestException("缺少模板展示类型", 422);
		}
		int distributorId = parseDistributorIdQuery(distributorIdParam);
		int page = parsePositiveIntWithDefault(pageParam, 1);
		int pageSize = parsePositiveIntWithDefault(pageSizeParam, 50);
		if (page < 1) {
			page = 1;
		}
		if (pageSize < 1) {
			pageSize = 1;
		}
		PagesTemplateFrontDetailQuery query =
				new PagesTemplateFrontDetailQuery(
						companyId,
						parseH5UserId(request),
						parseQueryLongDefaultZero(regionauthIdParam),
						distributorId,
						weappPages,
						templateName.trim(),
						version,
						page,
						pageSize,
						parseOptionalLongParam(weappSettingIdParam),
						parseOptionalLongParam(goodsGridTabIdParam),
						parseQueryLongDefaultZero(pagesTemplateIdParam),
						parseQueryLongDefaultZero(eActivityIdParam));
		Object body = pagesTemplateFrontDetailService.detail(query, RequestLangTag.current(langueProperties));
		if (body instanceof List<?> list && list.isEmpty()) {
			return ApiResult.ok(Collections.emptyList());
		}
		if (body instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> typedMap = (Map<String, Object>) body;
			return ApiResult.ok(typedMap);
		}
		return ApiResult.ok(body);
	}

	@GetMapping(value = "/widget/items", name = "模板组件商品")
	public ResponseEntity<Map<String, Object>> getWidgetItems(HttpServletRequest request) {
		long companyId = requireH5CompanyIdOrUnauthorized(request);
		long userId = parseH5UserId(request);
		String acceptLanguage = RequestLangTag.current(langueProperties);
		PagesTemplateWidgetItemsQuery q = PagesTemplateWidgetItemsQuery.fromH5FrontHttpServletRequest(request);
		Map<String, Object> body =
				pagesTemplateWidgetItemsService.getWidgetItems(companyId, acceptLanguage, q, Long.valueOf(userId));
		return ResponseEntity.ok(body);
	}

	@GetMapping(value = "/shopDetail", name = "店铺模板详情")
	public ApiResult<Map<String, Object>> shopDetail(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(name = "weapp_pages", required = false, defaultValue = "index") String weappPages,
			@RequestParam(name = "template_name", required = false) String templateName,
			@RequestParam(name = "version", required = false, defaultValue = "v1.0.2") String version) {
		long companyId = parseCompanyIdForValidator(request);
		long userId = parseH5UserId(request);
		long distributorLookupId = parseDistributorIdForShopDetailLookup(distributorIdParam);
		Optional<Map<String, Object>> shopOpt =
				distributorRepositoryGetInfoSimpleService.getDistributorApiRowByCompanyAndDistributorId(
						companyId, distributorLookupId);
		if (shopOpt.isEmpty()) {
			throw new ResourceException("当前店铺不存在");
		}
		Map<String, Object> shopRow = shopOpt.get();
		Object isValidRaw = shopRow.get("is_valid");
		if (isValidRaw == null || !"true".equals(String.valueOf(isValidRaw).trim())) {
			throw new ResourceException("当前店铺已失效");
		}
		if (!StringUtils.hasText(templateName)) {
			throw new BadRequestException("缺少模板展示类型");
		}
		long regionauthId = parseRegionauthIdFromShopRow(shopRow.get("regionauth_id"));
		PagesTemplateFrontShopDetailQuery query =
				new PagesTemplateFrontShopDetailQuery(
						companyId,
						userId,
						distributorLookupId,
						regionauthId,
						weappPages,
						templateName.trim(),
						version);
		Map<String, Object> body = pagesTemplateFrontShopDetailService.shopDetail(query, RequestLangTag.current(langueProperties));
		return ApiResult.ok(body);
	}

	@GetMapping(value = "/setInfo", name = "模板设置信息")
	public ApiResult<Object> setInfo(
			HttpServletRequest request,
			@RequestParam(name = "regionauth_id", required = false, defaultValue = "0") String regionauthIdParam) {
		long companyId = parseCompanyIdForValidator(request);
		String requestLang = RequestLangTag.current(langueProperties);
		long regionauthId = parseQueryLongDefaultZero(regionauthIdParam);
		Optional<Map<String, Object>> row =
				pagesTemplateSetGetInfoService.setInfo(companyId, requestLang, regionauthId);
		return row.isPresent()
				? ApiResult.<Object>ok(row.get())
				: ApiResult.<Object>ok(Collections.emptyList());
	}

	@GetMapping(value = "/gettdk", name = "tdk配置")
	public ApiResult<Object> getTdk(HttpServletRequest request) {
		String cc = RequestLangTag.current(langueProperties);
		String suffix;
		String q = request.getParameter("company_id");
		if (StringUtils.hasText(q)) {
			suffix = q;
		} else if (request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS) != null) {
			suffix = Long.toString(parseCompanyIdForValidator(request));
		} else {
			suffix = "";
		}
		Object payload = tdkGivenSaveService.getGlobalSetInfo(suffix, cc);
		return ApiResult.ok(payload);
	}

	private static long requireH5CompanyIdOrUnauthorized(HttpServletRequest request) {
		Object cid = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (cid == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		}
		if (result <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return result;
	}

	private long parseCompanyIdForValidator(HttpServletRequest request) {
		Object cid = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (cid == null) {
			throw new BadRequestException("缺少company_id");
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("缺少company_id");
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("缺少company_id");
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("缺少company_id");
			}
		}
		if (result <= 0L) {
			throw new BadRequestException("缺少company_id");
		}
		return result;
	}

	private static long parseQueryLongDefaultZero(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseH5UserId(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			return 0L;
		}
		Object uid = m.get("user_id");
		if (uid == null) {
			return 0L;
		}
		if (uid instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(uid).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseDistributorIdQuery(String raw) {
		if (raw == null) {
			return 0;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("缺少门店id");
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("缺少门店id");
		}
	}

	/**
	 * For {@code /shopDetail}: invalid or missing {@code distributor_id} maps to {@code 0L} so repository lookup is
	 * empty and the caller surfaces {@link ResourceException}「当前店铺不存在」.
	 */
	private static long parseDistributorIdForShopDetailLookup(String distributorIdParam) {
		if (distributorIdParam == null) {
			return 0L;
		}
		String t = distributorIdParam.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			return v < 1L ? 0L : v;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseRegionauthIdFromShopRow(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parsePositiveIntWithDefault(String raw, int defaultVal) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static Long parseOptionalLongParam(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
