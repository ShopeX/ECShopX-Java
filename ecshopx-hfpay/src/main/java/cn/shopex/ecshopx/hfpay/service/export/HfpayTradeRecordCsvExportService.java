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

package cn.shopex.ecshopx.hfpay.service.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayDistributorTransactionListService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayTradeRecordCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(HfpayTradeRecordCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final int PAGE_SIZE = 500;

	private final HfpayEnterapplyMapper enterapplyMapper;
	private final HfpayDistributorTransactionListService hfpayDistributorTransactionListService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public HfpayTradeRecordCsvExportService(
			HfpayEnterapplyMapper enterapplyMapper,
			HfpayDistributorTransactionListService hfpayDistributorTransactionListService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.enterapplyMapper = enterapplyMapper;
		this.hfpayDistributorTransactionListService = hfpayDistributorTransactionListService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(HfpayTradeRecordExportContext ctx) {
		long companyId = ctx.getCompanyId();
		long operatorId = ctx.getOperatorId();
		long supplierId = ctx.getSupplierId();
		String startDateTime = ctx.getStartDateTime();
		String endDateTime = ctx.getEndDateTime();
		Integer distributorId = ctx.getDistributorId();

		Long distFilter =
				(distributorId != null && distributorId != 0) ? distributorId.longValue() : null;
		long total = enterapplyMapper.countStatisticsEnterapplyList(companyId, distFilter);

		int pageSize = PAGE_SIZE;
		int pages = total == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

		LinkedHashMap<String, String> titles = buildTitles();
		List<Map<String, String>> allRows = new ArrayList<>();

		for (int page = 1; page <= pages; page++) {
			Map<String, Object> pageData =
					hfpayDistributorTransactionListService.transactionList(
							companyId, startDateTime, endDateTime, distributorId, page, pageSize);
			List<Map<String, Object>> list = extractList(pageData);
			for (Map<String, Object> row : list) {
				allRows.add(toCsvRow(row));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_汇付分账交易";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}

		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				companyId,
				operatorId,
				0L,
				supplierId,
				HfpayTradeRecordExportFileJobTypes.TYPE_HFPAY_TRADE_RECORD,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				finishSec);
	}

	private static LinkedHashMap<String, String> buildTitles() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("distributor_name", "店铺名称");
		m.put("withdrawal_balance", "可提现金额");
		m.put("order_count", "交易总笔数");
		m.put("order_total_fee", "总交易金额");
		m.put("order_refund_count", "已退款总笔数");
		m.put("order_refund_total_fee", "退款总金额");
		m.put("order_refunding_count", "在退总笔数");
		m.put("order_refunding_total_fee", "在退总金额");
		m.put("order_profit_sharing_charge", "已结算手续费总额");
		m.put("order_un_profit_sharing_charge", "未结算手续费总额");
		return m;
	}

	private static List<Map<String, Object>> extractList(Map<String, Object> pageData) {
		if (pageData == null) {
			return List.of();
		}
		Object listObj = pageData.get("list");
		if (!(listObj instanceof List<?> rawList)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : rawList) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(row);
			}
		}
		return out;
	}

	private static Map<String, String> toCsvRow(Map<String, Object> row) {
		Map<String, String> line = new LinkedHashMap<>();
		line.put("distributor_name", distributorNameCell(row.get("distributor_name")));
		line.put("withdrawal_balance", fenToYuanPlain(row.get("withdrawal_balance")));
		line.put("order_count", intCell(row.get("order_count")));
		line.put("order_total_fee", fenToYuanPlain(row.get("order_total_fee")));
		line.put("order_refund_count", intCell(row.get("order_refund_count")));
		line.put("order_refund_total_fee", fenToYuanPlain(row.get("order_refund_total_fee")));
		line.put("order_refunding_count", intCell(row.get("order_refunding_count")));
		line.put("order_refunding_total_fee", fenToYuanPlain(row.get("order_refunding_total_fee")));
		line.put("order_profit_sharing_charge", fenToYuanPlain(row.get("order_profit_sharing_charge")));
		line.put(
				"order_un_profit_sharing_charge",
				fenToYuanPlain(row.get("order_un_profit_sharing_charge")));
		return line;
	}

	private static String distributorNameCell(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : "";
	}

	private static String intCell(Object o) {
		if (o instanceof Number n) {
			return String.valueOf(n.intValue());
		}
		if (o == null) {
			return "0";
		}
		try {
			return String.valueOf(new BigDecimal(String.valueOf(o).trim()).intValue());
		} catch (Exception e) {
			return "0";
		}
	}

	private static String fenToYuanPlain(Object fenObj) {
		if (fenObj == null) {
			return "0";
		}
		BigDecimal fen;
		if (fenObj instanceof Number n) {
			fen = BigDecimal.valueOf(n.longValue());
		} else {
			try {
				fen = new BigDecimal(String.valueOf(fenObj).trim());
			} catch (Exception e) {
				return "0";
			}
		}
		return fen.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}
}
