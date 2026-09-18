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

package cn.shopex.ecshopx.pointsmall.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.pointsmall.service.PointsmallBaseSettingWriteService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallFrontSettingReadService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallTemplateSettingWriteService;
import cn.shopex.ecshopx.pointsmall.validation.PointsmallBaseSettingSaveParamValidator;
import cn.shopex.ecshopx.pointsmall.web.PointsmallAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("pointsmallSettingAdminV1")
@RequestMapping("/api/v1/pointsmall")
public class SettingController {

	private static final String ENTRANCE_DOT_PREFIX = "entrance.";
	private static final String SCREEN_DOT_PREFIX = "screen.";

	private final PointsmallBaseSettingSaveParamValidator baseSettingSaveParamValidator;
	private final PointsmallBaseSettingWriteService baseSettingWriteService;
	private final PointsmallTemplateSettingWriteService templateSettingWriteService;
	private final PointsmallFrontSettingReadService frontSettingReadService;

	public SettingController(
			PointsmallBaseSettingSaveParamValidator baseSettingSaveParamValidator,
			PointsmallBaseSettingWriteService baseSettingWriteService,
			PointsmallTemplateSettingWriteService templateSettingWriteService,
			PointsmallFrontSettingReadService frontSettingReadService) {
		this.baseSettingSaveParamValidator = baseSettingSaveParamValidator;
		this.baseSettingWriteService = baseSettingWriteService;
		this.templateSettingWriteService = templateSettingWriteService;
		this.frontSettingReadService = frontSettingReadService;
	}

	private void collapseEntranceDotKeys(Map<String, Object> merged) {
		ArrayList<String> dotKeys = new ArrayList<>();
		for (String k : merged.keySet()) {
			if (k != null && k.startsWith(ENTRANCE_DOT_PREFIX)) {
				dotKeys.add(k);
			}
		}
		LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
		Object existing = merged.get("entrance");
		if (existing instanceof Map<?, ?> em) {
			for (Map.Entry<?, ?> e : em.entrySet()) {
				if (e.getKey() instanceof String sk) {
					inner.put(sk, e.getValue());
				}
			}
		}
		for (String k : dotKeys) {
			String suffix = k.substring(ENTRANCE_DOT_PREFIX.length());
			inner.put(suffix, merged.get(k));
			merged.remove(k);
		}
		merged.put("entrance", inner);
	}

	private void collapseScreenDotKeys(Map<String, Object> merged) {
		ArrayList<String> dotKeys = new ArrayList<>();
		for (String k : merged.keySet()) {
			if (k != null && k.startsWith(SCREEN_DOT_PREFIX)) {
				dotKeys.add(k);
			}
		}
		LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
		Object existing = merged.get("screen");
		if (existing instanceof Map<?, ?> em) {
			for (Map.Entry<?, ?> e : em.entrySet()) {
				if (e.getKey() instanceof String sk) {
					inner.put(sk, e.getValue());
				}
			}
		}
		for (String k : dotKeys) {
			String suffix = k.substring(SCREEN_DOT_PREFIX.length());
			inner.put(suffix, merged.get(k));
			merged.remove(k);
		}
		merged.put("screen", inner);
	}

	@Activated(routeAlias = "pointsmall.setting.save")
	@PostMapping(value = "/setting", name = "保存基础设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> saveSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		collapseEntranceDotKeys(merged);

		Map<String, Object> mergedSubset = new LinkedHashMap<>();
		mergedSubset.put("freight_type", merged.get("freight_type"));
		if (merged.containsKey("proportion")) {
			mergedSubset.put("proportion", merged.get("proportion"));
		}
		if (merged.containsKey("rounding_mode")) {
			mergedSubset.put("rounding_mode", merged.get("rounding_mode"));
		}
		mergedSubset.put("entrance", merged.get("entrance"));

		baseSettingSaveParamValidator.validate(mergedSubset);
		long status = baseSettingWriteService.save(companyId, mergedSubset);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@Activated(routeAlias = "pointsmall.setting.get")
	@GetMapping(value = "/setting", name = "获取基础设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSetting(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, Object> data = frontSettingReadService.getAdminBaseSetting(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "pointsmall.template.setting.save")
	@PostMapping(value = "/template/setting", name = "保存模板设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> saveTemplateSetting(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		collapseScreenDotKeys(merged);

		Map<String, Object> subset = new LinkedHashMap<>();
		subset.put("pc_banner", merged.get("pc_banner"));
		subset.put("screen", merged.get("screen"));

		long status = templateSettingWriteService.saveTemplateSetting(companyId, subset);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@Activated(routeAlias = "pointsmall.template.setting.get")
	@GetMapping(value = "/template/setting", name = "获取模板设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTemplateSetting(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, Object> data = frontSettingReadService.getAdminTemplateSetting(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
