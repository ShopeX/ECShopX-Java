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

package cn.shopex.ecshopx.point.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("pointMemberRuleFrontV1")
@RequestMapping("/api/v1/h5app")
public class PointMemberRuleController {

	private final PointMemberRuleReadService pointMemberRuleReadService;

	public PointMemberRuleController(PointMemberRuleReadService pointMemberRuleReadService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
	}

	@GetMapping(value = "/wxapp/point/rule", name = "获取积分规则")
	public ResponseEntity<ApiResult<Map<String, Object>>> info(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		String countryCode = request.getParameter("country_code");
		if (!StringUtils.hasText(countryCode)) {
			countryCode = "zh-CN";
		} else {
			countryCode = countryCode.trim();
		}
		Map<String, Object> full = pointMemberRuleReadService.getPointRule(companyId, countryCode);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("name", full.get("name") == null ? "" : String.valueOf(full.get("name")));
		data.put("rule_desc", full.get("rule_desc") == null ? "" : String.valueOf(full.get("rule_desc")));
		return ResponseEntity.ok(ApiResult.ok(data));
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
