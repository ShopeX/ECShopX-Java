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

package cn.shopex.ecshopx.bspay.api.admin.v1;

import cn.shopex.ecshopx.bspay.service.BspayTradeExportDataService;
import cn.shopex.ecshopx.bspay.service.BspayWithdrawExportDataService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("bspayExportDataAdminV1")
@RequestMapping("/api/v1/bspay")
public class ExportDataController {

	private final BspayTradeExportDataService bspayTradeExportDataService;
	private final BspayWithdrawExportDataService bspayWithdrawExportDataService;

	public ExportDataController(
			BspayTradeExportDataService bspayTradeExportDataService,
			BspayWithdrawExportDataService bspayWithdrawExportDataService) {
		this.bspayTradeExportDataService = bspayTradeExportDataService;
		this.bspayWithdrawExportDataService = bspayWithdrawExportDataService;
	}

	@Activated(routeAlias = "bspay.trades.list.export")
	@GetMapping(value = "/trade/exportdata", name = "导出交易单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportTradeData(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		bspayTradeExportDataService.exportTradeData(jwtMap, request);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.NONE,
			unauthorized = true,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "bspay.withdraw.export")
	@GetMapping(value = "/withdraw/exportdata", name = "导出提现记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportWithdrawData(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		bspayWithdrawExportDataService.exportWithdrawData(jwtMap, request);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}
}
