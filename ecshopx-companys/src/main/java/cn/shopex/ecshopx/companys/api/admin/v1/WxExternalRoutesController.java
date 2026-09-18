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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.wxexternalroutes.WxExternalRoutesDeleteService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("companysAdminV1WxExternalRoutes")
@RequestMapping("/api/v1/wxexternalroutes")
public class WxExternalRoutesController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final WxExternalRoutesDeleteService wxExternalRoutesDeleteService;

	public WxExternalRoutesController(WxExternalRoutesDeleteService wxExternalRoutesDeleteService) {
		this.wxExternalRoutesDeleteService = wxExternalRoutesDeleteService;
	}

	@Activated(routeAlias = "wxexternalroutes.list")
	@GetMapping(value = "/list", name = "获取外部小程序路径列表")
	public ResponseEntity<Void> getwxexternalroutesList() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "wxexternalroutes.create")
	@PostMapping(value = "/create", name = "创建外部小程序路径")
	public ResponseEntity<Void> createwxexternalroutes() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "wxexternalroutes.update")
	@PutMapping(value = "/update/{wx_external_config_id}", name = "更新外部小程序路径")
	public ResponseEntity<Void> updatewxexternalroutes(@PathVariable("wx_external_config_id") String id) {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "wxexternalroutes.delete")
	@DeleteMapping(value = "/{wx_external_config_id}", name = "删除外部小程序路径")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteWxExternalRoutes(
			HttpServletRequest request,
			@PathVariable("wx_external_config_id") String wxExternalConfigId) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		wxExternalRoutesDeleteService.deleteWxExternalRoutes(companyId, wxExternalConfigId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}
}
