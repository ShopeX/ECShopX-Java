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

package cn.shopex.ecshopx.adapay.service.export;

import cn.shopex.ecshopx.adapay.service.AdapayTradeListQueryService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
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
public class AdapayTradeDataCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(AdapayTradeDataCsvExportService.class);
	private static final String EXPORT_TYPE = "adapay_tradedata";
	private static final int BATCH = 500;

	private static final LinkedHashMap<String, String> TITLE = new LinkedHashMap<>();

	static {
		TITLE.put("timeStart", "创建时间");
		TITLE.put("orderId", "订单号");
		TITLE.put("tradeId", "交易单号");
		TITLE.put("tradeState", "交易状态");
		TITLE.put("payFee", "订单金额");
		TITLE.put("divType", "分账类型");
		TITLE.put("canDiv", "分账状态");
		TITLE.put("adapayDivStatus", "是否分账");
		TITLE.put("adapayFeeMode", "手续费扣费方式");
		TITLE.put("adapayFee", "手续费");
		TITLE.put("divFee", "分账金额");
		TITLE.put("distributor_name", "店铺名称");
		TITLE.put("refundedFee", "退款金额");
	}

	private final AdapayTradeListQueryService adapayTradeListQueryService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public AdapayTradeDataCsvExportService(
			AdapayTradeListQueryService adapayTradeListQueryService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.adapayTradeListQueryService = adapayTradeListQueryService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(AdapayTradeDataExportContext ctx) {
		Map<String, Object> f = ctx.preparedFilter();
		long count = adapayTradeListQueryService.countGroupedTrades(f);
		if (count <= 0) {
			log.debug("adapay trade export: zero rows, skip file");
			return;
		}
		String fileBase =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(Instant.now())
						+ ctx.companyId()
						+ "adapay分账列表";
		List<Map<String, String>> allRows = new ArrayList<>();
		int pages = (int) Math.ceil(count / (double) BATCH);
		for (int p = 1; p <= pages; p++) {
			List<Map<String, Object>> page =
					adapayTradeListQueryService.loadTradePage(f, ctx.outputOperatorType(), p, BATCH);
			for (Map<String, Object> row : page) {
				allRows.add(formatCsvRow(row));
			}
		}
		LinkedHashMap<String, String> titleCopy = new LinkedHashMap<>(TITLE);
		Map<String, String> fileMeta = exportCsvFileService.exportCsv(fileBase, titleCopy, allRows);
		if (fileMeta == null || fileMeta.isEmpty()) {
			log.debug("adapay trade export: csv service returned empty");
			return;
		}
		String url = fileMeta.getOrDefault("url", "");
		String filename = fileMeta.getOrDefault("filename", fileBase + ".csv");
		long finish = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				ctx.companyId(), ctx.jwtOperatorId(), EXPORT_TYPE, filename, url, finish);
	}

	private static Map<String, String> formatCsvRow(Map<String, Object> value) {
		Map<String, String> row = new LinkedHashMap<>();
		Map<String, String> tradeState = Map.of(
				"PARTIAL_REFUND", "部分退款",
				"FULL_REFUND", "全额退款",
				"SUCCESS", "支付完成");
		Map<String, String> payChannel = Map.of("wx_lite", "微信小程序(线上)");
		Map<String, String> adapayDivStatus = Map.of("DIVED", "已分账", "NOTDIV", "未分账");
		Map<String, String> adapayFeeMode = Map.of("I", "内扣", "O", "外扣");

		for (String k : TITLE.keySet()) {
			row.put(k, formatCell(k, value, tradeState, payChannel, adapayDivStatus, adapayFeeMode));
		}
		return row;
	}

	private static String formatCell(
			String k,
			Map<String, Object> value,
			Map<String, String> tradeState,
			Map<String, String> payChannel,
			Map<String, String> adapayDivStatus,
			Map<String, String> adapayFeeMode) {
		if (("orderId".equals(k) || "tradeId".equals(k)) && value.get(k) != null) {
			return "'" + value.get(k) + "'";
		}
		if ("payFee".equals(k) || "refundedFee".equals(k) || "divFee".equals(k) || "adapayFee".equals(k)) {
			long cents = toLong(value.get(k));
			return formatMoneyYuan(cents);
		}
		if ("timeStart".equals(k)) {
			long sec = parseEpochSeconds(value.get("timeStart"));
			if (sec <= 0) {
				return "--";
			}
			return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
					.withZone(ZoneId.systemDefault())
					.format(Instant.ofEpochSecond(sec));
		}
		if ("tradeState".equals(k)) {
			Object ts = value.get("tradeState");
			return ts != null ? tradeState.getOrDefault(String.valueOf(ts), "--") : "--";
		}
		if ("payChannel".equals(k)) {
			Object pc = value.get("payChannel");
			return pc != null ? payChannel.getOrDefault(String.valueOf(pc), "--") : "--";
		}
		if ("adapayDivStatus".equals(k)) {
			Object s = value.get("adapayDivStatus");
			return s != null ? adapayDivStatus.getOrDefault(String.valueOf(s), "--") : "--";
		}
		if ("adapayFeeMode".equals(k)) {
			Object s = value.get("adapayFeeMode");
			return s != null ? adapayFeeMode.getOrDefault(String.valueOf(s), "--") : "--";
		}
		if ("divType".equals(k)) {
			Object pt = value.get("payType");
			return pt != null && "adapay".equalsIgnoreCase(String.valueOf(pt)) ? "线上" : "线下";
		}
		if ("canDiv".equals(k)) {
			Object c = value.get("canDiv");
			boolean b = c instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(c));
			return b ? "可分账" : "不可分账";
		}
		if ("distributor_name".equals(k)) {
			Object n = value.get("distributor_name");
			return n != null && StringUtils.hasText(n.toString()) ? n.toString() : "--";
		}
		Object raw = value.get(k);
		return raw != null && StringUtils.hasText(raw.toString()) ? raw.toString() : "--";
	}

	private static String formatMoneyYuan(long cents) {
		java.math.BigDecimal v = java.math.BigDecimal.valueOf(cents).divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
		return v.toPlainString();
	}

	private static long parseEpochSeconds(Object timeStart) {
		if (timeStart == null) {
			return 0L;
		}
		if (timeStart instanceof Number n) {
			return n.longValue();
		}
		String s = timeStart.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
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
}
