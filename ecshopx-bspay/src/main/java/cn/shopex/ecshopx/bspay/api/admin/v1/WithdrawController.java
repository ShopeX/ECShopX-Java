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

package cn.shopex.ecshopx.bspay.api.admin.v1;

import cn.shopex.ecshopx.bspay.api.admin.v1.request.WithdrawApplyBody;
import cn.shopex.ecshopx.bspay.api.admin.v1.request.WithdrawAuditBody;
import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.service.WithdrawApplyContext;
import cn.shopex.ecshopx.bspay.service.WithdrawApplyService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.dispatch.WithdrawJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("bspayWithdrawAdminV1")
@RequestMapping("/api/v1/bspay")
@Slf4j
public class WithdrawController {

	private final WithdrawApplyService withdrawApplyService;
	private final WithdrawJobDispatchPublisher withdrawJobDispatchPublisher;

	public WithdrawController(
			WithdrawApplyService withdrawApplyService,
			WithdrawJobDispatchPublisher withdrawJobDispatchPublisher) {
		this.withdrawApplyService = withdrawApplyService;
		this.withdrawJobDispatchPublisher = withdrawJobDispatchPublisher;
	}

	@Activated(routeAlias = "bspay.withdraw.balance")
	@GetMapping(value = "/withdraw/balance", name = "获取提现余额")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBalance(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) raw;
		Object companyIdRaw = jwt.get("company_id");
		if (!(companyIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		String opType = jwt.get("operator_type") == null ? "" : jwt.get("operator_type").toString().trim();

		long merchantId = 0L;
		if ("merchant".equals(opType) && jwtNumericClaimPresent(jwt.get("merchant_id"))) {
			merchantId = parseOptionalLongLenient(jwt.get("merchant_id"));
		}
		long distributorId = 0L;
		if ("distributor".equals(opType) && jwtNumericClaimPresent(jwt.get("distributor_id"))) {
			distributorId = parseOptionalLongLenient(jwt.get("distributor_id"));
		}

		Map<String, Long> balances =
				withdrawApplyService.getUserBalance(companyId, opType, distributorId, merchantId);
		return ResponseEntity.ok(
				ApiResult.ok(
						Map.of(
								"available_balance",
								balances.get("available_balance"),
								"pending_balance",
								balances.get("pending_balance"))));
	}

	@Activated(routeAlias = "bspay.withdraw.apply")
	@PostMapping(value = "/withdraw/apply", name = "申请提现")
	public ResponseEntity<ApiResult<Map<String, Object>>> apply(
			HttpServletRequest request,
			@FlexibleBody WithdrawApplyBody body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) raw;
		Object companyIdRaw = jwt.get("company_id");
		if (!(companyIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		String opType = jwt.get("operator_type") == null ? "" : jwt.get("operator_type").toString().trim();

		long opId = 0L;
		Object opIdRaw = jwt.get("operator_id");
		if (opIdRaw instanceof Number n) {
			opId = n.longValue();
		} else if (opIdRaw instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				try {
					opId = Long.parseLong(t);
				} catch (NumberFormatException e) {
					opId = 0L;
				}
			}
		}

		String mobile = jwt.get("mobile") == null ? "" : jwt.get("mobile").toString();

		long merchantId = 0L;
		long distributorId = 0L;
		if ("distributor".equals(opType)) {
			distributorId = parseRequiredPositiveLong(jwt.get("distributor_id"), "分销商身份信息异常，请重新登录");
		} else if ("merchant".equals(opType)) {
			merchantId = parseRequiredPositiveLong(jwt.get("merchant_id"), "商户身份信息异常，请重新登录");
		}

		var ctx = new WithdrawApplyContext(
				companyId, opType, opId, mobile, merchantId, distributorId,
				body.getAmount(), body.getWithdrawType(), body.getInvoiceUrl());
		WithdrawApply saved = withdrawApplyService.applyWithdraw(ctx);
		return ResponseEntity.ok(
				ApiResult.ok(Map.of("apply_id", saved.getId(), "status", saved.getStatus(), "amount", saved.getAmount())));
	}

	private static boolean jwtNumericClaimPresent(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue();
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		return false;
	}

	private static long parseOptionalLongLenient(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				try {
					return Long.parseLong(t);
				} catch (NumberFormatException e) {
					return 0L;
				}
			}
		}
		return 0L;
	}

	private static long parseRequiredPositiveLong(Object raw, String unauthorizedMessage) {
		long v = 0L;
		if (raw instanceof Number n) {
			v = n.longValue();
		} else if (raw instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				try {
					v = Long.parseLong(t);
				} catch (NumberFormatException e) {
					v = 0L;
				}
			}
		}
		if (v <= 0L) {
			throw new UnauthorizedException(unauthorizedMessage);
		}
		return v;
	}

	@Activated(routeAlias = "bspay.withdraw.lists")
	@GetMapping(value = "/withdraw/lists", name = "获取提现记录列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> lists(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) raw;
		Object companyIdRaw = jwt.get("company_id");
		if (!(companyIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		long jwtOperatorId = 0L;
		Object opIdRaw = jwt.get("operator_id");
		if (opIdRaw instanceof Number n) {
			jwtOperatorId = n.longValue();
		} else if (opIdRaw instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				try {
					jwtOperatorId = Long.parseLong(t);
				} catch (NumberFormatException e) {
					jwtOperatorId = 0L;
				}
			}
		}

		int page = parseWithdrawListPage(request.getParameter("page"));
		int pageSize = parseWithdrawListPageSize(request.getParameter("pageSize"));

		Map<String, Object> data =
				withdrawApplyService.lists(companyId, jwtOperatorId, jwt, request, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseWithdrawListPage(String pageRaw) {
		if (pageRaw == null) {
			return 1;
		}
		String t = pageRaw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("页码必须为大于等于1的整数");
		}
		int page;
		try {
			page = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("页码必须为大于等于1的整数");
		}
		if (page < 1) {
			throw new BadRequestException("页码必须为大于等于1的整数");
		}
		return page;
	}

	private static void validateWithdrawAuditBody(WithdrawAuditBody body) {
		Long applyId = body.getApplyId();
		if (applyId == null || applyId <= 0L) {
			throw new BadRequestException("申请ID必填且必须为整数");
		}
		String action = body.getAction();
		if (action == null) {
			throw new BadRequestException("审核操作必填且只能为approve或reject");
		}
		String trimmed = action.trim();
		if (trimmed.isEmpty() || (!"approve".equals(trimmed) && !"reject".equals(trimmed))) {
			throw new BadRequestException("审核操作必填且只能为approve或reject");
		}
		String remark = body.getRemark();
		if (remark != null && remark.length() > 500) {
			throw new BadRequestException("审核备注不能超过500个字符");
		}
	}

	private static int parseWithdrawListPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null) {
			throw new BadRequestException("每页数量为1-50的整数");
		}
		String t = pageSizeRaw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("每页数量为1-50的整数");
		}
		int ps;
		try {
			ps = Integer.parseInt(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("每页数量为1-50的整数");
		}
		if (ps < 1 || ps > 50) {
			throw new BadRequestException("每页数量为1-50的整数");
		}
		return ps;
	}

	@Activated(routeAlias = "bspay.withdraw.audit")
	@PostMapping(value = "/withdraw/audit", name = "审核提现申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> audit(
			HttpServletRequest request, @FlexibleBody WithdrawAuditBody body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw == null) {
			throw new UnauthorizedException("未登录");
		}
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new UnauthorizedException("未登录");
		}
		if (rawMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) raw;

		String auditor = jwt.get("mobile") == null ? "" : jwt.get("mobile").toString();

		long auditorOperatorId = 0L;
		Object opIdRaw = jwt.get("operator_id");
		if (opIdRaw instanceof Number n) {
			auditorOperatorId = n.longValue();
		} else if (opIdRaw instanceof String s) {
			String t = s.trim();
			if (!t.isEmpty()) {
				try {
					auditorOperatorId = Long.parseLong(t);
				} catch (NumberFormatException e) {
					auditorOperatorId = 0L;
				}
			}
		}

		String operatorTypeRaw =
				jwt.get("operator_type") == null ? "" : jwt.get("operator_type").toString().trim();

		validateWithdrawAuditBody(body);
		String actionTrimmed = body.getAction().trim();
		String remark = body.getRemark() == null ? "" : body.getRemark();

		Map<String, Object> row =
				withdrawApplyService.auditWithdraw(
						body.getApplyId(),
						actionTrimmed,
						auditor,
						auditorOperatorId,
						remark,
						operatorTypeRaw);

		if ("approve".equals(actionTrimmed) && row != null && !row.isEmpty()) {
			withdrawJobDispatchPublisher.enqueueWithdrawJob(body.getApplyId());
			log.info("提现申请审核::approve，已通过异步任务总线入队 apply_id:{}", body.getApplyId());
		}

		LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
		inner.put("id", row.get("id"));
		inner.put("amount", row.get("amount"));
		inner.put("status", row.get("status"));
		inner.put("audit_time", row.get("audit_time"));
		inner.put("auditor", row.get("auditor"));
		inner.put("audit_remark", row.get("audit_remark"));

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE, "data", inner)));
	}

	@Activated(routeAlias = "bspay.withdraw.huifu")
	@PostMapping(value = "/withdraw/huifu", name = "汇付取现接口")
	public ResponseEntity<Void> huifuWithdraw() {
		return ResponseEntity.ok().build();
	}
}
