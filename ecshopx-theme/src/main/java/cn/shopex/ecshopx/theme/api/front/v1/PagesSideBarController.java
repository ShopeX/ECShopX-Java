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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.theme.service.PagesSideBarWxappSidebarInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RequiredArgsConstructor
@RestController("themeFrontV1PagesSideBar")
@RequestMapping("/api/v1/h5app/wxapp/sidebar")
public class PagesSideBarController {

	private final PagesSideBarWxappSidebarInfoService pagesSideBarWxappSidebarInfoService;

	@GetMapping(value = "/info", name = "侧边栏设置")
	public ApiResult<Object> getPagesSideBar(
			HttpServletRequest request,
			@RequestParam(name = "page_type", required = false) String pageType,
			@RequestParam(name = "regionauth_id", required = false) String regionauthIdParam) {
		if (pageType == null || !StringUtils.hasText(pageType.trim())) {
			throw new ResourceException("validation.required");
		}
		long companyId = parseCompanyIdFromRequest(request);
		Object regionScalar = (regionauthIdParam == null) ? 0L : regionauthIdParam;
		Object data = pagesSideBarWxappSidebarInfoService.getPagesSideBar(companyId, regionScalar, pageType);
		return ApiResult.ok(data);
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}
}
