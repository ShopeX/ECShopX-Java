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

package cn.shopex.ecshopx.popularize.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.MerchantPaymentTradeQueryPort;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageCountReadService;
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageListReadService;
import cn.shopex.ecshopx.popularize.service.PopularizeCashWithdrawalListReadService;
import cn.shopex.ecshopx.popularize.service.PopularizeCashWithdrawalProcessRequestGate;
import cn.shopex.ecshopx.popularize.service.PopularizeCashWithdrawalProcessService;
import cn.shopex.ecshopx.popularize.service.PopularizeTaskBrokerageCountExportService;
import cn.shopex.ecshopx.popularize.service.PopularizeTaskBrokerageCountListQueryService;
import cn.shopex.ecshopx.popularize.service.PopularizeTaskBrokerageLogsListReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
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

@AdminAuth
@ShopLog
@RestController("popularizeBrokerageAdminV1")
@RequestMapping("/api/v1")
public class BrokerageController {

	private final PopularizeCashWithdrawalProcessRequestGate popularizeCashWithdrawalProcessRequestGate;
	private final PopularizeCashWithdrawalProcessService popularizeCashWithdrawalProcessService;
	private final PopularizeBrokerageCountReadService popularizeBrokerageCountReadService;
	private final PopularizeBrokerageListReadService popularizeBrokerageListReadService;
	private final PopularizeCashWithdrawalListReadService popularizeCashWithdrawalListReadService;
	private final MerchantPaymentTradeQueryPort merchantPaymentTradeQueryService;
	private final PopularizeTaskBrokerageCountExportService popularizeTaskBrokerageCountExportService;
	private final PopularizeTaskBrokerageCountListQueryService popularizeTaskBrokerageCountListQueryService;
	private final PopularizeTaskBrokerageLogsListReadService popularizeTaskBrokerageLogsListReadService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MembersMapper membersMapper;

	public BrokerageController(
			PopularizeCashWithdrawalProcessRequestGate popularizeCashWithdrawalProcessRequestGate,
			PopularizeCashWithdrawalProcessService popularizeCashWithdrawalProcessService,
			PopularizeBrokerageCountReadService popularizeBrokerageCountReadService,
			PopularizeBrokerageListReadService popularizeBrokerageListReadService,
			PopularizeCashWithdrawalListReadService popularizeCashWithdrawalListReadService,
				MerchantPaymentTradeQueryPort merchantPaymentTradeQueryService,
			PopularizeTaskBrokerageCountExportService popularizeTaskBrokerageCountExportService,
			PopularizeTaskBrokerageCountListQueryService popularizeTaskBrokerageCountListQueryService,
			PopularizeTaskBrokerageLogsListReadService popularizeTaskBrokerageLogsListReadService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MembersMapper membersMapper) {
		this.popularizeCashWithdrawalProcessRequestGate = popularizeCashWithdrawalProcessRequestGate;
		this.popularizeCashWithdrawalProcessService = popularizeCashWithdrawalProcessService;
		this.popularizeBrokerageCountReadService = popularizeBrokerageCountReadService;
		this.popularizeBrokerageListReadService = popularizeBrokerageListReadService;
		this.popularizeCashWithdrawalListReadService = popularizeCashWithdrawalListReadService;
		this.merchantPaymentTradeQueryService = merchantPaymentTradeQueryService;
		this.popularizeTaskBrokerageCountExportService = popularizeTaskBrokerageCountExportService;
		this.popularizeTaskBrokerageCountListQueryService = popularizeTaskBrokerageCountListQueryService;
		this.popularizeTaskBrokerageLogsListReadService = popularizeTaskBrokerageLogsListReadService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.membersMapper = membersMapper;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = false,
			notFound = false)
	@Activated(routeAlias = "popularize.cash_withdrawals.process")
	@PutMapping(value = "/popularize/cash_withdrawals/{cash_withdrawal_id}", name = "处理推广员佣金提现申请")
	public ResponseEntity<?> processCashWithdrawal(
			@PathVariable("cash_withdrawal_id") String cashWithdrawalId,
			@RequestParam(value = "process_type", required = false) String processType,
			@RequestParam(value = "remarks", required = false) String remarks,
			HttpServletRequest request) {
		popularizeCashWithdrawalProcessRequestGate.validateBeforeProcess(request);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());
		popularizeCashWithdrawalProcessService.process(companyId, cashWithdrawalId, processType, remarks);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = false,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "popularize.cash_withdrawals.list.get")
	@GetMapping(value = "/popularize/cashWithdrawals", name = "获取佣金提现列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCashWithdrawalList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "status", required = false) String status) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());
		String mobileArg =
				(mobile != null && StringUtils.hasText(mobile.trim())) ? mobile.trim() : null;
		String statusArg =
				(status != null && StringUtils.hasText(status.trim())) ? status.trim() : null;
		Map<String, Object> data =
				popularizeCashWithdrawalListReadService.getCashWithdrawalList(companyId, page, pageSize, mobileArg, statusArg);
		Object blockAttr = request.getAttribute("x-datapass-block");
		if (truthyDatapassBlock(blockAttr)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("list");
			if (rows != null) {
				for (Map<String, Object> row : rows) {
					Object mob = row.get("mobile");
					if (mob != null) {
						String plain = String.valueOf(mob).trim();
						if (!plain.isEmpty()) {
							row.put("mobile", DataMasking.maskMobile(String.valueOf(mob)));
						}
					}
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static boolean truthyDatapassBlock(Object o) {
		if (Boolean.TRUE.equals(o)) {
			return true;
		}
		if (o instanceof Number n && n.intValue() != 0) {
			return true;
		}
		if (o != null) {
			String t = o.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return true;
			}
		}
		return false;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = false,
			notFound = false)
	@Activated(routeAlias = "popularize.cash_withdrawals.pay.list.get")
	@GetMapping(value = "/popularize/cashWithdrawal/payinfo/{cash_withdrawal_id}", name = "获取佣金提现支付信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMerchantTradeList(
			HttpServletRequest request, @PathVariable("cash_withdrawal_id") String cashWithdrawalId) {
		if (cashWithdrawalId == null || !StringUtils.hasText(cashWithdrawalId.trim())) {
			throw new BadRequestException("参数错误");
		}
		String relSceneId = cashWithdrawalId.trim();
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());
		int totalCount = merchantPaymentTradeQueryService.countByPopularizeRebateCashWithdrawal(companyId, relSceneId);
		List<Map<String, Object>> list =
				merchantPaymentTradeQueryService.listByPopularizeRebateCashWithdrawal(companyId, relSceneId, 1, 100);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = false,
			notFound = false)
	@Activated(routeAlias = "popularize.brokerage.count")
	@GetMapping(value = "/popularize/brokerage/count", name = "获取佣金统计")
	public ResponseEntity<?> brokerageCount(
			@RequestParam(value = "user_id", required = false) String userId,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorId,
			HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());
		Map<String, Object> data = popularizeBrokerageCountReadService.brokerageCount(companyId, userId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = false,
			notFound = false)
	@Activated(routeAlias = "popularize.brokerage.logs")
	@GetMapping(value = "/popularize/brokerage/logs", name = "获取佣金记录")
	public ResponseEntity<?> getBrokerageList(
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "user_id", required = false) String userId,
			@RequestParam(value = "is_close", required = false) String isClose,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());
		Map<String, Object> data =
				popularizeBrokerageListReadService.getBrokerageList(companyId, page, pageSize, userId, isClose, distributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = false,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "popularize.task.brokerage.logs")
	@GetMapping(value = "/popularize/taskBrokerage/logs", name = "获取任务佣金记录")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTaskBrokerageList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "promoter_mobile", required = false) String promoterMobile,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "time_start", required = false) String timeStart,
			@RequestParam(value = "time_end", required = false) String timeEnd,
			@RequestParam(value = "plan_date", required = false) String planDate) {
		int pageInt = parseExportPage(page);
		int pageSizeInt = parseExportPageSize(pageSize);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());

		Long resolvedUserId = null;
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(promoterMobile)) {
			String enc = sensitiveFieldEncryptor.encrypt(promoterMobile.trim());
			Members found =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getMobile, enc));
			if (found == null || found.getUserId() == null) {
				return ResponseEntity.ok(
						ApiResult.ok(Map.of("total_count", 0L, "list", Collections.emptyList())));
			}
			resolvedUserId = found.getUserId();
		}

		Map<String, Object> filter =
				popularizeTaskBrokerageLogsListReadService.buildLogsListFilter(
						companyId, resolvedUserId, orderId, itemName, status, timeStart, timeEnd, planDate);
		Map<String, Object> data =
				popularizeTaskBrokerageLogsListReadService.getTaskBrokerageList(filter, pageInt, pageSizeInt);

		Object blockAttr = request.getAttribute("x-datapass-block");
		if (truthyDatapassBlock(blockAttr)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("list");
			if (rows != null) {
				for (Map<String, Object> row : rows) {
					Object pm = row.get("promoter_mobile");
					if (pm != null) {
						String plain = String.valueOf(pm).trim();
						if (!plain.isEmpty()) {
							row.put("promoter_mobile", DataMasking.maskMobile(String.valueOf(pm)));
						}
					}
					Object bm = row.get("buy_mobile");
					if (bm != null) {
						String plain = String.valueOf(bm).trim();
						if (!plain.isEmpty()) {
							row.put("buy_mobile", DataMasking.maskMobile(String.valueOf(bm)));
						}
					}
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "popularize.task.brokerage.count")
	@GetMapping(value = "/popularize/taskBrokerage/count", name = "获取任务佣金统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTaskBrokerageCountList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "promoter_mobile", required = false) String promoterMobile,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "time_start", required = false) String timeStart,
			@RequestParam(value = "time_end", required = false) String timeEnd,
			@RequestParam(value = "plan_date", required = false) String planDate) {
		int pageInt = parseExportPage(page);
		int pageSizeInt = parseExportPageSize(pageSize);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());

		Long resolvedUserId = null;
		if (PopularizeTaskBrokerageCountListQueryService.nonEmptyFilterString(promoterMobile)) {
			String enc = sensitiveFieldEncryptor.encrypt(promoterMobile.trim());
			Members found =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getMobile, enc));
			if (found == null || found.getUserId() == null) {
				return ResponseEntity.ok(
						ApiResult.ok(Map.of("total_count", 0L, "list", Collections.emptyList())));
			}
			resolvedUserId = found.getUserId();
		}

		Map<String, Object> filter =
				popularizeTaskBrokerageCountListQueryService.buildCountListFilter(
						companyId, resolvedUserId, itemName, timeStart, timeEnd, planDate);
		Map<String, Object> data =
				popularizeTaskBrokerageCountListQueryService.getTaskBrokerageCountList(
						filter, "*", pageInt, pageSizeInt);

		Object blockAttr = request.getAttribute("x-datapass-block");
		if (truthyDatapassBlock(blockAttr)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("list");
			if (rows != null) {
				for (Map<String, Object> row : rows) {
					Object mob = row.get("promoter_mobile");
					if (mob != null) {
						String plain = String.valueOf(mob).trim();
						if (!plain.isEmpty()) {
							row.put("promoter_mobile", DataMasking.maskMobile(String.valueOf(mob)));
						}
					}
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO_FIXED,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = false,
			notFound = false)
	@DataPass
	@Activated(routeAlias = "popularize.task.brokerage.count.export")
	@GetMapping(value = "/popularize/export/taskBrokerage/count", name = "获取任务佣金统计")
	public ResponseEntity<?> exportTaskBrokerageCount(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "promoter_mobile", required = false) String promoterMobile,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "time_start", required = false) String timeStart,
			@RequestParam(value = "time_end", required = false) String timeEnd,
			@RequestParam(value = "plan_date", required = false) String planDate) {
		validateTaskBrokerageExportPagination(page, pageSize);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = Long.parseLong(String.valueOf(jwt.get("company_id")).trim());
		String timeStartArg = normalizeOptionalQuery(timeStart);
		String timeEndArg = normalizeOptionalQuery(timeEnd);
		if (timeStartArg == null || timeEndArg == null) {
			timeStartArg = null;
			timeEndArg = null;
		}
		String planDateArg = normalizePlanDate(planDate);

		boolean datapassBlock = truthyDatapassBlock(request.getAttribute("x-datapass-block"));
		Object body =
				popularizeTaskBrokerageCountExportService.exportTaskBrokerageCount(
						companyId,
						page,
						pageSize,
						promoterMobile,
						itemName,
						timeStartArg,
						timeEndArg,
						planDateArg,
						datapassBlock);
		if (body instanceof ApiResult<?>) {
			return ResponseEntity.ok(body);
		}
		if (body instanceof List<?> list) {
			return ResponseEntity.ok(Collections.singletonMap("data", list));
		}
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, String> s3 = (LinkedHashMap<String, String>) body;
		return ResponseEntity.ok(s3);
	}

	private static void validateTaskBrokerageExportPagination(String page, String pageSize) {
		parseExportPage(page);
		parseExportPageSize(pageSize);
	}

	private static int parseExportPage(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new BadRequestException("分页参数错误");
		}
		int v;
		try {
			v = Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
		if (v < 1) {
			throw new BadRequestException("分页参数错误");
		}
		return v;
	}

	private static int parseExportPageSize(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new BadRequestException("分页参数错误");
		}
		int v;
		try {
			v = Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
		if (v < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (v > 50) {
			throw new BadRequestException("每页最多查询50条数据");
		}
		return v;
	}

	private static String normalizeOptionalQuery(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		return s.trim();
	}

	/**
	 * 将计划日期参数归一化为当月最后一天的 {@code yyyy-MM-dd}，作为 {@code plan_date} 查询条件。
	 * 支持 {@code yyyy-MM-dd}（按该日所在月取月末）或 {@code yyyy-MM}；空或仅空白返回 {@code null}（不应用计划日过滤）。
	 * 无法解析的字符串返回 {@code null}，效果与不传计划日相同。
	 */
	private static String normalizePlanDate(String planDate) {
		if (planDate == null || !StringUtils.hasText(planDate.trim())) {
			return null;
		}
		try {
			return PopularizeTaskBrokerageCountListQueryService.endOfMonthYmd(planDate.trim());
		} catch (Exception e) {
			return null;
		}
	}
}
