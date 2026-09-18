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
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.community.api.admin.v1.CommunityAdminRequestMerge;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import cn.shopex.ecshopx.community.service.CommunityChiefZitiService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("communityFrontChiefV1ChiefZiti")
@RequestMapping("/api/v1/h5app")
public class CommunityChiefZitiController {

	private final CommunityChiefService communityChiefService;
	private final CommunityChiefZitiService communityChiefZitiService;

	public CommunityChiefZitiController(
			CommunityChiefService communityChiefService,
			CommunityChiefZitiService communityChiefZitiService) {
		this.communityChiefService = communityChiefService;
		this.communityChiefZitiService = communityChiefZitiService;
	}

	@GetMapping(value = "/wxapp/community/chief/ziti", name = "自提点列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> actionList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		List<Map<String, Object>> list = communityChiefZitiService.listChiefZitiForH5(chiefId);
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/wxapp/community/chief/ziti", name = "添加自提点")
	public ResponseEntity<ApiResult<Map<String, Object>>> actionCreate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);

		String zitiName = stringVal(merged.get("ziti_name"));
		if (!StringUtils.hasText(zitiName)) {
			throw new BadRequestException("自提点名称不能为空");
		}
		String addr = stringVal(merged.get("address"));
		if (!StringUtils.hasText(addr)) {
			throw new BadRequestException("地址不能为空");
		}

		Map<String, Object> data = communityChiefZitiService.createChiefZiti(chiefId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@PostMapping(value = "/wxapp/community/chief/ziti/{ziti_id}", name = "修改自提点")
	public ResponseEntity<ApiResult<Map<String, Object>>> actionUpdate(
			HttpServletRequest request,
			@PathVariable("ziti_id") String zitiIdStr,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long zitiId = parseZitiIdPath(zitiIdStr);
		long companyId = parseCompanyIdFromRequest(request);
		long chiefId = resolveChiefId(request, companyId);
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = communityChiefZitiService.updateChiefZiti(zitiId, chiefId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private long parseZitiIdPath(String raw) {
		if (raw == null) {
			throw new ResourceException("无效的自提点");
		}
		String t = raw.trim();
		try {
			long id = Long.parseLong(t);
			if (id <= 0L) {
				throw new ResourceException("无效的自提点");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("无效的自提点");
		}
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
	private long resolveChiefId(HttpServletRequest request, long companyId) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> m)) {
			throw new ForbiddenException("只有团长才能操作");
		}
		return communityChiefService.resolveChiefIdForH5(companyId, (Map<String, Object>) m);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
