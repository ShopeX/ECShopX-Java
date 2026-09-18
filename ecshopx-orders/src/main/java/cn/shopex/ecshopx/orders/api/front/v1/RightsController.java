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

package cn.shopex.ecshopx.orders.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.wxapp.RightsFrontWxappGetRightsCodeService;
import cn.shopex.ecshopx.orders.service.front.wxapp.RightsFrontWxappGetRightsDetailService;
import cn.shopex.ecshopx.orders.service.front.wxapp.RightsFrontWxappGetRightsListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.RightsFrontWxappGetRightsLogListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@FrontAuth
@RestController("ordersFrontV1Rights")
@RequestMapping("/api/v1/h5app/wxapp")
public class RightsController {

	private final RightsFrontWxappGetRightsListService rightsFrontWxappGetRightsListService;
	private final RightsFrontWxappGetRightsDetailService rightsFrontWxappGetRightsDetailService;
	private final RightsFrontWxappGetRightsLogListService rightsFrontWxappGetRightsLogListService;
	private final RightsFrontWxappGetRightsCodeService rightsFrontWxappGetRightsCodeService;

	public RightsController(
			RightsFrontWxappGetRightsListService rightsFrontWxappGetRightsListService,
			RightsFrontWxappGetRightsDetailService rightsFrontWxappGetRightsDetailService,
			RightsFrontWxappGetRightsLogListService rightsFrontWxappGetRightsLogListService,
			RightsFrontWxappGetRightsCodeService rightsFrontWxappGetRightsCodeService) {
		this.rightsFrontWxappGetRightsListService = rightsFrontWxappGetRightsListService;
		this.rightsFrontWxappGetRightsDetailService = rightsFrontWxappGetRightsDetailService;
		this.rightsFrontWxappGetRightsLogListService = rightsFrontWxappGetRightsLogListService;
		this.rightsFrontWxappGetRightsCodeService = rightsFrontWxappGetRightsCodeService;
	}

	@GetMapping(
			value = "/rightsLogs",
			name = "核销记录",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getRightsLogList(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> body = rightsFrontWxappGetRightsLogListService.getRightsLogList(request, auth);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@GetMapping(value = "/rights", name = "权益列表")
	public ApiResult<Map<String, Object>> getRightsList(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return ApiResult.ok(rightsFrontWxappGetRightsListService.getRightsList(request, auth));
	}

	@GetMapping(value = "/rights/{rights_id}", name = "权益详情")
	public ApiResult<Map<String, Object>> getRightsDetail(
			@PathVariable("rights_id") String rightsId, HttpServletRequest request) {
		Object rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return ApiResult.ok(rightsFrontWxappGetRightsDetailService.getRightsDetail(rightsId, auth));
	}

	@GetMapping(
			value = "/rightscode/{rights_id}",
			name = "核销码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getRightsCode(@PathVariable("rights_id") String rightsId) {
		Map<String, Object> body = rightsFrontWxappGetRightsCodeService.getRightsCode(rightsId);
		return ResponseEntity.ok(body);
	}
}
