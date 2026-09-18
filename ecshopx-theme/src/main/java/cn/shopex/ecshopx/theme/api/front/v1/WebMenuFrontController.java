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

package cn.shopex.ecshopx.theme.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.theme.service.WebMenuFrontReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RequiredArgsConstructor
@RestController("themeFrontV1WebMenu")
@RequestMapping("/api/v1/h5app")
public class WebMenuFrontController {

	private final WebMenuFrontReadService webMenuFrontReadService;

	@GetMapping(value = "/web/menus/{key}", name = "Web导航菜单树")
	public ApiResult<Map<String, Object>> byKey(HttpServletRequest request, @PathVariable("key") String key) {
		long companyId = parseCompanyId(request);
		return ApiResult.ok(webMenuFrontReadService.byKey(companyId, key));
	}

	@GetMapping(value = "/wxapp/web/menus/id/{id}", name = "根据ID获取Web导航菜单树")
	public ApiResult<Map<String, Object>> byId(HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = parseCompanyId(request);
		return ApiResult.ok(webMenuFrontReadService.byId(companyId, parsePathId(id)));
	}

	private static long parseCompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new UnauthorizedException("缺少 company 上下文");
			}
		} else {
			throw new UnauthorizedException("缺少 company 上下文");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("缺少 company 上下文");
		}
		return companyId;
	}

	private static long parsePathId(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
