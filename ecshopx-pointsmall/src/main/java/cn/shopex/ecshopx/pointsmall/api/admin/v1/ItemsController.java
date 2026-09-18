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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminDetailAttributeEnricher;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminDetailService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminListService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsBatchStatusUpdateService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsBatchStoreUpdateService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsCreateService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsDeleteService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsRelCatsWriteService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsSortUpdateService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsTemplateWriteService;
import cn.shopex.ecshopx.pointsmall.validation.PointsmallItemsCreateParamValidator;
import cn.shopex.ecshopx.pointsmall.web.PointsmallAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("pointsmallItemsAdminV1")
@RequestMapping("/api/v1/pointsmall")
public class ItemsController {

	private final PointsmallItemsCreateParamValidator pointsmallItemsCreateParamValidator;
	private final PointsmallItemsCreateService pointsmallItemsCreateService;
	private final PointsmallItemsRelCatsWriteService pointsmallItemsRelCatsWriteService;
	private final PointsmallItemsSortUpdateService pointsmallItemsSortUpdateService;
	private final PointsmallItemsTemplateWriteService pointsmallItemsTemplateWriteService;
	private final PointsmallItemsBatchStatusUpdateService pointsmallItemsBatchStatusUpdateService;
	private final PointsmallItemsBatchStoreUpdateService pointsmallItemsBatchStoreUpdateService;
	private final PointsmallItemsAdminListService pointsmallItemsAdminListService;
	private final PointsmallItemsAdminDetailService pointsmallItemsAdminDetailService;
	private final PointsmallItemsAdminDetailAttributeEnricher pointsmallItemsAdminDetailAttributeEnricher;
	private final PointsmallItemsDeleteService pointsmallItemsDeleteService;

	public ItemsController(
			PointsmallItemsCreateParamValidator pointsmallItemsCreateParamValidator,
			PointsmallItemsCreateService pointsmallItemsCreateService,
			PointsmallItemsRelCatsWriteService pointsmallItemsRelCatsWriteService,
			PointsmallItemsSortUpdateService pointsmallItemsSortUpdateService,
			PointsmallItemsTemplateWriteService pointsmallItemsTemplateWriteService,
			PointsmallItemsBatchStatusUpdateService pointsmallItemsBatchStatusUpdateService,
			PointsmallItemsBatchStoreUpdateService pointsmallItemsBatchStoreUpdateService,
			PointsmallItemsAdminListService pointsmallItemsAdminListService,
			PointsmallItemsAdminDetailService pointsmallItemsAdminDetailService,
			PointsmallItemsAdminDetailAttributeEnricher pointsmallItemsAdminDetailAttributeEnricher,
			PointsmallItemsDeleteService pointsmallItemsDeleteService) {
		this.pointsmallItemsCreateParamValidator = pointsmallItemsCreateParamValidator;
		this.pointsmallItemsCreateService = pointsmallItemsCreateService;
		this.pointsmallItemsRelCatsWriteService = pointsmallItemsRelCatsWriteService;
		this.pointsmallItemsSortUpdateService = pointsmallItemsSortUpdateService;
		this.pointsmallItemsTemplateWriteService = pointsmallItemsTemplateWriteService;
		this.pointsmallItemsBatchStatusUpdateService = pointsmallItemsBatchStatusUpdateService;
		this.pointsmallItemsBatchStoreUpdateService = pointsmallItemsBatchStoreUpdateService;
		this.pointsmallItemsAdminListService = pointsmallItemsAdminListService;
		this.pointsmallItemsAdminDetailService = pointsmallItemsAdminDetailService;
		this.pointsmallItemsAdminDetailAttributeEnricher = pointsmallItemsAdminDetailAttributeEnricher;
		this.pointsmallItemsDeleteService = pointsmallItemsDeleteService;
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	private static ResponseEntity<?> dingo422(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	@Activated(routeAlias = "pointsmall.goods.items.create")
	@PostMapping(value = "/goods/items", name = "添加商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> createItems(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		if (!merged.containsKey("origincountry_id") || merged.get("origincountry_id") == null) {
			merged.put("origincountry_id", 0);
		}
		merged.put("company_id", companyId);
		if (ud.get("operator_type") != null) {
			merged.put("operator_type", ud.get("operator_type"));
		}
		merged.remove("item_id");

		pointsmallItemsCreateParamValidator.validate(merged);
		pointsmallItemsCreateService.addItems(merged);
		return ApiResult.ok(Map.of("status", true));
	}

	@Activated(routeAlias = "pointsmall.goods.items.templates_change")
	@PostMapping(value = "/goods/setItemsTemplate", name = "更新商品运费模板", produces = MediaType.APPLICATION_JSON_VALUE)
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemsTemplate(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");
		int templatesId = parseRequiredTemplatesIdForPointsmallSetItemsTemplate(merged.get("templates_id"));
		List<Long> itemIds = parseRequiredItemIdsForPointsmallSetItemsTemplate(merged.get("item_id"));
		pointsmallItemsTemplateWriteService.setItemsTemplate(companyId, templatesId, itemIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static int parseRequiredTemplatesIdForPointsmallSetItemsTemplate(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择运费模板");
		}
		if (raw instanceof Number n) {
			long l = n.longValue();
			if (l < 1L || l > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("请选择运费模板");
			}
			return (int) l;
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择运费模板");
			}
			long l;
			try {
				l = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择运费模板");
			}
			if (l < 1L || l > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("请选择运费模板");
			}
			return (int) l;
		}
		throw new BadRequestException("请选择运费模板");
	}

	private static List<Long> parseRequiredItemIdsForPointsmallSetItemsTemplate(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择商品");
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择商品");
			}
			try {
				return List.of(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择商品");
			}
		}
		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("请选择商品");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : col) {
				if (el instanceof Number num) {
					out.add(num.longValue());
				} else if (el instanceof String es) {
					if (!StringUtils.hasText(es.trim())) {
						throw new BadRequestException("请选择商品");
					}
					try {
						out.add(Long.parseLong(es.trim()));
					} catch (NumberFormatException e) {
						throw new BadRequestException("请选择商品");
					}
				} else {
					throw new BadRequestException("请选择商品");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择商品");
			}
			return out;
		}
		throw new BadRequestException("请选择商品");
	}

	@Activated(routeAlias = "pointsmall.goods.items.category_change")
	@PostMapping(value = "/goods/setItemsCategory", name = "更新商品分类", produces = MediaType.APPLICATION_JSON_VALUE)
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemsCategory(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");
		List<Long> itemIds = parseRequiredItemOrCategoryIdsForSetItemsCategory(merged.get("item_id"));
		List<Long> categoryIds = parseRequiredItemOrCategoryIdsForSetItemsCategory(merged.get("category_id"));
		pointsmallItemsRelCatsWriteService.setItemsCategoryTransactional(companyId, itemIds, categoryIds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static List<Long> parseRequiredItemOrCategoryIdsForSetItemsCategory(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择商品和分类的数据");
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
			try {
				return List.of(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
		}
		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : col) {
				if (el instanceof Number num) {
					out.add(num.longValue());
				} else if (el instanceof String es) {
					if (!StringUtils.hasText(es.trim())) {
						throw new BadRequestException("请选择商品和分类的数据");
					}
					try {
						out.add(Long.parseLong(es.trim()));
					} catch (NumberFormatException e) {
						throw new BadRequestException("请选择商品和分类的数据");
					}
				} else {
					throw new BadRequestException("请选择商品和分类的数据");
				}
			}
			if (out.isEmpty()) {
				throw new BadRequestException("请选择商品和分类的数据");
			}
			return out;
		}
		throw new BadRequestException("请选择商品和分类的数据");
	}

	@Activated(routeAlias = "pointsmall.goods.items.lists")
	@GetMapping(value = "/goods/items", name = "获取商品列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, List<String>> fieldErrors = validatePointsmallItemsListPageParams(request);
		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException("获取商品列表出错.", fieldErrors);
		}

		Map<String, Object> data = pointsmallItemsAdminListService.list(request, companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");
	private static final Pattern POSITIVE_INTEGER_STRING = Pattern.compile("^\\d+$");

	private static Map<String, List<String>> validatePointsmallItemsListPageParams(HttpServletRequest request) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		validatePointsmallListPage(request.getParameter("page"), fieldErrors);
		validatePointsmallListPageSize(request.getParameter("pageSize"), fieldErrors);
		return fieldErrors;
	}

	private static void validatePointsmallListPage(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("page", List.of("validation.required"));
			return;
		}
		String s = raw.trim();
		if (!POSITIVE_INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("page", List.of("validation.integer"));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("page", List.of("validation.integer"));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("page", List.of("validation.min.numeric"));
			return;
		}
		if (v > Integer.MAX_VALUE) {
			fieldErrors.put("page", List.of("validation.integer"));
		}
	}

	private static void validatePointsmallListPageSize(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("pageSize", List.of("validation.required"));
			return;
		}
		String s = raw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("pageSize", List.of("validation.integer"));
			return;
		}
		try {
			Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("pageSize", List.of("validation.integer"));
		}
	}

	@Activated(routeAlias = "pointsmall.goods.items.detail")
	@GetMapping(value = "/goods/items/{item_id}", name = "获取商品详情", produces = MediaType.APPLICATION_JSON_VALUE)
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsDetail(HttpServletRequest request,
			@PathVariable("item_id") String itemId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, List<String>> fieldErrors = validatePointsmallItemDetailId(itemId);
		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException("获取商品详情出错.", fieldErrors);
		}
		long itemIdLong = Long.parseLong(itemId.trim());
		String authorizerAppid = ud.get("authorizer_appid") != null ? ud.get("authorizer_appid").toString() : null;

		Map<String, Object> result = pointsmallItemsAdminDetailService.getDetail(itemIdLong, companyId, authorizerAppid);
		if (result.isEmpty()) {
			throw new ResourceException("获取商品信息有误，请确认商品ID.");
		}
		Object co = result.get("company_id");
		long rowCompany = co instanceof Number n ? n.longValue() : 0L;
		if (rowCompany != companyId) {
			throw new ResourceException("获取商品信息有误，请确认商品ID.");
		}

		pointsmallItemsAdminDetailAttributeEnricher.enrichAttributeLists(companyId, result);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static Map<String, List<String>> validatePointsmallItemDetailId(String raw) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("item_id", List.of("validation.required"));
			return fieldErrors;
		}
		String s = raw.trim();
		if (!POSITIVE_INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("item_id", List.of("validation.integer"));
			return fieldErrors;
		}
		try {
			long v = Long.parseLong(s);
			if (v < 1L) {
				fieldErrors.put("item_id", List.of("validation.min.numeric"));
			}
		} catch (NumberFormatException e) {
			fieldErrors.put("item_id", List.of("validation.integer"));
		}
		return fieldErrors;
	}

	@Activated(routeAlias = "pointsmall.goods.items.delete")
	@DeleteMapping(value = "/goods/items/{item_id}", name = "删除商品")
	public ResponseEntity<Void> deleteItems(HttpServletRequest request,
			@PathVariable("item_id") String itemId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, List<String>> fieldErrors = validatePointsmallItemDetailId(itemId);
		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException("删除商品出错.", fieldErrors);
		}
		long id = Long.parseLong(itemId.trim());
		pointsmallItemsDeleteService.deleteByItemId(companyId, id);
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "pointsmall.goods.items.update")
	@PutMapping(value = "/goods/items/{item_id}", name = "更新商品", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateItems(HttpServletRequest request,
			@PathVariable("item_id") String itemId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");

		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		merged.put("item_id", itemId);
		if (!merged.containsKey("origincountry_id") || merged.get("origincountry_id") == null) {
			merged.put("origincountry_id", 0);
		}
		merged.put("company_id", companyId);
		if (ud.get("operator_type") != null) {
			merged.put("operator_type", ud.get("operator_type"));
		}

		pointsmallItemsCreateParamValidator.validateForUpdate(merged);
		pointsmallItemsCreateService.addItems(merged);
		return ApiResult.ok(Map.of("status", true));
	}

	@Activated(routeAlias = "pointsmall.goods.items.sort")
	@PostMapping(value = "/goods/setItemsSort", name = "更新商品排序", produces = MediaType.APPLICATION_JSON_VALUE)
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	public ResponseEntity<ApiResult<Map<String, Object>>> setItemsSort(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");
		long itemId = readRequiredLongFromMerged(merged, "item_id");
		int sort = parseRequiredSort(merged.get("sort"));
		pointsmallItemsSortUpdateService.setItemsSort(companyId, itemId, sort);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long readRequiredLongFromMerged(Map<?, ?> merged, String key) {
		Object v = merged.get(key);
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

	private static int parseRequiredSort(Object sortRaw) {
		if (sortRaw == null) {
			throw new BadRequestException("请填写排序编号");
		}
		if (sortRaw instanceof Number n) {
			long sv = n.longValue();
			if (sv != (long) (int) sv || sv < 0) {
				throw new BadRequestException("请填写排序编号");
			}
			if (n instanceof Double d && d != sv) {
				throw new BadRequestException("请填写排序编号");
			}
			if (n instanceof Float f && f != sv) {
				throw new BadRequestException("请填写排序编号");
			}
			return (int) sv;
		}
		if (sortRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("请填写排序编号");
			}
			long v;
			try {
				v = Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("请填写排序编号");
			}
			if (v < 0 || v > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("请填写排序编号");
			}
			return (int) v;
		}
		throw new BadRequestException("请填写排序编号");
	}

	@Activated(routeAlias = "pointsmall.goods.store.upate")
	@PutMapping(value = "/goods/itemstoreupdate", name = "设置商品库存", produces = MediaType.APPLICATION_JSON_VALUE)
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchUpdateItemStore(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");
		pointsmallItemsBatchStoreUpdateService.updateFromMerged(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "pointsmall.goods.status.upate")
	@PutMapping(value = "/goods/itemstatusupdate", name = "设置商品状态", produces = MediaType.APPLICATION_JSON_VALUE)
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchUpdateItemStatus(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = new LinkedHashMap<>((Map<String, Object>) jwtMap);
		Map<String, Object> merged = PointsmallAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long companyId = PointsmallAdminRequestMerge.readRequiredLong(ud, "company_id");
		pointsmallItemsBatchStatusUpdateService.updateFromMerged(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
