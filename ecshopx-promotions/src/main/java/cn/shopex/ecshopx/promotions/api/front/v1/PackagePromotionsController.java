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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.promotions.service.PackagePromotionFrontPackageInfoService;
import cn.shopex.ecshopx.promotions.service.PackagePromotionFrontPackageListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("promotionsFrontV1PackagePromotions")
@RequestMapping("/api/v1/h5app/wxapp/promotions")
public class PackagePromotionsController {

	private final PackagePromotionFrontPackageListService packagePromotionFrontPackageListService;
	private final PackagePromotionFrontPackageInfoService packagePromotionFrontPackageInfoService;

	public PackagePromotionsController(
			PackagePromotionFrontPackageListService packagePromotionFrontPackageListService,
			PackagePromotionFrontPackageInfoService packagePromotionFrontPackageInfoService) {
		this.packagePromotionFrontPackageListService = packagePromotionFrontPackageListService;
		this.packagePromotionFrontPackageInfoService = packagePromotionFrontPackageInfoService;
	}

	@GetMapping(value = "/package", name = "组合商品列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(value = "item_id", required = false) String itemIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw) {
		long companyId = parseCompanyIdFromRequest(request);

		String itemIdText = itemIdRaw == null ? "" : itemIdRaw.trim();
		long itemId;
		if (itemIdText.isEmpty()) {
			itemId = 0L;
		} else {
			itemId = cn.shopex.ecshopx.common.util.LeadingNumberParser.parseAsLong(itemIdText);
		}

		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);

		String distributorText = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		long distributorId = LeadingNumberParser.parseAsLong(distributorText);

		Map<String, Object> result =
				packagePromotionFrontPackageListService.lists(companyId, itemId, page, pageSize, distributorId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@GetMapping(value = "/package/{packageId}", name = "组合商品详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> info(
			HttpServletRequest request,
			@PathVariable("packageId") String packageIdRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		String authorizerAppId =
				Objects.toString(readH5AuthClaimsMap(request).get("woa_appid"), "").trim();
		String raw = packageIdRaw == null ? "" : packageIdRaw.trim();
		long packageId = LeadingNumberParser.parseAsLong(raw);
		Map<String, Object> body =
				packagePromotionFrontPackageInfoService.info(companyId, packageId, authorizerAppId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
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
}
