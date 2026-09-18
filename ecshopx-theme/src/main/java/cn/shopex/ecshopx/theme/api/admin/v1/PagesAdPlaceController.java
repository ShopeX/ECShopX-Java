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

package cn.shopex.ecshopx.theme.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreatePagesAdPlaceRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesAdPlaceAuditRequest;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceAuditService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceCreateService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceDeleteService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceGetInfoService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceListService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceSubmitService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceUpdateService;
import cn.shopex.ecshopx.theme.service.PagesAdPlaceWithdrawService;
import cn.shopex.ecshopx.theme.service.dto.PagesAdPlaceListCriteria;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RequiredArgsConstructor
@RestController("themeAdminV1PagesAdPlace")
@RequestMapping("/api/v1")
public class PagesAdPlaceController {

	private final PagesAdPlaceCreateService pagesAdPlaceCreateService;

	private final PagesAdPlaceSubmitService pagesAdPlaceSubmitService;

	private final PagesAdPlaceAuditService pagesAdPlaceAuditService;

	private final PagesAdPlaceWithdrawService pagesAdPlaceWithdrawService;

	private final PagesAdPlaceUpdateService pagesAdPlaceUpdateService;

	private final PagesAdPlaceListService pagesAdPlaceListService;

	private final PagesAdPlaceGetInfoService pagesAdPlaceGetInfoService;

	private final PagesAdPlaceDeleteService pagesAdPlaceDeleteService;

	private final MessageSource messageSource;

	@Activated(routeAlias = "pages.adplace.popup.list")
	@GetMapping(value = { "/adplace/popup/list", "/adplace/carousel/list" }, name = "广告位列表")
	public ApiResult<Map<String, Object>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(value = "regionauth_id", required = false) String regionauthIdParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(value = "id", required = false) String idParam,
			@RequestParam(value = "name", required = false) String nameParam,
			@RequestParam(value = "ad_type", required = false) String adTypeParam,
			@RequestParam(value = "status", required = false) String statusParam,
			@RequestParam(value = "pages", required = false) String pagesParam,
			@RequestParam(value = "audit_status", required = false) String auditStatusParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		if (!StringUtils.hasText(pageParam)) {
			throw new BadRequestException("The page field is required.");
		}
		if (!StringUtils.hasText(pageSizeParam)) {
			throw new BadRequestException("The page size field is required.");
		}
		int page;
		int pageSize;
		try {
			page = Integer.parseInt(pageParam.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("The page must be an integer.");
		}
		try {
			pageSize = Integer.parseInt(pageSizeParam.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("The page size must be an integer.");
		}
		if (page < 1) {
			throw new BadRequestException("The page must be at least 1.");
		}
		if (pageSize < 1) {
			throw new BadRequestException("The page size must be at least 1.");
		}
		if (pageSize > 100) {
			throw new BadRequestException("The page size may not be greater than 100.");
		}
		if (StringUtils.hasText(adTypeParam)
				&& !"popup".equals(adTypeParam)
				&& !"carousel".equals(adTypeParam)) {
			throw new BadRequestException("The selected ad type is invalid.");
		}
		if (StringUtils.hasText(auditStatusParam)) {
			String trimmed = auditStatusParam.trim();
			if (!Set.of("submitting", "processing", "approved", "rejected").contains(trimmed)) {
				throw new BadRequestException("The selected audit status is invalid.");
			}
		}

		var b = PagesAdPlaceListCriteria.builder().companyId(companyId);
		if ("distributor".equals(operatorType)) {
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					long parsed = Long.parseLong(distributorIdParam.trim());
					b.sourceId(parsed);
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else {
				b.sourceId(0L);
			}
		} else {
			b.sourceId(0L);
		}
		if (StringUtils.hasText(regionauthIdParam)) {
			try {
				long value = Long.parseLong(regionauthIdParam.trim());
				if (value != 0L) {
					b.regionauthId(value);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid optional filter
			}
		}
		if (StringUtils.hasText(idParam)) {
			try {
				long value = Long.parseLong(idParam.trim());
				if (value != 0L) {
					b.id(value);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid optional filter
			}
		}
		if (StringUtils.hasText(nameParam)) {
			b.nameContains(nameParam.trim());
		}
		if (StringUtils.hasText(adTypeParam)) {
			b.adType(adTypeParam);
		}
		if (StringUtils.hasText(distributorIdParam)) {
			try {
				long d = Long.parseLong(distributorIdParam.trim());
				if (d > 0L) {
					b.distributorIdForJoin(d);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid optional join filter
			}
		}
		if (StringUtils.hasText(pagesParam)) {
			b.pagesExact(pagesParam);
		}
		if (StringUtils.hasText(auditStatusParam)) {
			String auditForFilter = auditStatusParam.trim();
			if (Set.of("submitting", "processing").contains(auditForFilter)) {
				b.endTimeGt(Instant.now().getEpochSecond());
			}
		}
		boolean hasStatusKey = request.getParameterMap().containsKey("status");
		long now = Instant.now().getEpochSecond();
		if (hasStatusKey) {
			String sv = statusParam == null ? "" : statusParam;
			switch (sv) {
				case "0":
					b.startTimeGt(now);
					break;
				case "1":
					b.startTimeLte(now);
					b.endTimeGte(now);
					break;
				case "2":
					b.endTimeLt(now);
					break;
				default:
					break;
			}
		}
		boolean suppressAuditStatusEq =
				request.getParameterMap().containsKey("status") && "2".equals(statusParam);
		if (StringUtils.hasText(auditStatusParam) && !suppressAuditStatusEq) {
			b.auditStatus(auditStatusParam.trim());
		}
		PagesAdPlaceListCriteria criteria = b.build();
		Map<String, Object> data = pagesAdPlaceListService.getList(criteria, page, pageSize);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pages.adplace.popup.getInfo")
	@GetMapping(value = { "/adplace/popup/{id}", "/adplace/carousel/{id}" }, name = "广告位详情")
	public ApiResult<Object> getInfo(
			HttpServletRequest request,
			@PathVariable("id") String idParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		Long adPlaceIdResolved = tryParseStrictPositiveLongPathId(idParam);
		if (adPlaceIdResolved == null) {
			return ApiResult.ok(Collections.emptyList());
		}
		long adPlaceId = adPlaceIdResolved;

		Long sourceIdFilter;
		if ("distributor".equals(operatorType)) {
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					sourceIdFilter = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else {
				sourceIdFilter = null;
			}
		} else {
			sourceIdFilter = Long.valueOf(0L);
		}

		Object data = pagesAdPlaceGetInfoService.getInfo(companyId, adPlaceId, sourceIdFilter);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pages.adplace.popup.create")
	@PostMapping(value = { "/adplace/popup", "/adplace/carousel" }, name = "广告位创建")
	public ApiResult<Map<String, Object>> create(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@Valid @FlexibleBody CreatePagesAdPlaceRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		List<Long> distributorIdsForRel;
		long sourceIdForEntity;
		if ("distributor".equals(operatorType)) {
			Long distributorFromRequest = null;
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					distributorFromRequest = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else if (body.getDistributorId() != null && !body.getDistributorId().isEmpty()) {
				Long first = body.getDistributorId().get(0);
				if (first == null) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
				distributorFromRequest = first;
			}
			distributorIdsForRel = new ArrayList<>();
			distributorIdsForRel.add(distributorFromRequest);
			sourceIdForEntity = distributorFromRequest != null ? distributorFromRequest : 0L;
		} else {
			distributorIdsForRel =
					body.getDistributorId() == null
							? Collections.emptyList()
							: new ArrayList<>(body.getDistributorId());
			sourceIdForEntity = 0L;
		}

		Map<String, Object> data =
				pagesAdPlaceCreateService.create(companyId, distributorIdsForRel, sourceIdForEntity, body);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pages.adplace.popup.update")
	@PutMapping(value = { "/adplace/popup/{id}", "/adplace/carousel/{id}" }, name = "广告位更新")
	public ApiResult<Map<String, Object>> update(
			HttpServletRequest request,
			@PathVariable("id") String idParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@Valid @FlexibleBody CreatePagesAdPlaceRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		long adPlaceId = parsePositiveLongPathId(idParam, "id 无效");

		List<Long> distributorIdsForRel;
		long sourceIdFilter;
		if ("distributor".equals(operatorType)) {
			Long distributorFromRequest = null;
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					distributorFromRequest = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else if (body.getDistributorId() != null && !body.getDistributorId().isEmpty()) {
				Long first = body.getDistributorId().get(0);
				if (first == null) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
				distributorFromRequest = first;
			}
			distributorIdsForRel = new ArrayList<>();
			distributorIdsForRel.add(distributorFromRequest);
			sourceIdFilter = distributorFromRequest != null ? distributorFromRequest : 0L;
		} else {
			distributorIdsForRel =
					body.getDistributorId() == null
							? Collections.emptyList()
							: new ArrayList<>(body.getDistributorId());
			sourceIdFilter = 0L;
		}

		Map<String, Object> data =
				pagesAdPlaceUpdateService.update(companyId, adPlaceId, sourceIdFilter, distributorIdsForRel, body);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pages.adplace.popup.delete")
	@DeleteMapping(value = { "/adplace/popup/{id}", "/adplace/carousel/{id}" }, name = "广告位删除")
	public ApiResult<Map<String, Object>> delete(
			HttpServletRequest request,
			@PathVariable("id") String idParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		Long adPlaceId = tryParseStrictPositiveLongPathId(idParam);
		if (adPlaceId == null) {
			return ApiResult.ok(Map.of("status", Boolean.TRUE));
		}

		Long sourceIdFilter;
		if ("distributor".equals(operatorType)) {
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					sourceIdFilter = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else {
				sourceIdFilter = null;
			}
		} else {
			sourceIdFilter = 0L;
		}

		pagesAdPlaceDeleteService.delete(companyId, adPlaceId.longValue(), sourceIdFilter);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "pages.adplace.popup.submit")
	@PostMapping(value = { "/adplace/popup/submit/{id}", "/adplace/carousel/submit/{id}" }, name = "广告位提交审核")
	public ApiResult<Map<String, Object>> submit(
			HttpServletRequest request,
			@PathVariable("id") String idParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		long adPlaceId = parsePositiveLongPathId(idParam, "id 无效");

		Long sourceIdFilter;
		if ("distributor".equals(operatorType)) {
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					sourceIdFilter = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else {
				sourceIdFilter = null;
			}
		} else {
			sourceIdFilter = 0L;
		}

		pagesAdPlaceSubmitService.submit(companyId, adPlaceId, sourceIdFilter);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "pages.adplace.popup.audit")
	@PostMapping(value = { "/adplace/popup/audit/{id}", "/adplace/carousel/audit/{id}" }, name = "广告位审核")
	public ApiResult<Map<String, Object>> audit(
			HttpServletRequest request,
			@PathVariable("id") String idParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@Valid @FlexibleBody PagesAdPlaceAuditRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		long adPlaceId = parsePositiveLongPathId(idParam, "id 无效");

		Long sourceIdFilter;
		if ("distributor".equals(operatorType)) {
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					sourceIdFilter = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"theme.pages_ad_place.invalid_distributor_id",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else {
				sourceIdFilter = null;
			}
		} else {
			sourceIdFilter = 0L;
		}

		Map<String, Object> data =
				pagesAdPlaceAuditService.audit(companyId, adPlaceId, sourceIdFilter, body);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pages.adplace.popup.withdraw")
	@PostMapping(value = { "/adplace/popup/withdraw/{id}", "/adplace/carousel/withdraw/{id}" }, name = "广告位撤回")
	public ApiResult<Map<String, Object>> withdraw(
			HttpServletRequest request,
			@PathVariable("id") String idParam,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		long adPlaceId = parsePositiveLongPathId(idParam, "id 无效");

		Long sourceIdFilter;
		if ("distributor".equals(operatorType)) {
			if (!StringUtils.hasText(distributorIdParam)) {
				throw new BadRequestException(
						messageSource.getMessage(
								"theme.pages_ad_place.invalid_distributor_id",
								null,
								LocaleContextHolder.getLocale()));
			}
			try {
				sourceIdFilter = Long.parseLong(distributorIdParam.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(
						messageSource.getMessage(
								"theme.pages_ad_place.invalid_distributor_id",
								null,
								LocaleContextHolder.getLocale()));
			}
		} else {
			sourceIdFilter = 0L;
		}

		Map<String, Object> data =
				pagesAdPlaceWithdrawService.withdraw(companyId, adPlaceId, sourceIdFilter);
		return ApiResult.ok(data);
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}

	private static long parsePositiveLongPathId(String idParam, String invalidMsg) {
		if (idParam == null || !StringUtils.hasText(idParam)) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		try {
			result = Long.parseLong(idParam.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(invalidMsg);
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}

	/**
	 * Path segment as decimal integer: strictly positive {@code long}, or {@code null} if blank,
	 * non-numeric, not positive, or out of range.
	 */
	private static Long tryParseStrictPositiveLongPathId(String idParam) {
		if (idParam == null || !StringUtils.hasText(idParam)) {
			return null;
		}
		try {
			long v = Long.parseLong(idParam.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
