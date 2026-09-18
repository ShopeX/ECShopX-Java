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
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendMainItemConflictException;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendRuleAdminService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("goodsAdminV1GoodsRecommendRule")
@RequestMapping("/api/v1/goods/recommend-rules")
public class GoodsRecommendRuleAdminController {

	private final GoodsRecommendRuleAdminService ruleAdminService;

	public GoodsRecommendRuleAdminController(GoodsRecommendRuleAdminService ruleAdminService) {
		this.ruleAdminService = ruleAdminService;
	}

	@Activated(routeAlias = "goods.recommend.rules.list")
	@GetMapping(name = "商品推荐规则列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> listRules(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestParam(value = "keyword", required = false) String keyword) {
		long companyId = readCompanyId(request);
		return ResponseEntity.ok(ApiResult.ok(ruleAdminService.listRules(companyId, page, pageSize, keyword)));
	}

	@Activated(routeAlias = "goods.recommend.rules.detail")
	@GetMapping(value = "/{id}", name = "商品推荐规则详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRule(
			HttpServletRequest request,
			@PathVariable("id") long id,
			@RequestParam(value = "main_page", defaultValue = "1") int mainPage,
			@RequestParam(value = "main_page_size", defaultValue = "20") int mainPageSize,
			@RequestParam(value = "recommend_page", defaultValue = "1") int recommendPage,
			@RequestParam(value = "recommend_page_size", defaultValue = "20") int recommendPageSize) {
		long companyId = readCompanyId(request);
		return ResponseEntity.ok(
				ApiResult.ok(
						ruleAdminService.getRuleDetail(
								companyId, id, mainPage, mainPageSize, recommendPage, recommendPageSize)));
	}

	@Activated(routeAlias = "goods.recommend.rules.create")
	@PostMapping(name = "新增商品推荐规则")
	public ResponseEntity<ApiResult<Map<String, Object>>> createRule(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		return ResponseEntity.ok(
				ApiResult.ok(ruleAdminService.saveRule(companyId, mergeInput(request, body), false)));
	}

	@Activated(routeAlias = "goods.recommend.rules.update")
	@PutMapping(name = "更新商品推荐规则")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateRule(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		Map<String, Object> input = mergeInput(request, body);
		if (!input.containsKey("id") && StringUtils.hasText(request.getParameter("id"))) {
			input.put("id", request.getParameter("id"));
		}
		return ResponseEntity.ok(ApiResult.ok(ruleAdminService.saveRule(companyId, input, true)));
	}

	@Activated(routeAlias = "goods.recommend.rules.delete")
	@DeleteMapping(value = "/{id}", name = "删除商品推荐规则")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteRule(
			HttpServletRequest request, @PathVariable("id") long id) {
		long companyId = readCompanyId(request);
		ruleAdminService.deleteRule(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(Map.of("id", id)));
	}

	@ExceptionHandler(GoodsRecommendMainItemConflictException.class)
	public ResponseEntity<?> handleConflict(GoodsRecommendMainItemConflictException ex) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		data.put("error_code", ex.getErrorCode());
		data.put("status_code", 409);
		data.put("conflicts", ex.getConflicts());
		return ResponseEntity.ok(Map.of("data", data));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		data.put("status_code", statusCode);
		if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
			data.put("errors", ex.getFieldErrors());
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v instanceof Number n) {
			return n.longValue();
		}
		throw new UnauthorizedException("未登录");
	}

	private Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>();
			request.getParameterMap().forEach((k, v) -> {
				if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
					input.put(k, v[0]);
				}
			});
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}
}
