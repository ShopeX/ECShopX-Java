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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.dispatch.HfpayTradeRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayCompanyStatisticsFacadeService;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayDistributorDayTotalStatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("hfpayStatisticsAdminV1")
@RequestMapping("/api/v1")
public class HfpayStatisticsController {

	private final HfpayCompanyStatisticsFacadeService companyStatisticsFacadeService;
	private final HfpayDistributorDayTotalStatisticsService distributorDayTotalStatisticsService;
	private final HfpayTradeRecordExportFileJobDispatchPublisher hfpayTradeRecordExportFileJobDispatchPublisher;

	public HfpayStatisticsController(
			HfpayCompanyStatisticsFacadeService companyStatisticsFacadeService,
			HfpayDistributorDayTotalStatisticsService distributorDayTotalStatisticsService,
			HfpayTradeRecordExportFileJobDispatchPublisher hfpayTradeRecordExportFileJobDispatchPublisher) {
		this.companyStatisticsFacadeService = companyStatisticsFacadeService;
		this.distributorDayTotalStatisticsService = distributorDayTotalStatisticsService;
		this.hfpayTradeRecordExportFileJobDispatchPublisher = hfpayTradeRecordExportFileJobDispatchPublisher;
	}

	@Activated(routeAlias = "hfpay.statistics.distributor")
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@GetMapping(value = "/hfpay/statistics/distributor", name = "店铺分账交易统计")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Map<String, Object>>> distributor(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "distributor_id", required = false) Integer distributorId,
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

		Long dist =
				(distributorId != null && distributorId != 0) ? distributorId.longValue() : null;
		Map<String, Object> totle = distributorDayTotalStatisticsService.countTotals(companyId, dist);

		Map<String, Object> listWrapper = new LinkedHashMap<>();
		listWrapper.put("total_count", 0);
		listWrapper.put("data", Collections.emptyList());

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("totle", totle);
		body.put("list", listWrapper);

		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "hfpay.statistics.company")
	@GetMapping(value = "/hfpay/statistics/company", name = "平台分账交易统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> company(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false) Integer distributorId,
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

		String sd = startDate == null ? "" : startDate.trim();
		String ed = endDate == null ? "" : endDate.trim();
		if (sd.isEmpty() || ed.isEmpty()) {
			throw new BadRequestException("请选择对应的日期范围");
		}
		try {
			LocalDate.parse(sd);
			LocalDate.parse(ed);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("日期格式有误");
		}

		Integer distributorFilter = distributorId != null && distributorId != 0 ? distributorId : null;
		String startDateTime = sd + " 00:00:00";
		String endDateTime = ed + " 23:59:59";

		Map<String, Object> data =
				companyStatisticsFacadeService.getCompanyStatistics(
						companyId, startDateTime, endDateTime, distributorFilter, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "hfpay.statistics.exportData")
	@GetMapping(value = "/hfpay/statistics/exportData", name = "导出报表")
	public ResponseEntity<Map<String, Object>> exportData(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") int distributorId) {
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

		String sd = startDate == null ? "" : startDate.trim();
		String ed = endDate == null ? "" : endDate.trim();
		if (sd.isEmpty() || ed.isEmpty()) {
			throw new ResourceException("请选择对应的日期范围");
		}
		try {
			LocalDate.parse(sd);
			LocalDate.parse(ed);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("日期格式有误");
		}

		String startDateTime = sd + " 00:00:00";
		String endDateTime = ed + " 23:59:59";

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("start_date", startDateTime);
		filter.put("end_date", endDateTime);
		if (distributorId != 0) {
			filter.put("distributor_id", distributorId);
		}
		hfpayTradeRecordExportFileJobDispatchPublisher.publish(companyId, operatorId, new LinkedHashMap<>(filter));

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
