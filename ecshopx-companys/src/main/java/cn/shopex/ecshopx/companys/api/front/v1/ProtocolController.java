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

package cn.shopex.ecshopx.companys.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("companysFrontV1Protocol")
@RequestMapping("/api/v1/h5app/wxapp/shops")
public class ProtocolController {

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final ShopProtocolSetService shopProtocolSetService;

	public ProtocolController(ShopProtocolSetService shopProtocolSetService) {
		this.shopProtocolSetService = shopProtocolSetService;
	}

	@GetMapping(value = "/protocol", name = "站点协议信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requirePositiveH5CompanyId(request);
		String typeParam = type == null ? "" : type;
		LinkedHashMap<String, Object> result = new LinkedHashMap<>(5);
		result.put("type", "");
		result.put("title", "");
		result.put("content", "");
		result.put("update_date", "");
		result.put("take_effect_date", "");
		if (!StringUtils.hasText(typeParam)) {
			return ResponseEntity.ok(ApiResult.ok(result));
		}
		String trimmed = typeParam.trim();
		Map<String, Object> data = shopProtocolSetService.get(companyId, trimmed, countryCode);
		Object rawRow = data.get(trimmed);
		if (rawRow instanceof Map<?, ?> src) {
			result.put("type", coalesceString(src.get("type")));
			result.put("title", coalesceString(src.get("title")));
			result.put("content", coalesceString(src.get("content")));
			result.put("update_date", coalesceString(src.get("update_date")));
			result.put("take_effect_date", coalesceString(src.get("take_effect_date")));
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static String coalesceString(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static long requirePositiveH5CompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		return companyId;
	}

	@GetMapping(value = "/protocolUpdateTime", name = "协议发布时间")
	public ResponseEntity<ApiResult<Map<String, Object>>> getUpdateTime(
			HttpServletRequest request,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requirePositiveH5CompanyId(request);
		long t = shopProtocolSetService.getUpdateTime(companyId, countryCode);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
		data.put("update_time", t == 0L ? Integer.valueOf(0) : Long.valueOf(t));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/protocolsaleman", name = "业务员协议")
	public ResponseEntity<ApiResult<Map<String, LinkedHashMap<String, String>>>> protocolsaleman(
			HttpServletRequest request,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requirePositiveH5CompanyId(request);
		LinkedHashMap<String, LinkedHashMap<String, String>> body =
				shopProtocolSetService.protocolsaleman(companyId, countryCode);
		return ResponseEntity.ok(ApiResult.ok(body));
	}
}
