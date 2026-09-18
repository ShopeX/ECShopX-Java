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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalListCoreService;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalListRequestGate;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalPayinfoRequestGate;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalProcessRequestGate;
import cn.shopex.ecshopx.distribution.service.CashWithdrawalProcessService;
import cn.shopex.ecshopx.common.port.MerchantPaymentTradeQueryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1CashWithdrawal")
@RequestMapping("/api/v1")
public class CashWithdrawalController {

	private static final String CASH_WITHDRAWAL_PAGE_ERROR = "分页参数错误";
	private static final String CASH_WITHDRAWAL_PAGESIZE_ERROR = "每页最多查询50条数据";

	private final CashWithdrawalListRequestGate cashWithdrawalListRequestGate;
	private final CashWithdrawalListCoreService cashWithdrawalListCoreService;
	private final CashWithdrawalPayinfoRequestGate cashWithdrawalPayinfoRequestGate;
	private final MerchantPaymentTradeQueryPort merchantPaymentTradeQueryService;
	private final CashWithdrawalProcessRequestGate cashWithdrawalProcessRequestGate;
	private final CashWithdrawalProcessService cashWithdrawalProcessService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public CashWithdrawalController(
			CashWithdrawalListRequestGate cashWithdrawalListRequestGate,
			CashWithdrawalListCoreService cashWithdrawalListCoreService,
			CashWithdrawalPayinfoRequestGate cashWithdrawalPayinfoRequestGate,
			MerchantPaymentTradeQueryPort merchantPaymentTradeQueryService,
			CashWithdrawalProcessRequestGate cashWithdrawalProcessRequestGate,
			CashWithdrawalProcessService cashWithdrawalProcessService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.cashWithdrawalListRequestGate = cashWithdrawalListRequestGate;
		this.cashWithdrawalListCoreService = cashWithdrawalListCoreService;
		this.cashWithdrawalPayinfoRequestGate = cashWithdrawalPayinfoRequestGate;
		this.merchantPaymentTradeQueryService = merchantPaymentTradeQueryService;
		this.cashWithdrawalProcessRequestGate = cashWithdrawalProcessRequestGate;
		this.cashWithdrawalProcessService = cashWithdrawalProcessService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "distribution.cash_withdrawal.list")
	@GetMapping(value = "/distribution/cash_withdrawals", name = "获取佣金提现列表")
	public ResponseEntity<?> getCashWithdrawalList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorId) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (page != null) {
			merged.put("page", page);
		}
		if (pageSize != null) {
			merged.put("pageSize", pageSize);
		}
		if (StringUtils.hasText(mobile)) {
			merged.put("mobile", mobile);
		}
		if (request.getParameterMap().containsKey("status")) {
			merged.put("status", status);
			merged.put("statusQueryPresent", Boolean.TRUE);
		} else {
			merged.put("statusQueryPresent", Boolean.FALSE);
		}
		merged.put("distributor_id", distributorId);
		try {
			Map<String, Object> user = cashWithdrawalListRequestGate.validateBeforeList(request, merged);
			Map<String, Object> data = cashWithdrawalListCoreService.buildList(user, merged);
			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			String msg = ex.getMessage();
			if (CASH_WITHDRAWAL_PAGE_ERROR.equals(msg) || CASH_WITHDRAWAL_PAGESIZE_ERROR.equals(msg)) {
				LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
				payload.put("message", msg);
				payload.put("status_code", 422);
				LinkedHashMap<String, Object> root = new LinkedHashMap<>();
				root.put("data", payload);
				return ResponseEntity.ok(root);
			}
			throw ex;
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400)
	@Activated(routeAlias = "distribution.cash_withdrawal.process")
	@PutMapping(value = "/distribution/cash_withdrawal/{id}", name = "处理佣金提现申请")
	public ResponseEntity<?> processCashWithdrawal(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestParam(value = "process_type", required = false) String processTypeQuery,
			@RequestParam(value = "remarks", required = false) String remarksQuery,
			@FlexibleBody(required = false) Map<String, Object> body) {
		final long cashWithdrawalId;
		try {
			cashWithdrawalId = Long.parseLong(id.trim());
		} catch (Exception e) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}

		String resolvedProcessType = null;
		if (StringUtils.hasText(processTypeQuery)) {
			resolvedProcessType = processTypeQuery.trim();
		} else if (body != null && body.get("process_type") != null) {
			resolvedProcessType = body.get("process_type").toString().trim();
		}
		if (!StringUtils.hasText(resolvedProcessType)) {
			throw new ResourceException("参数错误");
		}

		String resolvedRemarks = remarksQuery;
		if (!StringUtils.hasText(resolvedRemarks) && body != null && body.containsKey("remarks")) {
			Object r = body.get("remarks");
			resolvedRemarks = r == null ? null : r.toString();
		}

		Map<String, Object> user = cashWithdrawalProcessRequestGate.validateBeforeProcess(request);
		Object companyIdObj = user.get("company_id");
		long companyId = toLongCompany(companyIdObj);

		String clientIp = clientIpForWithdrawal(request);
		boolean status = cashWithdrawalProcessService.process(user, cashWithdrawalId, resolvedProcessType, resolvedRemarks, clientIp);

		long operatorId = toLongCompany(user.get("operator_id"));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("id", cashWithdrawalId);
		logParams.put("process_type", resolvedProcessType);
		logParams.put("remarks", resolvedRemarks);

		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/distribution/cash_withdrawal/" + cashWithdrawalId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "处理佣金提现申请");
		logCtx.put("log_type", "operator");
		Object merchantId = user.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@Activated(routeAlias = "front.wxapp.cashWithdrawal.payinfo")
	@GetMapping(value = "/distributor/cash_withdrawal/payinfo/{cash_withdrawal_id}", name = "获取佣金提现支付信息")
	public ResponseEntity<?> getMerchantTradeList(
			HttpServletRequest request, @PathVariable("cash_withdrawal_id") String cashWithdrawalId) {
		if (cashWithdrawalId == null || !StringUtils.hasText(cashWithdrawalId.trim())) {
			throw new BadRequestException("参数错误");
		}
		String relSceneId = cashWithdrawalId.trim();
		Map<String, Object> merged = new LinkedHashMap<>();
		Map<String, Object> user = cashWithdrawalPayinfoRequestGate.validateBeforePayinfo(request, merged);
		long companyId = toLongCompany(user.get("company_id"));
		int totalCount = merchantPaymentTradeQueryService.countByRebateCashWithdrawal(companyId, relSceneId);
		List<Map<String, Object>> list =
				merchantPaymentTradeQueryService.listByRebateCashWithdrawal(companyId, relSceneId, 1, 100);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", totalCount);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long toLongCompany(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private static String clientIpForWithdrawal(HttpServletRequest request) {
		String ip = clientIp(request);
		return StringUtils.hasText(ip) ? ip : "127.0.0.1";
	}
}
