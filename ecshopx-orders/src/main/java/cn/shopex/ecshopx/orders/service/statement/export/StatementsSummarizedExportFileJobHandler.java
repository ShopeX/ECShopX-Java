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

package cn.shopex.ecshopx.orders.service.statement.export;

import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatementsSummarizedExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(StatementsSummarizedExportFileJobHandler.class);

	private final StatementsSummarizedCsvExportService statementsSummarizedCsvExportService;
	private final ExportLogCreateService exportLogCreateService;

	public StatementsSummarizedExportFileJobHandler(
			StatementsSummarizedCsvExportService statementsSummarizedCsvExportService,
			ExportLogCreateService exportLogCreateService) {
		this.statementsSummarizedCsvExportService = statementsSummarizedCsvExportService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(StatementsSummarizedExportJobContext ctx) {
		Optional<Map<String, String>> upload =
				statementsSummarizedCsvExportService.runExport(ctx.filter(), ctx.operatorId());
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
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				"statements",
				fileName != null ? fileName : "",
				fileUrl,
				Instant.now().getEpochSecond());
	}
}
