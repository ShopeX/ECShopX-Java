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

package cn.shopex.ecshopx.popularize.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.popularize.service.PopularizeSettingSaveService;
import cn.shopex.ecshopx.popularize.service.PromoterGradeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("popularizeSettingAdminV1")
@RequestMapping("/api/v1")
public class SettingController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final PromoterGradeService promoterGradeService;

	public SettingController(
			PopularizeSettingSaveService popularizeSettingSaveService,
			PromoterGradeService promoterGradeService) {
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.promoterGradeService = promoterGradeService;
	}

	@Activated(routeAlias = "popularize.config.get")
	@GetMapping(value = "/popularize/config", name = "获取分销配置信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getConfig(
			HttpServletRequest request,
			@RequestParam(value = "pathSource", required = false) String pathSource) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Object rawUd = request.getAttribute(OPERATOR_JWT_USER_DATA);
		String operatorType = null;
		if (rawUd instanceof Map<?, ?> ud) {
			Object o = ud.get("operator_type");
			if (o != null) {
				String t = String.valueOf(o).trim();
				operatorType = t.isEmpty() ? null : t;
			}
		}
		Map<String, Object> data = popularizeSettingSaveService.getConfig(companyId, operatorType, pathSource);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "popularize.config.set")
	@PostMapping(value = "/popularize/config", name = "设置分销配置信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> setConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		popularizeSettingSaveService.setConfig(companyId, body != null ? body : new LinkedHashMap<>());
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	@Activated(routeAlias = "popularize.promoter.config.get")
	@GetMapping(value = "/popularize/promoter/config", name = "获取推广员等级")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPromoterGradeConfig(
			HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = promoterGradeService.getPromoterGradeConfig(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "popularize.promoter.config.set")
	@PostMapping(value = "/popularize/promoter/config", name = "设置推广员等级")
	public ResponseEntity<ApiResult<Map<String, Object>>> setPromoterGradeConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		promoterGradeService.setPromoterGradeConfig(
				companyId, body != null ? body : new LinkedHashMap<>());
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
