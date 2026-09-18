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

package cn.shopex.ecshopx.salesperson.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.QywxSalespersonAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.QywxSalespersonAuthAttributes;
import cn.shopex.ecshopx.salesperson.service.SalemanShopListService;
import cn.shopex.ecshopx.salesperson.service.SalespersonRelShopListService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonAdminAddService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonAdminUpdateService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonBindUserService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonAdminSalespersoninfoService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonAdminSalespersonlistService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonAdminStoremanagerinfoService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonBrokerageStaticListService;
import cn.shopex.ecshopx.salesperson.service.ShopSalespersonCheckDistributorIsValidService;
import cn.shopex.ecshopx.salesperson.web.ShopSalespersonFrontRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("salespersonShopFrontV1")
@RequestMapping("/api/v1/h5app")
public class ShopSalespersonController {

	private final ShopSalespersonBindUserService shopSalespersonBindUserService;
	private final ShopSalespersonAdminAddService shopSalespersonAdminAddService;
	private final ShopSalespersonAdminUpdateService shopSalespersonAdminUpdateService;
	private final ShopSalespersonCheckDistributorIsValidService shopSalespersonCheckDistributorIsValidService;
	private final SalespersonRelShopListService salespersonRelShopListService;
	private final SalemanShopListService salemanShopListService;
	private final ShopSalespersonBrokerageStaticListService shopSalespersonBrokerageStaticListService;
	private final ShopSalespersonAdminSalespersoninfoService shopSalespersonAdminSalespersoninfoService;
	private final ShopSalespersonAdminSalespersonlistService shopSalespersonAdminSalespersonlistService;
	private final ShopSalespersonAdminStoremanagerinfoService shopSalespersonAdminStoremanagerinfoService;

	public ShopSalespersonController(ShopSalespersonBindUserService shopSalespersonBindUserService,
			ShopSalespersonAdminAddService shopSalespersonAdminAddService,
			ShopSalespersonAdminUpdateService shopSalespersonAdminUpdateService,
			ShopSalespersonCheckDistributorIsValidService shopSalespersonCheckDistributorIsValidService,
			SalespersonRelShopListService salespersonRelShopListService,
			SalemanShopListService salemanShopListService,
			ShopSalespersonBrokerageStaticListService shopSalespersonBrokerageStaticListService,
			ShopSalespersonAdminSalespersoninfoService shopSalespersonAdminSalespersoninfoService,
			ShopSalespersonAdminSalespersonlistService shopSalespersonAdminSalespersonlistService,
			ShopSalespersonAdminStoremanagerinfoService shopSalespersonAdminStoremanagerinfoService) {
		this.shopSalespersonBindUserService = shopSalespersonBindUserService;
		this.shopSalespersonAdminAddService = shopSalespersonAdminAddService;
		this.shopSalespersonAdminUpdateService = shopSalespersonAdminUpdateService;
		this.shopSalespersonCheckDistributorIsValidService = shopSalespersonCheckDistributorIsValidService;
		this.salespersonRelShopListService = salespersonRelShopListService;
		this.salemanShopListService = salemanShopListService;
		this.shopSalespersonBrokerageStaticListService = shopSalespersonBrokerageStaticListService;
		this.shopSalespersonAdminSalespersoninfoService = shopSalespersonAdminSalespersoninfoService;
		this.shopSalespersonAdminSalespersonlistService = shopSalespersonAdminSalespersonlistService;
		this.shopSalespersonAdminStoremanagerinfoService = shopSalespersonAdminStoremanagerinfoService;
	}

	@GetMapping(value = "/wxapp/salesperson/salemanShopList", name = "业务员店铺列表")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Object>> salemanShopList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "500") int pageSize,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "name", required = false) String name) {
		H5CompanyAndMember ctx = parseH5CompanyAndMemberForSalemanShopList(request);
		Object body = salemanShopListService.salemanShopList(ctx.companyId(), ctx.memberUserId(), page, pageSize, mobile,
				name);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private record H5CompanyAndMember(long companyId, long memberUserId) {
	}

	@SuppressWarnings("unchecked")
	private static H5CompanyAndMember parseH5CompanyAndMemberForSalemanShopList(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		long companyId = parsePositiveLongClaim(claims, "company_id");
		long memberUserId = parsePositiveLongClaim(claims, "user_id");
		return new H5CompanyAndMember(companyId, memberUserId);
	}

	private static long parsePositiveLongClaim(Map<String, Object> claims, String key) {
		Object v = claims.get(key);
		if (v == null) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			if (x <= 0L) {
				throw new BadRequestException("导购更新用户信息错误");
			}
			return x;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		try {
			long x = Long.parseLong(s);
			if (x <= 0L) {
				throw new BadRequestException("导购更新用户信息错误");
			}
			return x;
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购更新用户信息错误");
		}
	}

	@PostMapping(value = "/wxapp/salesperson/bindusersalesperson", name = "业务员关系绑定24小时有效")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> bindusersalesperson(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long memberUserId = parseMemberUserIdFromRequest(request);
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		LinkedHashMap<String, Object> requestDataThreeKeys = new LinkedHashMap<>();
		requestDataThreeKeys.put("promoter_user_id", merged.get("promoter_user_id"));
		requestDataThreeKeys.put("promoter_shop_id", merged.get("promoter_shop_id"));
		requestDataThreeKeys.put("promoter_item_id", merged.get("promoter_item_id"));
		Map<String, Object> legacy = shopSalespersonBindUserService.bindusersalesperson(memberUserId, requestDataThreeKeys);
		return ResponseEntity.ok(ApiResult.ok(legacy));
	}

	@SuppressWarnings("unchecked")
	private static long parseMemberUserIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new ResourceException("导购更新用户信息错误");
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			throw new ResourceException("导购更新用户信息错误");
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException("导购更新用户信息错误");
			}
			return v;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new ResourceException("导购更新用户信息错误");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new ResourceException("导购更新用户信息错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("导购更新用户信息错误");
		}
	}

	@GetMapping(value = "/wxapp/salespersonadmin/storemanagerinfo", name = "查询管理信息")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> storemanagerinfo(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeQueryStringAndOptionalBody(request, body);
		Map<String, Object> payload = shopSalespersonAdminStoremanagerinfoService.storemanagerinfo(request, merged);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@PostMapping(value = "/wxapp/salespersonadmin/addsalesperson", name = "增加业务员")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> addsalesperson(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		putH5UserIdIntoMergedForInout(request, merged);
		Map<String, Object> legacy = shopSalespersonAdminAddService.addsalesperson(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(legacy));
	}

	/** Merges H5 JWT {@code user_id} into the input map so the legacy envelope {@code inoutData} lists it. */
	private static void putH5UserIdIntoMergedForInout(HttpServletRequest request, Map<String, Object> merged) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> claims)) {
			return;
		}
		Object uid = claims.get("user_id");
		if (uid == null) {
			return;
		}
		if (uid instanceof Number n) {
			merged.put("user_id", String.valueOf(n.longValue()));
		} else {
			String s = uid.toString().trim();
			if (StringUtils.hasText(s)) {
				merged.put("user_id", s);
			}
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

	@PostMapping(value = "/wxapp/salespersonadmin/updatesalesperson", name = "更新业务员")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> updatesalesperson(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		putH5UserIdIntoMergedForInout(request, merged);
		Map<String, Object> legacy = shopSalespersonAdminUpdateService.updatesalesperson(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(legacy));
	}

	@GetMapping(value = "/wxapp/salespersonadmin/salespersonlist", name = "查询业务员列表")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<?> salespersonlist(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeQueryStringAndOptionalBody(request, body);
		if (request.getParameterMap().containsKey("distributor_id") && !merged.containsKey("distributor_id")) {
			merged.put("distributor_id", "");
		}
		Map<String, Object> legacy = shopSalespersonAdminSalespersonlistService.salespersonlist(companyId, merged);
		Object inner = legacy.get("data");
		if (inner instanceof Map<?, ?> listdataRaw) {
			@SuppressWarnings("unchecked")
			Map<String, Object> listdata = (Map<String, Object>) listdataRaw;
			Object listObj = listdata.get("list");
			List<?> rows = listObj instanceof List<?> l ? l : null;
			Object tcObj = listdata.get("total_count");
			long tc = 0L;
			if (tcObj instanceof Number n) {
				tc = n.longValue();
			} else if (tcObj != null) {
				try {
					tc = Long.parseLong(String.valueOf(tcObj).trim());
				} catch (NumberFormatException e) {
					tc = 0L;
				}
			}
			if ((rows == null || rows.isEmpty()) && tc == 0L) {
				return ResponseEntity.ok(Collections.singletonMap("data", Collections.emptyList()));
			}
		}
		return ResponseEntity.ok(ApiResult.ok(legacy));
	}

	@GetMapping("/wxapp/salespersonadmin/salespersoninfo")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> salespersoninfo(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeQueryStringAndOptionalBody(request, body);
		Map<String, Object> legacy = shopSalespersonAdminSalespersoninfoService.salespersoninfo(companyId, merged,
				request);
		return ResponseEntity.ok(ApiResult.ok(legacy));
	}

	@GetMapping(value = "/wxapp/salespersonadmin/brokagestaticlist", name = "查询业绩统计")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> brokagestaticlist(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		Map<String, Object> authClaims = parseH5AuthClaimsForBrokerageStaticList(request);
		Map<String, Object> merged = ShopSalespersonFrontRequestMerge.mergeQueryStringAndOptionalBody(request, body);
		if (request.getParameterMap().containsKey("distributor_id") && !merged.containsKey("distributor_id")) {
			merged.put("distributor_id", "");
		}
		Map<String, Object> legacy = shopSalespersonBrokerageStaticListService.brokagestaticlist(companyId, authClaims,
				merged);
		return ResponseEntity.ok(ApiResult.ok(legacy));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parseH5AuthClaimsForBrokerageStaticList(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw == null || !(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		Map<String, Object> authClaims = (Map<String, Object>) rawMap;
		parsePositiveLongClaim(authClaims, "company_id");
		return authClaims;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@QywxSalespersonAuth
	@GetMapping(value = "/wxapp/salesperson/distributorlist", name = "获取导购店铺列表")
	public ResponseEntity<Map<String, Object>> getDistributorDataList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "500") int pageSize,
			@RequestParam(value = "store_type", required = false) String storeType,
			@RequestParam(value = "store_name", required = false) String storeName) {
		long companyId = parseQywxCompanyIdFromRequest(request);
		long salespersonId = parseQywxSalespersonIdFromRequest(request);
		String salespersonIdStr = String.valueOf(salespersonId);
		String raw = storeType;
		String t = (raw == null) ? "" : raw.trim();
		String storeTypeParam = t.isEmpty() ? "distributor" : t;
		Map<String, Object> body = salespersonRelShopListService.getDistributorDataList(companyId, salespersonIdStr,
				storeTypeParam, storeName, page, pageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@QywxSalespersonAuth
	@GetMapping(value = "/wxapp/salesperson/distributor/is_valid", name = "校验导购员的店铺信息")
	public ResponseEntity<Map<String, Object>> checkDistributorIsValid(
			HttpServletRequest request,
			@RequestParam(value = "salesperson_id", required = false) String salespersonIdRaw,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		if (salespersonIdRaw == null || !StringUtils.hasText(salespersonIdRaw.trim())) {
			throw new BadRequestException("导购员id不能为空");
		}
		if (distributorIdRaw == null || !StringUtils.hasText(distributorIdRaw.trim())) {
			throw new BadRequestException("店铺id不能为空");
		}
		long companyId = parseQywxCompanyIdFromRequest(request);
		boolean status = shopSalespersonCheckDistributorIsValidService.checkDistributorIsValid(companyId,
				salespersonIdRaw.trim(), distributorIdRaw.trim());
		return ResponseEntity.ok(Map.of("data", Map.of("status", status)));
	}

	@SuppressWarnings("unchecked")
	private static long parseQywxSalespersonIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购身份无效");
		}
		Map<String, Object> auth = (Map<String, Object>) rawMap;
		Object sid = auth.get("salesperson_id");
		if (sid == null) {
			throw new BadRequestException("导购身份无效");
		}
		if (sid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("导购身份无效");
			}
			return v;
		}
		String s = sid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("导购身份无效");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new BadRequestException("导购身份无效");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购身份无效");
		}
	}

	@SuppressWarnings("unchecked")
	private static long parseQywxCompanyIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("导购身份无效");
		}
		Map<String, Object> auth = (Map<String, Object>) rawMap;
		Object cid = auth.get("company_id");
		if (cid == null) {
			throw new BadRequestException("企业信息有误");
		}
		if (cid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("企业信息有误");
			}
			return v;
		}
		String s = cid.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("企业信息有误");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new BadRequestException("企业信息有误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业信息有误");
		}
	}
}
