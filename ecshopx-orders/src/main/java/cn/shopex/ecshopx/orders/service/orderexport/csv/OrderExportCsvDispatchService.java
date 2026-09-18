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

package cn.shopex.ecshopx.orders.service.orderexport.csv;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrderExportCsvDispatchService {

	private final NormalOrderExportCsvService normalOrderExportCsvService;
	private final NormalMasterOrderExportCsvService normalMasterOrderExportCsvService;
	private final ServiceOrderExportCsvService serviceOrderExportCsvService;
	private final SupplierOrderExportCsvService supplierOrderExportCsvService;

	public OrderExportCsvDispatchService(
			NormalOrderExportCsvService normalOrderExportCsvService,
			NormalMasterOrderExportCsvService normalMasterOrderExportCsvService,
			ServiceOrderExportCsvService serviceOrderExportCsvService,
			SupplierOrderExportCsvService supplierOrderExportCsvService) {
		this.normalOrderExportCsvService = normalOrderExportCsvService;
		this.normalMasterOrderExportCsvService = normalMasterOrderExportCsvService;
		this.serviceOrderExportCsvService = serviceOrderExportCsvService;
		this.supplierOrderExportCsvService = supplierOrderExportCsvService;
	}

	public Optional<Map<String, String>> export(String exportType, long companyId, LinkedHashMap<String, Object> filter) {
		return switch (exportType) {
			case "normal_order" -> normalOrderExportCsvService.export(companyId, filter);
			case "normal_master_order" -> normalMasterOrderExportCsvService.export(companyId, filter);
			case "service_order" -> serviceOrderExportCsvService.export(companyId, filter);
			case "supplier_order" -> supplierOrderExportCsvService.export(companyId, filter);
			default -> Optional.empty();
		};
	}
}
