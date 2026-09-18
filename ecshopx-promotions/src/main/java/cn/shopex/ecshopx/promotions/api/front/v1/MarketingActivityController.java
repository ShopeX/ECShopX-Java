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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityItemListService;
import cn.shopex.ecshopx.promotions.service.PlusPriceBuyItemListService;
import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
@RestController("promotionsFrontV1MarketingActivity")
@RequestMapping("/api/v1/h5app/wxapp/promotion")
public class MarketingActivityController {

	private final MarketingActivityItemListService marketingActivityItemListService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final MessageSource messageSource;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final SkuValidMarketingActivityService skuValidMarketingActivityService;
	private final PlusPriceBuyItemListService plusPriceBuyItemListService;

	public MarketingActivityController(
			MarketingActivityItemListService marketingActivityItemListService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			MessageSource messageSource,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			SkuValidMarketingActivityService skuValidMarketingActivityService,
			PlusPriceBuyItemListService plusPriceBuyItemListService) {
		this.marketingActivityItemListService = marketingActivityItemListService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.messageSource = messageSource;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.skuValidMarketingActivityService = skuValidMarketingActivityService;
		this.plusPriceBuyItemListService = plusPriceBuyItemListService;
	}

	@FrontAuth
	@GetMapping(value = "/pluspricebuy/getItemList", name = "加价购商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getPlusPriceBuyItem(
			HttpServletRequest request,
			@RequestParam(name = "marketing_id", required = false) String marketingIdRaw,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		Long mid = parseOptionalLong(marketingIdRaw);
		if (mid == null || mid < 1L) {
			return ResponseEntity.ok(ApiResult.<Object>ok(Collections.emptyList()));
		}
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);
		Map<String, Object> data = plusPriceBuyItemListService.getPlusPriceBuyItem(companyId, userId, mid, page, pageSize);
		data.put(
				"cur",
				companyDefaultCurrencyService.toCurResponseMap(companyDefaultCurrencyService.getCur(companyId)));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/getskumarketing", name = "SKU营销", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getValidMarketingActivityByItemId(
			HttpServletRequest request, @RequestParam(name = "item_id", required = false) String itemIdRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		Long itemId = parseOptionalLong(itemIdRaw);
		long distributorId = 0L;
		if (itemId != null) {
			Map<String, Object> narrow = marketingActivityCatalogAccess.loadItemNarrowMapForSkuMarketing(companyId, itemId);
			if (narrow != null) {
				Object d = narrow.get("distributor_id");
				if (d instanceof Number n) {
					distributorId = n.longValue();
				}
			}
		}
		List<Map<String, Object>> listOrNull =
				skuValidMarketingActivityService.getValidMarketingActivityByItemId(companyId, itemId, userId, distributorId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("promotion_activity", listOrNull == null || listOrNull.isEmpty() ? null : listOrNull);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/fullpromotion/getitemlist", name = "满促商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getMarketingActivityItemsList(
			HttpServletRequest request,
			@RequestParam(name = "marketing_id", required = false) String marketingIdRaw,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		Long marketingId = parseOptionalLong(marketingIdRaw);
		if (marketingId == null || marketingId <= 0L) {
			Locale locale = LocaleContextHolder.getLocale();
			throw new ResourceException(
					messageSource.getMessage("promotions.marketing.activity_expired", null, locale));
		}
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);
		Map<String, Object> data =
				marketingActivityItemListService.getActivityItemList(companyId, marketingId, page, pageSize, true);
		CurrencyExchangeRate curRow = companyDefaultCurrencyService.getCur(companyId);
		data.put("cur", companyDefaultCurrencyService.toCurResponseMap(curRow));
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

	private static Long parseOptionalLong(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		char c0 = t.charAt(0);
		if (!Character.isDigit(c0) && c0 != '+' && c0 != '-') {
			return null;
		}
		try {
			return Long.parseLong(LeadingNumberParser.parseAsString(t));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
