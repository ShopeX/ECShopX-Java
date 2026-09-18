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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.service.AdapayMemberGetAuditStateService;
import cn.shopex.ecshopx.adapay.service.AdapayMemberGetService;
import cn.shopex.ecshopx.adapay.service.AdapayMemberListQuery;
import cn.shopex.ecshopx.adapay.service.AdapayMemberListService;
import cn.shopex.ecshopx.adapay.service.AdapayMemberSetValidService;
import cn.shopex.ecshopx.adapay.service.AdapayPersonMemberCreateService;
import cn.shopex.ecshopx.adapay.service.AdapayPersonMemberModifyService;
import cn.shopex.ecshopx.adapay.service.AdapayPersonMemberUpdateService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("adapayMemberAdminV1")
@RequestMapping("/api/v1/adapay")
public class MemberController {

	private final AdapayPersonMemberCreateService adapayPersonMemberCreateService;
	private final AdapayPersonMemberModifyService adapayPersonMemberModifyService;
	private final AdapayPersonMemberUpdateService adapayPersonMemberUpdateService;
	private final AdapayMemberSetValidService adapayMemberSetValidService;
	private final AdapayMemberGetAuditStateService adapayMemberGetAuditStateService;
	private final AdapayMemberGetService adapayMemberGetService;
	private final AdapayMemberListService adapayMemberListService;

	public MemberController(
			AdapayPersonMemberCreateService adapayPersonMemberCreateService,
			AdapayPersonMemberModifyService adapayPersonMemberModifyService,
			AdapayPersonMemberUpdateService adapayPersonMemberUpdateService,
			AdapayMemberSetValidService adapayMemberSetValidService,
			AdapayMemberGetAuditStateService adapayMemberGetAuditStateService,
			AdapayMemberGetService adapayMemberGetService,
			AdapayMemberListService adapayMemberListService) {
		this.adapayPersonMemberCreateService = adapayPersonMemberCreateService;
		this.adapayPersonMemberModifyService = adapayPersonMemberModifyService;
		this.adapayPersonMemberUpdateService = adapayPersonMemberUpdateService;
		this.adapayMemberSetValidService = adapayMemberSetValidService;
		this.adapayMemberGetAuditStateService = adapayMemberGetAuditStateService;
		this.adapayMemberGetService = adapayMemberGetService;
		this.adapayMemberListService = adapayMemberListService;
	}

	@Activated(routeAlias = "adapay.member.auditState")
	@GetMapping(value = "/member/auditState", name = "用户对象审核状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAuditState(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> data = adapayMemberGetAuditStateService.getAuditState(companyId, jwtMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.member.valid")
	@GetMapping(value = "/member/setValid", name = "用户进入结算中心状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> setValid(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		adapayMemberSetValidService.setValid(companyId, jwtMap);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.member.info")
	@GetMapping(value = "/member/get", name = "获取个人用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> data = adapayMemberGetService.get(companyId, jwtMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.member.create")
	@PostMapping(value = "/member/create", name = "创建个人用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		adapayPersonMemberCreateService.createFromRequest(companyId, jwtMap, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.member.modify")
	@PostMapping(value = "/member/modify", name = "修改个人用户对象(未开户)")
	public ResponseEntity<ApiResult<Map<String, Object>>> modify(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		adapayPersonMemberModifyService.modify(companyId, jwtMap, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.member.update")
	@PostMapping(value = "/member/update", name = "更新个人用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		adapayPersonMemberUpdateService.update(companyId, jwtMap, body == null ? Map.of() : body);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DataPass
	@Activated(routeAlias = "adapay.member.list")
	@GetMapping(value = "/member/list", name = "adapay开户列表(店铺端 经销商端)")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(
			HttpServletRequest request,
			@RequestParam(name = "member_type", required = false) String memberType,
			@RequestParam(name = "operator_type", required = false) String operatorType,
			@RequestParam(name = "keywords", required = false) String keywords,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		String normMemberType = null;
		if (memberType != null && StringUtils.hasText(memberType.trim())) {
			normMemberType = memberType.trim();
		}
		String normOperatorType = null;
		if (operatorType != null && StringUtils.hasText(operatorType.trim())) {
			String ot = operatorType.trim();
			if (!"all".equals(ot)) {
				normOperatorType = ot;
			}
		}
		String normKeywords = null;
		if (keywords != null && StringUtils.hasText(keywords.trim())) {
			normKeywords = keywords.trim();
		}
		int normPage = Math.max(1, page);
		int normPageSize = Math.min(Math.max(1, pageSize), 200);

		AdapayMemberListQuery query =
				new AdapayMemberListQuery(normMemberType, normOperatorType, normKeywords, normPage, normPageSize);
		Map<String, Object> data =
				adapayMemberListService.lists(companyId, query, parseDatapassBlock(request));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private int parseDatapassBlock(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return 1;
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return 1;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return 0;
		}
		return 1;
	}
}
