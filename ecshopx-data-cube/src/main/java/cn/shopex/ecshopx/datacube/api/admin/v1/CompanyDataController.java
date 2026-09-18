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
import cn.shopex.ecshopx.datacube.service.companydata.AdminCompanyDataReadService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
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
@RestController("datacubeAdminV1CompanyData")
@RequestMapping("/api/v1/datacube")
public class CompanyDataController {

	private final CompanysActivationService companysActivationService;

	private final DatacubeShopRoutePermissionService datacubeShopRoutePermissionService;

	private final AdminCompanyDataReadService adminCompanyDataReadService;

	public CompanyDataController(
			CompanysActivationService companysActivationService,
			DatacubeShopRoutePermissionService datacubeShopRoutePermissionService,
			AdminCompanyDataReadService adminCompanyDataReadService) {
		this.companysActivationService = companysActivationService;
		this.datacubeShopRoutePermissionService = datacubeShopRoutePermissionService;
		this.adminCompanyDataReadService = adminCompanyDataReadService;
	}

	@Activated(routeAlias = "datacube.company.data")
	@GetMapping(value = "/companydata", name = "获取商城统计数据")
	public ResponseEntity<Map<String, Object>> getCompanyData(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) Integer ignoredPage,
			@RequestParam(name = "pageSize", required = false) Integer ignoredPageSize) {
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
		datacubeShopRoutePermissionService.assertCompanyData(user);

		Map<String, Object> inner = adminCompanyDataReadService.getCompanyDataResult(request, user);
		return ResponseEntity.ok(Map.of("data", inner));
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
