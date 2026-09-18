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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.PrivacySettingData;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.SetDomainSettingData;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.UpdateCompanyInfoData;
import cn.shopex.ecshopx.companys.service.CompanysInfoUpdateService;
import cn.shopex.ecshopx.companys.service.CompanysResourcesListService;
import cn.shopex.ecshopx.companys.service.domain.CompanysDomainSettingService;
import cn.shopex.ecshopx.companys.service.privacy.CompanysPrivacySettingService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivateInfoService;
import cn.shopex.ecshopx.companys.service.activation.CompanyLicenseCreateService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.companys.service.setting.CompanySettingReadService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("companysAdminV1Companys")
@RequestMapping("/api/v1")
public class CompanysController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final CompanysActivationService companysActivationService;
	private final CompanysInfoUpdateService companysInfoUpdateService;
	private final CompanyLicenseCreateService companyLicenseCreateService;
	private final CompanysDomainSettingService companysDomainSettingService;
	private final CompanysPrivacySettingService companysPrivacySettingService;
	private final CompanysActivateInfoService companysActivateInfoService;
	private final CompanysResourcesListService companysResourcesListService;
	private final CompanySettingReadService companySettingReadService;

	public CompanysController(
			CompanysActivationService companysActivationService,
			CompanysInfoUpdateService companysInfoUpdateService,
			CompanyLicenseCreateService companyLicenseCreateService,
			CompanysDomainSettingService companysDomainSettingService,
			CompanysPrivacySettingService companysPrivacySettingService,
			CompanysActivateInfoService companysActivateInfoService,
			CompanysResourcesListService companysResourcesListService,
			CompanySettingReadService companySettingReadService) {
		this.companysActivationService = companysActivationService;
		this.companysInfoUpdateService = companysInfoUpdateService;
		this.companyLicenseCreateService = companyLicenseCreateService;
		this.companysDomainSettingService = companysDomainSettingService;
		this.companysPrivacySettingService = companysPrivacySettingService;
		this.companysActivateInfoService = companysActivateInfoService;
		this.companysResourcesListService = companysResourcesListService;
		this.companySettingReadService = companySettingReadService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@PostMapping(value = "/company/activate", name = "系统激活")
	public ResponseEntity<ApiResult<Map<String, Object>>> active(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			throw new ResourceException("授权信息有误");
		}
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> params = new LinkedHashMap<>();
		for (String k : new String[] {"active_code", "shop_num", "source", "available_days"}) {
			if (merged.containsKey(k)) {
				params.put(k, merged.get(k));
			}
		}
		Map<String, Object> result = companyLicenseCreateService.createCompanyLicense(params, jwt);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/company/activate", name = "获取激活信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivateInfo(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			throw new ResourceException("授权信息有误");
		}
		Object v = jwt.get("company_id");
		if (v == null) {
			throw new ResourceException("授权信息有误");
		}
		long companyId;
		try {
			companyId = v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("授权信息有误");
		}
		if (companyId <= 0L) {
			throw new ResourceException("授权信息有误");
		}
		Map<String, Object> body = companysActivateInfoService.getActivateInfo(companyId, request);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/company/applications", name = "获取授权应用")
	public ResponseEntity<ApiResult<Map<String, Object>>> getApplications() {
		return ResponseEntity.ok(ApiResult.ok(companysActivationService.getApplications()));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "company.resources")
	@GetMapping(value = "/company/resources", name = "获取当前可用资源包列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getResourceList(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			throw new ResourceException("授权信息有误");
		}
		long companyId = requireCompanyIdForCompanyResource(jwt);
		int page = parsePageNo(request);
		int limit = parsePageSize(request);
		boolean isValidFilter = queryTokenLooksPresent(request.getParameter("is_valid"));
		Map<String, String[]> paramMap = request.getParameterMap();
		boolean hasLeftShopMin = paramMap.containsKey("left_shop_min");
		boolean hasLeftShopMax = paramMap.containsKey("left_shop_max");
		CompanysResourcesListService.LeftShopFilterMode leftShopMode;
		Integer leftShopMinValue = null;
		Integer leftShopMaxValue = null;
		if (hasLeftShopMin && hasLeftShopMax) {
			leftShopMode = CompanysResourcesListService.LeftShopFilterMode.BOTH_MIN_MAX;
			try {
				leftShopMinValue = parseRequiredIntParam(request.getParameter("left_shop_min"));
				leftShopMaxValue = parseRequiredIntParam(request.getParameter("left_shop_max"));
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数格式错误");
			}
		} else if (hasLeftShopMax && !hasLeftShopMin) {
			leftShopMode = CompanysResourcesListService.LeftShopFilterMode.MAX_ONLY;
			try {
				leftShopMaxValue = parseRequiredIntParam(request.getParameter("left_shop_max"));
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数格式错误");
			}
		} else if (hasLeftShopMin && !hasLeftShopMax) {
			leftShopMode = CompanysResourcesListService.LeftShopFilterMode.MIN_ONLY;
			try {
				leftShopMinValue = parseRequiredIntParam(request.getParameter("left_shop_min"));
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数格式错误");
			}
		} else {
			leftShopMode = CompanysResourcesListService.LeftShopFilterMode.NONE;
		}
		boolean leftDaysFilter = queryTokenLooksPresent(request.getParameter("leftDays"));
		Integer leftDaysParsed = null;
		if (leftDaysFilter) {
			try {
				int d = parseRequiredIntParam(request.getParameter("leftDays"));
				if (d <= 0) {
					throw new BadRequestException("参数格式错误");
				}
				leftDaysParsed = d;
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数格式错误");
			}
		}
		Map<String, Object> body = companysResourcesListService.getResourceList(
				companyId,
				page,
				limit,
				isValidFilter,
				leftShopMode,
				leftShopMinValue,
				leftShopMaxValue,
				leftDaysFilter,
				leftDaysParsed);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "companys.setting")
	@GetMapping(value = "/companys/setting", name = "获取商品配置信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCompanySetting(
			HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCode) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			throw new ResourceException("授权信息有误");
		}
		Object v = jwt.get("company_id");
		if (v == null) {
			throw new ResourceException("授权信息有误");
		}
		long companyId;
		try {
			companyId = v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("授权信息有误");
		}
		if (companyId <= 0L) {
			throw new ResourceException("授权信息有误");
		}
		String lang = !StringUtils.hasText(countryCode) ? "zh-CN" : countryCode.trim();
		return ResponseEntity.ok(ApiResult.ok(companySettingReadService.getCompanySetting(companyId, lang)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "companys.setting.setp")
	@PostMapping(value = "/company/privacy_setting", name = "隐私更新")
	public ApiResult<PrivacySettingData> setPrivacySetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			jwt = Map.of();
		}
		long companyId = requireCompanyIdForCompanyResource(jwt);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object countryCode = merged.get("country_code");
		String lang;
		if (countryCode != null) {
			String s = countryCode.toString().trim();
			lang = StringUtils.hasText(s) ? s : "zh-CN";
		} else {
			lang = "zh-CN";
		}
		Map<String, Object> patch = new LinkedHashMap<>();
		for (String k : new String[] {"pc_privacy_content", "h5_privacy_content"}) {
			if (merged.containsKey(k) && merged.get(k) != null) {
				patch.put(k, merged.get(k));
			}
		}
		PrivacySettingData out = companysPrivacySettingService.setPrivacySetting(companyId, lang, patch);
		return ApiResult.ok(out);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "companys.setting.getp")
	@GetMapping(value = "/company/privacy_setting", name = "隐私获取")
	public ResponseEntity<ApiResult<PrivacySettingData>> getPrivacySetting(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			throw new ResourceException("授权信息有误");
		}
		long companyId = requireCompanyIdForCompanyResource(jwt);
		String rawCountry = request.getParameter("country_code");
		final String lang;
		if (rawCountry != null) {
			String s = rawCountry.trim();
			lang = StringUtils.hasText(s) ? s : "zh-CN";
		} else {
			lang = "zh-CN";
		}
		PrivacySettingData out = companysPrivacySettingService.getPrivacySetting(companyId, lang);
		return ResponseEntity.ok(ApiResult.ok(out));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "companys.domain_setting.get")
	@GetMapping(value = "/company/domain_setting", name = "获取域名配置")
	public ResponseEntity<ApiResult<LinkedHashMap<String, Object>>> getDomainSetting(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			throw new ResourceException("授权信息有误");
		}
		long companyId = requireCompanyIdForCompanyResource(jwt);
		LinkedHashMap<String, Object> result = companysDomainSettingService.getDomainSetting(companyId);
		result.put("company_id", companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "companys.domain_setting.set")
	@PostMapping(value = "/company/domain_setting", name = "保存域名配置")
	public ApiResult<SetDomainSettingData> setDomainSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			jwt = Map.of();
		}
		long companyId = requireCompanyIdForCompanyResource(jwt);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		applyQueryEmptyDomainParams(request, merged);
		SetDomainSettingData out = companysDomainSettingService.setDomainSetting(companyId, merged);
		return ApiResult.ok(out);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "company.update")
	@PatchMapping(value = "/company", name = "更新企业信息")
	public ResponseEntity<ApiResult<UpdateCompanyInfoData>> updateCompanyInfo(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null) {
			jwt = Map.of();
		}
		long companyId = requireCompanyIdForCompanyResource(jwt);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		UpdateCompanyInfoData data = companysInfoUpdateService.updateCompanyInfo(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void applyQueryEmptyDomainParams(HttpServletRequest request, Map<String, Object> merged) {
		for (String name : new String[] {"pc_domain", "h5_domain"}) {
			if (request.getParameterMap().containsKey(name) && !merged.containsKey(name)) {
				String v = request.getParameter(name);
				merged.put(name, v != null ? v : "");
			}
		}
	}

	private static long requireCompanyIdForCompanyResource(Map<String, Object> jwt) {
		Object v = jwt.get("company_id");
		if (v == null) {
			throw new ResourceException("无相关企业信息！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new ResourceException("无相关企业信息！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("无相关企业信息！");
		}
	}

	private static boolean queryTokenLooksPresent(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static int parsePageNo(HttpServletRequest request) {
		String raw = request.getParameter("page_no");
		if (raw == null) {
			return 1;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return 1;
		}
		try {
			int p = Integer.parseInt(t);
			return p > 0 ? p : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(HttpServletRequest request) {
		String raw = request.getParameter("page_size");
		if (raw == null) {
			return 100000;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return 100000;
		}
		try {
			int lim = Integer.parseInt(t);
			if (lim <= 0) {
				return 100000;
			}
			return lim;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数格式错误");
		}
	}

	private static int parseRequiredIntParam(String raw) {
		if (raw == null) {
			throw new NumberFormatException();
		}
		return Integer.parseInt(raw.trim());
	}
}
