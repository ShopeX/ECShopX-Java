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

package cn.shopex.ecshopx.aftersales.service.export;

import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AftersalesFinancialCsvExportService {

	private static final int FINANCIAL_EXPORT_PAGE_SIZE = 500;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final Pattern NUMERIC_ONLY = Pattern.compile("^[0-9]+$");

	private final AftersalesMapper aftersalesMapper;
	private final ExportCsvFileService exportCsvFileService;

	public AftersalesFinancialCsvExportService(AftersalesMapper aftersalesMapper, ExportCsvFileService exportCsvFileService) {
		this.aftersalesMapper = aftersalesMapper;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> runExport(LinkedHashMap<String, Object> filter, long operatorId) {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("refund_bn", "退款单号");
		title.put("aftersales_bn", "售后单号");
		title.put("order_id", "订单号");
		title.put("refund_status", "退款状态");
		title.put("refund_fee", "退款金额");
		title.put("refund_point", "退款积分");
		title.put("create_time", "创建时间");
		title.put("refund_success_time", "退款成功时间");

		List<Map<String, String>> rows = new ArrayList<>();
		int pageNo = 1;
		while (true) {
			long offset = (pageNo - 1L) * FINANCIAL_EXPORT_PAGE_SIZE;
			List<Map<String, Object>> page =
					aftersalesMapper.selectFinancialExportPage(filter, offset, FINANCIAL_EXPORT_PAGE_SIZE);
			if (page == null || page.isEmpty()) {
				break;
			}
			for (Map<String, Object> raw : page) {
				rows.add(buildCsvRow(raw));
			}
			if (page.size() < FINANCIAL_EXPORT_PAGE_SIZE) {
				break;
			}
			pageNo++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_退款单";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private static LinkedHashMap<String, String> buildCsvRow(Map<String, Object> raw) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("refund_bn", "");
		row.put("aftersales_bn", textFieldCell(stringOf(raw.get("aftersales_bn"))));
		row.put("order_id", textFieldCell(stringOf(raw.get("order_id"))));
		row.put("refund_status", "");
		row.put("refund_fee", refundFeeYuan(raw.get("refund_fee")));
		row.put("refund_point", "");
		row.put("create_time", formatEpochSeconds(raw.get("create_time")));
		row.put("refund_success_time", formatEpochSeconds(raw.get("refund_success_time")));
		return row;
	}

	private static String stringOf(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static String textFieldCell(String value) {
		if (value.isEmpty()) {
			return "";
		}
		if (NUMERIC_ONLY.matcher(value).matches()) {
			return "\t" + value;
		}
		return value;
	}

	private static String refundFeeYuan(Object v) {
		if (v == null) {
			return "";
		}
		BigDecimal cents;
		if (v instanceof BigDecimal bd) {
			cents = bd;
		} else if (v instanceof Number n) {
			cents = BigDecimal.valueOf(n.longValue());
		} else {
			try {
				cents = new BigDecimal(String.valueOf(v).trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		if (cents.compareTo(BigDecimal.ZERO) == 0) {
			return "0";
		}
		return cents
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String formatEpochSeconds(Object v) {
		if (v == null) {
			return "";
		}
		long sec;
		if (v instanceof Number n) {
			sec = n.longValue();
		} else {
			try {
				sec = Long.parseLong(String.valueOf(v).trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		if (sec == 0L) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(sec), SHANGHAI).format(CSV_TIME);
	}
}
