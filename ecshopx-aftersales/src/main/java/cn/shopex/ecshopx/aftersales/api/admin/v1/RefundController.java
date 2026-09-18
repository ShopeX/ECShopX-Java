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

package cn.shopex.ecshopx.aftersales.api.admin.v1;

import cn.shopex.ecshopx.aftersales.dto.RefundAdminListQuery;
import cn.shopex.ecshopx.aftersales.dto.RefundLogExportQuery;
import cn.shopex.ecshopx.aftersales.service.AftersalesOfflineRefundSaveService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundLogExportService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundOfflineBankService;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("refundAdminV1")
@RequestMapping("/api/v1")
public class RefundController {

	private final AftersalesOfflineRefundSaveService aftersalesOfflineRefundSaveService;
	private final AftersalesRefundListService aftersalesRefundListService;
	private final AftersalesRefundLogExportService aftersalesRefundLogExportService;
	private final AftersalesRefundOfflineBankService aftersalesRefundOfflineBankService;

	public RefundController(
			AftersalesOfflineRefundSaveService aftersalesOfflineRefundSaveService,
			AftersalesRefundListService aftersalesRefundListService,
			AftersalesRefundLogExportService aftersalesRefundLogExportService,
			AftersalesRefundOfflineBankService aftersalesRefundOfflineBankService) {
		this.aftersalesOfflineRefundSaveService = aftersalesOfflineRefundSaveService;
		this.aftersalesRefundListService = aftersalesRefundListService;
		this.aftersalesRefundLogExportService = aftersalesRefundLogExportService;
		this.aftersalesRefundOfflineBankService = aftersalesRefundOfflineBankService;
	}

	@Activated(routeAlias = "refund.list")
	@GetMapping(value = "/refund", name = "获取退款单列表")
	public ApiResult<Map<String, Object>> getRefundList(
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "refund_bn", required = false) String refundBn,
			@RequestParam(value = "aftersales_bn", required = false) String aftersalesBn,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "shop_id", required = false) String shopId,
			@RequestParam(value = "refund_type", required = false) String refundType,
			@RequestParam(value = "refund_channel", required = false) String refundChannel,
			@RequestParam(value = "refund_status", required = false) String refundStatus,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "user_id", required = false) String userId,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw,
			HttpServletRequest request) {
		RefundAdminListQuery query = new RefundAdminListQuery();
		query.setRefundTypeParameterPresent(request.getParameterMap().containsKey("refund_type"));
		query.setPageRaw(pageRaw);
		query.setPageSizeRaw(pageSizeRaw);
		query.setMobile(mobile);
		query.setRefundBn(refundBn);
		query.setAftersalesBn(aftersalesBn);
		query.setOrderId(orderId);
		query.setShopId(shopId);
		query.setRefundType(refundType);
		query.setRefundChannel(refundChannel);
		query.setRefundStatus(refundStatus);
		query.setTimeStartBegin(timeStartBegin);
		query.setTimeStartEnd(timeStartEnd);
		query.setUserId(userId);
		query.setDistributorIdRaw(distributorIdRaw);
		return ApiResult.ok(aftersalesRefundListService.getRefundList(query, request));
	}

	@Activated(routeAlias = "refund.info")
	@GetMapping(value = "/refund/detail/{refund_bn}", name = "获取退款单详情")
	public ApiResult<Object> getRefundsDetail(
			@PathVariable("refund_bn") String refundBn, HttpServletRequest request) {
		return ApiResult.ok(aftersalesRefundListService.getRefundsDetail(refundBn, request));
	}

	@Activated(routeAlias = "refund.logexport")
	@GetMapping(value = "/refund/logExport", name = "导出退款单列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> logExport(
			HttpServletRequest request,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "refund_bn", required = false) String refundBn,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "refund_type", required = false) String refundType,
			@RequestParam(value = "refund_channel", required = false) String refundChannel,
			@RequestParam(value = "refund_status", required = false) String refundStatus,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "distributor_id", required = false) String distributorIdRaw) {
		RefundLogExportQuery q = new RefundLogExportQuery();
		q.setRefundTypeParameterPresent(request.getParameterMap().containsKey("refund_type"));
		q.setMobile(mobile);
		q.setRefundBn(refundBn);
		q.setOrderId(orderId);
		q.setRefundType(refundType);
		q.setRefundChannel(refundChannel);
		q.setRefundStatus(refundStatus);
		q.setTimeStartBegin(timeStartBegin);
		q.setTimeStartEnd(timeStartEnd);
		q.setDistributorIdRaw(distributorIdRaw);
		aftersalesRefundLogExportService.logExport(request, q);
		return ApiResult.ok(Map.of("status", true));
	}

	@Activated(routeAlias = "refund.offline")
	@GetMapping(value = "/refund/offline/bank", name = "获取线下退款银行信息")
	public ApiResult<Object> getOfflineBank(
			@RequestParam(value = "order_id", required = false) String orderIdRaw,
			HttpServletRequest request) {
		return ApiResult.ok(aftersalesRefundOfflineBankService.getOfflineBank(orderIdRaw, request));
	}

	@Activated(routeAlias = "refund.offline")
	@PostMapping(value = "/refund/offline", name = "线下退款", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Boolean> saveOfflineRefund(@FlexibleBody(required = false) Map<String, Object> body) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		long companyId = longVal(companyIdObj);

		aftersalesOfflineRefundSaveService.saveOfflineRefund(merged, companyId);
		return ResponseEntity.ok(Boolean.TRUE);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
