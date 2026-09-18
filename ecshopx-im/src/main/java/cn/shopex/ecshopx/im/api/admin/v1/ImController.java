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
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.im.service.MeiqiaConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO_FIXED,
		badRequest = DingoResponse.BadRequestStyle.DINGO_FIXED,
		notFound = true)
@RestController("imMeiqiaAdminV1")
@RequestMapping("/api/v1")
public class ImController {

	private final MeiqiaConfigService meiqiaConfigService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public ImController(
			MeiqiaConfigService meiqiaConfigService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.meiqiaConfigService = meiqiaConfigService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@GetMapping(value = "/im/meiqia", name = "获取im配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> meiqiaInfo(HttpServletRequest request) {
		Map<String, Object> ud = requireOperatorJwtMap(request);
		long companyId = requireCompanyId(ud);
		Map<String, Object> result = meiqiaConfigService.getInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@PostMapping(value = "/im/meiqia", name = "保存im配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> meiqiaUpdate(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> ud = requireOperatorJwtMap(request);
		long companyId = requireCompanyId(ud);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Object channelObj = merged.get("channel");
		if (channelObj == null || !StringUtils.hasText(String.valueOf(channelObj).trim())) {
			throw new BadRequestException("客服渠道必填");
		}
		String channel = String.valueOf(channelObj).trim();
		if (!"single".equals(channel) && !"multi".equals(channel)) {
			throw new BadRequestException("客服渠道必填");
		}

		if ("single".equals(channel)) {
			Object commonObj = merged.get("common");
			if (commonObj == null || !StringUtils.hasText(String.valueOf(commonObj).trim())) {
				throw new BadRequestException("客服链接必填");
			}
		}

		if (!merged.containsKey("is_distributor_open")) {
			throw new BadRequestException("是否开启店铺独立客服必填");
		}
		Object distRaw = merged.get("is_distributor_open");
		if (distRaw == null || !StringUtils.hasText(String.valueOf(distRaw).trim())) {
			throw new BadRequestException("是否开启店铺独立客服必填");
		}

		// Meiqia JSON uses boolean flags; im/echat stores is_open as Redis string literals instead.
		boolean isOpenNorm = normalizeMeiqiaBoolean(merged.get("is_open"));
		boolean isDistNorm = normalizeMeiqiaBoolean(merged.get("is_distributor_open"));

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("channel", channel);
		payload.put("common", merged.get("common"));
		payload.put("wxapp", merged.get("wxapp"));
		payload.put("h5", merged.get("h5"));
		payload.put("app", merged.get("app"));
		payload.put("aliapp", merged.get("aliapp"));
		payload.put("pc", merged.get("pc"));
		payload.put("is_open", isOpenNorm);
		payload.put("is_distributor_open", isDistNorm);

		Map<String, Object> result = meiqiaConfigService.saveImInfo(companyId, payload);

		int operatorIdInt = readOperatorIdForLog(ud);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", operatorIdInt);
		logCtx.put("request_uri", "/api/v1/im/meiqia");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "保存im配置");
		logCtx.put("log_type", "operator");
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
			// align with middleware: logging must not affect the response
		}

		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@GetMapping(value = "/im/meiqia/distributor/{distributor_id}", name = "获取店铺美洽客服配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorMeiQiaSetting(
			HttpServletRequest request,
			@PathVariable("distributor_id") String distributorId) {
		if (isFalsyDistributorPathParam(distributorId)) {
			throw new BadRequestException("店铺id必填");
		}
		Map<String, Object> ud = requireOperatorJwtMap(request);
		long companyId = requireCompanyId(ud);
		Map<String, Object> imInfo = meiqiaConfigService.getInfo(companyId);
		if (!normalizeMeiqiaBoolean(imInfo.get("is_distributor_open"))) {
			throw new ResourceException("未开启店铺独立客服");
		}
		String keySegment = LeadingNumberParser.parseAsString(distributorId);
		Map<String, Object> result = meiqiaConfigService.getDistributorMeiQia(companyId, keySegment);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "im.meiqia.distributor.set")
	@PutMapping(value = "/im/meiqia/distributor/{distributor_id}", name = "设置店铺美洽客服配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setDistributorMeiQia(
			HttpServletRequest request,
			@PathVariable("distributor_id") String distributorId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> ud = requireOperatorJwtMap(request);
		long companyId = requireCompanyId(ud);

		Map<String, Object> imInfo = meiqiaConfigService.getInfo(companyId);
		if (!normalizeMeiqiaBoolean(imInfo.get("is_distributor_open"))) {
			throw new ResourceException("未开启店铺独立客服");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Object channelObj = merged.get("channel");
		if (channelObj == null || !StringUtils.hasText(String.valueOf(channelObj).trim())) {
			throw new BadRequestException("店铺客服渠道必填");
		}
		String channel = String.valueOf(channelObj).trim();
		if (!"single".equals(channel) && !"multi".equals(channel)) {
			throw new BadRequestException("店铺客服渠道必填");
		}
		if ("single".equals(channel)) {
			Object commonObj = merged.get("common");
			if (commonObj == null || !StringUtils.hasText(String.valueOf(commonObj).trim())) {
				throw new BadRequestException("店铺客服链接必填");
			}
		}

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("channel", channel);
		payload.put("common", merged.get("common"));
		payload.put("wxapp", merged.get("wxapp"));
		payload.put("h5", merged.get("h5"));
		payload.put("app", merged.get("app"));
		payload.put("aliapp", merged.get("aliapp"));
		payload.put("pc", merged.get("pc"));

		Map<String, Object> result = meiqiaConfigService.saveDistributorMeiQia(companyId, distributorId, payload);

		int operatorIdInt = readOperatorIdForLog(ud);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", operatorIdInt);
		logCtx.put("request_uri", "/api/v1/im/meiqia/distributor/" + distributorId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "设置店铺美洽客服配置");
		logCtx.put("log_type", "operator");
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
			// align with middleware: logging must not affect the response
		}

		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static boolean isFalsyDistributorPathParam(String distributorId) {
		return distributorId == null || distributorId.isEmpty() || "0".equals(distributorId);
	}

	/**
	 * Boxed {@link Boolean}: true only for {@link Boolean#TRUE}. Otherwise true only when
	 * {@code String.valueOf(v).trim().equals("true")} (case-sensitive); numeric {@code 1} is not true.
	 */
	private static boolean normalizeMeiqiaBoolean(Object v) {
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		return "true".equals(String.valueOf(v).trim());
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
