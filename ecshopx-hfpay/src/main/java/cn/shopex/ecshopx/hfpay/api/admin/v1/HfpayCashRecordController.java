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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.hfpay.service.cashrecord.HfpayCashRecordWithdrawService;
import cn.shopex.ecshopx.hfpay.service.cashrecord.HfpayWithdrawRecordListService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayHfFileRequestMergeService;
import cn.shopex.ecshopx.common.dispatch.HfpayWithdrawRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.hfpay.service.export.HfpayWithdrawRecordExportContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("hfpayCashRecordAdminV1")
@RequestMapping("/api/v1")
public class HfpayCashRecordController {

	private final HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService;
	private final HfpayCashRecordWithdrawService hfpayCashRecordWithdrawService;
	private final HfpayWithdrawRecordExportFileJobDispatchPublisher hfpayWithdrawRecordExportFileJobDispatchPublisher;
	private final HfpayWithdrawRecordListService hfpayWithdrawRecordListService;

	public HfpayCashRecordController(
			HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService,
			HfpayCashRecordWithdrawService hfpayCashRecordWithdrawService,
			HfpayWithdrawRecordExportFileJobDispatchPublisher hfpayWithdrawRecordExportFileJobDispatchPublisher,
			HfpayWithdrawRecordListService hfpayWithdrawRecordListService) {
		this.hfpayHfFileRequestMergeService = hfpayHfFileRequestMergeService;
		this.hfpayCashRecordWithdrawService = hfpayCashRecordWithdrawService;
		this.hfpayWithdrawRecordExportFileJobDispatchPublisher = hfpayWithdrawRecordExportFileJobDispatchPublisher;
		this.hfpayWithdrawRecordListService = hfpayWithdrawRecordListService;
	}

	@Activated(routeAlias = "hfpay.withdraw.getList")
	@GetMapping(value = "/hfpay/withdraw/getList", name = "汇付提现记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "order_id", required = false, defaultValue = "0") String orderIdRaw,
			@RequestParam(value = "cash_status", required = false, defaultValue = "") String cashStatusRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") String pageSizeRaw) {
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

		String startTrim = startDate == null ? "" : startDate.trim();
		String endTrim = endDate == null ? "" : endDate.trim();
		if (!StringUtils.hasText(startTrim) || !StringUtils.hasText(endTrim)) {
			throw new ResourceException("请选择对应的日期范围");
		}
		try {
			LocalDate.parse(startTrim);
			LocalDate.parse(endTrim);
		} catch (DateTimeParseException e) {
			throw new ResourceException("日期格式有误");
		}
		String startDateTime = startTrim + " 00:00:00";
		String endDateTime = endTrim + " 23:59:59";

		Long distributorId = null;
		String distS = distributorIdRaw == null ? "0" : distributorIdRaw.trim();
		try {
			long d = Long.parseLong(distS);
			if (d > 0L) {
				distributorId = d;
			}
		} catch (NumberFormatException ignored) {
			// omit filter
		}

		String orderId = null;
		String orderS = orderIdRaw == null ? "0" : orderIdRaw.trim();
		try {
			long oid = Long.parseLong(orderS);
			if (oid > 0L) {
				orderId = orderS;
			}
		} catch (NumberFormatException ignored) {
			// omit filter
		}

		boolean cashStatusInProgress = false;
		Integer cashStatusExact = null;
		String cs = cashStatusRaw == null ? "" : cashStatusRaw.trim();
		if (StringUtils.hasText(cs)) {
			try {
				int parsed = Integer.parseInt(cs);
				if (parsed == 1) {
					cashStatusInProgress = true;
				} else {
					cashStatusExact = parsed;
				}
			} catch (NumberFormatException ignored) {
				// omit cash_status filter
			}
		}

		int page = parseLowerBoundedInt(pageRaw, 1, 1);
		int pageSize = parseLowerBoundedInt(pageSizeRaw, 20, 1);

		HfpayWithdrawRecordExportContext ctx =
				new HfpayWithdrawRecordExportContext(
						companyId,
						0L,
						startDateTime,
						endDateTime,
						distributorId,
						orderId,
						cashStatusInProgress,
						cashStatusExact);
		Map<String, Object> payload = hfpayWithdrawRecordListService.getWithdrawList(companyId, ctx, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private static int parseLowerBoundedInt(String raw, int defaultWhenInvalid, int floor) {
		String s = raw == null ? "" : raw.trim();
		if (!StringUtils.hasText(s)) {
			return Math.max(floor, defaultWhenInvalid);
		}
		try {
			return Math.max(floor, Integer.parseInt(s));
		} catch (NumberFormatException e) {
			return Math.max(floor, defaultWhenInvalid);
		}
	}

	@Activated(routeAlias = "hfpay.withdraw")
	@PostMapping(value = "/hfpay/withdraw", name = "汇付提现")
	public ResponseEntity<ApiResult<Map<String, Object>>> withdraw(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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

		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		Map<String, Object> data = hfpayCashRecordWithdrawService.withdraw(companyId, operatorId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseOperatorId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@Activated(routeAlias = "hfpay.withdraw.exportData")
	@GetMapping(value = "/hfpay/withdraw/exportData", name = "汇付提现记录导出")
	public ResponseEntity<Map<String, Object>> exportData(
			HttpServletRequest request,
			@RequestParam(value = "start_date", required = false) String startDate,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "order_id", required = false, defaultValue = "0") String orderIdRaw,
			@RequestParam(value = "cash_status", required = false, defaultValue = "") String cashStatusRaw) {
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

		String startTrim = startDate == null ? "" : startDate.trim();
		String endTrim = endDate == null ? "" : endDate.trim();
		if (!StringUtils.hasText(startTrim) || !StringUtils.hasText(endTrim)) {
			throw new ResourceException("请选择对应的日期范围");
		}
		try {
			LocalDate.parse(startTrim);
			LocalDate.parse(endTrim);
		} catch (DateTimeParseException e) {
			throw new ResourceException("日期格式有误");
		}
		String startDateTime = startTrim + " 00:00:00";
		String endDateTime = endTrim + " 23:59:59";

		Long distributorId = null;
		String distS = distributorIdRaw == null ? "0" : distributorIdRaw.trim();
		try {
			long d = Long.parseLong(distS);
			if (d > 0L) {
				distributorId = d;
			}
		} catch (NumberFormatException ignored) {
			// omit filter
		}

		String orderId = null;
		String orderS = orderIdRaw == null ? "0" : orderIdRaw.trim();
		try {
			long oid = Long.parseLong(orderS);
			if (oid > 0L) {
				orderId = orderS;
			}
		} catch (NumberFormatException ignored) {
			// omit filter
		}

		String cs = cashStatusRaw == null ? "" : cashStatusRaw.trim();

		LinkedHashMap<String, Object> jobFilter = new LinkedHashMap<>();
		jobFilter.put("company_id", companyId);
		jobFilter.put("start_date", startDateTime);
		jobFilter.put("end_date", endDateTime);
		if (distributorId != null) {
			jobFilter.put("distributor_id", distributorId);
		}
		if (orderId != null) {
			jobFilter.put("order_id", orderId);
		}
		if (StringUtils.hasText(cs)) {
			jobFilter.put("cash_status", cs);
		}
		hfpayWithdrawRecordExportFileJobDispatchPublisher.publish(companyId, operatorId, jobFilter);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", payload);
		return ResponseEntity.ok(body);
	}
}
