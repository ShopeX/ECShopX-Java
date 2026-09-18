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

package cn.shopex.ecshopx.salesperson.service.export;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfitExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(ProfitExportFileJobHandler.class);

	/**
	 * Raw export-type strings for which, when the job runs in a supplier-bound context
	 * ({@code supplierId > 0}), {@code supplier_id} is merged into the query filter so
	 * exported data is limited to that supplier. Comparison uses the request type as given
	 * (case-sensitive). The set aligns with admin profit export modes that support supplier scope.
	 */
	private static final Set<String> SUPPLIER_FILTER_EXPORT_TYPES = Set.of(
			"normal_order",
			"normal_master_order",
			"supplier_order",
			"supplier_goods",
			"aftersale_record_count",
			"items",
			"itemcode");

	private final OperatorsQueryService operatorsQueryService;
	private final ProfitExportDistributorCsvExportService profitExportDistributorCsvExportService;
	private final ProfitExportSalespersonCsvExportService profitExportSalespersonCsvExportService;
	private final ExportLogCreateService exportLogCreateService;

	public ProfitExportFileJobHandler(OperatorsQueryService operatorsQueryService,
			ProfitExportDistributorCsvExportService profitExportDistributorCsvExportService,
			ProfitExportSalespersonCsvExportService profitExportSalespersonCsvExportService,
			ExportLogCreateService exportLogCreateService) {
		this.operatorsQueryService = operatorsQueryService;
		this.profitExportDistributorCsvExportService = profitExportDistributorCsvExportService;
		this.profitExportSalespersonCsvExportService = profitExportSalespersonCsvExportService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(ProfitExportJobContext ctx) {
		long supplierId = 0L;
		if (ctx.distributorId() > 0) {
			Map<String, Object> opFilter = new LinkedHashMap<>();
			opFilter.put("company_id", ctx.companyId());
			opFilter.put("operator_id", ctx.distributorId());
			opFilter.put("operator_type", "supplier");
			Map<String, Object> supplier = operatorsQueryService.getInfo(opFilter);
			if (supplier != null && !supplier.isEmpty()) {
				supplierId = ctx.distributorId();
			}
		}

		Map<String, Object> mergedFilter = new LinkedHashMap<>();
		mergedFilter.put("company_id", ctx.companyId());
		mergedFilter.put("date", ctx.dateYm());
		mergedFilter.put("profit_user_type", ctx.profitUserTypeRaw());

		String rawExportType = ctx.rawExportType();
		if (supplierId > 0 && rawExportType != null && SUPPLIER_FILTER_EXPORT_TYPES.contains(rawExportType)) {
			mergedFilter.put("supplier_id", supplierId);
		}

		String exportTypeNorm = rawExportType == null ? "" : rawExportType.trim().toLowerCase(Locale.ROOT);
		Optional<Map<String, String>> upload = switch (exportTypeNorm) {
			case ProfitExportFileJobTypes.TYPE_PROFIT_DISTRIBUTOR -> profitExportDistributorCsvExportService.runExport(
					mergedFilter, supplierId, ctx.distributorId());
			case ProfitExportFileJobTypes.TYPE_PROFIT_SALESPERSON -> profitExportSalespersonCsvExportService.runExport(
					mergedFilter, supplierId, ctx.distributorId());
			default -> throw new IllegalStateException("无此导出类型");
		};

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
		String exportTypeForLog = ctx.rawExportType();
		Object orderClass = mergedFilter.get("order_class");
		if (orderClass != null && "drug".equals(orderClass.toString())) {
			exportTypeForLog = "drug_order";
		}
		exportLogCreateService.createFinishLog(ctx.companyId(), ctx.distributorId(), 0L, supplierId, exportTypeForLog,
				fileName, fileUrl, Instant.now().getEpochSecond());
	}
}
