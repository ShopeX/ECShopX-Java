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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.operatorpending.OperatorPendingOrderService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@RestController("companysAdminV1OperatorPendingOrder")
@RequestMapping("/api/v1/operator")
public class OperatorPendingOrderController {

	/**
	 * Used when {@code pending_id} is present but not parseable as a positive long: delete conditions must match no
	 * row while still returning success {@code status: true}.
	 */
	private static final long PENDING_ID_NO_MATCH_PLACEHOLDER = -1L;

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final OperatorPendingOrderService operatorPendingOrderService;

	public OperatorPendingOrderController(OperatorPendingOrderService operatorPendingOrderService) {
		this.operatorPendingOrderService = operatorPendingOrderService;
	}

	@Activated(routeAlias = "companys.operator.pending.list")
	@GetMapping(value = "/pending/list", name = "管理员挂单列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> listPendingData(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false, defaultValue = "0") long distributorId,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readPositiveLongOperatorId(jwt);
		int normalizedPage = Math.max(1, page);
		int normalizedPageSize = pageSize < 1 ? 20 : pageSize;
		Map<String, Object> body = operatorPendingOrderService.listPendingData(
				companyId, operatorId, distributorId, normalizedPage, normalizedPageSize);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "companys.operator.cartdata.pending")
	@PostMapping(value = "/cartdata/pending", name = "管理员购物车挂单")
	public ResponseEntity<ApiResult<Map<String, Object>>> pendingCartData(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false, defaultValue = "0") long distributorId,
			@RequestParam(name = "user_id", required = false, defaultValue = "0") long userId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readPositiveLongOperatorId(jwt);
		Map<String, Object> result =
				operatorPendingOrderService.pendingCartData(companyId, operatorId, distributorId, userId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "companys.operator.order.pending")
	@PostMapping(value = "/order/pending", name = "管理员待支付订单挂单")
	public ResponseEntity<ApiResult<Map<String, Object>>> pendingOrderData(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false, defaultValue = "0") long distributorId,
			@RequestParam(name = "user_id", required = false, defaultValue = "0") long userId,
			@RequestParam(name = "order_id", required = false) String orderId) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readPositiveLongOperatorId(jwt);
		if (orderId == null) {
			throw new BadRequestException("缺少订单号");
		}
		String trimmedOrderId = orderId.trim();
		if (trimmedOrderId.isEmpty() || trimmedOrderId.equals("0")) {
			throw new BadRequestException("缺少订单号");
		}
		Map<String, Object> result = operatorPendingOrderService.pendingOrderData(
				companyId, operatorId, distributorId, userId, trimmedOrderId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "companys.operator.pending.fetch")
	@PostMapping(value = "/pending/fetch", name = "管理员取单")
	public ResponseEntity<ApiResult<Map<String, Object>>> fetchPendingData(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false, defaultValue = "0") long distributorId,
			@RequestParam(name = "user_id", required = false, defaultValue = "0") long userId,
			@RequestParam(name = "pending_id", required = false) String pendingIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readPositiveLongOperatorId(jwt);
		if (pendingIdRaw == null) {
			throw new BadRequestException("缺少挂单ID");
		}
		String trimmed = pendingIdRaw.trim();
		if (trimmed.isEmpty() || trimmed.equals("0")) {
			throw new BadRequestException("缺少挂单ID");
		}
		long pendingId;
		try {
			pendingId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("挂单数据为空");
		}
		Map<String, Object> result = operatorPendingOrderService.fetchPendingData(
				companyId, operatorId, distributorId, userId, pendingId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "companys.operator.pending.delete")
	@DeleteMapping(value = "/pending/delete", name = "管理员挂单数据删除")
	public ResponseEntity<ApiResult<Map<String, Object>>> delPendingData(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long operatorId = readPositiveLongOperatorId(jwt);
		Map<String, Object> input =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		Object pendingRaw = input.get("pending_id");
		if (pendingRaw == null) {
			throw new BadRequestException("缺少挂单ID");
		}
		if (pendingRaw instanceof Boolean b && !b) {
			throw new BadRequestException("缺少挂单ID");
		}
		if (pendingRaw instanceof Collection<?> c && c.isEmpty()) {
			throw new BadRequestException("缺少挂单ID");
		}
		long pendingId;
		if (pendingRaw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("缺少挂单ID");
			}
			pendingId = v;
		} else {
			String trimmed = pendingRaw.toString().trim();
			if (trimmed.isEmpty() || trimmed.equals("0")) {
				throw new BadRequestException("缺少挂单ID");
			}
			try {
				pendingId = Long.parseLong(trimmed);
			} catch (NumberFormatException e) {
				pendingId = PENDING_ID_NO_MATCH_PLACEHOLDER;
			}
		}
		Map<String, Object> result =
				operatorPendingOrderService.deletePendingByFilter(companyId, operatorId, pendingId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
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

	private static long readPositiveLongOperatorId(Map<String, Object> jwt) {
		return readPositiveLong(jwt, "operator_id", "无权访问该API,非法访问！");
	}

	private static long readPositiveLong(Map<String, Object> jwt, String key, String unauthorizedMsg) {
		Object v = jwt.get(key);
		if (v == null) {
			throw new UnauthorizedException(unauthorizedMsg);
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException(unauthorizedMsg);
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException(unauthorizedMsg);
		}
	}
}
