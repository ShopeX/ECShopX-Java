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

package cn.shopex.ecshopx.employeepurchase.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseBehaviorReportService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemDetailService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemListService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsCategoryRedisService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityListService;
import cn.shopex.ecshopx.employeepurchase.service.FrontActivityDetailService;
import cn.shopex.ecshopx.employeepurchase.service.InternalSaleEligibilityService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("employeepurchaseActivityFrontV1")
@RequestMapping("/api/v1/h5app")
public class ActivityController {

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final EmployeePurchaseActivityListService employeePurchaseActivityListService;
	private final EmployeePurchaseActivityItemDetailService employeePurchaseActivityItemDetailService;
	private final EmployeePurchaseActivityItemListService employeePurchaseActivityItemListService;
	private final EmployeePurchaseActivityItemsCategoryRedisService
			employeePurchaseActivityItemsCategoryRedisService;
	private final CompanysActivationService companysActivationService;
	private final PassphraseBehaviorReportService passphraseBehaviorReportService;
	private final InternalSaleEligibilityService internalSaleEligibilityService;
	private final FrontActivityDetailService frontActivityDetailService;

	public ActivityController(
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			EmployeePurchaseActivityListService employeePurchaseActivityListService,
			EmployeePurchaseActivityItemDetailService employeePurchaseActivityItemDetailService,
			EmployeePurchaseActivityItemListService employeePurchaseActivityItemListService,
			EmployeePurchaseActivityItemsCategoryRedisService
					employeePurchaseActivityItemsCategoryRedisService,
			CompanysActivationService companysActivationService,
			PassphraseBehaviorReportService passphraseBehaviorReportService,
			InternalSaleEligibilityService internalSaleEligibilityService,
			FrontActivityDetailService frontActivityDetailService) {
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.employeePurchaseActivityListService = employeePurchaseActivityListService;
		this.employeePurchaseActivityItemDetailService = employeePurchaseActivityItemDetailService;
		this.employeePurchaseActivityItemListService = employeePurchaseActivityItemListService;
		this.employeePurchaseActivityItemsCategoryRedisService =
				employeePurchaseActivityItemsCategoryRedisService;
		this.companysActivationService = companysActivationService;
		this.passphraseBehaviorReportService = passphraseBehaviorReportService;
		this.internalSaleEligibilityService = internalSaleEligibilityService;
		this.frontActivityDetailService = frontActivityDetailService;
	}

	@FrontNoAuth
	@PostMapping("/wxapp/employeepurchase/activity/behavior-report")
	public ResponseEntity<ApiResult<Map<String, Object>>> reportActivityBehavior(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeBehaviorReportInput(request, body);
		var claimsOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		if (claimsOpt.isPresent()) {
			Map<String, Object> claims = claimsOpt.get();
			if (isAccountDisabled(claims.get("disabled"))) {
				throw new UnauthorizedException("该账号已被禁用.");
			}
			merged.put("company_id", claims.get("company_id"));
			merged.put("user_id", resolveH5UserId(request, claims, merged));
		} else {
			Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
			if (companyAttr != null) {
				merged.putIfAbsent("company_id", companyAttr);
			}
		}
		Map<String, Object> data = passphraseBehaviorReportService.report(merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employeepurchase/is_open")
	public ResponseEntity<ApiResult<Map<String, Object>>> isOpen() {
		Map<String, Object> applications = companysActivationService.getApplications();
		Object raw = applications.get("employee_purchase");
		boolean open;
		if (raw == null) {
			open = false;
		} else if (raw instanceof Boolean b) {
			open = b.booleanValue();
		} else {
			open = Boolean.parseBoolean(raw.toString().trim());
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("is_open", open);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employeepurchase/internal-sale-eligibility")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInternalSaleEligibility(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long activityId = parseItemDetailRequiredPositiveQuery(activityIdRaw, "活动ID必填");
		long enterpriseId = parseItemDetailRequiredPositiveQuery(enterpriseIdRaw, "企业ID必填");

		Map<String, Object> data =
				internalSaleEligibilityService.getInternalSaleEligibility(
						companyId, userId, activityId, enterpriseId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping("/wxapp/employeepurchase/activity/detail")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityDetail(
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "company_id", required = false) String companyIdRaw) {
		long activityId = parseItemDetailRequiredPositiveQuery(activityIdRaw, "活动ID必填");
		long companyId = parseItemDetailRequiredPositiveQuery(companyIdRaw, "公司ID必填");
		Map<String, Object> data = frontActivityDetailService.getActivityDetail(companyId, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employeepurchase/activities")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(
			HttpServletRequest request,
			@RequestParam(value = "activity_name", required = false) String activityName,
			@RequestParam(value = "enterprise_id", required = false, defaultValue = "0")
					String enterpriseIdRaw,
			@RequestParam(value = "need_aggregate", required = false) String needAggregate,
			@RequestParam(value = "activity_id", required = false) Long activityId,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);
		long enterpriseId = parseLongFlexible(enterpriseIdRaw, 0L);

		Map<String, Object> payload =
				employeePurchaseActivityListService.getActivityList(
						companyId,
						userId,
						activityName,
						enterpriseId,
						needAggregate,
						activityId,
						status,
						page,
						pageSize);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@GetMapping("/wxapp/employeepurchase/activity/items")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityItemList(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize,
			@RequestParam(value = "main_cat_id", required = false) Long mainCatId,
			@RequestParam(value = "category", required = false) @SuppressWarnings("unused") Long category,
			@RequestParam(value = "cat_id", required = false) @SuppressWarnings("unused") Long catId,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "keywords", required = false) String keywords,
			@RequestParam(value = "goodsSort", required = false) Integer goodsSort,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") long distributorIdForVisibility) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		parseUserIdFromClaims(claims);

		long activityId = parseItemDetailRequiredPositiveQuery(activityIdRaw, "活动ID必填");

		Map<String, Object> data =
				employeePurchaseActivityItemListService.getActivityItemListForFront(
						companyId,
						activityId,
						page,
						pageSize,
						mainCatId,
						itemName,
						itemBn,
						keywords,
						goodsSort,
						distributorIdForVisibility > 0L);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employeepurchase/activity/item/{item_id}")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityItemDetail(
			HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		parseCompanyIdFromClaims(claims);

		long enterpriseId = parseItemDetailRequiredPositiveQuery(enterpriseIdRaw, "企业ID必填");
		long activityId = parseItemDetailRequiredPositiveQuery(activityIdRaw, "活动ID必填");
		long itemIdLong = parseItemDetailPathItemId(itemId);

		Map<String, Object> data =
				employeePurchaseActivityItemDetailService.buildActivityItemDetail(
						request, claims, itemIdLong, enterpriseId, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employeepurchase/activity/items/category")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getActivityItemCategory(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "main_cat_id", required = false) @SuppressWarnings("unused")
					String mainCatId,
			@RequestParam(value = "cat_id", required = false) @SuppressWarnings("unused") String catId,
			@RequestParam(value = "item_name", required = false) @SuppressWarnings("unused")
					String itemName,
			@RequestParam(value = "item_bn", required = false) @SuppressWarnings("unused") String itemBn) {
		Map<String, Object> claims =
				h5BearerJwtClaimsService
						.verifyAndExtractClaims(request)
						.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseCompanyIdFromClaims(claims);
		long aid = parseItemDetailRequiredPositiveQuery(activityIdRaw, "活动ID必填");

		List<Map<String, Object>> data =
				employeePurchaseActivityItemsCategoryRedisService.fetchActivityItemsCategory(
						companyId, aid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object raw = claims.get("company_id");
		if (raw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long v = Long.parseLong(raw.toString().trim());
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object claimUid = claims.get("user_id");
		if (claimUid == null || !authUserIdTruthy(claimUid)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long uid = parseLongFlexible(claimUid, 0L);
		if (uid <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return uid;
	}

	private static boolean isAccountDisabled(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean authUserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseItemDetailRequiredPositiveQuery(String raw, String message) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new ResourceException(message);
		}
		try {
			long v = Long.parseLong(raw.trim());
			if (v <= 0L) {
				throw new ResourceException(message);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
	}

	private static long parseItemDetailPathItemId(String itemIdRaw) {
		if (itemIdRaw == null || !StringUtils.hasText(itemIdRaw.trim())) {
			throw new ResourceException("商品ID必填");
		}
		try {
			long v = Long.parseLong(itemIdRaw.trim());
			if (v < 1L) {
				throw new ResourceException("商品ID必填");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("商品ID必填");
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static Map<String, Object> mergeBehaviorReportInput(
			HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, String[]> paramMap = request.getParameterMap();
		for (Map.Entry<String, String[]> e : paramMap.entrySet()) {
			String[] vals = e.getValue();
			if (vals != null && vals.length > 0) {
				merged.putIfAbsent(e.getKey(), vals[0]);
			}
		}
		return merged;
	}

	private long resolveH5UserId(
			HttpServletRequest request, Map<String, Object> claims, Map<String, Object> merged) {
		Object fromClaims = claims.get("user_id");
		long userId = parseLongFlexible(fromClaims, 0L);
		if (userId > 0L) {
			return userId;
		}
		Object fromMerged = merged.get("user_id");
		return parseLongFlexible(fromMerged, 0L);
	}
}
