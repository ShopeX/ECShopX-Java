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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.service.wxshops.H5WxShopsDetailService;
import cn.shopex.ecshopx.companys.service.wxshops.H5WxShopsListQueryService;
import cn.shopex.ecshopx.companys.service.wxshops.H5WxShopsSettingGetService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsBaseInfoService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsNearestService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("companysFrontV1ShopsWxapp")
@RequestMapping("/api/v1/h5app/wxapp/shops")
public class ShopsController {

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private static final String MSG_WX_SHOPS_LIST_BAD_REQUEST = "获取微信门店列表出错.";

	private static final String MSG_WX_SHOPS_DETAIL_BAD_REQUEST = "获取门店详情出错.";

	private final WxShopsNearestService wxShopsNearestService;
	private final WxShopsBaseInfoService wxShopsBaseInfoService;
	private final H5WxShopsSettingGetService h5WxShopsSettingGetService;
	private final H5WxShopsListQueryService h5WxShopsListQueryService;
	private final H5WxShopsDetailService h5WxShopsDetailService;

	public ShopsController(
			WxShopsNearestService wxShopsNearestService,
			WxShopsBaseInfoService wxShopsBaseInfoService,
			H5WxShopsSettingGetService h5WxShopsSettingGetService,
			H5WxShopsListQueryService h5WxShopsListQueryService,
			H5WxShopsDetailService h5WxShopsDetailService) {
		this.wxShopsNearestService = wxShopsNearestService;
		this.wxShopsBaseInfoService = wxShopsBaseInfoService;
		this.h5WxShopsSettingGetService = h5WxShopsSettingGetService;
		this.h5WxShopsListQueryService = h5WxShopsListQueryService;
		this.h5WxShopsDetailService = h5WxShopsDetailService;
	}

	/**
	 * DINGO Resource 默认响应不含 {@code errors}；携带 {@link ResourceException#getFieldErrors()} 时在此写出
	 * {@code message}、{@code errors}、{@code status_code} 信封（HTTP 200）。
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

	/**
	 * 与 {@link ResourceException} 路径一致：带 {@code fieldErrors} 时写出 Dingo 校验信封（HTTP 200、
	 * {@code data.status_code} 默认 422），避免全局 {@code BadRequestException} 分支不展开 {@code errors}。
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequestExceptionForWxShopsList(BadRequestException ex) {
		Map<String, List<String>> fe = ex.getFieldErrors();
		if (fe != null && !fe.isEmpty()) {
			int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
			return dingoValidationEnvelope(ex.getMessage(), fe, statusCode);
		}
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 400;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		data.put("status_code", statusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoValidationEnvelope(
			String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	@GetMapping(value = "/wxshops", produces = MediaType.APPLICATION_JSON_VALUE, name = "获取微信门店列表")
	public ResponseEntity<?> getWxShopsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(name = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(name = "lat", required = false) String lat,
			@RequestParam(name = "lng", required = false) String lng) {
		validateH5WxShopsListPaging(pageRaw, pageSizeRaw);
		long companyId = requirePositiveH5CompanyId(request);
		boolean distanceBranch = request.getParameterMap().containsKey("lat")
				&& request.getParameterMap().containsKey("lng")
				&& queryParamTruthyForLatLng(lat)
				&& queryParamTruthyForLatLng(lng);
		int page = Integer.parseInt(pageRaw.trim());
		long distributorId = parseLenientDistributorId(distributorIdRaw);
		Map<String, Object> payload =
				h5WxShopsListQueryService.getWxShopsList(
						companyId, page, distributorId, distanceBranch, lat, lng);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("data", payload);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(root);
	}

	@GetMapping(
			value = "/wxshops/{wx_shop_id}",
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "获取单个微信门店详情")
	public ResponseEntity<?> getWxShopsDetail(
			HttpServletRequest request, @PathVariable("wx_shop_id") String wxShopIdPath) {
		long wxShopId = parsePositiveWxShopIdForH5DetailOrThrow(wxShopIdPath);
		String lang = WxShopsBaseInfoService.resolveCountryCodeForWxSetting(request);
		Map<String, Object> payload = h5WxShopsDetailService.getWxShopsDetail(wxShopId, lang);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("data", payload);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(root);
	}

	@GetMapping(
			value = "/getNearestWxShops",
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "获取最近门店")
	public ResponseEntity<?> getNearestWxShops(
			HttpServletRequest request,
			@RequestParam(value = "lng", required = false) String lng,
			@RequestParam(value = "lat", required = false) String lat) {
		validateLngLatQueryParams(request);
		long companyId = requirePositiveH5CompanyId(request);
		boolean coordinateBranch = latOrLngQueryPresent(lat) || latOrLngQueryPresent(lng);
		WxShopsNearestService.NearestWxShopsResult r =
				wxShopsNearestService.getNearestWxShops(companyId, coordinateBranch, lat, lng);
		Map<String, Object> root = new LinkedHashMap<>();
		if (!r.coordinateBranch()) {
			root.put("data", r.payloadAsList());
		} else {
			root.put("data", r.payloadAsCoordBody());
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(root);
	}

	@GetMapping(
			value = "/info",
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "获取站点基本信息")
	public ResponseEntity<?> getBaseInfo(HttpServletRequest request) {
		long companyId = requirePositiveH5CompanyId(request);
		String countryCode = WxShopsBaseInfoService.resolveCountryCodeForWxSetting(request);
		Map<String, Object> payload = wxShopsBaseInfoService.getBaseInfo(companyId, countryCode);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("data", payload);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(root);
	}

	@GetMapping(
			value = "/setting",
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "门店通用配置")
	public ResponseEntity<?> getWxShopsSetting(
			HttpServletRequest request,
			@RequestParam(value = "domain", required = false) String domain) {
		String lang = WxShopsBaseInfoService.resolveCountryCodeForWxSetting(request);
		Object payload = h5WxShopsSettingGetService.getWxShopsSetting(domain, lang);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("data", payload);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(root);
	}

	private static void validateLngLatQueryParams(HttpServletRequest request) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		for (String p : List.of("lng", "lat")) {
			if (!request.getParameterMap().containsKey(p)) {
				continue;
			}
			String v = request.getParameter(p);
			if (v == null || v.trim().isEmpty()) {
				fieldErrors.put(p, List.of("validation.required"));
				continue;
			}
			try {
				double parsed = Double.parseDouble(v.trim());
				if (!Double.isFinite(parsed)) {
					fieldErrors.put(p, List.of("validation.numeric"));
				}
			} catch (NumberFormatException e) {
				fieldErrors.put(p, List.of("validation.numeric"));
			}
		}
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException("经纬度范围错误.", fieldErrors);
		}
	}

	private static boolean latOrLngQueryPresent(String raw) {
		if (raw == null) {
			return false;
		}
		if (raw.isEmpty()) {
			return false;
		}
		return !raw.equals("0");
	}

	private static boolean queryParamTruthyForLatLng(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			double d = Double.parseDouble(t);
			return Double.isFinite(d) && d != 0.0d;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parsePositiveWxShopIdForH5DetailOrThrow(String wxShopIdPath) {
		String t = wxShopIdPath == null ? "" : wxShopIdPath.trim();
		if (t.isEmpty()) {
			throw new BadRequestException(
					MSG_WX_SHOPS_DETAIL_BAD_REQUEST, Map.of("wx_shop_id", List.of("validation.required")));
		}
		long wxShopId;
		try {
			wxShopId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					MSG_WX_SHOPS_DETAIL_BAD_REQUEST, Map.of("wx_shop_id", List.of("validation.integer")));
		}
		if (wxShopId < 1L) {
			throw new BadRequestException(
					MSG_WX_SHOPS_DETAIL_BAD_REQUEST, Map.of("wx_shop_id", List.of("validation.min.numeric")));
		}
		return wxShopId;
	}

	private static void validateH5WxShopsListPaging(String pageRaw, String pageSizeRaw) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			fieldErrors.put("page", List.of("validation.required"));
		} else {
			try {
				int pv = Integer.parseInt(pageRaw.trim());
				if (pv < 1) {
					fieldErrors.put("page", List.of("validation.min.numeric"));
				}
			} catch (NumberFormatException e) {
				fieldErrors.put("page", List.of("validation.integer"));
			}
		}
		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			fieldErrors.put("pageSize", List.of("validation.required"));
		} else {
			try {
				int psv = Integer.parseInt(pageSizeRaw.trim());
				if (psv < 1) {
					fieldErrors.put("pageSize", List.of("validation.min.numeric"));
				} else if (psv > 50) {
					fieldErrors.put("pageSize", List.of("validation.max.numeric"));
				}
			} catch (NumberFormatException e) {
				fieldErrors.put("pageSize", List.of("validation.integer"));
			}
		}
		if (!fieldErrors.isEmpty()) {
			throw new BadRequestException(MSG_WX_SHOPS_LIST_BAD_REQUEST, fieldErrors);
		}
	}

	/** 非数字或空串按 0 处理，与 H5 Action 对 {@code distributor_id} 的宽松行为一致。 */
	private static long parseLenientDistributorId(String distributorIdRaw) {
		String distTrim = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		if (distTrim.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(distTrim);
		} catch (NumberFormatException e) {
			return 0L;
		}
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
}
