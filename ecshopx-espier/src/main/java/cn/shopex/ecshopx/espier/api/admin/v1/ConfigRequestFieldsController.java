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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("espierAdminV1ConfigRequestFields")
@RequestMapping("/api/v1/espier/config")
public class ConfigRequestFieldsController {

	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;
	private final LangueProperties langueProperties;

	public ConfigRequestFieldsController(
			ConfigRequestFieldsApplicationService configRequestFieldsApplicationService,
			LangueProperties langueProperties) {
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "espier.config.request_fields.get")
	@GetMapping(value = "/request_fields", name = "获取配置的请求字段列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));

		int companyId = extractCompanyIdAllowingZero(request);
		int moduleType = parseModuleTypeForListRequest(request);

		String idParam = request.getParameter("id");
		int idFilter;
		if (idParam == null) {
			idFilter = -1;
		} else {
			String t = idParam.trim();
			if (t.isEmpty()) {
				idFilter = 0;
			} else {
				long pl = LeadingNumberParser.parseAsLong(t);
				if (pl > Integer.MAX_VALUE) {
					idFilter = Integer.MAX_VALUE;
				} else if (pl < Integer.MIN_VALUE) {
					idFilter = Integer.MIN_VALUE;
				} else {
					idFilter = (int) pl;
				}
			}
		}

		int page = parseIntFromMergedBody(merged, "page", 1);
		int pageSize = parseIntFromMergedBody(merged, "page_size", 10);

		Map<String, Object> data = configRequestFieldsApplicationService.listEspierAdminPaginated(
				companyId, moduleType, idFilter, page, pageSize, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseModuleTypeForListRequest(HttpServletRequest request) {
		String raw = request.getParameter("module_type");
		if (raw == null) {
			return 0;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0;
		}
		long parsed = LeadingNumberParser.parseAsLong(s);
		if (parsed > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (parsed < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) parsed;
	}

	private static int parseIntFromMergedBody(
			Map<String, Object> merged, String key, int defaultWhenKeyAbsent) {
		if (merged == null || !merged.containsKey(key)) {
			return defaultWhenKeyAbsent;
		}
		Object raw = merged.get(key);
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			long ln = n.longValue();
			if (ln > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (ln < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) ln;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		long pl = LeadingNumberParser.parseAsLong(s);
		if (pl > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (pl < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) pl;
	}

	@Activated(routeAlias = "espier.config.request_fields.create")
	@PostMapping(value = "/request_fields", name = "创建配置的请求字段")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		long companyIdLong = extractCompanyId(request);
		int companyId = Math.toIntExact(companyIdLong);

		int moduleType = parseRequiredPositiveInt(merged.get("module_type"), "模块类型必填！");

		Object labelRaw = merged.get("label");
		if (labelRaw == null) {
			throw new BadRequestException("字段信息必填且长度不能超过50个字符！");
		}
		String label = String.valueOf(labelRaw).trim();
		if (!StringUtils.hasText(label)) {
			throw new BadRequestException("字段信息必填且长度不能超过50个字符！");
		}
		if (label.codePointCount(0, label.length()) > 50) {
			throw new BadRequestException("字段信息必填且长度不能超过50个字符！");
		}

		Object fieldTypeRaw = merged.get("field_type");
		if (fieldTypeRaw == null) {
			throw new BadRequestException("字段信息格式有误！");
		}
		int fieldType = parseStrictInt(fieldTypeRaw, "字段信息格式有误！");
		if (!ConfigRequestFieldsConstants.FIELD_TYPE_MAP.containsKey(fieldType)) {
			throw new BadRequestException("字段信息格式有误！");
		}

		Object alertReqRaw = merged.get("alert_required_message");
		if (alertReqRaw == null) {
			throw new BadRequestException("提示信息必填");
		}
		String alertRequired = String.valueOf(alertReqRaw).trim();
		if (!StringUtils.hasText(alertRequired)) {
			throw new BadRequestException("提示信息必填");
		}

		validateOptional01(merged, "is_open", "是否启用的参数有误！");
		validateOptional01(merged, "is_required", "是否必填的参数有误！");
		validateOptional01(merged, "is_edit", "是否可修改的参数有误！");

		Map<String, Object> result = configRequestFieldsApplicationService.create(companyId, moduleType, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static void validateOptional01(Map<String, Object> merged, String key, String errMsg) {
		if (!merged.containsKey(key) || merged.get(key) == null) {
			return;
		}
		if (!canNormalizeTo01(merged.get(key))) {
			throw new BadRequestException(errMsg);
		}
	}

	private static boolean canNormalizeTo01(Object v) {
		if (v instanceof Boolean) {
			return true;
		}
		if (v instanceof Number n) {
			int i = n.intValue();
			return i == 0 || i == 1;
		}
		String s = String.valueOf(v).trim();
		return "0".equals(s) || "1".equals(s);
	}

	private static int parseRequiredPositiveInt(Object raw, String errMsg) {
		if (raw == null) {
			throw new BadRequestException(errMsg);
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new BadRequestException(errMsg);
		}
		int v;
		if (raw instanceof Number n) {
			v = n.intValue();
		} else {
			try {
				v = Integer.parseInt(String.valueOf(raw).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(errMsg);
			}
		}
		if (v <= 0) {
			throw new BadRequestException(errMsg);
		}
		return v;
	}

	private static int parseStrictInt(Object raw, String errMsg) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(errMsg);
		}
	}

	@SuppressWarnings("unchecked")
	private long extractCompanyId(HttpServletRequest request) {
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (user == null) {
			throw new ResourceException("未登录");
		}
		Object v = user.get("company_id");
		if (v == null) {
			throw new ResourceException("无法获取公司ID");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString());
	}

	@SuppressWarnings("unchecked")
	private int extractCompanyIdAllowingZero(HttpServletRequest request) {
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (user == null) {
			return 0;
		}
		Object v = user.get("company_id");
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) LeadingNumberParser.parseAsLong(String.valueOf(v).trim());
	}

	private static int parseModuleTypeFromRequestOrBody(HttpServletRequest request, Map<String, Object> merged) {
		if (!merged.containsKey("module_type")) {
			return ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		}
		Object raw = merged.get("module_type");
		if (raw == null) {
			return ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			return ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		}
		String asStr = String.valueOf(raw).trim();
		if (!StringUtils.hasText(asStr)) {
			return ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return (int) LeadingNumberParser.parseAsLong(asStr);
	}

	@Activated(routeAlias = "espier.config.request_fields.switch")
	@PutMapping(value = "/request_fields/switch", name = "更新配置的请求字段的开关")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSwitch(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		int companyId = extractCompanyIdAllowingZero(request);
		int id = parseUpdateInfoIdFromMerged(merged);

		if (id > 0) {
			int type = parseSwitchTypeFromMerged(merged);
			boolean switchOn = parseLooseBoolean(merged.get("switch"));
			configRequestFieldsApplicationService.updateSwitch(companyId, id, type, switchOn, 0);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.config.request_fields.info")
	@PutMapping(value = "/request_fields/info", name = "更新配置的请求字段的内容")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateInfo(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		long companyIdLong = extractCompanyId(request);
		int companyId = Math.toIntExact(companyIdLong);

		int id = parseUpdateInfoIdFromMerged(merged);

		Object labelRaw = merged.get("label");
		if (labelRaw == null) {
			throw new BadRequestException("字段信息必填且长度不能超过50个字符！");
		}
		String label = String.valueOf(labelRaw).trim();
		if (!StringUtils.hasText(label)) {
			throw new BadRequestException("字段信息必填且长度不能超过50个字符！");
		}
		if (label.codePointCount(0, label.length()) > 50) {
			throw new BadRequestException("字段信息必填且长度不能超过50个字符！");
		}

		Object fieldTypeRaw = merged.get("field_type");
		if (fieldTypeRaw == null) {
			throw new BadRequestException("字段信息格式有误！");
		}
		int fieldType = parseStrictInt(fieldTypeRaw, "字段信息格式有误！");
		if (!ConfigRequestFieldsConstants.FIELD_TYPE_MAP.containsKey(fieldType)) {
			throw new BadRequestException("字段信息格式有误！");
		}

		Object alertReqRaw = merged.get("alert_required_message");
		if (alertReqRaw == null) {
			throw new BadRequestException("提示信息必填");
		}
		String alertRequired = String.valueOf(alertReqRaw).trim();
		if (!StringUtils.hasText(alertRequired)) {
			throw new BadRequestException("提示信息必填");
		}

		Map<String, Object> result = configRequestFieldsApplicationService.updateInfo(companyId, id, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static int parseUpdateInfoIdFromMerged(Map<String, Object> merged) {
		if (!merged.containsKey("id")) {
			return 0;
		}
		Object v = merged.get("id");
		if (v == null) {
			return 0;
		}
		if (v instanceof String s && !StringUtils.hasText(s.trim())) {
			return 0;
		}
		if (v instanceof Number n) {
			long ln = n.longValue();
			if (ln > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (ln < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) ln;
		}
		long parsed = LeadingNumberParser.parseAsLong(String.valueOf(v).trim());
		if (parsed > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (parsed < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) parsed;
	}

	private static int parseSwitchTypeFromMerged(Map<String, Object> merged) {
		if (!merged.containsKey("type")) {
			return 0;
		}
		Object raw = merged.get("type");
		if (raw == null) {
			return 0;
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		long parsed = LeadingNumberParser.parseAsLong(String.valueOf(raw).trim());
		if (parsed > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (parsed < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) parsed;
	}

	private static boolean parseLooseBoolean(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			String lower = t.toLowerCase(Locale.ROOT);
			if ("false".equals(lower) || "0".equals(lower)) {
				return false;
			}
			if ("true".equals(lower) || "1".equals(lower)) {
				return true;
			}
			return true;
		}
		String t = String.valueOf(raw).trim();
		if (!StringUtils.hasText(t)) {
			return false;
		}
		String lower = t.toLowerCase(Locale.ROOT);
		if ("false".equals(lower) || "0".equals(lower)) {
			return false;
		}
		if ("true".equals(lower) || "1".equals(lower)) {
			return true;
		}
		return true;
	}

	@Activated(routeAlias = "espier.config.request_fields.delete")
	@DeleteMapping(value = "/request_fields", name = "删除配置的请求字段")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		int companyId = extractCompanyIdAllowingZero(request);
		int id = parseUpdateInfoIdFromMerged(merged);

		configRequestFieldsApplicationService.deleteForChiefApplyField(companyId, id, 0);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.config.request_field_setting.post")
	@PostMapping(value = "/request_field_setting", name = "设置配置请求字段的配置项")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		int companyId = extractCompanyIdAllowingZero(request);
		int moduleType = parseModuleTypeFromRequestOrBody(request, merged);
		configRequestFieldsApplicationService.updateConfig(companyId, moduleType, merged, 0);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.config.request_field_setting.get")
	@GetMapping(value = "/request_field_setting", name = "获取配置请求字段的配置项")
	public ResponseEntity<ApiResult<Map<String, Object>>> getConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		int companyId = extractCompanyIdAllowingZero(request);
		int moduleType = parseModuleTypeFromRequestOrBody(request, merged);
		Map<String, Object> payload = configRequestFieldsApplicationService.getConfig(companyId, moduleType, 0);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}
}
