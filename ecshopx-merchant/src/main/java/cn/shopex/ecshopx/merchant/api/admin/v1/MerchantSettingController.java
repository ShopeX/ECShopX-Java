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

package cn.shopex.ecshopx.merchant.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.merchant.port.MerchantShopRoutePermissionPort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.merchant.service.MerchantListParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantTypeCreateService;
import cn.shopex.ecshopx.merchant.service.MerchantTypeDeleteService;
import cn.shopex.ecshopx.merchant.service.MerchantTypeListQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantTypeUpdateService;
import cn.shopex.ecshopx.merchant.service.MerchantTypeVisibleListQueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("merchantAdminV1MerchantSetting")
@RequestMapping("/api/v1/merchant")
public class MerchantSettingController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final List<String> CREATE_TYPE_PARAM_KEYS = List.of("name", "sort", "parent_id", "is_show");

	private static final List<String> SAVE_BASE_PARAM_KEYS =
			List.of("status", "display_on_pc", "settled_type", "content");

	private final ObjectMapper objectMapper;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final MerchantTypeCreateService merchantTypeCreateService;
	private final MerchantTypeUpdateService merchantTypeUpdateService;
	private final MerchantTypeDeleteService merchantTypeDeleteService;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final MerchantBaseSettingQueryService merchantBaseSettingQueryService;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final MerchantShopRoutePermissionPort merchantShopRoutePermissionPort;
	private final MerchantTypeListQueryService merchantTypeListQueryService;
	private final MerchantTypeVisibleListQueryService merchantTypeVisibleListQueryService;

	public MerchantSettingController(
			ObjectMapper objectMapper,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			MerchantTypeCreateService merchantTypeCreateService,
			MerchantTypeUpdateService merchantTypeUpdateService,
			MerchantTypeDeleteService merchantTypeDeleteService,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			MerchantBaseSettingQueryService merchantBaseSettingQueryService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			MerchantShopRoutePermissionPort merchantShopRoutePermissionPort,
			MerchantTypeListQueryService merchantTypeListQueryService,
			MerchantTypeVisibleListQueryService merchantTypeVisibleListQueryService) {
		this.objectMapper = objectMapper;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.merchantTypeCreateService = merchantTypeCreateService;
		this.merchantTypeUpdateService = merchantTypeUpdateService;
		this.merchantTypeDeleteService = merchantTypeDeleteService;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.merchantBaseSettingQueryService = merchantBaseSettingQueryService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.merchantShopRoutePermissionPort = merchantShopRoutePermissionPort;
		this.merchantTypeListQueryService = merchantTypeListQueryService;
		this.merchantTypeVisibleListQueryService = merchantTypeVisibleListQueryService;
	}

	@Activated(routeAlias = "merchant.basesetting.get")
	@GetMapping(value = "/basesetting", name = "获取基础设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBase(HttpServletRequest request) {
		long companyId = requireOperatorCompanyId(request);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> data = merchantBaseSettingQueryService.getBaseSetting(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "merchant.basesetting.save")
	@PostMapping(value = "/basesetting", name = "保存基础设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveBase(HttpServletRequest request) throws IOException {
		Map<String, Object> params = collectSaveBaseParams(request);

		long companyId = requireOperatorCompanyId(request);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		merchantBaseSettingSaveService.saveBase(companyId, params);

		Map<?, ?> ud = (Map<?, ?>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/merchant/basesetting");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(params));
		} catch (Exception e) {
			logCtx.put("params", params.toString());
		}
		logCtx.put("operator_name", "保存基础设置");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private Map<String, Object> collectSaveBaseParams(HttpServletRequest request) throws IOException {
		Map<String, Object> merged = new LinkedHashMap<>();
		for (String key : SAVE_BASE_PARAM_KEYS) {
			String[] vals = request.getParameterValues(key);
			if (vals == null || vals.length == 0) {
				vals = request.getParameterValues(key + "[]");
			}
			if (vals != null && vals.length > 0) {
				if (vals.length == 1) {
					merged.put(key, vals[0]);
				} else {
					merged.put(key, new ArrayList<>(Arrays.asList(vals)));
				}
			}
		}
		String ct = request.getContentType();
		if (ct != null) {
			String ctl = ct.toLowerCase(Locale.ROOT);
			if (ctl.contains("application/json")) {
				byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
				if (buf.length > 0) {
					Map<String, Object> jsonMap = objectMapper.readValue(buf, new TypeReference<Map<String, Object>>() {});
					for (String k : SAVE_BASE_PARAM_KEYS) {
						if (jsonMap.containsKey(k)) {
							merged.put(k, jsonMap.get(k));
						}
					}
				}
			}
		}
		return merged;
	}

	@Activated(routeAlias = "merchant.type.list")
	@GetMapping(value = "/type/list", name = "商户类型列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getTypeList(
			HttpServletRequest request,
			@RequestParam(value = "sort_order_by", required = false) String sortOrderBy,
			@RequestParam(value = "is_show_children", required = false, defaultValue = "true") String isShowChildrenRaw,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "country_code", required = false) String countryCode,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
		Map<String, Object> user = requireOperatorUserMap(request);
		long companyId = toLong(user.get("company_id"));
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);
		merchantShopRoutePermissionPort.assertMerchantTypeListAllowed(user);

		boolean isShowChildrenFlag = !"false".equals(isShowChildrenRaw);

		String langTag = resolveTypeListLangTag(request, countryCode, acceptLanguage);

		List<Map<String, Object>> data = merchantTypeListQueryService.queryTypeTree(
				companyId, sortOrderBy, isShowChildrenFlag, name, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveTypeListLangTag(
			HttpServletRequest request, String countryCodeParam, String acceptLanguage) {
		Object ccRaw =
				countryCodeParam != null
						? MerchantListParamValidator.scalarFrom(countryCodeParam)
						: scalarCountryCodeFromRequest(request);
		if (ccRaw != null) {
			String t = ccRaw.toString().trim();
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		if (StringUtils.hasText(acceptLanguage)) {
			return acceptLanguage.trim();
		}
		return "zh-CN";
	}

	private static Object scalarCountryCodeFromRequest(HttpServletRequest request) {
		String[] vs = request.getParameterValues("country_code");
		if (vs == null || vs.length == 0) {
			return null;
		}
		if (vs.length == 1) {
			return MerchantListParamValidator.scalarFrom(vs[0]);
		}
		return MerchantListParamValidator.scalarFrom(java.util.Arrays.asList(vs));
	}

	@Activated(routeAlias = "merchant.type.create")
	@PostMapping(value = "/type/create", name = "新增商户类型")
	public ResponseEntity<ApiResult<Map<String, Object>>> createType(
			HttpServletRequest request,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage)
			throws IOException {
		Map<String, Object> params = collectCreateTypeParams(request);

		long companyId = requireOperatorCompanyId(request);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> result = merchantTypeCreateService.createMerchantType(companyId, params, acceptLanguage);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private Map<String, Object> collectCreateTypeParams(HttpServletRequest request) throws IOException {
		Map<String, Object> merged = new LinkedHashMap<>();
		for (String key : CREATE_TYPE_PARAM_KEYS) {
			String[] vals = request.getParameterValues(key);
			if (vals == null || vals.length == 0) {
				vals = request.getParameterValues(key + "[]");
			}
			if (vals != null && vals.length > 0) {
				if (vals.length == 1) {
					merged.put(key, vals[0]);
				} else {
					merged.put(key, new ArrayList<>(Arrays.asList(vals)));
				}
			}
		}
		String ct = request.getContentType();
		if (ct != null) {
			String ctl = ct.toLowerCase(Locale.ROOT);
			if (ctl.contains("application/json")) {
				byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
				if (buf.length > 0) {
					Map<String, Object> jsonMap = objectMapper.readValue(buf, new TypeReference<Map<String, Object>>() {});
					for (String k : CREATE_TYPE_PARAM_KEYS) {
						if (jsonMap.containsKey(k)) {
							merged.put(k, jsonMap.get(k));
						}
					}
				}
			}
		}
		return merged;
	}

	private Map<String, Object> requireOperatorUserMap(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return user;
	}

	private long requireOperatorCompanyId(HttpServletRequest request) {
		Map<String, Object> user = requireOperatorUserMap(request);
		return toLong(user.get("company_id"));
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

	@Activated(routeAlias = "merchant.type.update")
	@PutMapping(value = "/type/{id}", name = "更新商户类型")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateType(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage)
			throws IOException {
		Map<String, Object> params = collectCreateTypeParams(request);

		long companyId = requireOperatorCompanyId(request);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String tid = id == null ? "" : id.trim();
		long typeId;
		try {
			typeId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的类型ID");
		}
		if (typeId <= 0) {
			throw new BadRequestException("无效的类型ID");
		}

		merchantTypeUpdateService.updateMerchantType(companyId, typeId, params, acceptLanguage);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "merchant.type.delete")
	@DeleteMapping(value = "/type/{id}", name = "删除商户类型")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteType(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> user = requireOperatorUserMap(request);
		long companyId = toLong(user.get("company_id"));
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);
		merchantShopRoutePermissionPort.assertMerchantTypeDeleteAllowed(user);

		String tid = id == null ? "" : id.trim();
		long typeId;
		try {
			typeId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的类型ID");
		}
		if (typeId <= 0) {
			throw new BadRequestException("无效的类型ID");
		}

		merchantTypeDeleteService.deleteMerchantType(companyId, typeId);

		Map<?, ?> ud = (Map<?, ?>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", request.getRequestURI());
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", typeId)));
		} catch (Exception e) {
			logCtx.put("params", String.valueOf(typeId));
		}
		logCtx.put("operator_name", "删除商户类型");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "merchant.visibletype.list")
	@GetMapping(value = "/visibletype/list", name = "可见商户类型列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getVisibleTypeList(
			HttpServletRequest request,
			@RequestParam(value = "parent_id", required = false, defaultValue = "0") String parentIdRaw,
			@RequestParam(value = "name", required = false) String name) {
		Map<String, Object> user = requireOperatorUserMap(request);
		long companyId = toLong(user.get("company_id"));
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);
		merchantShopRoutePermissionPort.assertMerchantVisibleTypeListAllowed(user);

		long parentId = parseParentId(parentIdRaw);
		List<Map<String, Object>> data =
				merchantTypeVisibleListQueryService.queryVisibleTypeList(companyId, parentId, name, "zh-CN");
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseParentId(String parentIdRaw) {
		if (parentIdRaw == null) {
			return 0L;
		}
		String t = parentIdRaw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0) {
				throw new BadRequestException("无效的 parent_id");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的 parent_id");
		}
	}
}
