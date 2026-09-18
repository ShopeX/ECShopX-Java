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
import cn.shopex.ecshopx.popularize.service.SalesmanBrokerageCountListQueryService;
import cn.shopex.ecshopx.popularize.service.SalesmanBrokerageLogsQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
public class PopularizeStaticCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);

	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;
	private final SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService;
	private final ExportCsvFileService exportCsvFileService;

	public PopularizeStaticCsvExportService(
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService,
			SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService,
			ExportCsvFileService exportCsvFileService) {
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
		this.salesmanBrokerageLogsQueryService = salesmanBrokerageLogsQueryService;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> runExport(
			Map<String, Object> filter, @SuppressWarnings("unused") boolean datapassBlock) {
		long companyId = longFrom(filter.get("company_id"));
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		work.remove("datapass_block");

		List<Map<String, Object>> allRows = new ArrayList<>();

		long total = this.salesmanBrokerageCountListQueryService.countGroupedSalesmanBrokerageRows(work);
		if (total <= 0) {
			return Optional.empty();
		}

		int totalPage = (int) Math.ceil(total / 500.0);

		for (int page = 1; page <= totalPage; page++) {
			List<Map<String, Object>> rows =
					salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountList(work, 500, page);
			if (rows == null || rows.isEmpty()) {
				continue;
			}
			salesmanBrokerageLogsQueryService.enrichListWithSalesperson(companyId, rows);
			for (Map<String, Object> row : rows) {
				allRows.add(row);
			}
		}

		List<Map<String, String>> csvRows = new ArrayList<>(allRows.size());
		for (Map<String, Object> row : allRows) {
			csvRows.add(mapRowToCsv(row));
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "popularize";
		Map<String, String> uploaded =
				exportCsvFileService.exportCsv(fileBaseName, buildStaticExportTitle(), csvRows);
		if (uploaded == null || uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return Optional.empty();
		}
		return Optional.of(uploaded);
	}

	private static LinkedHashMap<String, String> buildStaticExportTitle() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("salesperson_id", "导购员ID");
		title.put("user_id", "推广员会员ID");
		title.put("order_id", "订单编号");
		title.put("order_num", "有效佣金单数");
		title.put("order_num_refund", "退款佣金单数");
		title.put("rebate_sum_noclose", "未结算佣金(元)");
		title.put("rebate_sum", "佣金返利合计(元)");
		title.put("price_sum", "商品行金额合计(元)");
		title.put("total_fee", "订单金额(元)");
		title.put("username", "业务员");
		title.put("mobile", "业务员手机号");
		title.put("store_name", "店铺名称");
		title.put("distributor_id", "店铺ID");
		return title;
	}

	private static Map<String, String> mapRowToCsv(Map<String, Object> row) {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("salesperson_id", formatLongCell(row.get("salesperson_id")));

		long uid = longOrZero(row.get("user_id"));
		m.put("user_id", uid > 0L ? "\t" + uid + "\t" : "");

		Object orderIdRaw = row.get("order_id");
		String orderIdTrim = orderIdRaw == null ? "" : String.valueOf(orderIdRaw).trim();
		m.put("order_id", StringUtils.hasText(orderIdTrim) ? "\t" + orderIdTrim + "\t" : "");

		m.put("order_num", Long.toString(longOrZero(row.get("order_num"))));
		m.put("order_num_refund", Long.toString(longOrZero(row.get("order_num_refund"))));
		m.put("rebate_sum_noclose", centToYuanPlain(longOrZero(row.get("rebate_sum_noclose"))));
		m.put("rebate_sum", centToYuanPlain(longOrZero(row.get("rebate_sum"))));
		m.put("price_sum", centToYuanPlain(longOrZero(row.get("price_sum"))));
		m.put("total_fee", centToYuanPlain(longOrZero(row.get("total_fee"))));
		m.put("username", emptyIfNull(row.get("username")));
		m.put("mobile", emptyIfNull(row.get("mobile")));
		m.put("store_name", emptyIfNull(row.get("store_name")));
		m.put("distributor_id", formatLongCell(row.get("distributor_id")));
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
		return String.valueOf(v).trim();
	}

	private static String centToYuanPlain(long cents) {
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long longOrZero(Object v) {
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

	private static long longFrom(Object v) {
		return longOrZero(v);
	}
}
