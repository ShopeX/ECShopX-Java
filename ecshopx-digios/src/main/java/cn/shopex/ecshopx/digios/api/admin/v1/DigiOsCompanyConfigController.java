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

package cn.shopex.ecshopx.digios.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.DualJwtSupport;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.digios.service.DigiOsCompanyConfigAdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("digiosAdminV1CompanyConfig")
@RequestMapping("/api/v1/digios/company_config")
public class DigiOsCompanyConfigController {

	private final DigiOsCompanyConfigAdminService adminService;

	public DigiOsCompanyConfigController(DigiOsCompanyConfigAdminService adminService) {
		this.adminService = adminService;
	}

	@Activated(routeAlias = "digios.company_config.get")
	@GetMapping(name = "DigiOS进销存配置-获取")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(HttpServletRequest request) {
		long companyId = DualJwtSupport.requireCompanyId(request);
		return ResponseEntity.ok(ApiResult.ok(adminService.get(companyId)));
	}

	@Activated(routeAlias = "digios.company_config.save")
	@PostMapping(name = "DigiOS进销存配置-保存")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = DualJwtSupport.requireCompanyId(request);
		return ResponseEntity.ok(ApiResult.ok(adminService.save(companyId, body)));
	}
}
