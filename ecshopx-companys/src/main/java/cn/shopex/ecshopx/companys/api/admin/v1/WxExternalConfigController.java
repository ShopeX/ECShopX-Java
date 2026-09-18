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
import cn.shopex.ecshopx.companys.service.wxexternalconfig.WxExternalConfigCreateService;
import cn.shopex.ecshopx.companys.service.wxexternalconfig.WxExternalConfigDeleteService;
import cn.shopex.ecshopx.companys.service.wxexternalconfig.WxExternalConfigListService;
import cn.shopex.ecshopx.companys.service.wxexternalconfig.WxExternalConfigRoutesListService;
import cn.shopex.ecshopx.companys.service.wxexternalconfig.WxExternalConfigUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("companysAdminV1WxExternalConfig")
@RequestMapping("/api/v1")
public class WxExternalConfigController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final WxExternalConfigCreateService wxExternalConfigCreateService;
	private final WxExternalConfigUpdateService wxExternalConfigUpdateService;
	private final WxExternalConfigListService wxExternalConfigListService;
	private final WxExternalConfigRoutesListService wxExternalConfigRoutesListService;
	private final WxExternalConfigDeleteService wxExternalConfigDeleteService;

	public WxExternalConfigController(
			WxExternalConfigCreateService wxExternalConfigCreateService,
			WxExternalConfigUpdateService wxExternalConfigUpdateService,
			WxExternalConfigListService wxExternalConfigListService,
			WxExternalConfigRoutesListService wxExternalConfigRoutesListService,
			WxExternalConfigDeleteService wxExternalConfigDeleteService) {
		this.wxExternalConfigCreateService = wxExternalConfigCreateService;
		this.wxExternalConfigUpdateService = wxExternalConfigUpdateService;
		this.wxExternalConfigListService = wxExternalConfigListService;
		this.wxExternalConfigRoutesListService = wxExternalConfigRoutesListService;
		this.wxExternalConfigDeleteService = wxExternalConfigDeleteService;
	}

	@Activated(routeAlias = "wxexternalconfig.list")
	@GetMapping(value = "/wxexternalconfig/list", name = "获取外部小程序配置列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxExternalConfigList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "app_name", required = false) String appName) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data =
				wxExternalConfigListService.getWxExternalConfigList(companyId, appName, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wxexternalconfig.create")
	@PostMapping(value = "/wxexternalconfig/create", name = "创建外部小程序配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> createWxExternalConfig(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = wxExternalConfigCreateService.create(body == null ? Map.of() : body, companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	@Activated(routeAlias = "wxexternalconfig.update")
	@PutMapping(value = "/wxexternalconfig/update/{wx_external_config_id}", name = "更新外部小程序配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateWxExternalConfig(
			HttpServletRequest request,
			@PathVariable("wx_external_config_id") String wxExternalConfigIdRaw,
			@FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data =
				wxExternalConfigUpdateService.updateWxExternalConfig(
						companyId, wxExternalConfigIdRaw, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wxexternalconfig.delete")
	@DeleteMapping(value = "/wxexternalconfig/{wx_external_config_id}", name = "删除外部小程序配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteWxExternalConfig(
			HttpServletRequest request,
			@PathVariable("wx_external_config_id") String wxExternalConfigIdRaw) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		wxExternalConfigDeleteService.deleteWxExternalConfig(companyId, wxExternalConfigIdRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "wxexternalconfigroutes.list")
	@GetMapping(value = "/wxexternalconfigroutes/list", name = "获取外部小程序配置路径列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getConfigRoutesList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize,
			@RequestParam(name = "app_id", required = false) String appId,
			@RequestParam(name = "route_info", required = false) String routeInfo) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String appIdFilter = StringUtils.hasText(appId) ? appId : null;
		String routeInfoFilter = StringUtils.hasText(routeInfo) ? routeInfo : null;
		Map<String, Object> data = wxExternalConfigRoutesListService.getConfigRoutesList(
				companyId, appIdFilter, routeInfoFilter, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
