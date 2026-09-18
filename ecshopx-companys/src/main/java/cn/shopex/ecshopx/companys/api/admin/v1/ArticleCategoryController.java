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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.companys.service.article.ArticleCategoryDeleteService;
import cn.shopex.ecshopx.companys.service.article.ArticleCategoryQueryService;
import cn.shopex.ecshopx.companys.service.article.ArticleCategoryTreeSaveService;
import cn.shopex.ecshopx.companys.service.article.ArticleCategoryUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("companysAdminV1ArticleCategory")
@RequestMapping("/api/v1/article/category")
public class ArticleCategoryController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ArticleCategoryTreeSaveService articleCategoryTreeSaveService;
	private final ArticleCategoryUpdateService articleCategoryUpdateService;
	private final ArticleCategoryQueryService articleCategoryQueryService;
	private final ArticleCategoryDeleteService articleCategoryDeleteService;
	private final LangueProperties langueProperties;

	public ArticleCategoryController(
			ArticleCategoryTreeSaveService articleCategoryTreeSaveService,
			ArticleCategoryUpdateService articleCategoryUpdateService,
			ArticleCategoryQueryService articleCategoryQueryService,
			ArticleCategoryDeleteService articleCategoryDeleteService,
			LangueProperties langueProperties) {
		this.articleCategoryTreeSaveService = articleCategoryTreeSaveService;
		this.articleCategoryUpdateService = articleCategoryUpdateService;
		this.articleCategoryQueryService = articleCategoryQueryService;
		this.articleCategoryDeleteService = articleCategoryDeleteService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "article.create.category")
	@PostMapping(name = "创建文章栏目")
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		if (body == null || !body.containsKey("form") || body.get("form") == null) {
			throw new BadRequestException("请求体 form 无效或缺失");
		}
		articleCategoryTreeSaveService.saveArticleCategory(body.get("form"), companyId);
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

	@Activated(routeAlias = "article.list.category")
	@GetMapping(name = "获取文章栏目列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getCategory(
			HttpServletRequest request,
			@RequestParam(value = "category_type", required = false) String categoryType) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String raw = categoryType == null ? "" : categoryType.trim();
		String normalizedType = StringUtils.hasText(raw) ? raw : "bring";
		String requestLang = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> tree =
				articleCategoryQueryService.getCategory(companyId, normalizedType, requestLang, true);
		return ResponseEntity.ok(ApiResult.ok(tree));
	}

	@Activated(routeAlias = "article.info.category")
	@GetMapping(value = "/{category_id}", name = "获取单条文章栏目")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getCategory(
			HttpServletRequest request,
			@PathVariable("category_id") String categoryId,
			@RequestParam(value = "category_type", required = false) String categoryType) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String raw = categoryType == null ? "" : categoryType.trim();
		String normalizedType = StringUtils.hasText(raw) ? raw : "bring";
		String requestLang = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> tree =
				articleCategoryQueryService.getCategory(companyId, normalizedType, requestLang, true);
		return ResponseEntity.ok(ApiResult.ok(tree));
	}

	@Activated(routeAlias = "article.update.category")
	@PutMapping(value = "/{category_id}", name = "更新单条文章栏目")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCategory(
			HttpServletRequest request,
			@PathVariable("category_id") String categoryId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			merged.put(name, request.getParameter(name));
		}
		if (body != null) {
			merged.putAll(body);
		}
		String requestLang = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> data =
				articleCategoryUpdateService.updateCategory(categoryId, merged, companyId, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "article.delete.category")
	@DeleteMapping(value = "/{category_id}", name = "删除文章栏目")
	public ResponseEntity<Void> deleteCategory(
			HttpServletRequest request, @PathVariable("category_id") String categoryIdPath) {
		String p = categoryIdPath == null ? "" : categoryIdPath.trim();
		if (p.isEmpty()) {
			throw new BadRequestException(
					"删除分类出错", Map.of("category_id", List.of("validation.required")));
		}
		long v;
		try {
			v = Long.parseLong(p);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					"删除分类出错", Map.of("category_id", List.of("validation.integer")));
		}
		if (v < 1L) {
			throw new BadRequestException(
					"删除分类出错", Map.of("category_id", List.of("validation.min.numeric")));
		}
		long companyId = readCompanyIdFromOperatorJwt(request);
		articleCategoryDeleteService.deleteCategory(v, companyId);
		return ResponseEntity.ok().build();
	}
}
