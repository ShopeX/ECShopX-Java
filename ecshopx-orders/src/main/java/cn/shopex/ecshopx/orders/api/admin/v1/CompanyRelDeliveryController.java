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

package cn.shopex.ecshopx.orders.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.orders.service.companyreldelivery.CompanyRelDeliveryAdminSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
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
@RestController("ordersAdminV1CompanyRelDelivery")
@RequestMapping("/api/v1/company/delivery")
public class CompanyRelDeliveryController {

	private final CompanyRelDeliveryAdminSaveService companyRelDeliveryAdminSaveService;

	public CompanyRelDeliveryController(CompanyRelDeliveryAdminSaveService companyRelDeliveryAdminSaveService) {
		this.companyRelDeliveryAdminSaveService = companyRelDeliveryAdminSaveService;
	}

	@Activated(routeAlias = "company.delivery.info")
	@GetMapping(name = "同城配自配信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getInfo(HttpServletRequest request) {
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = companyRelDeliveryAdminSaveService.getInfo(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "company.delivery.save")
	@PostMapping(name = "保存同城配自配", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromJwt(request);
		Map<String, Object> data = companyRelDeliveryAdminSaveService.save(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
