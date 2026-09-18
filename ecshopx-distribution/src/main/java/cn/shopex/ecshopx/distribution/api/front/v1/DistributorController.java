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

package cn.shopex.ecshopx.distribution.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.port.DistributionCheckInRulePort;
import cn.shopex.ecshopx.distribution.service.DistributionStoreEntryRuleRedisService;
import cn.shopex.ecshopx.distribution.service.DistributorAftersalesAddressReadService;
import cn.shopex.ecshopx.distribution.service.DistributorCategoryService;
import cn.shopex.ecshopx.distribution.service.DistributorCheckSmsVcodeService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetAllDistributorListService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDistributorListService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetAllDistributorMinimalService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDistributionDefaultDetailService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetAreaByAddressService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetAreaByJwdService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetAreaInfoService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDeliveryTypeService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDistributorCountService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDistributorIsValidService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDistributorInfoService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetDistributorService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetSelfShopDetailService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetSalespersonQrcodeService;
import cn.shopex.ecshopx.distribution.service.DistributorH5GetSliderService;
import cn.shopex.ecshopx.distribution.service.DistributorH5MerchantAvailableService;
import cn.shopex.ecshopx.distribution.service.DistributorH5NearPickupLocationService;
import cn.shopex.ecshopx.distribution.service.DistributorH5ShopCaptchaSmsService;
import cn.shopex.ecshopx.distribution.service.DistributorWhiteListCheckUserValidService;
import cn.shopex.ecshopx.distribution.service.DistributorWhiteListMemberShopListService;
import cn.shopex.ecshopx.distribution.service.ShopScreenAdvertisementStartAdsService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.dto.DistributorIsValidQuery;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.PLAIN,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("distributionFrontV1Distributor")
@RequestMapping("/api/v1/h5app")
public class DistributorController {

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final DistributionCheckInRulePort distributionCheckInRulePort;
	private final DistributorCheckSmsVcodeService distributorCheckSmsVcodeService;
	private final DistributorH5GetDistributorService distributorH5GetDistributorService;
	private final DistributorH5GetDistributorInfoService distributorH5GetDistributorInfoService;
	private final DistributorH5GetDistributorCountService distributorH5GetDistributorCountService;
	private final DistributorH5GetAllDistributorListService distributorH5GetAllDistributorListService;
	private final DistributorH5GetDistributorListService distributorH5GetDistributorListService;
	private final DistributorH5GetAreaInfoService distributorH5GetAreaInfoService;
	private final ShopScreenAdvertisementStartAdsService shopScreenAdvertisementStartAdsService;
	private final DistributorAftersalesAddressReadService distributorAftersalesAddressReadService;
	private final DistributorWhiteListCheckUserValidService distributorWhiteListCheckUserValidService;
	private final DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService;
	private final DistributorH5GetDistributionDefaultDetailService distributorH5GetDistributionDefaultDetailService;
	private final DistributorH5GetDeliveryTypeService distributorH5GetDeliveryTypeService;
	private final DistributorH5GetAreaByAddressService distributorH5GetAreaByAddressService;
	private final DistributorH5GetAreaByJwdService distributorH5GetAreaByJwdService;
	private final DistributorH5GetAllDistributorMinimalService distributorH5GetAllDistributorMinimalService;
	private final DistributorH5GetSelfShopDetailService distributorH5GetSelfShopDetailService;
	private final DistributorH5ShopCaptchaSmsService distributorH5ShopCaptchaSmsService;
	private final DistributorH5MerchantAvailableService distributorH5MerchantAvailableService;
	private final DistributorH5NearPickupLocationService distributorH5NearPickupLocationService;
	private final DistributorWhiteListMemberShopListService distributorWhiteListMemberShopListService;
	private final DistributorH5GetDistributorIsValidService distributorH5GetDistributorIsValidService;
	private final DistributorH5GetSliderService distributorH5GetSliderService;
	private final DistributorCategoryService distributorCategoryService;
	private final DistributorH5GetSalespersonQrcodeService distributorH5GetSalespersonQrcodeService;
	private final LangueProperties langueProperties;

	public DistributorController(
			DistributionCheckInRulePort distributionCheckInRulePort,
			DistributorCheckSmsVcodeService distributorCheckSmsVcodeService,
			DistributorH5GetDistributorService distributorH5GetDistributorService,
			DistributorH5GetDistributorInfoService distributorH5GetDistributorInfoService,
			DistributorH5GetDistributorCountService distributorH5GetDistributorCountService,
			DistributorH5GetAllDistributorListService distributorH5GetAllDistributorListService,
			DistributorH5GetDistributorListService distributorH5GetDistributorListService,
			DistributorH5GetAreaInfoService distributorH5GetAreaInfoService,
			ShopScreenAdvertisementStartAdsService shopScreenAdvertisementStartAdsService,
			DistributorAftersalesAddressReadService distributorAftersalesAddressReadService,
			DistributorWhiteListCheckUserValidService distributorWhiteListCheckUserValidService,
			DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService,
			DistributorH5GetDistributionDefaultDetailService distributorH5GetDistributionDefaultDetailService,
			DistributorH5GetDeliveryTypeService distributorH5GetDeliveryTypeService,
			DistributorH5GetAreaByAddressService distributorH5GetAreaByAddressService,
			DistributorH5GetAreaByJwdService distributorH5GetAreaByJwdService,
			DistributorH5GetAllDistributorMinimalService distributorH5GetAllDistributorMinimalService,
			DistributorH5GetSelfShopDetailService distributorH5GetSelfShopDetailService,
			DistributorH5ShopCaptchaSmsService distributorH5ShopCaptchaSmsService,
			DistributorH5MerchantAvailableService distributorH5MerchantAvailableService,
			DistributorH5NearPickupLocationService distributorH5NearPickupLocationService,
			DistributorWhiteListMemberShopListService distributorWhiteListMemberShopListService,
			DistributorH5GetDistributorIsValidService distributorH5GetDistributorIsValidService,
			DistributorH5GetSliderService distributorH5GetSliderService,
			DistributorCategoryService distributorCategoryService,
			DistributorH5GetSalespersonQrcodeService distributorH5GetSalespersonQrcodeService,
			LangueProperties langueProperties) {
		this.distributionCheckInRulePort = distributionCheckInRulePort;
		this.distributorCheckSmsVcodeService = distributorCheckSmsVcodeService;
		this.distributorH5GetDistributorService = distributorH5GetDistributorService;
		this.distributorH5GetDistributorInfoService = distributorH5GetDistributorInfoService;
		this.distributorH5GetDistributorCountService = distributorH5GetDistributorCountService;
		this.distributorH5GetAllDistributorListService = distributorH5GetAllDistributorListService;
		this.distributorH5GetDistributorListService = distributorH5GetDistributorListService;
		this.distributorH5GetAreaInfoService = distributorH5GetAreaInfoService;
		this.shopScreenAdvertisementStartAdsService = shopScreenAdvertisementStartAdsService;
		this.distributorAftersalesAddressReadService = distributorAftersalesAddressReadService;
		this.distributorWhiteListCheckUserValidService = distributorWhiteListCheckUserValidService;
		this.distributionStoreEntryRuleRedisService = distributionStoreEntryRuleRedisService;
		this.distributorH5GetDistributionDefaultDetailService = distributorH5GetDistributionDefaultDetailService;
		this.distributorH5GetDeliveryTypeService = distributorH5GetDeliveryTypeService;
		this.distributorH5GetAreaByAddressService = distributorH5GetAreaByAddressService;
		this.distributorH5GetAreaByJwdService = distributorH5GetAreaByJwdService;
		this.distributorH5GetAllDistributorMinimalService = distributorH5GetAllDistributorMinimalService;
		this.distributorH5GetSelfShopDetailService = distributorH5GetSelfShopDetailService;
		this.distributorH5ShopCaptchaSmsService = distributorH5ShopCaptchaSmsService;
		this.distributorH5MerchantAvailableService = distributorH5MerchantAvailableService;
		this.distributorH5NearPickupLocationService = distributorH5NearPickupLocationService;
		this.distributorWhiteListMemberShopListService = distributorWhiteListMemberShopListService;
		this.distributorH5GetDistributorIsValidService = distributorH5GetDistributorIsValidService;
		this.distributorH5GetSliderService = distributorH5GetSliderService;
		this.distributorCategoryService = distributorCategoryService;
		this.distributorH5GetSalespersonQrcodeService = distributorH5GetSalespersonQrcodeService;
		this.langueProperties = langueProperties;
	}

	/**
	 * True only when {@code distributor_id} is missing ({@code null}) or blank after {@link String#trim()}.
	 * Any other query string (including {@code "0"}) is false here and is parsed in
	 * {@link #getAftersaleAddressByDistributor} with {@link Long#parseLong(String)}; after a successful parse,
	 * {@code distributorId == 0L} yields an empty list without calling the aftersale address read service.
	 */
	private static boolean isMissingOrBlankDistributorIdForAftersaleQuery(String distributorIdRaw) {
		return distributorIdRaw == null || distributorIdRaw.trim().isEmpty();
	}

	/**
	 * Quick branch when {@code distributor_id} is absent, blank, or exactly the literal {@code undefined} (case
	 * sensitive). Explicit {@code "0"} is not quick; other numeric-zero spellings are resolved via the shop path.
	 */
	private static boolean isDeliveryTypeQuickBranch(String distributorIdRaw) {
		if (distributorIdRaw == null) {
			return true;
		}
		String trim = distributorIdRaw.trim();
		if (trim.isEmpty()) {
			return true;
		}
		if ("undefined".equals(trim)) {
			return true;
		}
		if ("0".equals(trim)) {
			return false;
		}
		try {
			Long.parseLong(trim);
		} catch (NumberFormatException ignored) {
			// non-numeric (except handled above) uses conservative shop path
		}
		return false;
	}

	private static long resolveH5CompanyIdWithLegacyDefault(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (companyAttr == null) {
			return 1L;
		}
		if (companyAttr instanceof Number n) {
			return n.longValue();
		}
		if (companyAttr instanceof String s) {
			String trim = s.trim();
			if (!StringUtils.hasText(trim)) {
				return 0L;
			}
			try {
				return Long.parseLong(trim);
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		}
		throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
	}

	private static long requirePositiveH5CompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		return companyId;
	}

	private static Long readNullableH5CompanyIdFromRequestAttributes(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (companyAttr == null) {
			return null;
		}
		if (companyAttr instanceof Number n) {
			return Long.valueOf(n.longValue());
		}
		if (companyAttr instanceof String s) {
			String trim = (s == null) ? "" : s.trim();
			if (!StringUtils.hasText(trim)) {
				return 0L;
			}
			try {
				return Long.parseLong(trim);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static long parseH5DistributorIdLoose(String distributorIdRaw) {
		if (distributorIdRaw == null || !StringUtils.hasText(distributorIdRaw.trim())) {
			return 0L;
		}
		try {
			return Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseIntLoose(String raw, int defaultVal) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long resolveH5UserId(HttpServletRequest request) {
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

	private static String resolveWorkUseridFromBody(Map<String, Object> body) {
		if (body == null) {
			return null;
		}
		Object v = body.get("work_userid");
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		String t = String.valueOf(v).trim();
		return t.isEmpty() ? null : t;
	}

	@FrontAuth
	@GetMapping(value = "/wxapp/distributor", name = "获取分销商详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributor(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> data = distributorH5GetDistributorService.getDistributor(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/wxapp/distributor/count", name = "获取分销商统计")
	public ResponseEntity<ApiResult<Object>> getDistributorCount(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Object data = distributorH5GetDistributorCountService.getDistributorCount(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/wxapp/distributor/aftersaleaddress", name = "店铺售后地址")
	public ResponseEntity<ApiResult<Object>> getAftersaleAddressByDistributor(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		if (isMissingOrBlankDistributorIdForAftersaleQuery(distributorIdRaw)) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		String t = distributorIdRaw.trim();
		long distributorId;
		try {
			distributorId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			Map<String, Object> emptyPayload = new LinkedHashMap<>();
			Map<String, Object> addr = new LinkedHashMap<>();
			addr.put("total_count", 0L);
			addr.put("list", List.of());
			emptyPayload.put("address", addr);
			emptyPayload.put("distributor_info", Collections.emptyList());
			return ResponseEntity.ok(ApiResult.ok(emptyPayload));
		}
		if (distributorId == 0L) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> payload =
				distributorAftersalesAddressReadService.getAftersaleAddressByDistributor(
						companyId, distributorId, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@FrontAuth
	@GetMapping(value = "/wxapp/distributor/whitelistByMember", name = "用户白名单店铺列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getDistributorListByWhiteMember(
			HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		long userId = resolveH5UserId(request);
		List<Map<String, Object>> data =
				distributorWhiteListMemberShopListService.listShopsForMember(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/is_valid", name = "验证分销商id是否有效")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorIsValid(
			HttpServletRequest request,
			@RequestParam(name = "lng", required = false) String lngParam,
			@RequestParam(name = "lat", required = false) String latParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "show_type", required = false) String showType,
			@RequestParam(name = "show_score", required = false) String showScoreRaw,
			@RequestParam(name = "show_marketing_activity", required = false) String showMarketingRaw,
			@RequestParam(name = "show_sales_count", required = false) String showSalesCountRaw,
			@RequestParam(name = "isNostores", required = false) Integer isNostoresRaw,
			@RequestParam(name = "cart_type", required = false) String cartTypeRaw,
			@RequestParam(name = "order_type", required = false) String orderTypeRaw,
			@RequestParam(name = "seckill_id", required = false) String seckillIdRaw,
			@RequestParam(name = "seckill_ticket", required = false) String seckillTicketRaw,
			@RequestParam(name = "iscrossborder", required = false) String iscrossborderRaw,
			@RequestParam(name = "bargain_id", required = false) String bargainIdRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		long userId = resolveH5UserId(request);
		long filterCompanyId = companyId;
		long authCompanyId = companyId;
		DistributorIsValidQuery query =
				DistributorIsValidQuery.of(
						lngParam,
						latParam,
						distributorIdRaw,
						showType,
						showScoreRaw,
						showMarketingRaw,
						showSalesCountRaw,
						isNostoresRaw,
						cartTypeRaw,
						orderTypeRaw,
						seckillIdRaw,
						seckillTicketRaw,
						iscrossborderRaw,
						bargainIdRaw);
		Map<String, Object> data =
				distributorH5GetDistributorIsValidService.getDistributorIsValid(
						companyId, userId, filterCompanyId, authCompanyId, query);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/list", name = "获取店铺列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "10") String pageSizeRaw,
			@RequestParam(name = "lng", required = false) String lng,
			@RequestParam(name = "lat", required = false) String lat,
			@RequestParam(name = "province", required = false) String province,
			@RequestParam(name = "city", required = false) String city,
			@RequestParam(name = "area", required = false) String area,
			@RequestParam(name = "address", required = false) String address,
			@RequestParam(name = "type", required = false, defaultValue = "0") String typeRaw,
			@RequestParam(name = "show_tag", required = false, defaultValue = "1") String showTagRaw,
			@RequestParam(name = "show_discount", required = false, defaultValue = "0") String showDiscountRaw,
			@RequestParam(name = "show_marketing_activity", required = false, defaultValue = "0") String showMarketingActivityRaw,
			@RequestParam(name = "show_sales_count", required = false, defaultValue = "1") String showSalesCountRaw,
			@RequestParam(name = "show_score", required = false, defaultValue = "0") String showScoreRaw,
			@RequestParam(name = "show_items", required = false, defaultValue = "0") String showItemsRaw,
			@RequestParam(name = "item_tag_id", required = false) String itemTagIdRaw,
			@RequestParam(name = "search_type", required = false, defaultValue = "1") String searchTypeRaw,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "distributorIds", required = false) List<String> distributorIds,
			@RequestParam(name = "is_ziti", required = false) Integer isZiti,
			@RequestParam(name = "is_delivery", required = false) Integer isDelivery,
			@RequestParam(name = "is_dada", required = false) Integer isDada,
			@RequestParam(name = "distributor_category_id", required = false) String distributorCategoryIdRaw,
			@RequestParam(name = "exclude_distributor_id", required = false) String excludeDistributorIdRaw,
			@RequestParam(name = "is_valid", required = false) String isValidRaw,
			@RequestParam(name = "get_shop", required = false) String getShopRaw,
			@RequestParam(name = "card_id", required = false) String cardId,
			@RequestParam(name = "isNostores", required = false, defaultValue = "0") String isNostoresRaw,
			@RequestParam(name = "cart_type", required = false, defaultValue = "cart") String cartType,
			@RequestParam(name = "order_type", required = false, defaultValue = "service") String orderType,
			@RequestParam(name = "seckill_id", required = false, defaultValue = "") String seckillId,
			@RequestParam(name = "seckill_ticket", required = false, defaultValue = "") String seckillTicket,
			@RequestParam(name = "iscrossborder", required = false, defaultValue = "") String iscrossborder,
			@RequestParam(name = "bargain_id", required = false, defaultValue = "0") String bargainIdRaw,
			@RequestParam(name = "distributor_tag_id", required = false) String distributorTagIdRaw,
			@RequestParam(name = "sort_type", required = false, defaultValue = "0") String sortTypeRaw,
			@RequestParam(name = "show_type", required = false, defaultValue = "") String showType) {
		long companyId = requirePositiveH5CompanyId(request);
		long userId = resolveH5UserId(request);
		String requestLangTag = RequestLangTag.current(langueProperties);
		DistributorWxappShopListQuery q = buildWxappShopListQuery(
				pageRaw,
				pageSizeRaw,
				lng,
				lat,
				province,
				city,
				area,
				address,
				typeRaw,
				showTagRaw,
				showDiscountRaw,
				showMarketingActivityRaw,
				showSalesCountRaw,
				showScoreRaw,
				showItemsRaw,
				itemTagIdRaw,
				searchTypeRaw,
				name,
				distributorIdRaw,
				distributorIds,
				isZiti,
				isDelivery,
				isDada,
				distributorCategoryIdRaw,
				excludeDistributorIdRaw,
				isValidRaw,
				getShopRaw,
				cardId,
				isNostoresRaw,
				cartType,
				orderType,
				seckillId,
				seckillTicket,
				iscrossborder,
				bargainIdRaw,
				distributorTagIdRaw,
				sortTypeRaw,
				showType);
		Map<String, Object> body = distributorH5GetDistributorListService.getDistributorList(companyId, userId, q, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static DistributorWxappShopListQuery buildWxappShopListQuery(
			String pageRaw,
			String pageSizeRaw,
			String lng,
			String lat,
			String province,
			String city,
			String area,
			String address,
			String typeRaw,
			String showTagRaw,
			String showDiscountRaw,
			String showMarketingActivityRaw,
			String showSalesCountRaw,
			String showScoreRaw,
			String showItemsRaw,
			String itemTagIdRaw,
			String searchTypeRaw,
			String name,
			String distributorIdRaw,
			List<String> distributorIds,
			Integer isZiti,
			Integer isDelivery,
			Integer isDada,
			String distributorCategoryIdRaw,
			String excludeDistributorIdRaw,
			String isValidRaw,
			String getShopRaw,
			String cardId,
			String isNostoresRaw,
			String cartType,
			String orderType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainIdRaw,
			String distributorTagIdRaw,
			String sortTypeRaw,
			String showType) {
		int page = parseIntLoose(pageRaw, 1);
		int pageSize = parseIntLoose(pageSizeRaw, 10);
		int type = parseIntLoose(typeRaw, 0);
		int showTag = parseIntLoose(showTagRaw, 1);
		int showDiscount = parseIntLoose(showDiscountRaw, 0);
		int showMarketingActivity = parseIntLoose(showMarketingActivityRaw, 0);
		int showSalesCount = parseIntLoose(showSalesCountRaw, 1);
		int showScore = parseIntLoose(showScoreRaw, 0);
		int showItems = parseIntLoose(showItemsRaw, 0);
		int searchType = parseIntLoose(searchTypeRaw, 1);
		int sortType = parseIntLoose(sortTypeRaw, 0);
		String bargainId = bargainIdRaw == null ? "0" : bargainIdRaw;
		List<String> dids = distributorIds == null ? List.of() : distributorIds;
		return new DistributorWxappShopListQuery(
				page,
				pageSize,
				lng,
				lat,
				province,
				city,
				area,
				address,
				type,
				showTag,
				showDiscount,
				showMarketingActivity,
				showSalesCount,
				showScore,
				showItems,
				itemTagIdRaw,
				searchType,
				name,
				distributorIdRaw,
				dids,
				isZiti,
				isDelivery,
				isDada,
				distributorCategoryIdRaw,
				excludeDistributorIdRaw,
				isValidRaw,
				getShopRaw,
				cardId,
				isNostoresRaw,
				cartType,
				orderType,
				seckillId,
				seckillTicket,
				iscrossborder,
				bargainId,
				distributorTagIdRaw,
				sortType,
				showType);
	}

	@GetMapping(value = "/wxapp/distributor/category/list", name = "获取店铺分类列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorCategoryList(
			HttpServletRequest request,
			@RequestParam(value = "company_id", required = false) String companyIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "category_name", required = false) String categoryName) {
		long companyId = resolveCompanyIdForCategoryList(request, companyIdRaw);
		if (companyId <= 0L) {
			throw new ResourceException("公司ID不能为空");
		}
		int page = parseIntLoose(pageRaw, 1);
		int pageSize = parseIntLoose(pageSizeRaw, 20);
		Map<String, Object> data = distributorCategoryService.list(companyId, page, pageSize, categoryName);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * 对齐 PHP：{@code GET /wxapp/distributor/salesperson/qrcode}（frontnoauth）。
	 */
	@GetMapping(value = "/wxapp/distributor/salesperson/qrcode", name = "获取单门店导购二维码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorSalespersonQrcode(
			HttpServletRequest request,
			@RequestParam(value = "company_id", required = false) String companyIdRaw,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		long companyId = resolveCompanyIdForCategoryList(request, companyIdRaw);
		if (companyId <= 0L) {
			throw new ResourceException("公司ID不能为空");
		}
		long distributorId = parseH5DistributorIdLoose(distributorIdRaw);
		if (distributorId <= 0L) {
			throw new ResourceException("店铺ID不能为空");
		}
		long userId = resolveH5UserId(request);
		String unionid = resolveH5Unionid(request);
		Map<String, Object> data =
				distributorH5GetSalespersonQrcodeService.getSalespersonQrcode(
						companyId, distributorId, userId, unionid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * 对齐 PHP：{@code auth.company_id} 优先，否则 query {@code company_id}。
	 */
	private static long resolveCompanyIdForCategoryList(HttpServletRequest request, String companyIdRaw) {
		Long fromAuth = readNullableH5CompanyIdFromRequestAttributes(request);
		if (fromAuth != null && fromAuth > 0L) {
			return fromAuth;
		}
		if (companyIdRaw != null && StringUtils.hasText(companyIdRaw.trim())) {
			try {
				return Long.parseLong(companyIdRaw.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return fromAuth == null ? 0L : fromAuth;
	}

	private static String resolveH5Unionid(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			return "";
		}
		Object unionid = m.get("unionid");
		if (unionid == null) {
			return "";
		}
		String s = String.valueOf(unionid).trim();
		return StringUtils.hasText(s) ? s : "";
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/distributor/alllist", name = "店铺全量列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAllDistributorList(
			HttpServletRequest request,
			@RequestParam(value = "lng", required = false) String lng,
			@RequestParam(value = "lat", required = false) String lat,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "city", required = false) String city,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") String pageSizeRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		int page = 1;
		if (pageRaw != null && StringUtils.hasText(pageRaw.trim())) {
			try {
				page = Integer.parseInt(pageRaw.trim());
			} catch (NumberFormatException ignored) {
				page = 1;
			}
		}
		int pageSize = 10;
		if (pageSizeRaw != null && StringUtils.hasText(pageSizeRaw.trim())) {
			try {
				pageSize = Integer.parseInt(pageSizeRaw.trim());
			} catch (NumberFormatException ignored) {
				pageSize = 10;
			}
		}
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				distributorH5GetAllDistributorListService.getAllDistributorList(
						companyId, lng, lat, name, province, city, page, pageSize, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/distributor/get_all_distributor", name = "获取全部门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAllDistributor(
			HttpServletRequest request,
			@RequestParam(value = "regionauth_id", required = false, defaultValue = "0") String regionauthIdRaw,
			@RequestParam(value = "category_id", required = false, defaultValue = "0") String categoryIdRaw,
			@RequestParam(value = "first_letter", required = false) String firstLetter,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") String pageSizeRaw,
			@RequestParam(value = "sort_type", required = false, defaultValue = "0") String sortTypeRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		int regionauthId = parseIntLoose(regionauthIdRaw, 0);
		int categoryId = parseIntLoose(categoryIdRaw, 0);
		long distributorIdFilter = parseH5DistributorIdLoose(distributorIdRaw);
		int sortType = parseIntLoose(sortTypeRaw, 0);
		int page = parseIntLoose(pageRaw, 1);
		int pageSize = parseIntLoose(pageSizeRaw, 10);
		String countryCode = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				distributorH5GetAllDistributorMinimalService.getAllDistributor(
						companyId,
						regionauthId,
						categoryId,
						distributorIdFilter,
						firstLetter,
						sortType,
						page,
						pageSize,
						countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/distributor/self", name = "总部自提点店铺详情")
	public ResponseEntity<ApiResult<Object>> getDistributionSelfDetail(
			HttpServletRequest request) {
		Long authNullable = readNullableH5CompanyIdFromRequestAttributes(request);
		long companyIdForFilter = Optional.ofNullable(authNullable).orElse(1L);
		if (companyIdForFilter <= 0L) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> row =
				distributorH5GetSelfShopDetailService.getSelfShopRow(companyIdForFilter, requestLangTag);
		if (row == null || row.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@GetMapping(value = "/wxapp/distributor/default", name = "默认店铺详情")
	public ResponseEntity<ApiResult<Object>> getDistributionDefaultDetail(HttpServletRequest request) {
		long companyId = resolveH5CompanyIdWithLegacyDefault(request);
		if (companyId <= 0L) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> row =
				distributorH5GetDistributionDefaultDetailService.getDistributionDefaultDetail(
						companyId, requestLangTag);
		if (row.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@SuppressWarnings("unused")
	@GetMapping(value = "/wxapp/distributor/deliverytype", name = "店铺配送方式")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getDeliveryType(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "lng", required = false) String lng,
			@RequestParam(value = "lat", required = false) String lat) {
		if (isDeliveryTypeQuickBranch(distributorIdRaw)) {
			List<Map<String, Object>> quick = new ArrayList<>();
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("delivery_name", "快递配送");
			one.put("delivery_type", "delivery");
			quick.add(one);
			return ResponseEntity.ok(ApiResult.ok(quick));
		}
		long companyId = requirePositiveH5CompanyId(request);
		long distributorIdForResolve;
		if (distributorIdRaw != null && StringUtils.hasText(distributorIdRaw.trim())) {
			try {
				distributorIdForResolve = Long.parseLong(distributorIdRaw.trim());
			} catch (NumberFormatException e) {
				distributorIdForResolve = 0L;
			}
		} else {
			distributorIdForResolve = 0L;
		}
		List<Map<String, Object>> data =
				distributorH5GetDeliveryTypeService.getDeliveryType(companyId, distributorIdForResolve);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/areainfo", name = "逆地址解析")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAreaInfo(
			HttpServletRequest request,
			@RequestParam(value = "lat", required = false) String lat,
			@RequestParam(value = "lng", required = false) String lng) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> data = distributorH5GetAreaInfoService.getAreaInfo(companyId, lat, lng);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.PLAIN,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/advertisements", name = "大屏首屏广告")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getAdvertisements(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		if (distributorIdRaw == null || !StringUtils.hasText(distributorIdRaw.trim())) {
			throw new BadRequestException(
					"参数错误.", Map.of("distributor_id", List.of("validation.required")));
		}
		String t = distributorIdRaw.trim();
		long distributorId;
		try {
			distributorId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					"参数错误.", Map.of("distributor_id", List.of("validation.integer")));
		}
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> payload =
				shopScreenAdvertisementStartAdsService.getAdvertisements(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(List.of(payload)));
	}

	@GetMapping(value = "/wxapp/distributor/slider", name = "大屏首页轮播")
	public ResponseEntity<ApiResult<Object>> getSlider(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		long distributorId = parseH5DistributorIdLoose(distributorIdRaw);
		Object data = distributorH5GetSliderService.getSlider(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.NONE,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/image/code", name = "图片验证码")
	public ResponseEntity<ApiResult<Map<String, String>>> getImageVcode(
			HttpServletRequest request, @RequestParam(value = "type", required = false, defaultValue = "bind") String type) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, String> data = distributorH5ShopCaptchaSmsService.generateImageVcode(companyId, type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/sms/code", name = "短信验证码")
	public ResponseEntity<ApiResult<Map<String, String>>> getSmsCode(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "yzm", required = false) String yzmRaw,
			@RequestParam(value = "token", required = false) String tokenRaw,
			@RequestParam(value = "type", required = false, defaultValue = "bind") String type) {
		long companyId = requirePositiveH5CompanyId(request);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, String> data =
				distributorH5ShopCaptchaSmsService.getSmsCode(
						companyId, distributorIdRaw, yzmRaw, tokenRaw, type, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/wxapp/distributor/sms/code", name = "验证短信验证码", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> checkSmsVcode(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requirePositiveH5CompanyId(request);
		Object did = body == null ? null : body.get("distributor_id");
		String distributorIdRaw = did == null ? null : String.valueOf(did).trim();
		Object vc = body == null ? null : body.get("vcode");
		String vcodeRaw = vc == null ? null : String.valueOf(vc).trim();
		Map<String, Object> data =
				distributorCheckSmsVcodeService.checkSmsVcode(companyId, distributorIdRaw, vcodeRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@SuppressWarnings("unused")
	@FrontNoAuth
	@GetMapping(value = "/wxapp/distributor/getDistributorInfo", name = "指定或默认门店信息")
	public ResponseEntity<ApiResult<Object>> getDistributorInfo(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "company_id", required = false) String companyIdRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		long parsedDistributorId = parseH5DistributorIdLoose(distributorIdRaw);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				distributorH5GetDistributorInfoService.getDistributorInfo(companyId, parsedDistributorId, requestLangTag);
		if (data.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.NONE,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/merchant/isvaild", name = "店铺关联商家是否可用")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> getDistributorMerchantIsvaild(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		long distributorId = parseH5DistributorIdLoose(distributorIdRaw);
		boolean status = distributorH5MerchantAvailableService.merchantAvailableForDistributor(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/wxapp/distributor/pickuplocation", name = "附近自提点")
	public ResponseEntity<ApiResult<Map<String, Object>>> getNearPickupLocation(
			HttpServletRequest request,
			@RequestParam(value = "lng", required = false) String lng,
			@RequestParam(value = "lat", required = false) String lat,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "isNostores", required = false) String isNostoresRaw,
			@RequestParam(value = "cart_type", required = false, defaultValue = "cart") String cartType,
			@RequestParam(value = "seckill_id", required = false, defaultValue = "") String seckillId,
			@RequestParam(value = "seckill_ticket", required = false, defaultValue = "") String seckillTicket,
			@RequestParam(value = "iscrossborder", required = false, defaultValue = "") String iscrossborder,
			@RequestParam(value = "bargain_id", required = false, defaultValue = "0") String bargainIdRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		long userId = resolveH5UserId(request);
		long distributorId = parseH5DistributorIdLoose(distributorIdRaw);
		Map<String, Object> data =
				distributorH5NearPickupLocationService.getNearPickupLocation(
						companyId,
						userId,
						lng,
						lat,
						distributorId,
						isNostoresRaw,
						cartType,
						seckillId,
						seckillTicket,
						iscrossborder,
						bargainIdRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.PLAIN,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@GetMapping(
			value = "/wxapp/distributor/aftersaleslocation",
			name = "附近退货点",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getNearAftersalesLocation(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "distributor_name", required = false) String distributorNameRaw,
			@RequestParam(value = "lng", required = false) String lngRaw,
			@RequestParam(value = "lat", required = false) String latRaw) {
		long companyId = requirePositiveH5CompanyId(request);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> data = distributorAftersalesAddressReadService.getNearAftersalesLocation(
				companyId, distributorIdRaw, distributorNameRaw, lngRaw, latRaw, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(
			value = "/wxapp/distributor/checkUserInWhite",
			name = "检查用户是否在白名单",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> checkUserValidInDistributor(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		long distributorId = parseH5DistributorIdLoose(distributorIdRaw);
		long companyId = requirePositiveH5CompanyId(request);
		long userId = resolveH5UserId(request);
		boolean status =
				distributorWhiteListCheckUserValidService.checkUserValidInDistributor(
						distributorId, userId, companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@GetMapping(value = "/wxapp/distributor/config/inRule", name = "店铺进店规则")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInRule(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		Map<String, Object> data = distributionStoreEntryRuleRedisService.getInRule(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/distributor/config/inRule/check", name = "验证进店规则", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> checkInRule(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requirePositiveH5CompanyId(request);
		long userId = resolveH5UserId(request);
		String workUserid = resolveWorkUseridFromBody(body);
		try {
			return ResponseEntity.ok(distributionCheckInRulePort.checkInRule(companyId, userId, workUserid));
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("status", false);
			err.put("msg", e.getMessage() != null ? e.getMessage() : "");
			err.put("data", Collections.emptyList());
			return ResponseEntity.ok(err);
		}
	}

	@GetMapping(value = "/wxapp/distributor/getAreaByAddress", name = "根据地址获取地区")
	public ResponseEntity<ApiResult<Object>> getAreaByAddress(
			HttpServletRequest request,
			@RequestParam(value = "address", required = false) String address) {
		if (address == null || address.isEmpty() || "0".equals(address)) {
			throw new BadRequestException("详细地址必填");
		}
		long companyId = requirePositiveH5CompanyId(request);
		Object data = distributorH5GetAreaByAddressService.getAreaByAddress(companyId, address);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/distributor/getAreaByJwd", name = "根据经纬度逆解析地区")
	public ResponseEntity<ApiResult<Object>> getAreaByJwd(
			HttpServletRequest request,
			@RequestParam(value = "lat", required = false) String lat,
			@RequestParam(value = "lng", required = false) String lng) {
		if (lat == null || lat.isEmpty() || "0".equals(lat) || lng == null || lng.isEmpty() || "0".equals(lng)) {
			throw new BadRequestException("详细地址必填");
		}
		long companyId = requirePositiveH5CompanyId(request);
		Object data = distributorH5GetAreaByJwdService.getAreaByJwd(companyId, lat, lng);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
