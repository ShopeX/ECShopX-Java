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
import cn.shopex.ecshopx.crossborder.service.OriginCountryAddService;
import cn.shopex.ecshopx.crossborder.service.OriginCountryDeleteService;
import cn.shopex.ecshopx.crossborder.service.OriginCountryListService;
import cn.shopex.ecshopx.crossborder.service.OriginCountryUpdateService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
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
@RestController("crossborderOriginCountryAdminV1")
@RequestMapping("/api/v1")
public class OriginCountryController {

	private static final String MSG_NAME_INVALID = "国家名称不能为空,且长度不大于20个字。";
	private static final String MSG_IMG_REQUIRED = "国旗图片不能为空";

	private final OriginCountryAddService originCountryAddService;
	private final OriginCountryListService originCountryListService;
	private final OriginCountryUpdateService originCountryUpdateService;
	private final OriginCountryDeleteService originCountryDeleteService;
	private final CompanysActivationService companysActivationService;
	private final CrossBorderShopRoutePermissionService crossBorderShopRoutePermissionService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public OriginCountryController(
			OriginCountryAddService originCountryAddService,
			OriginCountryListService originCountryListService,
			OriginCountryUpdateService originCountryUpdateService,
			OriginCountryDeleteService originCountryDeleteService,
			CompanysActivationService companysActivationService,
			CrossBorderShopRoutePermissionService crossBorderShopRoutePermissionService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.originCountryAddService = originCountryAddService;
		this.originCountryListService = originCountryListService;
		this.originCountryUpdateService = originCountryUpdateService;
		this.originCountryDeleteService = originCountryDeleteService;
		this.companysActivationService = companysActivationService;
		this.crossBorderShopRoutePermissionService = crossBorderShopRoutePermissionService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "crossborder.origincountry.getlist")
	@GetMapping(value = "/crossborder/origincountry", name = "获取产地国列表")
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
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_ORIGINCOUNTRY_GETLIST);
		int page = normalizePageParam(pageParam);
		int pageSize = normalizePageSizeParam(pageSizeParam);
		return ResponseEntity.ok(
				ApiResult.ok(originCountryListService.list(companyId, page, pageSize, keywords)));
	}

	@Activated(routeAlias = "crossborder.origincountry.isadd")
	@PostMapping(value = "/crossborder/origincountry", name = "产地国添加&修改")
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
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_ORIGINCOUNTRY_ISADD);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String nameRaw = validatedOriginCountryName(merged);
		String imgUrlRaw = validatedOriginCountryImgUrl(merged);

		long id = originCountryAddService.add(companyId, nameRaw, imgUrlRaw);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);
		payload.put("origincountry_id", id);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/origincountry");
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("origincountry_name", nameRaw);
		logParams.put("origincountry_img_url", imgUrlRaw);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "产地国添加&修改");
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

	@Activated(routeAlias = "crossborder.origincountry.isupdate")
	@PutMapping(value = "/crossborder/origincountry/{origincountry_id}", name = "产地国修改")
	public ResponseEntity<?> isUpdate(
			HttpServletRequest request,
			@PathVariable("origincountry_id") String origincountryId,
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
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_ORIGINCOUNTRY_ISUPDATE);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String nameRaw = validatedOriginCountryName(merged);
		String imgUrlRaw = validatedOriginCountryImgUrl(merged);
		long id = parseOrigincountryIdPath(origincountryId);
		originCountryUpdateService.update(companyId, id, nameRaw, imgUrlRaw);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/origincountry/" + id);
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("origincountry_name", nameRaw);
		logParams.put("origincountry_img_url", imgUrlRaw);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "产地国修改");
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

	@Activated(routeAlias = "crossborder.origincountry.isdel")
	@DeleteMapping(value = "/crossborder/origincountry/{origincountry_id}", name = "产地国删除")
	public ResponseEntity<?> isDel(HttpServletRequest request, @PathVariable("origincountry_id") String origincountryId) {
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
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_ORIGINCOUNTRY_ISDEL);

		long id = parseOrigincountryIdPath(origincountryId);
		originCountryDeleteService.softDelete(companyId, id);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/origincountry/" + id);
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "产地国删除");
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

	private static String validatedOriginCountryName(Map<String, Object> merged) {
		Object v = merged.get("origincountry_name");
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

	private static String validatedOriginCountryImgUrl(Map<String, Object> merged) {
		Object v = merged.get("origincountry_img_url");
		if (v == null) {
			throw new BadRequestException(MSG_IMG_REQUIRED);
		}
		if (!(v instanceof String s)) {
			throw new BadRequestException(MSG_IMG_REQUIRED);
		}
		if (s.trim().isEmpty()) {
			throw new BadRequestException(MSG_IMG_REQUIRED);
		}
		return s;
	}

	private static long parseOrigincountryIdPath(String raw) {
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
