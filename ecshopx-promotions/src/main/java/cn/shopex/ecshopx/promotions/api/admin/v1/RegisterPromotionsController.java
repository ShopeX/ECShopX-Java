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

package cn.shopex.ecshopx.promotions.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.RegisterDistributorCreateService;
import cn.shopex.ecshopx.promotions.service.RegisterPointConfigService;
import cn.shopex.ecshopx.promotions.service.RegisterPromotionsConfigReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
@RestController("promotionsAdminV1RegisterPromotions")
@RequestMapping("/api/v1/promotions")
public class RegisterPromotionsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final RegisterPointConfigService registerPointConfigService;
	private final RegisterDistributorCreateService registerDistributorCreateService;
	private final RegisterPromotionsConfigReadService registerPromotionsConfigReadService;
	private final LangueProperties langueProperties;

	public RegisterPromotionsController(
			RegisterPointConfigService registerPointConfigService,
			RegisterDistributorCreateService registerDistributorCreateService,
			RegisterPromotionsConfigReadService registerPromotionsConfigReadService,
			LangueProperties langueProperties) {
		this.registerPointConfigService = registerPointConfigService;
		this.registerDistributorCreateService = registerDistributorCreateService;
		this.registerPromotionsConfigReadService = registerPromotionsConfigReadService;
		this.langueProperties = langueProperties;
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								input.put(k, v[0]);
							}
						});
		if (body != null) {
			input.putAll(body);
		}
		return input;
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
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

	@Activated(routeAlias = "promotions.register.get")
	@GetMapping(value = "/register", name = "注册引导营销配置")
	public ResponseEntity<ApiResult<Object>> getRegisterPromotionsConfig(
			HttpServletRequest request,
			@RequestParam(value = "register_type", required = false) String registerType) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String requestLangTag = RequestLangTag.current(langueProperties);
		Object data =
				registerPromotionsConfigReadService.getRegisterPromotionsConfig(
						companyId, registerType, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "Promotions.register.add")
	@PostMapping(value = "/register", name = "保存注册引导营销")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveRegisterPromotionsConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		if (!merged.containsKey("is_open")) {
			merged.put("is_open", "false");
		}
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		registerDistributorCreateService.saveRegisterPromotionsConfig(companyId, merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "promotions.register.point.get")
	@GetMapping(value = "/point", name = "注册积分配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRegisterPointConfig(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Map<String, Object> data = registerPointConfigService.getRegisterPointConfig(companyId, type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "Promotions.register.point.add")
	@PostMapping(value = "/point", name = "保存注册积分")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveRegisterPointConfig(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		registerPointConfigService.saveRegisterPointConfig(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
