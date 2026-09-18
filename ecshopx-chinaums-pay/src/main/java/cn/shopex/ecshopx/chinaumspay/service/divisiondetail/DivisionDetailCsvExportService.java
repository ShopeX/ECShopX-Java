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

package cn.shopex.ecshopx.chinaumspay.service.divisiondetail;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionDetail;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DivisionDetailCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(DivisionDetailCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final DateTimeFormatter ROW_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);

	private final ChinaumsDivisionDetailQueryService queryService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public DivisionDetailCsvExportService(ChinaumsDivisionDetailQueryService queryService,
			ExportCsvFileService exportCsvFileService, ExportLogCreateService exportLogCreateService) {
		this.queryService = queryService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(DivisionDetailExportContext ctx) {
		DivisionDetailExportFilter filter = ctx.getFilter();
		long companyId = ctx.getCompanyId();
		long operatorId = ctx.getOperatorId();

		int count = queryService.countByFilter(filter);
		if (count <= 0) {
			log.debug("division detail export skipped: count=0 companyId={}", companyId);
			return;
		}

		LinkedHashMap<String, String> titles = buildTitles();
		List<Map<String, String>> rows = new ArrayList<>();
		int pageSize = ChinaumsDivisionDetailQueryService.DEFAULT_PAGE_SIZE;
		for (int page = 1; ; page++) {
			List<ChinaumspayDivisionDetail> chunk = queryService.listPageByFilter(filter, page, pageSize);
			if (chunk.isEmpty()) {
				break;
			}
			for (ChinaumspayDivisionDetail d : chunk) {
				rows.add(toRow(d));
			}
		}

		String fileBase = FILE_TS.format(ZonedDateTime.now(CN)) + companyId + "分账单明细";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, titles, rows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return;
		}

		long finishSec = Instant.now().getEpochSecond();
		String filename = uploaded.getOrDefault("filename", fileBase + ".csv");
		String url = uploaded.get("url");
		exportLogCreateService.createFinishLog(companyId, operatorId, "chinaums_division_detail", filename, url,
				finishSec);
	}

	private static LinkedHashMap<String, String> buildTitles() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("division_id", "指令ID");
		m.put("order_id", "订单号");
		m.put("total_fee", "订单金额");
		m.put("actual_fee", "实际金额");
		m.put("commission_rate_fee", "收单手续费");
		m.put("division_fee", "分账金额");
		m.put("create_time", "创建时间");
		return m;
	}

	private static Map<String, String> toRow(ChinaumspayDivisionDetail d) {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("division_id", tabPrefixed(d.getDivisionId()));
		row.put("order_id", tabPrefixed(d.getOrderId()));
		row.put("total_fee", centsStringToYuan(d.getTotalFee()));
		row.put("actual_fee", centsStringToYuan(d.getActualFee()));
		row.put("commission_rate_fee", centsIntToYuan(d.getCommissionRateFee()));
		row.put("division_fee", centsIntToYuan(d.getDivisionFee()));
		row.put("create_time", formatCreateTime(d.getCreateTime()));
		return row;
	}

	private static String tabPrefixed(Long v) {
		if (v == null) {
			return "--";
		}
		return "\t" + v;
	}

	private static String centsStringToYuan(String cents) {
		if (!StringUtils.hasText(cents)) {
			return formatYuan(BigDecimal.ZERO);
		}
		try {
			BigDecimal bd = new BigDecimal(cents.trim()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			return formatYuan(bd);
		} catch (Exception e) {
			return formatYuan(BigDecimal.ZERO);
		}
	}

	private static String centsIntToYuan(Integer cents) {
		if (cents == null) {
			return formatYuan(BigDecimal.ZERO);
		}
		BigDecimal bd = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return formatYuan(bd);
	}

	private static String formatYuan(BigDecimal bd) {
		return bd.stripTrailingZeros().toPlainString();
	}

	private static String formatCreateTime(Integer unixSec) {
		if (unixSec == null || unixSec <= 0) {
			return "--";
		}
		return ROW_TIME.format(Instant.ofEpochSecond(unixSec.longValue()).atZone(CN));
	}
}
