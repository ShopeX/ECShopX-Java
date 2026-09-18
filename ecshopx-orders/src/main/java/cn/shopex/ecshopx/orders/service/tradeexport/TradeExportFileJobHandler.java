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

package cn.shopex.ecshopx.orders.service.tradeexport;

import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.tradeexport.csv.TradeDataExportCsvService;
import cn.shopex.ecshopx.orders.service.tradeexport.support.TradeExportQuerySupport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(TradeExportFileJobHandler.class);

	private final TradeMapper tradeMapper;
	private final TradeDataExportCsvService tradeDataExportCsvService;
	private final ExportLogCreateService exportLogCreateService;

	public TradeExportFileJobHandler(
			TradeMapper tradeMapper,
			TradeDataExportCsvService tradeDataExportCsvService,
			ExportLogCreateService exportLogCreateService) {
		this.tradeMapper = tradeMapper;
		this.tradeDataExportCsvService = tradeDataExportCsvService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(TradeExportJobContext ctx) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(ctx.filter());
		filter.put("company_id", Long.valueOf(ctx.companyId()));

		long jobCount = tradeMapper.selectCount(TradeExportQuerySupport.toCountWrapper(ctx.companyId(), filter));
		if (jobCount <= 0L) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}

		Optional<Map<String, String>> upload = tradeDataExportCsvService.export(ctx.companyId(), filter);
		if (upload.isEmpty()) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		Map<String, String> m = upload.get();
		String fileUrl = m.get("url");
		if (!StringUtils.hasText(fileUrl)) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		String fileName = m.get("filename");

		long merchantIdForLog = 0L;
		Object mid = filter.get("merchant_id");
		if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
			merchantIdForLog = longVal(mid);
		}

		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				merchantIdForLog,
				"tradedata",
				fileName != null ? fileName : "",
				fileUrl,
				Instant.now().getEpochSecond());
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
