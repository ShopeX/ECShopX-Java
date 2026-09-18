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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.wechat.WechatCustomizePageCategoryBindFacade;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.wechat.service.CustomizePageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
@RestController("wechatAdminV1CustomizePage")
@RequestMapping("/api/v1/wxa")
public class CustomizePageController {

	private static final List<String> MERGE_KEYS = List.of(
			"template_name",
			"page_name",
			"page_description",
			"page_share_title",
			"page_share_desc",
			"page_share_imageUrl",
			"is_open",
			"page_type",
			"regionauth_id");

	private static final List<String> UPDATE_MERGE_KEYS = List.of(
			"template_name",
			"page_name",
			"page_description",
			"page_share_title",
			"page_share_desc",
			"page_share_imageUrl",
			"is_open",
			"regionauth_id");

	private final CustomizePageService customizePageService;
	private final WechatCustomizePageCategoryBindFacade customizePageCategoryBindFacade;
	private final LangueProperties langueProperties;

	public CustomizePageController(CustomizePageService customizePageService,
			WechatCustomizePageCategoryBindFacade customizePageCategoryBindFacade, LangueProperties langueProperties) {
		this.customizePageService = customizePageService;
		this.customizePageCategoryBindFacade = customizePageCategoryBindFacade;
		this.langueProperties = langueProperties;
	}

	@PostMapping(value = "/customizepage", name = "增加自定义页面", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createCustomizePage(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (String k : MERGE_KEYS) {
			if (body != null && body.containsKey(k)) {
				merged.put(k, body.get(k));
			} else if (request.getParameterMap().containsKey(k)) {
				merged.put(k, request.getParameter(k));
			}
		}

		long companyId = readCompanyIdFromOperatorJwt(request);

		CustomizePageService.CustomizePageCreateOutcome outcome =
				customizePageService.createCustomizePage(companyId, merged);

		if (outcome.duplicateMy()) {
			LinkedHashMap<String, Object> dup = new LinkedHashMap<>();
			dup.put("status", Boolean.FALSE);
			dup.put("message", "已经有启用的模版");
			dup.put("status_code", 422);
			return ResponseEntity.ok(ApiResult.ok(dup));
		}
		return ResponseEntity.ok(ApiResult.ok(outcome.successData()));
	}

	@PutMapping(value = "/customizepage/{id}", name = "更新自定义页面", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateCustomizePage(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long pageId = parseUpdateCustomizePageIdFromPath(id);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (String k : UPDATE_MERGE_KEYS) {
			if (body != null && body.containsKey(k)) {
				merged.put(k, body.get(k));
			} else if (request.getParameterMap().containsKey(k)) {
				merged.put(k, request.getParameter(k));
			}
		}

		Object pageTypeRaw = null;
		if (body != null && body.containsKey("page_type")) {
			pageTypeRaw = body.get("page_type");
		} else if (request.getParameterMap().containsKey("page_type")) {
			pageTypeRaw = request.getParameter("page_type");
		}

		long companyId = readCompanyIdFromOperatorJwt(request);
		CustomizePageService.CustomizePageUpdateOutcome outcome =
				customizePageService.updateCustomizePage(companyId, pageId, pageTypeRaw, merged);

		if (outcome.duplicateMy()) {
			LinkedHashMap<String, Object> dup = new LinkedHashMap<>();
			dup.put("status", Boolean.FALSE);
			dup.put("message", "已经有启用的模版");
			dup.put("status_code", 422);
			return ResponseEntity.ok(ApiResult.ok(dup));
		}
		return ResponseEntity.ok(ApiResult.ok(outcome.successData()));
	}

	@DeleteMapping(value = "/customizepage/{id}", name = "删除自定义页面", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteCustomizePage(HttpServletRequest request,
			@PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long pageId = parseDeleteCustomizePageIdFromPath(id);
		Map<String, Object> data = customizePageService.deleteCustomizePage(companyId, pageId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.customizepage.list")
	@GetMapping(value = "/customizepage/list", name = "自定义页面列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getCustomizepageList(HttpServletRequest request,
			@RequestParam(name = "template_name", required = false) String templateName,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(name = "page_type", required = false) String pageTypeRaw,
			@RequestParam(name = "regionauth_id", required = false) String regionauthIdRaw) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) attr;
		long jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));
		boolean templateNameParamPresent = request.getParameterMap().containsKey("template_name");
		boolean regionauthKeyPresent = request.getParameterMap().containsKey("regionauth_id");
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data = customizePageService.getCustomizepageList(companyId, jwtDistributorId, templateNameParamPresent,
				templateName, pageRaw, pageSizeRaw, pageTypeRaw, regionauthKeyPresent, regionauthIdRaw, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.customizepage.info")
	@GetMapping(value = "/customizepage/{id}", name = "自定义页面详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getCustomizePageInfo(HttpServletRequest request,
			@PathVariable("id") String id) {
		long pageId = parseGetCustomizePageInfoIdFromPath(id);
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data = customizePageService.getCustomizePageInfo(pageId, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/salesperson/customizepage", name = "导购货架首页", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSalespersonCustomizePage(HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateName) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String requestLang = RequestLangTag.current(langueProperties);
		return ResponseEntity.ok(ApiResult.ok(
				customizePageService.getSalespersonCustomizePage(companyId, templateName, requestLang)));
	}

	@PutMapping(value = "/customizepage/{id}/bindcategory", name = "绑定分类", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> bindCategoryId(HttpServletRequest request, @PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long customizePageId = parseCustomizePageIdFromPath(id);

		Object regionRaw = null;
		if (body != null && body.containsKey("regionauth_id")) {
			regionRaw = body.get("regionauth_id");
		} else if (request.getParameterMap().containsKey("regionauth_id")) {
			regionRaw = request.getParameter("regionauth_id");
		}
		long regionauthId;
		if (regionRaw == null || String.valueOf(regionRaw).trim().isEmpty()) {
			regionauthId = 0L;
		} else {
			try {
				regionauthId = Long.parseLong(String.valueOf(regionRaw).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("regionauth_id 格式不正确", 422);
			}
		}

		Object categoryRaw = null;
		if (body != null && body.containsKey("category_id")) {
			categoryRaw = body.get("category_id");
		} else {
			categoryRaw = request.getParameter("category_id");
		}
		if (categoryRaw == null || String.valueOf(categoryRaw).trim().isEmpty()) {
			throw new BadRequestException("分类ID不能为空", 422);
		}
		long categoryId;
		try {
			categoryId = Long.parseLong(String.valueOf(categoryRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("category_id 格式不正确", 422);
		}

		long companyId = readCompanyIdFromOperatorJwt(request);
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) attr;
		long jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));

		Map<String, Object> data = customizePageCategoryBindFacade.bindCategoryId(companyId, jwtDistributorId, customizePageId,
				regionauthId, categoryId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.customizepage.category.copy")
	@PostMapping(value = "/customizepage/copy/{id}", name = "复制页面", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> copy(HttpServletRequest request, @PathVariable("id") String id) {
		long sourcePageId = parseCustomizePageIdFromPath(id);
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ResponseEntity.ok(ApiResult.ok(customizePageService.copy(companyId, sourcePageId)));
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
	}

	private static long parseCustomizePageIdFromPath(String id) {
		if (id == null || id.trim().isEmpty()) {
			throw new ResourceException("页面不存在");
		}
		try {
			return Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("页面不存在");
		}
	}

	private static long parseGetCustomizePageInfoIdFromPath(String id) {
		if (id == null || id.trim().isEmpty()) {
			throw new ResourceException("自定义页面不存在");
		}
		try {
			return Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("自定义页面不存在");
		}
	}

	private static long parseDeleteCustomizePageIdFromPath(String id) {
		if (id == null || id.trim().isEmpty()) {
			throw new ResourceException("自定义页面不存在");
		}
		try {
			return Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("自定义页面不存在");
		}
	}

	private static long parseUpdateCustomizePageIdFromPath(String id) {
		if (id == null || id.trim().isEmpty()) {
			throw new BadRequestException("页面ID必传");
		}
		String trimmed = id.trim();
		if ("0".equals(trimmed)) {
			throw new BadRequestException("页面ID必传");
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException("页面ID必传");
		}
	}

	private static long parseLongOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
