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

package cn.shopex.ecshopx.espier.api.admin.v1;

import java.util.Map;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.upgrade.UpgradeAgreementQueryService;
import cn.shopex.ecshopx.espier.service.upgrade.UpgradeExecuteService;
import cn.shopex.ecshopx.espier.service.upgrade.UpgradeRollbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@RestController("espierAdminV1Upgrade")
@RequestMapping("/api/v1/espier/system")
public class UpgradeController {

	private final UpgradeAgreementQueryService upgradeAgreementQueryService;
	private final UpgradeRollbackService upgradeRollbackService;
	private final UpgradeExecuteService upgradeExecuteService;

	public UpgradeController(
			UpgradeAgreementQueryService upgradeAgreementQueryService,
			UpgradeRollbackService upgradeRollbackService,
			UpgradeExecuteService upgradeExecuteService) {
		this.upgradeAgreementQueryService = upgradeAgreementQueryService;
		this.upgradeRollbackService = upgradeRollbackService;
		this.upgradeExecuteService = upgradeExecuteService;
	}

	@AdminAuth
	@ShopLog
	@PostMapping(value = "/detect_version", name = "检测版本")
	public ResponseEntity<ApiResult<Object>> detectVersion() {
		return ResponseEntity.ok(ApiResult.ok(upgradeAgreementQueryService.detectVersion()));
	}

	@AdminAuth
	@ShopLog
	@PostMapping(value = "/upgrade", name = "升级")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> upgrade(HttpServletRequest request) {
		long companyId = extractCompanyId(request);
		upgradeExecuteService.upgrade(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@SuppressWarnings("unchecked")
	private long extractCompanyId(HttpServletRequest request) {
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (user == null) {
			throw new ResourceException("未登录");
		}
		Object v = user.get("company_id");
		if (v == null) {
			throw new ResourceException("无法获取公司ID");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString());
	}

	@AdminAuth
	@ShopLog
	@PostMapping(value = "/rollback", name = "回滚")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> rollback() {
		upgradeRollbackService.rollback();
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@AdminAuth
	@ShopLog
	@PostMapping(value = "/changelog", name = "更新日志")
	public ResponseEntity<ApiResult<Object>> changelog() {
		return ResponseEntity.ok(ApiResult.ok(upgradeAgreementQueryService.changelog()));
	}

	@PostMapping(value = "/agreement", name = "获取安装协议")
	public ResponseEntity<ApiResult<Object>> getAgreement() {
		return ResponseEntity.ok(ApiResult.ok(upgradeAgreementQueryService.getAgreement()));
	}
}
