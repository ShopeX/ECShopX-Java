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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.mapper.BspayTradeExportQueryMapper;
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
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BspayTradeListQueryService {

	private static final int PAGE_SIZE_EXPORT = 500;

	private static final long TIME_RANGE_SECONDS = 3L * 30 * 24 * 3600;

	private final BspayTradeExportQueryMapper bspayTradeExportQueryMapper;
	private final BspayTradeDivFeeBatchService bspayTradeDivFeeBatchService;
	private final DistributorListQueryService distributorListQueryService;
	private final BsPayOperatorResolveService bsPayOperatorResolveService;

	public BspayTradeListQueryService(
			BspayTradeExportQueryMapper bspayTradeExportQueryMapper,
			BspayTradeDivFeeBatchService bspayTradeDivFeeBatchService,
			DistributorListQueryService distributorListQueryService,
			BsPayOperatorResolveService bsPayOperatorResolveService) {
		this.bspayTradeExportQueryMapper = bspayTradeExportQueryMapper;
		this.bspayTradeDivFeeBatchService = bspayTradeDivFeeBatchService;
		this.distributorListQueryService = distributorListQueryService;
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
	}

	public Map<String, Object> getTradelist(Map<String, Object> jwtMap, HttpServletRequest request) {
		long companyId = parseLongSafe(jwtMap.get("company_id"));
		if (companyId <= 0) {
			return emptyTradeListResult();
		}
		String rawOperatorType = str(jwtMap.get("operator_type")).trim();
		String outputOperatorType = "staff".equalsIgnoreCase(rawOperatorType) ? "admin" : rawOperatorType;
		long jwtOperatorId = parseLongSafe(jwtMap.get("operator_id"));

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		if (StringUtils.hasText(request.getParameter("status"))) {
			filter.put("status", request.getParameter("status").trim().toUpperCase(Locale.ROOT));
		}
		if (request.getParameterMap().containsKey("can_div")) {
			String raw = request.getParameter("can_div");
			filter.put("can_div", raw != null && "true".equals(raw.trim()));
		}
		if (StringUtils.hasText(request.getParameter("bspay_fee_mode"))) {
			filter.put(
					"bspay_fee_mode",
					request.getParameter("bspay_fee_mode").trim().toUpperCase(Locale.ROOT));
		} else if (StringUtils.hasText(request.getParameter("adapay_fee_mode"))) {
			filter.put(
					"bspay_fee_mode",
					request.getParameter("adapay_fee_mode").trim().toUpperCase(Locale.ROOT));
		}
		if (StringUtils.hasText(request.getParameter("bspay_div_status"))) {
			filter.put(
					"bspay_div_status",
					request.getParameter("bspay_div_status").trim().toUpperCase(Locale.ROOT));
		} else if (StringUtils.hasText(request.getParameter("adapay_div_status"))) {
			filter.put(
					"bspay_div_status",
					request.getParameter("adapay_div_status").trim().toUpperCase(Locale.ROOT));
		}
		for (String p : List.of("pay_channel", "order_id", "trade_id")) {
			String v = request.getParameter(p);
			if (v == null) {
				continue;
			}
			String t = v.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			if ("0".equals(t)) {
				continue;
			}
			if ("false".equalsIgnoreCase(t)) {
				continue;
			}
			filter.put(p, t);
		}

		String opLower = rawOperatorType.toLowerCase(Locale.ROOT);
		if ("distributor".equals(opLower)) {
			long did = parseLongSafe(jwtMap.get("distributor_id"));
			if (did <= 0) {
				return emptyTradeListResult();
			}
			filter.put("distributor_id", did);
		} else if ("merchant".equals(opLower)) {
			Optional<Long> midOpt =
					bsPayOperatorResolveService.resolveMerchantFilterIdForTradeListOrThrow(jwtOperatorId);
			if (midOpt.isEmpty()) {
				return emptyTradeListResult();
			}
			filter.put("merchant_id", midOpt.get());
		} else if ("supplier".equals(opLower)) {
			long sid = parseLongSafe(jwtMap.get("operator_id"));
			if (sid <= 0) {
				return emptyTradeListResult();
			}
			filter.put("supplier_id", sid);
		}

		String dn = request.getParameter("distributor_name");
		if (dn != null && StringUtils.hasText(dn.trim()) && !"0".equals(dn.trim())) {
			List<Long> ids =
					distributorListQueryService.listDistributorIdsByCompanyAndNameContains(companyId, dn.trim());
			if (ids.isEmpty()) {
				return emptyTradeListResult();
			}
			filter.put("distributor_id", ids);
		}

		applyTimeStartRangeFromRequest(filter, request);

		int page = parsePageInt(request, "page", 1);
		int pageSize = parsePageInt(request, "pageSize", 20);

		filter.put("operator_type", rawOperatorType);
		Map<String, Object> prepared = new LinkedHashMap<>(filter);
		prepareFilterForTradeExport(prepared);

		long now = Instant.now().getEpochSecond();
		List<Map<String, Object>> allRows = bspayTradeExportQueryMapper.selectGroupedAll(prepared, now);
		int totalCount = allRows.size();

		long sumTotalFee = 0L;
		long sumPayFeeNet = 0L;
		long sumBspayFee = 0L;
		for (Map<String, Object> row : allRows) {
			long payFee = toMoneyLong(row.get("payFee"));
			long refunded = toMoneyLong(row.get("refundedFee"));
			long bspayFee = toMoneyLong(row.get("bspayFee"));
			sumTotalFee += payFee;
			sumPayFeeNet += (payFee - refunded);
			sumBspayFee += bspayFee;
		}
		List<String> allTradeIds =
				allRows.stream().map(r -> str(r.get("tradeId"))).filter(StringUtils::hasText).toList();
		Map<String, Long> divMap =
				bspayTradeDivFeeBatchService.sumDivFeeByTradeIds(companyId, allTradeIds, outputOperatorType);
		long divFeeTotal = divMap.values().stream().mapToLong(Long::longValue).sum();
		Map<String, Object> total = new LinkedHashMap<>();
		total.put("totalFee", sumTotalFee);
		total.put("payFee", sumPayFeeNet);
		total.put("bspayFee", sumBspayFee);
		total.put("divFee", divFeeTotal);

		List<Map<String, Object>> pageRaw;
		if (pageSize <= 0) {
			pageRaw = allRows;
		} else {
			int p = Math.max(page, 1);
			long offset = (long) (p - 1) * pageSize;
			pageRaw = bspayTradeExportQueryMapper.selectGroupedPage(prepared, now, offset, pageSize);
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
		total0.put("totalFee", 0L);
		total0.put("payFee", 0L);
		total0.put("bspayFee", 0L);
		total0.put("divFee", 0L);
		empty.put("total", total0);
		empty.put("total_count", 0);
		return empty;
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

	public void applyTimeStartRangeFromRequest(Map<String, Object> filter, HttpServletRequest request) {
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

	public long countGroupedTrades(Map<String, Object> preparedFilter) {
		long now = Instant.now().getEpochSecond();
		return bspayTradeExportQueryMapper.countGrouped(preparedFilter, now);
	}

	public List<Map<String, Object>> loadTradePage(
			Map<String, Object> preparedFilter, String outputOperatorType, int pageOneBased, int pageSize) {
		long now = Instant.now().getEpochSecond();
		int p = Math.max(pageOneBased, 1);
		int ps = pageSize > 0 ? pageSize : PAGE_SIZE_EXPORT;
		long offset = (long) (p - 1) * ps;
		List<Map<String, Object>> raw =
				bspayTradeExportQueryMapper.selectGroupedPage(preparedFilter, now, offset, ps);
		long companyId = parseLongSafe(preparedFilter.get("company_id"));
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
				bspayTradeDivFeeBatchService.sumDivFeeByTradeIds(companyId, tradeIds, outputOperatorType);
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
		Object divStatus = m.get("bspayDivStatus");
		if (divStatus == null || !StringUtils.hasText(String.valueOf(divStatus))) {
			m.put("bspayDivStatus", "NOTDIV");
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

	private static long parseLongSafe(Object o) {
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
