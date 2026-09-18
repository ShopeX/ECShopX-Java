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

package cn.shopex.ecshopx.aliyunsms.api.admin.v1;

import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsShopRoutePermissionService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSignDeleteService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSignInfoService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSignListService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSignListService.SignListQuery;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSignWriteService;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsSyncSmsSignsJobDispatchPublisher;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminFlexibleInputMerge;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminSignInfoQueryId;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		notFound = true
)
@RestController("aliyunsmsSignAdminV1")
@RequestMapping("/api/v1/aliyunsms")
public class SignController {

	private final CompanysActivationService companysActivationService;
	private final AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService;
	private final AliyunsmsSignWriteService aliyunsmsSignWriteService;
	private final AliyunsmsSignInfoService aliyunsmsSignInfoService;
	private final AliyunsmsSignListService aliyunsmsSignListService;
	private final AliyunsmsSignDeleteService aliyunsmsSignDeleteService;
	private final AliyunsmsSyncSmsSignsJobDispatchPublisher aliyunsmsSyncSmsSignsJobDispatchPublisher;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public SignController(
			CompanysActivationService companysActivationService,
			AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService,
			AliyunsmsSignWriteService aliyunsmsSignWriteService,
			AliyunsmsSignInfoService aliyunsmsSignInfoService,
			AliyunsmsSignListService aliyunsmsSignListService,
			AliyunsmsSignDeleteService aliyunsmsSignDeleteService,
			AliyunsmsSyncSmsSignsJobDispatchPublisher aliyunsmsSyncSmsSignsJobDispatchPublisher,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.aliyunsmsShopRoutePermissionService = aliyunsmsShopRoutePermissionService;
		this.aliyunsmsSignWriteService = aliyunsmsSignWriteService;
		this.aliyunsmsSignInfoService = aliyunsmsSignInfoService;
		this.aliyunsmsSignListService = aliyunsmsSignListService;
		this.aliyunsmsSignDeleteService = aliyunsmsSignDeleteService;
		this.aliyunsmsSyncSmsSignsJobDispatchPublisher = aliyunsmsSyncSmsSignsJobDispatchPublisher;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "aliyunsms.sign.getList")
	@GetMapping(value = "/sign/list", name = "aliyunsms.sign.getList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer pageIgnored,
			@RequestParam(value = "pageSize", required = false) Integer pageSizeIgnored,
			@RequestParam(value = "page_size", required = false) Integer pageSizeUnderscoreIgnored,
			@RequestParam(value = "sign_name", required = false) String signNameIgnored,
			@RequestParam(value = "status", required = false) String statusIgnored) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
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

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSignGetList(user);

		SignListQuery q = new SignListQuery();
		q.setPage(parsePage(request.getParameter("page")));
		q.setPageSize(resolvePageSize(request));
		applySignNameToQuery(request, q);
		var pm = request.getParameterMap();
		q.setStatusKeyPresent(pm.containsKey("status"));
		q.setStatusValue(request.getParameter("status"));

		Map<String, Object> data = aliyunsmsSignListService.getList(companyId, q, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.sign.getInfo")
	@GetMapping(value = "/sign/info", name = "aliyunsms.sign.getInfo")
	public ResponseEntity<ApiResult<Object>> getInfo(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
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

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSignGetInfo(user);

		Optional<Long> idOpt = AliyunsmsAdminSignInfoQueryId.resolveSignInfoPrimaryKey(request);
		Long id = idOpt.orElse(null);
		Map<String, Object> data = aliyunsmsSignInfoService.getInfo(id, request);
		if (data.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.sign.add")
	@PostMapping(value = "/sign/add", name = "新增签名")
	public ResponseEntity<ApiResult<Map<String, Object>>> addSign(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
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

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSignAdd(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);
		validateAddSignMerged(merged);

		String signName = stringValue(merged.get("sign_name"));
		int signNameCp = signName.codePointCount(0, signName.length());
		if (signNameCp < 2 || signNameCp > 12) {
			throw new ResourceException("签名有效长度2-12个字符");
		}
		String remark = stringValue(merged.get("remark"));
		if (remark.codePointCount(0, remark.length()) > 200) {
			throw new ResourceException("申请说明长度不超过200个字符");
		}
		int signSource = parseSignSource(merged.get("sign_source"));
		boolean thirdParty = parseThirdPartyStrict(merged.get("third_party"));
		String qualificationId = stringValue(merged.get("qualification_id"));
		String signFile = optionalTrimmedPath(merged, "sign_file");
		String delegateFile = optionalTrimmedPath(merged, "delegate_file");

		aliyunsmsSignWriteService.addSign(
				companyId, signName, signSource, remark, thirdParty, qualificationId, signFile, delegateFile);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/sign/add");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "新增签名");
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

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "aliyunsms.sign.modify")
	@PostMapping(value = "/sign/modify", name = "修改签名")
	public ResponseEntity<ApiResult<Map<String, Object>>> modifySign(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
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

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSignModify(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);
		validateModifySignMerged(merged);

		int signSource = parseSignSource(merged.get("sign_source"));
		String remark = stringValue(merged.get("remark"));
		if (remark.codePointCount(0, remark.length()) > 200) {
			throw new ResourceException("申请说明长度不超过200个字符");
		}
		boolean thirdParty = parseThirdPartyStrict(merged.get("third_party"));
		String qualificationId = stringValue(merged.get("qualification_id"));
		String signFile = optionalTrimmedPath(merged, "sign_file");
		String delegateFile = optionalTrimmedPath(merged, "delegate_file");

		aliyunsmsSignWriteService.modifySign(
				companyId,
				merged.get("id"),
				merged.containsKey("sign_name"),
				merged.get("sign_name"),
				signSource,
				remark,
				thirdParty,
				qualificationId,
				signFile,
				delegateFile);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/sign/modify");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "修改签名");
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

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "aliyunsms.sign.delete")
	@DeleteMapping(value = "/sign/delete/{id}", name = "删除签名")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteSign(
			HttpServletRequest request, @PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
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

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSignDelete(user);

		long signId = parseRequiredSignDeleteId(id);
		aliyunsmsSignDeleteService.deleteSign(companyId, signId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/sign/delete/" + signId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", signId)));
		} catch (Exception e) {
			logCtx.put("params", Map.of("id", signId).toString());
		}
		logCtx.put("operator_name", "删除签名");
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

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "aliyunsms.sign.sync")
	@PostMapping(value = "/sign/sync", name = "同步签名")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncSign(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyObj = ud.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsSyncSmsSignsJobDispatchPublisher.publish(companyId);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE, "message", "同步任务已提交")));
	}

	private static long parseRequiredSignDeleteId(String id) {
		if (id == null) {
			throw new ResourceException("id必填");
		}
		String t = id.trim();
		if (t.isEmpty()) {
			throw new ResourceException("id必填");
		}
		if ("0".equals(t)) {
			throw new ResourceException("id必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Long.MIN_VALUE;
		}
	}

	private static void validateAddSignMerged(Map<String, Object> merged) {
		requireNonEmptyTrimmed(merged, "sign_name", "签名名称必填");
		requireSignSourceKeyPresent(merged);
		requireNonEmptyTrimmed(merged, "remark", "申请说明必填");
		requireThirdPartyStringIn(merged);
		requireNonEmptyTrimmed(merged, "qualification_id", "资质ID必填");
	}

	private static void validateModifySignMerged(Map<String, Object> merged) {
		requireSignSourceKeyPresent(merged);
		requireNonEmptyTrimmed(merged, "remark", "申请说明必填");
		requireThirdPartyStringIn(merged);
		requireNonEmptyTrimmed(merged, "qualification_id", "资质ID必填");
	}

	private static void requireNonEmptyTrimmed(Map<String, Object> merged, String key, String message) {
		if (!merged.containsKey(key)) {
			throw new ResourceException(message);
		}
		Object v = merged.get(key);
		if (v == null) {
			throw new ResourceException(message);
		}
		if (stringValue(v).isEmpty()) {
			throw new ResourceException(message);
		}
	}

	private static void requireSignSourceKeyPresent(Map<String, Object> merged) {
		if (!merged.containsKey("sign_source") || merged.get("sign_source") == null) {
			throw new ResourceException("签名来源有误");
		}
	}

	private static void requireThirdPartyStringIn(Map<String, Object> merged) {
		if (!merged.containsKey("third_party") || merged.get("third_party") == null) {
			throw new ResourceException("签名用途必填");
		}
		Object v = merged.get("third_party");
		if (!(v instanceof String s)) {
			throw new ResourceException("签名用途必填");
		}
		if (!"true".equals(s) && !"false".equals(s)) {
			throw new ResourceException("签名用途必填");
		}
	}

	private static boolean parseThirdPartyStrict(Object v) {
		String s = (String) v;
		return "true".equalsIgnoreCase(s);
	}

	private static int parseSignSource(Object v) {
		if (v instanceof Boolean) {
			throw new ResourceException("签名来源有误");
		}
		if (v instanceof Integer i) {
			return checkSignSourceRange(i);
		}
		if (v instanceof Long l) {
			if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
				throw new ResourceException("签名来源有误");
			}
			return checkSignSourceRange(l.intValue());
		}
		if (v instanceof Byte b) {
			return checkSignSourceRange(b.intValue());
		}
		if (v instanceof Short s) {
			return checkSignSourceRange(s.intValue());
		}
		if (v instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d)) {
				throw new ResourceException("签名来源有误");
			}
			if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
				throw new ResourceException("签名来源有误");
			}
			return checkSignSourceRange((int) d);
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException("签名来源有误");
			}
			if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
				throw new ResourceException("签名来源有误");
			}
			int parsed;
			try {
				parsed = Integer.parseInt(t);
			} catch (NumberFormatException e) {
				throw new ResourceException("签名来源有误");
			}
			return checkSignSourceRange(parsed);
		}
		throw new ResourceException("签名来源有误");
	}

	private static int checkSignSourceRange(int n) {
		if (n < 0 || n > 5) {
			throw new ResourceException("签名来源有误");
		}
		return n;
	}

	private static String optionalTrimmedPath(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key)) {
			return null;
		}
		Object v = merged.get(key);
		if (v == null) {
			return null;
		}
		String s = stringValue(v);
		return s.isEmpty() ? null : s;
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

	private static String stringValue(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s.trim();
		}
		return String.valueOf(o).trim();
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private static void applySignNameToQuery(HttpServletRequest request, SignListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("sign_name")) {
			q.setSignNameFilterActive(false);
			return;
		}
		String raw = request.getParameter("sign_name");
		if (raw == null || raw.isEmpty() || "0".equals(raw)) {
			q.setSignNameFilterActive(false);
			return;
		}
		q.setSignNameFilterActive(true);
		q.setSignNameContains(raw);
	}

	private static int resolvePageSize(HttpServletRequest request) {
		var pm = request.getParameterMap();
		if (pm.containsKey("pageSize")) {
			return parsePositiveIntOrDefault(request.getParameter("pageSize"), 10);
		}
		if (pm.containsKey("page_size")) {
			return parsePositiveIntOrDefault(request.getParameter("page_size"), 10);
		}
		return 10;
	}

	private static int parsePositiveIntOrDefault(String raw, int def) {
		if (raw == null) {
			return def;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v > 0 ? v : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int parsePage(String p) {
		if (p == null) {
			return 1;
		}
		try {
			int v = Integer.parseInt(p.trim());
			return v > 0 ? v : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}
}
