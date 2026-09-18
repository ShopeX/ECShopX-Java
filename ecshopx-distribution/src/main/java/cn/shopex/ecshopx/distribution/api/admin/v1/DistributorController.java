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
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.distribution.service.DistributorDefaultSetRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorDefaultSetService;
import cn.shopex.ecshopx.distribution.service.DistributorCreateOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorCreateParamNormalizer;
import cn.shopex.ecshopx.distribution.service.DistributorCreateParamValidator;
import cn.shopex.ecshopx.distribution.service.DistributorCreateRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorItemsCreateRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorItemsDeleteRequestValidator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsDeleteService;
import cn.shopex.ecshopx.distribution.service.DistributorItemsExportOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsExportRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorItemsListOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsListRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorItemsSaveOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsSaveRequestValidator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsUpdateOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorItemsUpdateRequestGate;
import cn.shopex.ecshopx.distribution.service.dto.SaveDistributorItemsResult;
import cn.shopex.ecshopx.distribution.service.DistributorPaymentSubjectRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributorPaymentSubjectService;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateParamNormalizer;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateParamValidator;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateRequestGate;
import cn.shopex.ecshopx.distribution.service.DistributeCountReadService;
import cn.shopex.ecshopx.distribution.service.DistributorEasyListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorWxaCodeOrchestrator;
import cn.shopex.ecshopx.distribution.service.DistributorInfoAccessGate;
import cn.shopex.ecshopx.distribution.service.DistributorInfoQueryHandler;
import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.distribution.service.DistributorAdminListContext;
import cn.shopex.ecshopx.distribution.service.DistributorAdminListCoreService;
import cn.shopex.ecshopx.distribution.service.DistributorAdminListResult;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryHandler;
import cn.shopex.ecshopx.distribution.service.DistributorMenuPermissionService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.distribution.service.DistributorDeliveryDistanceQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorDeliveryDistanceUpdateService;
import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.distribution.service.DistributorListBackgroundRedisService;
import cn.shopex.ecshopx.distribution.service.OfflineAftersalesDistributorListService;

/**
 * Shop-batch distributor creation delegates to the same {@link DistributorCreateOrchestrator} and
 * {@link cn.shopex.ecshopx.common.dispatch.DistributionAddEventDispatchPublisher} after-commit path as
 * admin distributor create ({@code POST /api/v1/distributor}) on this controller.
 *
 * <p>Shop-batch distributor updates likewise converge on {@link DistributorUpdateOrchestrator} and the
 * same after-commit sequence as {@code PUT /api/v1/distributor/{distributor_id}} on this controller:
 * {@link cn.shopex.ecshopx.common.dispatch.DistributionEditEventDispatchPublisher} (including its
 * configured dual publish) followed by {@link cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher},
 * matching the former standalone {@code PUT /shops/{distributor_id}} route that invoked the same
 * update stack before routes were unified here.
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributorCrud")
@RequestMapping("/api/v1")
public class DistributorController {

	private final DistributorCreateRequestGate distributorCreateRequestGate;
	private final DistributorCreateParamNormalizer distributorCreateParamNormalizer;
	private final DistributorCreateOrchestrator distributorCreateOrchestrator;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final CompanysActivationService companysActivationService;
	private final DistributorMenuPermissionService distributorMenuPermissionService;
	private final DistributorListQueryHandler distributorListQueryHandler;
	private final DistributorInfoAccessGate distributorInfoAccessGate;
	private final DistributorInfoQueryHandler distributorInfoQueryHandler;
	private final DistributorUpdateRequestGate distributorUpdateRequestGate;
	private final DistributorUpdateParamNormalizer distributorUpdateParamNormalizer;
	private final DistributorUpdateOrchestrator distributorUpdateOrchestrator;
	private final DistributorPaymentSubjectRequestGate distributorPaymentSubjectRequestGate;
	private final DistributorPaymentSubjectService distributorPaymentSubjectService;
	private final DistributorInfoResolveService distributorInfoResolveService;
	private final DistributeCountReadService distributeCountReadService;
	private final DistributorWxaCodeOrchestrator distributorWxaCodeOrchestrator;
	private final DistributorEasyListQueryService distributorEasyListQueryService;
	private final DistributorItemsCreateRequestGate distributorItemsCreateRequestGate;
	private final DistributorItemsListRequestGate distributorItemsListRequestGate;
	private final DistributorItemsListOrchestrator distributorItemsListOrchestrator;
	private final DistributorItemsSaveOrchestrator distributorItemsSaveOrchestrator;
	private final DistributorItemsExportRequestGate distributorItemsExportRequestGate;
	private final DistributorItemsExportOrchestrator distributorItemsExportOrchestrator;
	private final DistributorItemsDeleteService distributorItemsDeleteService;
	private final DistributorItemsUpdateRequestGate distributorItemsUpdateRequestGate;
	private final DistributorItemsUpdateOrchestrator distributorItemsUpdateOrchestrator;
	private final DistributorDefaultSetRequestGate distributorDefaultSetRequestGate;
	private final DistributorDefaultSetService distributorDefaultSetService;
	private final DistributorAdminListCoreService distributorAdminListCoreService;
	private final DistributorDeliveryDistanceUpdateService distributorDeliveryDistanceUpdateService;
	private final DistributorDeliveryDistanceQueryService distributorDeliveryDistanceQueryService;
	private final OfflineAftersalesDistributorListService offlineAftersalesDistributorListService;
	private final LangueProperties langueProperties;
	private final CompanyMapGeocodePort companyMapGeocodePort;
	private final DistributorListBackgroundRedisService distributorListBackgroundRedisService;

	public DistributorController(
			DistributorCreateRequestGate distributorCreateRequestGate,
			DistributorCreateParamNormalizer distributorCreateParamNormalizer,
			DistributorCreateOrchestrator distributorCreateOrchestrator,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			CompanysActivationService companysActivationService,
			DistributorMenuPermissionService distributorMenuPermissionService,
			DistributorListQueryHandler distributorListQueryHandler,
			DistributorInfoAccessGate distributorInfoAccessGate,
			DistributorInfoQueryHandler distributorInfoQueryHandler,
			DistributorUpdateRequestGate distributorUpdateRequestGate,
			DistributorUpdateParamNormalizer distributorUpdateParamNormalizer,
			DistributorUpdateOrchestrator distributorUpdateOrchestrator,
			DistributorPaymentSubjectRequestGate distributorPaymentSubjectRequestGate,
			DistributorPaymentSubjectService distributorPaymentSubjectService,
			DistributorInfoResolveService distributorInfoResolveService,
			DistributeCountReadService distributeCountReadService,
			DistributorWxaCodeOrchestrator distributorWxaCodeOrchestrator,
			DistributorEasyListQueryService distributorEasyListQueryService,
			DistributorItemsCreateRequestGate distributorItemsCreateRequestGate,
			DistributorItemsListRequestGate distributorItemsListRequestGate,
			DistributorItemsListOrchestrator distributorItemsListOrchestrator,
			DistributorItemsSaveOrchestrator distributorItemsSaveOrchestrator,
			DistributorItemsExportRequestGate distributorItemsExportRequestGate,
			DistributorItemsExportOrchestrator distributorItemsExportOrchestrator,
			DistributorItemsDeleteService distributorItemsDeleteService,
			DistributorItemsUpdateRequestGate distributorItemsUpdateRequestGate,
			DistributorItemsUpdateOrchestrator distributorItemsUpdateOrchestrator,
			DistributorDefaultSetRequestGate distributorDefaultSetRequestGate,
			DistributorDefaultSetService distributorDefaultSetService,
			DistributorAdminListCoreService distributorAdminListCoreService,
			DistributorDeliveryDistanceUpdateService distributorDeliveryDistanceUpdateService,
			DistributorDeliveryDistanceQueryService distributorDeliveryDistanceQueryService,
			OfflineAftersalesDistributorListService offlineAftersalesDistributorListService,
			LangueProperties langueProperties,
			CompanyMapGeocodePort companyMapGeocodePort,
			DistributorListBackgroundRedisService distributorListBackgroundRedisService) {
		this.distributorCreateRequestGate = distributorCreateRequestGate;
		this.distributorCreateParamNormalizer = distributorCreateParamNormalizer;
		this.distributorCreateOrchestrator = distributorCreateOrchestrator;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.companysActivationService = companysActivationService;
		this.distributorMenuPermissionService = distributorMenuPermissionService;
		this.distributorListQueryHandler = distributorListQueryHandler;
		this.distributorInfoAccessGate = distributorInfoAccessGate;
		this.distributorInfoQueryHandler = distributorInfoQueryHandler;
		this.distributorUpdateRequestGate = distributorUpdateRequestGate;
		this.distributorUpdateParamNormalizer = distributorUpdateParamNormalizer;
		this.distributorUpdateOrchestrator = distributorUpdateOrchestrator;
		this.distributorPaymentSubjectRequestGate = distributorPaymentSubjectRequestGate;
		this.distributorPaymentSubjectService = distributorPaymentSubjectService;
		this.distributorInfoResolveService = distributorInfoResolveService;
		this.distributeCountReadService = distributeCountReadService;
		this.distributorWxaCodeOrchestrator = distributorWxaCodeOrchestrator;
		this.distributorEasyListQueryService = distributorEasyListQueryService;
		this.distributorItemsCreateRequestGate = distributorItemsCreateRequestGate;
		this.distributorItemsListRequestGate = distributorItemsListRequestGate;
		this.distributorItemsListOrchestrator = distributorItemsListOrchestrator;
		this.distributorItemsSaveOrchestrator = distributorItemsSaveOrchestrator;
		this.distributorItemsExportRequestGate = distributorItemsExportRequestGate;
		this.distributorItemsExportOrchestrator = distributorItemsExportOrchestrator;
		this.distributorItemsDeleteService = distributorItemsDeleteService;
		this.distributorItemsUpdateRequestGate = distributorItemsUpdateRequestGate;
		this.distributorItemsUpdateOrchestrator = distributorItemsUpdateOrchestrator;
		this.distributorDefaultSetRequestGate = distributorDefaultSetRequestGate;
		this.distributorDefaultSetService = distributorDefaultSetService;
		this.distributorAdminListCoreService = distributorAdminListCoreService;
		this.distributorDeliveryDistanceUpdateService = distributorDeliveryDistanceUpdateService;
		this.distributorDeliveryDistanceQueryService = distributorDeliveryDistanceQueryService;
		this.offlineAftersalesDistributorListService = offlineAftersalesDistributorListService;
		this.langueProperties = langueProperties;
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.distributorListBackgroundRedisService = distributorListBackgroundRedisService;
	}

	@Activated(routeAlias = "distributor.create")
	@PostMapping(value = "/distributor", name = "创建店铺")
	public ResponseEntity<ApiResult<Map<String, Object>>> createDistributor(
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
		if (body != null && body.isEmpty()) {
			throw new BadRequestException("缺少必填字段: offline_aftersales_other");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		distributorCreateRequestGate.validateBeforeCreate(request, merged);
		distributorCreateParamNormalizer.normalize(merged, user);
		DistributorCreateParamValidator.validate(merged);
		String requestLang = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = distributorCreateOrchestrator.create(merged, user, requestLang);

		long companyId = toLong(ud.get("company_id"));
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "创建店铺");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
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

	private record DeliveryDistanceWriteParsed(int forDb, Object echoInData) {}

	private static DeliveryDistanceWriteParsed parseDeliveryDistanceForWrite(Object distRaw) {
		if (distRaw == null) {
			return new DeliveryDistanceWriteParsed(0, 0);
		}
		BigDecimal bd;
		try {
			if (distRaw instanceof Number n) {
				if (n instanceof Double d) {
					if (!Double.isFinite(d)) {
						throw new BadRequestException("distance 参数无效");
					}
					bd = BigDecimal.valueOf(d);
				} else if (n instanceof Float f) {
					if (!Float.isFinite(f)) {
						throw new BadRequestException("distance 参数无效");
					}
					bd = new BigDecimal(n.toString());
				} else {
					bd = new BigDecimal(n.toString());
				}
			} else {
				String s = distRaw.toString().trim();
				if (s.isEmpty()) {
					throw new BadRequestException("distance 参数无效");
				}
				bd = new BigDecimal(s);
			}
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException("distance 参数无效");
		}
		BigDecimal maxBd = BigDecimal.valueOf(Integer.MAX_VALUE);
		BigDecimal minBd = BigDecimal.valueOf(Integer.MIN_VALUE);
		if (bd.compareTo(maxBd) > 0 || bd.compareTo(minBd) < 0) {
			throw new BadRequestException("distance 参数无效");
		}
		int forDb = bd.intValue();
		BigDecimal norm = bd.stripTrailingZeros();
		Object echo = norm.scale() > 0 ? norm.toPlainString() : norm.intValueExact();
		return new DeliveryDistanceWriteParsed(forDb, echo);
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
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	@DataPass
	@Activated(routeAlias = "distributor.list")
	@GetMapping(value = "/distributors", name = "获取店铺列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorList(
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
		distributorMenuPermissionService.assertRouteAllowed(user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_LIST);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = distributorListQueryHandler.handle(request, user, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static final String SELECT_STORE_MSG = "请选择店铺";

	/** {@link #delDistributorItems} 校验失败时的错误消息。 */
	private static final String DEL_ITEMS_SELECT_STORE_MSG = "请选择需要删除的店铺";

	private static final String DEL_ITEMS_SELECT_GOODS_MSG = "请选择需要删除的商品";

	/** {@link #updateDistributorItem} 场景 8 校验失败时的错误消息。 */
	private static final String UPDATE_DIST_ITEM_SELECT_GOODS_MSG = "请先选择商品";

	/** {@link #updateDistributorItem} 场景 9：商品缺少 goods_id 时与 orchestrator 抛出的 {@link ResourceException} 消息一致。 */
	private static final String UPDATE_DIST_ITEM_UNDEFINED_GOODS_ID_MSG = "缺少必填字段: goods_id";

	private static final String SPECIFY_DISTRIBUTOR_COUNT_MSG = "请指定需要获取的经销商";

	/** 小程序码参数错误提示。 */
	private static final String MINIPROGRAM_CODE_PARAMS_MSG = "获取小程序码参数出错，请检查.";

	/** 兼容风格 wxacode 错误：HTTP 200 + {@code data.message} / {@code data.status_code} / 可选 {@code data.errors}。 */
	private static ResponseEntity<ApiResult<Map<String, Object>>> wxaCodeDingoStyle(String message, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static ResponseEntity<ApiResult<Map<String, Object>>> wxaCodeDingoStyle(
			String message, int statusCode, Map<String, Object> errors) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		data.put("errors", errors);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/** 兼容风格：HTTP 200 + {@code data.message} + {@code data.status_code}（如 count 路径 id=0）。 */
	private static ResponseEntity<ApiResult<Object>> distributorCountDingoStyle(String message, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "distributor.info")
	@GetMapping(value = "/distributors/info", name = "获取指定店铺信息")
	public ResponseEntity<?> getDistributorInfo(
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
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_INFO);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		normalizeDistributorIdDefault(merged);
		distributorInfoAccessGate.apply(request, user, merged);
		try {
			Map<String, Object> data = distributorInfoQueryHandler.handle(request, user, merged);
			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			if (!SELECT_STORE_MSG.equals(ex.getMessage())) {
				throw ex;
			}
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("message", SELECT_STORE_MSG);
			inner.put("status_code", 422);
			Map<String, Object> bodyOnlyData = new LinkedHashMap<>();
			bodyOnlyData.put("data", inner);
			return ResponseEntity.ok(bodyOnlyData);
		}
	}

	private static void normalizeDistributorIdDefault(Map<String, Object> merged) {
		Object v = merged.get("distributor_id");
		if (v == null || (v instanceof String s && !StringUtils.hasText(s))) {
			merged.put("distributor_id", 0L);
			return;
		}
		if (v instanceof Number n) {
			merged.put("distributor_id", n.longValue());
			return;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			merged.put("distributor_id", 0L);
			return;
		}
		try {
			merged.put("distributor_id", Long.parseLong(s));
		} catch (NumberFormatException e) {
			merged.put("distributor_id", 0L);
		}
	}

	@GetMapping(value = "/distributor/getAreaByAddress", name = "根据地址获取地区信息")
	public ResponseEntity<ApiResult<Object>> getAreaByAddress(
			HttpServletRequest request,
			@RequestParam(value = "address", required = false) String address) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		if (address == null || address.isEmpty() || "0".equals(address)) {
			throw new BadRequestException("详细地址必填");
		}
		Object data = companyMapGeocodePort.getLatAndLngByPositionRaw(companyId, address);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distributor.edit")
	@PutMapping(value = "/distributor/{distributor_id:\\d+}", name = "更新店铺")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDistributor(
			HttpServletRequest request,
			@PathVariable("distributor_id") String distributorIdRaw,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		final long pathDistributorId;
		try {
			pathDistributorId = Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("店铺ID无效");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		String reviewResult = request.getParameter("review_result");
		if (StringUtils.hasText(reviewResult) && !merged.containsKey("review_result")) {
			merged.put("review_result", reviewResult);
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorUpdateRequestGate.validateBeforeUpdate(request, merged, pathDistributorId);
		distributorUpdateParamNormalizer.normalize(merged, user, pathDistributorId);
		Set<String> datapassBlockCols = DistributorUpdateParamValidator.datapassBlockColsForShopCodeBranch(merged);
		DistributorUpdateParamValidator.validate(merged, pathDistributorId, datapassBlockCols);
		String requestLang = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = distributorUpdateOrchestrator.update(merged, user, pathDistributorId, requestLang, datapassBlockCols);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/" + pathDistributorId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "更新店铺");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "distributor.payment.subject.set")
	@PutMapping(value = "/distributor/{distributor_id:\\d+}/payment-subject", name = "设置店铺收款主体")
	public ResponseEntity<ApiResult<Map<String, Object>>> setPaymentSubject(
			HttpServletRequest request,
			@PathVariable("distributor_id") String distributorIdRaw,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		final long pathDistributorId;
		try {
			pathDistributorId = Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("店铺ID无效");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorPaymentSubjectRequestGate.validate(request, merged, pathDistributorId);
		PaymentSubjectParse paymentParse = parsePaymentSubject(merged);
		if (paymentParse instanceof PaymentSubjectParse.SqlBug) {
			return paymentSubjectDingoStyle(
					"SQLSTATE[HY000]: General error: 1366 Incorrect integer value: '' for column 'payment_subject' at row 1",
					500);
		}
		if (paymentParse instanceof PaymentSubjectParse.Invalid) {
			return paymentSubjectDingoStyle("payment_subject参数错误，只能为0（平台）或1（店铺）", 422);
		}
		int paymentSubject = ((PaymentSubjectParse.Valid) paymentParse).value();
		try {
			distributorPaymentSubjectService.setPaymentSubject(companyId, pathDistributorId, paymentSubject);
		} catch (ResourceException ex) {
			return paymentSubjectDingoStyle(ex.getMessage(), 422);
		}

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/" + pathDistributorId + "/payment-subject");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "设置店铺收款主体");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static ResponseEntity<ApiResult<Map<String, Object>>> paymentSubjectDingoStyle(String message, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/** 解析 {@code payment_subject} 的三种结果：合法取值、参数错误、将触发整型列写入异常的情形。 */
	private sealed interface PaymentSubjectParse permits PaymentSubjectParse.Valid, PaymentSubjectParse.Invalid, PaymentSubjectParse.SqlBug {
		record Valid(int value) implements PaymentSubjectParse {}
		record Invalid() implements PaymentSubjectParse {}
		record SqlBug() implements PaymentSubjectParse {}
	}

	/**
	 * 从 {@code merged} 读取 {@code payment_subject}，解析为收款主体：0 表示平台，1 表示店铺。
	 * <ul>
	 * <li>缺少键、值为 {@code null}、或字符串在 trim 前 {@link String#isEmpty()}：返回 {@link PaymentSubjectParse.SqlBug}（由调用方按数据库整型列错误响应）。</li>
	 * <li>{@link String}：trim 后若为空串则 {@link PaymentSubjectParse.Invalid}；字面 {@code "0"}/{@code "1"}，或经 {@link Double#parseDouble(String)} 解析后与 {@code 0.0}/{@code 1.0} 相等则为 {@link PaymentSubjectParse.Valid}；否则 {@link PaymentSubjectParse.Invalid}。</li>
	 * <li>{@link Boolean}：{@code false}→0，{@code true}→1。</li>
	 * <li>{@link Number}：{@link Number#doubleValue()} 为 {@code 0.0} 或 {@code 1.0} 则为合法，否则 {@link PaymentSubjectParse.Invalid}。</li>
	 * <li>其余类型：{@link PaymentSubjectParse.Invalid}。</li>
	 * </ul>
	 */
	private static PaymentSubjectParse parsePaymentSubject(Map<String, Object> merged) {
		if (!merged.containsKey("payment_subject")) {
			return new PaymentSubjectParse.SqlBug();
		}
		Object raw = merged.get("payment_subject");
		if (raw == null) {
			return new PaymentSubjectParse.SqlBug();
		}
		if (raw instanceof String s) {
			if (s.isEmpty()) {
				return new PaymentSubjectParse.SqlBug();
			}
			String t = s.trim();
			if (t.isEmpty()) {
				return new PaymentSubjectParse.Invalid();
			}
			if ("0".equals(t)) {
				return new PaymentSubjectParse.Valid(0);
			}
			if ("1".equals(t)) {
				return new PaymentSubjectParse.Valid(1);
			}
			try {
				double d = Double.parseDouble(t);
				if (d == 0.0) {
					return new PaymentSubjectParse.Valid(0);
				}
				if (d == 1.0) {
					return new PaymentSubjectParse.Valid(1);
				}
			} catch (NumberFormatException ignored) {
				// fall through
			}
			return new PaymentSubjectParse.Invalid();
		}
		if (raw instanceof Boolean b) {
			return new PaymentSubjectParse.Valid(b ? 1 : 0);
		}
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (d == 0.0) {
				return new PaymentSubjectParse.Valid(0);
			}
			if (d == 1.0) {
				return new PaymentSubjectParse.Valid(1);
			}
			return new PaymentSubjectParse.Invalid();
		}
		return new PaymentSubjectParse.Invalid();
	}

	@Activated(routeAlias = "front.wxapp.distributor.count")
	@GetMapping(value = "/distributor/count/{distributorId:\\d+}", name = "获取店铺统计")
	public ResponseEntity<ApiResult<Object>> getDistributorCount(
			HttpServletRequest request, @PathVariable("distributorId") String distributorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		if (distributorId == null) {
			return distributorCountDingoStyle(SPECIFY_DISTRIBUTOR_COUNT_MSG, 422);
		}
		String trimmed = distributorId.trim();
		if (!StringUtils.hasText(trimmed) || "0".equals(trimmed)) {
			return distributorCountDingoStyle(SPECIFY_DISTRIBUTOR_COUNT_MSG, 422);
		}
		final long pathParsedLong;
		try {
			pathParsedLong = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			// 非数字路径不会匹配到店铺 → 返回空列表
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		if (pathParsedLong <= 0) {
			return distributorCountDingoStyle(SPECIFY_DISTRIBUTOR_COUNT_MSG, 422);
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_COUNT);
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("distributor_id", pathParsedLong);
		distributorInfoAccessGate.apply(request, user, merged);
		String requestLang = RequestLangTag.current(langueProperties);
		Optional<Map<String, Object>> store =
				distributorInfoResolveService.resolveStoreDetail(companyId, pathParsedLong, requestLang);
		if (store.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(
				ApiResult.ok(distributeCountReadService.getDistributorCount(pathParsedLong)));
	}

	@Activated(routeAlias = "distributor.wxacode")
	@GetMapping(value = "/distributor/wxacode", name = "获取店铺小程序码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxaDistributorCodeStream(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(value = "codetype", required = false) String codeTypeRaw,
			@RequestParam(value = "template_name", required = false) String templateName) {
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
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_WXACODE);
		// distributor_id 必填且长度 >= 1；"0" 通过校验（按字符串长度判断）并进入 weapp 分支。
		if (!StringUtils.hasText(distributorIdRaw)) {
			Map<String, Object> errors = new LinkedHashMap<>();
			errors.put("distributor_id", List.of("validation.required"));
			return wxaCodeDingoStyle(MINIPROGRAM_CODE_PARAMS_MSG, 422, errors);
		}
		final long distributorId;
		try {
			distributorId = Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			Map<String, Object> errors = new LinkedHashMap<>();
			errors.put("distributor_id", List.of("validation.required"));
			return wxaCodeDingoStyle(MINIPROGRAM_CODE_PARAMS_MSG, 422, errors);
		}
		String codeType = StringUtils.hasText(codeTypeRaw) ? codeTypeRaw.trim() : "index";
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("distributor_id", distributorId);
		merged.put("codetype", codeType);
		distributorInfoAccessGate.apply(request, user, merged);
		try {
			Map<String, Object> data =
					distributorWxaCodeOrchestrator.buildWxaCodePayload(companyId, distributorId, codeType, templateName);
			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			return wxaCodeDingoStyle(ex.getMessage(), 422);
		}
	}

	@Activated(routeAlias = "distributor.easy.list")
	@GetMapping(value = "/distributor/easylist", name = "获取店铺简易列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getEasyList(
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
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_EASY_LIST);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		distributorInfoAccessGate.apply(request, user, merged);
		applyJwtDistributorIdsToMerged(ud, merged);
		Map<String, Object> data = distributorEasyListQueryService.buildEasyList(request, user, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private void applyJwtDistributorIdsToMerged(Map<?, ?> ud, Map<String, Object> merged) {
		List<Long> fromJwt = parseLongIdsFromJwt(ud.get("distributor_ids"));
		if (!fromJwt.isEmpty()) {
			merged.put("distributorIds", fromJwt);
		}
	}

	private List<Long> parseLongIdsFromJwt(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				if (o instanceof Map<?, ?> mm) {
					Long v = longOrNullEasyList(mm.get("distributor_id"));
					if (v != null) {
						out.add(v);
					}
				} else {
					Long v = longOrNullEasyList(o);
					if (v != null) {
						out.add(v);
					}
				}
			}
			return out;
		}
		if (raw instanceof String s && s.trim().startsWith("[")) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node.isArray()) {
					List<Long> out = new ArrayList<>();
					for (JsonNode n : node) {
						if (n.isObject()) {
							JsonNode idNode = n.get("distributor_id");
							if (idNode != null && idNode.isNumber()) {
								out.add(idNode.longValue());
							} else if (idNode != null && idNode.isTextual()) {
								Long v = longOrNullEasyList(idNode.asText());
								if (v != null) {
									out.add(v);
								}
							}
						} else if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual()) {
							Long v = longOrNullEasyList(n.asText());
							if (v != null) {
								out.add(v);
							}
						}
					}
					return out;
				}
			} catch (Exception ignored) {
				return List.of();
			}
		}
		Long single = longOrNullEasyList(raw);
		if (single != null) {
			return List.of(single);
		}
		return List.of();
	}

	private static Long longOrNullEasyList(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Activated(routeAlias = "distributor.item.list")
	@GetMapping(value = "/distributor/items", name = "获取店铺关联商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> operatorJwt = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			operatorJwt.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(
				operatorJwt, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_ITEM_LIST);
		distributorItemsListRequestGate.assertCanList(request, merged);
		Map<String, Object> data =
				distributorItemsListOrchestrator.list(request, operatorJwt, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distributor.item.create")
	@PostMapping(value = "/distributor/items", name = "添加店铺关联店铺商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveDistributorItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> operatorJwt = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			operatorJwt.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		try {
			distributorItemsCreateRequestGate.validateBeforeSave(request, merged);
			DistributorItemsSaveRequestValidator.validate(merged);
		} catch (ResourceException ex) {
			return wxaCodeDingoStyle(ex.getMessage(), 422);
		}
		SaveDistributorItemsResult out = distributorItemsSaveOrchestrator.save(request, operatorJwt, merged);
		long companyId = toLong(ud.get("company_id"));
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/items");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加店铺关联店铺商品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);
		return ResponseEntity.ok(
				ApiResult.ok(Map.of("status", out.status(), "res", out.res() != null ? out.res() : true)));
	}

	@Activated(routeAlias = "distributor.item.exportlist")
	@GetMapping(value = "/distributor/items/export", name = "导出店铺关联商品列表")
	public ResponseEntity<?> exportDistributorItems(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> operatorJwt = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			operatorJwt.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(operatorJwt,
				DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_ITEM_EXPORTLIST);
		try {
			distributorItemsExportRequestGate.assertCanExport(request, merged);
			distributorItemsExportOrchestrator.submitExport(request, operatorJwt, merged);
		} catch (ResourceException ex) {
			if (!SELECT_STORE_MSG.equals(ex.getMessage())) {
				throw ex;
			}
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("message", SELECT_STORE_MSG);
			inner.put("status_code", 422);
			Map<String, Object> bodyOnlyData = new LinkedHashMap<>();
			bodyOnlyData.put("data", inner);
			return ResponseEntity.ok(bodyOnlyData);
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "distributor.item.delete")
	@DeleteMapping(value = "/distributor/items", name = "删除经销商商品")
	public ResponseEntity<?> delDistributorItems(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> operatorJwt = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			operatorJwt.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorItemsListRequestGate.assertCanDelete(request, merged);
		DistributorItemsDeleteRequestValidator.ParseResult parsed;
		try {
			parsed = DistributorItemsDeleteRequestValidator.validateAndParse(merged);
		} catch (ResourceException ex) {
			String msg = ex.getMessage();
			if (DEL_ITEMS_SELECT_STORE_MSG.equals(msg) || DEL_ITEMS_SELECT_GOODS_MSG.equals(msg)) {
				Map<String, Object> inner = new LinkedHashMap<>();
				inner.put("message", msg);
				inner.put("status_code", 422);
				Map<String, Object> bodyOnlyData = new LinkedHashMap<>();
				bodyOnlyData.put("data", inner);
				return ResponseEntity.ok(bodyOnlyData);
			}
			throw ex;
		}
		distributorItemsDeleteService.deleteByFilter(companyId, parsed.distributorId(), parsed.goodsIds());

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/items");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "删除经销商商品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "distributor.item.update")
	@PutMapping(value = "/distributors/item", name = "配置店铺价格或库存")
	public ResponseEntity<?> updateDistributorItem(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> operatorJwt = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			operatorJwt.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorItemsUpdateRequestGate.assertCanUpdate(request, merged);
		try {
			distributorItemsUpdateOrchestrator.update(request, operatorJwt, merged);

			long operatorId = toLong(ud.get("operator_id"));
			Map<String, Object> logCtx = new LinkedHashMap<>();
			logCtx.put("company_id", companyId);
			logCtx.put("operator_id", (int) operatorId);
			logCtx.put("request_uri", "/api/v1/distributors/item");
			logCtx.put("ip", clientIp(request));
			try {
				logCtx.put("params", objectMapper.writeValueAsString(merged));
			} catch (Exception e) {
				logCtx.put("params", merged.toString());
			}
			logCtx.put("operator_name", "配置店铺价格或库存");
			logCtx.put("log_type", "operator");
			Object merchantId = ud.get("merchant_id");
			if (merchantId != null) {
				logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
			}
			operatorLogsWriteService.addLogs(logCtx);

			return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
		} catch (BadRequestException e) {
			if (UPDATE_DIST_ITEM_SELECT_GOODS_MSG.equals(e.getMessage())) {
				Map<String, Object> inner = new LinkedHashMap<>();
				inner.put("message", UPDATE_DIST_ITEM_SELECT_GOODS_MSG);
				inner.put("status_code", 422);
				Map<String, Object> bodyOnlyData = new LinkedHashMap<>();
				bodyOnlyData.put("data", inner);
				return ResponseEntity.ok(bodyOnlyData);
			}
			throw e;
		} catch (ResourceException e) {
			if (UPDATE_DIST_ITEM_UNDEFINED_GOODS_ID_MSG.equals(e.getMessage())) {
				throw new BadRequestException(UPDATE_DIST_ITEM_UNDEFINED_GOODS_ID_MSG);
			}
			throw e;
		}
	}

	@DataPass
	@Activated(routeAlias = "distributor.get.shop")
	@GetMapping(value = "/distributor/getShop", name = "获取有效的门店列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getValidShopList(
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
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		DistributorAdminListContext ctx = new DistributorAdminListContext();
		ctx.setMergedInput(merged);
		ctx.setJwt(user);
		ctx.setRequestLang(RequestLangTag.current(langueProperties));
		ctx.setStoresOnlyMode(true);
		ctx.setProductModel("");
		DistributorAdminListResult core = distributorAdminListCoreService.buildCore(ctx);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", core.getList());
		data.put("total_count", core.getTotalCount());
		data.put("tagList", core.getTopTagList());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distributor.default.set")
	@PostMapping(value = "/distributor/default", name = "设置默认门店")
	public ResponseEntity<?> defaultSetDistributor(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}

		Object rawDid = merged.get("distributor_id");
		if (rawDid == null) {
			return defaultSetDistributorDingoStyle(411, "店铺必选！");
		}
		final long targetDistributorId;
		if (rawDid instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return defaultSetDistributorDingoStyle(411, "店铺必选！");
			}
			long parsed;
			try {
				parsed = Long.parseLong(t);
			} catch (NumberFormatException e) {
				return defaultSetDistributorDingoStyle(422, "distributor_id=" + t + "的店铺不存在");
			}
			if (parsed <= 0) {
				return defaultSetDistributorDingoStyle(411, "店铺必选！");
			}
			targetDistributorId = parsed;
		} else if (rawDid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				return defaultSetDistributorDingoStyle(411, "店铺必选！");
			}
			targetDistributorId = v;
		} else {
			return defaultSetDistributorDingoStyle(422,
					"distributor_id=" + String.valueOf(rawDid) + "的店铺不存在");
		}

		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorDefaultSetRequestGate.validate(request, merged, targetDistributorId);
		try {
			distributorDefaultSetService.setDefaultDistributor(companyId, targetDistributorId);
		} catch (ResourceException ex) {
			return defaultSetDistributorDingoStyle(422,
					"distributor_id=" + targetDistributorId + "的店铺不存在");
		}

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distributor/default");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "设置默认门店");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number mn ? mn.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "distributor.aftersales.list")
	@GetMapping(value = "/distributors/aftersales", name = "获取可退货店铺列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOtherOfflineAftersalesDistributor(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdParam,
			@RequestParam(value = "merchant_id", required = false, defaultValue = "0") String merchantIdParam,
			@RequestParam(value = "is_selected", required = false, defaultValue = "0") String isSelected,
			@RequestParam(value = "distributor_name", required = false) String distributorName,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageParam,
			@RequestParam(value = "pageSize", required = false, defaultValue = "10") String pageSizeParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(user.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		long requestDistributorId = Optional.ofNullable(longOrNullEasyList(distributorIdParam)).orElse(0L);
		long requestMerchantId = Optional.ofNullable(longOrNullEasyList(merchantIdParam)).orElse(0L);
		int page = 1;
		Long nPage = longOrNullEasyList(pageParam);
		if (nPage != null && nPage >= 1L && nPage <= (long) Integer.MAX_VALUE) {
			page = nPage.intValue();
		}
		int pageSize = 10;
		Long nPs = longOrNullEasyList(pageSizeParam);
		if (nPs != null && nPs >= 1L && nPs <= (long) Integer.MAX_VALUE) {
			pageSize = nPs.intValue();
		}
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> result =
				offlineAftersalesDistributorListService.getOtherOfflineAftersalesDistributor(
						companyId,
						requestDistributorId,
						requestMerchantId,
						isSelected,
						distributorName,
						page,
						pageSize,
						requestLang);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "distribution.distance.get")
	@GetMapping(value = "/distribution/getdistance", name = "获取距离配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistance(
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
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTION_DISTANCE_GET);
		long distributorId = parseSingleDistributorIdForGetDistance(request, body);
		int distance = distributorDeliveryDistanceQueryService.getDistance(companyId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("distance", distance)));
	}

	private long parseSingleDistributorIdForGetDistance(HttpServletRequest request, Map<String, Object> body) {
		List<String> fromQuery = new ArrayList<>();
		appendTrimmedNonEmptyParameterValues(request.getParameterValues("distributor_id"), fromQuery);
		appendTrimmedNonEmptyParameterValues(request.getParameterValues("distributor_id[]"), fromQuery);

		List<String> fromBody = new ArrayList<>();
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike && body != null && body.containsKey("distributor_id")) {
			Object rawDid = body.get("distributor_id");
			if (rawDid != null) {
				appendDistributorIdSegmentsFromBodyValue(rawDid, fromBody);
			}
		}

		List<String> segments = new ArrayList<>(fromQuery);
		segments.addAll(fromBody);

		if (segments.size() > 1) {
			throw new ResourceException("只能查看单个店铺的配送距离");
		}
		if (segments.isEmpty()) {
			throw new ResourceException("店铺ID必填");
		}
		Long parsed = longOrNullEasyList(segments.get(0));
		if (parsed == null || parsed <= 0L) {
			throw new ResourceException("店铺ID必填");
		}
		return parsed;
	}

	private static void appendTrimmedNonEmptyParameterValues(String[] values, List<String> out) {
		if (values == null) {
			return;
		}
		for (String v : values) {
			if (v == null) {
				continue;
			}
			String t = v.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
	}

	private static void appendDistributorIdSegmentsFromBodyValue(Object v, List<String> out) {
		if (v == null) {
			return;
		}
		if (v instanceof Collection<?> c) {
			for (Object o : c) {
				appendOneDistributorIdSegmentFromScalar(o, out);
			}
			return;
		}
		if (v instanceof Object[] arr) {
			for (Object o : arr) {
				appendOneDistributorIdSegmentFromScalar(o, out);
			}
			return;
		}
		if (v instanceof String[] arr) {
			for (String o : arr) {
				appendOneDistributorIdSegmentFromScalar(o, out);
			}
			return;
		}
		if (v instanceof long[] arr) {
			for (long n : arr) {
				out.add(Long.toString(n));
			}
			return;
		}
		if (v instanceof int[] arr) {
			for (int n : arr) {
				out.add(Integer.toString(n));
			}
			return;
		}
		if (v instanceof short[] arr) {
			for (short n : arr) {
				out.add(Short.toString(n));
			}
			return;
		}
		if (v instanceof byte[] arr) {
			for (byte n : arr) {
				out.add(Byte.toString(n));
			}
			return;
		}
		if (v instanceof double[] arr) {
			for (double n : arr) {
				out.add(Double.toString(n));
			}
			return;
		}
		if (v instanceof float[] arr) {
			for (float n : arr) {
				out.add(Float.toString(n));
			}
			return;
		}
		if (v instanceof boolean[] arr) {
			for (boolean n : arr) {
				out.add(Boolean.toString(n));
			}
			return;
		}
		appendOneDistributorIdSegmentFromScalar(v, out);
	}

	private static void appendOneDistributorIdSegmentFromScalar(Object o, List<String> out) {
		if (o == null) {
			return;
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
			return;
		}
		if (o instanceof Number n) {
			out.add(n.toString());
			return;
		}
		if (o instanceof Boolean b) {
			out.add(Boolean.toString(b));
			return;
		}
		String t = o.toString().trim();
		if (!t.isEmpty()) {
			out.add(t);
		}
	}

	@GetMapping(value = "/distributor/list/background", name = "获取门店列表背景图")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorListBackground(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		String backgroundUrl = distributorListBackgroundRedisService.getBackgroundUrl(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("background_url", backgroundUrl)));
	}

	@PostMapping(value = "/distributor/list/background", name = "保存门店列表背景图")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveDistributorListBackground(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@RequestParam(value = "background_url", required = false) String backgroundUrlParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String backgroundUrl = resolveBackgroundUrl(merged, backgroundUrlParam);
		if (!StringUtils.hasText(backgroundUrl)) {
			throw new BadRequestException("背景图URL不能为空");
		}
		backgroundUrl = backgroundUrl.trim();
		if (!distributorListBackgroundRedisService.setBackgroundUrl(companyId, backgroundUrl)) {
			throw new ResourceException("背景图保存失败");
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		data.put("background_url", backgroundUrl);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveBackgroundUrl(Map<String, Object> merged, String backgroundUrlParam) {
		if (StringUtils.hasText(backgroundUrlParam)) {
			return backgroundUrlParam;
		}
		Object v = merged.get("background_url");
		return v == null ? null : String.valueOf(v);
	}

	@Activated(routeAlias = "distribution.distance.save")
	@PostMapping(value = "/distribution/setdistance", name = "保存距离配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setDistance(
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
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTION_DISTANCE_SAVE);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		DeliveryDistanceWriteParsed parsed = parseDeliveryDistanceForWrite(merged.get("distance"));
		distributorDeliveryDistanceUpdateService.setDistance(user, merged, parsed.forDb());
		return ResponseEntity.ok(ApiResult.ok(Map.of("distance", parsed.echoInData())));
	}

	/** 兼容风格响应体：HTTP 200 + {@code {"data":{"message","status_code"}}}（不使用 ApiResult 包装）。 */
	private static ResponseEntity<?> defaultSetDistributorDingoStyle(int statusCode, String message) {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("message", message);
		inner.put("status_code", statusCode);
		Map<String, Object> bodyOnlyData = new LinkedHashMap<>();
		bodyOnlyData.put("data", inner);
		return ResponseEntity.ok(bodyOnlyData);
	}
}
