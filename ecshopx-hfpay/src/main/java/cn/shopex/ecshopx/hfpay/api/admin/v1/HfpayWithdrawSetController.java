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

package cn.shopex.ecshopx.hfpay.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayHfFileRequestMergeService;
import cn.shopex.ecshopx.hfpay.service.withdraw.HfpayWithdrawSetSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
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
		notFound = true)
@AdminAuth
@ShopLog
@RestController("hfpayWithdrawSetAdminV1")
@RequestMapping("/api/v1")
public class HfpayWithdrawSetController {

	private final HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService;
	private final HfpayWithdrawSetSaveService hfpayWithdrawSetSaveService;

	public HfpayWithdrawSetController(
			HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService,
			HfpayWithdrawSetSaveService hfpayWithdrawSetSaveService) {
		this.hfpayHfFileRequestMergeService = hfpayHfFileRequestMergeService;
		this.hfpayWithdrawSetSaveService = hfpayWithdrawSetSaveService;
	}

	@Activated(routeAlias = "hfpay.withdraw.get")
	@GetMapping(value = "/hfpay/getwithdrawset", name = "汇付获取提现设置")
	public ResponseEntity<ApiResult<Object>> index(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> data = hfpayWithdrawSetSaveService.getWithdrawSet(companyId);
		if (data == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "hfpay.withdraw.save")
	@PostMapping(value = "/hfpay/savewithdrawset", name = "汇付提现设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		merged.put("company_id", companyId);
		Map<String, Object> data = hfpayWithdrawSetSaveService.save(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
