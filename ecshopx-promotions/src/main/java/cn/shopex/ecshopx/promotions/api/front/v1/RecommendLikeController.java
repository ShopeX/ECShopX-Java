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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.RecommendLikeListService;
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
		notFound = false)
@FrontNoAuth
@RestController("promotionsFrontV1RecommendLike")
@RequestMapping("/api/v1/h5app/wxapp/promotions")
public class RecommendLikeController {

	private final RecommendLikeListService recommendLikeListService;
	private final LangueProperties langueProperties;

	public RecommendLikeController(
			RecommendLikeListService recommendLikeListService, LangueProperties langueProperties) {
		this.recommendLikeListService = recommendLikeListService;
		this.langueProperties = langueProperties;
	}

	@GetMapping(value = "/recommendlike", name = "猜你喜欢", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecommendLikeLists(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "6") String pageSizeRaw,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 6);
		long distributorId = parseDistributorIdFromQuery(distributorIdRaw);
		Map<String, Object> data =
				recommendLikeListService.getRecommendLikeLists(
						companyId,
						page,
						pageSize,
						distributorId,
						Boolean.TRUE,
						userId,
						RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseDistributorIdFromQuery(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id格式错误");
		}
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

	private static int parsePositivePagingInt(String raw, int defaultVal) {
		try {
			String s = raw == null ? "" : raw.trim();
			String digits = LeadingNumberParser.parseAsString(s);
			int v = Integer.parseInt(digits);
			return Math.max(1, v);
		} catch (Exception e) {
			return defaultVal;
		}
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
}
