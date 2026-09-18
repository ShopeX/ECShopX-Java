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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingGetService;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingSetService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsCreateService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsDeleteService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSetDefaultService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsUpdateService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSetResourceService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsDetailService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsJwtShopIdWhitelist;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsListService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSetShopStatusService;
import cn.shopex.ecshopx.companys.service.wxshops.WxShopsSyncService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Shops")
@RequestMapping("/api/v1/shops/wxshops")
public class ShopsController {

	/** 与 Dingo 411 错误响应对齐：必选门店参数缺失或无效时写入 {@code data.status_code}。 */
	private static final int WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE = 411;

	private static final int RESOURCE_ID_REQUIRED_DINGO_STATUS_CODE = 411;

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final WxShopsCreateService wxShopsCreateService;
	private final WxShopsUpdateService wxShopsUpdateService;
	private final WxShopsSetDefaultService wxShopsSetDefaultService;
	private final WxShopsSetResourceService wxShopsSetResourceService;
	private final WxShopsSetShopStatusService wxShopsSetShopStatusService;
	private final WxShopsSettingSetService wxShopsSettingSetService;
	private final WxShopsSettingGetService wxShopsSettingGetService;
	private final WxShopsListService wxShopsListService;
	private final WxShopsDetailService wxShopsDetailService;
	private final WxShopsJwtShopIdWhitelist wxShopsJwtShopIdWhitelist;
	private final WxShopsSyncService wxShopsSyncService;
	private final WxShopsDeleteService wxShopsDeleteService;
	private final ObjectMapper objectMapper;

	public ShopsController(
			WxShopsCreateService wxShopsCreateService,
			WxShopsUpdateService wxShopsUpdateService,
			WxShopsSetDefaultService wxShopsSetDefaultService,
			WxShopsSetResourceService wxShopsSetResourceService,
			WxShopsSetShopStatusService wxShopsSetShopStatusService,
			WxShopsSettingSetService wxShopsSettingSetService,
			WxShopsSettingGetService wxShopsSettingGetService,
			WxShopsListService wxShopsListService,
			WxShopsDetailService wxShopsDetailService,
			WxShopsJwtShopIdWhitelist wxShopsJwtShopIdWhitelist,
			WxShopsSyncService wxShopsSyncService,
			WxShopsDeleteService wxShopsDeleteService,
			ObjectMapper objectMapper) {
		this.wxShopsCreateService = wxShopsCreateService;
		this.wxShopsUpdateService = wxShopsUpdateService;
		this.wxShopsSetDefaultService = wxShopsSetDefaultService;
		this.wxShopsSetResourceService = wxShopsSetResourceService;
		this.wxShopsSetShopStatusService = wxShopsSetShopStatusService;
		this.wxShopsSettingSetService = wxShopsSettingSetService;
		this.wxShopsSettingGetService = wxShopsSettingGetService;
		this.wxShopsListService = wxShopsListService;
		this.wxShopsDetailService = wxShopsDetailService;
		this.wxShopsJwtShopIdWhitelist = wxShopsJwtShopIdWhitelist;
		this.wxShopsSyncService = wxShopsSyncService;
		this.wxShopsDeleteService = wxShopsDeleteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "shops.create")
	@PostMapping(name = "添加微信门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> createWxShops(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = wxShopsCreateService.createWxShop(merged, jwt, companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "shops.sync")
	@GetMapping(value = "/sync", name = "同步微信门店到本地")
	public ResponseEntity<Void> syncWxShops(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Object rawAid = jwt.get("authorizer_appid");
		if (rawAid == null || rawAid.toString().trim().isEmpty()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		String authorizerAppid = rawAid.toString().trim();
		wxShopsSyncService.syncWxShops(companyId, authorizerAppid);
		return ResponseEntity.noContent().build();
	}

	@Activated(routeAlias = "shops.setting.get")
	@GetMapping(value = "/setting", name = "获取门店通用配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxShopsSetting(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> merged =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, Collections.emptyMap());
		Object countryRaw = merged.get("country_code");
		String countryCodeRaw;
		if (countryRaw == null) {
			countryCodeRaw = null;
		} else if (countryRaw instanceof String s) {
			countryCodeRaw = s;
		} else {
			countryCodeRaw = String.valueOf(countryRaw);
		}
		Map<String, Object> data = wxShopsSettingGetService.getWxShopsSetting(companyId, countryCodeRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Updates tenant-wide WeChat shop display defaults from the request body and query; persists settings
	 * after optional self-distributor brand and logo sync delegated to {@link WxShopsSettingSetService}.
	 */
	@Activated(routeAlias = "shops.setting.set")
	@PutMapping(value = "/setting", name = "配置门店通用配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setWxShopsSetting(
			HttpServletRequest request, @RequestBody(required = false) String rawJsonBody) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> bodyMap =
				CompanysAdminRequestMerge.parseWxShopsSettingJsonBodyToBodyMap(objectMapper, rawJsonBody);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, bodyMap);
		Map<String, Object> normalizedMerged =
				CompanysAdminRequestMerge.buildWxShopsSettingNormalizedMerged(merged);
		wxShopsSettingSetService.setWxShopsSetting(companyId, normalizedMerged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "shops.lists")
	@GetMapping(name = "获取微信门店列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxShopsList(
			HttpServletRequest request,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "pageSize", required = false, defaultValue = "1000") Integer pageSize,
			@RequestParam(name = "isValid", required = false) String isValid,
			@RequestParam(name = "is_valid", required = false) String isValidUnderscore,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "distributor_id", required = false) Long distributorId,
			@RequestParam(name = "province", required = false) String province,
			@RequestParam(name = "city", required = false) String city,
			@RequestParam(name = "area", required = false) String area,
			@RequestParam(name = "poi_id", required = false) String poiId,
			@RequestParam(name = "wx_shop_id", required = false) String wxShopId,
			@RequestParam(name = "is_direct_store", required = false) String isDirectStore) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> data =
				wxShopsListService.getWxShopsList(
						companyId,
						jwt,
						page,
						pageSize,
						isValid,
						isValidUnderscore,
						name,
						distributorId,
						province,
						city,
						area,
						poiId,
						wxShopId,
						isDirectStore);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "shops.defaultshop.set")
	@PostMapping(value = "/setDefaultShop", name = "设置默认门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> setDefaultShop(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object wxShopIdRaw = merged.get("wx_shop_id");
		long wxShopId = parsePositiveWxShopIdOrThrow(wxShopIdRaw);
		wxShopsSetDefaultService.setDefaultWxShop(companyId, wxShopId, jwt);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "shops.shopresource.set")
	@PostMapping(value = "/setShopResource", name = "激活门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> setResource(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long wxShopId = parsePositiveWxShopIdOrThrow(merged.get("wx_shop_id"));
		long resourceId = parsePositiveResourceIdOrThrow(merged.get("resource_id"));
		Map<String, Object> status =
				wxShopsSetResourceService.setShopResource(companyId, wxShopId, resourceId, jwt);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", status);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "shops.detail")
	@GetMapping(value = "/{wx_shop_id}", name = "获取单个微信门店详情")
	public ResponseEntity<ApiResult<Object>> getWxShopsDetail(
			HttpServletRequest request, @PathVariable("wx_shop_id") String wxShopIdPath) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		List<Long> allowed = wxShopsJwtShopIdWhitelist.allowedShopIds(jwt.get("shop_ids"));
		if (!allowed.isEmpty()) {
			Long pathId = null;
			try {
				String trimmed = wxShopIdPath == null ? "" : wxShopIdPath.trim();
				if (!trimmed.isEmpty()) {
					pathId = Long.parseLong(trimmed);
				}
			} catch (NumberFormatException e) {
				pathId = null;
			}
			if (pathId == null || !allowed.contains(pathId)) {
				return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
			}
		}
		String t = wxShopIdPath == null ? "" : wxShopIdPath.trim();
		if (t.isEmpty()) {
			throw new ResourceException(
					"获取门店详情出错.", Map.of("wx_shop_id", List.of("validation.required")));
		}
		long wxShopId;
		try {
			wxShopId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException(
					"获取门店详情出错.", Map.of("wx_shop_id", List.of("validation.integer")));
		}
		if (wxShopId < 1L) {
			throw new ResourceException(
					"获取门店详情出错.", Map.of("wx_shop_id", List.of("validation.min.numeric")));
		}
		Map<String, Object> result = wxShopsDetailService.getWxShopsDetail(wxShopId);
		Object cidObj = result.get("company_id");
		if (cidObj == null) {
			throw new ResourceException("获取门店信息有误，请确认您的门店的ID.");
		}
		long rowCompanyId;
		try {
			rowCompanyId = cidObj instanceof Number n ? n.longValue() : Long.parseLong(cidObj.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("获取门店信息有误，请确认您的门店的ID.");
		}
		if (companyId != rowCompanyId) {
			throw new ResourceException("获取门店信息有误，请确认您的门店的ID.");
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "shops.delete")
	@DeleteMapping(value = "/{wx_shop_id}", name = "删除微信门店")
	public ResponseEntity<Void> deleteWxShops(HttpServletRequest request, @PathVariable("wx_shop_id") String wxShopId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String t = wxShopId == null ? "" : wxShopId.trim();
		long parsedId;
		try {
			parsedId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException(
					"删除门店出错.", Map.of("wx_shop_id", List.of("validation.integer")));
		}
		if (parsedId < 1L) {
			throw new ResourceException(
					"删除门店出错.", Map.of("wx_shop_id", List.of("validation.min.numeric")));
		}
		wxShopsDeleteService.deleteWxShops(parsedId, jwt, companyId);
		return ResponseEntity.noContent().build();
	}

	@Activated(routeAlias = "shops.update")
	@PutMapping(value = "/{wx_shop_id}", name = "更新微信门店")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateWxShops(
			HttpServletRequest request,
			@PathVariable("wx_shop_id") String wxShopIdPath,
			@FlexibleBody Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long wxShopId = parsePositiveWxShopIdOrThrow(wxShopIdPath);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = wxShopsUpdateService.updateWxShops(wxShopId, merged, jwt, companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "shops.status.set")
	@PostMapping(value = "/setShopStatus", name = "设置门店状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> setShopStatus(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		Map<String, Object> merged = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		wxShopsSetShopStatusService.setShopStatus(merged.get("wx_shop_id"), merged.get("status"), jwt);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long parsePositiveWxShopIdOrThrow(Object v) {
		if (isMissingWxShopIdLikeInput(v)) {
			throw new BadRequestException("门店必选！", WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE);
		}
		try {
			long id;
			if (v instanceof Number n) {
				id = n.longValue();
			} else {
				id = Long.parseLong(v.toString().trim());
			}
			if (id <= 0L) {
				throw new BadRequestException("门店必选！", WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE);
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("门店必选！", WX_SHOP_ID_REQUIRED_DINGO_STATUS_CODE);
		}
	}

	private static long parsePositiveResourceIdOrThrow(Object v) {
		if (isMissingResourceIdLikeInput(v)) {
			throw new BadRequestException("资源包必选", RESOURCE_ID_REQUIRED_DINGO_STATUS_CODE);
		}
		try {
			long id;
			if (v instanceof Number n) {
				id = n.longValue();
			} else {
				id = Long.parseLong(v.toString().trim());
			}
			if (id <= 0L) {
				throw new BadRequestException("资源包必选", RESOURCE_ID_REQUIRED_DINGO_STATUS_CODE);
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("资源包必选", RESOURCE_ID_REQUIRED_DINGO_STATUS_CODE);
		}
	}

	private static boolean isMissingResourceIdLikeInput(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static boolean isMissingWxShopIdLikeInput(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
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
}
