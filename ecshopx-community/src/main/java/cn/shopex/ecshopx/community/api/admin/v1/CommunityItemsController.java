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

package cn.shopex.ecshopx.community.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.community.dto.CommunityItemsListQuery;
import cn.shopex.ecshopx.community.service.CommunityItemsCreateService;
import cn.shopex.ecshopx.community.service.CommunityItemsListService;
import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
		notFound = false
)
@AdminAuth
@ShopLog
@RestController("communityAdminV1Items")
@RequestMapping("/api/v1/community")
public class CommunityItemsController {

	private final CommunityItemsCreateService communityItemsCreateService;
	private final CommunityItemsListService communityItemsListService;

	public CommunityItemsController(
			CommunityItemsCreateService communityItemsCreateService,
			CommunityItemsListService communityItemsListService) {
		this.communityItemsCreateService = communityItemsCreateService;
		this.communityItemsListService = communityItemsListService;
	}

	@Activated(routeAlias = "community.items.list.get")
	@GetMapping(value = "/items", name = "商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "keywords", required = false) String keywords,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "barcode", required = false) String barcode,
			@RequestParam(value = "approve_status", required = false) String approveStatus,
			@RequestParam(value = "brand_id", required = false) String brandId,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "distributor_id", required = false) Integer distributorId,
			@RequestParam(value = "in_activity", required = false) String inActivityRaw,
			@RequestParam(value = "activit_id", required = false) Long activitId,
			@RequestParam(value = "activity_id", required = false) Long activityId) {
		Map<String, List<String>> paginationErrors = new LinkedHashMap<>();
		if (page == null) {
			paginationErrors.put("page", List.of("validation.required"));
		}
		if (pageSize == null) {
			paginationErrors.put("pageSize", List.of("validation.required"));
		}
		if (!paginationErrors.isEmpty()) {
			throw new BadRequestException("获取商品列表出错.", paginationErrors, 422);
		}
		if (page < 1) {
			throw new BadRequestException(
					"获取商品列表出错.", Map.of("page", List.of("validation.min.numeric")), 422);
		}

		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		String operatorType = stringVal(ud.get("operator_type"));
		int distributorIdVal = 0;
		if ("distributor".equals(operatorType)) {
			Map<String, Object> queryMerged = new LinkedHashMap<>();
			if (distributorId != null) {
				queryMerged.put("distributor_id", distributorId);
			}
			distributorIdVal = resolveDistributorIdForCreate(request, queryMerged, ud);
		}

		boolean inActivityKeyPresent = request.getParameterMap().containsKey("in_activity");
		boolean inActivity = "true".equals(inActivityRaw);

		Long normalizedActivityId = null;
		if (activityId != null && activityId > 0) {
			normalizedActivityId = activityId;
		} else if (activitId != null && activitId > 0) {
			normalizedActivityId = activitId;
		}

		CommunityItemsListQuery query = CommunityItemsListQuery.builder()
				.companyId(companyId)
				.operatorType(operatorType)
				.distributorId(distributorIdVal)
				.keywords(keywords)
				.itemName(itemName)
				.itemBn(itemBn)
				.barcode(barcode)
				.approveStatus(approveStatus)
				.brandIdRaw(brandId)
				.categoryRaw(category)
				.inActivityParameterPresent(inActivityKeyPresent)
				.inActivity(inActivity)
				.normalizedActivityId(normalizedActivityId)
				.page(page.intValue())
				.pageSize(pageSize.intValue())
				.build();

		PageResult<Map<String, Object>> pr = communityItemsListService.getItemsList(query);
		Map<String, Object> data = Map.of("total_count", pr.getTotal(), "list", pr.getList());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.items.add")
	@PostMapping(value = "/items", name = "添加商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> createItems(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String operatorType = stringVal(ud.get("operator_type"));
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			distributorId = resolveDistributorIdForCreate(request, merged, ud);
		}

		List<Long> goodsIds = normalizeGoodsIds(merged.get("goods_id"));
		if (goodsIds.isEmpty()) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("goods_id", List.of("validation.required")));
		}

		communityItemsCreateService.batchInsert(companyId, distributorId, goodsIds);
		Map<String, Object> data = Map.of("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static List<Long> normalizeGoodsIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				Long id = toPositiveLong(o);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<Long> out = new ArrayList<>();
			for (Object o : arr) {
				Long id = toPositiveLong(o);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? List.of(v) : List.of();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return List.of();
			}
			List<Long> out = new ArrayList<>();
			for (String part : t.split(",")) {
				String p = part.trim();
				if (!StringUtils.hasText(p)) {
					continue;
				}
				Long id = toPositiveLong(p);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		return List.of();
	}

	private static Long toPositiveLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		try {
			long v = Long.parseLong(o.toString().trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseCompanyId(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static int resolveDistributorIdForCreate(
			HttpServletRequest request, Map<String, Object> merged, Map<?, ?> ud) {
		Object d = merged.get("distributor_id");
		if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
			d = request.getParameter("distributor_id");
		}
		if (d == null) {
			Object attr = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
			if (attr != null) {
				d = attr;
			}
		}
		if (d == null && ud != null) {
			Object jwtDid = ud.get("distributor_id");
			if (jwtDid != null) {
				d = jwtDid;
			}
		}
		return parseIntLoose(d, 0);
	}

	private static int parseIntLoose(Object v, int defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static boolean isMinDeliveryNumInputPresent(Map<String, Object> merged) {
		if (!merged.containsKey("min_delivery_num")) {
			return false;
		}
		Object v = merged.get("min_delivery_num");
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			return StringUtils.hasText(s.trim());
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		return true;
	}

	private static boolean isSortInputPresent(Map<String, Object> merged) {
		if (!merged.containsKey("sort")) {
			return false;
		}
		Object v = merged.get("sort");
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			return StringUtils.hasText(s.trim());
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		return true;
	}

	private static int parseMinDeliveryNumForStorage(Object raw) {
		Object v = unwrapMinDeliveryNumValue(raw);
		Double parsed = parseLooseDouble(v);
		if (parsed == null || !Double.isFinite(parsed)) {
			return 0;
		}
		double d = parsed;
		if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("min_delivery_num", List.of("validation.numeric")));
		}
		if (d > 0) {
			return (int) Math.floor(d);
		}
		return 0;
	}

	private static int parseSortForStorage(Object raw) {
		Object v = unwrapMinDeliveryNumValue(raw);
		Double parsed = parseLooseDouble(v);
		if (parsed == null || !Double.isFinite(parsed)) {
			return 0;
		}
		double d = parsed;
		if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("sort", List.of("validation.numeric")));
		}
		if (d > 0) {
			return (int) Math.floor(d);
		}
		return 0;
	}

	private static Object unwrapMinDeliveryNumValue(Object raw) {
		if (raw instanceof Collection<?> c && !c.isEmpty()) {
			return c.iterator().next();
		}
		if (raw instanceof Object[] arr && arr.length > 0) {
			return arr[0];
		}
		return raw;
	}

	private static Double parseLooseDouble(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			try {
				return Double.parseDouble(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	@Activated(routeAlias = "community.item.minDeliveryNum.update")
	@PostMapping(value = "/itemMinDeliveryNum", name = "修改商品起送量")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateItemMinDeliveryNum(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String operatorType = stringVal(ud.get("operator_type"));
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			distributorId = resolveDistributorIdForCreate(request, merged, ud);
		}

		List<Long> goodsIds = normalizeGoodsIds(merged.get("goods_id"));
		if (goodsIds.isEmpty()) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("goods_id", List.of("validation.required")));
		}

		if (!isMinDeliveryNumInputPresent(merged)) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("min_delivery_num", List.of("validation.required")));
		}

		int minDeliveryNum = parseMinDeliveryNumForStorage(merged.get("min_delivery_num"));

		communityItemsCreateService.updateMinDeliveryNumByFilter(companyId, distributorId, goodsIds, minDeliveryNum);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "community.item.sort.update")
	@PostMapping(value = "/itemSort", name = "修改商品排序编号")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateItemSort(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String operatorType = stringVal(ud.get("operator_type"));
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			distributorId = resolveDistributorIdForCreate(request, merged, ud);
		}

		List<Long> goodsIds = normalizeGoodsIds(merged.get("goods_id"));
		if (goodsIds.isEmpty()) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("goods_id", List.of("validation.required")));
		}

		if (!isSortInputPresent(merged)) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("sort", List.of("validation.required")));
		}

		int sortValue = parseSortForStorage(merged.get("sort"));

		communityItemsCreateService.updateSortByFilter(companyId, distributorId, goodsIds, sortValue);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "community.item.delete")
	@DeleteMapping(value = "/item/{goods_id}", name = "删除商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteItem(
			HttpServletRequest request,
			@PathVariable("goods_id") String goodsId,
			@RequestParam(value = "distributor_id", required = false) Integer distributorIdParam) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		String operatorType = stringVal(ud.get("operator_type"));
		int distributorIdVal = 0;
		if ("distributor".equals(operatorType)) {
			Map<String, Object> queryMerged = new LinkedHashMap<>();
			if (distributorIdParam != null) {
				queryMerged.put("distributor_id", distributorIdParam);
			}
			distributorIdVal = resolveDistributorIdForCreate(request, queryMerged, ud);
		}

		Long parsedGoods = toPositiveLong(goodsId);
		long goodsIdVal = parsedGoods != null ? parsedGoods : 0L;

		communityItemsListService.assertNotInActiveActivityForDelete(companyId, distributorIdVal, goodsIdVal);
		communityItemsCreateService.deleteByGoodsFilter(companyId, distributorIdVal, goodsIdVal);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
