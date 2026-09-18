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

package cn.shopex.ecshopx.aftersales.api.admin.v1;

import cn.shopex.ecshopx.aftersales.service.AftersalesReasonSaveService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("reasonAdminV1")
@RequestMapping("/api/v1")
public class ReasonController {

	private final AftersalesReasonSaveService aftersalesReasonSaveService;

	public ReasonController(AftersalesReasonSaveService aftersalesReasonSaveService) {
		this.aftersalesReasonSaveService = aftersalesReasonSaveService;
	}

	@Activated(routeAlias = "aftersales.reason.list")
	@GetMapping(value = "/aftersales/reason/list", name = "售后原因列表获取")
	public ApiResult<List<String>> getSreasonList(
			@RequestParam(value = "country_code", required = false) String countryCode,
			HttpServletRequest request) {
		String lang;
		if (countryCode == null || countryCode.trim().isEmpty()) {
			lang = "zh-CN";
		} else {
			lang = countryCode.trim();
		}

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);

		List<String> list = aftersalesReasonSaveService.getSreasonList(companyId, lang, true);
		return ApiResult.ok(list);
	}

	@Activated(routeAlias = "aftersales.reason.save")
	@PostMapping(value = "/aftersales/reason/save", name = "售后原因列表保存")
	public ApiResult<List<Object>> Saveset(
			@FlexibleBody(required = false) Map<String, Object> body,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object reasonPayload = merged.get("reason");
		Object cc = merged.get("country_code");
		String lang;
		if (cc == null || String.valueOf(cc).trim().isEmpty()) {
			lang = "zh-CN";
		} else {
			lang = String.valueOf(cc).trim();
		}

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);

		aftersalesReasonSaveService.Saveset(companyId, reasonPayload, lang);
		return ApiResult.ok(new ArrayList<>());
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
