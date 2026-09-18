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

package cn.shopex.ecshopx.salesperson.service.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.salesperson.domain.ProfitStatistics;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfitExportSalespersonCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(ProfitExportSalespersonCsvExportService.class);

	private static final int PAGE_SIZE = 500;

	private final ProfitStatisticsExportQueryService profitStatisticsExportQueryService;
	private final ExportCsvFileService exportCsvFileService;
	private final ObjectMapper objectMapper;

	public ProfitExportSalespersonCsvExportService(ProfitStatisticsExportQueryService profitStatisticsExportQueryService,
			ExportCsvFileService exportCsvFileService, ObjectMapper objectMapper) {
		this.profitStatisticsExportQueryService = profitStatisticsExportQueryService;
		this.exportCsvFileService = exportCsvFileService;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, String>> runExport(Map<String, Object> filter, long supplierId, long distributorId) {
		long companyId = longFromFilter(filter, "company_id");
		String dateYm = stringFromFilter(filter, "date");
		String profitUserTypeRaw = profitUserTypeRawFromFilter(filter);

		long count = profitStatisticsExportQueryService.countForFilter(companyId, dateYm, profitUserTypeRaw);
		if (count == 0) {
			return Optional.empty();
		}

		DateTimeFormatter ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String fileBaseName = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(ts) + companyId + "salesperson_profit";

		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("salesperson_id", "导购ID");
		titles.put("name", "导购名称");
		titles.put("distributor_name", "门店名称");
		titles.put("popularize_commissions_num", "导购推广笔数");
		titles.put("popularize_commissions", "导购推广金额");
		titles.put("total", "总计金额");

		int totalPages = (int) Math.ceil(count / (double) PAGE_SIZE);
		List<Map<String, String>> rows = new ArrayList<>();
		for (int p = 1; p <= totalPages; p++) {
			List<ProfitStatistics> page = profitStatisticsExportQueryService.pageForFilter(companyId, dateYm,
					profitUserTypeRaw, p, PAGE_SIZE);
			for (ProfitStatistics row : page) {
				rows.add(buildRow(titles, row));
			}
		}

		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, rows);
		if (uploaded == null || uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return Optional.empty();
		}
		return Optional.of(uploaded);
	}

	private Map<String, String> buildRow(LinkedHashMap<String, String> titleKeys, ProfitStatistics entity) {
		Map<String, Object> params = parseParamsJson(entity.getParams());
		Map<String, String> out = new LinkedHashMap<>();
		for (String k : titleKeys.keySet()) {
			out.put(k, cellSalesperson(k, entity, params));
		}
		return out;
	}

	private String cellSalesperson(String k, ProfitStatistics value, Map<String, Object> params) {
		if ("salesperson_id".equals(k) && value.getProfitUserId() != null) {
			return String.valueOf(value.getProfitUserId());
		}
		if ("name".equals(k)) {
			return value.getName() != null ? value.getName() : "--";
		}
		if ("distributor_name".equals(k)) {
			Object dn = params.get("distributor_name");
			return dn == null ? "0" : dn.toString();
		}
		if ("popularize_commissions_num".equals(k)) {
			return String.valueOf(longFromParam(params, "popularize_commissions_num"));
		}
		if ("popularize_commissions".equals(k)) {
			return fenToYuanPlain(params.get("popularize_commissions"));
		}
		if ("total".equals(k)) {
			Long wf = value.getWithdrawalsFee();
			if (wf == null) {
				return "0.00";
			}
			return BigDecimal.valueOf(wf).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		}
		return "--";
	}

	private Map<String, Object> parseParamsJson(String paramsJson) {
		if (!StringUtils.hasText(paramsJson)) {
			return Map.of();
		}
		try {
			Map<String, Object> m = objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {
			});
			return m != null ? m : Map.of();
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static long longFromParam(Map<String, Object> params, String key) {
		Object o = params.get(key);
		if (o instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static String fenToYuanPlain(Object o) {
		if (!(o instanceof Number n)) {
			return "0";
		}
		return BigDecimal.valueOf(n.longValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long longFromFilter(Map<String, Object> filter, String key) {
		Object o = filter.get(key);
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			throw new IllegalStateException(key);
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String stringFromFilter(Map<String, Object> filter, String key) {
		Object o = filter.get(key);
		if (o == null) {
			throw new IllegalStateException(key);
		}
		return o.toString();
	}

	private static String profitUserTypeRawFromFilter(Map<String, Object> filter) {
		if (!filter.containsKey("profit_user_type")) {
			return null;
		}
		Object o = filter.get("profit_user_type");
		return o == null ? null : o.toString();
	}
}
