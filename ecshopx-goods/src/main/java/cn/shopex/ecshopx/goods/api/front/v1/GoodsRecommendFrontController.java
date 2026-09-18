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

package cn.shopex.ecshopx.goods.api.front.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendCheckoutAddService;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendErrorCodes;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendErrorMessages;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendMatchService;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendRequestParser;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendSalabilityResolver;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendScene;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("goodsFrontV1GoodsRecommend")
@RequestMapping("/api/v1/h5app/wxapp/goods/recommendations")
public class GoodsRecommendFrontController {

	private final GoodsRecommendMatchService matchService;

	private final GoodsRecommendCheckoutAddService checkoutAddService;

	private final GoodsRecommendSalabilityResolver salabilityResolver;

	private final MessageSource messageSource;

	private final LangueProperties langueProperties;

	public GoodsRecommendFrontController(
			GoodsRecommendMatchService matchService,
			GoodsRecommendCheckoutAddService checkoutAddService,
			GoodsRecommendSalabilityResolver salabilityResolver,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.matchService = matchService;
		this.checkoutAddService = checkoutAddService;
		this.salabilityResolver = salabilityResolver;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.recommend.match")
	@PostMapping(value = "/match", name = "匹配商品推荐", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> match(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyId(request);
		Map<String, Object> input = mergeInput(request, body);
		GoodsRecommendScene scene = GoodsRecommendScene.parse(stringVal(input.get("scene")));
		if (scene == null) {
			throw badRequest(GoodsRecommendErrorCodes.DISPLAY_SETTING_INVALID);
		}
		OptionalLong matchDistributorId =
				GoodsRecommendRequestParser.resolveDistributorIdForMatch(
						salabilityResolver.resolveProductModel(companyId), input.get("distributor_id"));
		if (matchDistributorId.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(matchService.emptyMatchResult()));
		}
		List<Long> mainItemIds = parseItemIds(input.get("main_item_ids"));
		List<Long> excludeItemIds = parseItemIds(input.get("exclude_item_ids"));
		long userId = parseOptionalUserIdFromRequest(request);
		String acceptLanguage = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				matchService.match(
						companyId,
						scene,
						mainItemIds,
						matchDistributorId.getAsLong(),
						excludeItemIds,
						userId,
						acceptLanguage);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@Activated(routeAlias = "goods.recommend.checkout_add")
	@PostMapping(value = "/checkout-add", name = "结算页推荐加购", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> checkoutAdd(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyId(request);
		Map<String, Object> input = mergeInput(request, body);
		GoodsRecommendRequestParser.CheckoutAddDistributorIdResult distributorResult =
				GoodsRecommendRequestParser.resolveDistributorIdForCheckoutAdd(
						salabilityResolver.resolveProductModel(companyId), input.get("distributor_id"));
		if (!distributorResult.isOk()) {
			throw badRequest(distributorResult.errorCode());
		}
		Map<String, Object> claims = readH5AuthClaims(request);
		Map<String, Object> data =
				checkoutAddService.checkoutAdd(
						request, companyId, input, claims, distributorResult.distributorId());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private List<Long> parseItemIds(Object raw) {
		try {
			return GoodsRecommendRequestParser.parseItemIds(raw);
		} catch (IllegalArgumentException e) {
			throw badRequest(GoodsRecommendErrorCodes.ITEM_IDS_INVALID);
		}
	}

	private BadRequestException badRequest(String errorCode) {
		return new BadRequestException(GoodsRecommendErrorMessages.message(messageSource, errorCode));
	}

	private static long parseCompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (companyAttr instanceof Number n) {
			long companyId = n.longValue();
			if (companyId > 0) {
				return companyId;
			}
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				long companyId = Long.parseLong(s.trim());
				if (companyId > 0) {
					return companyId;
				}
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		if (body != null) {
			input.putAll(body);
		}
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0]) && !input.containsKey(k)) {
				input.put(k, v[0]);
			}
		});
		return input;
	}

	private static String stringVal(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}

	@SuppressWarnings("unchecked")
	private static long parseOptionalUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return 0L;
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			return 0L;
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
