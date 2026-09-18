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
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.community.domain.dto.CommunityChiefListRowDto;
import cn.shopex.ecshopx.community.service.CommunityChiefCashWithdrawalProcessService;
import cn.shopex.ecshopx.community.service.CommunityChiefCashWithdrawalQueryService;
import cn.shopex.ecshopx.community.service.CommunityChiefService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.common.port.MerchantPaymentTradeQueryPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@ShopLog
@RestController("communityAdminV1ChiefCashWithdrawal")
@RequestMapping("/api/v1/community")
public class CommunityChiefCashWithdrawalController {

	private final CommunityChiefCashWithdrawalProcessService communityChiefCashWithdrawalProcessService;
	private final CommunityChiefCashWithdrawalQueryService communityChiefCashWithdrawalQueryService;
	private final MerchantPaymentTradeQueryPort merchantPaymentTradeQueryService;
	private final CommunityChiefService communityChiefService;
	private final ObjectMapper objectMapper;

	public CommunityChiefCashWithdrawalController(
			CommunityChiefCashWithdrawalProcessService communityChiefCashWithdrawalProcessService,
			CommunityChiefCashWithdrawalQueryService communityChiefCashWithdrawalQueryService,
			MerchantPaymentTradeQueryPort merchantPaymentTradeQueryService,
			CommunityChiefService communityChiefService,
			ObjectMapper objectMapper) {
		this.communityChiefCashWithdrawalProcessService = communityChiefCashWithdrawalProcessService;
		this.communityChiefCashWithdrawalQueryService = communityChiefCashWithdrawalQueryService;
		this.merchantPaymentTradeQueryService = merchantPaymentTradeQueryService;
		this.communityChiefService = communityChiefService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "community.chief.rebate.count")
	@GetMapping(value = "/rebate/count", name = "团长业绩列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getChiefRebateCount(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(value = "chief_name", required = false) String chiefName,
			@RequestParam(value = "chief_mobile", required = false) String chiefMobile) {
		int page = parseCashWithdrawalListPage(pageParam);
		int pageSize = parseCashWithdrawalListPageSize(pageSizeParam);

		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);
		String operatorType = stringVal(ud.get("operator_type"));

		Long distributorIdFilter;
		if (Objects.equals(operatorType, "distributor")) {
			Integer parsed = parseIntFromStringLooseOrNull(request.getParameter("distributor_id"));
			distributorIdFilter = parsed == null ? null : Long.valueOf(parsed.longValue());
		} else {
			distributorIdFilter = 0L;
		}

		String chiefNameOrNull =
				chiefName == null || !StringUtils.hasText(chiefName.trim()) ? null : chiefName.trim();
		String chiefMobileOrNull =
				chiefMobile == null || !StringUtils.hasText(chiefMobile.trim()) ? null : chiefMobile.trim();

		Map<String, Object> data =
				communityChiefService.listChiefsByDistributorJoin(
						distributorIdFilter, companyId, chiefNameOrNull, chiefMobileOrNull, page, pageSize);

		Object listObj = data.get("list");
		if (!(listObj instanceof List<?> list) || list.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(data));
		}

		List<Long> chiefIds = new ArrayList<>();
		for (Object el : list) {
			if (el instanceof CommunityChiefListRowDto dto && dto.getChiefId() != null) {
				chiefIds.add(dto.getChiefId());
			}
		}
		if (chiefIds.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(data));
		}

		Map<Long, Map<String, Object>> agg =
				communityChiefCashWithdrawalQueryService.buildChiefRebateAggregateByChiefIds(companyId, chiefIds);

		List<Map<String, Object>> outputList = new ArrayList<>(list.size());
		for (Object el : list) {
			CommunityChiefListRowDto dto = (CommunityChiefListRowDto) el;
			Map<String, Object> rowMap =
					new LinkedHashMap<>(
							objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {}));
			Long chiefId = dto.getChiefId();
			Map<String, Object> aggRow = chiefId != null ? agg.get(chiefId) : null;
			rowMap.put("cash_withdrawal_rebate", longOrZero(aggRow, "cash_withdrawal_rebate"));
			rowMap.put("payed_rebate", longOrZero(aggRow, "payed_rebate"));
			rowMap.put("freeze_cash_withdrawal_rebate", longOrZero(aggRow, "freeze_cash_withdrawal_rebate"));
			rowMap.put("no_close_rebate", longOrZero(aggRow, "no_close_rebate"));
			rowMap.put("rebate_total", longOrZero(aggRow, "rebate_total"));
			outputList.add(rowMap);
		}
		data.put("list", outputList);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.cash_withdrawal.list")
	@GetMapping(value = "/cash_withdrawal", name = "团长佣金提现列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCashWithdrawalList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageParam,
			@RequestParam(value = "pageSize", required = false) String pageSizeParam,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "status", required = false) String status) {
		int page = parseCashWithdrawalListPage(pageParam);
		int pageSize = parseCashWithdrawalListPageSize(pageSizeParam);

		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);
		String operatorType = stringVal(ud.get("operator_type"));

		int distributorId = 0;
		if (Objects.equals(operatorType, "distributor")) {
			distributorId = parseIntLoose(request.getParameter("distributor_id"), 0);
		}

		Map<String, Object> data =
				communityChiefCashWithdrawalQueryService.buildListResponse(
						companyId, operatorType, distributorId, mobile, status, page, pageSize, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.cash_withdrawal.process")
	@PostMapping(value = "/cash_withdrawal/{cash_withdrawal_id}", name = "处理团长佣金提现申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> processCashWithdrawal(
			HttpServletRequest request,
			@PathVariable("cash_withdrawal_id") String cashWithdrawalId,
			@RequestParam(value = "process_type", required = false) String processType,
			@RequestParam(value = "remarks", required = false) String remarks) {
		final long id;
		try {
			id = Long.parseLong(cashWithdrawalId.trim());
		} catch (Exception e) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}

		if (processType == null || !StringUtils.hasText(processType.trim())) {
			throw new BadRequestException("参数错误");
		}
		String normalizedProcessType = processType.trim();

		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		String clientIp = clientIpForWithdrawal(request);
		boolean ok = communityChiefCashWithdrawalProcessService.process(
				companyId, id, normalizedProcessType, remarks, clientIp);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", ok)));
	}

	@Activated(routeAlias = "community.chief.cash_withdrawal.payinfo")
	@GetMapping(value = "/cash_withdrawal/payinfo/{cash_withdrawal_id}", name = "获取佣金提现支付信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMerchantTradeList(
			HttpServletRequest request, @PathVariable("cash_withdrawal_id") String cashWithdrawalId) {
		if (cashWithdrawalId == null || !StringUtils.hasText(cashWithdrawalId.trim())) {
			throw new BadRequestException("参数错误");
		}
		String relSceneId = cashWithdrawalId.trim();

		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLongCompany(cid);

		int totalCount = merchantPaymentTradeQueryService.countByCommunityChiefCashWithdrawal(companyId, relSceneId);
		List<Map<String, Object>> list =
				merchantPaymentTradeQueryService.listByCommunityChiefCashWithdrawal(companyId, relSceneId, 1, 100);
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
		return StringUtils.hasText(ip) ? ip : null;
	}

	private static int parseIntLoose(String v, int defaultVal) {
		if (v == null || !StringUtils.hasText(v.trim())) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int parseCashWithdrawalListPage(String raw) {
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

	private static int parseCashWithdrawalListPageSize(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new BadRequestException("每页最多查询100条数据");
		}
		int v;
		try {
			v = Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("每页最多查询100条数据");
		}
		if (v < 1 || v > 100) {
			throw new BadRequestException("每页最多查询100条数据");
		}
		return v;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static Integer parseIntFromStringLooseOrNull(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longOrZero(Map<String, Object> aggRow, String key) {
		if (aggRow == null) {
			return 0L;
		}
		Object o = aggRow.get(key);
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}
}
