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
import cn.shopex.ecshopx.companys.service.article.ArticleCreateService;
import cn.shopex.ecshopx.companys.service.article.ArticleDeleteService;
import cn.shopex.ecshopx.companys.service.article.ArticleDetailQueryService;
import cn.shopex.ecshopx.companys.service.article.ArticleListQueryService;
import cn.shopex.ecshopx.companys.service.article.ArticleUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
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
		notFound = false)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Article")
@RequestMapping("/api/v1/article")
public class ArticleController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ArticleCreateService articleCreateService;
	private final ArticleUpdateService articleUpdateService;
	private final ArticleListQueryService articleListQueryService;
	private final ArticleDetailQueryService articleDetailQueryService;
	private final ArticleDeleteService articleDeleteService;
	private final LangueProperties langueProperties;

	public ArticleController(
			ArticleCreateService articleCreateService,
			ArticleUpdateService articleUpdateService,
			ArticleListQueryService articleListQueryService,
			ArticleDetailQueryService articleDetailQueryService,
			ArticleDeleteService articleDeleteService,
			LangueProperties langueProperties) {
		this.articleCreateService = articleCreateService;
		this.articleUpdateService = articleUpdateService;
		this.articleListQueryService = articleListQueryService;
		this.articleDetailQueryService = articleDetailQueryService;
		this.articleDeleteService = articleDeleteService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "article.create")
	@PostMapping(value = "/management", name = "创建文章")
	public ResponseEntity<ApiResult<Map<String, Object>>> createDataArticle(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long operatorId = readOperatorIdFromOperatorJwt(request);
		String requestLang = RequestCountryCode.resolve(langueProperties, body);
		Map<String, Object> data = articleCreateService.create(body, companyId, operatorId, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	private static long readOperatorIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("operator_id");
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

	@Activated(routeAlias = "article.update")
	@PutMapping(value = "/management/{article_id}", name = "更新文章")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDataArticle(
			HttpServletRequest request,
			@PathVariable("article_id") String articleId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long operatorId = readOperatorIdFromOperatorJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
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
				articleUpdateService.updateDataArticle(articleId, merged, companyId, operatorId, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "article.delete")
	@DeleteMapping(value = "/management/{article_id}", name = "删除文章")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteDataArticle(
			HttpServletRequest request, @PathVariable("article_id") String articleId) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		articleDeleteService.deleteDataArticle(articleId, companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "article.list")
	@GetMapping(value = "/management", name = "获取文章列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> listDataArticle(
			HttpServletRequest request,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "article_id", required = false) String[] articleIdParams,
			@RequestParam(value = "release_status", required = false) String releaseStatusRaw,
			@RequestParam(value = "article_type", required = false, defaultValue = "general") String articleType,
			@RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String authorizerAppid = readAuthorizerAppidFromOperatorJwt(request);
		boolean titleSearchPresent = title != null && !title.isBlank();
		String titleNeedle = titleSearchPresent ? title.trim() : "";
		long distributorId = 0L;
		try {
			distributorId = Long.parseLong(distributorIdRaw == null ? "0" : distributorIdRaw.trim());
		} catch (NumberFormatException ignored) {
			distributorId = 0L;
		}
		List<Long> requestedArticleIds = null;
		if (articleIdParams != null && articleIdParams.length > 0) {
			requestedArticleIds = new ArrayList<>();
			for (String s : articleIdParams) {
				if (s == null) {
					continue;
				}
				String t = s.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					requestedArticleIds.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
					// skip invalid id
				}
			}
			if (requestedArticleIds.isEmpty()) {
				requestedArticleIds = null;
			}
		}
		boolean releaseStatusKeyPresent = request.getParameterMap().containsKey("release_status");
		int usePage = page <= 0 ? 1 : page;
		int usePageSize = pageSize <= 0 ? 100 : pageSize;
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> serviceResult =
				articleListQueryService.listDataArticle(
						companyId,
						authorizerAppid,
						titleSearchPresent,
						titleNeedle,
						distributorId,
						requestedArticleIds,
						releaseStatusKeyPresent,
						releaseStatusRaw,
						articleType,
						usePage,
						usePageSize,
						requestLang);
		return ResponseEntity.ok(ApiResult.ok(serviceResult));
	}

	private static String readAuthorizerAppidFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			return "";
		}
		Object v = ud.get("authorizer_appid");
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	@Activated(routeAlias = "article.info")
	@GetMapping(value = "/management/{article_id}", name = "获取文章详情")
	public ResponseEntity<ApiResult<Object>> infoDataArticle(
			HttpServletRequest request, @PathVariable("article_id") String articleIdPath) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String authorizerAppid = readAuthorizerAppidFromOperatorJwt(request);
		String requestLang = RequestLangTag.current(langueProperties);
		Optional<Map<String, Object>> body =
				articleDetailQueryService.infoDataArticle(
						companyId, authorizerAppid, articleIdPath, requestLang);
		if (body.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(body.get()));
	}

	@Activated(routeAlias = "article.update.sortstatus")
	@PutMapping(value = "/updatestatusorsort", name = "更新文章状态或排序")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateArticleStatusOrSort(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			merged.put(name, request.getParameter(name));
		}
		if (body != null) {
			merged.putAll(body);
		}
		Object raw = merged.get("inputdata");
		if (raw == null) {
			throw new BadRequestException("文章编辑参数有误");
		}
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("文章编辑参数有误");
		}
		List<Map<String, Object>> inputdata = new ArrayList<>();
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m)) {
				throw new BadRequestException("文章编辑参数有误");
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				Object key = e.getKey();
				if (key != null) {
					row.put(String.valueOf(key), e.getValue());
				}
			}
			inputdata.add(row);
		}
		articleUpdateService.updateArticleStatusOrSort(companyId, inputdata);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
