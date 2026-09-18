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

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.distribution.service.DistributorMenuPermissionService;
import cn.shopex.ecshopx.distribution.service.DistributorTagsCreateParamValidator;
import cn.shopex.ecshopx.distribution.service.DistributorTagsCreateService;
import cn.shopex.ecshopx.distribution.service.DistributorTagsDelInputNormalizer;
import cn.shopex.ecshopx.distribution.service.DistributorTagsDeleteService;
import cn.shopex.ecshopx.distribution.service.DistributorTagsGetInfoService;
import cn.shopex.ecshopx.distribution.service.DistributorTagsListCoreService;
import cn.shopex.ecshopx.distribution.service.DistributorTagsListRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorTagsRelInputNormalizer;
import cn.shopex.ecshopx.distribution.service.DistributorTagsRelRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorTagsRelWriteService;
import cn.shopex.ecshopx.distribution.service.DistributorTagsUpdateService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributorTags")
@RequestMapping("/api/v1/distributor")
public class DistributorTagsController {

	private static final String NO_UPDATE_DATA_FOUND_MSG = "未查询到更新数据";

	private static final String TAG_LIST_PAGE_REQUIRED = "page 必填";
	private static final String TAG_LIST_PAGESIZE_REQUIRED = "pageSize 必填";
	private static final String TAG_LIST_PAGE_ERROR = "分页参数错误";
	private static final String TAG_LIST_OFFSET_PREFIX = "TAG_LIST_OFFSET:";

	private final CompanysActivationService companysActivationService;
	private final DistributorMenuPermissionService distributorMenuPermissionService;
	private final DistributorTagsCreateService distributorTagsCreateService;
	private final DistributorTagsDeleteService distributorTagsDeleteService;
	private final DistributorTagsUpdateService distributorTagsUpdateService;
	private final DistributorTagsListRequestGate distributorTagsListRequestGate;
	private final DistributorTagsListCoreService distributorTagsListCoreService;
	private final DistributorTagsGetInfoService distributorTagsGetInfoService;
	private final DistributorTagsRelWriteService distributorTagsRelWriteService;
	private final DistributorTagsRelRequestGate distributorTagsRelRequestGate;
	private final DistributorTagsRelInputNormalizer distributorTagsRelInputNormalizer;
	private final DistributorTagsDelInputNormalizer distributorTagsDelInputNormalizer;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public DistributorTagsController(
			CompanysActivationService companysActivationService,
			DistributorMenuPermissionService distributorMenuPermissionService,
			DistributorTagsCreateService distributorTagsCreateService,
			DistributorTagsDeleteService distributorTagsDeleteService,
			DistributorTagsUpdateService distributorTagsUpdateService,
			DistributorTagsListRequestGate distributorTagsListRequestGate,
			DistributorTagsListCoreService distributorTagsListCoreService,
			DistributorTagsGetInfoService distributorTagsGetInfoService,
			DistributorTagsRelWriteService distributorTagsRelWriteService,
			DistributorTagsRelRequestGate distributorTagsRelRequestGate,
			DistributorTagsRelInputNormalizer distributorTagsRelInputNormalizer,
			DistributorTagsDelInputNormalizer distributorTagsDelInputNormalizer,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.companysActivationService = companysActivationService;
		this.distributorMenuPermissionService = distributorMenuPermissionService;
		this.distributorTagsCreateService = distributorTagsCreateService;
		this.distributorTagsDeleteService = distributorTagsDeleteService;
		this.distributorTagsUpdateService = distributorTagsUpdateService;
		this.distributorTagsListRequestGate = distributorTagsListRequestGate;
		this.distributorTagsListCoreService = distributorTagsListCoreService;
		this.distributorTagsGetInfoService = distributorTagsGetInfoService;
		this.distributorTagsRelWriteService = distributorTagsRelWriteService;
		this.distributorTagsRelRequestGate = distributorTagsRelRequestGate;
		this.distributorTagsRelInputNormalizer = distributorTagsRelInputNormalizer;
		this.distributorTagsDelInputNormalizer = distributorTagsDelInputNormalizer;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "distributor.tag.add")
	@PostMapping(value = "/tag", name = "添加店铺标签")
	public ResponseEntity<?> createTags(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_ADD);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("company_id", companyId);
		String validationMessage = DistributorTagsCreateParamValidator.firstValidationErrorOrNull(merged);
		if (validationMessage != null) {
			return ResponseEntity.ok(dingoValidation422(validationMessage));
		}
		String requestLang = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = distributorTagsCreateService.create(merged, companyId, requestLang);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/tag");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加店铺标签");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(row));
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input =
					new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	@Activated(routeAlias = "distributor.tag.delete")
	@DeleteMapping(value = "/tag/{tagId}", name = "删除店铺标签")
	public ResponseEntity<?> deleteTag(HttpServletRequest request, @PathVariable("tagId") String tagId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_DELETE);
		tryParseTagId(tagId)
				.ifPresent(id -> distributorTagsDeleteService.deleteByCompanyAndTagId(companyId, id));

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/tag/" + tagId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("tag_id", tagId)));
		} catch (Exception e) {
			logCtx.put("params", tagId);
		}
		logCtx.put("operator_name", "删除店铺标签");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static Optional<Long> tryParseTagId(String tagId) {
		if (tagId == null) {
			return Optional.empty();
		}
		String trimmed = tagId.trim();
		if (!StringUtils.hasText(trimmed)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(trimmed));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	@Activated(routeAlias = "distributor.tag.update")
	@PutMapping(value = "/tag/{tagId}", name = "更新店铺标签")
	public ResponseEntity<?> updateTags(
			HttpServletRequest request,
			@PathVariable("tagId") String tagId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_UPDATE);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String validationMessage = DistributorTagsCreateParamValidator.firstValidationErrorOrNullForUpdate(merged);
		if (validationMessage != null) {
			return ResponseEntity.ok(dingoValidation422(validationMessage));
		}
		try {
			Optional<Long> idOpt = tryParseTagId(tagId);
			if (idOpt.isEmpty()) {
				throw new ResourceException(NO_UPDATE_DATA_FOUND_MSG);
			}
			String requestLang = RequestCountryCode.resolve(langueProperties, merged);
			Map<String, Object> row =
					distributorTagsUpdateService.updateOneByCompanyAndTagId(idOpt.get(), companyId, merged, requestLang);

			long operatorId = toLong(ud.get("operator_id"));
			Map<String, Object> logCtx = new LinkedHashMap<>();
			logCtx.put("company_id", companyId);
			logCtx.put("operator_id", (int) operatorId);
			logCtx.put("request_uri", "/api/v1/distributor/tag/" + tagId);
			logCtx.put("ip", clientIp(request));
			try {
				logCtx.put("params", objectMapper.writeValueAsString(merged));
			} catch (Exception e) {
				logCtx.put("params", merged.toString());
			}
			logCtx.put("operator_name", "更新店铺标签");
			logCtx.put("log_type", "operator");
			Object merchantId = ud.get("merchant_id");
			if (merchantId != null) {
				logCtx.put(
						"merchant_id",
						merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
			}
			operatorLogsWriteService.addLogs(logCtx);

			return ResponseEntity.ok(ApiResult.ok(row));
		} catch (ResourceException e) {
			if (NO_UPDATE_DATA_FOUND_MSG.equals(e.getMessage())) {
				return ResponseEntity.ok(dingoValidation422(NO_UPDATE_DATA_FOUND_MSG));
			}
			throw e;
		}
	}

	private static Map<String, Object> dingoValidation422(String message) {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("message", message);
		inner.put("status_code", 422);
		Map<String, Object> dingoStyle = new LinkedHashMap<>();
		dingoStyle.put("data", inner);
		return dingoStyle;
	}

	@Activated(routeAlias = "distributor.tag.list")
	@GetMapping(value = "/tag", name = "获取店铺标签列表")
	public ResponseEntity<?> getTagsList(
			HttpServletRequest request,
			@RequestParam(value = "tag_name", required = false) String tagName,
			@RequestParam(value = "front_show", required = false) String frontShowRaw) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (tagName != null) {
			merged.put("tag_name", tagName);
		}
		merged.put("front_show_query_present", request.getParameterMap().containsKey("front_show"));
		if (request.getParameterMap().containsKey("front_show")) {
			merged.put("front_show_raw", frontShowRaw != null ? frontShowRaw : "");
		}
		String requestLang = RequestLangTag.current(langueProperties);
		try {
			Map<String, Object> user = distributorTagsListRequestGate.validateBeforeList(request, merged);
			Map<String, Object> data = distributorTagsListCoreService.buildList(user, merged, requestLang);
			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			String msg = ex.getMessage();
			if (msg != null && msg.startsWith(TAG_LIST_OFFSET_PREFIX)) {
				int offset = Integer.parseInt(msg.substring(TAG_LIST_OFFSET_PREFIX.length()));
				throw new BadRequestException(
						"Offset must be a positive integer or zero, " + offset + " given");
			}
			if (TAG_LIST_PAGE_REQUIRED.equals(msg)
					|| TAG_LIST_PAGESIZE_REQUIRED.equals(msg)
					|| TAG_LIST_PAGE_ERROR.equals(msg)) {
				return ResponseEntity.ok(dingoValidation422(msg));
			}
			throw ex;
		}
	}

	@Activated(routeAlias = "distributor.tag.get")
	@GetMapping(value = "/tag/{tagId}", name = "获取店铺标签详情")
	public ResponseEntity<?> getTagsInfo(HttpServletRequest request, @PathVariable("tagId") String tagId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_GET);
		Optional<Long> idOpt = tryParseTagId(tagId);
		if (idOpt.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> row =
				distributorTagsGetInfoService.getOneRowWithLang(companyId, idOpt.get(), requestLang);
		if (row == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "distributor.tag.rel")
	@PostMapping(value = "/reltag", name = "店铺关联标签")
	public ResponseEntity<?> tagsRelDistributor(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_REL);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		DistributorTagsRelInputNormalizer.NormalizedRelTagInput normalized =
				distributorTagsRelInputNormalizer.normalize(merged);
		if (!normalized.hasDistributorSelection()) {
			return ResponseEntity.ok(dingoValidation422("请选择店铺"));
		}
		if (!normalized.hasTagSelection()) {
			return ResponseEntity.ok(dingoValidation422("请选择标签"));
		}

		List<Long> touchBase =
				normalized.distributorIdIsArray()
						? normalized.distributorIds()
						: List.of(normalized.scalarDistributorId());
		List<Long> distributorIdsToTouch = new ArrayList<>(new LinkedHashSet<>(touchBase));

		distributorTagsRelRequestGate.assertCallerMayUseDistributors(
				request, user, companyId, distributorIdsToTouch);

		distributorTagsRelWriteService.applyRelTags(companyId, normalized);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/reltag");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "关联店铺标签");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "distributor.tag.del")
	@PostMapping(value = "/deltag", name = "店铺与店铺标签解绑")
	public ResponseEntity<?> tagsRemoveDistributor(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_TAG_DEL);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		final DistributorTagsDelInputNormalizer.NormalizedDelTagInput in;
		try {
			in = distributorTagsDelInputNormalizer.normalize(merged);
		} catch (BadRequestException e) {
			throw e;
		}

		List<Long> touch = new ArrayList<>(new LinkedHashSet<>(in.distributorIds()));
		distributorTagsRelRequestGate.assertCallerMayUseDistributors(request, user, companyId, touch);

		distributorTagsRelWriteService.deleteRelTags(companyId, in.distributorIds(), in.tagIds());

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/deltag");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "店铺与店铺标签解绑");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", 1)));
	}
}
