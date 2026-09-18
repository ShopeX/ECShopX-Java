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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ItemsCategoryClassificationService;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDeleteService;
import cn.shopex.ecshopx.goods.service.ItemsCategoryInfoService;
import cn.shopex.ecshopx.goods.service.ItemsCategoryQueryService;
import cn.shopex.ecshopx.goods.service.ItemsCategorySaleableFilterService;
import cn.shopex.ecshopx.goods.service.ItemsCategorySaveService;
import cn.shopex.ecshopx.goods.service.ItemsCategoryUpdateService;
import cn.shopex.ecshopx.goods.service.dto.CategoryTreeNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
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
@RestController("goodsAdminV1ItemsCategory")
@RequestMapping("/api/v1/goods")
public class ItemsCategoryController {

	private final ItemsCategorySaveService itemsCategorySaveService;
	private final ItemsCategoryQueryService itemsCategoryQueryService;
	private final ItemsCategoryInfoService itemsCategoryInfoService;
	private final ItemsCategoryClassificationService itemsCategoryClassificationService;
	private final ObjectMapper objectMapper;
	private final ItemsCategoryDeleteService itemsCategoryDeleteService;
	private final ItemsCategoryUpdateService itemsCategoryUpdateService;
	private final ItemsCategorySaleableFilterService itemsCategorySaleableFilterService;
	private final LangueProperties langueProperties;

	public ItemsCategoryController(ItemsCategorySaveService itemsCategorySaveService,
			ItemsCategoryQueryService itemsCategoryQueryService, ItemsCategoryInfoService itemsCategoryInfoService,
			ItemsCategoryClassificationService itemsCategoryClassificationService, ObjectMapper objectMapper,
			ItemsCategoryDeleteService itemsCategoryDeleteService, ItemsCategoryUpdateService itemsCategoryUpdateService,
			ItemsCategorySaleableFilterService itemsCategorySaleableFilterService, LangueProperties langueProperties) {
		this.itemsCategorySaveService = itemsCategorySaveService;
		this.itemsCategoryQueryService = itemsCategoryQueryService;
		this.itemsCategoryInfoService = itemsCategoryInfoService;
		this.itemsCategoryClassificationService = itemsCategoryClassificationService;
		this.objectMapper = objectMapper;
		this.itemsCategoryDeleteService = itemsCategoryDeleteService;
		this.itemsCategoryUpdateService = itemsCategoryUpdateService;
		this.itemsCategorySaleableFilterService = itemsCategorySaleableFilterService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "goods.category.saleable_filter.get")
	@GetMapping(value = "/category/saleable-filter/get", name = "获取分类可售过滤开关")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSaleableCategoryFilterStatus(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		boolean enabled = itemsCategorySaleableFilterService.isEnabled(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("enabled", enabled)));
	}

	@Activated(routeAlias = "goods.category.saleable_filter.set")
	@PostMapping(value = "/category/saleable-filter", name = "设置分类可售过滤开关")
	public ResponseEntity<ApiResult<Map<String, Object>>> setSaleableCategoryFilterStatus(HttpServletRequest request,
			@RequestParam(name = "enabled", required = false) String enabledParam,
			@FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object enabledRaw = enabledParam;
		if (enabledRaw == null && body != null) {
			enabledRaw = body.get("enabled");
		}
		if (enabledRaw == null) {
			throw new ResourceException("enabled必填");
		}
		boolean enabled = parseSaleableFilterEnabled(enabledRaw);
		long companyId = readRequiredLong(ud, "company_id");
		itemsCategorySaleableFilterService.setEnabled(companyId, enabled);
		return ResponseEntity.ok(ApiResult.ok(Map.of("enabled", enabled)));
	}

	/** 对齐 PHP：仅 1/'1'/true/'true' 为开启，其余（含 false/0）为关闭。 */
	private static boolean parseSaleableFilterEnabled(Object enabledRaw) {
		if (enabledRaw instanceof Boolean b) {
			return b;
		}
		if (enabledRaw instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = enabledRaw.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	@Activated(routeAlias = "goods.category.get")
	@GetMapping(value = "/category/{category_id}", name = "分类详情")
	public ResponseEntity<?> getCategoryInfo(HttpServletRequest request,
			@PathVariable("category_id") long categoryId,
			@RequestParam(name = "item_id", required = false) String itemIdRaw,
			@RequestParam(name = "is_point", required = false, defaultValue = "0") String isPointRaw,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		CategoryInfoQueryParsed parsed = parseCategoryInfoQueryParams(itemIdRaw, isPointRaw);
		if (parsed.errorBody() != null) {
			return ResponseEntity.ok(parsed.errorBody());
		}
		Object data = itemsCategoryInfoService.getCategoryInfo(companyId, categoryId, parsed.itemIdOpt(), parsed.isPoint(),
				countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static CategoryInfoQueryParsed parseCategoryInfoQueryParams(String itemIdRaw, String isPointRaw) {
		if (!StringUtils.hasText(itemIdRaw) || "0".equals(itemIdRaw.trim())) {
			return new CategoryInfoQueryParsed(Optional.empty(), 0, null);
		}
		String trimmed = itemIdRaw.trim();
		long itemId;
		try {
			itemId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return CategoryInfoQueryParsed.validationError(Map.of("item_id", List.of("validation.integer")));
		}
		if (itemId < 1L) {
			return CategoryInfoQueryParsed.validationError(Map.of("item_id", List.of("validation.min.numeric")));
		}
		int isPoint = parseIsPointZeroOrOneStrict(isPointRaw);
		if (isPoint < 0) {
			return CategoryInfoQueryParsed.validationError(Map.of("is_point", List.of("validation.in")));
		}
		return new CategoryInfoQueryParsed(Optional.of(itemId), isPoint, null);
	}

	private static int parseIsPointZeroOrOneStrict(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		String t = raw.trim();
		if ("0".equals(t)) {
			return 0;
		}
		if ("1".equals(t)) {
			return 1;
		}
		return -1;
	}

	private record CategoryInfoQueryParsed(Optional<Long> itemIdOpt, int isPoint, Map<String, Object> errorBody) {
		static CategoryInfoQueryParsed validationError(Map<String, List<String>> errors) {
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("message", "参数错误.");
			inner.put("errors", errors);
			inner.put("status_code", 422);
			return new CategoryInfoQueryParsed(Optional.empty(), 0, Map.of("data", inner));
		}
	}

	@Activated(routeAlias = "goods.category.lists")
	@GetMapping(value = "/category", name = "分类列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getCategory(HttpServletRequest request,
			@RequestParam(name = "is_main_category", required = false) String isMainCategory,
			@RequestParam(name = "category_level", required = false) String categoryLevel,
			@RequestParam(name = "parent_id", required = false) String parentId,
			@RequestParam(name = "distributor_id", required = false) Long distributorId,
			@RequestParam(name = "is_show", required = false, defaultValue = "true") String isShow,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long jwtDistributorId = readDistributorId(ud);
		boolean isShowBool = !"false".equals(isShow);
		boolean ignoreNone = parseIgnoreNone(request);
		List<Map<String, Object>> data = itemsCategoryQueryService.getCategory(companyId, isMainCategory, categoryLevel,
				parentId, distributorId, isShowBool, ignoreNone, countryCode, jwtDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static boolean parseIgnoreNone(HttpServletRequest request) {
		String raw = request.getParameter("ignore_none");
		if (raw == null || !StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		if ("1".equals(t)) {
			return true;
		}
		if ("true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t)) {
			return true;
		}
		try {
			return Integer.parseInt(t) != 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Activated(routeAlias = "goods.category.update")
	@PutMapping(value = "/category/{category_id}", name = "更新分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCategory(HttpServletRequest request,
			@PathVariable("category_id") String categoryIdRaw,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long categoryId = parseCategoryIdPathForUpdate(categoryIdRaw);
		Map<String, Object> payload = body != null ? body : Map.of();
		String countryCode = resolveCountryCode(countryCodeParam, payload);
		Map<String, Object> data = itemsCategoryUpdateService.updateCategory(companyId, categoryId, payload, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "goods.category.delete")
	@DeleteMapping(value = "/category/{category_id}", name = "删除分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteCategory(HttpServletRequest request,
			@PathVariable("category_id") String categoryIdRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long categoryId = parseCategoryIdPath(categoryIdRaw, "删除分类出错.");
		itemsCategoryDeleteService.deleteItemsCategory(companyId, categoryId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long parseCategoryIdPathForUpdate(String categoryIdRaw) {
		if (!StringUtils.hasText(categoryIdRaw)) {
			throw new ResourceException("更新的分类不存在");
		}
		String t = categoryIdRaw.trim();
		try {
			long categoryId = Long.parseLong(t);
			if (categoryId < 1L) {
				throw new ResourceException("更新的分类不存在");
			}
			return categoryId;
		} catch (NumberFormatException e) {
			throw new ResourceException("更新的分类不存在");
		}
	}

	private static long parseCategoryIdPath(String categoryIdRaw, String msg) {
		if (!StringUtils.hasText(categoryIdRaw)) {
			throw new BadRequestException(msg);
		}
		String t = categoryIdRaw.trim();
		long categoryId;
		try {
			categoryId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(msg);
		}
		if (categoryId < 1L) {
			throw new BadRequestException(msg);
		}
		return categoryId;
	}

	@Activated(routeAlias = "goods.category.create")
	@PostMapping(value = "/category", name = "添加分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> createCategory(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object formObj = body != null ? body.get("form") : null;
		if (!(formObj instanceof String formStr)) {
			throw new BadRequestException("form 须为 JSON 字符串");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(formStr);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("form JSON 无法解析");
		}
		if (root == null) {
			throw new BadRequestException("form 须为分类节点数组");
		}
		if (!root.isArray()) {
			throw new BadRequestException("分类名称必填");
		}
		List<CategoryTreeNode> nodes = CategoryTreeNode.fromJsonArray(root);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long distributorId = readDistributorId(ud);

		itemsCategorySaveService.saveItemsCategory(companyId, distributorId, nodes);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.createcategory.create")
	@PostMapping(value = "/createcategory", name = "平面创建分类")
	public ResponseEntity<Map<String, Object>> createClassification(HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readJwtLongOrUnauthorized(ud, "company_id");
		long jwtDistributorId = readDistributorId(ud);
		Map<String, Object> p = body != null ? body : Map.of();
		validateCreateClassificationBody(p);
		LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(p);
		normalizeCustomizePageId(normalized);
		String countryCode = resolveCountryCode(countryCodeParam, normalized);
		itemsCategoryClassificationService.createClassification(companyId, jwtDistributorId, normalized, countryCode);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	private static void validateCreateClassificationBody(Map<String, Object> body) {
		Object nameObj = body.get("category_name");
		if (nameObj == null || !StringUtils.hasText(nameObj.toString().trim())) {
			throw new ResourceException("分类名称必填");
		}
		if (!body.containsKey("sort") || body.get("sort") == null) {
			throw new ResourceException("排序必须大于等于0");
		}
		long sort = parseNonNegativeLongStrict(body.get("sort"), "排序必须大于等于0");
		if (sort < 0L) {
			throw new ResourceException("排序必须大于等于0");
		}
		if (body.containsKey("parent_id")) {
			parseNonNegativeLongStrict(body.get("parent_id"), "父级ID必须大于等于0");
		}
	}

	private static void normalizeCustomizePageId(Map<String, Object> body) {
		if (!body.containsKey("customize_page_id")) {
			return;
		}
		Object v = body.get("customize_page_id");
		if (v == null) {
			body.put("customize_page_id", 0L);
			return;
		}
		if (v instanceof String s && !StringUtils.hasText(s)) {
			body.put("customize_page_id", 0L);
		}
	}

	private static long parseNonNegativeLongStrict(Object v, String err) {
		if (v == null) {
			throw new ResourceException(err);
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(err);
		}
	}

	/** Write path: body {@code country_code} → query → Accept-Language / default. */
	private String resolveCountryCode(String queryParam, Map<String, Object> body) {
		return RequestCountryCode.resolve(langueProperties, queryParam, body);
	}

	private static long readJwtLongOrUnauthorized(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}

	private static long readDistributorId(Map<?, ?> ud) {
		Object v = ud.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}
}
