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

package cn.shopex.ecshopx.shopmenuborder.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuCreateService;
import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuDeleteService;
import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuExportService;
import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuQueryService;
import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuUpdateService;
import cn.shopex.ecshopx.shopmenuborder.service.ShopMenuUploadService;
import cn.shopex.ecshopx.shopmenuborder.service.dto.BorderShopMenuCreateInput;
import cn.shopex.ecshopx.shopmenuborder.service.dto.BorderShopMenuUpdateInput;
import cn.shopex.ecshopx.shopmenuborder.service.dto.ShopMenuCreateAuditContext;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("shopmenuBorderAdminV1")
@RequestMapping("/api/v1")
public class ShopMenuController {

	private final ShopMenuUploadService shopMenuUploadService;
	private final ShopMenuCreateService shopMenuCreateService;
	private final ShopMenuDeleteService shopMenuDeleteService;
	private final ShopMenuUpdateService shopMenuUpdateService;
	private final ShopMenuQueryService shopMenuQueryService;
	private final ShopMenuExportService shopMenuExportService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public ShopMenuController(
			ShopMenuUploadService shopMenuUploadService,
			ShopMenuCreateService shopMenuCreateService,
			ShopMenuDeleteService shopMenuDeleteService,
			ShopMenuUpdateService shopMenuUpdateService,
			ShopMenuQueryService shopMenuQueryService,
			ShopMenuExportService shopMenuExportService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.shopMenuUploadService = shopMenuUploadService;
		this.shopMenuCreateService = shopMenuCreateService;
		this.shopMenuDeleteService = shopMenuDeleteService;
		this.shopMenuUpdateService = shopMenuUpdateService;
		this.shopMenuQueryService = shopMenuQueryService;
		this.shopMenuExportService = shopMenuExportService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "super.admin.shopmenu.get")
	@GetMapping("/shopmenu")
	public ResponseEntity<ApiResult<Map<String, Object>>> getShopMenu(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		// Default version=1 only applies when the key is absent; explicit empty or non-int yields no rows.
		String versionRaw = request.getParameter("version");
		int version;
		if (versionRaw == null) {
			version = 1;
		} else {
			String trimmed = versionRaw.trim();
			if (!StringUtils.hasText(trimmed)) {
				return ResponseEntity.ok(ApiResult.ok(Map.of("tree", List.of(), "list", List.of())));
			}
			try {
				version = Integer.parseInt(trimmed);
			} catch (NumberFormatException e) {
				return ResponseEntity.ok(ApiResult.ok(Map.of("tree", List.of(), "list", List.of())));
			}
		}
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> result = shopMenuQueryService.getBorderShopMenu(version, requestLang);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "super.admin.shopmenu.add")
	@PostMapping("/shopmenu")
	public ResponseEntity<?> addShopMenu(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeAddShopMenuInput(request, body);

		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		Object verObj = merged.get("version");
		int versionDefault = 1;
		if (verObj instanceof Number n) {
			versionDefault = n.intValue();
		} else if (verObj != null && StringUtils.hasText(verObj.toString())) {
			try {
				versionDefault = Integer.parseInt(verObj.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		merged.put("version", versionDefault);
		merged.put("company_id", 0);

		try {
			List<String> menuTypeNames = parseMenuTypeNames(merged.get("menu_type"));

			Long shopmenuId = parseOptionalLongKey(merged, "shopmenu_id");
			Long pid = parseLongWithDefault(merged, "pid", 0L);
			Integer sort = parseIntegerWithDefault(merged, "sort", 1);

			String aliasName = stringVal(merged.get("alias_name"));

			BorderShopMenuCreateInput input = new BorderShopMenuCreateInput(
					shopmenuId,
					0,
					pid,
					sort,
					versionDefault,
					aliasName,
					menuTypeNames,
					merged.containsKey("name"),
					stringVal(merged.get("name")),
					merged.containsKey("url"),
					stringVal(merged.get("url")),
					merged.containsKey("apis"),
					merged.get("apis") != null ? merged.get("apis").toString() : null,
					merged.containsKey("icon"),
					stringVal(merged.get("icon")),
					merged.containsKey("is_show"),
					merged.get("is_show"),
					merged.containsKey("is_menu"),
					merged.get("is_menu"),
					merged.containsKey("disabled"),
					merged.get("disabled"),
					RequestCountryCode.resolve(langueProperties, merged));

			String userId = ud.get("operator_id") != null ? String.valueOf(ud.get("operator_id")) : "";
			Map<String, Object> requestSnapshot = new LinkedHashMap<>(merged);
			ShopMenuCreateAuditContext ctx =
					new ShopMenuCreateAuditContext(clientIp(request), userId, requestSnapshot);

			ShopMenu created = shopMenuCreateService.createFromBorderRequest(input, ctx);
			Map<String, Object> menuMap = shopMenuToSnakeMap(created);

			appendOperatorLog(request, ud, merged, "/api/v1/shopmenu", "新增菜单");

			return ResponseEntity.ok(ApiResult.ok(menuMap));
		} catch (ResourceException e) {
			return dingoStyleFromResourceException(e);
		} catch (BadRequestException e) {
			return dingoStyleFromBadRequestAddShopMenu(e);
		}
	}

	private static Map<String, Object> shopMenuToSnakeMap(ShopMenu m) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("shopmenu_id", m.getShopmenuId());
		map.put("company_id", m.getCompanyId());
		map.put("name", m.getName());
		map.put("url", m.getUrl());
		map.put("sort", m.getSort());
		map.put("is_menu", m.getIsMenu());
		map.put("pid", m.getPid());
		map.put("apis", m.getApis());
		map.put("icon", m.getIcon());
		map.put("is_show", m.getIsShow());
		map.put("alias_name", m.getAliasName());
		map.put("version", m.getVersion());
		map.put("disabled", m.getDisabled());
		map.put("created", m.getCreated());
		map.put("updated", m.getUpdated());
		return map;
	}

	private void appendOperatorLog(
			HttpServletRequest request,
			Map<?, ?> ud,
			Map<String, Object> merged,
			String requestUri,
			String operatorName) {
		long companyId = toLong(ud.get("company_id"));
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", requestUri);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", request.getQueryString() != null ? request.getQueryString() : "");
		}
		logCtx.put("operator_name", operatorName);
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			try {
				logCtx.put(
						"merchant_id",
						merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
			} catch (Exception ignored) {
			}
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}
	}

	private Map<String, Object> mergeAddShopMenuInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToSingleValues(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static List<String> parseMenuTypeNames(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("menu_type 须为 JSON 字符串数组");
		}
		List<String> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof String s)) {
				throw new BadRequestException("menu_type 须为 JSON 字符串数组");
			}
			out.add(s);
		}
		return out;
	}

	private static Long parseOptionalLongKey(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return null;
		}
		Object v = merged.get(key);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 格式错误");
		}
	}

	private static long parseRequiredShopmenuId(Map<String, Object> merged) {
		if (!merged.containsKey("shopmenu_id")) {
			throw new BadRequestException("缺少必填字段: shopmenu_id");
		}
		Object v = merged.get("shopmenu_id");
		if (v == null) {
			throw new BadRequestException("缺少必填字段: shopmenu_id");
		}
		long id;
		if (v instanceof Number n) {
			id = n.longValue();
		} else {
			String s = v.toString().trim();
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("缺少必填字段: shopmenu_id");
			}
			try {
				id = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new BadRequestException("缺少必填字段: shopmenu_id");
			}
		}
		if (id <= 0L) {
			throw new BadRequestException("缺少必填字段: shopmenu_id");
		}
		return id;
	}

	private static Integer parseOptionalIntegerKey(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return null;
		}
		Object v = merged.get(key);
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 格式错误");
		}
	}

	private static long parseLongWithDefault(Map<String, Object> merged, String key, long def) {
		if (!merged.containsKey(key) || merged.get(key) == null) {
			return def;
		}
		Object v = merged.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Integer parseIntegerWithDefault(Map<String, Object> merged, String key, int def) {
		if (!merged.containsKey(key) || merged.get(key) == null) {
			return def;
		}
		Object v = merged.get(key);
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object o) {
		if (o == null) {
			return null;
		}
		return o.toString();
	}

	@Activated(routeAlias = "super.admin.shopmenu.update")
	@PutMapping("/shopmenu")
	public ResponseEntity<?> updateShopMenu(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeAddShopMenuInput(request, body);

		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		merged.put("company_id", 0);

		try {
			long shopmenuId = parseRequiredShopmenuId(merged);

			boolean aliasTruthy =
					merged.containsKey("alias_name")
							&& StringUtils.hasText(stringVal(merged.get("alias_name")));
			if (aliasTruthy) {
				Object ver = merged.get("version");
				if (!merged.containsKey("version")
						|| ver == null
						|| (ver instanceof String sv && !StringUtils.hasText(sv.trim()))) {
					throw new BadRequestException("缺少必填字段: version");
				}
			}
			List<String> menuTypeNames =
					aliasTruthy ? parseMenuTypeNames(merged.get("menu_type")) : List.of();

			BorderShopMenuUpdateInput input = new BorderShopMenuUpdateInput(
					shopmenuId,
					merged.containsKey("name"),
					stringVal(merged.get("name")),
					merged.containsKey("url"),
					stringVal(merged.get("url")),
					merged.containsKey("sort"),
					parseOptionalIntegerKey(merged, "sort"),
					merged.containsKey("pid"),
					parseOptionalLongKey(merged, "pid"),
					merged.containsKey("alias_name"),
					stringVal(merged.get("alias_name")),
					merged.containsKey("version"),
					parseOptionalIntegerKey(merged, "version"),
					menuTypeNames,
					merged.containsKey("apis"),
					merged.get("apis") != null ? merged.get("apis").toString() : null,
					merged.containsKey("icon"),
					stringVal(merged.get("icon")),
					merged.containsKey("is_show"),
					merged.get("is_show"),
					merged.containsKey("is_menu"),
					merged.get("is_menu"),
					merged.containsKey("disabled"),
					merged.get("disabled"),
					RequestCountryCode.resolve(langueProperties, merged));

			String userId = ud.get("operator_id") != null ? String.valueOf(ud.get("operator_id")) : "";
			Map<String, Object> requestSnapshot = new LinkedHashMap<>(merged);
			ShopMenuCreateAuditContext ctx =
					new ShopMenuCreateAuditContext(clientIp(request), userId, requestSnapshot);

			ShopMenu updated = shopMenuUpdateService.updateFromBorderRequest(input, ctx);
			Map<String, Object> menuMap = shopMenuToSnakeMap(updated);

			appendOperatorLog(request, ud, merged, "/api/v1/shopmenu", "更新菜单");

			return ResponseEntity.ok(ApiResult.ok(menuMap));
		} catch (ResourceException e) {
			return dingoStyleFromResourceException(e);
		} catch (BadRequestException e) {
			return dingoStyleFromBadRequestAddShopMenu(e);
		}
	}

	@Activated(routeAlias = "super.admin.shopmenu.del")
	@DeleteMapping("/shopmenu/{shopmenuId}")
	public ResponseEntity<?> deleteShopMenu(
			HttpServletRequest request, @PathVariable("shopmenuId") String shopmenuIdRaw) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		try {
			if (shopmenuIdRaw == null) {
				throw new ResourceException("参数错误");
			}
			String trimmed = shopmenuIdRaw.trim();
			if (!StringUtils.hasText(trimmed) || "0".equals(trimmed)) {
				throw new ResourceException("参数错误");
			}

			Map<String, Object> data = shopMenuDeleteService.deleteMenus(trimmed);

			Map<String, Object> logMerged = Map.of("shopmenu_id", trimmed);
			appendOperatorLog(request, ud, logMerged, "/api/v1/shopmenu", "删除菜单");

			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException e) {
			return dingoStyleFromResourceException(e);
		} catch (BadRequestException e) {
			return dingoStyleFromBadRequestAddShopMenu(e);
		}
	}

	@Activated(routeAlias = "super.admin.shopmenu.down")
	@GetMapping("/shopmenu/down")
	public ResponseEntity<Map<String, Object>> downShopMenu(HttpServletRequest request)
			throws JsonProcessingException {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, String> inner = shopMenuExportService.exportDownShopMenu(request);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", inner);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
	}

	@Activated(routeAlias = "super.admin.shopmenu.upload")
	@PostMapping(value = "/shopmenu/upload", name = "上传菜单")
	public ResponseEntity<?> uploadMenu(
			HttpServletRequest request, @RequestParam(value = "file", required = false) MultipartFile file) {
		if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
			throw new BadRequestException("请上传菜单文件");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(file.getBytes());
		} catch (JsonProcessingException e) {
			return dingoStyleDataError(
					422, "菜单文件不是合法 JSON，无法解析为数组");
		} catch (Exception e) {
			return dingoStyleDataError(
					422, "菜单文件不是合法 JSON，无法解析为数组");
		}
		if (root == null || !root.isArray()) {
			return dingoStyleDataError(
					422, "菜单文件 JSON 根节点须为 JSON 数组");
		}

		List<JsonNode> rows = new ArrayList<>();
		for (JsonNode node : root) {
			rows.add(node);
		}

		shopMenuUploadService.uploadMenus(rows);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = 0L;
		long operatorId = 0L;
		if (raw instanceof Map<?, ?> ud) {
			companyId = toLong(ud.get("company_id"));
			operatorId = toLong(ud.get("operator_id"));
		}

		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/shopmenu/upload");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(parameterMapToSingleValues(request)));
		} catch (Exception e) {
			logCtx.put("params", request.getQueryString() != null ? request.getQueryString() : "");
		}
		logCtx.put("operator_name", "上传菜单");
		logCtx.put("log_type", "operator");
		if (raw instanceof Map<?, ?> ud) {
			Object merchantId = ud.get("merchant_id");
			if (merchantId != null) {
				logCtx.put(
						"merchant_id",
						merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
			}
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	/**
	 * 返回 HTTP 200，响应体为 {@code { "data": { "message", "status_code" } }}；{@code status_code} 为业务语义码（非 HTTP 状态码）。
	 * 与 {@link #uploadMenu} 错误体结构一致，供 upload 入参错误与 addShopMenu 业务/校验错误使用。
	 */
	private static ResponseEntity<Map<String, Object>> dingoStyleDataError(int statusCode, String message) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	private static ResponseEntity<Map<String, Object>> dingoStyleFromResourceException(ResourceException e) {
		int code = e.getEmbeddedStatusCode() != null ? e.getEmbeddedStatusCode() : 422;
		return dingoStyleDataError(code, e.getMessage());
	}

	/**
	 * addShopMenu 的 {@link BadRequestException} 映射：{@code data.status_code} 取自异常的嵌入状态码，未设置时默认为 422。
	 * 非法 {@code menu_type} 类型在 {@link #parseMenuTypeNames} 处以嵌入码 500 抛出。
	 */
	private static ResponseEntity<Map<String, Object>> dingoStyleFromBadRequestAddShopMenu(BadRequestException e) {
		int code = e.getEmbeddedStatusCode() != null ? e.getEmbeddedStatusCode() : 422;
		return dingoStyleDataError(code, e.getMessage());
	}

	private static Map<String, Object> parameterMapToSingleValues(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
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
