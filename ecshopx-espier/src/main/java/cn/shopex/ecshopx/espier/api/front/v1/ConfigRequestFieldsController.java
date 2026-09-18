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

package cn.shopex.ecshopx.espier.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("espierFrontV1ConfigRequestFields")
@RequestMapping("/api/v1/h5app")
public class ConfigRequestFieldsController {

	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;

	public ConfigRequestFieldsController(ConfigRequestFieldsApplicationService configRequestFieldsApplicationService) {
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
	}

	@GetMapping(value = "/wxapp/espier/config/request_field_setting", name = "请求字段配置项")
	public ResponseEntity<ApiResult<Map<String, Object>>> getConfig(
			HttpServletRequest request,
			@RequestParam(value = "module_type", required = false) String moduleTypeParam) {
		int moduleType;
		if (moduleTypeParam == null) {
			moduleType = ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		} else {
			String s = moduleTypeParam.trim();
			long pl = LeadingNumberParser.parseAsLong(s);
			if (pl > Integer.MAX_VALUE) {
				moduleType = Integer.MAX_VALUE;
			} else if (pl < Integer.MIN_VALUE) {
				moduleType = Integer.MIN_VALUE;
			} else {
				moduleType = (int) pl;
			}
		}

		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		int companyIdInt = (int) companyId;
		Map<String, Object> data = configRequestFieldsApplicationService.getConfig(companyIdInt, moduleType, 0);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long resolveCompanyIdForFrontNoAuth(HttpServletRequest request, Map<String, Object> claims) {
		long fromAttr = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		long fromQuery = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
		if (fromQuery > 0L) {
			return fromQuery;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
