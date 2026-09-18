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

package cn.shopex.ecshopx.community.api.front.v1.chief;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.community.api.admin.v1.CommunityAdminRequestMerge;
import cn.shopex.ecshopx.community.service.CommunityChiefApplySubmitService;
import cn.shopex.ecshopx.community.service.CommunityChiefDistributorListQueryService;
import cn.shopex.ecshopx.community.service.CommunityChiefFrontApplyQueryService;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import cn.shopex.ecshopx.community.service.CommunitySettingService;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("communityFrontChiefV1Chief")
@RequestMapping("/api/v1/h5app")
public class CommunityChiefController {

	private final CommunityChiefApplySubmitService communityChiefApplySubmitService;
	private final CommunityChiefFrontApplyQueryService communityChiefFrontApplyQueryService;
	private final CommunityChiefService communityChiefService;
	private final CommunityChiefDistributorListQueryService communityChiefDistributorListQueryService;
	private final CommunitySettingService communitySettingService;
	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;
	private final LangueProperties langueProperties;

	public CommunityChiefController(
			CommunityChiefApplySubmitService communityChiefApplySubmitService,
			CommunityChiefFrontApplyQueryService communityChiefFrontApplyQueryService,
			CommunityChiefService communityChiefService,
			CommunityChiefDistributorListQueryService communityChiefDistributorListQueryService,
			CommunitySettingService communitySettingService,
			ConfigRequestFieldsApplicationService configRequestFieldsApplicationService,
			LangueProperties langueProperties) {
		this.communityChiefApplySubmitService = communityChiefApplySubmitService;
		this.communityChiefFrontApplyQueryService = communityChiefFrontApplyQueryService;
		this.communityChiefService = communityChiefService;
		this.communityChiefDistributorListQueryService = communityChiefDistributorListQueryService;
		this.communitySettingService = communitySettingService;
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
		this.langueProperties = langueProperties;
	}

	@GetMapping(value = "/wxapp/community/chief/aggrement_and_explanation", name = "团长协议与说明")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAggrementAndExplanation(
			HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		int distributorId = parseDistributorId(request.getParameter("distributor_id"));
		Map<String, Object> merged =
				communitySettingService.getSetting(companyId, true, Integer.valueOf(distributorId));
		Object aggrement = merged.get("aggrement");
		Object explanation = merged.get("explanation");
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("aggrement", aggrement == null ? "" : String.valueOf(aggrement));
		payload.put("explanation", explanation == null ? "" : String.valueOf(explanation));
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@GetMapping(value = "/wxapp/community/chief/apply_fields", name = "申请字段")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getApplyFields(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		int distributorId = parseDistributorId(request.getParameter("distributor_id"));
		List<Map<String, Object>> list =
				configRequestFieldsApplicationService.getChiefApplyFieldsSettingList(
						companyId, distributorId, RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@PostMapping(value = "/wxapp/community/chief/apply", name = "团长申请提交")
	public ResponseEntity<ApiResult<Map<String, Object>>> apply(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long userId = CommunityChiefService.parseUserIdLoose(claims.get("user_id"));
		if (userId <= 0L) {
			userId = CommunityChiefService.parseUserIdLoose(claims.get("sub"));
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		String mobile = communityChiefService.resolveAuthMobile(companyId, claims);

		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		int distributorId = parseDistributorId(merged.get("distributor_id"));

		Map<String, Object> data =
				communityChiefApplySubmitService.submit(
						companyId,
						userId,
						mobile,
						distributorId,
						merged,
						RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/chief/apply", name = "团长申请信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getApplyInfo(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long userId = CommunityChiefService.parseUserIdLoose(claims.get("user_id"));
		if (userId <= 0L) {
			userId = CommunityChiefService.parseUserIdLoose(claims.get("sub"));
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		int distributorId = parseDistributorId(request.getParameter("distributor_id"));
		Map<String, Object> data =
				communityChiefFrontApplyQueryService.getApplyInfoData(companyId, userId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/community/checkChief", name = "检查用户是否是团长")
	public ResponseEntity<ApiResult<Map<String, Object>>> checkChief(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long userId = CommunityChiefService.parseUserIdLoose(claims.get("user_id"));
		if (userId <= 0L) {
			userId = CommunityChiefService.parseUserIdLoose(claims.get("sub"));
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		String mobile;
		try {
			mobile = communityChiefService.resolveAuthMobile(companyId, claims);
		} catch (BadRequestException e) {
			throw new UnauthorizedException("请重新登录");
		}
		Map<String, Object> body = communityChiefService.buildCheckChiefResponse(companyId, userId, mobile);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@GetMapping(value = "/wxapp/community/chief/distributor", name = "团长店铺列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getDisitrbutorList(
			HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long userId = CommunityChiefService.parseUserIdLoose(claims.get("user_id"));
		if (userId <= 0L) {
			userId = CommunityChiefService.parseUserIdLoose(claims.get("sub"));
		}
		if (userId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			communityChiefService.resolveAuthMobile(companyId, claims);
		} catch (BadRequestException e) {
			throw new UnauthorizedException("请重新登录");
		}
		List<Map<String, Object>> list =
				communityChiefDistributorListQueryService.listForChiefMember(companyId, userId);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long cid;
		if (companyAttr instanceof Number n) {
			cid = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				cid = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (cid <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return cid;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> requireClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return (Map<String, Object>) m;
	}

	private static int parseDistributorId(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
