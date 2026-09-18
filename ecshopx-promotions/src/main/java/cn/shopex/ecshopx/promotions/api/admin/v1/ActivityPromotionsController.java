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
import cn.shopex.ecshopx.promotions.service.ActiveArticlesService;
import cn.shopex.ecshopx.promotions.service.LiveRoomListService;
import cn.shopex.ecshopx.promotions.service.PromotionActivityCreateService;
import cn.shopex.ecshopx.promotions.service.PromotionActivityListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.i18n.LocaleContextHolder;
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
@RestController("promotionsAdminV1ActivityPromotions")
@RequestMapping("/api/v1/promotions")
public class ActivityPromotionsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final PromotionActivityCreateService promotionActivityCreateService;
	private final PromotionActivityListService promotionActivityListService;
	private final ActiveArticlesService activeArticlesService;
	private final LiveRoomListService liveRoomListService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public ActivityPromotionsController(
			PromotionActivityCreateService promotionActivityCreateService,
			PromotionActivityListService promotionActivityListService,
			ActiveArticlesService activeArticlesService,
			LiveRoomListService liveRoomListService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.promotionActivityCreateService = promotionActivityCreateService;
		this.promotionActivityListService = promotionActivityListService;
		this.activeArticlesService = activeArticlesService;
		this.liveRoomListService = liveRoomListService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.activity.validNum")
	@PostMapping(value = "/activity/validNum", name = "活动有效数量")
	public ResponseEntity<ApiResult<Boolean>> checkActiveValidNum(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Object raw = merged.get("activity_type");
		String activityTypeRaw = raw == null ? null : String.valueOf(raw);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		promotionActivityCreateService.checkActiveValidNum(companyId, activityTypeRaw);
		return ResponseEntity.ok(ApiResult.ok(Boolean.TRUE));
	}

	@Activated(routeAlias = "promotions.activity.invalid")
	@PutMapping(value = "/activity/invalid", name = "活动失效")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateStatusInvalid(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String activityIdRaw =
				merged.get("activity_id") == null ? "" : String.valueOf(merged.get("activity_id")).trim();
		Map<String, Object> row = promotionActivityCreateService.updateStatusInvalid(companyId, activityIdRaw);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "promotions.activity.create")
	@PostMapping(value = "/activity/create", name = "创建自动化营销")
	public ResponseEntity<ApiResult<Map<String, Object>>> createActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String countryCodeTrimmed = java.util.Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		String langTag =
				org.springframework.util.StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
		Map<String, Object> row = promotionActivityCreateService.createActivity(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
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

	@Activated(routeAlias = "promotions.activity.lists")
	@GetMapping(value = "/activity/lists", name = "自动化营销列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityList(
			HttpServletRequest request,
			@RequestParam(value = "activity_status", required = false) String activityStatus,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "country_code", required = false) String countryCodeRaw) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> data =
				promotionActivityListService.getActivityList(
						companyId, activityStatus, pageRaw, pageSizeRaw, countryCodeRaw, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.article.save")
	@PostMapping(value = "/activearticle", name = "活动文章保存")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveActiveArticle(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> result = activeArticlesService.saveActiveArticle(merged, locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.article.list")
	@GetMapping(value = "/activearticle/list", name = "活动文章列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActiveArticleList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw,
			@RequestParam(value = "article_title", required = false) String articleTitle,
			@RequestParam(value = "article_subtitle", required = false) String articleSubtitle,
			@RequestParam(value = "article_content", required = false) String articleContent,
			@RequestParam(value = "update_start", required = false) String updateStart,
			@RequestParam(value = "update_end", required = false) String updateEnd) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> data =
				activeArticlesService.getActiveArticleList(
						companyId,
						pageRaw,
						pageSizeRaw,
						articleTitle,
						articleSubtitle,
						articleContent,
						updateStart,
						updateEnd,
						locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.article.detail")
	@GetMapping(value = "/activearticle/{id}", name = "活动文章详情")
	public ResponseEntity<ApiResult<Object>> getActiveArticleDetail(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Optional<Map<String, Object>> row = activeArticlesService.getActiveArticleDetail(companyId, id);
		if (row.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(row.get()));
	}

	@Activated(routeAlias = "promotions.article.update")
	@PutMapping(value = "/activearticle", name = "活动文章更新")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateActiveArticle(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> result = activeArticlesService.updateActiveArticle(merged, locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.article.delete")
	@DeleteMapping(value = "/activearticle/{id}", name = "活动文章删除")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteActiveArticle(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> result = activeArticlesService.deleteActiveArticle(companyId, id, locale);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "promotions.liverooms")
	@GetMapping(value = "/liverooms", name = "直播回放列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLiveRooms(
			HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateName,
			@RequestParam(value = "wxapp_id", required = false) String wxappId,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "roomid", required = false) String roomid,
			@RequestParam(value = "action", required = false) String action) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> data =
				liveRoomListService.getLiveRooms(
						companyId, templateName, wxappId, page, pageSize, roomid, action, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
