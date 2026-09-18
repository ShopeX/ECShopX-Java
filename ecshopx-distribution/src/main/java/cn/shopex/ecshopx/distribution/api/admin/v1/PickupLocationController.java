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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.CancelRelDistributorRequest;
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.CreatePickupLocationRequest;
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.RelDistributorRequest;
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.CreatePickupLocationRequestValidator;
import cn.shopex.ecshopx.distribution.service.PickupLocationCreateService;
import cn.shopex.ecshopx.distribution.service.PickupLocationListService;
import cn.shopex.ecshopx.distribution.service.PickupLocationRelDistributorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
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
@RestController("distributionAdminV1PickupLocation")
@RequestMapping("/api/v1/pickuplocation")
public class PickupLocationController {

	private final PickupLocationCreateService pickupLocationCreateService;

	private final PickupLocationRelDistributorService pickupLocationRelDistributorService;

	private final PickupLocationListService pickupLocationListService;

	private final MessageSource messageSource;

	private final Validator validator;
	private final LangueProperties langueProperties;

	public PickupLocationController(
			PickupLocationCreateService pickupLocationCreateService,
			PickupLocationRelDistributorService pickupLocationRelDistributorService,
			PickupLocationListService pickupLocationListService,
			MessageSource messageSource,
			Validator validator,
			LangueProperties langueProperties) {
		this.pickupLocationCreateService = pickupLocationCreateService;
		this.pickupLocationRelDistributorService = pickupLocationRelDistributorService;
		this.pickupLocationListService = pickupLocationListService;
		this.messageSource = messageSource;
		this.validator = validator;
		this.langueProperties = langueProperties;
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.list.get")
	@GetMapping(value = "/list", name = "获取自提点列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPickupLocationList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeSnakeRaw,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "city", required = false) String city,
			@RequestParam(value = "area", required = false) String area,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "rel_distributor_id", required = false) String relDistributorIdRaw,
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

		String pageSizeEffective =
				StringUtils.hasText(pageSizeRaw) ? pageSizeRaw : pageSizeSnakeRaw;
		String effectiveDistributorId = resolveDistributorIdParam(request, distributorIdParam);
		Map<String, Object> data =
				pickupLocationListService.getPickupLocationList(
						companyId,
						operatorType,
						effectiveDistributorId,
						pageRaw,
						pageSizeEffective,
						name,
						province,
						city,
						area,
						address,
						relDistributorIdRaw,
						RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.info.get")
	@GetMapping(value = "/{id:[0-9]+}", name = "获取自提点详情")
	public ResponseEntity<ApiResult<Object>> getPickupLocationInfo(
			HttpServletRequest request,
			@PathVariable("id") String id,
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

		long pickupLocationId = parsePositivePickupLocationIdFromPath(id);
		Object payload =
				pickupLocationListService.getPickupLocationInfo(
						companyId, operatorType, distributorIdParam, pickupLocationId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.create")
	@PostMapping(name = "新增自提点")
	public ResponseEntity<ApiResult<Map<String, Object>>> createPickupLocation(
			HttpServletRequest request, @FlexibleBody CreatePickupLocationRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		CreatePickupLocationRequestValidator.validate(validator, body);

		if ("distributor".equals(operatorType) && body.getDistributorId() == null) {
			Object attrId = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
			if (attrId instanceof Number n && n.longValue() > 0L) {
				body.setDistributorId(n.longValue());
			}
		}

		Map<String, Object> data =
				pickupLocationCreateService.createPickupLocation(companyId, operatorType, body);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.update")
	@PutMapping(value = "/{id}", name = "更新自提点")
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePickupLocation(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody CreatePickupLocationRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		CreatePickupLocationRequestValidator.validate(validator, body);

		long pickupLocationId = parsePositivePickupLocationIdFromPath(id);

		long distributorScopeId = 0L;
		if ("distributor".equals(operatorType)) {
			Long distributorFromRequest = null;
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					distributorFromRequest = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"distribution.pickupLocation.relDistributor.invalidDistributorId",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else if (body.getDistributorId() != null) {
				distributorFromRequest = body.getDistributorId();
			}
			distributorScopeId = distributorFromRequest != null ? distributorFromRequest : 0L;
		}

		Map<String, Object> data =
				pickupLocationCreateService.updatePickupLocation(
						companyId, distributorScopeId, pickupLocationId, body);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.delete")
	@DeleteMapping(value = "/{id}", name = "删除自提点")
	public ResponseEntity<ApiResult<Map<String, Object>>> delPickupLocation(
			HttpServletRequest request,
			@PathVariable("id") String id,
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

		// Delete: blank, non-numeric, or non-positive path ids imply no matching primary key; respond with the
		// same success payload as a delete that affects zero rows (no client error for this path shape).
		OptionalLong pickupLocationId = tryParsePositivePickupLocationIdPathForDelete(id);
		if (pickupLocationId.isEmpty()) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", Boolean.TRUE);
			return ResponseEntity.ok(ApiResult.ok(data));
		}
		pickupLocationListService.delPickupLocation(
				companyId, operatorType, distributorIdParam, pickupLocationId.getAsLong());

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.reldistributor")
	@PostMapping(value = "/reldistributor", name = "自提点关联门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> relDistributor(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody RelDistributorRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		long distributorScopeId = 0L;
		if ("distributor".equals(operatorType)) {
			Long distributorFromRequest = null;
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					distributorFromRequest = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"distribution.pickupLocation.relDistributor.invalidDistributorId",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else if (body.getDistributorId() != null) {
				distributorFromRequest = body.getDistributorId();
			}
			distributorScopeId = distributorFromRequest != null ? distributorFromRequest : 0L;
		}

		List<Long> ids = body.getId();
		if (ids == null || ids.isEmpty()) {
			throw new BadRequestException("自提点id必填");
		}
		if (body.getRelDistributorId() == null) {
			throw new BadRequestException("店铺id必填");
		}
		if (ids.stream().anyMatch(Objects::isNull)) {
			throw new BadRequestException("自提点id必填");
		}

		long rel = body.getRelDistributorId();
		pickupLocationRelDistributorService.relDistributor(companyId, distributorScopeId, ids, rel);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "pickuplocation.reldistributor.cancel")
	@PostMapping(value = "/reldistributor/cancel", name = "自提点取消关联门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> cancelRelDistributor(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody CancelRelDistributorRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Object opType = jwt.get("operator_type");
		String operatorType = opType == null ? "" : String.valueOf(opType);

		long distributorScopeId = 0L;
		if ("distributor".equals(operatorType)) {
			Long distributorFromRequest = null;
			if (StringUtils.hasText(distributorIdParam)) {
				try {
					distributorFromRequest = Long.parseLong(distributorIdParam.trim());
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							messageSource.getMessage(
									"distribution.pickupLocation.relDistributor.invalidDistributorId",
									null,
									LocaleContextHolder.getLocale()));
				}
			} else if (body.getDistributorId() != null) {
				distributorFromRequest = body.getDistributorId();
			}
			distributorScopeId = distributorFromRequest != null ? distributorFromRequest : 0L;
		}

		List<Long> ids = body.getId();
		if (ids == null || ids.isEmpty()) {
			throw new BadRequestException("自提点id必填");
		}
		if (body.getRelDistributorId() == null) {
			throw new BadRequestException("店铺id必填");
		}
		if (ids.stream().anyMatch(Objects::isNull)) {
			throw new BadRequestException("自提点id必填");
		}

		long rel = body.getRelDistributorId();
		pickupLocationRelDistributorService.cancelRelDistributor(companyId, distributorScopeId, ids, rel);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveDistributorIdParam(HttpServletRequest request, String queryParam) {
		if (StringUtils.hasText(queryParam)) {
			return queryParam;
		}
		Object attrId = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
		if (attrId != null) {
			return String.valueOf(attrId);
		}
		return null;
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	/**
	 * DELETE-only path id parsing: blank, unparsable, or non-positive values are treated as no matching primary
	 * key, so callers can short-circuit to the same success response as a zero-row delete.
	 */
	private static OptionalLong tryParsePositivePickupLocationIdPathForDelete(String id) {
		if (!StringUtils.hasText(id)) {
			return OptionalLong.empty();
		}
		try {
			long v = Long.parseLong(id.trim());
			if (v <= 0L) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(v);
		} catch (NumberFormatException ex) {
			return OptionalLong.empty();
		}
	}

	private long parsePositivePickupLocationIdFromPath(String id) {
		if (!StringUtils.hasText(id)) {
			throw new BadRequestException("自提点id必填");
		}
		long v;
		try {
			v = Long.parseLong(id.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.pickupLocation.invalidPickupLocationId",
							null,
							LocaleContextHolder.getLocale()));
		}
		if (v <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.pickupLocation.invalidPickupLocationId",
							null,
							LocaleContextHolder.getLocale()));
		}
		return v;
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
}
