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

package cn.shopex.ecshopx.goods.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.QywxSalespersonAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.QywxSalespersonAuthAttributes;
import cn.shopex.ecshopx.goods.service.salesperson.SalespersonItemsDetailService;
import cn.shopex.ecshopx.goods.service.salesperson.SalespersonItemsListService;
import cn.shopex.ecshopx.goods.service.salesperson.SalespersonItemsListServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@QywxSalespersonAuth
@RestController("goodsSalespersonItemsFrontV1")
@RequestMapping("/api/v1/h5app")
public class SalespersonItemsController {

	private final SalespersonItemsListService salespersonItemsListService;
	private final SalespersonItemsDetailService salespersonItemsDetailService;

	public SalespersonItemsController(SalespersonItemsListService salespersonItemsListService,
			SalespersonItemsDetailService salespersonItemsDetailService) {
		this.salespersonItemsListService = salespersonItemsListService;
		this.salespersonItemsDetailService = salespersonItemsDetailService;
	}

	@GetMapping(value = "/wxapp/goods/salesperson/items", name = "获取商品列表")
	public ResponseEntity<ApiResult<Object>> getItemList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		Object out = salespersonItemsListService.execute(companyId, userId, request);
		if (out == SalespersonItemsListServiceImpl.EMPTY_TOP_LEVEL_ARRAY) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) out;
		return ResponseEntity.ok(ApiResult.ok(dataMap));
	}

	@GetMapping(value = "/wxapp/goods/salesperson/itemsinfo", name = "获取商品详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsDetail(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseOptionalUserIdFromRequest(request);
		long goodsId = parseLongParam(request, "goods_id", 0L);
		long itemId = parseLongParam(request, "item_id", 0L);
		long distributorId = parseDistributorIdParam(request);
		String woaAppid = parseOptionalWoaAppidFromRequest(request);
		Map<String, Object> data = salespersonItemsDetailService.execute(companyId, userId, goodsId, itemId, distributorId, woaAppid, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseLongParam(HttpServletRequest request, String name, long defaultVal) {
		String v = request.getParameter(name);
		if (!StringUtils.hasText(v)) {
			return defaultVal;
		}
		try {
			return Long.parseLong(v.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static long parseDistributorIdParam(HttpServletRequest request) {
		String raw = request.getParameter("distributor_id");
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t) || "undefined".equalsIgnoreCase(t) || "null".equalsIgnoreCase(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@SuppressWarnings("unchecked")
	private static String parseOptionalWoaAppidFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return "";
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object woa = claims.get("woa_appid");
		if (woa == null) {
			return "";
		}
		return woa.toString().trim();
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> auth = (Map<String, Object>) rawMap;
		Object companyObj = auth.get("company_id");
		long companyId;
		if (companyObj instanceof Number n) {
			companyId = n.longValue();
		} else if (companyObj instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	@SuppressWarnings("unchecked")
	private static long parseOptionalUserIdFromRequest(HttpServletRequest request) {
		Object rawClaims = request.getAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR);
		if (!(rawClaims instanceof Map<?, ?> rawMap)) {
			return 0L;
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			return 0L;
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
