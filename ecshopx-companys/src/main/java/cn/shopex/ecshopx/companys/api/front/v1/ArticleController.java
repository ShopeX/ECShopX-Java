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

package cn.shopex.ecshopx.companys.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.article.ArticleCategoryQueryService;
import cn.shopex.ecshopx.companys.service.article.ArticleDetailQueryService;
import cn.shopex.ecshopx.companys.service.article.ArticleListQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("companysFrontV1ArticleWxapp")
@RequestMapping("/api/v1/h5app/wxapp/article")
public class ArticleController {

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final ArticleCategoryQueryService articleCategoryQueryService;
	private final LangueProperties langueProperties;
	private final ArticleListQueryService articleListQueryService;
	private final ArticleDetailQueryService articleDetailQueryService;

	public ArticleController(
			ArticleCategoryQueryService articleCategoryQueryService,
			LangueProperties langueProperties,
			ArticleListQueryService articleListQueryService,
			ArticleDetailQueryService articleDetailQueryService) {
		this.articleCategoryQueryService = articleCategoryQueryService;
		this.langueProperties = langueProperties;
		this.articleListQueryService = articleListQueryService;
		this.articleDetailQueryService = articleDetailQueryService;
	}

	@GetMapping(value = "/focus/{article_id}", name = "文章关注")
	public ResponseEntity<ApiResult<Map<String, Object>>> articleFocus(
			HttpServletRequest request,
			@PathVariable("article_id") String articleIdRaw,
			@RequestParam(value = "company_id", required = false) String companyIdParam) {
		if (articleIdRaw == null) {
			throw new BadRequestException("获取关注信息错误!");
		}
		String t = articleIdRaw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			throw new BadRequestException("获取关注信息错误!");
		}
		long articleId;
		try {
			articleId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("获取关注信息错误!");
		}
		if (articleId < 1L) {
			throw new BadRequestException("获取关注信息错误!");
		}
		long companyId = resolveMergedCompanyId(request, companyIdParam);
		Map<String, Object> data = articleListQueryService.articleFocus(companyId, articleId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/focus/num/{article_id}", name = "文章关注数")
	public ResponseEntity<ApiResult<Map<String, Object>>> articleFocusNum(
			HttpServletRequest request,
			@PathVariable("article_id") String articleId,
			@RequestParam(value = "company_id", required = false) String companyIdParam) {
		long companyId = resolveMergedCompanyId(request, companyIdParam);
		Map<String, Object> data = articleListQueryService.articleFocusNum(companyId, articleId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/praise/num/{article_id}", name = "文章点赞数")
	public ResponseEntity<ApiResult<Map<String, Object>>> articlePraiseNum(
			HttpServletRequest request,
			@PathVariable("article_id") String articleId,
			@RequestParam(value = "company_id", required = false) String companyIdParam) {
		long companyId = resolveMergedCompanyId(request, companyIdParam);
		Map<String, Object> data = articleListQueryService.articlePraiseNum(companyId, articleId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/category", name = "文章栏目")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getCategory(
			HttpServletRequest request,
			@RequestParam(value = "category_type", required = false) String categoryType) {
		long companyId = requirePositiveH5CompanyId(request);
		String trimmedCategoryType = categoryType == null ? "" : categoryType.trim();
		String normalizedType = StringUtils.hasText(trimmedCategoryType) ? trimmedCategoryType : "bring";
		String requestLang = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> tree =
				articleCategoryQueryService.getCategory(companyId, normalizedType, requestLang, true);
		return ResponseEntity.ok(ApiResult.ok(tree));
	}

	private static Long parsePositiveCompanyIdOrNull(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? Long.valueOf(v) : null;
		}
		if (value instanceof String s) {
			return parsePositiveCompanyIdOrNull(s);
		}
		return null;
	}

	private static Long parsePositiveCompanyIdOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0 ? Long.valueOf(v) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long resolveMergedCompanyId(HttpServletRequest request, String companyIdQueryParam) {
		Object attr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		Long fromAuth = parsePositiveCompanyIdOrNull(attr);
		if (fromAuth != null) {
			return fromAuth.longValue();
		}
		Long fromParam = parsePositiveCompanyIdOrNull(companyIdQueryParam);
		if (fromParam != null) {
			return fromParam.longValue();
		}
		throw new BadRequestException("参数错误");
	}

	private static long resolveMergedUserId(HttpServletRequest request, String userIdQueryParam) {
		long fromClaims = parseH5UserIdForArticleList(request);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		Long fromParam = parsePositiveCompanyIdOrNull(userIdQueryParam);
		if (fromParam != null) {
			return fromParam.longValue();
		}
		throw new BadRequestException("参数错误");
	}

	private static long requirePositiveH5CompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		return companyId;
	}

	@GetMapping(value = "/province", name = "文章省份")
	public ResponseEntity<ApiResult<List<Object>>> getAllProvince(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		List<Object> data = articleListQueryService.getAllProvince(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/praise/{article_id}", name = "文章点赞")
	public ResponseEntity<ApiResult<Map<String, Object>>> articlePraise(
			HttpServletRequest request,
			@PathVariable("article_id") String articleIdRaw,
			@RequestParam(value = "company_id", required = false) String companyIdParam,
			@RequestParam(value = "user_id", required = false) String userIdParam) {
		if (articleIdRaw == null) {
			throw new BadRequestException("参数错误");
		}
		String t = articleIdRaw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			throw new BadRequestException("参数错误");
		}
		long articleId;
		try {
			articleId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
		if (articleId < 1L) {
			throw new BadRequestException("参数错误");
		}
		long companyId = resolveMergedCompanyId(request, companyIdParam);
		long userId = resolveMergedUserId(request, userIdParam);
		Map<String, Object> data = articleListQueryService.articlePraise(companyId, articleId, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/praise/check/{article_id}", name = "文章点赞校验")
	public ResponseEntity<ApiResult<Map<String, Object>>> articlePraiseCheck(
			HttpServletRequest request,
			@PathVariable("article_id") String articleIdRaw,
			@RequestParam(value = "company_id", required = false) String companyIdParam,
			@RequestParam(value = "user_id", required = false) String userIdParam) {
		if (articleIdRaw == null) {
			throw new BadRequestException("参数错误");
		}
		String articleIdPath = articleIdRaw.trim();
		if (articleIdPath.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		if ("0".equals(articleIdPath)) throw new BadRequestException("参数错误");
		long companyId = resolveMergedCompanyId(request, companyIdParam);
		long userId = resolveMergedUserId(request, userIdParam);
		Map<String, Object> data =
				articleDetailQueryService.articlePraiseCheck(companyId, articleIdPath, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/praises/getcountresult", name = "批量点赞状态")
	public ResponseEntity<ApiResult<Object>> getArticlePraiseData(
			HttpServletRequest request,
			@RequestParam(value = "article_ids", required = false) String[] articleIdsParam) {
		long companyId = requirePositiveH5CompanyId(request);
		long parsedUserId = parseH5UserIdForArticleList(request);
		boolean includeUserStatus = parsedUserId > 0L;
		long userId = includeUserStatus ? parsedUserId : 0L;
		List<String> ids = parseArticleIdsForPraiseBatch(articleIdsParam);
		if (ids.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		Map<String, Object> data =
				articleListQueryService.getArticlePraiseResult(
						companyId, ids, includeUserStatus, userId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/management", name = "获取文章列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> listDataArticle(
			HttpServletRequest request,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "article_id", required = false) String[] articleIdParams,
			@RequestParam(value = "article_type", required = false, defaultValue = "general") String articleType,
			@RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "category_id", required = false) String categoryIdRaw,
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "city", required = false) String city,
			@RequestParam(value = "area", required = false) String area) {
		return listDataArticleInternal(
				request,
				title,
				articleIdParams,
				articleType,
				pageSize,
				page,
				categoryIdRaw,
				province,
				city,
				area);
	}

	@FrontAuth
	@GetMapping(value = "/usermanagement", name = "获取文章列表（需登录）")
	public ResponseEntity<ApiResult<Map<String, Object>>> listDataArticleUserManagement(
			HttpServletRequest request,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "article_id", required = false) String[] articleIdParams,
			@RequestParam(value = "article_type", required = false, defaultValue = "general") String articleType,
			@RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "category_id", required = false) String categoryIdRaw,
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "city", required = false) String city,
			@RequestParam(value = "area", required = false) String area) {
		return listDataArticleInternal(
				request,
				title,
				articleIdParams,
				articleType,
				pageSize,
				page,
				categoryIdRaw,
				province,
				city,
				area);
	}

	private ResponseEntity<ApiResult<Map<String, Object>>> listDataArticleInternal(
			HttpServletRequest request,
			String title,
			String[] articleIdParams,
			String articleType,
			int pageSize,
			int page,
			String categoryIdRaw,
			String province,
			String city,
			String area) {
		long companyId = requirePositiveH5CompanyId(request);
		long userIdForPraise = parseH5UserIdForArticleList(request);
		boolean titleSearchPresent = title != null && !title.isBlank();
		String titleNeedle = titleSearchPresent ? title.trim() : "";
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
		Long categoryIdOrNull = null;
		if (categoryIdRaw != null && !categoryIdRaw.isBlank()) {
			try {
				categoryIdOrNull = Long.parseLong(categoryIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数错误");
			}
		}
		String provinceParam =
				province != null && StringUtils.hasText(province.trim()) ? province.trim() : null;
		String cityParam = city != null && StringUtils.hasText(city.trim()) ? city.trim() : null;
		String areaParam = area != null && StringUtils.hasText(area.trim()) ? area.trim() : null;
		int usePage = page <= 0 ? 1 : page;
		int usePageSize = pageSize <= 0 ? 100 : pageSize;
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				articleListQueryService.listDataArticle(
						companyId,
						userIdForPraise,
						titleSearchPresent,
						titleNeedle,
						requestedArticleIds,
						categoryIdOrNull,
						provinceParam,
						cityParam,
						areaParam,
						articleType,
						usePage,
						usePageSize,
						requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static List<String> parseArticleIdsForPraiseBatch(String[] articleIdsParam) {
		if (articleIdsParam == null || articleIdsParam.length == 0) {
			return Collections.emptyList();
		}
		List<String> ordered = new ArrayList<>();
		for (String s : articleIdsParam) {
			if (s == null) {
				continue;
			}
			String t = s.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				long parsedLong = Long.parseLong(t);
				ordered.add(String.valueOf(parsedLong));
			} catch (NumberFormatException ignored) {
				// skip invalid id
			}
		}
		if (ordered.isEmpty()) {
			return Collections.emptyList();
		}
		return ordered;
	}

	private static long parseH5UserIdForArticleList(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> claims)) {
			return 0L;
		}
		if (!claims.containsKey("user_id")) {
			return 0L;
		}
		Object v = claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@GetMapping(value = "/management/{article_id}", name = "获取文章详情")
	public ResponseEntity<ApiResult<Object>> infoDataArticle(
			HttpServletRequest request, @PathVariable("article_id") String articleId) {
		return infoDataArticleInternal(request, articleId);
	}

	@FrontAuth
	@GetMapping(value = "/usermanagement/{article_id}", name = "获取文章详情（需登录）")
	public ResponseEntity<ApiResult<Object>> infoDataArticleUserManagement(
			HttpServletRequest request, @PathVariable("article_id") String articleId) {
		return infoDataArticleInternal(request, articleId);
	}

	private static String resolveWoaAppidFromH5Claims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> claims)) {
			return "";
		}
		Object v = claims.get("woa_appid");
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static void validateH5ArticleIdPathOrThrow(String articleIdRaw) {
		if (articleIdRaw == null) {
			throw new ResourceException("种草不存在");
		}
		String t = articleIdRaw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			throw new ResourceException("种草不存在");
		}
		try {
			long id = Long.parseLong(t);
			if (id < 1L) {
				throw new ResourceException("种草不存在");
			}
		} catch (NumberFormatException e) {
			throw new ResourceException("种草不存在");
		}
	}

	private ResponseEntity<ApiResult<Object>> infoDataArticleInternal(
			HttpServletRequest request, String articleIdPath) {
		long companyId = requirePositiveH5CompanyId(request);
		validateH5ArticleIdPathOrThrow(articleIdPath);
		String woa = resolveWoaAppidFromH5Claims(request);
		long userId = parseH5UserIdForArticleList(request);
		String requestLang = RequestLangTag.current(langueProperties);
		Optional<Map<String, Object>> body =
				articleDetailQueryService.infoDataArticle(
						companyId, woa, articleIdPath, requestLang, userId);
		if (body.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(body.get()));
	}
}
