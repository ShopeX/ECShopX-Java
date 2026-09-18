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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.wechat.service.WechatMenuService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("wechatAdminV1Menu")
@RequestMapping("/api/v1/wechat")
public class MenuController {

	private final WechatMenuService wechatMenuService;

	public MenuController(WechatMenuService wechatMenuService) {
		this.wechatMenuService = wechatMenuService;
	}

	@Activated(routeAlias = "wechat.add_menu")
	@PostMapping(value = "/menu", name = "添加菜单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> addMenu(
			HttpServletRequest request, @RequestBody(required = false) List<Map<String, Object>> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) attr;
		Object appidObj = map.get("authorizer_appid");
		String authorizerAppId = String.valueOf(appidObj == null ? "" : appidObj).trim();
		Object companyIdRaw = map.get("company_id");
		List<Map<String, Object>> validated = wechatMenuService.addMenu(
				authorizerAppId,
				companyIdRaw == null ? "" : String.valueOf(companyIdRaw).trim(),
				body == null ? List.of() : body);
		return ResponseEntity.ok(ApiResult.ok(validated));
	}

	@Activated(routeAlias = "wechat.remove_menu")
	@DeleteMapping(value = "/menu", name = "删除菜单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> removeMenu(
			HttpServletRequest request, @RequestBody(required = false) List<Map<String, Object>> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) attr;
		Object appidObj = map.get("authorizer_appid");
		String authorizerAppId = String.valueOf(appidObj == null ? "" : appidObj).trim();
		Object companyIdRaw = map.get("company_id");
		Map<String, Object> serviceResult = wechatMenuService.removeMenu(
				authorizerAppId,
				companyIdRaw == null ? "" : String.valueOf(companyIdRaw).trim(),
				body == null ? List.of() : body);
		return ResponseEntity.ok(ApiResult.ok(serviceResult));
	}

	@Activated(routeAlias = "wechat.get_menu")
	@GetMapping(value = "/menutree", name = "菜单树")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getMenuTree(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) attr;
		Object appidObj = map.get("authorizer_appid");
		String authorizerAppId = String.valueOf(appidObj == null ? "" : appidObj).trim();
		Object companyIdRaw = map.get("company_id");
		List<Map<String, Object>> data = wechatMenuService.getMenuTree(
				authorizerAppId, companyIdRaw == null ? "" : String.valueOf(companyIdRaw).trim());
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
