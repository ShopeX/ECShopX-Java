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

package cn.shopex.ecshopx.form.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.form.service.FormRoutePermissionService;
import cn.shopex.ecshopx.form.service.UserTranscriptCreateService;
import cn.shopex.ecshopx.form.service.UserTranscriptListService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("formUserTranscriptsAdminV1")
@RequestMapping("/api/v1")
public class UserTranscriptsController {

	private final CompanysActivationService companysActivationService;
	private final FormRoutePermissionService formRoutePermissionService;
	private final UserTranscriptCreateService userTranscriptCreateService;
	private final UserTranscriptListService userTranscriptListService;
	private final ObjectMapper objectMapper;

	public UserTranscriptsController(
			CompanysActivationService companysActivationService,
			FormRoutePermissionService formRoutePermissionService,
			UserTranscriptCreateService userTranscriptCreateService,
			UserTranscriptListService userTranscriptListService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.formRoutePermissionService = formRoutePermissionService;
		this.userTranscriptCreateService = userTranscriptCreateService;
		this.userTranscriptListService = userTranscriptListService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "usertranscript.create")
	@PostMapping(value = "/usertranscript", name = "创建用户成绩单")
	public ResponseEntity<ApiResult<Map<String, Object>>> createUserTranscript(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
		formRoutePermissionService.assertUserTranscriptCreate(user);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("company_id", companyId);

		if (!isNonBlankParam(merged.get("user_id"))) {
			throw new BadRequestException("参数缺失：user_id");
		}
		if (!isNonBlankParam(merged.get("transcript_id"))) {
			throw new BadRequestException("参数缺失：transcript_id");
		}
		if (!isNonBlankParam(merged.get("transcript_name"))) {
			throw new BadRequestException("参数缺失：transcript_name");
		}

		Map<String, Object> result = userTranscriptCreateService.create(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "usertranscript.list")
	@GetMapping(value = "/usertranscript", name = "获取用户成绩单")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUserTranscript(HttpServletRequest request) {
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
		formRoutePermissionService.assertUserTranscriptList(user);

		Map<String, Object> merged = mergeGetInput(request);
		Map<String, Object> result = userTranscriptListService.list(merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static boolean isNonBlankParam(Object v) {
		if (v == null) {
			return false;
		}
		String s = String.valueOf(v);
		return StringUtils.hasText(s);
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

	private Map<String, Object> mergeGetInput(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(parameterMapToMap(request));
		String ct = request.getContentType();
		if (ct != null && ct.toLowerCase().contains("application/json")) {
			try {
				String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
				if (StringUtils.hasText(body.trim())) {
					Map<String, Object> jsonMap =
							objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
					merged.putAll(jsonMap);
				}
			} catch (IOException e) {
				throw new BadRequestException("请求体 JSON 解析失败");
			}
		}
		return merged;
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
