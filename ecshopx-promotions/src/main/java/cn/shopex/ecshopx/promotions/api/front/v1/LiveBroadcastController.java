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

package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.companys.language.CompanyLanguageResolver;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestMessageLocale;
import cn.shopex.ecshopx.promotions.service.LiveRoomListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
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
@RestController("promotionsFrontV1LiveBroadcast")
@RequestMapping("/api/v1/h5app/wxapp/promotion")
public class LiveBroadcastController {

	private final LiveRoomListService liveRoomListService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;
	private final CompanyLanguageResolver companyLanguageResolver;

	public LiveBroadcastController(
			LiveRoomListService liveRoomListService,
			MessageSource messageSource,
			LangueProperties langueProperties,
			CompanyLanguageResolver companyLanguageResolver) {
		this.liveRoomListService = liveRoomListService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
		this.companyLanguageResolver = companyLanguageResolver;
	}

	@GetMapping(value = "/live/list", name = "直播列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getLiveList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "page_size", required = false) String pageSizeRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		Locale locale = RequestMessageLocale.messageLocale(
				langueProperties, request, null, companyLanguageResolver.getDefaultLanguage(companyId));
		Object h5AuthClaimsAttr = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		final String authorizer;
		if (h5AuthClaimsAttr instanceof Map<?, ?> m && !m.isEmpty()) {
			authorizer = toStr(m.get("wxapp_appid"));
		} else {
			Map<String, Object> claims = readH5AuthClaimsMap(request);
			authorizer =
					firstNonBlank(
							toStr(claims.get("wxapp_appid")),
							trimToEmpty(request.getParameter("appid")),
							trimToEmpty(request.getHeader("authorizer-appid")));
		}
		if (!StringUtils.hasText(authorizer)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.live_list.wxapp_required", null, locale));
		}
		Map<String, Object> paramMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		Object oPage = paramMap.get("page");
		String mergedPageRaw = (oPage == null) ? pageRaw : String.valueOf(oPage).trim();
		Object oPageSize = paramMap.get("page_size");
		String mergedPageSizeRaw = (oPageSize == null) ? pageSizeRaw : String.valueOf(oPageSize).trim();
		Map<String, Object> data =
				liveRoomListService.getLiveList(companyId, authorizer, mergedPageRaw, mergedPageSizeRaw, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/replay/list", name = "回放列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getReplayList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "page_size", required = false) String pageSizeRaw,
			@RequestParam(name = "room_id", required = false) String roomIdRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		Locale locale = RequestMessageLocale.messageLocale(
				langueProperties, request, null, companyLanguageResolver.getDefaultLanguage(companyId));
		Object h5AuthClaimsAttr = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		final String authorizer;
		if (h5AuthClaimsAttr instanceof Map<?, ?> m && !m.isEmpty()) {
			authorizer = toStr(m.get("wxapp_appid"));
		} else {
			Map<String, Object> claims = readH5AuthClaimsMap(request);
			authorizer =
					firstNonBlank(
							toStr(claims.get("wxapp_appid")),
							trimToEmpty(request.getParameter("appid")),
							trimToEmpty(request.getHeader("authorizer-appid")));
		}
		if (!StringUtils.hasText(authorizer)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.live_list.wxapp_required", null, locale));
		}
		Map<String, Object> paramMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		Object oPage = paramMap.get("page");
		String mergedPageRaw = (oPage == null) ? pageRaw : String.valueOf(oPage).trim();
		Object oPageSize = paramMap.get("page_size");
		String mergedPageSizeRaw = (oPageSize == null) ? pageSizeRaw : String.valueOf(oPageSize).trim();
		Object oRoom = paramMap.get("room_id");
		String mergedRoomRaw = (oRoom == null) ? roomIdRaw : String.valueOf(oRoom).trim();
		Map<String, Object> data =
				liveRoomListService.getReplayList(
						companyId, authorizer, mergedPageRaw, mergedPageSizeRaw, mergedRoomRaw, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static String firstNonBlank(String a, String b, String c) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		if (StringUtils.hasText(b)) {
			return b;
		}
		if (StringUtils.hasText(c)) {
			return c;
		}
		return "";
	}

	private static String toStr(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}
}
