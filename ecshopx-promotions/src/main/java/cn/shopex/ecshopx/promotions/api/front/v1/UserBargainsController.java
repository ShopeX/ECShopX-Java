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
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.bargain.UserBargainCreateBargainLogService;
import cn.shopex.ecshopx.promotions.service.bargain.UserBargainCreateUserBargainService;
import cn.shopex.ecshopx.promotions.service.bargain.UserBargainFriendWxaCodeService;
import cn.shopex.ecshopx.promotions.service.bargain.UserBargainGetUserBargainService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = true)
@FrontNoAuth
@RestController("promotionsFrontV1UserBargains")
@RequestMapping("/api/v1/h5app/wxapp/promotion")
public class UserBargainsController {

	private final UserBargainCreateBargainLogService userBargainCreateBargainLogService;
	private final UserBargainCreateUserBargainService userBargainCreateUserBargainService;
	private final UserBargainFriendWxaCodeService userBargainFriendWxaCodeService;
	private final UserBargainGetUserBargainService userBargainGetUserBargainService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public UserBargainsController(
			UserBargainCreateBargainLogService userBargainCreateBargainLogService,
			UserBargainCreateUserBargainService userBargainCreateUserBargainService,
			UserBargainFriendWxaCodeService userBargainFriendWxaCodeService,
			UserBargainGetUserBargainService userBargainGetUserBargainService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.userBargainCreateBargainLogService = userBargainCreateBargainLogService;
		this.userBargainCreateUserBargainService = userBargainCreateUserBargainService;
		this.userBargainFriendWxaCodeService = userBargainFriendWxaCodeService;
		this.userBargainGetUserBargainService = userBargainGetUserBargainService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@FrontAuth
	@PostMapping(value = "/userbargain", name = "参与砍价", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createUserBargain(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object rawBargainId = merged.get("bargain_id");
		String bargainIdRaw = rawBargainId == null ? "" : String.valueOf(rawBargainId).trim();
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		String authorizerAppid = Objects.toString(claims.get("woa_appid"), "").trim();
		String wxaAppid = Objects.toString(claims.get("wxapp_appid"), "").trim();
		Locale locale = request.getLocale();
		Map<String, Object> row =
				userBargainCreateUserBargainService.createUserBargain(
						companyId, userId, authorizerAppid, wxaAppid, bargainIdRaw, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@FrontAuth
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/bargainfriendwxappcode", name = "砍价分享码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getBargainFriendWxaCode(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Object rawBargainId = FlexibleHttpServletParameterMap.toObjectMap(request).get("bargain_id");
		String bargainId = rawBargainId == null ? "" : String.valueOf(rawBargainId);
		if (bargainId.length() < 1) {
			throw new BadRequestException(
					"获取小程序码参数出错，请检查.",
					Map.of("bargain_id", List.of("validation.required")));
		}
		String wxappAppid = Objects.toString(claims.get("wxapp_appid"), "");
		if (wxappAppid.isEmpty()) {
			throw new BadRequestException("获取小程序码参数出错");
		}
		Map<String, Object> data =
				userBargainFriendWxaCodeService.getBargainFriendWxaCode(wxappAppid, userId, bargainId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/userbargain", name = "砍价详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserBargain(HttpServletRequest request) {
		Map<String, Object> query = FlexibleHttpServletParameterMap.toObjectMap(request);
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long authUserId = parseAuthUserIdFromClaims(claims);
		Locale locale = request.getLocale();

		List<String> bargainParts = new ArrayList<>();
		Object rawBargainId = query.get("bargain_id");
		String bargainText = rawBargainId == null ? "" : String.valueOf(rawBargainId).trim();
		long bargainId = 0L;
		if (bargainText.isEmpty()) {
			bargainParts.add(
					messageSource.getMessage("promotions.bargain.bargain_activity_id_required", null, locale));
		} else {
			bargainId = LeadingNumberParser.parseAsLong(bargainText);
		}
		if (!bargainParts.isEmpty()) {
			throw new ResourceException(trimTrailingCommaSeparators(String.join("，", bargainParts)));
		}

		Object rawUserId = query.get("user_id");
		String userText = rawUserId == null ? "" : String.valueOf(rawUserId).trim();
		long resolvedUserId = userText.isEmpty() ? authUserId : LeadingNumberParser.parseAsLong(userText);

		Object rawHasOrder = query.get("has_order");
		boolean hasOrder =
				Objects.equals("true", rawHasOrder == null ? null : String.valueOf(rawHasOrder).trim());

		String acceptLanguage = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				userBargainGetUserBargainService.getUserBargain(
						companyId, authUserId, resolvedUserId, bargainId, hasOrder, acceptLanguage, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String trimTrailingCommaSeparators(String joined) {
		String s = joined;
		while (s.endsWith("，") || s.endsWith(",")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	@PostMapping(value = "/bargainlog", name = "砍价助力", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createBargainLog(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = parseCompanyIdFromRequest(request);
		merged.put("company_id", companyId);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		merged.put("authorizer_appid", Objects.toString(claims.get("woa_appid"), "").trim());
		merged.put("wxa_appid", Objects.toString(claims.get("wxapp_appid"), "").trim());
		long authUserId = parseAuthUserIdFromClaims(claims);
		if (authUserId > 0L) {
			merged.put("open_id", firstNonBlankString(merged.get("open_id"), claims.get("open_id")));
			merged.put("nickname", firstNonBlankString(merged.get("nickname"), claims.get("nickname")));
			merged.put("headimgurl", firstNonBlankString(merged.get("headimgurl"), claims.get("headimgurl")));
		}
		Locale locale = request.getLocale();
		Map<String, Object> row = userBargainCreateBargainLogService.createBargainLog(merged, locale);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long id = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return id > 0L ? id : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String firstNonBlankString(Object a, Object b) {
		if (a == null) {
			return Objects.toString(b, "").trim();
		}
		String sa = String.valueOf(a).trim();
		return sa.isEmpty() ? Objects.toString(b, "").trim() : sa;
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
}
