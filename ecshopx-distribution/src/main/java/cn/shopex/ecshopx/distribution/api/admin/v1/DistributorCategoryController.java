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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.distribution.service.DistributorCategoryService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributorCategory")
@RequestMapping("/api/v1/distributor")
public class DistributorCategoryController {

	private static final String NO_UPDATE_DATA_FOUND_MSG = "未查询到更新数据";

	private final CompanysActivationService companysActivationService;
	private final DistributorCategoryService distributorCategoryService;

	public DistributorCategoryController(
			CompanysActivationService companysActivationService,
			DistributorCategoryService distributorCategoryService) {
		this.companysActivationService = companysActivationService;
		this.distributorCategoryService = distributorCategoryService;
	}

	@GetMapping(value = "/category", name = "获取店铺分类列表")
	public ResponseEntity<?> getCategoryList(
			HttpServletRequest request,
			@RequestParam(value = "category_name", required = false) String categoryName) {
		long companyId = readCompanyId(request);
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Pagination pagination = parseRequiredPagination(request);
		if (pagination.validationMessage() != null) {
			return ResponseEntity.ok(dingoValidation422(pagination.validationMessage()));
		}
		Map<String, Object> data =
				distributorCategoryService.list(companyId, pagination.page(), pagination.pageSize(), categoryName);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/category/{categoryId}", name = "获取店铺分类详情")
	public ResponseEntity<?> getCategoryInfo(HttpServletRequest request, @PathVariable("categoryId") String categoryId) {
		long companyId = readCompanyId(request);
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Optional<Long> idOpt = tryParseCategoryId(categoryId);
		if (idOpt.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		Map<String, Object> row = distributorCategoryService.getInfo(companyId, idOpt.get());
		if (row.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@PostMapping(value = "/category", name = "新增店铺分类")
	public ResponseEntity<?> createCategory(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> merged = mergeInput(request, body);
		String categoryName = stringParam(merged, "category_name");
		if (!StringUtils.hasText(categoryName)) {
			return ResponseEntity.ok(dingoValidation422("请填写店铺分类名称"));
		}
		Map<String, Object> data = distributorCategoryService.create(companyId, categoryName);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PutMapping(value = "/category/{categoryId}", name = "更新店铺分类")
	public ResponseEntity<?> updateCategory(
			HttpServletRequest request,
			@PathVariable("categoryId") String categoryId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Optional<Long> idOpt = tryParseCategoryId(categoryId);
		if (idOpt.isEmpty()) {
			return ResponseEntity.ok(dingoValidation422(NO_UPDATE_DATA_FOUND_MSG));
		}
		Map<String, Object> merged = mergeInput(request, body);
		String categoryName = stringParam(merged, "category_name");
		if (!StringUtils.hasText(categoryName)) {
			return ResponseEntity.ok(dingoValidation422("请填写店铺分类名称"));
		}
		String categoryCode = stringParam(merged, "category_code");
		if (!StringUtils.hasText(categoryCode)) {
			return ResponseEntity.ok(dingoValidation422("请填写分类编号"));
		}
		try {
			Map<String, Object> data =
					distributorCategoryService.update(companyId, idOpt.get(), categoryName, categoryCode);
			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			if (NO_UPDATE_DATA_FOUND_MSG.equals(ex.getMessage())) {
				return ResponseEntity.ok(dingoValidation422(NO_UPDATE_DATA_FOUND_MSG));
			}
			throw ex;
		}
	}

	@DeleteMapping(value = "/category/{categoryId}", name = "删除店铺分类")
	public ResponseEntity<?> deleteCategory(HttpServletRequest request, @PathVariable("categoryId") String categoryId) {
		long companyId = readCompanyId(request);
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Optional<Long> idOpt = tryParseCategoryId(categoryId);
		if (idOpt.isEmpty()) {
			return ResponseEntity.ok(dingoValidation422("分类不存在"));
		}
		try {
			distributorCategoryService.delete(companyId, idOpt.get());
		} catch (ResourceException ex) {
			if ("分类不存在".equals(ex.getMessage())) {
				return ResponseEntity.ok(dingoValidation422("分类不存在"));
			}
			throw ex;
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		return toLong(ud.get("company_id"));
	}

	private static Pagination parseRequiredPagination(HttpServletRequest request) {
		Map<String, String[]> pm = request.getParameterMap();
		if (!pm.containsKey("page")) {
			return Pagination.invalid("请填写页码");
		}
		if (!pm.containsKey("pageSize")) {
			return Pagination.invalid("请填写每页长度");
		}
		String pageTrim = trimOrEmpty(request.getParameter("page"));
		String pageSizeTrim = trimOrEmpty(request.getParameter("pageSize"));
		int page = isFalsyForPagination(pageTrim) ? 1 : parseIntOrDefault(pageTrim, 1);
		int pageSize = isFalsyForPagination(pageSizeTrim) ? 20 : parseIntOrDefault(pageSizeTrim, 20);
		long offsetBase = (page - 1L) * (long) pageSize;
		if (offsetBase < 0) {
			return Pagination.invalid("Offset must be a positive integer or zero, " + offsetBase + " given");
		}
		return Pagination.valid(page, pageSize);
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input =
					new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static String stringParam(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		return v == null ? null : v.toString();
	}

	private static Optional<Long> tryParseCategoryId(String categoryId) {
		if (categoryId == null) {
			return Optional.empty();
		}
		String trimmed = categoryId.trim();
		if (!StringUtils.hasText(trimmed)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(trimmed));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static Map<String, Object> dingoValidation422(String message) {
		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("message", message);
		inner.put("status_code", 422);
		Map<String, Object> dingoStyle = new LinkedHashMap<>();
		dingoStyle.put("data", inner);
		return dingoStyle;
	}

	private static String trimOrEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static boolean isFalsyForPagination(String trimmed) {
		return trimmed.isEmpty() || "0".equals(trimmed);
	}

	private static int parseIntOrDefault(String s, int defaultVal) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
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

	private record Pagination(int page, int pageSize, String validationMessage) {

		static Pagination valid(int page, int pageSize) {
			return new Pagination(page, pageSize, null);
		}

		static Pagination invalid(String message) {
			return new Pagination(1, 20, message);
		}
	}
}
