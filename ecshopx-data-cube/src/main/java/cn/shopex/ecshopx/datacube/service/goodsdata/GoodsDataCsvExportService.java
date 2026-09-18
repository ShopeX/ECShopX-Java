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

package cn.shopex.ecshopx.datacube.service.goodsdata;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
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
public class GoodsDataCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(GoodsDataCsvExportService.class);

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);

	private static final LinkedHashMap<String, String> TITLE_HEADERS = new LinkedHashMap<>();

	static {
		TITLE_HEADERS.put("no", "NO");
		TITLE_HEADERS.put("sap_code", "商品编号");
		TITLE_HEADERS.put("top_level", "分类");
		TITLE_HEADERS.put("product", "商品名称");
		TITLE_HEADERS.put("quantity", "销量");
		TITLE_HEADERS.put("fix_price", "销售额");
		TITLE_HEADERS.put("settle_price", "实付额");
	}

	private final AdminGoodsDataListService adminGoodsDataListService;

	private final ExportCsvFileService exportCsvFileService;

	private final ExportLogCreateService exportLogCreateService;

	public GoodsDataCsvExportService(
			AdminGoodsDataListService adminGoodsDataListService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.adminGoodsDataListService = adminGoodsDataListService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(AdminGoodsDataFilter filter) {
		List<Map<String, Object>> list = adminGoodsDataListService.getGoodsDataList(filter);
		if (list.isEmpty()) {
			return;
		}
		List<Map<String, String>> csvRows = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Map<String, String> csv = new LinkedHashMap<>();
			csv.put("no", stringifyCell(row.get("no")));
			csv.put("sap_code", stringifyCell(row.get("sap_code")));
			csv.put("top_level", stringifyCell(row.get("top_level")));
			csv.put("product", stringifyCell(row.get("product")));
			csv.put("quantity", stringifyCell(row.get("quantity")));
			csv.put("fix_price", stringifyCell(row.get("fix_price")));
			csv.put("settle_price", stringifyCell(row.get("settle_price")));
			csvRows.add(csv);
		}
		String fileBase = FILE_TS.format(ZonedDateTime.now(SHANGHAI)) + "goodsData";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, TITLE_HEADERS, csvRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		long merchantId = filter.merchantIdOrNull() != null ? filter.merchantIdOrNull() : 0L;
		exportLogCreateService.createFinishLog(
				filter.companyId(),
				filter.operatorId(),
				merchantId,
				"goods_data",
				uploaded.get("filename"),
				uploaded.get("url"),
				finishSec);
	}

	private static String stringifyCell(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}
}
