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

package cn.shopex.ecshopx.pointsmall.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsFrontDetailService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsFrontListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("pointsmallItemsFrontV1")
@RequestMapping("/api/v1/h5app")
public class ItemsController {

	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");
	private static final Pattern POSITIVE_INTEGER_STRING = Pattern.compile("^\\d+$");

	private final PointsmallItemsFrontListService pointsmallItemsFrontListService;
	private final PointsmallItemsFrontDetailService pointsmallItemsFrontDetailService;

	public ItemsController(PointsmallItemsFrontListService pointsmallItemsFrontListService,
			PointsmallItemsFrontDetailService pointsmallItemsFrontDetailService) {
		this.pointsmallItemsFrontListService = pointsmallItemsFrontListService;
		this.pointsmallItemsFrontDetailService = pointsmallItemsFrontDetailService;
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingoValidationEnvelope(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	/**
	 * 与 {@link cn.shopex.ecshopx.common.exception.GlobalExceptionHandler} 中 ResourceException + DINGO 分支对齐；
	 * 若携带 {@link ResourceException#getFieldErrors()} 则输出兼容校验形态（message + errors + status_code）。
	 */
	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<?> handleResourceException(ResourceException ex) {
		Map<String, List<String>> fe = ex.getFieldErrors();
		if (fe != null && !fe.isEmpty()) {
			int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
			return dingoValidationEnvelope(ex.getMessage(), fe, statusCode);
		}
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		if (ex.getEmbeddedBusinessCode() != null) {
			data.put("code", ex.getEmbeddedBusinessCode());
		}
		data.put("status_code", statusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoValidationEnvelope(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	@GetMapping(value = "/wxapp/pointsmall/goods/items", name = "商品列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);

		Map<String, List<String>> fieldErrors = validatePointsmallItemsListPageParams(request);
		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException("获取商品列表出错.", fieldErrors);
		}

		Map<String, Object> data = pointsmallItemsFrontListService.list(request, companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
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

	private static Map<String, List<String>> validatePointsmallItemsListPageParams(HttpServletRequest request) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		validatePointsmallListPage(request.getParameter("page"), fieldErrors);
		validatePointsmallListPageSize(request.getParameter("pageSize"), fieldErrors);
		return fieldErrors;
	}

	private static void validatePointsmallListPage(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("page", List.of("validation.required"));
			return;
		}
		String s = raw.trim();
		if (!POSITIVE_INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("page", List.of("validation.integer"));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("page", List.of("validation.integer"));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("page", List.of("validation.min.numeric"));
			return;
		}
		if (v > Integer.MAX_VALUE) {
			fieldErrors.put("page", List.of("validation.integer"));
		}
	}

	private static void validatePointsmallListPageSize(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("pageSize", List.of("validation.required"));
			return;
		}
		String s = raw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("pageSize", List.of("validation.integer"));
			return;
		}
		int v;
		try {
			v = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("pageSize", List.of("validation.integer"));
			return;
		}
		if (v < 1) {
			fieldErrors.put("pageSize", List.of("validation.min.numeric"));
			return;
		}
		if (v > 50) {
			fieldErrors.put("pageSize", List.of("validation.max.numeric"));
		}
	}

	@GetMapping(value = "/wxapp/pointsmall/lovely/goods/items", name = "猜你喜欢商品列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getLovelyItemsList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);

		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		validateLovelyItemIdParam(request.getParameter("item_id"), fieldErrors);
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException("获取商品列表出错.", fieldErrors);
		}
		long itemId = Long.parseLong(request.getParameter("item_id").trim());

		Map<String, Object> data = pointsmallItemsFrontListService.lovelyList(request, companyId, itemId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void validateLovelyItemIdParam(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("item_id", List.of("validation.required"));
			return;
		}
		String s = raw.trim();
		if (!POSITIVE_INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("item_id", List.of("validation.integer"));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("item_id", List.of("validation.integer"));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("item_id", List.of("validation.min.numeric"));
		}
	}

	@GetMapping(value = "/wxapp/pointsmall/goods/items/{item_id}", name = "商品详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getItemsDetail(@PathVariable("item_id") String itemId, HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		String woaAppid = readWoaAppidFromClaims(request);
		String goodsIdParam = request.getParameter("goods_id");
		Map<String, Object> data = pointsmallItemsFrontDetailService.getDetailPayload(request, companyId, itemId, goodsIdParam, woaAppid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String readWoaAppidFromClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			Object v = m.get("woa_appid");
			return v == null ? "" : String.valueOf(v).trim();
		}
		return "";
	}
}
