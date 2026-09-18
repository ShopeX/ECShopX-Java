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

package cn.shopex.ecshopx.crossborder.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.crossborder.service.CrossBorderShopRoutePermissionService;
import cn.shopex.ecshopx.crossborder.service.TaxstrategyAddService;
import cn.shopex.ecshopx.crossborder.service.TaxstrategyDeleteService;
import cn.shopex.ecshopx.crossborder.service.TaxstrategyInfoQueryService;
import cn.shopex.ecshopx.crossborder.service.TaxstrategyListService;
import cn.shopex.ecshopx.crossborder.service.TaxstrategyUpdateService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
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

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422
)
@RestController("crossborderTaxstrategyAdminV1")
@RequestMapping("/api/v1")
public class TaxstrategyController {

	private static final String MSG_NAME_INVALID = "策略名称不能为空,且长度不大于20个字。";
	private static final String MSG_CONTENT_REQUIRED = "策略内容不能为空";

	private final TaxstrategyAddService taxstrategyAddService;
	private final TaxstrategyListService taxstrategyListService;
	private final TaxstrategyUpdateService taxstrategyUpdateService;
	private final TaxstrategyDeleteService taxstrategyDeleteService;
	private final TaxstrategyInfoQueryService taxstrategyInfoQueryService;
	private final CompanysActivationService companysActivationService;
	private final CrossBorderShopRoutePermissionService crossBorderShopRoutePermissionService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public TaxstrategyController(
			TaxstrategyAddService taxstrategyAddService,
			TaxstrategyListService taxstrategyListService,
			TaxstrategyUpdateService taxstrategyUpdateService,
			TaxstrategyDeleteService taxstrategyDeleteService,
			TaxstrategyInfoQueryService taxstrategyInfoQueryService,
			CompanysActivationService companysActivationService,
			CrossBorderShopRoutePermissionService crossBorderShopRoutePermissionService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.taxstrategyAddService = taxstrategyAddService;
		this.taxstrategyListService = taxstrategyListService;
		this.taxstrategyUpdateService = taxstrategyUpdateService;
		this.taxstrategyDeleteService = taxstrategyDeleteService;
		this.taxstrategyInfoQueryService = taxstrategyInfoQueryService;
		this.companysActivationService = companysActivationService;
		this.crossBorderShopRoutePermissionService = crossBorderShopRoutePermissionService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "crossborder.taxstrategy.getlist")
	@GetMapping(value = "/crossborder/taxstrategy", name = "税费策略列表")
	public ResponseEntity<?> getList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@RequestParam(name = "keywords", required = false) String keywords) {
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
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_TAXSTRATEGY_GETLIST);
		int page = normalizePageParam(pageParam);
		int pageSize = normalizePageSizeParam(pageSizeParam);
		return ResponseEntity.ok(
				ApiResult.ok(taxstrategyListService.list(companyId, page, pageSize, keywords)));
	}

	@Activated(routeAlias = "crossborder.taxstrategy.getinfo")
	@GetMapping(value = "/crossborder/taxstrategy/{taxstrategy_id}", name = "税费策略详情")
	public ResponseEntity<?> getInfo(HttpServletRequest request, @PathVariable("taxstrategy_id") String taxstrategyId) {
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
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_TAXSTRATEGY_GETINFO);
		long id = parseTaxstrategyIdPath(taxstrategyId);
		Map<String, Object> data = taxstrategyInfoQueryService.getInfo(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "crossborder.taxstrategy.isadd")
	@PostMapping(value = "/crossborder/taxstrategy", name = "税费策略添加")
	public ResponseEntity<?> isAdd(
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
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_TAXSTRATEGY_ISADD);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String nameRaw = validatedTaxstrategyName(merged);
		String contentJson = validatedTaxstrategyContentJson(merged, objectMapper);

		long id = taxstrategyAddService.add(companyId, nameRaw, contentJson);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);
		payload.put("taxstrategy_id", id);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/taxstrategy");
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("taxstrategy_name", nameRaw);
		logParams.put("taxstrategy_content", contentJson);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "税费策略添加");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "crossborder.taxstrategy.isupdate")
	@PutMapping(value = "/crossborder/taxstrategy/{taxstrategy_id}", name = "税费策略修改")
	public ResponseEntity<?> isUpdate(
			HttpServletRequest request,
			@PathVariable("taxstrategy_id") String taxstrategyId,
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
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_TAXSTRATEGY_ISUPDATE);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String nameRaw = validatedTaxstrategyName(merged);
		String contentJson = validatedTaxstrategyContentJson(merged, objectMapper);
		long id = parseTaxstrategyIdPath(taxstrategyId);
		taxstrategyUpdateService.update(companyId, id, nameRaw, contentJson);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/taxstrategy/" + id);
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("taxstrategy_name", nameRaw);
		logParams.put("taxstrategy_content", contentJson);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "税费策略修改");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "crossborder.taxstrategy.isdel")
	@DeleteMapping(value = "/crossborder/taxstrategy/{taxstrategy_id}", name = "税费策略删除")
	public ResponseEntity<?> isDel(HttpServletRequest request, @PathVariable("taxstrategy_id") String taxstrategyId) {
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
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_TAXSTRATEGY_ISDEL);

		long id = parseTaxstrategyIdPath(taxstrategyId);
		taxstrategyDeleteService.softDelete(companyId, id);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/taxstrategy/" + id);
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "税费策略删除");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private static int normalizePageParam(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 1;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 1;
		}
		try {
			int v = Integer.parseInt(t);
			if (v == 0 || v < 1) {
				return 1;
			}
			return v;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int normalizePageSizeParam(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 10;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 10;
		}
		try {
			int v = Integer.parseInt(t);
			if (v == 0 || v < 1) {
				return 10;
			}
			return v;
		} catch (NumberFormatException e) {
			return 10;
		}
	}

	private static long parseTaxstrategyIdPath(String raw) {
		if (raw == null) {
			throw new ResourceException("操作失败");
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			throw new ResourceException("操作失败");
		}
		try {
			long v = Long.parseLong(trimmed);
			if (v <= 0) {
				throw new ResourceException("操作失败");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new ResourceException("操作失败");
		}
	}

	private static String validatedTaxstrategyName(Map<String, Object> merged) {
		Object v = merged.get("taxstrategy_name");
		if (v == null) {
			throw new BadRequestException(MSG_NAME_INVALID);
		}
		if (!(v instanceof String s)) {
			throw new BadRequestException(MSG_NAME_INVALID);
		}
		if (s.trim().isEmpty()) {
			throw new BadRequestException(MSG_NAME_INVALID);
		}
		if (s.codePointCount(0, s.length()) > 20) {
			throw new BadRequestException(MSG_NAME_INVALID);
		}
		return s;
	}

	private static String validatedTaxstrategyContentJson(
			Map<String, Object> merged, ObjectMapper objectMapper) {
		Object v = merged.get("taxstrategy_content");
		if (v == null) {
			throw new BadRequestException(MSG_CONTENT_REQUIRED);
		}
		JsonNode node = objectMapper.valueToTree(v);
		if (node == null || node.isNull()) {
			throw new BadRequestException(MSG_CONTENT_REQUIRED);
		}
		if (!(node.isArray() || node.isObject())) {
			throw new BadRequestException(MSG_CONTENT_REQUIRED);
		}
		try {
			return objectMapper.writeValueAsString(node);
		} catch (Exception e) {
			throw new BadRequestException(MSG_CONTENT_REQUIRED);
		}
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
}
