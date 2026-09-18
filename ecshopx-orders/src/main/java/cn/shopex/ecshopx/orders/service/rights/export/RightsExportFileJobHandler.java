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

package cn.shopex.ecshopx.orders.service.rights.export;

import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RightsExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(RightsExportFileJobHandler.class);

	private final RightsMapper rightsMapper;
	private final RightsExportCsvExportService rightsExportCsvExportService;
	private final ExportLogCreateService exportLogCreateService;

	public RightsExportFileJobHandler(
			RightsMapper rightsMapper,
			RightsExportCsvExportService rightsExportCsvExportService,
			ExportLogCreateService exportLogCreateService) {
		this.rightsMapper = rightsMapper;
		this.rightsExportCsvExportService = rightsExportCsvExportService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(RightsExportJobContext ctx) {
		LinkedHashMap<String, Object> raw = ctx.filter();
		boolean datapassBlock = parseDatapassBlock(raw.get("datapass_block"));
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(raw);
		work.remove("datapass_block");
		long jobCount = rightsMapper.selectCount(RightsExportQuerySupport.toCountWrapper(ctx.companyId(), work));
		if (jobCount <= 0) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		Optional<Map<String, String>> upload = rightsExportCsvExportService.runExport(ctx.companyId(), work, datapassBlock);
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
				0L,
				0L,
				"right",
				fileName != null ? fileName : "",
				fileUrl,
				Instant.now().getEpochSecond());
	}

	private static boolean parseDatapassBlock(Object raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.toString().trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		try {
			return Integer.parseInt(s) != 0;
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
