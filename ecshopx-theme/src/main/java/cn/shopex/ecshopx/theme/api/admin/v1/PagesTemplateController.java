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

package cn.shopex.ecshopx.theme.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateAddRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateCopyRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateModifyStatusRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateSyncRequest;
import cn.shopex.ecshopx.theme.service.PagesTemplateAddService;
import cn.shopex.ecshopx.theme.service.PagesTemplateCopyService;
import cn.shopex.ecshopx.theme.service.PagesTemplateDeleteService;
import cn.shopex.ecshopx.theme.service.PagesTemplateDetailService;
import cn.shopex.ecshopx.theme.service.PagesTemplateEditService;
import cn.shopex.ecshopx.theme.service.PagesTemplateListService;
import cn.shopex.ecshopx.theme.service.PagesTemplateModifyStatusService;
import cn.shopex.ecshopx.theme.service.PagesTemplateSyncService;
import cn.shopex.ecshopx.theme.service.PagesTemplateWidgetItemsService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateListQuery;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateWidgetItemsQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
		notFound = false)
@AdminAuth
@ShopLog
@RequiredArgsConstructor
@RestController("themeAdminV1PagesTemplate")
@RequestMapping("/api/v1/pagestemplate")
public class PagesTemplateController {

	private final PagesTemplateAddService pagesTemplateAddService;

	private final PagesTemplateCopyService pagesTemplateCopyService;

	private final PagesTemplateDeleteService pagesTemplateDeleteService;

	private final PagesTemplateDetailService pagesTemplateDetailService;

	private final PagesTemplateListService pagesTemplateListService;

	private final ObjectMapper objectMapper;

	private final PagesTemplateEditService pagesTemplateEditService;

	private final PagesTemplateModifyStatusService pagesTemplateModifyStatusService;

	private final PagesTemplateSyncService pagesTemplateSyncService;

	private final PagesTemplateWidgetItemsService pagesTemplateWidgetItemsService;

	@Activated(routeAlias = "pagestemplate.lists")
	@GetMapping(value = "/lists", name = "模板列表")
	public ApiResult<Map<String, Object>> lists(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) Integer distributorId,
			@RequestParam(value = "weapp_pages", required = false, defaultValue = "index") String weappPages,
			@RequestParam(value = "page", required = false) String pageStr,
			@RequestParam(value = "pageSize", required = false) String pageSizeStr) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String locale = request.getHeader("country-code");
		if (!StringUtils.hasText(locale)) {
			locale = "zh-CN";
		} else {
			locale = locale.trim();
		}

		int dist = distributorId == null ? 0 : distributorId;
		if (dist < 0) {
			dist = 0;
		}
		if (dist <= 0) {
			Object jwtDist = jwt.get("distributor_id");
			if (jwtDist instanceof Number n) {
				int parsed = n.intValue();
				if (parsed > 0) {
					dist = parsed;
				}
			} else if (jwtDist != null) {
				try {
					int parsed = Integer.parseInt(String.valueOf(jwtDist).trim());
					if (parsed > 0) {
						dist = parsed;
					}
				} catch (NumberFormatException ignored) {
					// keep dist as 0
				}
			}
		}
		String we = weappPages == null ? "" : weappPages.trim();
		if (!StringUtils.hasText(we)) {
			we = "index";
		}
		if (dist > 0 && "index".equals(we)) {
			we = "distributor_index";
		}

		boolean regionauthPresent = request.getParameterMap().containsKey("regionauth_id");
		Long regionauthId = null;
		if (regionauthPresent) {
			String ridRaw = request.getParameter("regionauth_id");
			regionauthId = parseRegionauthIdQueryParam(ridRaw);
		}

		int page = parseListPage(pageStr);
		int pageSize = parseListPageSize(pageSizeStr);

		PagesTemplateListQuery query =
				new PagesTemplateListQuery(
						companyId,
						dist,
						we,
						regionauthPresent,
						regionauthPresent ? regionauthId : null,
						page,
						pageSize);
		return ApiResult.ok(pagesTemplateListService.lists(query, locale));
	}

	@Activated(routeAlias = "pagestemplate.add")
	@PostMapping(value = "/add", name = "新增模板")
	public ApiResult<Map<String, Object>> add(HttpServletRequest request, @FlexibleBody PagesTemplateAddRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		return ApiResult.ok(pagesTemplateAddService.add(companyId, body));
	}

	@Activated(routeAlias = "pagestemplate.edit")
	@PutMapping(value = "/edit", name = "编辑模板")
	public ApiResult<Map<String, Object>> edit(
			HttpServletRequest request, @FlexibleBody(required = false) JsonNode body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		JsonNode effective = body == null ? objectMapper.createObjectNode() : body;
		return ApiResult.ok(pagesTemplateEditService.handleEdit(companyId, effective));
	}

	@Activated(routeAlias = "pagestemplate.detail")
	@GetMapping(value = "/detail", name = "模板详情")
	public ApiResult<Object> detail(
			HttpServletRequest request,
			@RequestParam(value = "pages_template_id", required = false) Long pagesTemplateId,
			@RequestParam(value = "version", required = false, defaultValue = "v1.0.2") String version) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String locale = request.getHeader("country-code");
		if (!StringUtils.hasText(locale)) {
			locale = "zh-CN";
		} else {
			locale = locale.trim();
		}
		return ApiResult.ok(pagesTemplateDetailService.detail(companyId, pagesTemplateId, version, locale));
	}

	@Activated(routeAlias = "pagestemplate.widget.items")
	@GetMapping(value = "/widget/items", name = "模板组件商品")
	public ResponseEntity<Map<String, Object>> getWidgetItems(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String locale = request.getHeader("country-code");
		if (!StringUtils.hasText(locale)) {
			locale = "zh-CN";
		} else {
			locale = locale.trim();
		}
		PagesTemplateWidgetItemsQuery q = PagesTemplateWidgetItemsQuery.fromHttpServletRequest(request);
		return ResponseEntity.ok(pagesTemplateWidgetItemsService.getWidgetItems(companyId, locale, q, null));
	}

	@Activated(routeAlias = "pagestemplate.copy")
	@PostMapping(value = "/copy", name = "复制模板")
	public ApiResult<Map<String, Object>> copy(HttpServletRequest request, @FlexibleBody PagesTemplateCopyRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Long pagesTemplateId = body.getPagesTemplateId();
		String locale = request.getHeader("country-code");
		if (!StringUtils.hasText(locale)) {
			locale = "zh-CN";
		} else {
			locale = locale.trim();
		}
		return ApiResult.ok(pagesTemplateCopyService.copy(companyId, pagesTemplateId, locale));
	}

	@Activated(routeAlias = "pagestemplate.delete")
	@DeleteMapping(value = "/del/{pages_template_id}", name = "废弃模板")
	public ApiResult<Map<String, Object>> delete(
			HttpServletRequest request, @PathVariable("pages_template_id") String pagesTemplateIdRaw) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String trimmed = pagesTemplateIdRaw == null ? "" : pagesTemplateIdRaw.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("pages_template_id 无效");
		}
		long pagesTemplateId;
		try {
			pagesTemplateId = Long.parseLong(trimmed);
			if (pagesTemplateId <= 0) {
				throw new BadRequestException("pages_template_id 无效");
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("pages_template_id 无效");
		}
		pagesTemplateDeleteService.delete(companyId, pagesTemplateId);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "pagestemplate.modifyStatus")
	@PutMapping(value = "/modifyStatus", name = "模板状态变更")
	public ApiResult<Map<String, Object>> modifyStatus(
			HttpServletRequest request, @FlexibleBody PagesTemplateModifyStatusRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		return ApiResult.ok(pagesTemplateModifyStatusService.modifyStatus(companyId, body));
	}

	@Activated(routeAlias = "pagestemplate.sync")
	@PutMapping(value = "/sync", name = "模板同步")
	public ResponseEntity<Map<String, Object>> sync(
			HttpServletRequest request, @FlexibleBody PagesTemplateSyncRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String locale = request.getHeader("country-code");
		if (!StringUtils.hasText(locale)) {
			locale = "zh-CN";
		} else {
			locale = locale.trim();
		}
		Map<String, Object> syncResult = pagesTemplateSyncService.sync(companyId, body, locale);
		return ResponseEntity.ok(Map.of("data", syncResult));
	}

	// 与 PagesAdPlaceController 保持一致
	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static int parseListPage(String pageStr) {
		String raw = (pageStr != null && StringUtils.hasText(pageStr)) ? pageStr.trim() : "1";
		if (!StringUtils.hasText(raw)) {
			raw = "1";
		}
		try {
			int v = Integer.parseInt(raw.trim());
			if (v < 1) {
				throw new BadRequestException("page 无效");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("page 无效");
		}
	}

	private static int parseListPageSize(String pageSizeStr) {
		String raw = (pageSizeStr != null && StringUtils.hasText(pageSizeStr)) ? pageSizeStr.trim() : "50";
		int v;
		try {
			v = Integer.parseInt(raw);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("pageSize 无效");
		}
		if (v <= 0) {
			return 50;
		}
		return v;
	}

	private static long parseRegionauthIdQueryParam(String ridRaw) {
		String t = ridRaw == null ? "" : ridRaw.trim();
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		try {
			return new BigDecimal(t).longValueExact();
		} catch (ArithmeticException | NumberFormatException ex) {
			throw new BadRequestException("regionauth_id 无效");
		}
	}

	// 与 PagesAdPlaceController 保持一致
	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}
}
