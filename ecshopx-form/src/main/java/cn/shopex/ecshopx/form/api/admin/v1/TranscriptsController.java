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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.form.service.FormRoutePermissionService;
import cn.shopex.ecshopx.form.service.TranscriptCreateService;
import cn.shopex.ecshopx.form.service.TranscriptDeleteService;
import cn.shopex.ecshopx.form.service.TranscriptGetService;
import cn.shopex.ecshopx.form.service.TranscriptUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("formTranscriptsAdminV1")
@RequestMapping("/api/v1")
public class TranscriptsController {

	private final CompanysActivationService companysActivationService;
	private final FormRoutePermissionService formRoutePermissionService;
	private final TranscriptCreateService transcriptCreateService;
	private final TranscriptGetService transcriptGetService;
	private final TranscriptUpdateService transcriptUpdateService;
	private final TranscriptDeleteService transcriptDeleteService;

	public TranscriptsController(
			CompanysActivationService companysActivationService,
			FormRoutePermissionService formRoutePermissionService,
			TranscriptCreateService transcriptCreateService,
			TranscriptGetService transcriptGetService,
			TranscriptUpdateService transcriptUpdateService,
			TranscriptDeleteService transcriptDeleteService) {
		this.companysActivationService = companysActivationService;
		this.formRoutePermissionService = formRoutePermissionService;
		this.transcriptCreateService = transcriptCreateService;
		this.transcriptGetService = transcriptGetService;
		this.transcriptUpdateService = transcriptUpdateService;
		this.transcriptDeleteService = transcriptDeleteService;
	}

	@Activated(routeAlias = "transcript.create")
	@PostMapping(value = "/transcript", name = "创建成绩单")
	public ResponseEntity<ApiResult<Map<String, Object>>> createTranscript(
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
		formRoutePermissionService.assertTranscriptCreate(user);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("company_id", companyId);

		if (!isNonBlankParam(merged.get("transcript_name"))) {
			throw new BadRequestException("成绩单名称必填！", 411);
		}
		if (!isNonBlankParam(merged.get("template_name"))) {
			throw new BadRequestException("模板必填", 411);
		}

		Map<String, Object> result = transcriptCreateService.create(companyId, merged);
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

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	@Activated(routeAlias = "transcript.detail")
	@GetMapping(value = "/transcript/{transcript_id}", name = "获取成绩单")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTranscript(
			HttpServletRequest request,
			@PathVariable("transcript_id") String transcriptIdPath) {
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
		formRoutePermissionService.assertTranscriptDetail(user);

		long transcriptId = parseTranscriptId(transcriptIdPath);
		Map<String, Object> result = transcriptGetService.getInfo(companyId, transcriptId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "transcript.update")
	@PatchMapping(value = "/transcript/{transcript_id}", name = "更新成绩单")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateTranscript(
			HttpServletRequest request,
			@PathVariable("transcript_id") String transcriptIdPath,
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
		formRoutePermissionService.assertTranscriptUpdate(user);

		long transcriptId = parseTranscriptId(transcriptIdPath);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("company_id", companyId);
		merged.put("transcript_id", transcriptId);

		Map<String, Object> result = transcriptUpdateService.update(companyId, transcriptId, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static long parseTranscriptId(String transcriptIdPath) {
		if (!StringUtils.hasText(transcriptIdPath)) {
			throw new BadRequestException("transcript_id 无效");
		}
		try {
			return Long.parseLong(transcriptIdPath.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("transcript_id 无效");
		}
	}

	@Activated(routeAlias = "transcript.delete")
	@DeleteMapping(value = "/transcript/{transcript_id}", name = "删除成绩单")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTranscript(
			HttpServletRequest request,
			@PathVariable("transcript_id") String transcriptIdPath) {
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
		formRoutePermissionService.assertTranscriptDelete(user);

		long transcriptId = parseTranscriptId(transcriptIdPath);
		Map<String, Object> result = transcriptDeleteService.delete(companyId, transcriptId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}
}
