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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreatePagesSideBarRequest;
import cn.shopex.ecshopx.theme.service.PagesSideBarCreateService;
import cn.shopex.ecshopx.theme.service.PagesSideBarDeleteService;
import cn.shopex.ecshopx.theme.service.PagesSideBarGetInfoService;
import cn.shopex.ecshopx.theme.service.PagesSideBarListService;
import cn.shopex.ecshopx.theme.service.PagesSideBarUpdateService;
import cn.shopex.ecshopx.theme.service.dto.PagesSideBarListCriteria;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController("themeAdminV1PagesSideBar")
@RequestMapping("/api/v1/sidebar")
public class PagesSideBarController {

	private final PagesSideBarCreateService pagesSideBarCreateService;

	private final PagesSideBarUpdateService pagesSideBarUpdateService;

	private final PagesSideBarListService pagesSideBarListService;

	private final PagesSideBarGetInfoService pagesSideBarGetInfoService;

	private final PagesSideBarDeleteService pagesSideBarDeleteService;

	@Activated(routeAlias = "pages.sidebar.list")
	@GetMapping(value = "/list", name = "侧边栏列表")
	public ApiResult<Map<String, Object>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(value = "regionauth_id", required = false) String regionauthIdParam,
			@RequestParam(value = "id", required = false) String idParam,
			@RequestParam(value = "name", required = false) String nameParam,
			@RequestParam(value = "page_type", required = false) String pageTypeParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		if (!StringUtils.hasText(pageParam)) {
			throw new BadRequestException("The page field is required.");
		}
		if (!StringUtils.hasText(pageSizeParam)) {
			throw new BadRequestException("The page size field is required.");
		}
		int page;
		int pageSize;
		try {
			page = Integer.parseInt(pageParam.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("The page must be an integer.");
		}
		try {
			pageSize = Integer.parseInt(pageSizeParam.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("The page size must be an integer.");
		}
		if (page < 1) {
			throw new BadRequestException("The page must be at least 1.");
		}
		if (pageSize < 1) {
			throw new BadRequestException("The page size must be at least 1.");
		}
		if (pageSize > 50) {
			throw new BadRequestException("The page size may not be greater than 50.");
		}

		Long regionauthIdForFilter = null;
		if (StringUtils.hasText(regionauthIdParam)) {
			try {
				long v = Long.parseLong(regionauthIdParam.trim());
				if (v != 0L) {
					regionauthIdForFilter = v;
				}
			} catch (NumberFormatException ex) {
				// keep null, same as optional id strategy on PagesAdPlace
			}
		}
		Long idForFilter = null;
		if (StringUtils.hasText(idParam)) {
			try {
				long v = Long.parseLong(idParam.trim());
				if (v != 0L) {
					idForFilter = v;
				}
			} catch (NumberFormatException ex) {
				// keep null
			}
		}
		String nameForFilter = null;
		if (StringUtils.hasText(nameParam)) {
			nameForFilter = nameParam.trim();
		}
		String pagesContainsInnerForFilter = null;
		if (StringUtils.hasText(pageTypeParam)) {
			pagesContainsInnerForFilter = "," + pageTypeParam.trim() + ",";
		}

		PagesSideBarListCriteria criteria = PagesSideBarListCriteria.builder()
				.companyId(companyId)
				.regionauthId(regionauthIdForFilter)
				.id(idForFilter)
				.nameContains(nameForFilter)
				.pagesContainsInner(pagesContainsInnerForFilter)
				.build();

		return ApiResult.ok(pagesSideBarListService.getList(criteria, page, pageSize));
	}

	@Activated(routeAlias = "pages.sidebar.getInfo")
	@GetMapping(value = "/{id}", name = "侧边栏详情")
	public ApiResult<Object> getInfo(HttpServletRequest request, @PathVariable("id") String id) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		Long sidebarIdResolved = tryParseStrictPositiveLongPathId(id);
		if (sidebarIdResolved == null) {
			return ApiResult.ok(Collections.emptyList());
		}
		long sidebarId = sidebarIdResolved;
		Object data = pagesSideBarGetInfoService.getInfo(companyId, sidebarId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pages.sidebar.create")
	@PostMapping(name = "创建侧边栏")
	public ApiResult<Map<String, Object>> create(
			HttpServletRequest request, @Valid @FlexibleBody CreatePagesSideBarRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		return ApiResult.ok(pagesSideBarCreateService.create(companyId, body));
	}

	@Activated(routeAlias = "pages.sidebar.update")
	@PutMapping(value = "/{id}", name = "更新侧边栏")
	public ApiResult<Map<String, Object>> update(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		return ApiResult.ok(pagesSideBarUpdateService.update(companyId, id, body));
	}

	@Activated(routeAlias = "pages.sidebar.delete")
	@DeleteMapping(value = "/{id}", name = "删除侧边栏")
	public ApiResult<Map<String, Object>> delete(HttpServletRequest request, @PathVariable("id") String id) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		Long sidebarIdResolved = tryParseStrictPositiveLongPathId(id);
		if (sidebarIdResolved == null) {
			return ApiResult.ok(Map.of("status", Boolean.TRUE));
		}
		pagesSideBarDeleteService.delete(companyId, sidebarIdResolved.longValue());
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

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

	private static Long tryParseStrictPositiveLongPathId(String idParam) {
		if (idParam == null || !StringUtils.hasText(idParam)) {
			return null;
		}
		try {
			long v = Long.parseLong(idParam.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
