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
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Protocol")
@RequestMapping("/api/v1/shops")
public class ProtocolController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ShopProtocolSetService shopProtocolSetService;

	public ProtocolController(ShopProtocolSetService shopProtocolSetService) {
		this.shopProtocolSetService = shopProtocolSetService;
	}

	@Activated(routeAlias = "shops.protocol.put")
	@PutMapping(value = "/protocol", name = "更新协议信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> set(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			merged.put(name, request.getParameter(name));
		}
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = readCompanyIdFromOperatorJwt(request);
		shopProtocolSetService.set(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
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

	@Activated(routeAlias = "shops.protocol.get")
	@GetMapping(value = "/protocol", name = "获取协议信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = shopProtocolSetService.get(companyId, type, countryCode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
