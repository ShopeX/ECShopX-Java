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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.service.AdapayDistributorListService;
import cn.shopex.ecshopx.adapay.service.AdapayTradeInfoService;
import cn.shopex.ecshopx.adapay.service.AdapayTradeListQueryService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("adapayTradeAdminV1")
@RequestMapping("/api/v1/adapay")
public class AdapayTradeController {

	private final AdapayDistributorListService adapayDistributorListService;
	private final AdapayTradeInfoService adapayTradeInfoService;
	private final AdapayTradeListQueryService adapayTradeListQueryService;

	public AdapayTradeController(
			AdapayDistributorListService adapayDistributorListService,
			AdapayTradeInfoService adapayTradeInfoService,
			AdapayTradeListQueryService adapayTradeListQueryService) {
		this.adapayDistributorListService = adapayDistributorListService;
		this.adapayTradeInfoService = adapayTradeInfoService;
		this.adapayTradeListQueryService = adapayTradeListQueryService;
	}

	@Activated(routeAlias = "adapay.trade.getList")
	@GetMapping(value = "/trade/list", name = "交易单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTradelist(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Map<String, Object> data = adapayTradeListQueryService.getTradelist(jwtMap, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.tradeInfo.get")
	@GetMapping(value = "/trade/info/{trade_id}", name = "交易单详情")
	public ResponseEntity<ApiResult<Object>> getTradeInfo(
			@PathVariable("trade_id") String tradeId,
			HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object body = adapayTradeInfoService.getTradeInfo(tradeId, jwtMap);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "adapay.distributor.list")
	@GetMapping(value = "/distributor/list", name = "获取店铺列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Map<String, Object> data = adapayDistributorListService.getDistributorList(jwtMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
