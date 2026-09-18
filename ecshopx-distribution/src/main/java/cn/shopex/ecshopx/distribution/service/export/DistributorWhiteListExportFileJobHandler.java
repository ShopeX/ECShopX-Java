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

package cn.shopex.ecshopx.distribution.service.export;

import cn.shopex.ecshopx.distribution.service.DistributorWhiteListExportQueryService;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportRow;
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
public class DistributorWhiteListExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(DistributorWhiteListExportFileJobHandler.class);

	private static final String EXPORT_TYPE = "distributor_white_list";
	private static final int BATCH = 100;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);

	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final DistributorWhiteListExportQueryService distributorWhiteListExportQueryService;

	public DistributorWhiteListExportFileJobHandler(
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService,
			DistributorWhiteListExportQueryService distributorWhiteListExportQueryService) {
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.distributorWhiteListExportQueryService = distributorWhiteListExportQueryService;
	}

	public void run(DistributorWhiteListExportJobContext ctx) {
		long total = distributorWhiteListExportQueryService.countGroupedByMobile(ctx.exportFilter());
		if (total == 0L) {
			return;
		}
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("mobile", "手机号");
		titles.put("username", "姓名");
		titles.put("shop", "店铺");
		List<Map<String, String>> allRows = new ArrayList<>();
		int pages = (int) ((total + BATCH - 1L) / BATCH);
		for (int page = 1; page <= pages; page++) {
			List<DistributorWhiteListExportRow> chunk =
					distributorWhiteListExportQueryService.pageGroupedByMobile(ctx.exportFilter(), page, BATCH);
			for (DistributorWhiteListExportRow r : chunk) {
				Map<String, String> row = new LinkedHashMap<>();
				row.put("mobile", r.mobile());
				row.put("username", r.username());
				row.put("shop", r.shopLabel());
				allRows.add(row);
			}
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + ctx.companyId() + "店铺白名单导出";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				ctx.merchantId(),
				ctx.supplierId(),
				EXPORT_TYPE,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}
}
