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

package cn.shopex.ecshopx.popularize.service.export;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeStaticExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(PopularizeStaticExportFileJobHandler.class);

	private final OperatorsQueryService operatorsQueryService;
	private final PopularizeStaticCsvExportService popularizeStaticCsvExportService;
	private final ExportLogCreateService exportLogCreateService;

	public PopularizeStaticExportFileJobHandler(
			OperatorsQueryService operatorsQueryService,
			PopularizeStaticCsvExportService popularizeStaticCsvExportService,
			ExportLogCreateService exportLogCreateService) {
		this.operatorsQueryService = operatorsQueryService;
		this.popularizeStaticCsvExportService = popularizeStaticCsvExportService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(PopularizeStaticExportJobContext ctx) {
		long supplierId = 0L;
		if (ctx.operatorId() > 0L) {
			LinkedHashMap<String, Object> opFilter = new LinkedHashMap<>();
			opFilter.put("company_id", ctx.companyId());
			opFilter.put("operator_id", ctx.operatorId());
			opFilter.put("operator_type", "supplier");
			Map<String, Object> supplier = operatorsQueryService.getInfo(opFilter);
			if (supplier != null && !supplier.isEmpty()) {
				supplierId = ctx.operatorId();
			}
		}
		if (log.isTraceEnabled() && supplierId > 0L) {
			log.trace("popularize static export ignores supplier_id filter even when supplierId={}", supplierId);
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", ctx.companyId());
		if (StringUtils.hasText(ctx.mobile())) {
			merged.put("mobile", ctx.mobile().trim());
		}
		if (StringUtils.hasText(ctx.username())) {
			merged.put("username", ctx.username().trim());
		}
		if (ctx.distributorIdRaw() != null && StringUtils.hasText(ctx.distributorIdRaw())) {
			merged.put("distributor_id", ctx.distributorIdRaw());
		}
		if (ctx.dIds() != null && !ctx.dIds().isEmpty()) {
			merged.put("dIds", ctx.dIds());
		}
		merged.put("date_start", ctx.dateStart());
		merged.put("date_end", ctx.dateEnd());

		Optional<Map<String, String>> upload =
				popularizeStaticCsvExportService.runExport(merged, ctx.datapassBlock());
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
				ctx.merchantId(),
				supplierId,
				"popularizestatic",
				fileName != null ? fileName : "",
				fileUrl,
				Instant.now().getEpochSecond());
	}
}
