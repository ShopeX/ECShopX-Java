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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreateWebMenuRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.UpdateWebMenuRequest;
import cn.shopex.ecshopx.theme.service.WebMenuCreateService;
import cn.shopex.ecshopx.theme.service.WebMenuDeleteService;
import cn.shopex.ecshopx.theme.service.WebMenuDetailService;
import cn.shopex.ecshopx.theme.service.WebMenuListService;
import cn.shopex.ecshopx.theme.service.WebMenuUpdateService;
import jakarta.servlet.http.HttpServletRequest;
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
@RestController("themeAdminV1WebMenu")
@RequestMapping("/api/v1/web-menus")
public class WebMenuController {

	private final WebMenuListService webMenuListService;

	private final WebMenuCreateService webMenuCreateService;

	private final WebMenuDetailService webMenuDetailService;

	private final WebMenuUpdateService webMenuUpdateService;

	private final WebMenuDeleteService webMenuDeleteService;

	@Activated(routeAlias = "webmenu.lists")
	@GetMapping(name = "Web菜单列表")
	public ApiResult<Map<String, Object>> list(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "page_size", required = false) String pageSizeParam,
			@RequestParam(value = "name", required = false) String nameParam) {
		long companyId = parseCompanyId(request);
		int page = parseOptionalInt(pageParam, 1);
		int pageSize = parseOptionalInt(pageSizeParam, 20);
		String name = StringUtils.hasText(nameParam) ? nameParam.trim() : null;
		return ApiResult.ok(webMenuListService.listMenus(companyId, page, pageSize, name));
	}

	@Activated(routeAlias = "webmenu.create")
	@PostMapping(name = "新建Web菜单")
	public ApiResult<Map<String, Object>> create(
			HttpServletRequest request,
			@FlexibleBody CreateWebMenuRequest body) {
		long companyId = parseCompanyId(request);
		return ApiResult.ok(webMenuCreateService.create(companyId, body));
	}

	@Activated(routeAlias = "webmenu.detail")
	@GetMapping(value = "/{id}", name = "Web菜单详情")
	public ApiResult<Map<String, Object>> detail(HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = parseCompanyId(request);
		long menuId = parseMenuId(id);
		return ApiResult.ok(webMenuDetailService.detail(menuId, companyId));
	}

	@Activated(routeAlias = "webmenu.update")
	@PutMapping(value = "/{id}", name = "编辑Web菜单")
	public ApiResult<Map<String, Object>> update(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody UpdateWebMenuRequest body) {
		long companyId = parseCompanyId(request);
		long menuId = parseMenuId(id);
		return ApiResult.ok(webMenuUpdateService.update(menuId, companyId, body));
	}

	@Activated(routeAlias = "webmenu.delete")
	@DeleteMapping(value = "/{id}", name = "删除Web菜单")
	public ApiResult<Map<String, Object>> delete(HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = parseCompanyId(request);
		long menuId = parseMenuId(id);
		webMenuDeleteService.delete(menuId, companyId);
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	private static long parseCompanyId(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		Object companyIdClaim = jwt.get("company_id");
		if (companyIdClaim == null) {
			throw new UnauthorizedException("company_id 无效");
		}
		long companyId;
		if (companyIdClaim instanceof Number n) {
			companyId = n.longValue();
		} else if (companyIdClaim instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new UnauthorizedException("company_id 无效");
			}
		} else {
			throw new UnauthorizedException("company_id 无效");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("company_id 无效");
		}
		return companyId;
	}

	private static long parseMenuId(String id) {
		if (!StringUtils.hasText(id)) {
			throw new ResourceException("菜单不存在");
		}
		try {
			long menuId = Long.parseLong(id.trim());
			if (menuId <= 0L) {
				throw new ResourceException("菜单不存在");
			}
			return menuId;
		} catch (NumberFormatException ex) {
			throw new ResourceException("菜单不存在");
		}
	}

	private static int parseOptionalInt(String raw, int defaultValue) {
		if (!StringUtils.hasText(raw)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> map) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			out.put(String.valueOf(entry.getKey()), entry.getValue());
		}
		return out;
	}
}
