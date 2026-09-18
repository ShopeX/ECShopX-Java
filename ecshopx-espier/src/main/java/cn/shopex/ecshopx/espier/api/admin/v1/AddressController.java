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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.espier.service.address.EspierAddressTreeApplicationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("espierAdminV1Address")
@RequestMapping("/api/v1/espier")
public class AddressController {

	private final EspierAddressTreeApplicationService espierAddressTreeApplicationService;

	public AddressController(EspierAddressTreeApplicationService espierAddressTreeApplicationService) {
		this.espierAddressTreeApplicationService = espierAddressTreeApplicationService;
	}

	@Activated(routeAlias = "espier.address.info")
	@GetMapping(value = "/address", name = "获取地址")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> get() {
		return ResponseEntity.ok(ApiResult.ok(espierAddressTreeApplicationService.get()));
	}
}
