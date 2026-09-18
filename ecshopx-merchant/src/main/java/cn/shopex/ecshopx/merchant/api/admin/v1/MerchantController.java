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
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.merchant.port.MerchantShopRoutePermissionPort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorDatapassApplyPort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.merchant.service.MerchantAuditGoodsUpdateService;
import cn.shopex.ecshopx.merchant.service.MerchantCreateService;
import cn.shopex.ecshopx.merchant.service.MerchantDataMasking;
import cn.shopex.ecshopx.merchant.service.MerchantDetailQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantDisabledUpdateService;
import cn.shopex.ecshopx.merchant.service.MerchantListParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantListQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantListRequestParamsResolver;
import cn.shopex.ecshopx.merchant.service.MerchantUpdateMerchantService;
import cn.shopex.ecshopx.merchant.web.MerchantDatapassBlockSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("merchantAdminV1Merchant")
@RequestMapping("/api/v1/merchant")
public class MerchantController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final List<String> CREATE_MERCHANT_PARAM_KEYS = List.of(
			"merchant_type_id",
			"settled_type",
			"merchant_name",
			"social_credit_code_id",
			"regions_id",
			"regions",
			"address",
			"legal_name",
			"legal_cert_id",
			"legal_mobile",
			"email",
			"bank_acct_type",
			"card_id_mask",
			"bank_name",
			"bank_mobile",
			"audit_goods",
			"license_url",
			"legal_certid_front_url",
			"legal_cert_id_back_url",
			"bank_card_front_url",
			"contract_url",
			"mobile",
			"settled_succ_sendsms");

	private static final List<String> UPDATE_MERCHANT_PARAM_KEYS = List.of(
			"merchant_type_id",
			"regions_id",
			"regions",
			"address",
			"legal_name",
			"legal_cert_id",
			"legal_mobile",
			"email",
			"audit_goods",
			"license_url",
			"legal_certid_front_url",
			"legal_cert_id_back_url",
			"bank_card_front_url",
			"contract_url",
			"bank_acct_type",
			"card_id_mask",
			"bank_name",
			"bank_mobile");

	private final ObjectMapper objectMapper;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final MerchantCreateService merchantCreateService;
	private final MerchantAuditGoodsUpdateService merchantAuditGoodsUpdateService;
	private final MerchantDisabledUpdateService merchantDisabledUpdateService;
	private final MerchantUpdateMerchantService merchantUpdateMerchantService;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final ShopOperatorDatapassApplyPort shopOperatorDatapassApplyPort;
	private final MerchantShopRoutePermissionPort merchantShopRoutePermissionPort;
	private final MerchantListRequestParamsResolver merchantListRequestParamsResolver;
	private final MerchantListParamValidator merchantListParamValidator;
	private final MerchantListQueryService merchantListQueryService;
	private final MerchantDetailQueryService merchantDetailQueryService;

	public MerchantController(
			ObjectMapper objectMapper,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			MerchantCreateService merchantCreateService,
			MerchantAuditGoodsUpdateService merchantAuditGoodsUpdateService,
			MerchantDisabledUpdateService merchantDisabledUpdateService,
			MerchantUpdateMerchantService merchantUpdateMerchantService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ShopOperatorDatapassApplyPort shopOperatorDatapassApplyPort,
			MerchantShopRoutePermissionPort merchantShopRoutePermissionPort,
			MerchantListRequestParamsResolver merchantListRequestParamsResolver,
			MerchantListParamValidator merchantListParamValidator,
			MerchantListQueryService merchantListQueryService,
			MerchantDetailQueryService merchantDetailQueryService) {
		this.objectMapper = objectMapper;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.merchantCreateService = merchantCreateService;
		this.merchantAuditGoodsUpdateService = merchantAuditGoodsUpdateService;
		this.merchantDisabledUpdateService = merchantDisabledUpdateService;
		this.merchantUpdateMerchantService = merchantUpdateMerchantService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.shopOperatorDatapassApplyPort = shopOperatorDatapassApplyPort;
		this.merchantShopRoutePermissionPort = merchantShopRoutePermissionPort;
		this.merchantListRequestParamsResolver = merchantListRequestParamsResolver;
		this.merchantListParamValidator = merchantListParamValidator;
		this.merchantListQueryService = merchantListQueryService;
		this.merchantDetailQueryService = merchantDetailQueryService;
	}

	@DataPass
	@Activated(routeAlias = "merchant.list")
	@GetMapping(value = "/list", name = "merchant.list")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@SuppressWarnings("unused") @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
					String acceptLanguage)
			throws IOException {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		merchantShopRoutePermissionPort.assertMerchantListAllowed(user);
		shopOperatorDatapassApplyPort.apply(request, user, "merchant.list");

		Map<String, Object> params = merchantListRequestParamsResolver.resolveToMap(request);
		merchantListParamValidator.validateFromMap(params);

		Object ccRaw = MerchantListParamValidator.scalarFrom(params.get("country_code"));
		String langTag = "zh-CN";
		if (ccRaw != null) {
			String t = ccRaw.toString().trim();
			if (StringUtils.hasText(t)) {
				langTag = t;
			}
		}

		Map<String, Object> body = merchantListQueryService.query(companyId, params, langTag);
		Object datapassEcho = MerchantDatapassBlockSupport.resolveEchoValue(request);
		body.put("datapass_block", datapassEcho);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
		MerchantDataMasking.applyListIfNeeded(list, MerchantDatapassBlockSupport.shouldMask(datapassEcho));

		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@DataPass
	@Activated(routeAlias = "merchant.detail.get")
	@GetMapping(value = "/detail/{id}", name = "merchant.detail.get")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDetail(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestParam(value = "action", required = false) String action,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String tid = id == null ? "" : id.trim();
		long merchantId;
		try {
			merchantId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的商户ID");
		}
		if (merchantId <= 0) {
			throw new BadRequestException("无效的商户ID");
		}

		shopOperatorDatapassApplyPort.apply(request, user, "merchant.detail.get");
		Map<String, Object> result = merchantDetailQueryService.buildDetailRow(merchantId, acceptLanguage);

		if ("edit".equals(action == null ? "" : action.trim())) {
			return ResponseEntity.ok(ApiResult.ok(result));
		}
		result.put("datapass_block", MerchantDatapassBlockSupport.resolveEchoValue(request));
		if (MerchantDatapassBlockSupport.shouldMask(result.get("datapass_block"))) {
			MerchantDataMasking.applyDetail(result);
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "merchant.disabled.update")
	@PostMapping(value = "/disabled/update/{id}", name = "修改商户禁用状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMerchantDisabled(
			HttpServletRequest request, @PathVariable("id") String id) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String tid = id == null ? "" : id.trim();
		long merchantId;
		try {
			merchantId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的商户ID");
		}
		if (merchantId <= 0) {
			throw new BadRequestException("无效的商户ID");
		}

		merchantDisabledUpdateService.updateByMerchantId(merchantId, readDisabledRaw(request));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "merchant.audit.goods.update")
	@PostMapping(value = "/auditgoods/update/{id}", name = "修改审核商品状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMerchantAuditGoods(
			HttpServletRequest request, @PathVariable("id") String id) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String tid = id == null ? "" : id.trim();
		long merchantId;
		try {
			merchantId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的商户ID");
		}
		if (merchantId <= 0) {
			throw new BadRequestException("无效的商户ID");
		}

		merchantAuditGoodsUpdateService.updateByMerchantId(merchantId, readAuditGoodsRaw(request));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "merchant.info.get")
	@GetMapping(value = "/info", name = "商户信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInfo(
			HttpServletRequest request,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		long merchantId = parseMerchantIdFromJwtUser(user.get("merchant_id"));
		Map<String, Object> result = merchantDetailQueryService.buildDetailRow(merchantId, acceptLanguage);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "merchant.update")
	@PostMapping(value = "/{id}", name = "更新商户")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMerchant(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage)
			throws IOException {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String tid = id == null ? "" : id.trim();
		long merchantId;
		try {
			merchantId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的商户ID");
		}
		if (merchantId <= 0) {
			throw new BadRequestException("无效的商户ID");
		}

		Map<String, Object> merged = collectUpdateMerchantParams(request);
		merchantUpdateMerchantService.update(merchantId, companyId, merged, acceptLanguage);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/merchant/" + tid);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "更新商户");
		logCtx.put("log_type", "operator");
		Object merchantIdObj = ud.get("merchant_id");
		if (merchantIdObj != null) {
			logCtx.put(
					"merchant_id",
					merchantIdObj instanceof Number n ? n.longValue() : Long.parseLong(merchantIdObj.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "merchant.create")
	@PostMapping(name = "新增商户")
	public ResponseEntity<ApiResult<Map<String, Object>>> createMerchant(
			HttpServletRequest request,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage)
			throws IOException {
		Map<String, Object> params = collectCreateMerchantParams(request);

		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);
		params.put("company_id", companyId);
		params.put("source", "admin");
		params.put("disabled", false);

		Map<String, Object> data = merchantCreateService.createMerchant(params, resolveMerchantLangTag(request));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveMerchantLangTag(HttpServletRequest request) {
		String[] vs = request.getParameterValues("country_code");
		if (vs != null && vs.length > 0 && StringUtils.hasText(vs[0].trim())) {
			return vs[0].trim();
		}
		return "zh-CN";
	}

	private Map<String, Object> collectCreateMerchantParams(HttpServletRequest request) throws IOException {
		Map<String, Object> merged = new LinkedHashMap<>();
		for (String key : CREATE_MERCHANT_PARAM_KEYS) {
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
					for (String k : CREATE_MERCHANT_PARAM_KEYS) {
						if (jsonMap.containsKey(k)) {
							merged.put(k, jsonMap.get(k));
						}
					}
				}
			}
		}
		return merged;
	}

	private Map<String, Object> collectUpdateMerchantParams(HttpServletRequest request) throws IOException {
		Map<String, Object> merged = new LinkedHashMap<>();
		for (String key : UPDATE_MERCHANT_PARAM_KEYS) {
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
					try {
						JsonNode root = objectMapper.readTree(buf);
						if (!root.isObject()) {
							throw new ResourceException("请求体须为JSON对象");
						}
						ObjectNode obj = (ObjectNode) root;
						for (String k : UPDATE_MERCHANT_PARAM_KEYS) {
							if (obj.has(k)) {
								merged.put(k, objectMapper.convertValue(obj.get(k), Object.class));
							}
						}
					} catch (ResourceException e) {
						throw e;
					} catch (JsonProcessingException e) {
						throw new ResourceException("请求体JSON格式错误");
					}
				}
			}
		}
		return merged;
	}

	private Object readAuditGoodsRaw(HttpServletRequest request) {
		Object base = null;
		String[] vals = request.getParameterValues("audit_goods");
		if (vals != null && vals.length > 0) {
			base = vals[0];
		} else {
			base = request.getParameter("audit_goods");
		}

		String ct = request.getContentType();
		if (ct == null || !ct.toLowerCase(Locale.ROOT).contains("application/json")) {
			return base;
		}
		try {
			byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
			if (buf.length == 0) {
				return base;
			}
			JsonNode root = objectMapper.readTree(buf);
			if (!root.isObject()) {
				throw new ResourceException("请求体须为JSON对象");
			}
			if (root.has("audit_goods")) {
				JsonNode ag = root.get("audit_goods");
				if (ag.isNull()) {
					base = null;
				} else if (ag.isBoolean()) {
					base = ag.booleanValue();
				} else if (ag.isNumber()) {
					if (ag.isIntegralNumber()) {
						base = ag.longValue();
					} else {
						base = ag.doubleValue();
					}
				} else if (ag.isTextual()) {
					base = ag.asText();
				} else {
					base = objectMapper.convertValue(ag, Object.class);
				}
			}
		} catch (ResourceException e) {
			throw e;
		} catch (IOException e) {
			throw new ResourceException("请求体JSON格式错误");
		}
		return base;
	}

	private Object readDisabledRaw(HttpServletRequest request) {
		Object base = null;
		String[] vals = request.getParameterValues("disabled");
		if (vals != null && vals.length > 0) {
			base = vals[0];
		} else {
			base = request.getParameter("disabled");
		}

		String ct = request.getContentType();
		if (ct == null || !ct.toLowerCase(Locale.ROOT).contains("application/json")) {
			return base;
		}
		try {
			byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
			if (buf.length == 0) {
				return base;
			}
			JsonNode root = objectMapper.readTree(buf);
			if (!root.isObject()) {
				throw new ResourceException("请求体须为JSON对象");
			}
			if (root.has("disabled")) {
				JsonNode d = root.get("disabled");
				if (d.isNull()) {
					base = null;
				} else if (d.isBoolean()) {
					base = d.booleanValue();
				} else if (d.isNumber()) {
					if (d.isIntegralNumber()) {
						base = d.longValue();
					} else {
						base = d.doubleValue();
					}
				} else if (d.isTextual()) {
					base = d.asText();
				} else {
					base = objectMapper.convertValue(d, Object.class);
				}
			}
		} catch (ResourceException e) {
			throw e;
		} catch (IOException e) {
			throw new ResourceException("请求体JSON格式错误");
		}
		return base;
	}

	private static long parseMerchantIdFromJwtUser(Object merchantIdObj) {
		if (merchantIdObj == null) {
			throw new ResourceException("商户ID不能为空");
		}
		if (merchantIdObj instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException("商户ID不能为空");
			}
			long merchantId;
			try {
				merchantId = Long.parseLong(t);
			} catch (NumberFormatException ex) {
				throw new BadRequestException("无效的商户ID");
			}
			if (merchantId <= 0) {
				throw new ResourceException("商户ID不能为空");
			}
			return merchantId;
		}
		if (merchantIdObj instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new ResourceException("商户ID不能为空");
			}
			return v;
		}
		String t = merchantIdObj.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("商户ID不能为空");
		}
		long merchantId;
		try {
			merchantId = Long.parseLong(t);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的商户ID");
		}
		if (merchantId <= 0) {
			throw new ResourceException("商户ID不能为空");
		}
		return merchantId;
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
}
