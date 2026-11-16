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
import cn.shopex.ecshopx.popularize.service.PopularizeBrokerageListReadService;
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

	private final PopularizeBrokerageListReadService popularizeBrokerageListReadService;
	private final ExportCsvFileService exportCsvFileService;

	public PopularizeStaticCsvExportService(
			PopularizeBrokerageListReadService popularizeBrokerageListReadService,
			ExportCsvFileService exportCsvFileService) {
		this.popularizeBrokerageListReadService = popularizeBrokerageListReadService;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> runExport(
			Map<String, Object> filter, @SuppressWarnings("unused") boolean datapassBlock) {
		long companyId = longFrom(filter.get("company_id"));
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filter);
		work.remove("datapass_block");

		String userIdRaw = work.get("user_id") != null ? String.valueOf(work.get("user_id")) : null;
		String isCloseRaw = work.get("is_close") != null ? String.valueOf(work.get("is_close")) : null;

		Map<String, Object> probe =
				popularizeBrokerageListReadService.getBrokerageList(companyId, "1", "1", userIdRaw, isCloseRaw, null);
		long total = longFrom(probe.get("total_count"));
		if (total <= 0L) {
			return Optional.empty();
		}

		int limit = 500;
		int totalPage = (int) Math.ceil(total / (double) limit);
		List<Map<String, String>> csvRows = new ArrayList<>();

		for (int page = 1; page <= totalPage; page++) {
			Map<String, Object> pageData =
					popularizeBrokerageListReadService.getBrokerageList(
							companyId,
							String.valueOf(page),
							String.valueOf(limit),
							userIdRaw,
							isCloseRaw,
							null);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rows = (List<Map<String, Object>>) pageData.get("list");
			if (rows == null || rows.isEmpty()) {
				continue;
			}
			for (Map<String, Object> row : rows) {
				csvRows.add(mapRowToCsv(row));
			}
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
		title.put("user_id", "推广员会员ID");
		title.put("order_id", "订单编号");
		title.put("price", "佣金(元)");
		title.put("rebate", "返利(元)");
		title.put("brokerage_type", "佣金类型");
		title.put("is_close", "是否结算");
		title.put("created", "创建时间");
		return title;
	}

	private static Map<String, String> mapRowToCsv(Map<String, Object> row) {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("user_id", row.get("user_id") == null ? "" : String.valueOf(row.get("user_id")));
		m.put("order_id", row.get("order_id") == null ? "" : String.valueOf(row.get("order_id")));
		m.put("price", centToYuanPlain(row.get("price")));
		m.put("rebate", centToYuanPlain(row.get("rebate")));
		m.put("brokerage_type", row.get("brokerage_type") == null ? "" : String.valueOf(row.get("brokerage_type")));
		m.put("is_close", Boolean.TRUE.equals(row.get("is_close")) ? "是" : "否");
		m.put("created", row.get("created") == null ? "" : String.valueOf(row.get("created")));
		return m;
	}

	private static String centToYuanPlain(Object v) {
		long cents = longFrom(v);
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}