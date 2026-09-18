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

package cn.shopex.ecshopx.hfpay.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.dispatch.HfpayOrderRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordExportContext;
import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordQuerySupport;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderDetailService;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderListService;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分账统计订单列表/详情/导出；位于 ecshopx-orders 模块以避免 ecshopx-hfpay 与 ecshopx-orders 的 Maven 循环依赖。
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("hfpayStatisticsOrderRecordAdminV1")
@RequestMapping("/api/v1")
public class HfpayStatisticsOrderRecordController {

	private final HfpayOrderRecordExportFileJobDispatchPublisher hfpayOrderRecordExportFileJobDispatchPublisher;
	private final HfpayStatisticsOrderDetailService orderDetailService;
	private final HfpayStatisticsOrderListService orderListService;
	private final HfpayStatisticsOrderMetricsService orderMetricsService;

	public HfpayStatisticsOrderRecordController(
			HfpayOrderRecordExportFileJobDispatchPublisher hfpayOrderRecordExportFileJobDispatchPublisher,
			HfpayStatisticsOrderDetailService orderDetailService,
			HfpayStatisticsOrderListService orderListService,
			HfpayStatisticsOrderMetricsService orderMetricsService) {
		this.hfpayOrderRecordExportFileJobDispatchPublisher = hfpayOrderRecordExportFileJobDispatchPublisher;
		this.orderDetailService = orderDetailService;
		this.orderListService = orderListService;
		this.orderMetricsService = orderMetricsService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "hfpay.statistics.orderList")
	@GetMapping(value = "/hfpay/statistics/orderList", name = "分账统计列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> orderList(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") int distributorId,
			@RequestParam(value = "order_id", required = false, defaultValue = "0") String orderId,
			@RequestParam(value = "app_pay_type", required = false, defaultValue = "") String appPayType,
			@RequestParam(value = "profitsharing_status", required = false, defaultValue = "0") String profitsharingStatusRaw,
			@RequestParam(value = "order_status", required = false, defaultValue = "") String orderStatus,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") int pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		ZoneId cn = ZoneId.of("Asia/Shanghai");
		long[] range = HfpayOrderRecordQuerySupport.validateAndParseEpoch(startDate, endDate, cn);
		long startEpoch = range[0];
		long endEpoch = range[1];

		Long dist = distributorId != 0 ? (long) distributorId : null;
		String orderIdFilter = HfpayOrderRecordQuerySupport.normalizeOrderId(orderId);
		String appPayFilter = HfpayOrderRecordQuerySupport.normalizeAppPayType(appPayType);
		Integer profitsharingFilter = HfpayOrderRecordQuerySupport.parseProfitsharingStatus(profitsharingStatusRaw);
		String orderStatusFilter =
				(orderStatus == null || orderStatus.isBlank()) ? null : orderStatus.trim();

		HfpayOrderRecordExportContext ctx =
				new HfpayOrderRecordExportContext(
						companyId,
						0L,
						0L,
						startEpoch,
						endEpoch,
						dist,
						orderIdFilter,
						appPayFilter,
						profitsharingFilter,
						orderStatusFilter);

		Map<String, Object> total = orderMetricsService.countForOrderListStatistics(ctx);
		long totalCount = orderListService.countOrders(ctx);
		List<Map<String, Object>> data = orderListService.pageOrders(ctx, page, pageSize);
		for (Map<String, Object> row : data) {
			orderListService.enrichListRow(companyId, row);
		}

		Map<String, Object> listWrapper = new LinkedHashMap<>();
		listWrapper.put("total_count", totalCount);
		listWrapper.put("data", data);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total", total);
		body.put("list", listWrapper);

		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "hfpay.statistics.orderDetail")
	@GetMapping(value = "/hfpay/statistics/orderDetail/{orderId}", name = "分账统计详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> orderDetail(
			HttpServletRequest request, @PathVariable("orderId") String orderId) {
		if (orderId == null || orderId.isBlank()) {
			throw new BadRequestException("orderId 不能为空");
		}
		String orderIdTrimmed = orderId.trim();

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> data = orderDetailService.getOrderDetail(companyId, orderIdTrimmed);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "hfpay.statistics.orderExportData")
	@GetMapping(value = "/hfpay/statistics/orderExportData", name = "分账统计导出报表")
	public ResponseEntity<Map<String, Object>> orderExportData(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") int distributorId,
			@RequestParam(value = "order_id", required = false, defaultValue = "0") String orderId,
			@RequestParam(value = "app_pay_type", required = false, defaultValue = "") String appPayType,
			@RequestParam(value = "profitsharing_status", required = false, defaultValue = "0") String profitsharingStatusRaw,
			@RequestParam(value = "order_status", required = false, defaultValue = "") String orderStatus) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		long operatorId = parseOperatorId(operatorJwt.get("operator_id"));

		ZoneId cn = ZoneId.of("Asia/Shanghai");
		long[] range = HfpayOrderRecordQuerySupport.validateAndParseEpoch(startDate, endDate, cn);
		long startEpoch = range[0];
		long endEpoch = range[1];

		Long dist = distributorId != 0 ? (long) distributorId : null;
		String orderIdFilter = HfpayOrderRecordQuerySupport.normalizeOrderId(orderId);
		String appPayFilter = HfpayOrderRecordQuerySupport.normalizeAppPayType(appPayType);
		Integer profitsharingFilter = HfpayOrderRecordQuerySupport.parseProfitsharingStatus(profitsharingStatusRaw);
		String orderStatusFilter =
				(orderStatus == null || orderStatus.isBlank()) ? null : orderStatus.trim();

		LinkedHashMap<String, Object> jobFilter = new LinkedHashMap<>();
		jobFilter.put("company_id", companyId);
		jobFilter.put("start_date", startEpoch);
		jobFilter.put("end_date", endEpoch);
		if (dist != null) {
			jobFilter.put("distributor_id", dist);
		}
		if (orderIdFilter != null) {
			jobFilter.put("order_id", orderIdFilter);
		}
		if (appPayFilter != null) {
			jobFilter.put("app_pay_type", appPayFilter);
		}
		if (profitsharingFilter != null) {
			jobFilter.put("profitsharing_status", profitsharingFilter);
		}
		if (orderStatusFilter != null) {
			jobFilter.put("order_status", orderStatusFilter);
		}
		hfpayOrderRecordExportFileJobDispatchPublisher.publish(companyId, operatorId, jobFilter);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", payload);
		return ResponseEntity.ok(body);
	}

	private static long parseOperatorId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
