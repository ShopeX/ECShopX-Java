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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.MemberCenterShareSetRequest;
import cn.shopex.ecshopx.theme.service.MemberCenterShareSetService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RequiredArgsConstructor
@RestController("themeAdminV1MemberCenterShare")
@RequestMapping("/api/v1/memberCenterShare")
public class MemberCenterShareController {

	private final MemberCenterShareSetService memberCenterShareSetService;

	@Activated(routeAlias = "memberCenterShare.set")
	@PostMapping(value = "/set", name = "设置会员中心分享")
	public ApiResult<Map<String, Object>> set(
			HttpServletRequest request, @FlexibleBody MemberCenterShareSetRequest body) {
		long companyId = requireCompanyIdFromOperatorJwt(request);

		String title = body.getShareTitle() == null ? "" : body.getShareTitle().trim();
		if (!StringUtils.hasText(title)) {
			throw new BadRequestException("缺少分享标题");
		}
		String description = body.getShareDescription() == null ? "" : body.getShareDescription().trim();
		if (!StringUtils.hasText(description)) {
			throw new BadRequestException("缺少模板展示类型");
		}

		return ApiResult.ok(memberCenterShareSetService.set(companyId, body));
	}

	@Activated(routeAlias = "memberCenterShare.getInfo")
	@GetMapping(value = "/getInfo", name = "获取会员中心分享")
	public ApiResult<Object> getInfo(HttpServletRequest request) {
		long companyId = requireCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(memberCenterShareSetService.getInfo(companyId));
	}

	private long requireCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		return parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
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
}
