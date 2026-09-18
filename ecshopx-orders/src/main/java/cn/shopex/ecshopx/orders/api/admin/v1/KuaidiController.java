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

package cn.shopex.ecshopx.orders.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.setting.KuaidiSettingRedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("ordersAdminV1Kuaidi")
@RequestMapping("/api/v1/trade/kuaidi")
public class KuaidiController {

	private final KuaidiSettingRedisService kuaidiSettingRedisService;
	private final ObjectMapper objectMapper;

	public KuaidiController(
			KuaidiSettingRedisService kuaidiSettingRedisService, ObjectMapper objectMapper) {
		this.kuaidiSettingRedisService = kuaidiSettingRedisService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "trade.kuaidi.setting.set")
	@PostMapping(value = "/setting", name = "快递配置保存", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setKuaidiSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Object kuaidiTypeObj = merged.get("kuaidi_type");
		String rawType = kuaidiTypeObj == null ? null : String.valueOf(kuaidiTypeObj).trim();
		if (rawType == null || rawType.isEmpty()) {
			throw new BadRequestException("快递类型无效");
		}
		if (!("kdniao".equals(rawType) || "kuaidi100".equals(rawType))) {
			throw new BadRequestException("快递类型无效");
		}
		Object configRaw = merged.get("config");
		JsonNode payload = normalizeConfigToJsonNode(objectMapper, configRaw);
		kuaidiSettingRedisService.setKuaidiSetting(companyId, rawType, payload);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	private static JsonNode normalizeConfigToJsonNode(ObjectMapper objectMapper, Object configRaw) {
		if (configRaw == null) {
			return objectMapper.getNodeFactory().nullNode();
		}
		if (configRaw instanceof Map<?, ?> m) {
			return objectMapper.valueToTree(m);
		}
		if (configRaw instanceof String s) {
			try {
				JsonNode t = objectMapper.readTree(s);
				if (t != null && t.isObject()) {
					return t;
				}
			} catch (JsonProcessingException ignored) {
			}
			return TextNode.valueOf(s);
		}
		return objectMapper.valueToTree(configRaw);
	}

	@Activated(routeAlias = "trade.kuaidi.setting.get")
	@GetMapping(value = "/setting", name = "快递配置")
	public ApiResult<Object> getKuaidiSetting(
			HttpServletRequest request,
			@RequestParam(value = "kuaidi_type", required = false) String kuaidiTypeRaw) {
		long companyId = readCompanyIdFromJwt(request);
		String rawType = kuaidiTypeRaw == null ? "" : kuaidiTypeRaw.trim();
		if (rawType.isEmpty() || !("kdniao".equals(rawType) || "kuaidi100".equals(rawType))) {
			throw new BadRequestException("快递类型无效");
		}
		return ApiResult.ok(kuaidiSettingRedisService.getKuaidiSetting(companyId, rawType));
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
