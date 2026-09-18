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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.theme.service.MemberCenterShareSetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RequiredArgsConstructor
@RestController("themeFrontV1MemberCenterShare")
@RequestMapping("/api/v1/h5app/wxapp/memberCenterShare")
public class MemberCenterShareController {

	private final MemberCenterShareSetService memberCenterShareSetService;

	@GetMapping(value = "/getInfo", name = "会员中心分享")
	public ApiResult<Object> getInfo(HttpServletRequest request) {
		Object payload;
		if (request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS) != null) {
			long companyId = parseCompanyIdForValidator(request);
			payload = memberCenterShareSetService.getInfo(companyId);
		} else {
			String raw = request.getParameter("company_id");
			if (raw == null || !StringUtils.hasText(raw.trim())) {
				throw new BadRequestException("缺少company_id", 422);
			}
			String trimmed = raw.trim();
			try {
				long companyId = Long.parseLong(trimmed);
				payload = companyId <= 0L
						? Collections.emptyList()
						: memberCenterShareSetService.getInfo(companyId);
			} catch (NumberFormatException ex) {
				payload = Collections.emptyList();
			}
		}
		return ApiResult.ok(payload);
	}

	private long parseCompanyIdForValidator(HttpServletRequest request) {
		Object cid = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (cid == null) {
			throw new BadRequestException("缺少company_id");
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("缺少company_id");
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("缺少company_id");
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("缺少company_id");
			}
		}
		if (result <= 0L) {
			throw new BadRequestException("缺少company_id");
		}
		return result;
	}
}
