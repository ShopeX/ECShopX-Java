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

package cn.shopex.ecshopx.wsugc.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.setting.UgcSettingListReadService;
import cn.shopex.ecshopx.wsugc.service.setting.UgcSettingSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("wsugcAdminV1Setting")
@RequestMapping("/api/v1/ugc/setting/point")
public class SettingController {

	private final UgcSettingSaveService ugcSettingSaveService;
	private final UgcSettingListReadService ugcSettingListReadService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public SettingController(
			UgcSettingSaveService ugcSettingSaveService,
			UgcSettingListReadService ugcSettingListReadService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.ugcSettingSaveService = ugcSettingSaveService;
		this.ugcSettingListReadService = ugcSettingListReadService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@PostMapping(value = "/saveSetting", name = "保存积分设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> savePointSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body, jsonLike);
		// FlexibleBody 的 toObjectMap 会丢弃空串；query 中 type= 时仍保留空串以与表单合并行为一致
		if (request.getParameterMap().containsKey("type")) {
			String[] tv = request.getParameterMap().get("type");
			merged.put("type", tv != null && tv.length > 0 && tv[0] != null ? tv[0] : "");
		}
		// toObjectMap 丢弃空串；当 setting= 为空值时仍需走 JSON 解析/遍历逻辑，须保留空串以对齐
		if (request.getParameterMap().containsKey("setting")) {
			String[] sv = request.getParameterMap().get("setting");
			merged.put("setting", sv != null && sv.length > 0 && sv[0] != null ? sv[0] : "");
		}
		Object typeObj = merged.get("type");
		String typeString = typeObj != null ? typeObj.toString() : null;
		Object settingObj = merged.get("setting");

		long companyId = readLong(ud, "company_id", 0L);
		Map<String, Object> data =
				ugcSettingSaveService.savePointSettings(companyId, typeString, settingObj, jsonLike);

		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/setting/point/saveSetting");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "保存积分设置");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long readLong(Map<?, ?> ud, String key, long defaultVal) {
		Object v = ud.get(key);
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body, boolean jsonLike) {
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
			if (v != null && v.length > 0) {
				// 单值参数保留原始空串，避免被当作「无该键」处理
				m.put(k, v[0] != null ? v[0] : "");
			}
		});
		return m;
	}

	@Activated(routeAlias = "ugc.setting.point.getSetting")
	@GetMapping(value = "/getSetting", name = "获取积分设置")
	public ResponseEntity<ApiResult<Object>> getPointSetting(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readLong(ud, "company_id", 0L);
		String typeStr = type != null ? type : "";
		Map<String, String> map = ugcSettingListReadService.loadKeyValueByCompanyAndType(companyId, typeStr);
		if (map.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(map));
	}
}
