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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.mapper.AdapayTradeExportQueryMapper;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayTradeListQueryService {

	private static final int PAGE_SIZE_EXPORT = 500;

	private static final long TIME_RANGE_SECONDS = 3L * 30 * 24 * 3600;

	private final AdapayTradeExportQueryMapper adapayTradeExportQueryMapper;
	private final AdapayTradeDivFeeBatchService adapayTradeDivFeeBatchService;
	private final DistributorListQueryService distributorListQueryService;
	private final AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper;

	public AdapayTradeListQueryService(
			AdapayTradeExportQueryMapper adapayTradeExportQueryMapper,
			AdapayTradeDivFeeBatchService adapayTradeDivFeeBatchService,
			DistributorListQueryService distributorListQueryService,
			AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper) {
		this.adapayTradeExportQueryMapper = adapayTradeExportQueryMapper;
		this.adapayTradeDivFeeBatchService = adapayTradeDivFeeBatchService;
		this.distributorListQueryService = distributorListQueryService;
		this.adapayDealerOperatorResolveHelper = adapayDealerOperatorResolveHelper;
	}

	/**
	 * 将业务 filter 转为 Mapper 形态（就地修改），与列表查询入口一致。
	 */
	public void prepareFilterForTradeExport(Map<String, Object> filter) {
		filter.remove("operator_type");
		Object status = filter.remove("status");
		if (status != null && StringUtils.hasText(status.toString())) {
			filter.put("h_status", status.toString().trim().toUpperCase(Locale.ROOT));
		}
		if (filter.containsKey("can_div")) {
			filter.put("h_canDiv", filter.remove("can_div"));
		}
		Object gte = filter.remove("time_start|gte");
		if (gte != null && StringUtils.hasText(gte.toString())) {
			filter.put("timeStartGte", gte.toString().trim());
			Object lte = filter.remove("time_start|lte");
			if (lte != null && StringUtils.hasText(lte.toString())) {
				filter.put("timeStartLte", lte.toString().trim());
			} else {
				filter.put("timeStartLte", null);
			}
		} else {
			filter.remove("time_start|lte");
		}
	}

	public long countGroupedTrades(Map<String, Object> preparedFilter) {
		long now = Instant.now().getEpochSecond();
		return adapayTradeExportQueryMapper.countGrouped(preparedFilter, now);
	}

	public Map<String, Object> getTradelist(Map<String, Object> jwtMap, HttpServletRequest request) {
		Map<String, Object> empty = emptyTradeListResult();
		long companyId = parseLong(jwtMap.get("company_id"));
		if (companyId <= 0) {
			return empty;
		}
		String rawOperatorType = str(jwtMap.get("operator_type")).trim();
		String outputOperatorType = "staff".equalsIgnoreCase(rawOperatorType) ? "admin" : rawOperatorType;
		long jwtOperatorId = parseLong(jwtMap.get("operator_id"));

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		String st = request.getParameter("status");
		if (StringUtils.hasText(st)) {
			filter.put("status", st.trim().toUpperCase(Locale.ROOT));
		}
		String rawCanDiv = request.getParameter("can_div");
		if (StringUtils.hasText(rawCanDiv)) {
			filter.put("can_div", "true".equals(rawCanDiv));
		}
		String feeMode = request.getParameter("adapay_fee_mode");
		if (StringUtils.hasText(feeMode)) {
			filter.put("adapay_fee_mode", feeMode.trim().toUpperCase(Locale.ROOT));
		}
		String divStatus = request.getParameter("adapay_div_status");
		if (StringUtils.hasText(divStatus)) {
			filter.put("adapay_div_status", divStatus.trim().toUpperCase(Locale.ROOT));
		}
		String payChannel = request.getParameter("pay_channel");
		if (StringUtils.hasText(payChannel)) {
			filter.put("pay_channel", payChannel.trim());
		}
		String orderId = request.getParameter("order_id");
		if (StringUtils.hasText(orderId)) {
			filter.put("order_id", orderId.trim());
		}
		String tradeId = request.getParameter("trade_id");
		if (StringUtils.hasText(tradeId)) {
			filter.put("trade_id", tradeId.trim());
		}
		applyTimeStartRangeFromRequest(filter, request);

		String operatorTypeLower = rawOperatorType.toLowerCase(Locale.ROOT);
		if ("distributor".equals(operatorTypeLower)) {
			long did = parseLong(jwtMap.get("distributor_id"));
			if (did <= 0) {
				return empty;
			}
			filter.put("distributor_id", did);
		} else if ("dealer".equals(operatorTypeLower)) {
			long dealerId = adapayDealerOperatorResolveHelper.resolveMainDealerOperatorIdOrThrow(jwtOperatorId);
			if (dealerId <= 0) {
				return empty;
			}
			filter.put("dealer_id", dealerId);
		}

		String distributorName = request.getParameter("distributor_name");
		if (distributorName != null
				&& StringUtils.hasText(distributorName.trim())
				&& !"0".equals(distributorName.trim())) {
			List<Long> ids = distributorListQueryService.listDistributorIdsByCompanyAndNameContains(
					companyId, distributorName.trim());
			if (ids.isEmpty()) {
				return empty;
			}
			filter.put("distributor_id", ids);
		}

		int page = parsePageInt(request, "page", 1);
		int pageSize = parsePageInt(request, "pageSize", 20);
		filter.put("operator_type", rawOperatorType);

		Map<String, Object> prepared = new LinkedHashMap<>(filter);
		prepareFilterForTradeExport(prepared);
		long now = Instant.now().getEpochSecond();

		List<Map<String, Object>> allRows = adapayTradeExportQueryMapper.selectGroupedAll(prepared, now);
		int totalCount = allRows.size();

		long sumTotalFee = 0L;
		long sumPayFeeNet = 0L;
		long sumAdapayFee = 0L;
		for (Map<String, Object> row : allRows) {
			long payFee = toMoneyLong(row.get("payFee"));
			long refunded = toMoneyLong(row.get("refundedFee"));
			long adapayFee = toMoneyLong(row.get("adapayFee"));
			sumTotalFee += payFee;
			sumPayFeeNet += payFee - refunded;
			sumAdapayFee += adapayFee;
		}
		List<String> allTradeIds =
				allRows.stream().map(r -> str(r.get("tradeId"))).filter(StringUtils::hasText).toList();
		Map<String, Long> divMap =
				adapayTradeDivFeeBatchService.sumDivFeeByTradeIds(companyId, allTradeIds, outputOperatorType);
		long divFeeTotal = divMap.values().stream().mapToLong(Long::longValue).sum();

		Map<String, Object> total = new LinkedHashMap<>();
		total.put("totalFee", sumTotalFee);
		total.put("payFee", sumPayFeeNet);
		total.put("divFee", divFeeTotal);
		total.put("adapayFee", sumAdapayFee);

		List<Map<String, Object>> pageRaw;
		if (pageSize <= 0) {
			pageRaw = allRows;
		} else {
			int p = Math.max(page, 1);
			long offset = (long) (p - 1) * pageSize;
			pageRaw = adapayTradeExportQueryMapper.selectGroupedPage(prepared, now, offset, pageSize);
		}
		List<Map<String, Object>> list = enrichRows(pageRaw, companyId, outputOperatorType, now);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total", total);
		out.put("total_count", totalCount);
		return out;
	}

	private static Map<String, Object> emptyTradeListResult() {
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("list", List.of());
		Map<String, Object> total0 = new LinkedHashMap<>();
		total0.put("totalFee", 0);
		total0.put("payFee", 0);
		total0.put("divFee", 0);
		total0.put("adapayFee", 0);
		empty.put("total", total0);
		empty.put("total_count", 0);
		return empty;
	}

	private static void applyTimeStartRangeFromRequest(Map<String, Object> filter, HttpServletRequest request) {
		String beginParam = request.getParameter("time_start_begin");
		if (beginParam == null || !StringUtils.hasText(beginParam.trim())) {
			return;
		}
		String gteStr = takeFirst10Chars(beginParam.trim());
		String lteParam = request.getParameter("time_start_end");
		String lteStr = lteParam != null ? takeFirst10Chars(lteParam.trim()) : "";

		String gteTrim = gteStr.trim();
		String lteTrim = lteStr.trim();
		if (gteTrim.matches("^-?\\d+$")
				&& lteTrim.matches("^-?\\d+$")
				&& gteTrim.length() <= 12
				&& lteTrim.length() <= 12) {
			try {
				long gteNum = Long.parseLong(gteTrim);
				long lteNum = Long.parseLong(lteTrim);
				if (lteNum - gteNum > TIME_RANGE_SECONDS) {
					gteNum = lteNum - TIME_RANGE_SECONDS;
					gteStr = Long.toString(gteNum);
				}
			} catch (NumberFormatException ignored) {
				// no numeric clamp
			}
		} else {
			try {
				if (StringUtils.hasText(gteTrim) && StringUtils.hasText(lteTrim)) {
					LocalDate gteLd = LocalDate.parse(gteTrim, DateTimeFormatter.ISO_LOCAL_DATE);
					LocalDate lteLd = LocalDate.parse(lteTrim, DateTimeFormatter.ISO_LOCAL_DATE);
					if (ChronoUnit.DAYS.between(gteLd, lteLd) > 90) {
						gteLd = lteLd.minusDays(90);
						gteStr = gteLd.format(DateTimeFormatter.ISO_LOCAL_DATE);
					}
				}
			} catch (Exception ignored) {
				// no calendar clamp
			}
		}
		filter.put("time_start|gte", gteStr);
		filter.put("time_start|lte", lteStr);
	}

	private static String takeFirst10Chars(String s) {
		if (s.length() >= 10) {
			return s.substring(0, 10);
		}
		return s;
	}

	private static int parsePageInt(HttpServletRequest request, String name, int defaultValue) {
		String v = request.getParameter(name);
		if (!StringUtils.hasText(v)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public List<Map<String, Object>> loadTradePage(
			Map<String, Object> preparedFilter, String outputOperatorType, int pageOneBased, int pageSize) {
		long now = Instant.now().getEpochSecond();
		int p = Math.max(pageOneBased, 1);
		int ps = pageSize > 0 ? pageSize : PAGE_SIZE_EXPORT;
		long offset = (long) (p - 1) * ps;
		List<Map<String, Object>> raw =
				adapayTradeExportQueryMapper.selectGroupedPage(preparedFilter, now, offset, ps);
		long companyId = toLong(preparedFilter.get("company_id"));
		return enrichRows(raw, companyId, outputOperatorType, now);
	}

	private List<Map<String, Object>> enrichRows(
			List<Map<String, Object>> raw, long companyId, String outputOperatorType, long nowEpoch) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		List<String> tradeIds =
				raw.stream().map(m -> str(m.get("tradeId"))).filter(StringUtils::hasText).collect(Collectors.toList());
		Map<String, Long> divByTrade =
				adapayTradeDivFeeBatchService.sumDivFeeByTradeIds(companyId, tradeIds, outputOperatorType);
		List<Long> distIds = new ArrayList<>();
		for (Map<String, Object> row : raw) {
			Long did = toLongObj(row.get("distributorId"));
			if (did != null && did > 0) {
				distIds.add(did);
			}
		}
		Map<Long, String> distNameById = new LinkedHashMap<>();
		if (!distIds.isEmpty()) {
			List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, distIds);
			for (Distributor d : distributors) {
				if (d.getDistributorId() != null) {
					distNameById.put(d.getDistributorId(), d.getName() != null ? d.getName() : "");
				}
			}
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : raw) {
			out.add(enrichRow(row, divByTrade, distNameById, nowEpoch));
		}
		return out;
	}

	private Map<String, Object> enrichRow(
			Map<String, Object> row,
			Map<String, Long> divByTrade,
			Map<Long, String> distNameById,
			long nowEpoch) {
		Map<String, Object> m = new LinkedHashMap<>(row);
		Object divStatus = m.get("adapayDivStatus");
		if (divStatus == null || !StringUtils.hasText(String.valueOf(divStatus))) {
			m.put("adapayDivStatus", "NOTDIV");
		}
		long refunded = toMoneyLong(m.get("refundedFee"));
		long payFee = toMoneyLong(m.get("payFee"));
		String tradeState;
		if (refunded <= 0) {
			tradeState = "SUCCESS";
		} else if (refunded < payFee) {
			tradeState = "PARTIAL_REFUND";
		} else {
			tradeState = "FULL_REFUND";
		}
		m.put("tradeState", tradeState);
		String tid = str(m.get("tradeId"));
		m.put("divFee", divByTrade.getOrDefault(tid, 0L));
		Long did = toLongObj(m.get("distributorId"));
		if (did != null && distNameById.containsKey(did)) {
			m.put("distributor_name", distNameById.get(did));
		}
		boolean canDiv = computeCanDiv(m, refunded, payFee, nowEpoch);
		m.put("canDiv", canDiv);
		return m;
	}

	private static boolean computeCanDiv(Map<String, Object> row, long refundedFee, long payFee, long nowEpoch) {
		if (refundedFee >= payFee) {
			return false;
		}
		Object normalOrderId = row.get("normalOrderId");
		if (normalOrderId == null || !StringUtils.hasText(String.valueOf(normalOrderId))) {
			return true;
		}
		Object closeRaw = row.get("closeAftersalesTime");
		long close = toMoneyLong(closeRaw);
		return close > 0 && close < nowEpoch;
	}

	private static long toMoneyLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return new java.math.BigDecimal(o.toString().trim()).longValue();
		} catch (Exception e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static Long toLongObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
