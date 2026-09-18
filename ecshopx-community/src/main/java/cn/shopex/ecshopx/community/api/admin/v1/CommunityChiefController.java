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

package cn.shopex.ecshopx.community.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.community.service.CommunityChiefApplyInfoQueryService;
import cn.shopex.ecshopx.community.service.CommunityChiefApplyWxaCodeService;
import cn.shopex.ecshopx.community.service.CommunityChiefApprovalService;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController("communityAdminV1Chief")
@RequestMapping("/api/v1/community")
public class CommunityChiefController {

	private final CommunityChiefApprovalService communityChiefApprovalService;
	private final CommunityChiefService communityChiefService;
	private final CommunityChiefApplyInfoQueryService communityChiefApplyInfoQueryService;
	private final CommunityChiefApplyWxaCodeService communityChiefApplyWxaCodeService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	public CommunityChiefController(
			CommunityChiefApprovalService communityChiefApprovalService,
			CommunityChiefService communityChiefService,
			CommunityChiefApplyInfoQueryService communityChiefApplyInfoQueryService,
			CommunityChiefApplyWxaCodeService communityChiefApplyWxaCodeService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver) {
		this.communityChiefApprovalService = communityChiefApprovalService;
		this.communityChiefService = communityChiefService;
		this.communityChiefApplyInfoQueryService = communityChiefApplyInfoQueryService;
		this.communityChiefApplyWxaCodeService = communityChiefApplyWxaCodeService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
	}

	@SuppressWarnings("unused")
	@Activated(routeAlias = "community.chief.apply.wxaCode")
	@GetMapping(value = "/chief/apply/wxaCode", name = "团长申请页小程序码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxaCode(
			HttpServletRequest request,
			@RequestParam(value = "path", required = false, defaultValue = "pages/index") String path,
			@RequestParam(value = "wxaAppId", required = false) String wxaAppIdQueryUnused) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);
		String operatorType = stringVal(ud.get("operator_type"));
		String distributorIdQueryRaw =
				"distributor".equals(operatorType) ? request.getParameter("distributor_id") : null;
		Map<String, Object> data =
				communityChiefApplyWxaCodeService.buildChiefApplyWxaCode(
						companyId, operatorType, distributorIdQueryRaw, path);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.apply.list")
	@GetMapping(value = "/chief/apply/list", name = "团长申请列表")
	public ResponseEntity<ApiResult<Object>> getApplyList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		String operatorType = stringVal(ud.get("operator_type"));

		Integer distributorIdFilter;
		if (!"distributor".equals(operatorType)) {
			distributorIdFilter = 0;
		} else {
			String p = request.getParameter("distributor_id");
			if (p == null || !StringUtils.hasText(p.trim())) {
				distributorIdFilter = null;
			} else {
				distributorIdFilter = parseIntFromStringLooseOrNull(p.trim());
			}
		}

		int page = parseIntQueryOrDefault(request.getParameter("page"), 1);
		int pageSize = parseIntQueryOrDefault(request.getParameter("pageSize"), 10);

		boolean restrictToPendingApproval = "0".equals(request.getParameter("approve_status"));

		String nameParam = request.getParameter("name");
		String chiefNameOrNull =
				(nameParam != null && StringUtils.hasText(nameParam.trim())) ? nameParam.trim() : null;
		String mobileParam = request.getParameter("mobile");
		String chiefMobileOrNull =
				(mobileParam != null && StringUtils.hasText(mobileParam.trim())) ? mobileParam.trim() : null;

		Map<String, Object> data =
				communityChiefApplyInfoQueryService.listApplyForAdmin(
						companyId,
						operatorType,
						distributorIdFilter,
						page,
						pageSize,
						restrictToPendingApproval,
						chiefNameOrNull,
						chiefMobileOrNull);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.apply.info")
	@GetMapping(value = "/chief/apply/info/{apply_id}", name = "团长申请信息")
	public ResponseEntity<ApiResult<Object>> getApplyInfo(
			HttpServletRequest request, @PathVariable("apply_id") String applyId) {
		final long applyIdLong;
		try {
			applyIdLong = Long.parseLong(applyId.trim());
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		String operatorType = stringVal(ud.get("operator_type"));

		Integer distributorIdFilter;
		if (!"distributor".equals(operatorType)) {
			distributorIdFilter = 0;
		} else {
			String p = request.getParameter("distributor_id");
			if (p == null || !StringUtils.hasText(p.trim())) {
				distributorIdFilter = null;
			} else {
				distributorIdFilter = parseIntFromStringLooseOrNull(p.trim());
			}
		}

		Object payload =
				communityChiefApplyInfoQueryService.getApplyInfoForAdmin(
						companyId, operatorType, distributorIdFilter, applyIdLong);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "community.chief.approve")
	@PostMapping(value = "/chief/approve/{apply_id}", name = "团长申请审批")
	public ResponseEntity<ApiResult<Map<String, Object>>> approve(
			HttpServletRequest request,
			@PathVariable("apply_id") String applyId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		final long applyIdLong;
		try {
			applyIdLong = Long.parseLong(applyId.trim());
		} catch (Exception e) {
			throw new ResourceException("处理的申请不存在");
		}

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			distributorId = parseIntLoose(d, 0);
		}

		communityChiefApprovalService.approve(companyId, distributorId, applyIdLong, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "community.setting.chief")
	@PostMapping(value = "/chief/setMemberCommunity", name = "设置团长")
	public ResponseEntity<ApiResult<Map<String, Object>>> setMemberCommunity(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			distributorId = parseIntLoose(d, 0);
		}

		communityChiefService.createChiefFromAdmin(companyId, operatorType, distributorId, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "community.chief.list")
	@GetMapping(value = "/chief/list", name = "团长列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getChiefList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);
		String operatorType = stringVal(ud.get("operator_type"));

		String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);

		Long distributorFilter;
		if ("distributor".equals(operatorType)) {
			Integer parsed = parseIntFromStringLooseOrNull(request.getParameter("distributor_id"));
			distributorFilter = parsed == null ? null : parsed.longValue();
		} else {
			distributorFilter = "platform".equals(productModel) ? 0L : null;
		}

		int page = parseIntQueryOrDefault(request.getParameter("page"), 1);
		int pageSize = parseIntQueryOrDefault(request.getParameter("pageSize"), 10);

		String nameParam = request.getParameter("name");
		String chiefNameOrNull =
				(nameParam != null && StringUtils.hasText(nameParam.trim())) ? nameParam.trim() : null;
		String mobileParam = request.getParameter("mobile");
		String chiefMobileOrNull =
				(mobileParam != null && StringUtils.hasText(mobileParam.trim())) ? mobileParam.trim() : null;

		Map<String, Object> data =
				communityChiefService.listChiefsByDistributorJoin(
						distributorFilter, companyId, chiefNameOrNull, chiefMobileOrNull, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.info")
	@GetMapping(value = "/chief/{chief_id}", name = "团长详情")
	public ResponseEntity<ApiResult<Object>> getChiefInfo(
			HttpServletRequest request, @PathVariable("chief_id") String chiefId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		String distributorIdRaw = request.getParameter("distributor_id");
		if (distributorIdRaw == null || !StringUtils.hasText(distributorIdRaw.trim())) {
			throw new BadRequestException("所属店铺ID必填");
		}
		Integer distributorParsed = parseIntFromStringLooseOrNull(distributorIdRaw.trim());
		if (distributorParsed == null) {
			throw new BadRequestException("所属店铺ID格式错误");
		}
		long distributorId = distributorParsed.longValue();

		String chiefIdTrimmed = chiefId == null ? "" : chiefId.trim();

		Object data =
				communityChiefService.getChiefInfoByDistributorJoin(distributorId, companyId, chiefIdTrimmed);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long toLongCompany(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static int parseIntLoose(Object v, int defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	/**
	 * 与 {@link #parseIntLoose(Object, int)} 类似的宽松解析；失败返回 {@code null}（用于 GET query，与未传同义）。
	 */
	private static Integer parseIntFromStringLooseOrNull(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Query 整数：缺省、空白或非数字时返回 defaultVal（与 GET query 解析一致）。 */
	private static int parseIntQueryOrDefault(String raw, int defaultVal) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
