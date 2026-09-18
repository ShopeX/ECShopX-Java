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
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.theme.service.PcLoginPageSettingGetService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetDecorationContentService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetHeaderOrFooterService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RequiredArgsConstructor
@RestController("themeFrontV1PcTemplate")
@RequestMapping("/api/v1/h5app/wxapp/pctemplate")
public class PcTemplateController {

	/** PHP open {@code page_type} allow-list: home/header/footer/custom/product_list. */
	private static final Set<String> FRONT_PAGE_TYPES =
			Set.of("home", "header", "footer", "custom", "product_list");

	private final PcTemplateGetHeaderOrFooterService pcTemplateGetHeaderOrFooterService;
	private final PcTemplateGetDecorationContentService pcTemplateGetDecorationContentService;
	private final LangueProperties langueProperties;
	private final PcLoginPageSettingGetService pcLoginPageSettingGetService;

	@GetMapping(value = "/getHeaderOrFooter", name = "PC头尾部")
	public ApiResult<Object> getHeaderOrFooter(
			HttpServletRequest request,
			@RequestParam(value = "page_name", required = false) String pageName) {
		CompanyIdResolve companyIdResolve = resolveCompanyIdForGetHeaderOrFooter(request);
		if (companyIdResolve.nonNumericExplicitQuery()) {
			return ApiResult.ok(Collections.emptyList());
		}
		long companyId = companyIdResolve.companyId();
		String trimmedPageName = pageName == null ? "" : pageName.trim();
		if (trimmedPageName.isEmpty()) {
			throw new BadRequestException("缺少page_name");
		}
		String requestLang = RequestLangTag.current(langueProperties);
		Object data = pcTemplateGetHeaderOrFooterService.getHeaderOrFooter(companyId, requestLang, trimmedPageName);
		return ApiResult.ok(data);
	}

	@GetMapping(value = "/getTemplateContent", name = "PC模板内容")
	public ApiResult<Map<String, Object>> getTemplateContent(
			HttpServletRequest request,
			@RequestParam(value = "theme_pc_template_id", required = false) String themePcTemplateId,
			@RequestParam(value = "page_type", required = false) String pageType,
			@RequestParam(value = "page_id", required = false) String pageId,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		long companyId = resolveCompanyIdForGetTemplateContent(request);
		String effectivePageType =
				(pageType == null || pageType.isBlank()) ? "home" : pageType.trim();
		if (!FRONT_PAGE_TYPES.contains(effectivePageType)) {
			throw new BadRequestException("缺少或错误的page_type");
		}
		if ("custom".equals(effectivePageType) && isPhpEmptyId(pageId)) {
			throw new BadRequestException("缺少page_id");
		}
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				pcTemplateGetDecorationContentService.getDecorationContent(
						companyId,
						requestLang,
						themePcTemplateId,
						effectivePageType,
						pageId,
						parseDistributorIdOrZero(distributorId),
						false);
		return ApiResult.ok(data);
	}

	@GetMapping(value = "/loginPage/setting", name = "PC登录页设置")
	public ApiResult<Object> getLoginPageSetting(HttpServletRequest request) {
		long companyId = resolveCompanyIdForLoginPageSetting(request);
		Map<String, Object> data = pcLoginPageSettingGetService.getLoginPageSetting(companyId);
		if (data != null && data.isEmpty()) {
			return ApiResult.ok(Collections.emptyList());
		}
		return ApiResult.ok(data);
	}

	private static long resolveCompanyIdForLoginPageSetting(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawClaims instanceof Map<?, ?> claims) {
			return parsePositiveCompanyIdFromMemberClaims(claims.get("company_id"));
		}
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (companyAttr != null) {
			return parsePositiveCompanyIdFromMemberClaims(companyAttr);
		}
		throw new BadRequestException("缺少company_id");
	}

	private static long resolveCompanyIdForGetTemplateContent(HttpServletRequest request) {
		if (request.getParameterMap().containsKey("company_id")) {
			String q = request.getParameter("company_id");
			if (q == null || q.isBlank()) {
				throw new BadRequestException("缺少company_id");
			}
			String t = q.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("缺少company_id");
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					throw new BadRequestException("缺少company_id");
				}
				return v;
			} catch (NumberFormatException ex) {
				throw new BadRequestException("缺少company_id");
			}
		}
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawClaims instanceof Map<?, ?> claims) {
			return parsePositiveCompanyIdFromMemberClaims(claims.get("company_id"));
		}
		throw new BadRequestException("缺少company_id");
	}

	private static long parseDistributorIdOrZero(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v < 0L ? 0L : v;
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	/** Matches PHP {@code empty()} for {@code page_id} / custom required_if. */
	private static boolean isPhpEmptyId(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static CompanyIdResolve resolveCompanyIdForGetHeaderOrFooter(HttpServletRequest request) {
		if (request.getParameterMap().containsKey("company_id")) {
			String q = request.getParameter("company_id");
			if (q == null || q.isEmpty()) {
				throw new BadRequestException("缺少company_id");
			}
			String t = q.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("缺少company_id");
			}
			try {
				return CompanyIdResolve.ok(Long.parseLong(t));
			} catch (NumberFormatException ex) {
				return CompanyIdResolve.nonNumericQuery();
			}
		}
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawClaims instanceof Map<?, ?> claims) {
			return CompanyIdResolve.ok(parsePositiveCompanyIdFromMemberClaims(claims.get("company_id")));
		}
		throw new BadRequestException("缺少company_id");
	}

	private static final class CompanyIdResolve {
		private final boolean nonNumericExplicitQuery;
		private final long companyId;

		private CompanyIdResolve(boolean nonNumericExplicitQuery, long companyId) {
			this.nonNumericExplicitQuery = nonNumericExplicitQuery;
			this.companyId = companyId;
		}

		static CompanyIdResolve ok(long companyId) {
			return new CompanyIdResolve(false, companyId);
		}

		static CompanyIdResolve nonNumericQuery() {
			return new CompanyIdResolve(true, 0L);
		}

		boolean nonNumericExplicitQuery() {
			return nonNumericExplicitQuery;
		}

		long companyId() {
			return companyId;
		}
	}

	private static long parsePositiveCompanyIdFromMemberClaims(Object attr) {
		if (attr == null) {
			throw new BadRequestException("缺少company_id");
		}
		if (attr instanceof Number n) {
			long v = n.longValue();
			if (v < 1L) {
				throw new BadRequestException("缺少company_id");
			}
			return v;
		}
		if (attr instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("缺少company_id");
			}
			try {
				long v = Long.parseLong(t);
				if (v < 1L) {
					throw new BadRequestException("缺少company_id");
				}
				return v;
			} catch (NumberFormatException ex) {
				throw new BadRequestException("缺少company_id");
			}
		}
		try {
			long v = Long.parseLong(String.valueOf(attr).trim());
			if (v < 1L) {
				throw new BadRequestException("缺少company_id");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("缺少company_id");
		}
	}

}
