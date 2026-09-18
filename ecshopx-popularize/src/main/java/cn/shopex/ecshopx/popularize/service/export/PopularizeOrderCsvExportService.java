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

package cn.shopex.ecshopx.popularize.service.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.popularize.service.SalesmanBrokerageLogsQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeOrderCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CREATED_CELL =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);
	private static final DateTimeFormatter CREATED_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService;
	private final ExportCsvFileService exportCsvFileService;

	public PopularizeOrderCsvExportService(
			SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService,
			ExportCsvFileService exportCsvFileService) {
		this.salesmanBrokerageLogsQueryService = salesmanBrokerageLogsQueryService;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> runExport(
			Map<String, Object> filter, @SuppressWarnings("unused") boolean datapassBlock) {
		long companyId = longFrom(filter.get("company_id"));
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		work.remove("datapass_block");

		long total = salesmanBrokerageLogsQueryService.countByFilter(work);
		if (total <= 0L) {
			return Optional.empty();
		}

		int limit = 500;
		int totalPage = (int) Math.ceil(total / (double) limit);
		List<Map<String, String>> allRows = new ArrayList<>();
		LinkedHashMap<String, String> title = buildOrderExportTitle();

		for (int page = 1; page <= totalPage; page++) {
			List<Map<String, Object>> rows =
					salesmanBrokerageLogsQueryService.selectPageByFilter(work, limit, page);
			if (rows == null || rows.isEmpty()) {
				continue;
			}
			salesmanBrokerageLogsQueryService.enrichListWithSalesperson(companyId, rows);
			for (Map<String, Object> row : rows) {
				allRows.add(mapRowToCsv(row));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "popularizeorder";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, title, allRows);
		if (uploaded == null || uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return Optional.empty();
		}
		return Optional.of(uploaded);
	}

	private static LinkedHashMap<String, String> buildOrderExportTitle() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单编号");
		title.put("title", "商品名称");
		title.put("store_name", "店铺名称");
		title.put("distributor_id", "店铺ID");
		title.put("username", "业务员");
		title.put("mobile", "业务员手机号");
		title.put("price", "佣金(元)");
		title.put("total_fee", "订单金额(元)");
		title.put("brokerage_type", "佣金类型");
		title.put("commission_type", "返佣类型");
		title.put("is_close", "是否结算");
		title.put("created", "佣金创建时间");
		return title;
	}

	private static Map<String, String> mapRowToCsv(Map<String, Object> row) {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("order_id", row.get("order_id") == null ? "" : String.valueOf(row.get("order_id")));
		m.put("title", emptyIfNull(row.get("title")));
		m.put("store_name", emptyIfNull(row.get("store_name")));
		m.put("distributor_id", formatLongCell(row.get("distributor_id")));
		m.put("username", emptyIfNull(row.get("username")));
		m.put("mobile", emptyIfNull(row.get("mobile")));
		m.put("price", centToYuanPlain(row.get("price")));
		m.put("total_fee", centToYuanPlain(row.get("total_fee")));
		m.put("brokerage_type", row.get("brokerage_type") == null ? "" : String.valueOf(row.get("brokerage_type")));
		m.put("commission_type", emptyIfNull(row.get("commission_type")));
		m.put("is_close", formatIsClose(row.get("is_close")));
		m.put("created", formatCreatedCell(row.get("created")));
		return m;
	}

	private static String emptyIfNull(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String formatLongCell(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		String t = String.valueOf(v).trim();
		return t;
	}

	private static String centToYuanPlain(Object v) {
		long cents = longFrom(v);
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String formatIsClose(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return "是";
		}
		if (v instanceof Number n && n.intValue() == 1) {
			return "是";
		}
		if (v instanceof String s && "1".equals(s.trim())) {
			return "是";
		}
		return "否";
	}

	private static String formatCreatedCell(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Number n) {
			long sec = n.longValue();
			return CREATED_CELL.format(Instant.ofEpochSecond(sec));
		}
		if (v instanceof java.sql.Timestamp ts) {
			return CREATED_CELL.format(ts.toInstant());
		}
		if (v instanceof java.util.Date d) {
			return CREATED_CELL.format(d.toInstant());
		}
		if (v instanceof LocalDateTime ldt) {
			return ldt.atZone(ZoneId.systemDefault()).withZoneSameInstant(SHANGHAI).format(CREATED_LOCAL);
		}
		if (v instanceof java.time.Instant ins) {
			return CREATED_CELL.format(ins);
		}
		return String.valueOf(v);
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
