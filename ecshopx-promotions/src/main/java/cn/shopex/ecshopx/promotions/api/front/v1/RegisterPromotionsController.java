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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.RegisterPromotionsConfigReadService;
import cn.shopex.ecshopx.promotions.service.register.RegisterPromotionsMembercardPromotionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
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
		notFound = true)
@FrontAuth
@RestController("promotionsFrontV1RegisterPromotions")
@RequestMapping("/api/v1/h5app/wxapp/promotion")
public class RegisterPromotionsController {

	private final RegisterPromotionsMembercardPromotionService registerPromotionsMembercardPromotionService;
	private final RegisterPromotionsConfigReadService registerPromotionsConfigReadService;
	private final LangueProperties langueProperties;

	public RegisterPromotionsController(
			RegisterPromotionsMembercardPromotionService registerPromotionsMembercardPromotionService,
			RegisterPromotionsConfigReadService registerPromotionsConfigReadService,
			LangueProperties langueProperties) {
		this.registerPromotionsMembercardPromotionService = registerPromotionsMembercardPromotionService;
		this.registerPromotionsConfigReadService = registerPromotionsConfigReadService;
		this.langueProperties = langueProperties;
	}

	@FrontNoAuth
	@GetMapping(value = "/register", name = "注册引导营销", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getRegisterPromotionsConfig(
			HttpServletRequest request,
			@RequestParam(value = "register_type", required = false) String registerType) {
		long companyId = parseCompanyIdFromRequest(request);
		String typeParam = registerType == null ? "" : registerType.trim();
		String effectiveRegisterType = StringUtils.hasText(typeParam) ? typeParam : "general";
		String requestLangTag = RequestLangTag.current(langueProperties);
		Object data =
				registerPromotionsConfigReadService.getRegisterPromotionsConfig(
						companyId, effectiveRegisterType, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/getMemberCard", name = "付费会员卡", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getMembercardPromotions(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		String mobile = parseMobileFromClaims(claims);
		Map<String, Object> body =
				registerPromotionsMembercardPromotionService.getMembercardPromotions(companyId, userId, mobile);
		return ResponseEntity.ok(ApiResult.ok(body));
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

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long parsed = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return parsed > 0L ? parsed : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String parseMobileFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("mobile");
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}
}
