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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthCreateService;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthDelService;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthDetailService;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthEnableService;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthListService;
import cn.shopex.ecshopx.companys.service.regionauth.RegionauthUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
		notFound = false)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Regionauth")
@RequestMapping("/api/v1")
public class RegionauthController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final RegionauthCreateService regionauthCreateService;
	private final RegionauthEnableService regionauthEnableService;
	private final RegionauthUpdateService regionauthUpdateService;
	private final RegionauthDelService regionauthDelService;
	private final RegionauthListService regionauthListService;
	private final RegionauthDetailService regionauthDetailService;

	public RegionauthController(
			RegionauthCreateService regionauthCreateService,
			RegionauthEnableService regionauthEnableService,
			RegionauthUpdateService regionauthUpdateService,
			RegionauthDelService regionauthDelService,
			RegionauthListService regionauthListService,
			RegionauthDetailService regionauthDetailService) {
		this.regionauthCreateService = regionauthCreateService;
		this.regionauthEnableService = regionauthEnableService;
		this.regionauthUpdateService = regionauthUpdateService;
		this.regionauthDelService = regionauthDelService;
		this.regionauthListService = regionauthListService;
		this.regionauthDetailService = regionauthDetailService;
	}

	@Activated(routeAlias = "company.regionauth.list")
	@GetMapping(value = "/regionauth", produces = MediaType.APPLICATION_JSON_VALUE, name = "地区权限列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getlist(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize,
			@RequestParam(name = "regionauth_id", required = false) String regionauthIdRaw,
			@RequestParam(name = "state", required = false) String stateRaw) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Long regionauthIdOrNull = null;
		if (regionauthIdRaw != null && !regionauthIdRaw.isBlank()) {
			try {
				regionauthIdOrNull = Long.parseLong(regionauthIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数错误");
			}
		}
		boolean stateParamPresent = stateRaw != null;
		Integer stateOrNull = null;
		if (stateParamPresent) {
			String trimmed = stateRaw.trim();
			if (trimmed.isEmpty()) {
				throw new BadRequestException("参数错误");
			}
			try {
				stateOrNull = Integer.parseInt(trimmed);
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数错误");
			}
		}
		Map<String, Object> data =
				regionauthListService.getlist(
						companyId, page, pageSize, regionauthIdOrNull, stateOrNull, stateParamPresent);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "company.regionauth.info")
	@GetMapping(
			value = "/regionauth/{id:[0-9]+}",
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "地区权限详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getinfo(
			HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		long rid;
		try {
			rid = Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
		Map<String, Object> data = regionauthDetailService.getinfo(companyId, rid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "company.regionauth.add")
	@PostMapping(value = "/regionauth", name = "地区权限添加")
	public ResponseEntity<ApiResult<Map<String, Object>>> add(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		regionauthCreateService.create(body == null ? Map.of() : body, companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "company.regionauth.update")
	@PutMapping(value = "/regionauth/{id}", name = "地区权限修改")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String regionauthIdPath = id == null ? "" : id;
		Map<String, Object> params = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			for (Map.Entry<String, Object> e : body.entrySet()) {
				params.put(e.getKey(), e.getValue());
			}
		}
		Object regionauthNameRaw = params.get("regionauth_name");
		regionauthUpdateService.update(companyId, regionauthIdPath, regionauthNameRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "company.regionauth.dell")
	@DeleteMapping(value = "/regionauth/{id}", name = "地区权限删除")
	public ResponseEntity<ApiResult<Map<String, Object>>> del(
			HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String regionauthIdPath = id == null ? "" : id;
		regionauthDelService.del(companyId, regionauthIdPath);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "company.regionauth.enable")
	@PutMapping(value = "/regionauth/enable/{id}", name = "地区权限状态操作")
	public ResponseEntity<ApiResult<Map<String, Object>>> enable(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		String regionauthIdPath = id == null ? "" : id;
		Map<String, Object> params = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			for (Map.Entry<String, Object> e : body.entrySet()) {
				params.put(e.getKey(), e.getValue());
			}
		}
		Object enableRaw = params.get("enable");
		regionauthEnableService.enable(companyId, regionauthIdPath, enableRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
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
}
