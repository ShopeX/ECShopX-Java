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
import cn.shopex.ecshopx.hfpay.config.HfpayOrderStatusProperties;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderListService;
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
public class HfpayOrderRecordCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(HfpayOrderRecordCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final int PAGE_SIZE = 500;

	private final HfpayStatisticsOrderListService orderListService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final HfpayOrderStatusProperties orderStatusProperties;

	public HfpayOrderRecordCsvExportService(
			HfpayStatisticsOrderListService orderListService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService,
			HfpayOrderStatusProperties orderStatusProperties) {
		this.orderListService = orderListService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.orderStatusProperties = orderStatusProperties;
	}

	public void runExport(HfpayOrderRecordExportContext ctx) {
		long companyId = ctx.getCompanyId();
		long operatorId = ctx.getOperatorId();
		long supplierId = ctx.getSupplierId();

		long total = orderListService.countOrders(ctx);
		int pageSize = PAGE_SIZE;
		int pages = total == 0 ? 0 : (int) Math.ceil(total / (double) pageSize);

		LinkedHashMap<String, String> titles = buildTitles();
		List<Map<String, String>> allRows = new ArrayList<>();

		for (int page = 1; page <= pages; page++) {
			List<Map<String, Object>> batch = orderListService.pageOrders(ctx, page, pageSize);
			for (Map<String, Object> raw : batch) {
				Map<String, Object> row = new LinkedHashMap<>(raw);
				orderListService.enrichListRow(companyId, row);
				allRows.add(toCsvRow(row));
			}
		}

		String fileBaseName = FILE_TS.format(Instant.now()) + "_汇付订单交易";
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
				HfpayOrderRecordExportFileJobTypes.TYPE_HFPAY_ORDER_RECORD,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				finishSec);
	}

	private static LinkedHashMap<String, String> buildTitles() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("create_time", "时间");
		m.put("order_id", "订单号");
		m.put("profitsharing_status", "结算状态");
		m.put("total_fee", "交易金额");
		m.put("charge", "平台手续费");
		m.put("distributor_name", "店铺名称");
		m.put("refund_fee", "退款金额");
		m.put("order_status", "订单状态");
		return m;
	}

	private Map<String, String> toCsvRow(Map<String, Object> value) {
		Map<String, String> line = new LinkedHashMap<>();
		line.put("create_time", strCell(value.get("create_time")));
		String orderId = strCell(value.get("order_id"));
		line.put("order_id", "\t" + orderId);
		line.put("profitsharing_status", profitsharingLabel(value.get("profitsharing_status")));
		line.put("total_fee", fenToYuanPlain(value.get("total_fee")));
		line.put("charge", fenToYuanPlain(value.get("charge")));
		line.put("distributor_name", strCell(value.get("distributor_name")));
		line.put("refund_fee", fenToYuanPlain(value.get("refund_fee")));
		line.put("order_status", orderStatusLabel(strCell(value.get("order_status"))));
		return line;
	}

	private String profitsharingLabel(Object raw) {
		if (raw instanceof Number n) {
			int v = n.intValue();
			return switch (v) {
				case 1 -> "未结算";
				case 2 -> "已结算";
				default -> "--";
			};
		}
		return "--";
	}

	private String orderStatusLabel(String code) {
		if (!StringUtils.hasText(code)) {
			return "";
		}
		Map<String, String> m = orderStatusProperties.getHfpayOrderStatus();
		if (m != null) {
			String zh = m.get(code.trim());
			if (StringUtils.hasText(zh)) {
				return zh;
			}
		}
		return code;
	}

	private static String strCell(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim();
		return s;
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
