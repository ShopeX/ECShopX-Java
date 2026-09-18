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

package cn.shopex.ecshopx.salesperson.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.salesperson.service.SalespersonRelationshipContinuityService;
import cn.shopex.ecshopx.salesperson.service.SalespersonSubtaskPostService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskShareService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("salespersonFrontV1SalespersonTask")
@RequestMapping("/api/v1/h5app")
public class SalespersonTaskController {

	private final SalespersonSubtaskPostService salespersonSubtaskPostService;
	private final SalespersonRelationshipContinuityService salespersonRelationshipContinuityService;
	private final SalespersonTaskShareService salespersonTaskShareService;

	public SalespersonTaskController(
			SalespersonSubtaskPostService salespersonSubtaskPostService,
			SalespersonRelationshipContinuityService salespersonRelationshipContinuityService,
			SalespersonTaskShareService salespersonTaskShareService) {
		this.salespersonSubtaskPostService = salespersonSubtaskPostService;
		this.salespersonRelationshipContinuityService = salespersonRelationshipContinuityService;
		this.salespersonTaskShareService = salespersonTaskShareService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontAuth
	@PostMapping(value = "/wxapp/salesperson/task/share", name = "完成导购分享任务")
	public ResponseEntity<ApiResult<Map<String, Object>>> share(
			HttpServletRequest request,
			@RequestParam(value = "salesperson_id", required = false) String salespersonIdRaw,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "id", required = false) String id) {
		if (salespersonIdRaw == null || !StringUtils.hasText(salespersonIdRaw.trim())) {
			throw new BadRequestException("导购id必填");
		}
		long spId = LeadingNumberParser.parseAsLong(salespersonIdRaw.trim());
		long userId = SalespersonController.parseMemberUserIdFromRequest(request);
		String typeEff = StringUtils.hasText(type) ? type.trim() : "index";
		String idEff = StringUtils.hasText(id) ? id.trim() : "0";
		Object claimsRaw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = (Map<String, Object>) claimsRaw;
		String username = firstNonBlankText(claims.get("nickname"), claims.get("username"));
		if (!StringUtils.hasText(username)) {
			username = "微信用户";
		}
		long companyId = parseCompanyIdFromRequest(request);
		boolean ok = salespersonTaskShareService.share(companyId, userId, username, spId, typeEff, idEff);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", ok)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/salesperson/subtask/post", name = "提交子任务参数")
	public ResponseEntity<ApiResult<Map<String, Object>>> postSubtask(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		LinkedHashMap<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			input.putAll(body);
		}
		Map<String, Object> data =
				salespersonSubtaskPostService.postSubtask(companyId, input, resolveUnionIdForJob(request, input));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = false)
	@FrontNoAuth
	@PostMapping(value = "/wxapp/salesperson/relationshipcontinuity", name = "关系延续埋点")
	public ResponseEntity<ApiResult<Map<String, Object>>> relationshipContinuity(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		LinkedHashMap<String, Object> input = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			input.putAll(body);
		}
		Map<String, Object> data = salespersonRelationshipContinuityService.relationshipContinuity(companyId, input);
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

	@SuppressWarnings("unchecked")
	private static String resolveUnionIdForJob(HttpServletRequest request, Map<String, Object> input) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> rawMap) {
			Map<String, Object> claims = (Map<String, Object>) rawMap;
			String u = firstNonBlankText(claims.get("unionid"), claims.get("union_id"));
			if (StringUtils.hasText(u)) {
				return u.trim();
			}
		}
		String u2 = firstNonBlankText(input.get("unionid"), input.get("union_id"));
		return StringUtils.hasText(u2) ? u2.trim() : null;
	}

	private static String firstNonBlankText(Object a, Object b) {
		if (a != null) {
			String s = a.toString().trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		if (b != null) {
			String s = b.toString().trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		return null;
	}
}
