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

package cn.shopex.ecshopx.members.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.admin.MembersWhitelistCreateDataService;
import cn.shopex.ecshopx.members.service.admin.MembersWhitelistDeleteDataService;
import cn.shopex.ecshopx.members.service.admin.MembersWhitelistGetInfoService;
import cn.shopex.ecshopx.members.service.admin.MembersWhitelistGetListsService;
import cn.shopex.ecshopx.members.service.admin.MembersWhitelistUpdateDataService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("membersAdminV1Whitelist")
@RequestMapping("/api/v1/members/whitelist")
public class MembersWhitelistController {

	private final MembersWhitelistCreateDataService membersWhitelistCreateDataService;
	private final MembersWhitelistUpdateDataService membersWhitelistUpdateDataService;
	private final MembersWhitelistGetListsService membersWhitelistGetListsService;
	private final MembersWhitelistGetInfoService membersWhitelistGetInfoService;
	private final MembersWhitelistDeleteDataService membersWhitelistDeleteDataService;

	public MembersWhitelistController(
			MembersWhitelistCreateDataService membersWhitelistCreateDataService,
			MembersWhitelistUpdateDataService membersWhitelistUpdateDataService,
			MembersWhitelistGetListsService membersWhitelistGetListsService,
			MembersWhitelistGetInfoService membersWhitelistGetInfoService,
			MembersWhitelistDeleteDataService membersWhitelistDeleteDataService) {
		this.membersWhitelistCreateDataService = membersWhitelistCreateDataService;
		this.membersWhitelistUpdateDataService = membersWhitelistUpdateDataService;
		this.membersWhitelistGetListsService = membersWhitelistGetListsService;
		this.membersWhitelistGetInfoService = membersWhitelistGetInfoService;
		this.membersWhitelistDeleteDataService = membersWhitelistDeleteDataService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "member.whitelist.list")
	@GetMapping(value = "/list", name = "白名单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLists(HttpServletRequest request) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (merged.get("page") == null || !StringUtils.hasText(String.valueOf(merged.get("page")).trim())) {
			merged.put("page", "1");
		}
		if (merged.get("pageSize") == null || !StringUtils.hasText(String.valueOf(merged.get("pageSize")).trim())) {
			merged.put("pageSize", "20");
		}

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		return ResponseEntity.ok(ApiResult.ok(membersWhitelistGetListsService.getLists(companyId, merged)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.whitelist.info")
	@GetMapping(value = "/{id}", name = "白名单详情")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Object>> getInfo(
			HttpServletRequest request,
			@PathVariable("id") String pathId) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Object raw = merged.get("id");
		String effectiveId = null;
		if (raw != null) {
			String q = String.valueOf(raw).trim();
			if (StringUtils.hasText(q)) {
				effectiveId = q;
			}
		}
		if (effectiveId == null || "0".equals(effectiveId)) {
			throw new ResourceException("id必填");
		}

		return ResponseEntity.ok(ApiResult.ok(membersWhitelistGetInfoService.getInfo(companyId, effectiveId)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@PostMapping(name = "创建白名单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String mobileStr = resolveMergedScalarString("mobile", merged);
		String nameStr = resolveMergedScalarString("name", merged);

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		return ResponseEntity.ok(
				ApiResult.ok(membersWhitelistCreateDataService.createData(companyId, mobileStr, nameStr)));
	}

	private static String resolveMergedScalarString(String key, Map<String, Object> merged) {
		Object v = merged.get(key);
		if (v == null) {
			return null;
		}
		if (v instanceof String) {
			return (String) v;
		}
		if (v instanceof Number) {
			return new BigDecimal(v.toString()).stripTrailingZeros().toPlainString();
		}
		throw new BadRequestException("参数格式错误");
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.whitelist.update")
	@PostMapping(value = "/{id}", name = "更新白名单", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateData(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String nameStr = resolveMergedScalarString("name", merged);

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		return ResponseEntity.ok(
				ApiResult.ok(membersWhitelistUpdateDataService.updateData(companyId, id, nameStr)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DeleteMapping(value = "/{id}", name = "删除白名单")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteData(
			HttpServletRequest request,
			@PathVariable("id") String id) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		return ResponseEntity.ok(
				ApiResult.ok(membersWhitelistDeleteDataService.deleteData(companyId, id)));
	}
}
