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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.service.WechatOpenUserPlatformService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("wechatAdminV1Open")
@RequestMapping("/api/v1/wechat")
public class OpenController {

	private final WechatOpenUserPlatformService wechatOpenUserPlatformService;

	public OpenController(WechatOpenUserPlatformService wechatOpenUserPlatformService) {
		this.wechatOpenUserPlatformService = wechatOpenUserPlatformService;
	}

	@Activated(routeAlias = "wechat.user.open")
	@PostMapping(value = "/open", name = "开通开放平台并绑定小程序", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> openCreate(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object raw = map.get("authorizer_appid");
		String jwtAuthorizerAppidStringOrNull = (raw == null) ? null : String.valueOf(raw).trim();
		wechatOpenUserPlatformService.openCreate(companyId, jwtAuthorizerAppidStringOrNull);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
