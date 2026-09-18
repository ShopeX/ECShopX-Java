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
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.dispatch.CommunityNormalOrderActivityExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.community.service.admin.CommunityActivityAdminListQuerySupport;
import cn.shopex.ecshopx.community.service.admin.CommunityAdminOrderDetailService;
import cn.shopex.ecshopx.community.service.admin.CommunityAdminOrderListService;
import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("communityAdminV1Order")
@RequestMapping("/api/v1/community")
public class CommunityOrderController {

	private final CommunityAdminOrderDetailService communityAdminOrderDetailService;
	private final CommunityAdminOrderListService communityAdminOrderListService;
	private final CommunityNormalOrderActivityExportFileJobDispatchPublisher
			communityNormalOrderActivityExportFileJobDispatchPublisher;

	public CommunityOrderController(
			CommunityAdminOrderDetailService communityAdminOrderDetailService,
			CommunityAdminOrderListService communityAdminOrderListService,
			CommunityNormalOrderActivityExportFileJobDispatchPublisher
					communityNormalOrderActivityExportFileJobDispatchPublisher) {
		this.communityAdminOrderDetailService = communityAdminOrderDetailService;
		this.communityAdminOrderListService = communityAdminOrderListService;
		this.communityNormalOrderActivityExportFileJobDispatchPublisher =
				communityNormalOrderActivityExportFileJobDispatchPublisher;
	}

	@DataPass
	@Activated(routeAlias = "community.order.list.get")
	@GetMapping(value = "/orders", name = "获取订单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderList(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) rawUd;
		Object cid = jwtMap.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		Map<String, Object> data = communityAdminOrderListService.getOrderList(companyId, jwtMap, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "community.order.list.export")
	@GetMapping(value = "/orders/export", name = "导出团购订单")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportActivityOrderData(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) ud;
		Object cid = jwtMap.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		long operatorId = parseOperatorIdOrZero(jwtMap.get("operator_id"));
		CommunityActivityAdminListQuery query = CommunityActivityAdminListQuerySupport.buildQuery(jwtMap, request);
		Long optionalActivityId = CommunityActivityAdminListQuerySupport.parseOptionalActivityId(request);
		boolean datapassAllowed = !DatapassBlockResolver.isBlockedFromQueryParameter(request);
		communityNormalOrderActivityExportFileJobDispatchPublisher.publish(
				companyId, operatorId, datapassAllowed, query, optionalActivityId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	/**
	 * 兼容管理端网关对「订单详情」路径末尾无 {@code order_id} 段的请求：返回与既有客户端约定的 404 语义（{@link ResourceException}），
	 * 避免仅注册 {@code /order/{order_id}} 时落到框架默认的未匹配资源处理。
	 */
	@DataPass
	@GetMapping(value = "/order/", name = "获取订单详情（路径缺失 order_id）")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderDetailMissingOrderIdSegment() {
		throw new ResourceException("404 Not Found", 404);
	}

	@DataPass
	@Activated(routeAlias = "community.order.detail.get")
	@GetMapping(value = "/order/{order_id}", name = "获取订单详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOrderDetail(
			@PathVariable("order_id") String orderId, HttpServletRequest request) {
		if (!StringUtils.hasText(orderId) || orderId.trim().isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		Map<String, Object> data = communityAdminOrderDetailService.getDetail(companyId, orderId.trim(), request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseCompanyId(Object cid) {
		if (cid instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(cid).trim();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static long parseOperatorIdOrZero(Object raw) {
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
