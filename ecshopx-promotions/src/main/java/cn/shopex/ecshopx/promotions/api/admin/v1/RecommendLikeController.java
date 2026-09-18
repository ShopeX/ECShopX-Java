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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.RecommendLikeCreateService;
import cn.shopex.ecshopx.promotions.service.RecommendLikeDeleteService;
import cn.shopex.ecshopx.promotions.service.RecommendLikeItemsService;
import cn.shopex.ecshopx.promotions.service.RecommendLikeListService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RestController("promotionsAdminV1RecommendLike")
@RequestMapping("/api/v1/promotions")
public class RecommendLikeController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final RecommendLikeCreateService recommendLikeCreateService;
	private final RecommendLikeListService recommendLikeListService;
	private final RecommendLikeItemsService recommendLikeItemsService;
	private final RecommendLikeDeleteService recommendLikeDeleteService;
	private final LangueProperties langueProperties;

	public RecommendLikeController(
			RecommendLikeCreateService recommendLikeCreateService,
			RecommendLikeListService recommendLikeListService,
			RecommendLikeItemsService recommendLikeItemsService,
			RecommendLikeDeleteService recommendLikeDeleteService,
			LangueProperties langueProperties) {
		this.recommendLikeCreateService = recommendLikeCreateService;
		this.recommendLikeListService = recommendLikeListService;
		this.recommendLikeItemsService = recommendLikeItemsService;
		this.recommendLikeDeleteService = recommendLikeDeleteService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "recommendlike.list")
	@GetMapping(value = "/recommendlike", name = "猜你喜欢列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecommendLikeLists(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		int page = parseRecommendLikePage(request.getParameter("page"));
		int pageSize = parseRecommendLikePageSize(request.getParameter("pageSize"));
		long distributorId = optionalLong(request.getParameter("distributor_id"), 0L);
		Boolean isCanSale = resolveIsCanSaleFilter(request);
		String acceptLanguageHeader = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				recommendLikeListService.getRecommendLikeLists(
						companyId, page, pageSize, distributorId, isCanSale, 0L, acceptLanguageHeader);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "recommendlike.update")
	@PutMapping(value = "/recommendlike", name = "编辑猜你喜欢")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateRecommendLike(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Map<String, Object> result = recommendLikeCreateService.updateRecommendLike(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "recommendlike.list")
	@GetMapping(value = "/recommendlikes", name = "猜你喜欢商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRecommendLikeItems(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		boolean isAllFull = parseRecommendLikeIsAll(request.getParameter("is_all"));
		String acceptLanguageHeader = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				recommendLikeItemsService.getRecommendLikeItems(companyId, isAllFull, acceptLanguageHeader);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "recommendlike.save")
	@PostMapping(value = "/recommendlike", name = "添加猜你喜欢")
	public ResponseEntity<ApiResult<Map<String, Object>>> createRecommendLike(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);

		if (!merged.containsKey("items") || itemsCollectionIsEmpty(merged.get("items"))) {
			throw new ResourceException("请选择商品");
		}
		Object rawItems = merged.get("items");
		List<Map<String, Object>> items = tryNormalizeItemRows(rawItems);
		if (items == null) {
			throw new ResourceException("items必须是数组");
		}
		validateRecommendLikeItems(items);
		Map<String, Object> result = recommendLikeCreateService.createRecommendLike(companyId, items);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "recommendlike.delete")
	@DeleteMapping(value = "/recommendlike/{id}", name = "删除猜你喜欢")
	public ResponseEntity<ApiResult<Map<String, Object>>> delRecommendLike(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> data = recommendLikeDeleteService.delRecommendLike(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void validateRecommendLikeItems(List<Map<String, Object>> items) {
		for (Map<String, Object> m : items) {
			if (!m.containsKey("item_id")) {
				throw new ResourceException("商品id不能为空");
			}
			Long itemId = parseLong(m.get("item_id"));
			if (itemId == null) {
				throw new ResourceException("商品id不能为空");
			}
			if (!m.containsKey("sort")) {
				throw new ResourceException("排序不能为空");
			}
			Integer sort = parseIntegerAllowingZero(m.get("sort"));
			if (sort == null) {
				throw new ResourceException("排序格式错误");
			}
		}
	}

	private static boolean itemsCollectionIsEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?>) {
			return false;
		}
		if (v instanceof CharSequence cs) {
			return cs.length() == 0;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			if (n instanceof BigDecimal bd) {
				return bd.signum() == 0;
			}
			double d = n.doubleValue();
			return d == 0.0;
		}
		return false;
	}

	private static List<Map<String, Object>> tryNormalizeItemRows(Object raw) {
		if (raw instanceof List<?> list) {
			return normalizeListElements(list);
		}
		if (raw instanceof Collection<?> coll) {
			return normalizeListElements(new ArrayList<>(coll));
		}
		if (raw instanceof Object[] arr) {
			return normalizeListElements(Arrays.asList(arr));
		}
		return null;
	}

	private static List<Map<String, Object>> normalizeListElements(List<?> list) {
		List<Map<String, Object>> out = new ArrayList<>(list.size());
		for (Object e : list) {
			if (!(e instanceof Map<?, ?> m)) {
				throw new ResourceException("商品数据格式错误");
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : m.entrySet()) {
				Object k = en.getKey();
				if (k != null) {
					row.put(k.toString(), en.getValue());
				}
			}
			out.add(row);
		}
		return out;
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

	private static Long parseLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseIntegerAllowingZero(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long optionalLong(Object o, long default0) {
		if (o == null) {
			return default0;
		}
		Long p = parseLong(o);
		return p != null ? p : default0;
	}

	private static Boolean resolveIsCanSaleFilter(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("is_can_sale")) {
			return null;
		}
		if (ValuePresence.hasEffectiveValue(request.getParameter("is_can_sale"))) {
			return Boolean.TRUE;
		}
		return null;
	}

	private static int parseRecommendLikePage(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return 1;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			if (v < 1) {
				throw new BadRequestException("分页参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
	}

	private static int parseRecommendLikePageSize(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return 20;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
	}

	/**
	 * {@code is_all}：缺失或空为 false；{@code false}/{@code 0}（忽略大小写）为 false；{@code true}/{@code 1} 为 true；其余非空字符串为 true。
	 */
	private static boolean parseRecommendLikeIsAll(String raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return false;
		}
		String lower = s.toLowerCase();
		if ("false".equals(lower) || "0".equals(lower)) {
			return false;
		}
		return true;
	}
}
