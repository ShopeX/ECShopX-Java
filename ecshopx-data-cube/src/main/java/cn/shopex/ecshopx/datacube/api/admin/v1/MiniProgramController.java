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

package cn.shopex.ecshopx.datacube.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.datacube.service.DatacubeShopRoutePermissionService;
import cn.shopex.ecshopx.datacube.service.miniprogram.DatacubeMiniProgramPagesService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("datacubeAdminV1MiniProgram")
@RequestMapping("/api/v1/datacube/miniprogram")
public class MiniProgramController {

	private final CompanysActivationService companysActivationService;

	private final DatacubeShopRoutePermissionService datacubeShopRoutePermissionService;

	private final DatacubeMiniProgramPagesService datacubeMiniProgramPagesService;

	public MiniProgramController(
			CompanysActivationService companysActivationService,
			DatacubeShopRoutePermissionService datacubeShopRoutePermissionService,
			DatacubeMiniProgramPagesService datacubeMiniProgramPagesService) {
		this.companysActivationService = companysActivationService;
		this.datacubeShopRoutePermissionService = datacubeShopRoutePermissionService;
		this.datacubeMiniProgramPagesService = datacubeMiniProgramPagesService;
	}

	@Activated(routeAlias = "miniprogram.pages")
	@GetMapping(value = "/pages", name = "获取小程序的页面及参数信息")
	public ResponseEntity<?> getPages(
			HttpServletRequest request,
			@RequestParam(name = "wxappid", required = false) String wxappid) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMiniprogramPages(user);

		List<Map<String, Object>> pages = datacubeMiniProgramPagesService.getPages(companyId, wxappid);
		return ResponseEntity.ok(Map.of("data", pages));
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
