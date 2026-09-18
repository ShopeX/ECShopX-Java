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

package cn.shopex.ecshopx.aftersales.api.front.v1;

import cn.shopex.ecshopx.aftersales.service.AftersalesReasonSaveService;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
@FrontAuth
@RestController("reasonFrontV1")
@RequestMapping("/api/v1/h5app")
public class ReasonController {

	private final AftersalesReasonSaveService aftersalesReasonSaveService;

	public ReasonController(AftersalesReasonSaveService aftersalesReasonSaveService) {
		this.aftersalesReasonSaveService = aftersalesReasonSaveService;
	}

	@GetMapping(
			value = "/wxapp/aftersales/reason/list",
			name = "获取售后原因列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<String>>> getSreasonList(
			@RequestParam(value = "country_code", required = false) String countryCode,
			HttpServletRequest request) {
		String lang;
		if (countryCode == null || countryCode.trim().isEmpty()) {
			lang = "zh-CN";
		} else {
			lang = countryCode.trim();
		}
		Map<String, Object> auth = mergeAuth(request);
		long companyId = parseLongStrict(auth.get("company_id"), "企业id必填");
		List<String> list = aftersalesReasonSaveService.getSreasonList(companyId, lang, false);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	private Map<String, Object> mergeAuth(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private static long parseLongStrict(Object o, String absentMessage) {
		if (o == null) {
			throw new BadRequestException(absentMessage);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(absentMessage);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(absentMessage);
		}
	}
}
