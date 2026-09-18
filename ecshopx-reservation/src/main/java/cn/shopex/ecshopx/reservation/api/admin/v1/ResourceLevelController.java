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

package cn.shopex.ecshopx.reservation.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.reservation.port.ReservationRoutePermissionPort;
import cn.shopex.ecshopx.reservation.service.ResourceLevelCreateService;
import cn.shopex.ecshopx.reservation.service.ResourceLevelDeleteService;
import cn.shopex.ecshopx.reservation.service.ResourceLevelGetService;
import cn.shopex.ecshopx.reservation.service.ResourceLevelListService;
import cn.shopex.ecshopx.reservation.service.ResourceLevelSetStatusService;
import cn.shopex.ecshopx.reservation.service.ResourceLevelUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("reservationResourceLevelAdminV1")
@RequestMapping("/api/v1")
public class ResourceLevelController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ReservationRoutePermissionPort reservationRoutePermissionPort;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final ResourceLevelCreateService resourceLevelCreateService;
	private final ResourceLevelUpdateService resourceLevelUpdateService;
	private final ResourceLevelSetStatusService resourceLevelSetStatusService;
	private final ResourceLevelGetService resourceLevelGetService;
	private final ResourceLevelListService resourceLevelListService;
	private final ResourceLevelDeleteService resourceLevelDeleteService;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final ObjectMapper objectMapper;

	public ResourceLevelController(
			ReservationRoutePermissionPort reservationRoutePermissionPort,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			ResourceLevelCreateService resourceLevelCreateService,
			ResourceLevelUpdateService resourceLevelUpdateService,
			ResourceLevelSetStatusService resourceLevelSetStatusService,
			ResourceLevelGetService resourceLevelGetService,
			ResourceLevelListService resourceLevelListService,
			ResourceLevelDeleteService resourceLevelDeleteService,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			ObjectMapper objectMapper) {
		this.reservationRoutePermissionPort = reservationRoutePermissionPort;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.resourceLevelCreateService = resourceLevelCreateService;
		this.resourceLevelUpdateService = resourceLevelUpdateService;
		this.resourceLevelSetStatusService = resourceLevelSetStatusService;
		this.resourceLevelGetService = resourceLevelGetService;
		this.resourceLevelListService = resourceLevelListService;
		this.resourceLevelDeleteService = resourceLevelDeleteService;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "resource.level.add")
	@PostMapping(value = "/resource/level", name = "新增资源位")
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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

		reservationRoutePermissionPort.assertResourceLevelAddAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		List<Long> materialIds = validateCreateResourceLevelInput(input);

		Map<String, Object> post = buildPostData(input);
		resourceLevelCreateService.createResourceLevel(companyId, post, materialIds);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/resource/level");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "新增资源位");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private Map<String, Object> buildPostData(Map<String, Object> input) {
		Map<String, Object> post = new LinkedHashMap<>();
		post.put("shopId", Objects.toString(input.get("shopId"), "").trim());
		post.put("shopName", Objects.toString(input.get("shopName"), ""));
		String name = input.get("name") != null ? input.get("name").toString().trim() : "";
		String description = input.get("description") != null ? input.get("description").toString().trim() : "";
		post.put("name", name);
		post.put("description", description);
		if (input.containsKey("imageUrl")) {
			Object iu = input.get("imageUrl");
			if (iu == null) {
				post.put("image_url", null);
			} else {
				String s = iu.toString().trim();
				post.put("image_url", s.isEmpty() ? null : s);
			}
		} else {
			post.put("image_url", null);
		}
		post.put("status", "active");
		return post;
	}

	/**
	 * 校验顺序：shopId → name → description → materialIds；多字段错误时用全角逗号拼接为一条消息。
	 */
	private static List<Long> validateCreateResourceLevelInput(Map<String, Object> input) {
		List<String> parts = new ArrayList<>();
		if (isBlank(input.get("shopId"))) {
			parts.add("门店必选");
		}
		String name = input.get("name") != null ? input.get("name").toString().trim() : "";
		if (name.isEmpty()) {
			parts.add("资源位名称必填");
		} else if (name.length() > 10) {
			parts.add("最多10字");
		}
		String description = input.get("description") != null ? input.get("description").toString().trim() : "";
		if (description.isEmpty()) {
			parts.add("简介必填");
		} else if (description.length() > 100) {
			parts.add("最多100字");
		}

		List<Long> materialIds = null;
		if (!input.containsKey("materialIds")) {
			parts.add("服务项目必填");
		} else {
			Object mv = input.get("materialIds");
			if (mv == null) {
				parts.add("服务项目必填");
			} else if (mv instanceof String s && !StringUtils.hasText(s.trim())) {
				parts.add("服务项目必填");
			} else if (!isArrayOrCollection(mv)) {
				parts.add("服务项目必填");
			} else {
				List<Object> elems = materialIdsToElementList(mv);
				List<Long> parsed = parseMaterialIdElements(elems);
				if (parsed == null) {
					parts.add("服务项目格式错误");
				} else {
					materialIds = parsed;
				}
			}
		}

		if (!parts.isEmpty()) {
			throw new BadRequestException(String.join("\uFF0C", parts));
		}
		return materialIds != null ? materialIds : List.of();
	}

	private static boolean isArrayOrCollection(Object v) {
		if (v instanceof Collection<?>) {
			return true;
		}
		return v instanceof Object[]
				|| v instanceof int[]
				|| v instanceof long[]
				|| v instanceof Integer[]
				|| v instanceof Long[];
	}

	private static List<Object> materialIdsToElementList(Object v) {
		if (v instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (v instanceof Object[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (v instanceof int[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (int x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof long[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (long x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof Integer[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (v instanceof Long[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		return List.of();
	}

	/** 元素无法解析为 long 时返回 null；空列表返回空 List。 */
	private static List<Long> parseMaterialIdElements(List<Object> elems) {
		List<Long> out = new ArrayList<>(elems.size());
		for (Object elem : elems) {
			if (elem == null) {
				return null;
			}
			try {
				if (elem instanceof Number n) {
					out.add(n.longValue());
				} else {
					out.add(Long.parseLong(elem.toString().trim()));
				}
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return out;
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> merged =
					new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				merged.putAll(body);
			}
			return merged;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static boolean isBlank(Object v) {
		if (v == null) {
			return true;
		}
		return !StringUtils.hasText(v.toString().trim());
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	@Activated(routeAlias = "resource.level.update")
	@PatchMapping(value = "/resource/level", name = "更新资源位")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateData(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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

		reservationRoutePermissionPort.assertResourceLevelUpdateAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		List<Long> materialIds = validateUpdateResourceLevelInput(input);
		Long resourceLevelId = parseResourceLevelIdForUpdate(input);
		String shopIdStr = Objects.toString(input.get("shopId"), "").trim();
		Map<String, Object> post = buildUpdatePostData(companyId, input);

		boolean ok = resourceLevelUpdateService.updateResourceLevel(
				companyId, resourceLevelId, shopIdStr, post, materialIds);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/resource/level");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "更新资源位");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", ok)));
	}

	/**
	 * 校验顺序：name → description → materialIds；多字段错误时用全角逗号拼接；不对 name/description 做长度上限（与 PATCH 一致）。
	 */
	private static List<Long> validateUpdateResourceLevelInput(Map<String, Object> input) {
		List<String> parts = new ArrayList<>();
		String name = input.get("name") != null ? input.get("name").toString().trim() : "";
		if (name.isEmpty()) {
			parts.add("资源位名称必填");
		}
		String description = input.get("description") != null ? input.get("description").toString().trim() : "";
		if (description.isEmpty()) {
			parts.add("简介必填");
		}

		List<Long> materialIds = null;
		if (!input.containsKey("materialIds")) {
			parts.add("服务项目必填");
		} else {
			Object mv = input.get("materialIds");
			if (mv == null) {
				parts.add("服务项目必填");
			} else if (mv instanceof String s && !StringUtils.hasText(s.trim())) {
				parts.add("服务项目必填");
			} else if (!isArrayOrCollection(mv)) {
				parts.add("服务项目必填");
			} else {
				List<Object> elems = materialIdsToElementList(mv);
				List<Long> parsed = parseMaterialIdElements(elems);
				if (parsed == null) {
					parts.add("服务项目格式错误");
				} else {
					materialIds = parsed;
				}
			}
		}

		if (!parts.isEmpty()) {
			throw new BadRequestException(String.join("\uFF0C", parts));
		}
		return materialIds != null ? materialIds : List.of();
	}

	/**
	 * 缺失或空白返回 null（查无行 → status false）；非空白但非法 long 抛 BadRequestException。
	 */
	private static Long parseResourceLevelIdForUpdate(Map<String, Object> input) {
		if (!input.containsKey("resourceLevelId")) {
			return null;
		}
		Object v = input.get("resourceLevelId");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("resourceLevelId 格式错误");
		}
	}

	private Map<String, Object> buildUpdatePostData(long companyId, Map<String, Object> input) {
		Map<String, Object> post = new LinkedHashMap<>();
		post.put("companyId", String.valueOf(companyId));
		post.put("shopId", Objects.toString(input.get("shopId"), "").trim());
		post.put("shopName", Objects.toString(input.get("shopName"), ""));
		String name = input.get("name") != null ? input.get("name").toString().trim() : "";
		String description = input.get("description") != null ? input.get("description").toString().trim() : "";
		post.put("name", name);
		post.put("description", description);
		if (input.containsKey("imageUrl")) {
			Object iu = input.get("imageUrl");
			if (iu == null) {
				post.put("image_url", null);
			} else {
				String s = iu.toString().trim();
				post.put("image_url", s.isEmpty() ? null : s);
			}
		} else {
			post.put("image_url", null);
		}
		post.put("status", "active");
		return post;
	}

	@Activated(routeAlias = "resource.level.delete")
	@DeleteMapping(value = "/resource/level", name = "删除资源位")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteData(HttpServletRequest request) {
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

		reservationRoutePermissionPort.assertResourceLevelDeleteAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, String> queryMap = toQueryStringMap(request);
		String resourceLevelId = queryMap.get("resource_level_id");
		String shopId = queryMap.get("shop_id");
		boolean result = resourceLevelDeleteService.deleteResourceLevel(companyId, resourceLevelId, shopId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/resource/level");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(queryMap));
		} catch (Exception e) {
			logCtx.put("params", queryMap.toString());
		}
		logCtx.put("operator_name", "删除资源位");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", result)));
	}

	@Activated(routeAlias = "resource.level.get")
	@GetMapping(value = "/resource/level/{id}", name = "获取资源位详细信息")
	public ResponseEntity<ApiResult<Object>> getData(HttpServletRequest request, @PathVariable("id") String id) {
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

		reservationRoutePermissionPort.assertResourceLevelGetAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Object result = resourceLevelGetService.getResourceLevel(companyId, id);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/resource/level/" + id);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", id)));
		} catch (Exception e) {
			logCtx.put("params", Map.of("id", id).toString());
		}
		logCtx.put("operator_name", "获取资源位详细信息");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "resource.level.list")
	@GetMapping(value = "/resource/levellist", name = "获取资源位列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getListData(
			HttpServletRequest request, @RequestParam(value = "shopId", required = false) String shopId) {
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

		reservationRoutePermissionPort.assertResourceLevelListAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, String> queryMap = toQueryStringMap(request);
		validateResourceLevelListQuery(queryMap);

		String shopFilter = shopId == null ? null : shopId.trim();
		if (shopFilter != null && shopFilter.isEmpty()) {
			shopFilter = null;
		}
		Map<String, Object> data = resourceLevelListService.listResourceLevels(companyId, shopFilter);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/resource/levellist");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(queryMap));
		} catch (Exception e) {
			logCtx.put("params", queryMap.toString());
		}
		logCtx.put("operator_name", "获取资源位列表");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, String> toQueryStringMap(HttpServletRequest request) {
		Map<String, String> m = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			m.put(name, request.getParameter(name));
		}
		return m;
	}

	/**
	 * 与校验规则一致：page、pageSize 必填；page≥1；pageSize 1～50。多字段错误按字段块用全角逗号连接，整体末尾保留全角逗号。
	 */
	private static void validateResourceLevelListQuery(Map<String, String> queryMap) {
		List<String> fieldBlocks = new ArrayList<>();
		List<String> pageErrs = new ArrayList<>();
		String pageRaw = queryMap.get("page");
		if (!StringUtils.hasText(pageRaw == null ? "" : pageRaw.trim())) {
			pageErrs.add("page 字段是必须的");
		} else {
			try {
				int p = Integer.parseInt(pageRaw.trim());
				if (p < 1) {
					pageErrs.add("page 必须至少为 1");
				}
			} catch (NumberFormatException e) {
				pageErrs.add("page 必须是一个整数");
			}
		}
		if (!pageErrs.isEmpty()) {
			fieldBlocks.add(String.join("\uFF0C", pageErrs));
		}

		List<String> pageSizeErrs = new ArrayList<>();
		String psRaw = queryMap.get("pageSize");
		if (!StringUtils.hasText(psRaw == null ? "" : psRaw.trim())) {
			pageSizeErrs.add("pageSize 字段是必须的");
		} else {
			try {
				int ps = Integer.parseInt(psRaw.trim());
				if (ps < 1) {
					pageSizeErrs.add("pageSize 必须至少为 1");
				} else if (ps > 50) {
					pageSizeErrs.add("pageSize 不能大于 50");
				}
			} catch (NumberFormatException e) {
				pageSizeErrs.add("pageSize 必须是一个整数");
			}
		}
		if (!pageSizeErrs.isEmpty()) {
			fieldBlocks.add(String.join("\uFF0C", pageSizeErrs));
		}

		if (!fieldBlocks.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String block : fieldBlocks) {
				sb.append(block).append("\uFF0C");
			}
			throw new ResourceException(sb.toString());
		}
	}

	@Activated(routeAlias = "resource.level.set.status")
	@PutMapping(value = "/resource/setlevelstatus", name = "修改资源状态")
	public ResponseEntity<ApiResult<Object>> updateResourceLevelStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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

		reservationRoutePermissionPort.assertResourceLevelSetStatusAllowed(user);
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, Object> input = mergeInputLikeFlexibleResolver(request, body);
		String newStatus = resolveSetLevelStatus(input);
		Long resourceLevelId = parseResourceLevelIdForSetStatus(input);
		Object result = resourceLevelSetStatusService.updateResourceLevelStatus(companyId, resourceLevelId, newStatus);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/resource/setlevelstatus");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(input));
		} catch (Exception e) {
			logCtx.put("params", input.toString());
		}
		logCtx.put("operator_name", "修改资源状态");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static String resolveSetLevelStatus(Map<String, Object> input) {
		Object raw = input.get("status");
		if (raw == null) {
			return "invalid";
		}
		String s = String.valueOf(raw);
		return "true".equals(s) ? "active" : "invalid";
	}

	/** 非法 long 返回 null（查无行 → 空数组），不抛校验异常。 */
	private static Long parseResourceLevelIdForSetStatus(Map<String, Object> input) {
		if (!input.containsKey("resourceLevelId")) {
			return null;
		}
		Object v = input.get("resourceLevelId");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
