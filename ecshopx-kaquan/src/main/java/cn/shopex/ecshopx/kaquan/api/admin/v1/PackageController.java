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

package cn.shopex.ecshopx.kaquan.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageCreateService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageDetailQueryService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageEditService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageGradeLimitCheckService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageListQueryService;
import cn.shopex.ecshopx.kaquan.service.cardpackage.PackageReceivesLogQueryService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCreateRequestMergeService;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import cn.shopex.ecshopx.kaquan.web.VipGradeOrderDatapassSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("kaquanPackageAdminV1")
@RequestMapping("/api/v1")
public class PackageController {

	private final DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService;
	private final CardPackageCreateService cardPackageCreateService;
	private final CardPackageEditService cardPackageEditService;
	private final CardPackageGradeLimitCheckService cardPackageGradeLimitCheckService;
	private final CardPackageDetailQueryService cardPackageDetailQueryService;
	private final CardPackageListQueryService cardPackageListQueryService;
	private final PackageReceivesLogQueryService packageReceivesLogQueryService;

	public PackageController(DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService,
			CardPackageCreateService cardPackageCreateService, CardPackageEditService cardPackageEditService,
			CardPackageGradeLimitCheckService cardPackageGradeLimitCheckService,
			CardPackageDetailQueryService cardPackageDetailQueryService,
			CardPackageListQueryService cardPackageListQueryService,
			PackageReceivesLogQueryService packageReceivesLogQueryService) {
		this.discountCardCreateRequestMergeService = discountCardCreateRequestMergeService;
		this.cardPackageCreateService = cardPackageCreateService;
		this.cardPackageEditService = cardPackageEditService;
		this.cardPackageGradeLimitCheckService = cardPackageGradeLimitCheckService;
		this.cardPackageDetailQueryService = cardPackageDetailQueryService;
		this.cardPackageListQueryService = cardPackageListQueryService;
		this.packageReceivesLogQueryService = packageReceivesLogQueryService;
	}

	@Activated(routeAlias = "voucher.package.list")
	@GetMapping(value = "/voucher/package/list", name = "获取卡券包列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeCompatRaw,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		String effectivePageSizeRaw = resolveEffectivePageSizeRaw(pageSizeRaw, pageSizeCompatRaw);
		long page = parseRequiredPositiveLong(pageRaw, KaquanDiscountCardMessages.PAGE_REQUIRED);
		long pageSize = parseRequiredPositiveLong(effectivePageSizeRaw, KaquanDiscountCardMessages.PAGE_SIZE_REQUIRED);
		Map<String, Object> data = cardPackageListQueryService.getList(companyId, page, pageSize, title, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "voucher.package.details")
	@GetMapping(value = "/voucher/package/details", name = "获取卡券包详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDetails(HttpServletRequest request,
			@RequestParam(value = "package_id", required = false) String packageIdRaw,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long packageId = parsePackageIdForDetails(packageIdRaw);
		Map<String, Object> data = cardPackageDetailQueryService.getDetails(companyId, packageId, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parsePackageIdForDetails(String packageIdRaw) {
		if (packageIdRaw == null || packageIdRaw.isBlank()) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
		}
		try {
			long v = Long.parseLong(packageIdRaw.trim());
			if (v < 1L) {
				throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
		}
	}

	@Activated(routeAlias = "voucher.package.check_grade_limit")
	@PostMapping(value = "/voucher/package/check_grade_limit", name = "校验卡券包等级限制")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> checkCardPackageGradeLimit(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		List<Long> packageIds = parsePackageIdList(body != null ? body.get("package_id_list") : null);
		String setType = parseSetType(body);
		long gradeId = parseGradeId(body);

		List<Map<String, Object>> list =
				cardPackageGradeLimitCheckService.checkGradeLimit(companyId, packageIds, setType, gradeId);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	private static List<Long> parsePackageIdList(Object raw) {
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_LIST_REQUIRED);
		}
		List<Long> out = new ArrayList<>(list.size());
		for (Object o : list) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o instanceof String s) {
				try {
					out.add(Long.parseLong(s.trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_LIST_REQUIRED);
				}
			} else {
				throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_LIST_REQUIRED);
			}
		}
		return out;
	}

	private static String parseSetType(Map<String, Object> body) {
		if (body == null) {
			throw new BadRequestException(KaquanDiscountCardMessages.SET_TYPE_REQUIRED);
		}
		Object st = body.get("set_type");
		if (!(st instanceof String setType) || setType.isEmpty()) {
			throw new BadRequestException(KaquanDiscountCardMessages.SET_TYPE_REQUIRED);
		}
		if (!"grade".equals(setType) && !"vip_grade".equals(setType)) {
			throw new BadRequestException(KaquanDiscountCardMessages.SET_TYPE_REQUIRED);
		}
		return setType;
	}

	private static long parseGradeId(Map<String, Object> body) {
		if (body == null) {
			throw new BadRequestException(KaquanDiscountCardMessages.GRADE_ID_REQUIRED);
		}
		Object gid = body.get("grade_id");
		long gradeId;
		if (gid instanceof Number n) {
			gradeId = n.longValue();
		} else if (gid instanceof String s) {
			try {
				gradeId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(KaquanDiscountCardMessages.GRADE_ID_REQUIRED);
			}
		} else {
			throw new BadRequestException(KaquanDiscountCardMessages.GRADE_ID_REQUIRED);
		}
		if (gradeId < 1L) {
			throw new BadRequestException(KaquanDiscountCardMessages.GRADE_ID_REQUIRED);
		}
		return gradeId;
	}

	@DataPass
	@Activated(routeAlias = "voucher.package.receives_log")
	@GetMapping(value = "/voucher/package/get_receives_log", name = "卡券包领取日志")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPackageReceivesLog(HttpServletRequest request,
			@RequestParam(value = "package_id", required = false) String packageIdRaw,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeCompatRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long packageId = parsePackageIdForDetails(packageIdRaw);
		long page = parseRequiredPositiveLong(pageRaw, KaquanDiscountCardMessages.PAGE_REQUIRED);
		String effectivePageSizeRaw = resolveEffectivePageSizeRaw(pageSizeRaw, pageSizeCompatRaw);
		long pageSize = parseRequiredPositiveLong(effectivePageSizeRaw, KaquanDiscountCardMessages.PAGE_SIZE_REQUIRED);
		Object datapassBlock = VipGradeOrderDatapassSupport.resolveXDatapassBlock(request);
		boolean needEncode = VipGradeOrderDatapassSupport.isTruthyForMasking(datapassBlock);
		Map<String, Object> data =
				packageReceivesLogQueryService.getReceivesLog(companyId, packageId, page, pageSize, needEncode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveEffectivePageSizeRaw(String pageSizeRaw, String pageSizeCompatRaw) {
		if (StringUtils.hasText(pageSizeCompatRaw)) {
			try {
				long v = Long.parseLong(pageSizeCompatRaw.trim());
				if (v > 0L) {
					return pageSizeCompatRaw.trim();
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return pageSizeRaw;
	}

	private static long parseRequiredPositiveLong(String raw, String errorMessage) {
		if (raw == null || raw.isBlank()) {
			throw new BadRequestException(errorMessage);
		}
		try {
			long v = Long.parseLong(raw.trim());
			if (v < 1L) {
				throw new BadRequestException(errorMessage);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(errorMessage);
		}
	}

	@Activated(routeAlias = "voucher.package.create")
	@PostMapping(value = "/voucher/package", name = "创建卡券包")
	public ResponseEntity<ApiResult<Map<String, Object>>> createPackage(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> inputData = discountCardCreateRequestMergeService.merge(request, body);
		cardPackageCreateService.create(companyId, inputData);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "voucher.package.edit")
	@PatchMapping(value = "/voucher/package", name = "编辑卡券包")
	public ResponseEntity<ApiResult<Map<String, Object>>> editPackage(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> inputData = discountCardCreateRequestMergeService.merge(request, body);
		cardPackageEditService.editPackage(companyId, inputData);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "voucher.package.delete")
	@DeleteMapping(value = "/voucher/package", name = "删除卡券包")
	public ResponseEntity<ApiResult<Map<String, Object>>> deletePackage(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		long packageId = parsePackageIdForDetails(packageIdToString(merged.get("package_id")));
		cardPackageEditService.deletePackage(companyId, packageId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String packageIdToString(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}
}
