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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.community.api.admin.v1.CommunityAdminRequestMerge;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import cn.shopex.ecshopx.community.service.chief.CommunityBuyerWriteoffService;
import cn.shopex.ecshopx.community.service.chief.CommunityChiefBatchWriteoffService;
import cn.shopex.ecshopx.community.service.chief.CommunityChiefQrWriteoffService;
import cn.shopex.ecshopx.community.service.front.CommunityFrontOrderListService;
import jakarta.servlet.http.HttpServletRequest;
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
@RestController("communityFrontChiefV1Order")
@RequestMapping("/api/v1/h5app")
public class CommunityOrderController {

	private final CommunityChiefService communityChiefService;
	private final CommunityChiefBatchWriteoffService communityChiefBatchWriteoffService;
	private final CommunityChiefQrWriteoffService communityChiefQrWriteoffService;
	private final CommunityBuyerWriteoffService communityBuyerWriteoffService;
	private final CommunityFrontOrderListService communityFrontOrderListService;

	public CommunityOrderController(
			CommunityChiefService communityChiefService,
			CommunityChiefBatchWriteoffService communityChiefBatchWriteoffService,
			CommunityChiefQrWriteoffService communityChiefQrWriteoffService,
			CommunityBuyerWriteoffService communityBuyerWriteoffService,
			CommunityFrontOrderListService communityFrontOrderListService) {
		this.communityChiefService = communityChiefService;
		this.communityChiefBatchWriteoffService = communityChiefBatchWriteoffService;
		this.communityChiefQrWriteoffService = communityChiefQrWriteoffService;
		this.communityBuyerWriteoffService = communityBuyerWriteoffService;
		this.communityFrontOrderListService = communityFrontOrderListService;
	}

	@GetMapping(value = "/wxapp/community/orders", name = "团长订单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderList(HttpServletRequest request) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long userId = CommunityChiefService.parseUserIdLoose(claims.get("user_id"));
		if (userId == 0L) {
			userId = CommunityChiefService.parseUserIdLoose(claims.get("sub"));
		}
		if (userId == 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long chiefIdFromJwt = CommunityChiefService.parseUserIdLoose(claims.get("chief_id"));
		Map<String, Object> data =
				communityFrontOrderListService.loadOrderList(companyId, userId, chiefIdFromJwt, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/community/orders/export", name = "导出团购订单")
	public ResponseEntity<Void> exportActivityOrderData() {
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/wxapp/community/orders/batch_writeoff", name = "一键核销")
	public ResponseEntity<ApiResult<Map<String, Object>>> batchWriteoff(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long chiefId;
		try {
			chiefId = communityChiefService.resolveChiefIdForH5(companyId, claims);
		} catch (ForbiddenException ex) {
			throw new ForbiddenException("只有团长可以核销订单");
		}
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long activityId = parseLongLoose(merged.get("activity_id"));
		if (chiefId <= 0L) {
			throw new ForbiddenException("只有团长可以核销订单");
		}
		communityChiefBatchWriteoffService.executeBatchWriteoff(companyId, chiefId, activityId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@PostMapping(value = "/wxapp/community/orders/qr_writeoff", name = "扫码核销")
	public ResponseEntity<ApiResult<Map<String, Object>>> writeoffQR(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long chiefId;
		try {
			chiefId = communityChiefService.resolveChiefIdForH5(companyId, claims);
		} catch (ForbiddenException ex) {
			throw new ForbiddenException("只有团长可以核销订单");
		}
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object codeRaw = merged.get("code");
		String codeString = codeRaw == null ? "" : codeRaw.toString().trim();
		if (!StringUtils.hasText(codeString)) {
			throw new ResourceException("code参数必填");
		}
		Map<String, Object> data =
				communityChiefQrWriteoffService.executeQrWriteoff(companyId, chiefId, codeString);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/writeoff/{order_id}", name = "自助核销")
	public ResponseEntity<ApiResult<Map<String, Object>>> writeoff(
			HttpServletRequest request, @PathVariable("order_id") String orderIdRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> claims = requireClaims(request);
		long memberUserId = CommunityChiefService.parseUserIdLoose(claims.get("user_id"));
		if (memberUserId == 0L) {
			memberUserId = CommunityChiefService.parseUserIdLoose(claims.get("sub"));
		}
		if (memberUserId == 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long orderId = LeadingNumberParser.parseAsLong(orderIdRaw);
		if (orderId <= 0L) {
			throw new ResourceException("订单不存在");
		}
		Map<String, Object> data =
				communityBuyerWriteoffService.executeBuyerWriteoff(companyId, memberUserId, orderId);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	private static long parseLongLoose(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
