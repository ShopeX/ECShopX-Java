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

package cn.shopex.ecshopx.im.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.im.service.EChatConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(badRequest = DingoResponse.BadRequestStyle.DINGO_FIXED)
@RestController("imEChatAdminV1")
@RequestMapping("/api/v1")
public class EChatController {

	private final EChatConfigService eChatConfigService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public EChatController(
			EChatConfigService eChatConfigService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.eChatConfigService = eChatConfigService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@GetMapping(value = "/im/echat", name = "获取一洽配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInfo(HttpServletRequest request) {
		Map<String, Object> ud = requireOperatorJwtMap(request);
		long companyId = requireCompanyId(ud);
		Map<String, Object> result = eChatConfigService.getInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@PostMapping(value = "/im/echat", name = "保存一洽配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveInfo(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> ud = requireOperatorJwtMap(request);
		long companyId = requireCompanyId(ud);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("is_open", normalizeIsOpen(merged.get("is_open")));
		Object urlObj = merged.get("echat_url");
		payload.put("echat_url", urlObj != null ? urlObj : null);

		Map<String, Object> result = eChatConfigService.saveInfo(companyId, payload);

		int operatorIdInt = readOperatorIdForLog(ud);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", operatorIdInt);
		logCtx.put("request_uri", "/api/v1/im/echat");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "保存一洽配置");
		logCtx.put("log_type", "operator");
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
			// align with middleware: logging must not affect the response
		}

		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static String normalizeIsOpen(Object v) {
		if (v == null) {
			return "false";
		}
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b) ? "true" : "false";
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0 ? "true" : "false";
		}
		if (v instanceof CharSequence cs) {
			return "true".contentEquals(cs) ? "true" : "false";
		}
		String str = String.valueOf(v);
		return "true".contentEquals(str) ? "true" : "false";
	}

	private static Map<String, Object> requireOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		return user;
	}

	private static long requireCompanyId(Map<String, Object> user) {
		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
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

	private static int readOperatorIdForLog(Map<String, Object> ud) {
		Object opIdObj = ud.get("operator_id");
		if (opIdObj instanceof Number n) {
			return (int) n.longValue();
		}
		if (opIdObj != null) {
			try {
				return (int) Long.parseLong(opIdObj.toString());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
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
