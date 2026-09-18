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

package cn.shopex.ecshopx.orders.service.invoice.export;

import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvoiceExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(InvoiceExportFileJobHandler.class);

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final InvoiceExportCsvExportService invoiceExportCsvExportService;
	private final ExportLogCreateService exportLogCreateService;

	public InvoiceExportFileJobHandler(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			InvoiceExportCsvExportService invoiceExportCsvExportService,
			ExportLogCreateService exportLogCreateService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.invoiceExportCsvExportService = invoiceExportCsvExportService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(InvoiceExportJobContext ctx) {
		long jobCount;
		if ("supplier_order".equals(ctx.orderType())) {
			jobCount =
					supplierOrderMapper.selectCount(
							InvoiceExportSupplierOrderQuerySupport.toCountWrapper(ctx.companyId(), ctx.filter()));
		} else {
			jobCount =
					normalOrdersMapper.selectCount(
							InvoiceExportNormalOrderQuerySupport.toCountWrapper(ctx.companyId(), ctx.filter()));
		}
		if (jobCount <= 0) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}

		Optional<Map<String, String>> upload = invoiceExportCsvExportService.runExport(ctx);
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
		Object mid = ctx.filter().get("merchant_id");
		if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
			merchantIdForLog = longVal(mid);
		}
		long supplierIdForLog = "supplier_order".equals(ctx.orderType()) ? ctx.operatorId() : 0L;

		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				merchantIdForLog,
				supplierIdForLog,
				"invoice",
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
