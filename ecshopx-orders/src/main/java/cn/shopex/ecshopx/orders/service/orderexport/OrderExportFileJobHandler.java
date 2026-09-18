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

package cn.shopex.ecshopx.orders.service.orderexport;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.orders.service.orderexport.csv.OrderExportCsvDispatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(OrderExportFileJobHandler.class);

	private final OperatorsMapper operatorsMapper;
	private final OrderExportCountCoordinator orderExportCountCoordinator;
	private final OrderExportCsvDispatchService orderExportCsvDispatchService;
	private final ExportLogCreateService exportLogCreateService;

	public OrderExportFileJobHandler(
			OperatorsMapper operatorsMapper,
			OrderExportCountCoordinator orderExportCountCoordinator,
			OrderExportCsvDispatchService orderExportCsvDispatchService,
			ExportLogCreateService exportLogCreateService) {
		this.operatorsMapper = operatorsMapper;
		this.orderExportCountCoordinator = orderExportCountCoordinator;
		this.orderExportCsvDispatchService = orderExportCsvDispatchService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void run(OrderExportJobContext ctx) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(ctx.filter());
		filter.put("company_id", Long.valueOf(ctx.companyId()));

		long supplierIdForLog = 0L;
		if (ctx.operatorId() > 0L) {
			Long ct =
					operatorsMapper.selectCount(
							new LambdaQueryWrapper<Operators>()
									.eq(Operators::getCompanyId, ctx.companyId())
									.eq(Operators::getOperatorId, ctx.operatorId())
									.eq(Operators::getOperatorType, "supplier"));
			if (ct != null && ct > 0L) {
				supplierIdForLog = ctx.operatorId();
				applySupplierFilterPatch(ctx.exportType(), filter, ctx.operatorId());
			}
		}

		String orderTypeKey = orderTypeKeyFromExportType(ctx.exportType());
		long jobCount = orderExportCountCoordinator.count(orderTypeKey, ctx.exportType(), filter);
		if (jobCount <= 0L) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}

		Optional<Map<String, String>> upload =
				orderExportCsvDispatchService.export(ctx.exportType(), ctx.companyId(), filter);
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

		String exportTypeForLog = ctx.exportType();
		if (filter.containsKey("order_class") && "drug".equals(String.valueOf(filter.get("order_class")))) {
			exportTypeForLog = "drug_order";
		}

		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				merchantIdForLog,
				supplierIdForLog,
				exportTypeForLog,
				fileName != null ? fileName : "",
				fileUrl,
				Instant.now().getEpochSecond());
	}

	private static void applySupplierFilterPatch(String exportType, LinkedHashMap<String, Object> filter, long operatorId) {
		switch (exportType) {
			case "normal_order", "normal_master_order", "supplier_order", "supplier_goods", "aftersale_record_count",
					"items", "itemcode" -> filter.put("supplier_id", Long.valueOf(operatorId));
			default -> {
			}
		}
	}

	private static String orderTypeKeyFromExportType(String exportType) {
		if ("service_order".equals(exportType)) {
			return "service";
		}
		if ("supplier_order".equals(exportType)) {
			return "supplier_order";
		}
		return "normal";
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
