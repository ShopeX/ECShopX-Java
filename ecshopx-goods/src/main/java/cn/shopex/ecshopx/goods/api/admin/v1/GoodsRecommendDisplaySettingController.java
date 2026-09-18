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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendDisplaySettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("goodsAdminV1GoodsRecommendDisplaySetting")
@RequestMapping("/api/v1/goods/recommend-display-settings")
public class GoodsRecommendDisplaySettingController {

	private final GoodsRecommendDisplaySettingService displaySettingService;

	public GoodsRecommendDisplaySettingController(GoodsRecommendDisplaySettingService displaySettingService) {
		this.displaySettingService = displaySettingService;
	}

	@Activated(routeAlias = "goods.recommend.display_settings.get")
	@GetMapping(name = "读取商品推荐展示设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDisplaySettings(HttpServletRequest request) {
		long companyId = readCompanyId(request);
		return ResponseEntity.ok(ApiResult.ok(displaySettingService.getDisplaySetting(companyId)));
	}

	@Activated(routeAlias = "goods.recommend.display_settings.save")
	@PutMapping(name = "保存商品推荐展示设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveDisplaySettings(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		Map<String, Object> input = mergeInput(request, body);
		return ResponseEntity.ok(ApiResult.ok(displaySettingService.saveDisplaySetting(companyId, input)));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		data.put("status_code", statusCode);
		if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
			data.put("errors", ex.getFieldErrors());
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}
}
