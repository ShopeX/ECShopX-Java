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
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.PromotionRegisterDistributorListService;
import cn.shopex.ecshopx.promotions.service.RegisterDistributorCreateService;
import cn.shopex.ecshopx.promotions.service.RegisterDistributorDeleteService;
import cn.shopex.ecshopx.promotions.service.RegisterDistributorPromotionListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController("promotionsAdminV1Register")
@RequestMapping("/api/v1/promotions")
public class RegisterController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 200;

	private final RegisterDistributorCreateService registerDistributorCreateService;
	private final PromotionRegisterDistributorListService promotionRegisterDistributorListService;
	private final RegisterDistributorPromotionListService registerDistributorPromotionListService;
	private final RegisterDistributorDeleteService registerDistributorDeleteService;
	private final LangueProperties langueProperties;

	public RegisterController(
			RegisterDistributorCreateService registerDistributorCreateService,
			PromotionRegisterDistributorListService promotionRegisterDistributorListService,
			RegisterDistributorPromotionListService registerDistributorPromotionListService,
			RegisterDistributorDeleteService registerDistributorDeleteService,
			LangueProperties langueProperties) {
		this.registerDistributorCreateService = registerDistributorCreateService;
		this.promotionRegisterDistributorListService = promotionRegisterDistributorListService;
		this.registerDistributorPromotionListService = registerDistributorPromotionListService;
		this.registerDistributorDeleteService = registerDistributorDeleteService;
		this.langueProperties = langueProperties;
	}

	private static boolean filterValuePresent(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (v instanceof CharSequence s) {
			String t = s.toString().trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
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

	@Activated(routeAlias = "Promotions.register.add")
	@PostMapping(value = "/register/distributor", name = "注册促销分销商")
	public ResponseEntity<ApiResult<Map<String, Object>>> createRegister(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		if (!merged.containsKey("register_type") || !filterValuePresent(merged.get("register_type"))) {
			merged.put("register_type", "distributor");
		}
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String requestLangTag = RequestLangTag.current(langueProperties);
		registerDistributorCreateService.createRegister(companyId, merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "promotions.register.get")
	@GetMapping(value = "/register/distributor", name = "注册促销列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRegisterList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		int p = parsePageParam(page);
		int ps = parsePageSizeParam(pageSize);
		String lang = RequestLangTag.current(langueProperties);
		Map<String, Object> data = registerDistributorPromotionListService.getRegisterList(companyId, p, ps, lang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePageParam(String raw) {
		Integer v = parsePositiveIntOrNull(raw);
		if (v == null) {
			return DEFAULT_PAGE;
		}
		return Math.max(1, v);
	}

	private static int parsePageSizeParam(String raw) {
		Integer v = parsePositiveIntOrNull(raw);
		if (v == null) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(Math.max(1, v), MAX_PAGE_SIZE);
	}

	/** null = 使用调用方默认值（空、空白、"0"、非数字、<=0） */
	private static Integer parsePositiveIntOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		int v;
		try {
			v = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return null;
		}
		if (v <= 0) {
			return null;
		}
		return v;
	}

	@Activated(routeAlias = "promotions.register.get")
	@GetMapping(value = "/register/distributor/{id}", name = "注册促销详情")
	public ResponseEntity<ApiResult<Object>> getRegisterInfo(
			HttpServletRequest request,
			@PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Object body = registerDistributorPromotionListService.getRegisterInfo(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "promotions.register.get")
	@DeleteMapping(value = "/register/distributor/{id}", name = "删除注册促销")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteRegister(
			HttpServletRequest request,
			@PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		registerDistributorDeleteService.deleteRegister(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "promotions.register.get")
	@GetMapping(value = "/distributor", name = "分销商列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorList(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		return ResponseEntity.ok(
				ApiResult.ok(promotionRegisterDistributorListService.getDistributorList(companyId, request)));
	}
}
